package com.example.myapplication.domain.usecase

import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.data.SettingsManager
import com.example.myapplication.data.SwiftPayService
import com.example.myapplication.data.loadSwiftPayCredentials

/**
 * Use case for authentication operations.
 * Handles login, OTP verification, and merchant profile operations.
 */
class AuthenticateUseCase(
    private val authRepository: AuthRepository,
    private val settingsManager: SettingsManager
) {

    /**
     * Step 1: Request OTP using email and an access key (Secret Key)
     * This also validates if the provided access key is correct.
     */
    suspend fun requestAccess(email: String, accessKey: String): Result<Unit> {
        return try {
            if (!isValidEmail(email)) {
                return Result.failure(Exception("Invalid email format"))
            }
            if (accessKey.isBlank()) {
                return Result.failure(Exception("Access Key is required"))
            }

            // Create a temporary service with the provided key to validate it
            val tempService = SwiftPayService(customSecretKey = accessKey)
            val refNo = "AUTH${System.currentTimeMillis()}"
            
            val result = tempService.requestEmailOtp(email, refNo)
            
            if (result.isSuccess) {
                // Temporarily cache the key so we can use it for verification
                settingsManager.saveSecretKey(accessKey)
                Result.success(Unit)
            } else {
                result
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Step 2: Verify the OTP code sent to the email
     */
    suspend fun verifyAccess(email: String, code: String): Result<Boolean> {
        return try {
            if (code.length != 6 || !code.all { it.isDigit() }) {
                return Result.failure(Exception("Invalid OTP format (6 digits required)"))
            }

            // Load the key we cached in Step 1
            val credentials = settingsManager.loadSwiftPayCredentials()
            val tempService = SwiftPayService(customSecretKey = credentials.secretKey)
            val refNo = "VERIFY${System.currentTimeMillis()}"

            val result = tempService.verifyEmailOtp(email, code, refNo)
            
            if (result.isSuccess) {
                settingsManager.setLoggedIn(email, true)
                Result.success(true)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Verification failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validate email format
     */
    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$".toRegex()
        return emailRegex.matches(email)
    }
}
