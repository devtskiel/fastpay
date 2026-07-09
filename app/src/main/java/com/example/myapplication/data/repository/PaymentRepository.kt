package com.example.myapplication.data.repository

import com.example.myapplication.data.SwiftPayService
import com.example.myapplication.data.api.*
import kotlinx.coroutines.flow.flow

/**
 * Repository for all payment-related operations.
 * Abstracts the SwiftPayService and provides a clean API for ViewModels.
 */
class PaymentRepository(private val swiftPayService: SwiftPayService) {

    /**
     * Create a checkout (Netbank v1)
     */
    suspend fun createCheckout(paymentData: com.example.myapplication.bridge.PaymentData) =
        swiftPayService.createCheckout(paymentData)

    /**
     * Create an order (SwiftPay v2.8)
     */
    suspend fun createOrder(
        amount: Double,
        referenceNo: String,
        customerName: String? = null,
        email: String? = null
    ) = swiftPayService.createOrder(amount, referenceNo, customerName, email)

    /**
     * Bootstrap QRPH (SwiftPay v2.8)
     */
    suspend fun bootstrapQrph(amount: Double, referenceNo: String) =
        swiftPayService.bootstrapQrph(amount, referenceNo)

    /**
     * Get institutions
     */
    suspend fun getInstitutions() = swiftPayService.getInstitutions()

    /**
     * Get disbursement banks
     */
    suspend fun getBanks() = swiftPayService.getBanks()

    /**
     * Execute disbursement
     */
    suspend fun disburse(
        amount: Double,
        accountNumber: String,
        firstName: String,
        lastName: String,
        bankCode: String? = null
    ) = swiftPayService.disburse(amount, accountNumber, firstName, lastName, bankCode)

    /**
     * Get checkout status
     */
    suspend fun getCheckoutStatus(checkoutId: String) =
        swiftPayService.getCheckoutStatus(checkoutId)

    /**
     * Create a payment link
     */
    suspend fun createPaymentLink(amount: Double = 0.0, description: String = "") =
        swiftPayService.createPaymentLink(amount, description)

    /**
     * Get payment status
     */
    suspend fun getPaymentStatus(paymentId: String) =
        swiftPayService.getPaymentStatus(paymentId)

    /**
     * Create a dynamic QR code
     */
    suspend fun createDynamicQr(amount: Double) =
        swiftPayService.createDynamicQr(amount)

    /**
     * Get internal transactions
     */
    suspend fun getInternalTransactions() =
        swiftPayService.getInternalTransactions()
}



