package com.example.myapplication.ui.screens

import android.app.Application
import android.util.Log
import androidx.compose.runtime.*
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val NFC_SESSION_SECONDS = 90

sealed interface MiniAppUiState {
    data object Idle : MiniAppUiState
    data class Processing(val message: String) : MiniAppUiState
    data class Error(val message: String) : MiniAppUiState
    data class DynamicQrReady(val qrData: String, val amount: Double) : MiniAppUiState
    data class PaymentLinkReady(val url: String) : MiniAppUiState
    data class PaymentRedirect(val url: String, val data: PaymentData? = null) : MiniAppUiState
    data class WaitingForNFC(
        val amount: Double, 
        val timeLeft: Int,
        val merchantName: String = "SwiftPay Merchant",
        val merchantAddress: String = "Manila, PH"
    ) : MiniAppUiState
    data class WaitingForCVV(val amount: Double, val pan: String, val expiry: String, val label: String) : MiniAppUiState
}

class MiniAppViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsManager = SettingsManager(application)
    private val transactionStore = TransactionStore(application)
    
    private suspend fun getRepository() = PaymentRepository(settingsManager.createSwiftPayService())

    var uiState by mutableStateOf<MiniAppUiState>(MiniAppUiState.Idle)
        private set

    val transactions = TransactionRepository(SwiftPayService(), transactionStore)
        .getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val walletBalance = settingsManager.walletBalance
        .map { it?.toDoubleOrNull() ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    var isLoadingBalance by mutableStateOf(false)
        private set

    var institutions by mutableStateOf<List<BankResponse>>(emptyList())
        private set

    var webhooks by mutableStateOf<List<WebhookRequest>>(emptyList())
        private set

    var pendingDeposits by mutableStateOf<List<DepositResponse>>(emptyList())
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
            } finally { isLoadingBalance = false }
        }
    }

    fun refreshTransactions() {
        viewModelScope.launch {
            TransactionRepository(settingsManager.createSwiftPayService(), transactionStore).syncWithApi()
        }
    }

    fun onBootstrapQrphRequest(amount: Double) {
        viewModelScope.launch {
            uiState = MiniAppUiState.Processing("Generating QR Ph...")
            getRepository().bootstrapQrph(amount, "QRPH${System.currentTimeMillis()}").onSuccess {
                it.qrCode?.let { qr -> uiState = MiniAppUiState.DynamicQrReady(qr, amount) }
            }.onFailure { uiState = MiniAppUiState.Error(it.message ?: "Failed") }
        }
    }

    fun onPaymentLinkRequest(data: PaymentData) {
        viewModelScope.launch {
            uiState = MiniAppUiState.Processing("Generating Link...")
            getRepository().createPaymentLink(data.amount, data.description).onSuccess {
                uiState = MiniAppUiState.PaymentLinkReady(it.paymentLinkUrl ?: "")
            }.onFailure { uiState = MiniAppUiState.Error(it.message ?: "Failed") }
        }
    }

    fun onGenerateDynamicQr(amount: Double) {
        viewModelScope.launch {
            uiState = MiniAppUiState.Processing("Generating QR...")
            getRepository().createDynamicQr(amount).onSuccess {
                uiState = MiniAppUiState.DynamicQrReady(it.qrCodeBody ?: "", amount)
            }.onFailure { uiState = MiniAppUiState.Error(it.message ?: "Failed") }
        }
    }

    fun onDisburseRequest(amount: Double, accountNo: String, firstName: String, lastName: String, bankCode: String?, remarks: String? = null) {
        viewModelScope.launch {
            uiState = MiniAppUiState.Processing("Processing Payout...")
            getRepository().disburse(amount, accountNo, firstName, lastName, bankCode = bankCode, remarks = remarks).onSuccess {
                uiState = MiniAppUiState.Idle
                refreshBalance()
            }.onFailure { uiState = MiniAppUiState.Error(it.message ?: "Payout Failed") }
        }
    }

    fun onBanksRequest() {
        viewModelScope.launch {
            getRepository().getInstitutions().onSuccess { institutions = it }
        }
    }

    fun onWebhooksRequest() {
        viewModelScope.launch {
            getRepository().getWebhooks().onSuccess { webhooks = it }
        }
    }

    fun onAddWebhookRequest(name: String, url: String) {
        viewModelScope.launch {
            uiState = MiniAppUiState.Processing("Adding Webhook...")
            getRepository().registerWebhook(name, url).onSuccess {
                uiState = MiniAppUiState.Idle
                onWebhooksRequest()
            }.onFailure { uiState = MiniAppUiState.Error(it.message ?: "Failed") }
        }
    }

    fun onDeleteWebhookRequest(id: String) {
        viewModelScope.launch {
            getRepository().deleteWebhook(id).onSuccess { onWebhooksRequest() }
        }
    }

    fun onCreateInvoiceRequest(amount: Double, description: String) {
        viewModelScope.launch {
            uiState = MiniAppUiState.Processing("Creating Invoice...")
            getRepository().createInvoice(amount, description).onSuccess { uiState = MiniAppUiState.Idle }
            .onFailure { uiState = MiniAppUiState.Error(it.message ?: "Failed") }
        }
    }

    fun onGenerateVcaRequest(accountName: String) {
        viewModelScope.launch {
            uiState = MiniAppUiState.Processing("Generating VCA...")
            getRepository().generateVca(accountName).onSuccess { uiState = MiniAppUiState.Idle }
            .onFailure { uiState = MiniAppUiState.Error(it.message ?: "Failed") }
        }
    }

    fun onFetchPendingDeposits() {
        viewModelScope.launch {
            getRepository().getAdminDeposits().onSuccess { pendingDeposits = it }
        }
    }

    fun onUpdateDepositStatus(id: String, status: String) {
        viewModelScope.launch {
            getRepository().updateDepositStatus(id, status).onSuccess { onFetchPendingDeposits() }
        }
    }

    fun onSubmitDeposit(amount: Double, ref: String, bank: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            uiState = MiniAppUiState.Processing("Submitting...")
            getRepository().submitDeposit(amount, ref, bank).onSuccess {
                uiState = MiniAppUiState.Idle
                onSuccess()
            }.onFailure { uiState = MiniAppUiState.Error(it.message ?: "Failed") }
        }
    }

    fun onAddMemberRequest(name: String, email: String, role: String) {
        Log.d("MiniAppViewModel", "Add Member: $name")
    }

    fun handleDeepLink(linkId: String, status: String?) {
        viewModelScope.launch {
            TransactionRepository(settingsManager.createSwiftPayService(), transactionStore).saveTransaction(
                InternalTransaction(linkId, 0.0, status ?: "SUCCESS", TransactionStore.nowLabel())
            )
            refreshTransactions()
        }
    }

    fun dismissError() { uiState = MiniAppUiState.Idle }
    fun dismissConsent() { uiState = MiniAppUiState.Idle }
    fun retryNfc() { (uiState as? MiniAppUiState.WaitingForNFC)?.let { onScanNFCCard(it.amount) } }
    
    fun onScanNFCCard(amount: Double) {
        viewModelScope.launch {
            var seconds = NFC_SESSION_SECONDS
            while (seconds >= 0) {
                uiState = MiniAppUiState.WaitingForNFC(amount, seconds)
                if (seconds == 0) { uiState = MiniAppUiState.Error("Timeout"); break }
                delay(1000); seconds--
            }
        }
    }
}
