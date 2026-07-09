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
     * Create a checkout
     */
    suspend fun createCheckout(paymentData: com.example.myapplication.bridge.PaymentData) =
        swiftPayService.createCheckout(paymentData)

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



