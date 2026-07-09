package com.example.myapplication.bridge

import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.example.myapplication.data.api.InternalTransaction
import com.example.myapplication.data.api.SwiftPayTransaction
import com.example.myapplication.data.api.PaymentChannel

class SwiftPaySDKBridge(
    val webView: WebView,
    private val scope: CoroutineScope,
    private val onPaymentRequest: (PaymentData) -> Unit,
    private val onBalanceRequest: () -> Unit,
    private val onTransactionsRequest: () -> Unit,
    private val onPaymentChannelsRequest: () -> Unit,
    private val onPaymentLinkRequest: (PaymentData) -> Unit,
    private val onSaveSettings: (Map<String, String>) -> Unit,
    private val onGetSettings: () -> Unit,
    private val onScanNFCCard: (Double) -> Unit,
    private val onGenerateDynamicQr: (Double) -> Unit,
    private val onWebhooksRequest: () -> Unit = {},
    private val onAddWebhook: (String, String) -> Unit = { _, _ -> },
    private val onDeleteWebhook: (String) -> Unit = {},
    private val onCreateInvoice: (Double, String) -> Unit = { _, _ -> },
    private val onMembersRequest: () -> Unit = {},
    private val onAddMember: (String, String, String) -> Unit = { _, _, _ -> },
    private val onDeleteMember: (String) -> Unit = {},
    private val onBanksRequest: () -> Unit = {},
    private val onDisburseRequest: (Double, String, String, String, String?) -> Unit = { _, _, _, _, _ -> },
    private val onGenerateVca: (String) -> Unit = {},
    private val onVcaTransactionsRequest: () -> Unit = {}
) {

    private val json = Json { ignoreUnknownKeys = true }

    @JavascriptInterface
    fun postMessage(message: String) {
        scope.launch(Dispatchers.Main) {
            try {
                val request = json.decodeFromString<BridgeRequest>(message)
                handleRequest(request)
            } catch (e: Exception) {
                sendError("Invalid message format: ${e.message}")
            }
        }
    }

    private fun handleRequest(request: BridgeRequest) {
        when (request.action) {
            "get_balance" -> onBalanceRequest()
            "get_transactions" -> onTransactionsRequest()
            "get_payment_channels" -> onPaymentChannelsRequest()
            "request_payment" -> {
                val data = request.data?.let { json.decodeFromJsonElement<PaymentData>(it) }
                if (data != null) onPaymentRequest(data) else sendError("Missing payment data")
            }
            "create_payment_link" -> {
                val data = request.data?.let { json.decodeFromJsonElement<PaymentData>(it) }
                if (data != null) onPaymentLinkRequest(data) else sendError("Missing data")
            }
            "save_settings" -> {
                val data = request.data?.jsonObject
                if (data != null) {
                    val map = mutableMapOf<String, String>()
                    data["secretKey"]?.jsonPrimitive?.content?.let { map["secretKey"] = it }
                    data["publicKey"]?.jsonPrimitive?.content?.let { map["publicKey"] = it }
                    data["mid"]?.jsonPrimitive?.content?.let { map["mid"] = it }
                    data["terminalId"]?.jsonPrimitive?.content?.let { map["terminalId"] = it }
                    data["merchantAlias"]?.jsonPrimitive?.content?.let { map["merchantAlias"] = it }
                    onSaveSettings(map)
                }
            }
            "get_settings" -> onGetSettings()
            "scan_nfc_card" -> {
                val amount = request.data?.jsonObject?.get("amount")?.jsonPrimitive?.doubleOrNull ?: 0.0
                onScanNFCCard(amount)
            }
            "generate_dynamic_qr" -> {
                val amount = request.data?.jsonObject?.get("amount")?.jsonPrimitive?.doubleOrNull ?: 0.0
                onGenerateDynamicQr(amount)
            }
            "get_webhooks" -> onWebhooksRequest()
            "add_webhook" -> {
                val obj = request.data?.jsonObject
                val name = obj?.get("name")?.jsonPrimitive?.content ?: ""
                val url = obj?.get("url")?.jsonPrimitive?.content ?: ""
                onAddWebhook(name, url)
            }
            "delete_webhook" -> {
                val id = request.data?.jsonObject?.get("id")?.jsonPrimitive?.content ?: ""
                onDeleteWebhook(id)
            }
            "create_invoice" -> {
                val obj = request.data?.jsonObject
                val amount = obj?.get("amount")?.jsonPrimitive?.doubleOrNull ?: 0.0
                val desc = obj?.get("description")?.jsonPrimitive?.content ?: ""
                onCreateInvoice(amount, desc)
            }
            "get_members" -> onMembersRequest()
            "add_member" -> {
                val obj = request.data?.jsonObject
                val name = obj?.get("name")?.jsonPrimitive?.content ?: ""
                val email = obj?.get("email")?.jsonPrimitive?.content ?: ""
                val role = obj?.get("role")?.jsonPrimitive?.content ?: ""
                onAddMember(name, email, role)
            }
            "delete_member" -> {
                val id = request.data?.jsonObject?.get("id")?.jsonPrimitive?.content ?: ""
                onDeleteMember(id)
            }
            "get_banks" -> onBanksRequest()
            "disburse" -> {
                val obj = request.data?.jsonObject
                val amount = obj?.get("amount")?.jsonPrimitive?.doubleOrNull ?: 0.0
                val accountNo = obj?.get("accountNumber")?.jsonPrimitive?.content ?: ""
                val firstName = obj?.get("firstName")?.jsonPrimitive?.content ?: ""
                val lastName = obj?.get("lastName")?.jsonPrimitive?.content ?: ""
                val bankCode = obj?.get("bankCode")?.jsonPrimitive?.content
                onDisburseRequest(amount, accountNo, firstName, lastName, bankCode)
            }
            "generate_vca" -> {
                val accountName = request.data?.jsonObject?.get("accountName")?.jsonPrimitive?.content ?: ""
                onGenerateVca(accountName)
            }
            "get_vca_transactions" -> onVcaTransactionsRequest()
            else -> sendError("Unknown action: ${request.action}")
        }
    }

    fun sendResponse(data: Any?) {
        scope.launch(Dispatchers.Main) {
            val responseData: JsonElement? = when (data) {
                null -> null
                is JsonElement -> data
                is String -> json.encodeToJsonElement(data)
                is Number -> when (data) {
                    is Int -> json.encodeToJsonElement(data)
                    is Long -> json.encodeToJsonElement(data)
                    is Float -> json.encodeToJsonElement(data)
                    is Double -> json.encodeToJsonElement(data)
                    else -> json.encodeToJsonElement(data.toDouble())
                }
                is Boolean -> json.encodeToJsonElement(data)
                is UserProfile -> json.encodeToJsonElement(data)
                is PaymentData -> json.encodeToJsonElement(data)
                is InternalTransaction -> json.encodeToJsonElement(data)
                is SwiftPayTransaction -> json.encodeToJsonElement(data)
                is PaymentChannel -> json.encodeToJsonElement(data)
                is com.example.myapplication.data.Member -> json.encodeToJsonElement(data)
                is com.example.myapplication.data.api.WebhookRequest -> json.encodeToJsonElement(data)
                is com.example.myapplication.data.api.InvoiceResponse -> json.encodeToJsonElement(data)
                is com.example.myapplication.data.api.BankResponse -> json.encodeToJsonElement(data)
                is com.example.myapplication.data.api.DisbursementResponse -> json.encodeToJsonElement(data)
                is com.example.myapplication.data.api.VcaResponse -> json.encodeToJsonElement(data)
                is List<*> -> {
                    val elementList = data.map { item ->
                        when (item) {
                            is InternalTransaction -> json.encodeToJsonElement(item)
                            is SwiftPayTransaction -> json.encodeToJsonElement(item)
                            is PaymentChannel -> json.encodeToJsonElement(item)
                            is com.example.myapplication.data.Member -> json.encodeToJsonElement(item)
                            is com.example.myapplication.data.api.WebhookRequest -> json.encodeToJsonElement(item)
                            is com.example.myapplication.data.api.BankResponse -> json.encodeToJsonElement(item)
                            is com.example.myapplication.data.api.DisbursementResponse -> json.encodeToJsonElement(item)
                            is com.example.myapplication.data.api.VcaResponse -> json.encodeToJsonElement(item)
                            is String -> json.encodeToJsonElement(item)
                            is Number -> when (item) {
                                is Int -> json.encodeToJsonElement(item)
                                is Long -> json.encodeToJsonElement(item)
                                is Float -> json.encodeToJsonElement(item)
                                is Double -> json.encodeToJsonElement(item)
                                else -> json.encodeToJsonElement(item.toDouble())
                            }
                            is Boolean -> json.encodeToJsonElement(item)
                            else -> json.encodeToJsonElement(item.toString())
                        }
                    }
                    kotlinx.serialization.json.JsonArray(elementList)
                }
                is Map<*, *> -> {
                    val stringMap = data.entries.associate { entry -> entry.key.toString() to entry.value.toString() }
                    json.encodeToJsonElement(stringMap)
                }
                else -> try {
                    json.encodeToJsonElement(data.toString())
                } catch (_: Exception) {
                    json.encodeToJsonElement(data.toString())
                }
            }

            val response = BridgeResponse(
                status = "success",
                data = responseData
            )
            val jsonResponse = json.encodeToString(response)
            evaluateJavaScriptResponse(jsonResponse)
        }
    }

    fun sendError(message: String) {
        scope.launch(Dispatchers.Main) {
            val response = BridgeResponse(
                status = "error",
                message = message
            )
            val jsonResponse = json.encodeToString(response)
            evaluateJavaScriptResponse(jsonResponse)
        }
    }

    private fun evaluateJavaScriptResponse(jsonResponse: String) {
        try {
            val escapedJson = jsonResponse
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
            
            webView.evaluateJavascript("window.onSwiftPayResponse(\"$escapedJson\")", null)
        } catch (e: Exception) {
            android.util.Log.e("SwiftPaySDKBridge", "Error evaluating JavaScript response", e)
        }
    }
}
