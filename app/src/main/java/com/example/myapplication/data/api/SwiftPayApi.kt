package com.example.myapplication.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url
import retrofit2.http.Path

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

    // --- Legacy / Netbank Infrastructure Operations ---

    @GET("v1/account/balance")
    suspend fun getWalletBalance(
        @Header("Authorization") auth: String,
        @Header("Request-Reference-No") refNo: String? = null
    ): Response<BalanceResponse>

    @POST("v1/collect/checkout")
    suspend fun createCheckout(
        @Header("Authorization") auth: String,
        @Body request: CheckoutRequest
    ): Response<CheckoutResponse>

    @GET("v1/collect/payments")
    suspend fun getPaymentsList(
        @Header("Authorization") auth: String
    ): Response<SwiftPayTransactionResponse>

    @GET("v1/collect/payments/{paymentId}")
    suspend fun getPaymentStatus(
        @Header("Authorization") auth: String,
        @Path("paymentId") paymentId: String
    ): Response<VaultPaymentResponse>

    @POST("v1/collect/qr/payments")
    suspend fun createDynamicQr(
        @Header("Authorization") auth: String,
        @Body request: DynamicQrRequest
    ): Response<DynamicQrResponse>

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
