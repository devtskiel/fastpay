package com.example.myapplication.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.myapplication.bridge.SwiftPaySDKBridge
import com.example.myapplication.bridge.PaymentData
import kotlinx.coroutines.delay

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SwiftPayWebView(
    url: String,
    modifier: Modifier = Modifier,
    onPaymentRequest: (PaymentData) -> Unit,
    onBalanceRequest: () -> Unit,
    onTransactionsRequest: () -> Unit,
    onPaymentChannelsRequest: () -> Unit,
    onPaymentLinkRequest: (PaymentData) -> Unit,
    onSaveSettings: (Map<String, String>) -> Unit,
    onGetSettings: () -> Unit,
    onScanNFCCard: (Double) -> Unit,
    onGenerateDynamicQr: (Double) -> Unit,
    onWebhooksRequest: () -> Unit = {},
    onAddWebhook: (String, String) -> Unit = { _, _ -> },
    onDeleteWebhook: (String) -> Unit = {},
    onCreateInvoice: (Double, String) -> Unit = { _, _ -> },
    onMembersRequest: () -> Unit = {},
    onAddMember: (String, String, String) -> Unit = { _, _, _ -> },
    onDeleteMember: (String) -> Unit = {},
    onBanksRequest: () -> Unit = {},
    onDisburseRequest: (Double, String, String, String, String?) -> Unit = { _, _, _, _, _ -> },
    onGenerateVca: (String) -> Unit = {},
    onVcaTransactionsRequest: () -> Unit = {},
    onBridgeReady: (SwiftPaySDKBridge) -> Unit
) {
    val scope = rememberCoroutineScope()
    var webView: WebView? by remember { mutableStateOf(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    var error: String? by remember { mutableStateOf(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, webView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> webView?.onResume()
                Lifecycle.Event.ON_PAUSE -> webView?.onPause()
                Lifecycle.Event.ON_DESTROY -> {
                    webView?.destroy()
                    webView = null
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BackHandler(enabled = canGoBack) {
        webView?.goBack()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (isLoading || progress < 1f) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }

            AndroidView(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                                progress = 0f
                                error = null
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                progress = 1f
                                canGoBack = view?.canGoBack() ?: false
                            }

                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, errorObj: WebResourceError?) {
                                if (request?.isForMainFrame == true) {
                                    error = errorObj?.description?.toString() ?: "Failed to load page"
                                    isLoading = false
                                }
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progress = newProgress / 100f
                            }
                        }

                        val bridge = SwiftPaySDKBridge(
                            webView = this,
                            scope = scope,
                            onPaymentRequest = onPaymentRequest,
                            onBalanceRequest = onBalanceRequest,
                            onTransactionsRequest = onTransactionsRequest,
                            onPaymentChannelsRequest = onPaymentChannelsRequest,
                            onPaymentLinkRequest = onPaymentLinkRequest,
                            onSaveSettings = onSaveSettings,
                            onGetSettings = onGetSettings,
                            onScanNFCCard = onScanNFCCard,
                            onGenerateDynamicQr = onGenerateDynamicQr,
                            onWebhooksRequest = onWebhooksRequest,
                            onAddWebhook = onAddWebhook,
                            onDeleteWebhook = onDeleteWebhook,
                            onCreateInvoice = onCreateInvoice,
                            onMembersRequest = onMembersRequest,
                            onAddMember = onAddMember,
                            onDeleteMember = onDeleteMember,
                            onBanksRequest = onBanksRequest,
                            onDisburseRequest = onDisburseRequest,
                            onGenerateVca = onGenerateVca,
                            onVcaTransactionsRequest = onVcaTransactionsRequest
                        )
                        
                        addJavascriptInterface(bridge, "SwiftPaySDK")
                        onBridgeReady(bridge)
                        
                        loadUrl(url)
                        webView = this
                    }
                },
                update = { view ->
                    if (view.url != url) {
                        view.loadUrl(url)
                    }
                }
            )
        }

        if (error != null) {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(Icons.Rounded.ErrorOutline, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Unable to load content", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error ?: "Unknown error", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { error = null; isLoading = true; webView?.reload() }, shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry")
                    }
                }
            }
        }
    }
}
