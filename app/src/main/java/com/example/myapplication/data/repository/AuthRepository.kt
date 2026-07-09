package com.example.myapplication.data.repository

import com.example.myapplication.data.SwiftPayService
import com.example.myapplication.data.api.*

/**
 * Repository for authentication and merchant operations.
 * Handles OTP, login, and merchant profile related operations.
 */
class AuthRepository(private val swiftPayService: SwiftPayService) {

    /**
     * Send custom OTP code
     */
    suspend fun sendEmailOtp(email: String, refNo: String): Result<Unit> =
        swiftPayService.requestEmailOtp(email, refNo)

    /**
     * Verify OTP code
     */
    suspend fun verifyEmailOtp(email: String, code: String, refNo: String): Result<Unit> =
        swiftPayService.verifyEmailOtp(email, code, refNo)
}



