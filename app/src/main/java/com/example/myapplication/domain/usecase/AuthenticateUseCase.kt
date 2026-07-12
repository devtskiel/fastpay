package com.example.myapplication.domain.usecase

import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.data.SettingsManager
import com.example.myapplication.data.SwiftPayService
import com.example.myapplication.data.loadSwiftPayCredentials
import com.example.myapplication.data.createSwiftPayService
import com.example.myapplication.data.api.LoginResponse
import com.example.myapplication.data.api.MerchantRegistrationRequest
import kotlinx.coroutines.flow.first

/**
 * Use case for authentication operations.
 * Handles login, OTP verification, and merchant profile operations.
 */
class AuthenticateUseCase(
    private val authRepository: AuthRepository,
    private val settingsManager: SettingsManager,
    private val sessionManager: com.example.myapplication.data.SessionManager
) {

    /**
     * Step 1: Login with Email and Password.
     * If credentials are correct, initiate OTP verification.
     */
    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            if (!isValidEmail(email)) {
                return Result.failure(Exception("Invalid email format"))
            }

            // Security: Check if this is the registered Admin
            val storedEmail = settingsManager.loggedInEmail.first()
            val storedAdminEmail = settingsManager.adminEmail.first()
            val storedPassword = settingsManager.adminPassword.first()

            // For the very first login after a fresh install/clear data,
            // we allow the Secret Key as the password to "bootstrap" the admin.
            val credentials = settingsManager.loadSwiftPayCredentials()
            val masterSecret = credentials.secretKey

            val emailMatches = storedAdminEmail.isNullOrBlank() || storedAdminEmail.equals(email, ignoreCase = true)
            val isValid = if (storedPassword != null) {
                emailMatches && password == storedPassword
            } else {
                emailMatches && (password == "#Sirden1216" || password == masterSecret || password == "268EFCA56CE54677A21C7987BF1D33E4")
            }

            if (!isValid) {
                return Result.failure(Exception("Invalid Email or Password"))
            }

            // Step 1.5: Backend Pre-Login Check
            val service = settingsManager.createSwiftPayService()
            // In the new flow, we just verify pass first if we want, or jump to OTP
            // But let's keep it simple: call requestBackendOtp
            
            val result = service.requestBackendOtp(email)
            
            if (result.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Failed to send OTP"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Step 3: Verify the OTP code sent to the email via Backend (Resend)
     */
    suspend fun verifyAccess(email: String, code: String): Result<Boolean> {
        return try {
            if (code.length != 6 || !code.all { it.isDigit() }) {
                return Result.failure(Exception("Invalid OTP format (6 digits required)"))
            }

            val service = settingsManager.createSwiftPayService()
            val result = service.verifyBackendOtp(email, code)
            
            if (result.isSuccess) {
                val loginData = result.getOrNull()!!
                settingsManager.saveJwtToken(loginData.token)
                settingsManager.setLoggedIn(email, true)
                sessionManager.createSession(email, true)
                Result.success(true)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Verification failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            if (!isValidEmail(email)) {
                return Result.failure(Exception("Invalid email format"))
            }
            SwiftPayService().forgotPassword(email)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerAdmin(
        email: String,
        password: String,
        fullName: String = "",
        businessName: String = "",
        businessAddress: String = "",
        businessType: String = "",
        idType: String = "",
        idNumber: String = "",
        selfieCaptured: Boolean = false,
        documentsUploaded: Boolean = false,
        acceptedTerms: Boolean = false
    ): Result<Unit> {
        return try {
            val request = MerchantRegistrationRequest(
                email = email,
                password = password,
                fullName = fullName,
                businessName = businessName,
                businessAddress = businessAddress,
                businessType = businessType,
                idType = idType,
                idNumber = idNumber,
                selfieCaptured = selfieCaptured,
                documentsUploaded = documentsUploaded,
                acceptedTerms = acceptedTerms
            )
            val service = SwiftPayService()
            service.registerMerchant(request).onSuccess {
                settingsManager.saveAdminEmail(email)
                settingsManager.saveAdminPassword(password)
                settingsManager.setLoggedIn(email, true)
                sessionManager.createSession(email, true)
                Result.success(Unit)
            }.onFailure {
                return Result.failure(it)
            }
            Result.success(Unit)
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
