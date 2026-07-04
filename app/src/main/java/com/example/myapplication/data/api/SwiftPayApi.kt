package com.example.myapplication.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface SwiftPayApi {

    // --- Discovery / Fallback Endpoints ---
    @GET
    suspend fun getTransactionsFullUrl(
        @retrofit2.http.Url url: String,
        @Header("Authorization") auth: String,
        @retrofit2.http.Query("mid") mid: String? = null,
        @retrofit2.http.Query("size") size: Int = 20
    ): Response<SwiftPayTransactionResponse>

    @POST
    suspend fun getBalanceFullUrl(
        @retrofit2.http.Url url: String,
        @Header("Authorization") auth: String,
        @Header("Request-Reference-No") refNo: String,
        @Body request: BalanceInquiryRequest = BalanceInquiryRequest()
    ): Response<BalanceResponse>

    @GET
    suspend fun getBalanceGetFullUrl(
        @retrofit2.http.Url url: String,
        @Header("Authorization") auth: String
    ): Response<BalanceResponse>

    // --- Official Funding Account Balance ---
    @GET("v1/account/balance")
    suspend fun getWalletBalance(
        @Header("Authorization") auth: String,
        @Header("Request-Reference-No") refNo: String? = null
    ): Response<BalanceResponse>

    // --- Standard Operations ---
    @POST("v1/collect/checkout")
    suspend fun createCheckout(
        @Header("Authorization") auth: String,
        @Body request: CheckoutRequest
    ): Response<CheckoutResponse>

    @GET("v1/collect/checkout/{id}")
    suspend fun getCheckoutStatus(
        @Header("Authorization") auth: String,
        @retrofit2.http.Path("id") checkoutId: String
    ): Response<CheckoutStatusResponse>

    @POST("v1/collect/invoice")
    suspend fun createInvoice(
        @Header("Authorization") auth: String,
        @Body request: InvoiceRequest
    ): Response<InvoiceResponse>

    // --- Payment Links API ---
    @POST("v1/collect/payment-links")
    suspend fun createPaymentLink(
        @Header("Authorization") auth: String,
        @Body request: PaymentLinkRequest
    ): Response<PaymentLinkResponse>

    @POST
    suspend fun createPaymentLinkFullUrl(
        @retrofit2.http.Url url: String,
        @Header("Authorization") auth: String,
        @Body request: PaymentLinkRequest
    ): Response<PaymentLinkResponse>

    // --- Transaction Status ---
    @GET("v1/collect/payments/{paymentId}")
    suspend fun getPaymentStatus(
        @Header("Authorization") auth: String,
        @retrofit2.http.Path("paymentId") paymentId: String
    ): Response<VaultPaymentResponse>

    // --- Vault / Payment Token Endpoints ---
    @POST("v1/collect/payment-tokens")
    suspend fun createPaymentToken(
        @Header("Authorization") pkAuth: String,
        @Body request: PaymentTokenRequest
    ): Response<PaymentTokenResponse>

    @POST("v1/collect/payments")
    suspend fun createVaultPayment(
        @Header("Authorization") skAuth: String,
        @Header("x-swiftpay-mid") mid: String? = null,
        @Header("Terminal-Id") terminalId: String? = null,
        @Header("Request-Reference-No") refNo: String? = null,
        @Body request: VaultPaymentRequest
    ): Response<VaultPaymentResponse>

    @POST
    suspend fun createDynamicQrFullUrl(
        @retrofit2.http.Url url: String,
        @Header("Authorization") auth: String,
        @Header("x-swiftpay-mid") mid: String? = null,
        @Header("x-skip-mid") skipMid: String? = null,
        @Body request: DynamicQrRequest
    ): Response<DynamicQrResponse>

    @POST("v1/collect/qr/payments")
    suspend fun createDynamicQr(
        @Header("Authorization") auth: String,
        @Body request: DynamicQrRequest
    ): Response<DynamicQrResponse>

    @GET("v1/collect/customizations")
    suspend fun getCustomizations(
        @Header("Authorization") auth: String
    ): Response<CustomizationRequest>

    @POST("v1/collect/customizations")
    suspend fun setCustomizations(
        @Header("Authorization") auth: String,
        @Body request: CustomizationRequest
    ): Response<Unit>

    // --- Identity / OTP Endpoints ---
    @POST
    suspend fun requestEmailOtp(
        @retrofit2.http.Url url: String,
        @Header("Authorization") auth: String,
        @Header("Request-Reference-No") refNo: String,
        @Body request: OtpRequest
    ): Response<Unit>

    @POST
    suspend fun verifyEmailOtp(
        @retrofit2.http.Url url: String,
        @Header("Authorization") auth: String,
        @Header("Request-Reference-No") refNo: String,
        @Body request: OtpVerifyRequest
    ): Response<Unit>

    // --- Resend Email API ---
    @POST
    suspend fun sendEmail(
        @retrofit2.http.Url url: String,
        @Header("Authorization") auth: String,
        @Body request: ResendRequest
    ): Response<ResendResponse>

    // --- Disbursement Endpoints ---
    @GET("v1/disburse/balance")
    suspend fun getDisbursementBalance(
        @Header("Authorization") auth: String
    ): Response<BalanceResponse>

    @POST("v1/disburse/execute")
    suspend fun createDisbursement(
        @Header("Authorization") auth: String,
        @Body request: DisbursementRequest
    ): Response<DisbursementResponse>

    @GET("v1/disburse/transactions")
    suspend fun getDisbursementTransactions(
        @Header("Authorization") auth: String
    ): Response<List<DisbursementResponse>>

    // --- Webhook Management ---
    @POST("v1/collect/webhooks")
    suspend fun registerWebhook(
        @Header("Authorization") auth: String,
        @Body request: WebhookRequest
    ): Response<WebhookRequest>

    @GET("v1/collect/webhooks")
    suspend fun getWebhooks(
        @Header("Authorization") auth: String
    ): Response<List<WebhookRequest>>

    @retrofit2.http.DELETE("v1/collect/webhooks/{id}")
    suspend fun deleteWebhook(
        @Header("Authorization") auth: String,
        @retrofit2.http.Path("id") id: String
    ): Response<Unit>
}
