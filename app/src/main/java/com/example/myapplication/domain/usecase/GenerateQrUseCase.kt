package com.example.myapplication.domain.usecase

import com.example.myapplication.data.repository.QrRepository

/**
 * Use case for QR code generation and processing.
 * Encapsulates business logic for QR operations.
 */
class GenerateQrUseCase(private val qrRepository: QrRepository) {

    /**
     * Generate a dynamic QR code for a given amount
     */
    suspend fun generateQr(amount: Double): Result<String> {
        return try {
            if (amount <= 0) {
                return Result.failure(Exception("Invalid amount: must be greater than 0"))
            }

            val result = qrRepository.createDynamicQr(amount)

            result.map { response ->
                response.qrCodeBody ?: throw Exception("No QR data returned")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validate QR code amount boundaries
     */
    fun isAmountValid(amount: Double): Boolean {
        return amount in 0.01..1_000_000.0
    }

    /**
     * Get user-friendly error message for invalid amount
     */
    fun getAmountErrorMessage(amount: Double): String? {
        return when {
            amount <= 0 -> "Amount must be greater than ₱0.00"
            amount > 1_000_000 -> "Amount cannot exceed ₱1,000,000.00"
            else -> null
        }
    }
}




