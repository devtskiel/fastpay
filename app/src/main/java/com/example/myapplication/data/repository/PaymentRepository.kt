package com.example.myapplication.data.repository

import com.example.myapplication.data.MayaService
import com.example.myapplication.data.api.*
import kotlinx.coroutines.flow.flow

/**
 * Repository for all payment-related operations.
 * Abstracts the MayaService and provides a clean API for ViewModels.
 */
class PaymentRepository(private val mayaService: MayaService) {

    /**
     * Create a checkout
     */
    suspend fun createCheckout(paymentData: com.example.myapplication.bridge.PaymentData) =
        mayaService.createCheckout(paymentData)

    /**
     * Get checkout status
     */
    suspend fun getCheckoutStatus(checkoutId: String) =
        mayaService.getCheckoutStatus(checkoutId)

    /**
     * Create a payment link
     */
    suspend fun createPaymentLink(amount: Double = 0.0, description: String = "") =
        mayaService.createPaymentLink(amount, description)

    /**
     * Get payment status
     */
    suspend fun getPaymentStatus(paymentId: String) =
        mayaService.getPaymentStatus(paymentId)

    /**
     * Create a dynamic QR code
     */
    suspend fun createDynamicQr(amount: Double) =
        mayaService.createDynamicQr(amount)

    /**
     * Get internal transactions
     */
    suspend fun getInternalTransactions() =
        mayaService.getInternalTransactions()
}



