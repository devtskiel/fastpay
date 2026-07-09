package com.example.myapplication.data

import android.util.Base64
import android.util.Log
import com.example.myapplication.BuildConfig
import com.example.myapplication.bridge.PaymentData
import com.example.myapplication.data.api.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.net.UnknownHostException

class SwiftPayService(
    customSecretKey: String? = null,
    customPublicKey: String? = null,
    customMid: String? = null,
    customTerminalId: String? = null,
    customCardMid: String? = null
) {
    
    private val secretKey = customSecretKey?.takeUnless { it.isBlank() } ?: BuildConfig.SWIFTPAY_SECRET_KEY
    private val publicKey = customPublicKey?.takeUnless { it.isBlank() } ?: BuildConfig.SWIFTPAY_PUBLIC_KEY
    
    // Explicit environment selection
    private val isSandbox = false
    
    private val hasSecretKey = !secretKey.isNullOrBlank() && secretKey != SwiftPayCredentials.MISSING_KEY
    private val hasPublicKey = !publicKey.isNullOrBlank() && publicKey != SwiftPayCredentials.MISSING_KEY
    
    // Base URLs for SwiftPay (Netbank infrastructure)
    private val pgBaseUrl = if (isSandbox) "https://api-sandbox.netbank.ph/" else "https://api.netbank.ph/"
    
    // SwiftPay v2.8 (Direct Integration) Base URLs
    private val payBaseUrl = if (isSandbox) "https://api.pay.sandbox.live.swiftpay.ph/api/" else "https://api.pay.live.swiftpay.ph/api/"

    private var activeMid: String? = customMid ?: BuildConfig.SWIFTPAY_QR_MID
    private var cardMid: String? = customCardMid ?: customMid ?: BuildConfig.SWIFTPAY_CARD_MID
    private var terminalId: String? = customTerminalId ?: BuildConfig.SWIFTPAY_TERMINAL_ID

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = false
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("Accept", "application/json")
                .header("User-Agent", "FastPayAndroid/1.0")
            
            if (original.header("x-swiftpay-mid") == null) {
                activeMid?.let { request.header("x-swiftpay-mid", it) }
            }
            
            if (original.header("Terminal-Id") == null) {
                terminalId?.let { request.header("Terminal-Id", it) }
            }
            
            chain.proceed(request.build())
        }
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            },
        )
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(pgBaseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val api = retrofit.create(SwiftPayApi::class.java)

    private val authHeader: String
        get() = "Basic " + Base64.encodeToString("${secretKey.trim()}:".toByteArray(), Base64.NO_WRAP)

    private val publicKeyAuth: String
        get() = "Basic " + Base64.encodeToString("${publicKey.trim()}:".toByteArray(), Base64.NO_WRAP)

    private val v2AuthHeader: String
        get() = "Basic " + Base64.encodeToString("${publicKey.trim()}:${secretKey.trim()}".toByteArray(), Base64.NO_WRAP)

    private fun <T> parseError(response: retrofit2.Response<T>): String {
        return try {
            val errorBody = response.errorBody()?.string() ?: return "Unknown error (${response.code()})"
            val error = try { json.decodeFromString<SwiftPayError>(errorBody) } catch (e: Exception) { null }
            error?.message ?: error?.code ?: "Error ${response.code()}"
        } catch (e: Exception) {
            "Error ${response.code()}"
        }
    }

    private fun missingSecretKey() = Result.failure<Nothing>(Exception("Configuration Error: SwiftPay Secret Key is missing."))
    private fun missingPublicKey() = Result.failure<Nothing>(Exception("Configuration Error: SwiftPay Public Key is missing."))

    // --- Collection Methods ---

    suspend fun createOrder(
        amount: Double,
        referenceNo: String,
        customerName: String? = null,
        email: String? = null
    ): Result<OrderResponse> {
        return try {
            if (!hasPublicKey || !hasSecretKey) return missingSecretKey()
            val amountStr = "%.2f".format(amount)
            val params = mapOf("x_access_key" to publicKey, "x_reference_no" to referenceNo, "x_amount" to amountStr)
            val signature = com.example.myapplication.util.SwiftPaySignatureHelper.calculateSignature(params, secretKey)
            val request = OrderRequest(
                accessKey = publicKey, referenceNo = referenceNo, amount = amountStr,
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
            val params = mapOf("x_access_key" to publicKey, "x_reference_no" to referenceNo, "x_amount" to amountStr, "x_currency" to "PHP")
            val signature = com.example.myapplication.util.SwiftPaySignatureHelper.calculateSignature(params, secretKey)
            val request = QrphBootstrapRequest(accessKey = publicKey, referenceNo = referenceNo, amount = amountStr, signature = signature)
            val response = api.bootstrapQrph(payBaseUrl + "bootstrap/qrph?type=P2M", request)
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!) else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getWalletBalance(): Double {
        if (!hasSecretKey) return 0.0
        return try {
            val response = api.getWalletBalance(authHeader, "BAL${System.currentTimeMillis()}")
            if (response.isSuccessful) {
                val b = response.body()
                b?.balance ?: 0.0
            } else 0.0
        } catch (e: Exception) { 0.0 }
    }

    // --- Disbursement Methods ---

    suspend fun disburse(
        amount: Double,
        accountNumber: String,
        firstName: String,
        lastName: String,
        bankCode: String? = null
    ): Result<Boolean> {
        return try {
            if (!hasSecretKey || !hasPublicKey) return missingSecretKey()
            val request = DisbursementRequest(
                merchantReferenceNo = "P${System.currentTimeMillis()}",
                institutionCode = bankCode ?: "",
                creditInformation = CreditInformation(amount = "%.2f".format(amount), remarks = "Payout"),
                recipientInformation = RecipientInformation(accountNumber = accountNumber, firstName = firstName, lastName = lastName)
            )
            val response = api.sendDisbursement(payBaseUrl + "disbursements/send", v2AuthHeader, request)
            if (response.isSuccessful) Result.success(true) else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getInstitutions(): Result<List<BankResponse>> {
        return try {
            val response = api.getInstitutions(payBaseUrl + "institutions")
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!) else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) { Result.failure(e) }
    }

    // --- Transaction History ---

    suspend fun getInternalTransactions(): Result<List<InternalTransaction>> {
        if (!hasSecretKey) return missingSecretKey()
        return try {
            val response = api.getPaymentsList(authHeader)
            if (response.isSuccessful && response.body() != null) {
                val apiTransactions = response.body()?.data ?: response.body()?.payments ?: emptyList<SwiftPayTransaction>()
                val transactions = apiTransactions.map {
                    InternalTransaction(
                        transactionId = it.id ?: "TRANS_${System.currentTimeMillis()}",
                        amount = it.amount?.toDoubleOrNull() ?: 0.0,
                        status = it.status ?: "UNKNOWN",
                        date = it.timestamp ?: ""
                    )
                }
                Result.success(transactions)
            } else Result.success(emptyList())
        } catch (e: Exception) { Result.failure(e) }
    }

    // --- Legacy / Infrastructure Support ---

    suspend fun createCheckout(paymentData: PaymentData): Result<CheckoutResponse> {
        return try {
            if (!hasPublicKey) return missingPublicKey()
            val request = CheckoutRequest(
                totalAmount = TotalAmount(value = paymentData.amount, currency = "PHP"),
                redirectUrl = RedirectUrl(success = BuildConfig.VAULT_SUCCESS_REDIRECT_URL, failure = BuildConfig.VAULT_FAILURE_REDIRECT_URL, cancel = BuildConfig.VAULT_CANCEL_REDIRECT_URL),
                requestReferenceNumber = "REF${System.currentTimeMillis()}"
            )
            val response = api.createCheckout(publicKeyAuth, request)
            if (response.isSuccessful && response.body()?.redirectUrl != null) Result.success(response.body()!!) else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getPaymentStatus(paymentId: String): Result<VaultPaymentResponse> {
        return try {
            if (!hasSecretKey) return missingSecretKey()
            val response = api.getPaymentStatus(authHeader, paymentId)
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!) else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun generateVca(accountName: String): Result<VcaResponse> {
        return try {
            if (!hasSecretKey) return missingSecretKey()
            val request = VcaRequest(accountName = accountName, merchantReferenceNumber = "VCA${System.currentTimeMillis()}")
            val response = api.generateVca(authHeader, request)
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!) else Result.failure(Exception(parseError(response)))
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
}
