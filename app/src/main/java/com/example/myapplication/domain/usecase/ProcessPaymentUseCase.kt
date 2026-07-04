package com.example.myapplication.domain.usecase

import com.example.myapplication.data.repository.PaymentRepository
import com.example.myapplication.bridge.PaymentData

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
            // Validate amount
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






