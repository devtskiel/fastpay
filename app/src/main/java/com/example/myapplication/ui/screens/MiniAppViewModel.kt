package com.example.myapplication.ui.screens

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.bridge.SwiftPaySDKBridge
import com.example.myapplication.bridge.PaymentData
import com.example.myapplication.data.SwiftPayService
import com.example.myapplication.data.SettingsManager
import com.example.myapplication.data.TransactionStore
import com.example.myapplication.data.createSwiftPayService
import com.example.myapplication.data.loadSwiftPayCredentials
import com.example.myapplication.data.api.InternalTransaction
import com.example.myapplication.data.api.OrderResponse
import com.example.myapplication.data.api.VaultPaymentResponse
import com.example.myapplication.data.repository.TransactionRepository
import com.example.myapplication.util.normalizePaymentStatus
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
    private val repository: TransactionRepository by lazy {
        TransactionRepository(SwiftPayService(), transactionStore)
    }

    var uiState by mutableStateOf<MiniAppUiState>(MiniAppUiState.Idle)
        private set

    var bridge: SwiftPaySDKBridge? = null
    private var nfcTimerJob: Job? = null

    val transactions = repository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val walletBalance = settingsManager.walletBalance.map { it?.toDoubleOrNull() ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    var isLoadingBalance by mutableStateOf(false)
        private set

    private suspend fun getService(): SwiftPayService = settingsManager.createSwiftPayService()

    fun refreshBalance() {
        viewModelScope.launch {
            isLoadingBalance = true
            try {
                val freshBalance = getService().getWalletBalance()
                settingsManager.saveWalletBalance(freshBalance.toString())
            } catch (e: Exception) {
                Log.e("MiniAppViewModel", "Refresh Error", e)
            } finally {
                isLoadingBalance = false
            }
        }
    }

    fun onWebhooksRequest() {
        viewModelScope.launch {
            getService().getWebhooks().onSuccess { bridge?.sendResponse(it) }
                .onFailure { bridge?.sendError(it.message ?: "Failed") }
        }
    }

    fun onAddWebhookRequest(name: String, url: String) {
        viewModelScope.launch {
            getService().registerWebhook(name, url).onSuccess { bridge?.sendResponse(it) }
                .onFailure { bridge?.sendError(it.message ?: "Failed") }
        }
    }

    fun onDeleteWebhookRequest(id: String) {
        viewModelScope.launch {
            getService().deleteWebhook(id).onSuccess { bridge?.sendResponse(true) }
                .onFailure { bridge?.sendError(it.message ?: "Failed") }
        }
    }

    fun onCreateInvoiceRequest(amount: Double, description: String) {
        viewModelScope.launch {
            getService().createInvoice(amount, description).onSuccess { bridge?.sendResponse(it) }
                .onFailure { bridge?.sendError(it.message ?: "Failed") }
        }
    }

    fun onBanksRequest() {
        viewModelScope.launch {
            getService().getInstitutions().onSuccess { bridge?.sendResponse(it) }
                .onFailure { bridge?.sendError(it.message ?: "Failed") }
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
            getService().disburse(
                amount, accountNo, firstName, lastName, middleName, bankCode, remarks, email, mobileNumber, address
            )
                .onSuccess {
                    recordLocalTransaction("D${System.currentTimeMillis()}", -amount, "SUCCESS")
                    uiState = MiniAppUiState.Idle
                    bridge?.sendResponse(mapOf("status" to "scheduled"))
                }
                .onFailure {
                    uiState = MiniAppUiState.Error(it.message ?: "Failed")
                    bridge?.sendError(it.message ?: "Failed")
                }
        }
    }

    fun onGenerateVcaRequest(accountName: String) {
        viewModelScope.launch {
            getService().generateVca(accountName).onSuccess { bridge?.sendResponse(it) }
                .onFailure { bridge?.sendError(it.message ?: "Failed") }
        }
    }

    fun onVcaTransactionsRequest() {
        viewModelScope.launch {
            getService().getVcaTransactions().onSuccess { bridge?.sendResponse(it) }
                .onFailure { bridge?.sendError(it.message ?: "Failed") }
        }
    }

    fun onCreateOrderRequest(amount: Double, customerName: String?, email: String?) {
        viewModelScope.launch {
            uiState = MiniAppUiState.Processing("Initializing Order...")
            val refNo = "ORD${System.currentTimeMillis()}"
            getService().createOrder(amount, refNo, customerName, email).onSuccess { response ->
                if (response.customerRedirectUrl != null) {
                    recordLocalTransaction(response.paymentId ?: refNo, amount, "PENDING")
                    uiState = MiniAppUiState.PaymentRedirect(response.customerRedirectUrl)
                    bridge?.sendResponse(response)
                } else bridge?.sendError("No redirect URL")
            }.onFailure {
                uiState = MiniAppUiState.Error(it.message ?: "Failed")
                bridge?.sendError(it.message ?: "Failed")
            }
        }
    }

    fun onBootstrapQrphRequest(amount: Double) {
        viewModelScope.launch {
            uiState = MiniAppUiState.Processing("Generating QR Ph...")
            val refNo = "QRPH${System.currentTimeMillis()}"
            getService().bootstrapQrph(amount, refNo).onSuccess { response ->
                if (response.qrCode != null) {
                    recordLocalTransaction(response.paymentId ?: refNo, amount, "PENDING")
                    uiState = MiniAppUiState.DynamicQrReady(response.qrCode, amount)
                    bridge?.sendResponse(response)
                } else bridge?.sendError("No QR data")
            }.onFailure {
                uiState = MiniAppUiState.Error(it.message ?: "Failed")
                bridge?.sendError(it.message ?: "Failed")
            }
        }
    }

    fun onBalanceRequest() {
        viewModelScope.launch { bridge?.sendResponse(walletBalance.value) }
    }

    fun onTransactionsRequest() {
        viewModelScope.launch {
            getService().getInternalTransactions().onSuccess { bridge?.sendResponse(it) }
                .onFailure { bridge?.sendError(it.message ?: "Failed") }
        }
    }

    fun onPaymentChannelsRequest() {
        viewModelScope.launch { bridge?.sendResponse(getService().getPaymentChannels()) }
    }

    fun onPaymentLinkRequest(data: PaymentData) {
        viewModelScope.launch {
            getService().createPaymentLink(data.amount, data.description).onSuccess {
                uiState = MiniAppUiState.PaymentLinkReady(it.paymentLinkUrl ?: "")
                bridge?.sendResponse(it)
            }.onFailure { bridge?.sendError(it.message ?: "Failed") }
        }
    }

    fun onScanNFCCard(amount: Double) {
        startNFCTimer(amount)
    }

    fun onGenerateDynamicQr(amount: Double) {
        viewModelScope.launch {
            getService().createDynamicQr(amount).onSuccess {
                uiState = MiniAppUiState.DynamicQrReady(it.qrCodeBody ?: "", amount)
                bridge?.sendResponse(it)
            }.onFailure { bridge?.sendError(it.message ?: "Failed") }
        }
    }

    private fun startNFCTimer(amount: Double) {
        nfcTimerJob?.cancel()
        nfcTimerJob = viewModelScope.launch {
            var seconds = NFC_SESSION_SECONDS
            while (seconds >= 0) {
                uiState = MiniAppUiState.WaitingForNFC(amount, timeLeft = seconds)
                if (seconds == 0) {
                    uiState = MiniAppUiState.Error("Timeout")
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
            uiState = MiniAppUiState.Processing("Authorizing...")
            getService().processVaultPayment(state.amount, com.example.myapplication.data.api.CardDetails(state.pan, "12", "2030", cvv), state.sessionRef)
                .onSuccess {
                    recordLocalTransaction(it.paymentId ?: "V", state.amount, "SUCCESS")
                    uiState = MiniAppUiState.Idle
                    bridge?.sendResponse(mapOf("status" to "success"))
                }
                .onFailure { uiState = MiniAppUiState.Error(it.message ?: "Failed") }
        }
    }

    fun approvePayment(data: PaymentData) {
        viewModelScope.launch {
            uiState = MiniAppUiState.Processing("Creating Checkout...")
            getService().createCheckout(data).onSuccess {
                if (it.redirectUrl != null) {
                    uiState = MiniAppUiState.PaymentRedirect(it.redirectUrl, data)
                }
            }.onFailure { uiState = MiniAppUiState.Error(it.message ?: "Failed") }
        }
    }

    private suspend fun recordLocalTransaction(id: String, amount: Double, status: String) {
        repository.saveTransaction(InternalTransaction(id, amount, status, TransactionStore.nowLabel()))
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
        }
    }
    
    // Stubs for missing methods required by components
    fun onNfcProgress(message: String) {}
    fun onAddMemberRequest(name: String, email: String, role: String) {}
    fun onDeleteMemberRequest(id: String) {}
    fun onMembersRequest() {}
    fun requestPaymentConsent(data: PaymentData) {
        uiState = MiniAppUiState.PaymentConsent(data)
    }
    fun onNfcError(error: String) { uiState = MiniAppUiState.Error(error) }
    fun retryNfc() { (uiState as? MiniAppUiState.WaitingForNFC)?.let { startNFCTimer(it.amount) } }
    fun onSaveSettings(map: Map<String, String>) {
        viewModelScope.launch {
            map["merchantAlias"]?.let { settingsManager.saveMerchantAlias(it) }
            map["mid"]?.let { settingsManager.saveMid(it) }
            map["terminalId"]?.let { settingsManager.saveTerminalId(it) }
            map["environment"]?.let { settingsManager.saveEnvironment(it) }
            bridge?.sendResponse(true)
        }
    }

    fun getSettings() {
        viewModelScope.launch {
            val credentials = settingsManager.loadSwiftPayCredentials()
            val map = mapOf(
                "merchantAlias" to (settingsManager.merchantAlias.first() ?: ""),
                "mid" to (credentials.mid ?: ""),
                "publicKey" to (credentials.publicKey ?: ""),
                "environment" to credentials.environment
            )
            bridge?.sendResponse(map)
        }
    }
    var transactionStatusMap by mutableStateOf<Map<String, OrderResponse>>(emptyMap())
        private set

    fun fetchPaymentStatus(id: String) {
        viewModelScope.launch {
            getService().getPaymentStatusV2(id).onSuccess { response ->
                transactionStatusMap = transactionStatusMap + (id to response)
            }
        }
    }

    fun fetchPaymentStatuses(ids: List<String>) {
        viewModelScope.launch {
            ids.forEach { id ->
                getService().getPaymentStatusV2(id).onSuccess { response ->
                    transactionStatusMap = transactionStatusMap + (id to response)
                }
            }
        }
    }

    fun getPaymentStatusForId(id: String): OrderResponse? = transactionStatusMap[id]
}
