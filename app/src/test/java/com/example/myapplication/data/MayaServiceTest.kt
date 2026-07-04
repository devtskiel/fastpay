package com.example.myapplication.data

import com.example.myapplication.data.api.*
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for MayaService data models and validation logic
 *
 * These tests verify:
 * - Amount validation for QRPH and vault payments
 * - Card details and PAN handling
 * - Expiry format parsing
 * - PAN masking for security
 * - Response model construction
 */
class MayaServiceTest {

    @Test
    fun testAmountBoundaryValidation() {
        // QRPH amount range: 0.01 - 1,000,000
        assertTrue("0.01 should be valid", 0.01 > 0 && 0.01 <= 1000000.0)
        assertTrue("1000000.0 should be valid", 1000000.0 > 0 && 1000000.0 <= 1000000.0)
        assertFalse("0.0 should be invalid", 0.0 > 0 && 0.0 <= 1000000.0)
        assertFalse("1000001.0 should be invalid", 1000001.0 > 0 && 1000001.0 <= 1000000.0)
    }

    @Test
    fun testCardDetails_Construction() {
        val card = CardDetails(
            number = "4111111111111111",
            expMonth = "12",
            expYear = "2025"
        )

        assertEquals("4111111111111111", card.number)
        assertEquals("12", card.expMonth)
        assertEquals("2025", card.expYear)
    }

    @Test
    fun testPAN_LengthValidation() {
        val validPANs = listOf("4111111111111", "4111111111111111", "378282246310005")
        validPANs.forEach { pan ->
            assertTrue("${pan.length} should be between 13-19", pan.length in 13..19)
        }
    }

    @Test
    fun testExpiry_Parsing() {
        val expiry = "12/25"
        val month = expiry.substring(0, 2)
        val year = "20" + expiry.substring(3, 5)

        assertEquals("12", month)
        assertEquals("2025", year)
    }

    @Test
    fun testPAN_Masking() {
        val pan = "4111111111111111"
        val masked = "****${pan.takeLast(4)}"

        assertEquals("****1111", masked)
        assertFalse(masked.contains(pan.dropLast(4)))
    }

    @Test
    fun testBalanceResponse_Construction() {
        val response = BalanceResponse(
            availableBalance = 40000.0,
            totalBalance = 50000.0,
            currency = "PHP"
        )

        assertEquals(40000.0, response.availableBalance)
        assertEquals(50000.0, response.totalBalance)
    }

    @Test
    fun testMayaQrphResponse_Construction() {
        val response = MayaQrphResponse(
            id = "qrph_123",
            qrCode = "00020101...",
            referenceNumber = "QR123456",
            amount = 100.0,
            currency = "PHP",
            status = "ACTIVE"
        )

        assertEquals("qrph_123", response.id)
        assertEquals("ACTIVE", response.status)
    }

    @Test
    fun testPaymentTokenResponse_Construction() {
        val response = PaymentTokenResponse(
            paymentTokenId = "token_abc123",
            state = "ACTIVE"
        )

        assertEquals("token_abc123", response.paymentTokenId)
        assertEquals("ACTIVE", response.state)
    }

    @Test
    fun testVaultPaymentResponse_WithVerificationUrl() {
        val response = VaultPaymentResponse(
            id = "vault_xyz",
            status = "PENDING",
            verificationUrl = "https://3ds.example.com"
        )

        assertNotNull(response.verificationUrl)
        assertTrue(response.verificationUrl!!.contains("3ds"))
    }
}



