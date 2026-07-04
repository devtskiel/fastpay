package com.example.myapplication.data.repository

import com.example.myapplication.data.MayaService
import com.example.myapplication.data.api.*

/**
 * Repository for authentication and merchant operations.
 * Handles OTP, login, and merchant profile related operations.
 */
class AuthRepository(private val mayaService: MayaService) {

    /**
     * Send custom OTP code
     */
    suspend fun sendCustomOtp(email: String, code: String): Result<Boolean> =
        mayaService.sendCustomOtp(email, code)

    /**
     * Online login with email and password
     */
    suspend fun onlineLogin(email: String, password: String): Result<Boolean> =
        mayaService.onlineLogin(email, password)

    /**
     * Verify OTP code
     */
    suspend fun verifyOnlineCode(email: String, code: String): Result<Boolean> =
        mayaService.verifyOnlineCode(email, code)
}



