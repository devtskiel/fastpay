package com.example.myapplication.ui.screens

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.PaymentData
import com.example.myapplication.data.SwiftPayService
import com.example.myapplication.data.SettingsManager
import com.example.myapplication.data.TransactionStore
import com.example.myapplication.data.createSwiftPayService
import com.example.myapplication.data.api.*
import com.example.myapplication.data.repository.PaymentRepository
import com.example.myapplication.data.repository.TransactionRepository
import com.example.myapplication.di.DIContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val NFC_SESSION_SECONDS = 90

sealed interface MiniAppUiState {
    data object Idle : MiniAppUiState
    data class PaymentConsent(val data: PaymentData) : MiniAppUiState
    data class PaymentRedirect(val url: String, val data: PaymentData? = null) : MiniAppUiState
    data class PaymentLinkReady(val url: String) : MiniAppUiState
    data class WaitingForNFC(
        val amount: Double,
        val merchantName: String = "SwiftPay Merchant",
        val merchantAddress: String = "Manila, PH",
        val timeLeft: Int = 60,
        val sessionDurationSeconds: Int = NFC_SESSION_SECONDS,
        val errorMessage: String? = null,
        val statusMessage: String? = null
    ) : MiniAppUiState
    data class WaitingForCVV(
        val amount: Double,
        val pan: String,
        val expiry: String,
        val label: String,
        val sessionRef: String = "VAULT${System.currentTimeMillis()}"
    ) : MiniAppUiState
    data class DynamicQrReady(val qrData: String, val amount: Double) : MiniAppUiState
    data class Processing(val message: String) : MiniAppUiState
    data class Error(val message: String) : MiniAppUiState
}

class MiniAppViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsManager = SettingsManager(application)
    private val transactionStore = TransactionStore(application)
    
    // We'll use a dynamic repository that always uses fresh credentials
    private suspend fun getRepository(): PaymentRepository {
        return PaymentRepository(settingsManager.createSwiftPayService())
    }

    private val transactionRepository: TransactionRepository by lazy {
        TransactionRepository(SwiftPayService(), transactionStore)
    }

    var uiState by mutableStateOf<MiniAppUiState>(MiniAppUiState.Idle)
        private set

    private var nfcTimerJob: Job? = null

    val transactions = transactionRepository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val walletBalance = settingsManager.walletBalance.map { it?.toDoubleOrNull() ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    var isLoadingBalance by mutableStateOf(false)
        private set

    var institutions by mutableStateOf<List<BankResponse>>(emptyList())
        private set

    var webhooks by mutableStateOf<List<WebhookRequest>>(emptyList())
        private set

    init {
        refreshTransactions()
    }

    fun refreshBalance() {
        viewModelScope.launch {
            isLoadingBalance = true
            try {
                val balance = getRepository().getWalletBalance()
                settingsManager.saveWalletBalance(balance.toString())
            } catch (e: Exception) {
                Log.e("MiniAppViewModel", "Refresh Balance Error", e)
            } finally {
                isLoadingBalance = false
            }
        }
    }

    fun refreshTransactions() {
        viewModelScope.launch {
            // Use the correct service for syncing
            val service = settingsManager.createSwiftPayService()
            val syncRepo = TransactionRepository(service, transactionStore)
            syncRepo.syncWithApi()
        }
    }

    fun onWebhooksRequest() {
        viewModelScope.launch {
            getRepository().getWebhooks()
                .onSuccess { webhooks = it }
                .onFailure { uiState = MiniAppUiState.Error(it.message ?: "Failed to load webhooks") }
        }
    }

    fun onAddWebhookRequest(name: String, url: String) {
        viewModelScope.launch {
            uiState = MiniAppUiState.Processing("Registering Webhook...")
            getRepository().registerWebhook(name, url)
                .onSuccess { 
                    onWebhooksRequest()
                    uiState = MiniAppUiState.Idle 
                }
                .onFailure { uiState = MiniAppUiState.Error(it.message ?: "Failed") }
        }
    }

    fun onDeleteWebhookRequest(id: String) {
        viewModelScope.launch {
            getRepository().deleteWebhook(id)
                .onSuccess { onWebhooksRequest() }
                .onFailure { uiState = MiniAppUiState.Error(it.message ?: "Failed") }
        }
    }

    fun onCreateInvoiceRequest(amount: Double, description: String) {
        viewModelScope.launch {
            uiState = MiniAppUiState.Processing("Creating Invoice...")
            getRepository().createInvoice(amount, description)
                .onSuccess { uiState = MiniAppUiState.Idle }
                .onFailure { uiState = MiniAppUiState.Error(it.message ?: "Failed") }
        }
    }

    fun onBanksRequest() {
        viewModelScope.launch {
            getRepository().getInstitutions()
                .onSuccess { institutions = it }
                .onFailure { uiState = MiniAppUiState.Error(it.message ?: "Failed to load institutions") }
        }
    }

    fun onDisburseRequest(
        amount: Double,
        accountNo: String,
        firstName: String,
        lastName: String,
        middleName: String? = null,
        bankCode: String? = null,
        remarks: String? = null,
        email: String? = null,
        mobileNumber: String? = null,
        address: com.example.myapplication.data.api.AddressV2? = null
    ) {
        viewModelScope.launch {
            uiState = MiniAppUiState.Processing("Processing Payout...")
            getRepository().disburse(
                amount, accountNo, firstName, lastName, middleName, bankCode, remarks, email, mobileNumber, address
            )
                .onSuccess {
                    recordLocalTransaction("D${System.currentTimeMillis()}", -amount, "SUCCESS")
                    uiState = MiniAppUiState.Idle
                }
                .onFailure {
                    uiState = MiniAppUiState.Error(it.message ?: "Payout Failed")
                }
        }
    }

    fun onGenerateVcaRequest(accountName: String) {
        viewModelScope.launch {
            uiState = MiniAppUiState.Processing("Generating VCA...")
            getRepository().generateVca(accountName)
                .onSuccess { uiState = MiniAppUiState.Idle }
                .onFailure { uiState = MiniAppUiState.Error(it.message ?: "Failed") }
        }
    }

    fun onVcaTransactionsRequest() {
        viewModelScope.launch {
            getRepository().getVcaTransactions()
                .onSuccess { /* Handle VCA tx */ }
                .onFailure { uiState = MiniAppUiState.Error(it.message ?: "Failed") }
        }
    }

    fun onCreateOrderRequest(amount: Double, customerName: String?, email: String?) {
        viewModelScope.launch {
            uiState = MiniAppUiState.Processing("Initializing Order...")
            val refNo = "ORD${System.currentTimeMillis()}"
            getRepository().createOrder(amount, refNo, customerName, email).onSuccess { response ->
                if (response.customerRedirectUrl != null) {
                    recordLocalTransaction(response.paymentId ?: refNo, amount, "PENDING")
                    uiState = MiniAppUiState.PaymentRedirect(response.customerRedirectUrl)
                }
            }.onFailure {
                uiState = MiniAppUiState.Error(it.message ?: "Order Creation Failed")
            }
        }
    }

    fun onBootstrapQrphRequest(amount: Double) {
        viewModelScope.launch {
            uiState = MiniAppUiState.Processing("Generating QR Ph...")
            val refNo = "QRPH${System.currentTimeMillis()}"
            getRepository().bootstrapQrph(amount, refNo).onSuccess { response ->
                if (response.qrCode != null) {
                    recordLocalTransaction(response.paymentId ?: refNo, amount, "PENDING")
                    uiState = MiniAppUiState.DynamicQrReady(response.qrCode, amount)
                }
            }.onFailure {
                uiState = MiniAppUiState.Error(it.message ?: "QR Generation Failed")
            }
        }
    }

    fun onPaymentLinkRequest(data: PaymentData) {
        viewModelScope.launch {
            uiState = MiniAppUiState.Processing("Generating Link...")
            getRepository().createPaymentLink(data.amount, data.description).onSuccess {
                uiState = MiniAppUiState.PaymentLinkReady(it.paymentLinkUrl ?: "")
            }.onFailure { uiState = MiniAppUiState.Error(it.message ?: "Link Generation Failed") }
        }
    }

    fun onScanNFCCard(amount: Double) {
        startNFCTimer(amount)
    }

    fun onGenerateDynamicQr(amount: Double) {
        viewModelScope.launch {
            uiState = MiniAppUiState.Processing("Generating QR...")
            getRepository().createDynamicQr(amount).onSuccess {
                uiState = MiniAppUiState.DynamicQrReady(it.qrCodeBody ?: "", amount)
            }.onFailure { uiState = MiniAppUiState.Error(it.message ?: "QR Failed") }
        }
    }

    private fun startNFCTimer(amount: Double) {
        nfcTimerJob?.cancel()
        nfcTimerJob = viewModelScope.launch {
            var seconds = NFC_SESSION_SECONDS
            while (seconds >= 0) {
                uiState = MiniAppUiState.WaitingForNFC(amount, timeLeft = seconds)
                if (seconds == 0) {
                    uiState = MiniAppUiState.Error("NFC Session Timeout")
                    return@launch
                }
                delay(1000)
                seconds--
            }
        }
    }

    fun onNFCCardDetected(pan: String, expiry: String, label: String) {
        nfcTimerJob?.cancel()
        val amount = (uiState as? MiniAppUiState.WaitingForNFC)?.amount ?: 0.0
        uiState = MiniAppUiState.WaitingForCVV(amount, pan, expiry, label)
    }

    fun onCvvEntered(cvv: String) {
        val state = uiState as? MiniAppUiState.WaitingForCVV ?: return
        viewModelScope.launch {
            uiState = MiniAppUiState.Processing("Authorizing Card...")
            getRepository().processVaultPayment(
                state.amount, 
                CardDetails(state.pan, "12", "2030", cvv), 
                state.sessionRef
            )
                .onSuccess {
                    recordLocalTransaction(it.paymentId ?: "V", state.amount, "SUCCESS")
                    uiState = MiniAppUiState.Idle
                }
                .onFailure { uiState = MiniAppUiState.Error(it.message ?: "Card Payment Failed") }
        }
    }

    fun approvePayment(data: PaymentData) {
        viewModelScope.launch {
            uiState = MiniAppUiState.Processing("Creating Checkout...")
            getRepository().createCheckout(data).onSuccess {
                if (it.redirectUrl != null) {
                    uiState = MiniAppUiState.PaymentRedirect(it.redirectUrl, data)
                }
            }.onFailure { uiState = MiniAppUiState.Error(it.message ?: "Checkout Failed") }
        }
    }

    private suspend fun recordLocalTransaction(id: String, amount: Double, status: String) {
        transactionRepository.saveTransaction(InternalTransaction(id, amount, status, TransactionStore.nowLabel()))
        if (status.uppercase() == "SUCCESS" && amount > 0) {
            val current = walletBalance.value
            settingsManager.saveWalletBalance((current + amount).toString())
        }
    }

    fun dismissConsent() { uiState = MiniAppUiState.Idle }
    fun dismissError() { uiState = MiniAppUiState.Idle }

    fun handleDeepLink(linkId: String, status: String?) {
        viewModelScope.launch {
            recordLocalTransaction(linkId, 0.0, status ?: "SUCCESS")
            refreshTransactions()
        }
    }
    
    fun retryNfc() { (uiState as? MiniAppUiState.WaitingForNFC)?.let { startNFCTimer(it.amount) } }
    
    fun onSaveSettings(map: Map<String, String>) {
        viewModelScope.launch {
            map["secretKey"]?.let { settingsManager.saveSecretKey(it) }
            map["publicKey"]?.let { settingsManager.savePublicKey(it) }
            map["mid"]?.let { settingsManager.saveMid(it) }
            map["terminalId"]?.let { settingsManager.saveTerminalId(it) }
            map["environment"]?.let { settingsManager.saveEnvironment(it) }
        }
    }

    var transactionStatusMap by mutableStateOf<Map<String, OrderResponse>>(emptyMap())
        private set

    fun fetchPaymentStatus(id: String) {
        viewModelScope.launch {
            getRepository().getPaymentStatus(id).onSuccess { response ->
                // Map VaultPaymentResponse to OrderResponse if needed, or keep separate
            }
        }
    }

    fun getPaymentStatusForId(id: String): OrderResponse? = transactionStatusMap[id]

    fun onAddMemberRequest(name: String, email: String, role: String) {
        viewModelScope.launch {
            Log.d("MiniAppViewModel", "Inviting $name ($email) as $role")
        }
    }

    fun onDeleteMemberRequest(email: String) {
        viewModelScope.launch {
            Log.d("MiniAppViewModel", "Removing member $email")
        }
    }
}
