package com.example.myapplication.data.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// --- Standard Checkout & Vault Models ---

@Serializable
data class CheckoutRequest(
    val totalAmount: TotalAmount,
    val buyer: Buyer? = null,
    val items: List<Item>? = null,
    val redirectUrl: RedirectUrl? = null,
    val requestReferenceNumber: String? = null,
    val metadata: Map<String, String>? = null,
)

@Serializable
data class TotalAmount(
    val value: Double,
    val currency: String,
    val details: AmountDetails? = null,
)

@Serializable
data class AmountDetails(
    val discount: Double? = null,
    val serviceCharge: Double? = null,
    val shippingFee: Double? = null,
    val tax: Double? = null,
    val subtotal: Double? = null,
)

@Serializable
data class Buyer(
    val firstName: String? = null,
    val lastName: String? = null,
    val contact: Contact? = null,
    val shippingAddress: Address? = null,
    val billingAddress: Address? = null
)

@Serializable
data class Contact(
    val phone: String? = null,
    val email: String? = null
)

@Serializable
data class Address(
    val line1: String? = null,
    val line2: String? = null,
    val city: String? = null,
    val state: String? = null,
    val zipCode: String? = null,
    val countryCode: String? = null
)

@Serializable
data class Item(
    val name: String,
    val quantity: Int? = null,
    val code: String? = null,
    val description: String? = null,
    val amount: ItemAmount? = null,
    val totalAmount: ItemAmount? = null,
)

@Serializable
data class ItemAmount(
    val value: Double,
    val details: AmountDetails? = null
)

@Serializable
data class RedirectUrl(
    val success: String,
    val failure: String,
    val cancel: String
)

@Serializable
data class CheckoutResponse(
    val checkoutId: String? = null,
    val redirectUrl: String? = null,
)

@Serializable
data class CheckoutStatusResponse(
    val id: String? = null,
    val status: String? = null,
    val paymentStatus: String? = null,
    val transactionReferenceNumber: String? = null,
    val receiptNumber: String? = null,
    val requestReferenceNumber: String? = null,
)

// --- SwiftPay Collection API (v2.8) ---

@Serializable
data class OrderRequest(
    @SerialName("x_access_key") val accessKey: String,
    @SerialName("x_reference_no") val referenceNo: String,
    @SerialName("x_amount") val amount: String,
    val details: OrderDetails,
    val signature: String,
    @SerialName("generate_customer_redirect_url") val generateRedirectUrl: Boolean = true,
    @SerialName("institution_code") val institutionCode: String? = null
)

@Serializable
data class OrderDetails(
    val customerName: String? = null,
    val customerAddress: OrderAddress? = null,
    val items: List<OrderItem>? = null
)

@Serializable
data class OrderAddress(
    val email: String? = null,
    val phone: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = "PH",
    val postcode: String? = null,
    @SerialName("address_1") val address1: String? = null,
    @SerialName("address_2") val address2: String? = null
)

@Serializable
data class OrderItem(
    val name: String,
    val quantity: Int,
    val amount: Double
)

@Serializable
data class OrderResponse(
    val customerRedirectUrl: String? = null,
    val paymentId: String? = null,
    val id: String? = null,
    val status: String? = null,
    val paymentStatus: String? = null,
    val referenceNo: String? = null,
    val amount: String? = null,
    val currency: String? = null,
    val merchantName: String? = null,
    val createdOn: String? = null
)

@Serializable
data class QrphBootstrapRequest(
    @SerialName("x_access_key") val accessKey: String,
    @SerialName("x_reference_no") val referenceNo: String,
    @SerialName("x_amount") val amount: String,
    @SerialName("x_currency") val currency: String = "PHP",
    val signature: String
)

@Serializable
data class QrphBootstrapResponse(
    val paymentId: String? = null,
    val paymentStatus: String? = null,
    val referenceNo: String? = null,
    val amount: Double? = null,
    val qrCode: String? = null,
    val bankAccountNumber: String? = null,
    val instapayBankCode: String? = null
)

// --- SwiftPay Disbursement API (v2.0) ---

@Serializable
data class DisbursementRequest(
    val merchantReferenceNo: String,
    val channel: String = "INSTAPAY",
    val institutionCode: String,
    val creditInformation: CreditInformation,
    val recipientInformation: RecipientInformation
)

@Serializable
data class CreditInformation(
    val amount: String,
    val remarks: String? = null
)

@Serializable
data class RecipientInformation(
    val accountNumber: String,
    val firstName: String,
    val middleName: String? = null,
    val lastName: String,
    val mobileNumber: String? = null,
    val email: String? = null,
    val address: AddressV2? = null
)

@Serializable
data class AddressV2(
    @SerialName("Line1") val line1: String? = null,
    @SerialName("Line2") val line2: String? = null,
    val city: String? = null,
    val postalCode: String? = null,
    val province: String? = null,
    val countryCode: String? = "PH"
)

@Serializable
data class DisbursementResponse(
    val id: Int? = null,
    val merchantId: Int? = null,
    val merchantName: String? = null,
    val registrationTime: String? = null,
    val settlementTime: String? = null,
    val merchantReferenceNo: String? = null,
    val channel: String? = null,
    val institutionCode: String? = null,
    val creditInformation: CreditInformation? = null,
    val recipientInformation: RecipientInformation? = null,
    @SerialName("Status") val status: String? = null,
    val errorMessage: String? = null,
    val channelReferenceNo: String? = null,
    val bankOperationId: String? = null
)

// --- Infrastructure & Utilities ---

@Serializable
data class BankResponse(
    val code: String,
    val name: String,
    val type: String? = null,
    val logoUrl: String? = null,
    val orderNo: Double? = null
)

@Serializable
data class BankListResponse(
    val data: List<BankResponse>
)

@Serializable
data class SwiftPayTransactionResponse(
    val data: List<SwiftPayTransaction>? = null,
    val payments: List<SwiftPayTransaction>? = null,
)

@Serializable
data class SwiftPayTransaction(
    val id: String? = null,
    @SerialName("requestReferenceNo")
    val requestReferenceNumber: String? = null,
    val amount: String? = null,
    val currency: String? = null,
    val status: String? = null,
    @SerialName("createdAt")
    val timestamp: String? = null,
)

@Serializable
data class BalanceResponse(
    val balance: Double? = null,
    val availableBalance: Double? = null,
    val totalBalance: Double? = null,
    val currency: String? = null,
)

@Serializable
data class InternalTransaction(
    val transactionId: String,
    val amount: Double,
    val status: String,
    val date: String,
)

@Serializable
data class PaymentChannel(
    val name: String,
    val status: String,
    val icon: String? = null,
)

@Serializable
data class PaymentTokenRequest(
    val card: CardDetails
)

@Serializable
data class CardDetails(
    val number: String,
    val expMonth: String,
    val expYear: String,
    val cvc: String? = null
)

@Serializable
data class PaymentTokenResponse(
    val paymentTokenId: String? = null,
    val state: String? = null,
)

@Serializable
data class VaultPaymentRequest(
    val totalAmount: TotalAmount,
    val paymentTokenId: String,
    val requestReferenceNumber: String,
    val redirectUrl: RedirectUrl? = null,
    val metadata: Map<String, String>? = null
)

@Serializable
data class VaultPaymentResponse(
    val id: String? = null,
    val paymentId: String? = null,
    val status: String? = null,
    val amount: String? = null,
    val currency: String? = null,
    val verificationUrl: String? = null,
)

@Serializable
data class DynamicQrRequest(
    val totalAmount: TotalAmount,
    val requestReferenceNumber: String,
    val redirectUrl: RedirectUrl? = null,
    val metadata: Map<String, String>? = null,
    val type: String? = null
)

@Serializable
data class DynamicQrResponse(
    val paymentId: String? = null,
    val qrCodeBody: String? = null,
    val redirectUrl: String? = null,
)

@Serializable
data class SwiftPayError(
    val code: String? = null,
    val message: String? = null,
    val errors: List<SwiftPayErrorDetail>? = null
)

@Serializable
data class SwiftPayErrorDetail(
    val code: String? = null,
    val message: String? = null,
    val field: String? = null
)

@Serializable
data class WebhookRequest(
    val name: String,
    val callbackUrl: String,
    val id: String? = null
)

@Serializable
data class VcaRequest(
    val accountName: String,
    val merchantReferenceNumber: String? = null,
    val metadata: Map<String, String>? = null
)

@Serializable
data class VcaResponse(
    val id: String? = null,
    val accountName: String? = null,
    val accountNumber: String? = null,
    val bankName: String? = null,
    val status: String? = null
)

@Serializable
data class InvoiceRequest(
    val invoiceNumber: String,
    val type: String = "SINGLE",
    val totalAmount: TotalAmount? = null,
    val redirectUrl: RedirectUrl? = null,
    val requestReferenceNumber: String? = null,
    val metadata: Map<String, String>? = null,
)

@Serializable
data class InvoiceResponse(
    val invoiceId: String? = null,
    val invoiceUrl: String? = null,
)

@Serializable
data class MerchantProfileResponse(
    val id: String? = null,
    val merchantId: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val businessName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val kycStatus: String? = null,
    val accountStatus: String? = null,
    val createdAt: String? = null,
    val address: Address? = null,
    val businessType: String? = null,
    val monthlyVolume: Double? = null,
    val masterMid: String? = null,
    val mids: List<String>? = null
)

@Serializable
data class PaymentLinkRequest(
    val description: String? = null,
    val totalAmount: TotalAmount? = null,
    val requestReferenceNumber: String? = null,
    val redirectUrl: RedirectUrl? = null,
    val metadata: Map<String, String>? = null,
    val expiresAt: String? = null
)

@Serializable
data class PaymentLinkResponse(
    val id: String? = null,
    val paymentLinkUrl: String? = null,
    val shortCode: String? = null,
    val status: String? = null,
    val description: String? = null,
    val totalAmount: TotalAmount? = null,
    @SerialName("createdAt")
    val createdAt: String? = null,
    @SerialName("expiresAt")
    val expiresAt: String? = null,
)

@Serializable
data class BalanceInquiryRequest(
    val merchant: MerchantInfo = MerchantInfo()
)

@Serializable
data class MerchantInfo(
    val acquiringTerminal: TerminalInfo = TerminalInfo()
)

@Serializable
data class TerminalInfo(
    val type: String = "POS"
)

@Serializable
data class OtpRequest(
    val email: String,
    val type: String = "LOGIN"
)

@Serializable
data class OtpVerifyRequest(
    val email: String,
    val code: String
)

@Serializable
data class CustomizationRequest(
    val logoUrl: String? = null,
    val iconUrl: String? = null,
    val appleTouchIconUrl: String? = null,
    val customTitle: String? = null,
    val colorScheme: String? = null,
    val showMerchantName: Boolean? = null,
    val skipResultPage: Boolean? = null,
    val redirectTimer: Int? = null
)

// --- Custom Backend / Deposit Models ---

@Serializable
data class DepositRequest(
    val amount: Double,
    val referenceNumber: String,
    val bankName: String
)

@Serializable
data class DepositResponse(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("user_email") val userEmail: String? = null,
    val amount: Double? = null,
    @SerialName("reference_number") val referenceNumber: String? = null,
    @SerialName("bank_name") val bankName: String? = null,
    val status: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class DepositStatusUpdateRequest(
    val status: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class MerchantRegistrationRequest(
    val email: String,
    val password: String,
    val fullName: String = "",
    val businessName: String = "",
    val businessAddress: String = "",
    val businessType: String = "",
    val idType: String = "",
    val idNumber: String = "",
    val selfieCaptured: Boolean = false,
    val documentsUploaded: Boolean = false,
    val acceptedTerms: Boolean = false
)

@Serializable
data class MerchantRegistrationResponse(
    val status: String,
    val registrationId: String? = null,
    val email: String? = null
)

@Serializable
data class LoginResponse(
    val token: String,
    val user: UserInfo
)

@Serializable
data class UserInfo(
    val id: String,
    val email: String,
    @SerialName("businessName") val businessName: String? = null
)


