package com.example.myapplication.ui.screens

import android.app.Application
import android.nfc.NfcAdapter
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
import com.example.myapplication.data.mergeTransactions
import com.example.myapplication.data.api.InternalTransaction
import com.example.myapplication.data.api.VaultPaymentResponse
import com.example.myapplication.util.normalizePaymentStatus
import com.example.myapplication.data.repository.TransactionRepository
import com.example.myapplication.util.findActivity
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
        val merchantName: String = "Fast Pay Merchant",
        val merchantAddress: String = "Pila, Laguna, PHL",
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
    private val memberStore = com.example.myapplication.data.MemberStore(application)
    private val repository: TransactionRepository by lazy {
        TransactionRepository(SwiftPayService(), transactionStore)
    }

    // ... existing ...

    fun onWebhooksRequest() {
        viewModelScope.launch {
            val result = getService().getWebhooks()
            result.onSuccess { bridge?.sendResponse(it) }
                .onFailure { bridge?.sendError(it.message ?: "Failed to fetch webhooks") }
        }
    }

    fun onAddWebhookRequest(name: String, url: String) {
        viewModelScope.launch {
            val result = getService().registerWebhook(name, url)
            result.onSuccess { bridge?.sendResponse(mapOf("status" to "added", "id" to (it.id ?: ""))) }
                .onFailure { bridge?.sendError(it.message ?: "Failed to add webhook") }
        }
    }

    fun onDeleteWebhookRequest(id: String) {
        viewModelScope.launch {
            val result = getService().deleteWebhook(id)
            result.onSuccess { bridge?.sendResponse(mapOf("status" to "deleted")) }
                .onFailure { bridge?.sendError(it.message ?: "Failed to delete webhook") }
        }
    }

    fun onCreateInvoiceRequest(amount: Double, description: String) {
        viewModelScope.launch {
            val result = getService().createInvoice(amount, description)
            result.onSuccess { bridge?.sendResponse(it) }
                .onFailure { bridge?.sendError(it.message ?: "Failed to create invoice") }
        }
    }

    fun onMembersRequest() {
        viewModelScope.launch {
            memberStore.members.collect { members ->
                bridge?.sendResponse(members)
            }
        }
    }

    fun onAddMemberRequest(name: String, email: String, role: String) {
        viewModelScope.launch {
            val member = com.example.myapplication.data.Member(
                id = "MEM${System.currentTimeMillis()}",
                name = name,
                email = email,
                role = role
            )
            memberStore.addMember(member)
            bridge?.sendResponse(mapOf("status" to "added", "member" to member))
        }
    }

    fun onDeleteMemberRequest(id: String) {
        viewModelScope.launch {
            memberStore.removeMember(id)
            bridge?.sendResponse(mapOf("status" to "deleted"))
        }
    }

    // Flow to expose wallet balance as Double
    val walletBalance = settingsManager.walletBalance.map { it?.toDoubleOrNull() ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    var isLoadingBalance by mutableStateOf(false)
        private set

    fun refreshBalance() {
        viewModelScope.launch {
            if (System.currentTimeMillis() - lastLocalCreditTime < 30000) {
                Log.d("MiniAppViewModel", "Skipping background balance refresh - recently credited locally.")
                return@launch
            }

            isLoadingBalance = true
            try {
                val freshBalance = getService().getWalletBalance()
                
                val localTransactions = repository.getAllTransactions().first()
                val totalCollected = localTransactions
                    .filter { normalizePaymentStatus(it.status) == "SUCCESS" && it.amount > 0 }
                    .sumOf { it.amount }
                
                val currentLocal = walletBalance.value
                val floorBalance = Math.max(currentLocal, totalCollected)
                
                if (freshBalance > floorBalance) {
                    Log.i("MiniAppViewModel", "Updating balance from server: $floorBalance -> $freshBalance")
                    settingsManager.saveWalletBalance(freshBalance.toString())
                } else if (floorBalance > currentLocal) {
                    Log.i("MiniAppViewModel", "Updating balance from local collection: $currentLocal -> $floorBalance")
                    settingsManager.saveWalletBalance(floorBalance.toString())
                }
            } catch (e: Exception) {
                Log.e("MiniAppViewModel", "Failed to refresh balance", e)
            } finally {
                isLoadingBalance = false
            }
        }
    }

    var uiState by mutableStateOf<MiniAppUiState>(MiniAppUiState.Idle)
        private set

    // Transaction Status State
    var transactionStatusMap by mutableStateOf<Map<String, VaultPaymentResponse>>(emptyMap())
        private set

    val latestTransactionStatus: Map.Entry<String, VaultPaymentResponse>?
        get() = transactionStatusMap.entries.lastOrNull { 
            val status = it.value.status?.uppercase()
            status != "PAYMENT_EXPIRED" && status != "EXPIRED"
        }

    var bridge: SwiftPaySDKBridge? = null
    private var nfcTimerJob: Job? = null
    private var threeDSecureTimeoutJob: Job? = null
    private var pendingCheckoutId: String? = null
        set(value) {
            field = value
            if (value != null) {
                viewModelScope.launch {
                    checkPendingPaymentStatus()
                }
            }
        }

    private suspend fun getService(): SwiftPayService {
        return settingsManager.createSwiftPayService()
    }

    fun requestPaymentConsent(data: PaymentData) {
        if (data.amount <= 0) {
            uiState = MiniAppUiState.Error("Invalid payment amount. Amount must be greater than ₱0.00")
            return
        }
        if (data.amount > 1_000_000) {
            uiState = MiniAppUiState.Error("Payment amount exceeds maximum limit of ₱1,000,000")
            return
        }
        uiState = MiniAppUiState.PaymentConsent(data)
    }

    fun onBalanceRequest() {
        viewModelScope.launch {
            val currentLocalBalance = walletBalance.value
            bridge?.sendResponse(currentLocalBalance)

            if (System.currentTimeMillis() - lastLocalCreditTime < 30000) {
                return@launch
            }

            try {
                val freshBalance = getService().getWalletBalance()
                if (freshBalance > currentLocalBalance || (currentLocalBalance == 0.0 && freshBalance > 0)) {
                    settingsManager.saveWalletBalance(freshBalance.toString())
                    bridge?.sendResponse(freshBalance)
                }
            } catch (e: Exception) {
                Log.e("MiniAppViewModel", "Failed to refresh balance from server", e)
            }
        }
    }

    fun onTransactionsRequest() {
        viewModelScope.launch {
            val result = getService().getInternalTransactions()
            val localTransactions = repository.getAllTransactions().first()
            result.onSuccess { transactions ->
                bridge?.sendResponse(mergeTransactions(localTransactions, transactions))
            }.onFailure { error ->
                if (localTransactions.isNotEmpty()) {
                    bridge?.sendResponse(localTransactions)
                } else {
                    val errorMessage = error.message ?: "Failed to fetch transactions"
                    bridge?.sendError(errorMessage)
                }
            }
        }
    }

    fun onPaymentChannelsRequest() {
        viewModelScope.launch {
            val channels = getService().getPaymentChannels()
            bridge?.sendResponse(channels)
        }
    }

    fun onPaymentLinkRequest(data: PaymentData) {
        viewModelScope.launch {
            try {
                uiState = MiniAppUiState.Processing("正在生成支付链接...")
                val result = getService().createPaymentLink(
                    amount = if (data.amount > 0) data.amount else 0.0,
                    description = data.description
                )

                result.onSuccess { response ->
                    val url = response.paymentLinkUrl
                    if (url != null) {
                        recordLocalTransaction(
                            id = response.id ?: "LINK${System.currentTimeMillis()}",
                            amount = data.amount,
                            status = "PENDING"
                        )
                        uiState = MiniAppUiState.PaymentLinkReady(url)
                        bridge?.sendResponse(mapOf(
                            "paymentLinkUrl" to url,
                            "linkId" to (response.id ?: ""),
                            "amount" to data.amount
                        ))
                    } else {
                        handlePaymentLinkError("Payment link URL was not provided")
                    }
                }.onFailure { error ->
                    handlePaymentLinkError(error.message ?: "Failed to generate payment link")
                }
            } catch (e: Exception) {
                handlePaymentLinkError("Unexpected error: ${e.message}")
            }
        }
    }

    private fun handlePaymentLinkError(errorMessage: String) {
        bridge?.sendError(errorMessage)
        uiState = MiniAppUiState.Error(errorMessage)
    }

    fun onScanNFCCard(amount: Double) {
        if (amount <= 0.0) {
            uiState = MiniAppUiState.Error("启动触碰支付前，请输入有效金额。")
            return
        }
        val activity = bridge?.webView?.context?.findActivity()
        val nfcAdapter = activity?.let { NfcAdapter.getDefaultAdapter(it) }
        
        if (nfcAdapter == null) {
            uiState = MiniAppUiState.Error("NFC is not supported on this device.")
        } else if (!nfcAdapter.isEnabled) {
            uiState = MiniAppUiState.Error("NFC is disabled.")
        } else {
            viewModelScope.launch {
                uiState = MiniAppUiState.Processing("正在准备安全 NFC 会话...")
                delay(100)
                startNFCTimer(amount = amount)
            }
        }
    }

    fun onGenerateDynamicQr(amount: Double) {
        viewModelScope.launch {
            uiState = MiniAppUiState.Processing("正在生成动态 QR Ph...")
            val result = getService().createDynamicQr(amount)
            result.onSuccess { response ->
                val qr = response.qrCodeBody
                if (qr != null) {
                    recordLocalTransaction(id = response.paymentId ?: "QR${System.currentTimeMillis()}", amount = amount, status = "PENDING")
                    uiState = MiniAppUiState.DynamicQrReady(qr, amount)
                    bridge?.sendResponse(mapOf("qrCodeBody" to qr))
                } else {
                    val errorMsg = "QR code data was not provided"
                    bridge?.sendError(errorMsg)
                    uiState = MiniAppUiState.Error(errorMsg)
                }
            }.onFailure { error ->
                val errorMsg = error.message ?: "QR generation failed"
                bridge?.sendError(errorMsg)
                uiState = MiniAppUiState.Error(errorMsg)
            }
        }
    }

    private fun startNFCTimer(amount: Double) {
        nfcTimerJob?.cancel()
        nfcTimerJob = viewModelScope.launch {
            var seconds = NFC_SESSION_SECONDS
            while (seconds >= 0) {
                uiState = MiniAppUiState.WaitingForNFC(
                    amount = amount,
                    timeLeft = seconds,
                    sessionDurationSeconds = NFC_SESSION_SECONDS,
                    statusMessage = "读卡器已就绪。请将卡片稳定地贴在手机背面。"
                )
                if (seconds == 0) {
                    uiState = MiniAppUiState.Error("扫描超时")
                    bridge?.sendError("NFC timeout")
                    return@launch
                }
                delay(1000)
                seconds--
            }
        }
    }

    fun onNFCCardDetected(pan: String, expiry: String, label: String = "Card") {
        nfcTimerJob?.cancel()
        viewModelScope.launch {
            val amount = (uiState as? MiniAppUiState.WaitingForNFC)?.amount ?: 0.0
            uiState = MiniAppUiState.WaitingForCVV(
                amount = amount,
                pan = pan,
                expiry = expiry,
                label = label
            )
        }
    }

    fun onCvvEntered(cvv: String) {
        val state = uiState as? MiniAppUiState.WaitingForCVV ?: return
        viewModelScope.launch {
            uiState = MiniAppUiState.Processing("正在验证卡片...")
            val expiryParts = state.expiry.split("/")
            val month = (expiryParts.getOrNull(0) ?: "12").padStart(2, '0')
            val year = "20" + (expiryParts.getOrNull(1) ?: "28")

            val result = getService().processVaultPayment(
                amount = state.amount,
                cardDetails = com.example.myapplication.data.api.CardDetails(
                    number = state.pan,
                    expMonth = month,
                    expYear = year,
                    cvc = cvv
                ),
                externalRefNo = state.sessionRef
            )
            
            result.onSuccess { response ->
                val paymentId = response.id ?: response.paymentId
                val status = response.status?.uppercase() ?: "PENDING"
                if (response.verificationUrl != null) {
                    recordLocalTransaction(id = paymentId ?: "V${System.currentTimeMillis()}", amount = state.amount, status = status)
                    pendingCheckoutId = paymentId
                    uiState = MiniAppUiState.PaymentRedirect(response.verificationUrl)
                } else if (status == "SUCCESS") {
                    recordLocalTransaction(id = paymentId ?: "V${System.currentTimeMillis()}", amount = state.amount, status = "SUCCESS")
                    bridge?.sendResponse(mapOf("status" to "success", "paymentId" to paymentId))
                    uiState = MiniAppUiState.Idle
                }
            }.onFailure { error ->
                uiState = MiniAppUiState.Error(error.message ?: "Vault payment failed")
            }
        }
    }

    fun approvePayment(data: PaymentData) {
        viewModelScope.launch {
            uiState = MiniAppUiState.Processing("正在创建收银台...")
            val result = getService().createCheckout(data)
            result.onSuccess { response ->
                if (response.redirectUrl != null) {
                    pendingCheckoutId = response.checkoutId
                    recordLocalTransaction(id = response.checkoutId ?: "C${System.currentTimeMillis()}", amount = data.amount, status = "PENDING")
                    uiState = MiniAppUiState.PaymentRedirect(response.redirectUrl, data)
                }
            }.onFailure { error ->
                uiState = MiniAppUiState.Error(error.message ?: "Checkout failed")
            }
        }
    }

    private var lastLocalCreditTime: Long = 0

    private suspend fun recordLocalTransaction(id: String, amount: Double, status: String) {
        val normalizedStatus = normalizePaymentStatus(status)
        repository.saveTransaction(InternalTransaction(id, amount, normalizedStatus, TransactionStore.nowLabel()))

        if (normalizedStatus == "SUCCESS" && amount > 0.0) {
            val currentBal = settingsManager.walletBalance.first()?.toDoubleOrNull() ?: 0.0
            settingsManager.saveWalletBalance((currentBal + amount).toString())
            lastLocalCreditTime = System.currentTimeMillis()
        }
    }

    fun dismissConsent() {
        uiState = MiniAppUiState.Idle
    }

    fun dismissError() {
        uiState = MiniAppUiState.Idle
    }

    private suspend fun checkPendingPaymentStatus() {
        val checkoutId = pendingCheckoutId ?: return
        delay(5000)
        val result = getService().getPaymentStatus(checkoutId)
        result.onSuccess { payment ->
            val status = payment.status ?: "PENDING"
            if (normalizePaymentStatus(status) == "SUCCESS") {
                recordLocalTransaction(checkoutId, 0.0, "SUCCESS")
                bridge?.sendResponse(mapOf("status" to "success", "paymentId" to checkoutId))
                pendingCheckoutId = null
            }
        }
    }

    fun handleDeepLink(linkId: String?, providedStatus: String? = null) {
        if (linkId.isNullOrBlank()) return
        viewModelScope.launch {
            val status = providedStatus ?: getService().getPaymentStatus(linkId).getOrNull()?.status
            status?.let { recordLocalTransaction(linkId, 0.0, it) }
        }
    }
}
