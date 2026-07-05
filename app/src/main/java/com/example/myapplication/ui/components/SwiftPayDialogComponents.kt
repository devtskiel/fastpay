package com.example.myapplication.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.myapplication.ui.theme.FastPayBlue
import com.example.myapplication.ui.theme.FastPayNavy
import java.net.URLEncoder

@Composable
fun SwiftPayBaseDialog(
    onDismissRequest: () -> Unit,
    icon: ImageVector? = null,
    iconColor: Color = FastPayBlue,
    title: String,
    description: String? = null,
    content: @Composable (ColumnScope.() -> Unit)? = null,
    buttons: @Composable (RowScope.() -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (icon != null) {
                    val infiniteTransition = rememberInfiniteTransition(label = "iconPulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.05f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = EaseInOutCubic),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "iconPulse"
                    )

                    Surface(
                        shape = CircleShape,
                        color = iconColor.copy(alpha = 0.12f),
                        modifier = Modifier
                            .size(72.dp)
                            .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = iconColor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                if (description != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }

                if (content != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    content()
                }

                if (buttons != null) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        buttons()
                    }
                }
            }
        }
    }
}

@Composable
fun SwiftPayProcessingDialog(message: String) {
    SwiftPayBaseDialog(
        onDismissRequest = {},
        title = message,
        icon = Icons.Rounded.AccountBalanceWallet,
        content = {
            val infiniteTransition = rememberInfiniteTransition(label = "processing")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotate"
            )

            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation },
                    color = FastPayNavy,
                    strokeWidth = 3.dp,
                    trackColor = FastPayNavy.copy(alpha = 0.1f)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { index ->
                    val dotScale by infiniteTransition.animateFloat(
                        initialValue = 0.5f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = index * 150, easing = EaseInOutCubic),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .graphicsLayer { scaleX = dotScale; scaleY = dotScale }
                            .background(FastPayNavy.copy(alpha = 0.4f), CircleShape)
                    )
                }
            }
        }
    )
}

@Composable
fun SwiftPayErrorDialog(message: String, onDismiss: () -> Unit) {
    SwiftPayBaseDialog(
        onDismissRequest = onDismiss,
        title = "Request Failed",
        description = message,
        icon = Icons.Rounded.ErrorOutline,
        iconColor = MaterialTheme.colorScheme.error,
        buttons = {
            SwiftPayPrimaryButton(
                text = "Close",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

@Composable
fun SwiftPayCvvDialog(
    amount: Double,
    cardLabel: String,
    last4: String,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit
) {
    var cvv by remember { mutableStateOf("") }
    
    SwiftPayBaseDialog(
        onDismissRequest = onCancel,
        icon = Icons.Rounded.Lock,
        title = "Security Check",
        description = "Please enter the 3-digit CVV for your $cardLabel ending in $last4 to authorize the payment of ₱${"%.2f".format(amount)}.",
        content = {
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = cvv,
                onValueChange = { if (it.length <= 3 && it.all { char -> char.isDigit() }) cvv = it },
                modifier = Modifier.width(120.dp),
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    textAlign = TextAlign.Center,
                    letterSpacing = 8.sp
                ),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                ),
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                placeholder = { Text("000", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FastPayBlue,
                    unfocusedBorderColor = FastPayNavy.copy(alpha = 0.2f),
                    focusedTextColor = FastPayNavy,
                    unfocusedTextColor = FastPayNavy
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
        },
        buttons = {
            SwiftPaySecondaryButton(text = "Cancel", onClick = onCancel, modifier = Modifier.weight(1f))
            SwiftPayPrimaryButton(
                text = "Authorize",
                onClick = { if (cvv.length >= 3) onConfirm(cvv) },
                modifier = Modifier.weight(1f),
                enabled = cvv.length >= 3
            )
        }
    )
}

@Composable
fun SwiftPayQrDialog(qrData: String, amount: Double, onDismiss: () -> Unit) {
    SwiftPayBaseDialog(
        onDismissRequest = onDismiss,
        title = "QR Ph Payment",
        description = "Accept payments from any Philippine banking app",
        content = {
            val qrImageUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=${URLEncoder.encode(qrData, "UTF-8")}"
            
            Surface(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, FastPayNavy.copy(alpha = 0.1f)),
                modifier = Modifier.size(200.dp),
            ) {
                AsyncImage(
                    model = qrImageUrl,
                    contentDescription = "QR Code",
                    modifier = Modifier.fillMaxSize().background(Color.White).padding(12.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "AMOUNT DUE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "₱${"%.2f".format(amount)}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        buttons = {
            SwiftPayPrimaryButton(text = "Done", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    )
}

@Composable
fun SwiftPayNfcTapDialog(
    amount: Double,
    merchantName: String,
    merchantAddress: String,
    timeLeft: Int,
    sessionDurationSeconds: Int = 90,
    errorMessage: String? = null,
    statusMessage: String? = null,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    val isError = errorMessage != null
    val isWarning = !isError && timeLeft in 1..15
    val accentColor = when {
        isError -> MaterialTheme.colorScheme.error
        isWarning -> Color(0xFFE65100)
        else -> FastPayBlue
    }
    val statusTitle = when {
        isError -> "Reader Needs Attention"
        !statusMessage.isNullOrBlank() -> "Card Detected"
        else -> "Reader Ready"
    }
    val statusBody = when {
        isError -> errorMessage
        !statusMessage.isNullOrBlank() -> statusMessage
        isWarning -> "Hold the card steady against the back of your phone."
        else -> "Hold your card or mobile wallet against the device."
    }
    val progressTarget = if (sessionDurationSeconds <= 0) 0f else {
        (timeLeft.coerceIn(0, sessionDurationSeconds)).toFloat() / sessionDurationSeconds.toFloat()
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(500, easing = EaseOutCubic),
        label = "nfcSessionProgress"
    )

    Dialog(onDismissRequest = onCancel) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                accentColor.copy(alpha = 0.06f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = FastPayNavy.copy(alpha = 0.06f),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = null,
                                    tint = FastPayNavy,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Contactless,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = accentColor
                            )
                            Text(
                                text = if (isError) "Reader Paused" else "Secure NFC",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Tap to Pay",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = FastPayNavy,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = statusBody,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 24.sp
                )

                Spacer(Modifier.height(16.dp))

                NfcStatusPill(
                    title = statusTitle,
                    subtitle = if (isError) "Check card position and retry" else "Supports contactless cards, phones, or wearables",
                    accentColor = accentColor,
                    isError = isError
                )

                Spacer(Modifier.height(20.dp))

                NfcReaderIllustration(
                    accentColor = accentColor,
                    isError = isError,
                    isWarning = isWarning
                )

                Spacer(Modifier.height(20.dp))

                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "AMOUNT DUE",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "₱ ${"%.2f".format(amount)}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    color = FastPayNavy
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = accentColor.copy(alpha = 0.1f)
                            ) {
                                Icon(
                                    Icons.Rounded.CreditCard,
                                    contentDescription = null,
                                    modifier = Modifier.padding(14.dp).size(24.dp),
                                    tint = accentColor
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = merchantName.uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = merchantAddress,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!isError) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Schedule,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = accentColor
                                        )
                                        Text(
                                            text = if (isWarning) "Session Ending" else "Reader Session Active",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Text(
                                        text = "${timeLeft}s",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = accentColor
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp),
                                    color = accentColor,
                                    trackColor = accentColor.copy(alpha = 0.12f)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NfcInstructionRow(
                        index = 1,
                        title = "Locate NFC Area",
                        subtitle = "Bring card close to the camera area on the back of your phone.",
                        accentColor = accentColor,
                        isError = isError
                    )
                    NfcInstructionRow(
                        index = 2,
                        title = "Hold Steady",
                        subtitle = "Keep the card, wearable, or phone still for 2-3 seconds.",
                        accentColor = accentColor,
                        isError = isError
                    )
                    NfcInstructionRow(
                        index = 3,
                        title = "Wait for Step",
                        subtitle = "If supported, CVV or payment confirmation will follow automatically.",
                        accentColor = accentColor,
                        isError = isError
                    )
                }

                Spacer(Modifier.weight(1f))

                if (isError) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SwiftPaySecondaryButton(
                            text = "Cancel",
                            onClick = onCancel,
                            modifier = Modifier.weight(1f)
                        )
                        SwiftPayPrimaryButton(
                            text = "Retry",
                            onClick = onRetry,
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.Refresh,
                            containerColor = accentColor
                        )
                    }
                } else {
                    SwiftPaySecondaryButton(
                        text = "Cancel Payment",
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun NfcStatusPill(
    title: String,
    subtitle: String,
    accentColor: Color,
    isError: Boolean
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = accentColor.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isError) Icons.Rounded.ErrorOutline else Icons.Rounded.WifiTethering,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = accentColor
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun NfcReaderIllustration(
    accentColor: Color,
    isError: Boolean,
    isWarning: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "nfcIllustration")
    val phoneFloat by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phoneFloat"
    )
    val cardTilt by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = -3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cardTilt"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        contentAlignment = Alignment.Center
    ) {
        repeat(3) { index ->
            val waveScale by infiniteTransition.animateFloat(
                initialValue = 0.78f,
                targetValue = 1.18f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 1800,
                        delayMillis = index * 260,
                        easing = EaseOutCubic
                    ),
                    repeatMode = RepeatMode.Restart
                ),
                label = "waveScale$index"
            )
            val waveAlpha by infiniteTransition.animateFloat(
                initialValue = if (isError) 0.08f else 0.28f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 1800,
                        delayMillis = index * 260,
                        easing = EaseOutCubic
                    ),
                    repeatMode = RepeatMode.Restart
                ),
                label = "waveAlpha$index"
            )

            Box(
                modifier = Modifier
                    .size((150 + (index * 42)).dp)
                    .graphicsLayer {
                        scaleX = waveScale
                        scaleY = waveScale
                        alpha = if (isError) 0.1f else waveAlpha
                    }
                    .border(
                        width = 1.5.dp,
                        color = accentColor.copy(alpha = if (isError) 0.18f else 0.35f),
                        shape = CircleShape
                    )
            )
        }

        Surface(
            shape = RoundedCornerShape(36.dp),
            color = FastPayNavy,
            shadowElevation = 18.dp,
            modifier = Modifier
                .size(width = 152.dp, height = 220.dp)
                .graphicsLayer { translationY = phoneFloat }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.82f) else accentColor,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.18f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.08f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (isError) Icons.Rounded.Close else Icons.Rounded.Contactless,
                                contentDescription = null,
                                modifier = Modifier.size(62.dp),
                                tint = Color.White
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = if (isError) "Paused" else if (isWarning) "Hold Steady" else "Ready to Scan",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = if (isError) MaterialTheme.colorScheme.errorContainer else Color(0xFFFFB54D),
            shadowElevation = 12.dp,
            modifier = Modifier
                .size(width = 118.dp, height = 74.dp)
                .offset(x = 72.dp, y = 40.dp)
                .graphicsLayer {
                    rotationZ = cardTilt
                    translationY = -phoneFloat / 2f
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x33FFFFFF),
                    modifier = Modifier
                        .width(26.dp)
                        .height(20.dp)
                        .align(Alignment.TopStart)
                ) {}

                Icon(
                    Icons.Rounded.WifiTethering,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.TopEnd),
                    tint = if (isError) MaterialTheme.colorScheme.error else Color.White.copy(alpha = 0.9f)
                )

                Text(
                    text = if (isError) "Try Again" else "Tap Card",
                    modifier = Modifier.align(Alignment.BottomStart),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isError) MaterialTheme.colorScheme.error else FastPayNavy
                )
            }
        }
    }
}

@Composable
private fun NfcInstructionRow(
    index: Int,
    title: String,
    subtitle: String,
    accentColor: Color,
    isError: Boolean
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.1f)),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = if (isError) 0.1f else 0.14f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = index.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun SwiftPayPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    containerColor: Color = FastPayNavy,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f)
        ),
        enabled = enabled
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun SwiftPaySecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        )
    }
}
