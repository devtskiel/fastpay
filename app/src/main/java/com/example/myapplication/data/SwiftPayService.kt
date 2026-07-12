package com.example.myapplication.data

import android.util.Base64
import android.util.Log
import com.example.myapplication.BuildConfig
import com.example.myapplication.data.model.PaymentData
import com.example.myapplication.data.api.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class SwiftPayService(
    customSecretKey: String? = null,
    customPublicKey: String? = null,
    customMid: String? = null,
    customTerminalId: String? = null,
    customCardMid: String? = null,
    forcedSandbox: Boolean? = null,
    private val jwtToken: String? = null
) {
    
    private val secretKey = customSecretKey?.takeUnless { it.isBlank() } ?: BuildConfig.SWIFTPAY_SECRET_KEY
    private val publicKey = customPublicKey?.takeUnless { it.isBlank() } ?: BuildConfig.SWIFTPAY_PUBLIC_KEY
    
    private val isSandbox = forcedSandbox ?: publicKey?.startsWith("pk_test") ?: false
    
    private val hasSecretKey = !secretKey.isNullOrBlank() && secretKey != SwiftPayCredentials.MISSING_KEY
    private val hasPublicKey = !publicKey.isNullOrBlank() && publicKey != SwiftPayCredentials.MISSING_KEY
    
    private val pgBaseUrl = if (isSandbox) "https://api-sandbox.netbank.ph/" else "https://api.netbank.ph/"
    private val payBaseUrl = if (isSandbox) "https://api.pay.sandbox.live.swiftpay.ph/api/" else "https://api.pay.live.swiftpay.ph/api/"
    private val backendUrl = if (BuildConfig.APP_SERVER_URL.isNotBlank()) {
        val url = BuildConfig.APP_SERVER_URL
        if (url.endsWith("/")) url else "$url/"
    } else "http://10.0.2.2:3000/api/"

    private var activeMid: String? = customMid ?: BuildConfig.SWIFTPAY_QR_MID

    private var cardMid: String? = customCardMid ?: customMid ?: BuildConfig.SWIFTPAY_CARD_MID
    private var terminalId: String? = customTerminalId ?: BuildConfig.SWIFTPAY_TERMINAL_ID

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("Accept", "application/json")
                .header("User-Agent", "SwiftPayAndroid/1.0")
            
            if (original.header("x-swiftpay-mid") == null) {
                activeMid?.let { request.header("x-swiftpay-mid", it) }
            }
            if (original.header("Terminal-Id") == null) {
                terminalId?.let { request.header("Terminal-Id", it) }
            }
            chain.proceed(request.build())
        }
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(pgBaseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val api = retrofit.create(SwiftPayApi::class.java)

    private val authHeader: String
        get() = "Basic " + Base64.encodeToString("${secretKey?.trim().orEmpty()}:".toByteArray(), Base64.NO_WRAP)

    private val publicKeyAuth: String
        get() = "Basic " + Base64.encodeToString("${publicKey?.trim().orEmpty()}:".toByteArray(), Base64.NO_WRAP)

    private val v2AuthHeader: String
        get() = "Basic " + Base64.encodeToString("${publicKey?.trim().orEmpty()}:${secretKey?.trim().orEmpty()}".toByteArray(), Base64.NO_WRAP)

    private fun <T> parseError(response: retrofit2.Response<T>): String {
        return try {
            val errorBody = response.errorBody()?.string() ?: return "Unknown error (${response.code()})"
            val error = try { json.decodeFromString<SwiftPayError>(errorBody) } catch (e: Exception) { null }
            error?.message ?: error?.code ?: "Error ${response.code()}"
        } catch (e: Exception) { "Error ${response.code()}" }
    }

    private fun missingSecretKey() = Result.failure<Nothing>(Exception("Configuration Error: SwiftPay Secret Key is missing."))
    private fun missingPublicKey() = Result.failure<Nothing>(Exception("Configuration Error: SwiftPay Public Key is missing."))

    // --- Collection Methods (v2.8) ---

    suspend fun createOrder(
        amount: Double,
        referenceNo: String,
        customerName: String? = null,
        email: String? = null
    ): Result<OrderResponse> {
        return try {
            if (!hasPublicKey || !hasSecretKey) return missingSecretKey()
            val amountStr = "%.2f".format(amount)
            val params = mapOf("x_access_key" to publicKey.orEmpty(), "x_reference_no" to referenceNo, "x_amount" to amountStr)
            val signature = com.example.myapplication.util.SwiftPaySignatureHelper.calculateSignature(params, secretKey.orEmpty())
            val request = OrderRequest(
                accessKey = publicKey.orEmpty(), referenceNo = referenceNo, amount = amountStr,
                details = OrderDetails(customerName = customerName, customerAddress = OrderAddress(email = email)),
                signature = signature
            )
            val response = api.createOrder(payBaseUrl + "orders", request)
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!) else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun bootstrapQrph(amount: Double, referenceNo: String): Result<QrphBootstrapResponse> {
        return try {
            if (!hasPublicKey || !hasSecretKey) return missingSecretKey()
            val amountStr = "%.2f".format(amount)
            val params = mapOf("x_access_key" to publicKey.orEmpty(), "x_reference_no" to referenceNo, "x_amount" to amountStr, "x_currency" to "PHP")
            val signature = com.example.myapplication.util.SwiftPaySignatureHelper.calculateSignature(params, secretKey.orEmpty())
            val request = QrphBootstrapRequest(accessKey = publicKey.orEmpty(), referenceNo = referenceNo, amount = amountStr, signature = signature)
            val response = api.bootstrapQrph(payBaseUrl + "bootstrap/qrph?type=P2M", request)
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!) else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun createPaymentLink(amount: Double = 0.0, description: String = ""): Result<PaymentLinkResponse> {
        return try {
            if (!hasPublicKey) return missingPublicKey()
            val request = PaymentLinkRequest(
                description = description.ifBlank { "SwiftPay Payment" },
                totalAmount = if (amount > 0) TotalAmount(value = amount, currency = "PHP") else null,
                requestReferenceNumber = "PLINK${System.currentTimeMillis()}"
            )
            val response = api.createPaymentLink(publicKeyAuth, request)
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!) else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun createDynamicQr(amount: Double): Result<DynamicQrResponse> {
        return try {
            if (!hasPublicKey) return missingPublicKey()
            val qrRequest = DynamicQrRequest(
                totalAmount = TotalAmount(value = amount, currency = "PHP"),
                requestReferenceNumber = "QR${System.currentTimeMillis()}",
                type = "DYNAMIC"
            )
            val response = api.createDynamicQr(publicKeyAuth, qrRequest)
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!) else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) { Result.failure(e) }
    }

    // --- Payout Methods (v2.0) ---

    suspend fun disburse(
        amount: Double,
        accountNumber: String,
        firstName: String,
        lastName: String,
        middleName: String? = null,
        bankCode: String? = null,
        remarks: String? = null,
        email: String? = null,
        mobileNumber: String? = null,
        address: AddressV2? = null
    ): Result<Boolean> {
        return try {
            if (!hasSecretKey || !hasPublicKey) return missingSecretKey()
            val request = DisbursementRequest(
                merchantReferenceNo = "P${System.currentTimeMillis()}",
                institutionCode = bankCode ?: "",
                creditInformation = CreditInformation(
                    amount = "%.2f".format(amount),
                    remarks = remarks ?: "Payout"
                ),
                recipientInformation = RecipientInformation(
                    accountNumber = accountNumber,
                    firstName = firstName,
                    middleName = middleName,
                    lastName = lastName,
                    email = email,
                    mobileNumber = mobileNumber,
                    address = address
                )
            )
            val response = api.sendDisbursement(payBaseUrl + "disbursements/send", v2AuthHeader, request)
            if (response.isSuccessful) Result.success(true) else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getDisbursementStatus(disbursementId: String): Result<DisbursementResponse> {
        return try {
            if (!hasSecretKey || !hasPublicKey) return missingSecretKey()
            val response = api.getDisbursement(payBaseUrl + "disbursements/$disbursementId", v2AuthHeader)
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!) else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun listDisbursements(
        status: String? = null,
        refNo: String? = null
    ): Result<List<DisbursementResponse>> {
        return try {
            if (!hasSecretKey || !hasPublicKey) return missingSecretKey()
            val response = api.listDisbursements(payBaseUrl + "disbursements", v2AuthHeader, status = status, merchantReferenceNo = refNo)
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!) else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getInstitutions(): Result<List<BankResponse>> {
        return try {
            val response = api.getInstitutions(payBaseUrl + "institutions")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getBanks(): Result<List<BankResponse>> {
        return try {
            if (!hasSecretKey) return missingSecretKey()
            val response = api.getBanks(authHeader)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    // --- Vault & Standard Operations ---

    suspend fun createCheckout(paymentData: PaymentData): Result<CheckoutResponse> {
        return try {
            if (!hasPublicKey) return missingPublicKey()
            val request = CheckoutRequest(
                totalAmount = TotalAmount(value = paymentData.amount, currency = "PHP"),
                requestReferenceNumber = "REF${System.currentTimeMillis()}"
            )
            val response = api.createCheckout(publicKeyAuth, request)
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!) else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getCheckoutStatus(checkoutId: String): Result<CheckoutStatusResponse> {
        return try {
            if (!hasSecretKey) return missingSecretKey()
            val response = api.getCheckoutStatus(authHeader, checkoutId)
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!) else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getPaymentStatus(paymentId: String): Result<VaultPaymentResponse> {
        return try {
            if (!hasSecretKey) return missingSecretKey()
            val response = api.getPaymentStatus(authHeader, paymentId)
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!) else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getPaymentStatusV2(paymentId: String): Result<OrderResponse> {
        return try {
            val response = api.getPaymentStatusV2(payBaseUrl + "payments/status", paymentId)
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!) else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun queryPaymentStatus(referenceNo: String): Result<OrderResponse> {
        return try {
            if (!hasPublicKey) return missingPublicKey()
            val response = api.queryPaymentStatus(payBaseUrl + "payments/status/query", publicKey, referenceNo)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                Result.success(response.body()!!.first())
            } else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun processVaultPayment(
        amount: Double,
        cardDetails: CardDetails,
        externalRefNo: String? = null
    ): Result<VaultPaymentResponse> {
        return try {
            if (!hasSecretKey || !hasPublicKey) return missingPublicKey()
            val tokenResponse = api.createPaymentToken(publicKeyAuth, PaymentTokenRequest(card = cardDetails))
            if (!tokenResponse.isSuccessful || tokenResponse.body()?.paymentTokenId == null) return Result.failure(Exception(parseError(tokenResponse)))
            val tokenId = tokenResponse.body()!!.paymentTokenId!!
            val request = VaultPaymentRequest(
                totalAmount = TotalAmount(value = amount, currency = "PHP"),
                paymentTokenId = tokenId,
                requestReferenceNumber = externalRefNo ?: "VAULT${System.currentTimeMillis()}"
            )
            val response = api.createVaultPayment(authHeader, cardMid, terminalId, request.requestReferenceNumber, request)
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!) else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) { Result.failure(e) }
    }

    // --- Infrastructure & Utilities ---

    suspend fun getWalletBalance(): Double {
        if (!hasSecretKey) return 0.0
        return try {
            val response = api.getWalletBalance(authHeader, "BAL${System.currentTimeMillis()}")
            if (response.isSuccessful) response.body()?.balance ?: 0.0 else 0.0
        } catch (e: Exception) { 0.0 }
    }

    suspend fun getInternalTransactions(): Result<List<InternalTransaction>> {
        if (!hasSecretKey) return missingSecretKey()
        return try {
            val response = api.getPaymentsList(authHeader)
            if (response.isSuccessful && response.body() != null) {
                val apiTransactions = response.body()!!.data ?: response.body()!!.payments ?: emptyList()
                val transactions = apiTransactions.map {
                    InternalTransaction(it.id ?: "TRANS", it.amount?.toDoubleOrNull() ?: 0.0, it.status ?: "UNKNOWN", it.timestamp ?: "")
                }
                Result.success(transactions)
            } else Result.success(emptyList())
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getWebhooks(): Result<List<WebhookRequest>> {
        if (!hasSecretKey) return missingSecretKey()
        return try {
            val response = api.getWebhooks(authHeader)
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!) else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun registerWebhook(name: String, url: String): Result<WebhookRequest> {
        if (!hasSecretKey) return missingSecretKey()
        return try {
            val response = api.registerWebhook(authHeader, WebhookRequest(name, url))
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!) else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteWebhook(id: String): Result<Boolean> {
        if (!hasSecretKey) return missingSecretKey()
        return try {
            val response = api.deleteWebhook(authHeader, id)
            if (response.isSuccessful) Result.success(true) else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun createInvoice(amount: Double, description: String): Result<InvoiceResponse> {
        if (!hasSecretKey) return missingSecretKey()
        return try {
            val request = InvoiceRequest(invoiceNumber = "INV${System.currentTimeMillis()}", totalAmount = TotalAmount(value = amount, currency = "PHP"))
            val response = api.createInvoice(authHeader, request)
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!) else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun generateVca(accountName: String): Result<VcaResponse> {
        if (!hasSecretKey) return missingSecretKey()
        return try {
            val request = VcaRequest(accountName = accountName, merchantReferenceNumber = "VCA${System.currentTimeMillis()}")
            val response = api.generateVca(authHeader, request)
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!) else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getVcaTransactions(): Result<List<InternalTransaction>> {
        if (!hasSecretKey) return missingSecretKey()
        return try {
            val response = api.getVcaTransactions(authHeader)
            if (response.isSuccessful && response.body() != null) {
                val apiTransactions = response.body()!!.data ?: response.body()!!.payments ?: emptyList()
                val transactions = apiTransactions.map {
                    InternalTransaction(it.id ?: "VCA", it.amount?.toDoubleOrNull() ?: 0.0, it.status ?: "SUCCESS", it.timestamp ?: "")
                }
                Result.success(transactions)
            } else Result.success(emptyList())
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun requestEmailOtp(email: String, refNo: String): Result<Unit> {
        return try {
            val response = api.requestEmailOtp(pgBaseUrl + "v1/identity/otp", authHeader, refNo, OtpRequest(email))
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun verifyEmailOtp(email: String, code: String, refNo: String): Result<Unit> {
        return try {
            val response = api.verifyEmailOtp(pgBaseUrl + "v1/identity/otp/verify", authHeader, refNo, OtpVerifyRequest(email, code))
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) { Result.failure(e) }
    }

    fun getPaymentChannels(): List<PaymentChannel> {
        return listOf(
            PaymentChannel("SwiftPay Wallet", "ACTIVE"),
            PaymentChannel("Direct Bank Transfer", "ACTIVE"),
            PaymentChannel("InstaPay", "ACTIVE"),
            PaymentChannel("PESONet", "ACTIVE"),
            PaymentChannel("QRPH", "ACTIVE")
        )
    }

    fun getTransactionUpdates() = flow {
        while (true) {
            try { getInternalTransactions().getOrNull()?.let { emit(it) } } catch (_: Exception) {}
            delay(10000L)
        }
    }

    // --- Custom Backend / Deposit Methods ---

    suspend fun submitDeposit(amount: Double, referenceNumber: String, bankName: String): Result<Unit> {
        return try {
            val response = api.submitDeposit(
                backendUrl + "swiftpay/deposit",
                "Bearer $jwtToken",
                DepositRequest(amount, referenceNumber, bankName)
            )
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getAdminDeposits(): Result<List<DepositResponse>> {
        return try {
            val response = api.getAdminDeposits(backendUrl + "admin/deposits", "Bearer $jwtToken")
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!) else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updateDepositStatus(id: String, status: String): Result<Unit> {
        return try {
            val response = api.updateDepositStatus(
                backendUrl + "admin/deposits/$id/status",
                "Bearer $jwtToken",
                DepositStatusUpdateRequest(status)
            )
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun login(email: String, pass: String): Result<LoginResponse> {
        return try {
            val response = api.login(backendUrl + "auth/login", LoginRequest(email, pass))
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!) else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun registerMerchant(request: MerchantRegistrationRequest): Result<MerchantRegistrationResponse> {
        return try {
            val response = api.registerMerchant(backendUrl + "auth/register", request)
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!) else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun forgotPassword(email: String): Result<Unit> {
        return try {
            val response = api.forgotPassword(backendUrl + "auth/forgot-password", mapOf("email" to email))
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) { Result.failure(e) }
    }
}

