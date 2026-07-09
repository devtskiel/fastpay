package com.example.myapplication.data.repository

import com.example.myapplication.data.SwiftPayService
import com.example.myapplication.data.api.*

/**
 * Repository for QR code operations.
 * Handles static and dynamic QR code generation and processing.
 */
class QrRepository(private val swiftPayService: SwiftPayService) {

    /**
     * Create a dynamic QR code for payment
     */
    suspend fun createDynamicQr(amount: Double): Result<DynamicQrResponse> =
        swiftPayService.createDynamicQr(amount)
}




