package com.example.myapplication.data.repository

import com.example.myapplication.data.SwiftPayService
import com.example.myapplication.data.api.*
import kotlinx.coroutines.flow.flow

/**
 * Repository for all payment-related operations.
 * Abstracts the SwiftPayService and provides a clean API for ViewModels.
 */
class PaymentRepository(private val swiftPayService: SwiftPayService) {

    /**
     * Create a checkout (Netbank v1)
     */
    suspend fun createCheckout(paymentData: com.example.myapplication.data.model.PaymentData) =
        swiftPayService.createCheckout(paymentData)

    /**
     * Create an order (SwiftPay v2.8)
     */
    suspend fun createOrder(
        amount: Double,
        referenceNo: String,
        customerName: String? = null,
        email: String? = null
    ) = swiftPayService.createOrder(amount, referenceNo, customerName, email)

    /**
     * Bootstrap QRPH (SwiftPay v2.8)
     */
    suspend fun bootstrapQrph(amount: Double, referenceNo: String) =
        swiftPayService.bootstrapQrph(amount, referenceNo)

    /**
     * Get institutions (Collection v2)
     */
    suspend fun getInstitutions() = swiftPayService.getInstitutions()

    /**
     * Get disbursement banks (v1/v2)
     */
    suspend fun getBanks() = swiftPayService.getBanks()

    /**
     * Execute disbursement
     */
    suspend fun disburse(
        amount: Double,
        accountNumber: String,
        firstName: String,
        lastName: String,
        middleName: String? = null,
        bankCode: String? = null,
        remarks: String? = null,
        email: String? = null,
        mobileNumber: String? = null,
        address: com.example.myapplication.data.api.AddressV2? = null
    ) = swiftPayService.disburse(
        amount, accountNumber, firstName, lastName, middleName, bankCode, remarks, email, mobileNumber, address
    )

    /**
     * Get disbursement status
     */
    suspend fun getDisbursementStatus(disbursementId: String) =
        swiftPayService.getDisbursementStatus(disbursementId)

    /**
     * Get checkout status
     */
    suspend fun getCheckoutStatus(checkoutId: String) =
        swiftPayService.getCheckoutStatus(checkoutId)

    /**
     * Create a payment link
     */
    suspend fun createPaymentLink(amount: Double = 0.0, description: String = "") =
        swiftPayService.createPaymentLink(amount, description)

    /**
     * Get payment status
     */
    suspend fun getPaymentStatus(paymentId: String) =
        swiftPayService.getPaymentStatus(paymentId)

    /**
     * Process a vault payment (Card)
     */
    suspend fun processVaultPayment(
        amount: Double,
        cardDetails: CardDetails,
        externalRefNo: String? = null
    ) = swiftPayService.processVaultPayment(amount, cardDetails, externalRefNo)

    /**
     * Create a dynamic QR code
     */
    suspend fun createDynamicQr(amount: Double) =
        swiftPayService.createDynamicQr(amount)

    /**
     * Get internal transactions
     */
    suspend fun getInternalTransactions() =
        swiftPayService.getInternalTransactions()

    /**
     * Get wallet balance
     */
    suspend fun getWalletBalance() =
        swiftPayService.getWalletBalance()

    /**
     * Create invoice
     */
    suspend fun createInvoice(amount: Double, description: String) =
        swiftPayService.createInvoice(amount, description)

    /**
     * Generate VCA
     */
    suspend fun generateVca(accountName: String) =
        swiftPayService.generateVca(accountName)

    /**
     * Get VCA transactions
     */
    suspend fun getVcaTransactions() =
        swiftPayService.getVcaTransactions()

    /**
     * Get webhooks
     */
    suspend fun getWebhooks() =
        swiftPayService.getWebhooks()

    /**
     * Register webhook
     */
    suspend fun registerWebhook(name: String, url: String) =
        swiftPayService.registerWebhook(name, url)

    /**
     * Delete webhook
     */
    suspend fun deleteWebhook(id: String) =
        swiftPayService.deleteWebhook(id)

    /**
     * Submit deposit proof
     */
    suspend fun submitDeposit(amount: Double, ref: String, bank: String) =
        swiftPayService.submitDeposit(amount, ref, bank)

    /**
     * Get deposits for admin
     */
    suspend fun getAdminDeposits() =
        swiftPayService.getAdminDeposits()

    /**
     * Update deposit status
     */
    suspend fun updateDepositStatus(id: String, status: String) =
        swiftPayService.updateDepositStatus(id, status)
}

