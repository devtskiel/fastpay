package com.example.myapplication.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.DELETE

interface SwiftPayApi {

    // --- SwiftPay Collection API (v2.8) ---
    
    @POST
    suspend fun createOrder(
        @Url url: String,
        @Body request: OrderRequest
    ): Response<OrderResponse>

    @POST
    suspend fun bootstrapQrph(
        @Url url: String,
        @Body request: QrphBootstrapRequest
    ): Response<QrphBootstrapResponse>

    @GET
    suspend fun getInstitutions(
        @Url url: String
    ): Response<List<BankResponse>>

    @GET
    suspend fun getPaymentStatusV2(
        @Url url: String,
        @Header("X-Swiftpay-Payment-Token") paymentId: String
    ): Response<OrderResponse>

    // --- SwiftPay Disbursement API (v2.0) ---

    @POST
    suspend fun sendDisbursement(
        @Url url: String,
        @Header("Authorization") auth: String,
        @Body request: DisbursementRequest
    ): Response<Unit>

    @GET
    suspend fun getDisbursement(
        @Url url: String,
        @Header("Authorization") auth: String
    ): Response<DisbursementResponse>

    @GET
    suspend fun listDisbursements(
        @Url url: String,
        @Header("Authorization") auth: String
    ): Response<List<DisbursementResponse>>

    // --- Standard Operations ---

    @POST("v1/collect/checkout")
    suspend fun createCheckout(
        @Header("Authorization") auth: String,
        @Body request: CheckoutRequest
    ): Response<CheckoutResponse>

    @GET("v1/collect/checkout/{id}")
    suspend fun getCheckoutStatus(
        @Header("Authorization") auth: String,
        @Path("id") checkoutId: String
    ): Response<CheckoutStatusResponse>

    @POST("v1/collect/invoice")
    suspend fun createInvoice(
        @Header("Authorization") auth: String,
        @Body request: InvoiceRequest
    ): Response<InvoiceResponse>

    @POST("v1/collect/payment-links")
    suspend fun createPaymentLink(
        @Header("Authorization") auth: String,
        @Body request: PaymentLinkRequest
    ): Response<PaymentLinkResponse>

    @GET("v1/collect/payments/{paymentId}")
    suspend fun getPaymentStatus(
        @Header("Authorization") auth: String,
        @Path("paymentId") paymentId: String
    ): Response<VaultPaymentResponse>

    @POST("v1/collect/payment-tokens")
    suspend fun createPaymentToken(
        @Header("Authorization") auth: String,
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

    @POST("v1/collect/qr/payments")
    suspend fun createDynamicQr(
        @Header("Authorization") auth: String,
        @Body request: DynamicQrRequest
    ): Response<DynamicQrResponse>

    // --- Disbursement Endpoints (Legacy) ---
    @GET("v1/disburse/balance")
    suspend fun getDisbursementBalance(
        @Header("Authorization") auth: String
    ): Response<BalanceResponse>

    @GET("v1/disburse/banks")
    suspend fun getBanks(
        @Header("Authorization") auth: String
    ): Response<BankListResponse>

    @GET("v1/disburse/transactions")
    suspend fun getDisbursementTransactions(
        @Header("Authorization") auth: String
    ): Response<List<DisbursementResponse>>

    // --- Identity / OTP Endpoints ---
    @POST
    suspend fun requestEmailOtp(
        @Url url: String,
        @Header("Authorization") auth: String,
        @Header("Request-Reference-No") refNo: String,
        @Body request: OtpRequest
    ): Response<Unit>

    @POST
    suspend fun verifyEmailOtp(
        @Url url: String,
        @Header("Authorization") auth: String,
        @Header("Request-Reference-No") refNo: String,
        @Body request: OtpVerifyRequest
    ): Response<Unit>

    // --- Infrastructure ---

    @GET("v1/account/balance")
    suspend fun getWalletBalance(
        @Header("Authorization") auth: String,
        @Header("Request-Reference-No") refNo: String? = null
    ): Response<BalanceResponse>

    @GET("v1/collect/payments")
    suspend fun getPaymentsList(
        @Header("Authorization") auth: String
    ): Response<SwiftPayTransactionResponse>

    @POST("v1/collect/webhooks")
    suspend fun registerWebhook(
        @Header("Authorization") auth: String,
        @Body request: WebhookRequest
    ): Response<WebhookRequest>

    @GET("v1/collect/webhooks")
    suspend fun getWebhooks(
        @Header("Authorization") auth: String
    ): Response<List<WebhookRequest>>

    @DELETE("v1/collect/webhooks/{id}")
    suspend fun deleteWebhook(
        @Header("Authorization") auth: String,
        @Path("id") id: String
    ): Response<Unit>

    @POST("v1/vca/generate")
    suspend fun generateVca(
        @Header("Authorization") auth: String,
        @Body request: VcaRequest
    ): Response<VcaResponse>

    @GET("v1/vca/transactions")
    suspend fun getVcaTransactions(
        @Header("Authorization") auth: String
    ): Response<SwiftPayTransactionResponse>
}
