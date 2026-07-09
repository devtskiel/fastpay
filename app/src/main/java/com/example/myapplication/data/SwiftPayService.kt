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
    
    private val hasSecretKey = secretKey.isConfiguredKey()
    private val hasPublicKey = publicKey.isConfiguredKey()
    
    // Base URLs for SwiftPay (Netbank infrastructure)
    private val pgBaseUrl = if (isSandbox) "https://api-sandbox.netbank.ph/" else "https://api.netbank.ph/"
    
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

    private fun <T> parseError(response: retrofit2.Response<T>): String {
        return try {
            val errorBody = response.errorBody()?.string() ?: return "Unknown error (${response.code()})"
            Log.e("SwiftPayService-Error", "Raw error response [${response.code()}]: $errorBody")
            
            val error = try {
                json.decodeFromString<SwiftPayError>(errorBody)
            } catch (e: Exception) {
                return "Error ${response.code()}: $errorBody"
            }
            
            val details = error.errors?.joinToString("\n") { detail ->
                val fieldInfo = if (detail.field != null) "[${detail.field}] " else ""
                "$fieldInfo${detail.message ?: detail.code ?: ""}"
            } ?: ""
            
            val mainMessage = error.message ?: error.code ?: "Error ${response.code()}"
            if (details.isNotBlank()) "$mainMessage\n$details" else mainMessage
        } catch (e: Exception) {
            "Error ${response.code()}: ${response.message()}"
        }
    }

    private fun String?.isConfiguredKey(): Boolean = !isNullOrBlank() && this != SwiftPayCredentials.MISSING_KEY

    private fun missingSecretKey() = Result.failure<Nothing>(Exception("Configuration Error: SwiftPay Secret Key is missing."))

    private fun missingPublicKey() = Result.failure<Nothing>(Exception("Configuration Error: SwiftPay Public Key is missing."))

    suspend fun createCheckout(paymentData: PaymentData): Result<CheckoutResponse> {
        return try {
            if (!hasPublicKey) return missingPublicKey()

            val request = CheckoutRequest(
                totalAmount = TotalAmount(value = paymentData.amount, currency = "PHP"),
                redirectUrl = RedirectUrl(
                    success = BuildConfig.VAULT_SUCCESS_REDIRECT_URL,
                    failure = BuildConfig.VAULT_FAILURE_REDIRECT_URL,
                    cancel = BuildConfig.VAULT_CANCEL_REDIRECT_URL
                ),
                requestReferenceNumber = "REF${System.currentTimeMillis()}"
            )

            val response = api.createCheckout(publicKeyAuth, request)
            if (response.isSuccessful) {
                val checkoutResponse = response.body()
                if (checkoutResponse != null && checkoutResponse.redirectUrl != null) {
                    Result.success(checkoutResponse)
                } else {
                    Result.failure(Exception("Incomplete response from SwiftPay API"))
                }
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCheckoutStatus(checkoutId: String): Result<CheckoutStatusResponse> {
        return try {
            if (!hasSecretKey) return missingSecretKey()
            val response = api.getCheckoutStatus(authHeader, checkoutId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPaymentStatus(paymentId: String): Result<VaultPaymentResponse> {
        return try {
            if (!hasSecretKey) return missingSecretKey()
            val response = api.getPaymentStatus(authHeader, paymentId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPaymentLink(amount: Double = 0.0, description: String = ""): Result<PaymentLinkResponse> {
        return try {
            if (!hasPublicKey) return missingPublicKey()
            val refNo = "SPLINK${System.currentTimeMillis()}"
            val request = PaymentLinkRequest(
                description = description.ifBlank { "SwiftPay Payment" },
                totalAmount = if (amount > 0) TotalAmount(value = amount, currency = "PHP") else null,
                requestReferenceNumber = refNo,
                redirectUrl = RedirectUrl(
                    success = BuildConfig.VAULT_SUCCESS_REDIRECT_URL,
                    failure = BuildConfig.VAULT_FAILURE_REDIRECT_URL,
                    cancel = BuildConfig.VAULT_CANCEL_REDIRECT_URL
                )
            )
            val response = api.createPaymentLink(publicKeyAuth, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWalletBalance(): Double {
        if (!hasSecretKey) return 0.0
        return try {
            val response = api.getWalletBalance(authHeader, "BAL${System.currentTimeMillis()}")
            if (response.isSuccessful) {
                val b = response.body()
                b?.availableBalance ?: b?.balance ?: b?.totalBalance ?: 0.0
            } else 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    suspend fun getInternalTransactions(): Result<List<InternalTransaction>> {
        if (!hasSecretKey) return missingSecretKey()
        return try {
            val response = api.getPaymentsList(authHeader)
            if (response.isSuccessful && response.body() != null) {
                val apiTransactions = response.body()?.data ?: response.body()?.payments ?: emptyList()
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
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getPaymentChannels(): List<PaymentChannel> {
        return listOf(
            PaymentChannel("SwiftPay Wallet", "ACTIVE"),
            PaymentChannel("Direct Bank Transfer", "ACTIVE"),
            PaymentChannel("InstaPay", "ACTIVE"),
            PaymentChannel("PESONet", "ACTIVE"),
            PaymentChannel("QRPH", "ACTIVE"),
            PaymentChannel("Visa/Mastercard", "ACTIVE")
        )
    }

    suspend fun requestEmailOtp(email: String, refNo: String): Result<Unit> {
        return try {
            val response = api.requestEmailOtp(pgBaseUrl + "v1/identity/otp", authHeader, refNo, OtpRequest(email))
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyEmailOtp(email: String, code: String, refNo: String): Result<Unit> {
        return try {
            val response = api.verifyEmailOtp(pgBaseUrl + "v1/identity/otp/verify", authHeader, refNo, OtpVerifyRequest(email, code))
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun processVaultPayment(
        amount: Double,
        cardDetails: CardDetails,
        externalRefNo: String? = null
    ): Result<VaultPaymentResponse> {
        return try {
            if (!hasSecretKey || !hasPublicKey) return missingPublicKey()

            val tokenResponse = api.createPaymentToken(publicKeyAuth, PaymentTokenRequest(card = cardDetails))
            if (!tokenResponse.isSuccessful || tokenResponse.body() == null) {
                return Result.failure(Exception(parseError(tokenResponse)))
            }
            val tokenId = tokenResponse.body()?.paymentTokenId ?: return Result.failure(Exception("Failed to obtain payment token"))

            val paymentRefNo = externalRefNo ?: "VAULT${System.currentTimeMillis()}"
            val paymentRequest = VaultPaymentRequest(
                totalAmount = TotalAmount(value = amount, currency = "PHP"),
                paymentTokenId = tokenId,
                requestReferenceNumber = paymentRefNo,
                redirectUrl = RedirectUrl(
                    success = BuildConfig.VAULT_SUCCESS_REDIRECT_URL,
                    failure = BuildConfig.VAULT_FAILURE_REDIRECT_URL,
                    cancel = BuildConfig.VAULT_CANCEL_REDIRECT_URL
                )
            )
            val response = api.createVaultPayment(authHeader, cardMid, terminalId, paymentRefNo, paymentRequest)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun applyBranding(): Result<Boolean> {
        return try {
            if (!hasSecretKey) return missingSecretKey()
            val request = CustomizationRequest(
                customTitle = "Fast Pay Business",
                colorScheme = "#0052CC"
            )
            val response = api.setCustomizations(authHeader, request)
            if (response.isSuccessful) Result.success(true) else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBanks(): Result<List<BankResponse>> {
        return try {
            if (!hasSecretKey) return missingSecretKey()
            val response = api.getBanks(authHeader)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun disburse(
        amount: Double,
        accountNumber: String,
        firstName: String,
        lastName: String,
        bankCode: String? = null
    ): Result<DisbursementResponse> {
        return try {
            if (!hasSecretKey) return missingSecretKey()
            val request = DisbursementRequest(
                totalAmount = TotalAmount(value = amount, currency = "PHP"),
                recipient = Recipient(
                    firstName = firstName,
                    lastName = lastName,
                    accountNumber = accountNumber,
                    bankCode = bankCode
                ),
                requestReferenceNumber = "DISB${System.currentTimeMillis()}"
            )
            val response = api.createDisbursement(authHeader, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else Result.failure(Exception("Disbursement Error: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createInvoice(amount: Double, description: String): Result<InvoiceResponse> {
        return try {
            if (!hasSecretKey) return missingSecretKey()
            val request = InvoiceRequest(
                invoiceNumber = "INV${System.currentTimeMillis()}",
                totalAmount = TotalAmount(value = amount, currency = "PHP"),
                metadata = mapOf("description" to description)
            )
            val response = api.createInvoice(authHeader, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerWebhook(name: String, callbackUrl: String): Result<WebhookRequest> {
        return try {
            if (!hasSecretKey) return missingSecretKey()
            val response = api.registerWebhook(authHeader, WebhookRequest(name, callbackUrl))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWebhooks(): Result<List<WebhookRequest>> {
        return try {
            if (!hasSecretKey) return missingSecretKey()
            val response = api.getWebhooks(authHeader)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteWebhook(id: String): Result<Boolean> {
        return try {
            if (!hasSecretKey) return missingSecretKey()
            val response = api.deleteWebhook(authHeader, id)
            if (response.isSuccessful) Result.success(true) else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateVca(accountName: String): Result<VcaResponse> {
        return try {
            if (!hasSecretKey) return missingSecretKey()
            val request = VcaRequest(accountName = accountName, merchantReferenceNumber = "VCA${System.currentTimeMillis()}")
            val response = api.generateVca(authHeader, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getVcaTransactions(): Result<List<InternalTransaction>> {
        if (!hasSecretKey) return missingSecretKey()
        return try {
            val response = api.getVcaTransactions(authHeader)
            if (response.isSuccessful && response.body() != null) {
                val apiTransactions = response.body()?.data ?: response.body()?.payments ?: emptyList()
                val transactions = apiTransactions.map {
                    InternalTransaction(
                        transactionId = it.id ?: "VCA_TX_${System.currentTimeMillis()}",
                        amount = it.amount?.toDoubleOrNull() ?: 0.0,
                        status = it.status ?: "SUCCESS",
                        date = it.timestamp ?: ""
                    )
                }
                Result.success(transactions)
            } else Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getTransactionUpdates() = flow {
        while (true) {
            try {
                val transactions = getInternalTransactions().getOrNull() ?: emptyList()
                emit(transactions)
            } catch (e: Exception) {
                Log.e("SwiftPayService", "Update error", e)
            }
            delay(10000L)
        }
    }
}
