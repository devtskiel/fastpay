package com.example.myapplication.domain.usecase

import com.example.myapplication.data.repository.PaymentRepository
import com.example.myapplication.data.model.PaymentData

/**
 * Use case for creating and processing payments.
 * Encapsulates business logic for payment operations.
 */
class ProcessPaymentUseCase(private val paymentRepository: PaymentRepository) {

    /**
     * Process a checkout payment with validation
     */
    suspend fun executeCheckout(paymentData: PaymentData): Result<String> {
        return try {
            if (paymentData.amount <= 0) {
                return Result.failure(Exception("Invalid amount"))
            }

            val result = paymentRepository.createCheckout(paymentData)
            result.map { it.redirectUrl ?: "UNKNOWN" }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Process an order with SwiftPay v2.8 (Full Page Redirect)
     */
    suspend fun executeOrder(
        amount: Double,
        customerName: String? = null,
        email: String? = null
    ): Result<String> {
        return try {
            if (amount <= 0) return Result.failure(Exception("Invalid amount"))
            val refNo = "ORD${System.currentTimeMillis()}"
            val result = paymentRepository.createOrder(amount, refNo, customerName, email)
            result.map { it.customerRedirectUrl ?: throw Exception("No redirect URL") }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generate QRPH using SwiftPay v2.8 bootstrap
     */
    suspend fun generateQrph(amount: Double): Result<String> {
        return try {
            if (amount <= 0) return Result.failure(Exception("Invalid amount"))
            val refNo = "QRPH${System.currentTimeMillis()}"
            val result = paymentRepository.bootstrapQrph(amount, refNo)
            result.map { it.qrCode ?: throw Exception("No QR code generated") }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get payment status
     */
    suspend fun getPaymentStatus(paymentId: String) =
        paymentRepository.getPaymentStatus(paymentId)

    /**
     * Create a payment link for payment collection
     */
    suspend fun createPaymentLink(
        amount: Double,
        description: String
    ): Result<String> {
        return try {
            val result = paymentRepository.createPaymentLink(amount, description)
            result.map { it.paymentLinkUrl ?: throw Exception("No payment link URL") }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Execute a disbursement (Payout)
     */
    suspend fun executeDisbursement(
        amount: Double,
        accountNumber: String,
        firstName: String,
        lastName: String,
        bankCode: String? = null
    ) = paymentRepository.disburse(
        amount = amount,
        accountNumber = accountNumber,
        firstName = firstName,
        lastName = lastName,
        bankCode = bankCode
    )
}

/**
 * Data class for card details input
 */
data class CardDetailsInput(
    val cardNumber: String,
    val expiryMonth: String,
    val expiryYear: String,
    val cvv: String,
    val cardHolderName: String
)
