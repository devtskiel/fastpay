package com.example.myapplication.ui.screens

import android.nfc.NfcAdapter
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import com.example.myapplication.ui.components.*
import com.example.myapplication.ui.theme.*
import com.example.myapplication.util.EmvParser
import com.example.myapplication.util.findActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniAppScreen(
    modifier: Modifier = Modifier,
    initialPath: String? = null,
    viewModel: MiniAppViewModel = com.example.myapplication.LocalMiniAppViewModel.current,
) {
    val uiState = viewModel.uiState
    val uriHandler = LocalUriHandler.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context.findActivity()

    val webViewUrl = remember(initialPath) {
        if (initialPath != null) {
            "file:///android_asset/index.html#$initialPath"
        } else {
            "file:///android_asset/index.html"
        }
    }

    val shouldEnableNFC = remember(uiState) { 
        uiState is MiniAppUiState.WaitingForNFC && uiState.errorMessage == null 
    }

    DisposableEffect(shouldEnableNFC, activity) {
        if (shouldEnableNFC && activity != null) {
            val nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
            if (nfcAdapter != null && nfcAdapter.isEnabled) {
                try {
                    nfcAdapter.enableReaderMode(
                        activity,
                        { tag ->
                            val isoDep = android.nfc.tech.IsoDep.get(tag)
                            if (isoDep != null) {
                                try {
                                    isoDep.timeout = 10000
                                    isoDep.connect()
                                    val cardData = EmvParser.readCardData(isoDep) { progress ->
                                        viewModel.onNfcProgress(progress)
                                    }
                                    if (cardData != null) {
                                        viewModel.onNFCCardDetected(cardData.first, cardData.second, cardData.third)
                                    } else {
                                        viewModel.onNfcError("Unable to read card data. Please keep the card steady and try again.")
                                    }
                                } catch (e: Exception) {
                                    Log.e("NFC", "Error reading tag", e)
                                    viewModel.onNfcError("Read failed: ${e.message}")
                                } finally {
                                    try { isoDep.close() } catch (_: Exception) {}
                                }
                            }
                        },
                        NfcAdapter.FLAG_READER_NFC_A or 
                        NfcAdapter.FLAG_READER_NFC_B or 
                        NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or
                        NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                        null
                    )
                } catch (e: Exception) {
                    Log.e("NFC", "Failed to enable reader mode", e)
                }
            }
        }
        onDispose {
            if (activity != null) {
                try {
                    NfcAdapter.getDefaultAdapter(activity)?.disableReaderMode(activity)
                } catch (_: Exception) {}
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            SwiftPayWebView(
                url = webViewUrl,
                modifier = Modifier.fillMaxSize(),
                onPaymentRequest = { viewModel.requestPaymentConsent(it) },
                onBalanceRequest = { viewModel.onBalanceRequest() },
                onTransactionsRequest = { viewModel.onTransactionsRequest() },
                onPaymentChannelsRequest = { viewModel.onPaymentChannelsRequest() },
                onPaymentLinkRequest = { viewModel.onPaymentLinkRequest(it) },
                onSaveSettings = { viewModel.onSaveSettings(it) },
                onGetSettings = { viewModel.getSettings() },
                onScanNFCCard = { viewModel.onScanNFCCard(it) },
                onGenerateDynamicQr = { viewModel.onGenerateDynamicQr(it) },
                onWebhooksRequest = { viewModel.onWebhooksRequest() },
                onAddWebhook = { name, url -> viewModel.onAddWebhookRequest(name, url) },
                onDeleteWebhook = { id -> viewModel.onDeleteWebhookRequest(id) },
                onCreateInvoice = { amount, desc -> viewModel.onCreateInvoiceRequest(amount, desc) },
                onMembersRequest = { viewModel.onMembersRequest() },
                onAddMember = { name, email, role -> viewModel.onAddMemberRequest(name, email, role) },
                onDeleteMember = { id -> viewModel.onDeleteMemberRequest(id) },
                onBanksRequest = { viewModel.onBanksRequest() },
                onDisburseRequest = { amount: Double, account: String, first: String, last: String, bank: String? ->
                    viewModel.onDisburseRequest(amount, account, first, last, bank) 
                },
                onGenerateVca = { name: String -> viewModel.onGenerateVcaRequest(name) },
                onVcaTransactionsRequest = { viewModel.onVcaTransactionsRequest() },
                onBridgeReady = { viewModel.bridge = it }
            )

            AnimatedContent(
                targetState = uiState,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f))
                        .togetherWith(fadeOut(animationSpec = tween(200)))
                },
                label = "OverlayState"
            ) { state ->
                when (state) {
                    is MiniAppUiState.Processing -> {
                        SwiftPayProcessingDialog(message = state.message)
                    }
                    is MiniAppUiState.Error -> {
                        SwiftPayErrorDialog(message = state.message, onDismiss = { viewModel.dismissError() })
                    }
                    is MiniAppUiState.DynamicQrReady -> {
                        SwiftPayQrDialog(
                            qrData = state.qrData,
                            amount = state.amount,
                            onDismiss = { viewModel.dismissConsent() }
                        )
                    }
                    is MiniAppUiState.WaitingForNFC -> {
                        SwiftPayNfcTapDialog(
                            amount = state.amount,
                            merchantName = state.merchantName,
                            merchantAddress = state.merchantAddress,
                            timeLeft = state.timeLeft,
                            sessionDurationSeconds = 90,
                            errorMessage = state.errorMessage,
                            statusMessage = state.statusMessage,
                            onRetry = { viewModel.retryNfc() },
                            onCancel = { viewModel.dismissConsent() }
                        )
                    }
                    is MiniAppUiState.WaitingForCVV -> {
                        SwiftPayCvvDialog(
                            amount = state.amount,
                            cardLabel = state.label,
                            last4 = state.pan.takeLast(4),
                            onConfirm = { cvv -> viewModel.onCvvEntered(cvv) },
                            onCancel = { viewModel.dismissConsent() }
                        )
                    }
                    is MiniAppUiState.PaymentConsent -> {
                        SwiftPayBaseDialog(
                            onDismissRequest = { viewModel.dismissConsent() },
                            icon = Icons.Rounded.Payment,
                            title = "Confirm Payment",
                            description = "Merchant: ${state.data.description}\nAmount: ${state.data.currency} ${state.data.amount}",
                            buttons = {
                                SwiftPaySecondaryButton(text = "Cancel", onClick = { viewModel.dismissConsent() }, modifier = Modifier.weight(1f))
                                SwiftPayPrimaryButton(text = "Pay Now", onClick = { viewModel.approvePayment(state.data) }, modifier = Modifier.weight(1f))
                            }
                        )
                    }
                    is MiniAppUiState.PaymentRedirect -> {
                        SwiftPayBaseDialog(
                            onDismissRequest = { viewModel.dismissConsent() },
                            icon = Icons.Rounded.Security,
                            title = "Secure Redirect",
                            description = "You will be redirected to a secure payment page to complete the transaction.",
                            buttons = {
                                SwiftPaySecondaryButton(text = "Cancel", onClick = { viewModel.dismissConsent() }, modifier = Modifier.weight(1f))
                                SwiftPayPrimaryButton(
                                    text = "Continue",
                                    onClick = {
                                        uriHandler.openUri(state.url)
                                        viewModel.dismissConsent()
                                    },
                                    icon = Icons.Rounded.OpenInBrowser,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        )
                    }
                    is MiniAppUiState.PaymentLinkReady -> {
                        PaymentLinkDialog(
                            url = state.url,
                            onOpen = {
                                uriHandler.openUri(state.url)
                                viewModel.dismissError()
                            },
                            onDismiss = { viewModel.dismissError() }
                        )
                    }
                    MiniAppUiState.Idle -> { /* Nothing */ }
                }
            }
        }
    }
}
