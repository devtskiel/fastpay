package com.example.myapplication.domain.usecase

import com.example.myapplication.data.SwiftPayService
import com.example.myapplication.data.api.PaymentLinkResponse
import com.example.myapplication.data.api.strategy.PaymentLinkRequestBuilder
import android.util.Log

/**
 * UseCase for generating payment links with proper validation and error handling.
 * Encapsulates business logic for payment link generation.
 */
class GeneratePaymentLinkUseCase(
    private val swiftPayService: SwiftPayService
) {
    /**
     * Generate a payment link for the given amount and description.
     *
     * @param amount Optional fixed amount (0 for open amount links)
     * @param description Payment description/memo
     * @param merchantAlias Optional merchant alias override
     * @return Result containing PaymentLinkResponse or error
     */
    suspend fun execute(
        amount: Double = 0.0,
        description: String = "",
        merchantAlias: String? = null
    ): Result<PaymentLinkResponse> {
        return try {
            // Validate inputs
            if (amount < 0) {
                return Result.failure(Exception("Amount cannot be negative"))
            }
            if (amount > 1_000_000) {
                return Result.failure(Exception("Amount exceeds maximum limit of ₱1,000,000"))
            }

            val desc = description.ifBlank { "Fast Pay Payment" }
            Log.d("GeneratePaymentLinkUseCase", "Generating payment link: amount=$amount, desc=$desc")

            // Use service to create link
            val result = swiftPayService.createPaymentLink(
                amount = if (amount > 0) amount else 0.0,
                description = desc
            )

            when {
                result.isSuccess -> {
                    val linkResponse = result.getOrNull()
                    Log.i("GeneratePaymentLinkUseCase", "Payment link generated: ${linkResponse?.paymentLinkUrl}")
                    Result.success(linkResponse!!)
                }
                else -> {
                    val error = result.exceptionOrNull()?.message ?: "Failed to generate payment link"
                    Log.e("GeneratePaymentLinkUseCase", "Payment link generation failed: $error")
                    Result.failure(Exception(error))
                }
            }
        } catch (e: Exception) {
            Log.e("GeneratePaymentLinkUseCase", "Exception in GeneratePaymentLinkUseCase", e)
            Result.failure(e)
        }
    }

    /**
     * Build a payment link request using the builder pattern.
     */
    fun buildRequest(): PaymentLinkRequestBuilder {
        return PaymentLinkRequestBuilder()
    }
}

