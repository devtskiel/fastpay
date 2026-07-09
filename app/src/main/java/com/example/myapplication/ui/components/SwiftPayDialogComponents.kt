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
import com.example.myapplication.ui.theme.*
import java.net.URLEncoder

@Composable
fun SwiftPayBaseDialog(
    onDismissRequest: () -> Unit,
    icon: ImageVector? = null,
    iconColor: Color = SwiftPayPrimary,
    title: String,
    description: String? = null,
    content: @Composable (ColumnScope.() -> Unit)? = null,
    buttons: @Composable (RowScope.() -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SwiftPayCard,
            border = BorderStroke(1.dp, SwiftPayBorder),
            modifier = Modifier.fillMaxWidth(0.94f)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (icon != null) {
                    Surface(
                        shape = CircleShape,
                        color = iconColor.copy(alpha = 0.1f),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = iconColor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = SwiftPayTextPrimary,
                    textAlign = TextAlign.Center
                )

                if (description != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SwiftPayTextSecondary,
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
        content = {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = SwiftPayPrimary,
                strokeWidth = 4.dp,
                trackColor = SwiftPayBorder
            )
        }
    )
}

@Composable
fun SwiftPayErrorDialog(message: String, onDismiss: () -> Unit) {
    SwiftPayBaseDialog(
        onDismissRequest = onDismiss,
        title = "Request Error",
        description = message,
        icon = Icons.Rounded.ErrorOutline,
        iconColor = SwiftPayError,
        buttons = {
            SwiftPayPrimaryButton(
                text = "Close Terminal",
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
        icon = Icons.Rounded.Security,
        title = "Authentication",
        description = "Enter 3-digit CVV for $cardLabel ending in $last4.",
        content = {
            OutlinedTextField(
                value = cvv,
                onValueChange = { if (it.length <= 3 && it.all { char -> char.isDigit() }) cvv = it },
                modifier = Modifier.width(140.dp),
                textStyle = MaterialTheme.typography.displayMedium.copy(
                    textAlign = TextAlign.Center,
                    letterSpacing = 10.sp
                ),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                ),
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SwiftPayPrimary,
                    unfocusedBorderColor = SwiftPayBorder,
                    focusedTextColor = SwiftPayTextPrimary,
                    unfocusedTextColor = SwiftPayTextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        },
        buttons = {
            SwiftPaySecondaryButton(text = "Cancel", onClick = onCancel, modifier = Modifier.weight(1f))
            SwiftPayPrimaryButton(
                text = "Confirm",
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
        title = "QR Ph Instant",
        description = "Scan to complete settlement",
        content = {
            val qrImageUrl = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=${URLEncoder.encode(qrData, "UTF-8")}"
            
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                modifier = Modifier.size(240.dp).padding(8.dp),
            ) {
                AsyncImage(
                    model = qrImageUrl,
                    contentDescription = "QR Code",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "₱${"%.2f".format(amount)}",
                style = MaterialTheme.typography.displayMedium,
                color = SwiftPayTextPrimary
            )
        },
        buttons = {
            SwiftPayPrimaryButton(text = "Done", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    )
}

@Composable
fun SwiftPayPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    containerColor: Color = SwiftPayPrimary,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            disabledContainerColor = containerColor.copy(alpha = 0.4f)
        ),
        enabled = enabled
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(text.uppercase(), style = MaterialTheme.typography.labelLarge, color = Color.White)
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
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SwiftPayBorder)
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = SwiftPayTextSecondary
        )
    }
}

// NFC Components remained for compatibility but updated to new design
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
    SwiftPayBaseDialog(
        onDismissRequest = onCancel,
        icon = Icons.Rounded.Contactless,
        title = "Tap to Pay",
        description = "Hold card steady against the device.",
        content = {
            Text(
                text = "₱ ${"%.2f".format(amount)}",
                style = MaterialTheme.typography.displayMedium,
                color = SwiftPayPrimary
            )
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { timeLeft.toFloat() / sessionDurationSeconds.toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = SwiftPayPrimary,
                trackColor = SwiftPayBorder
            )
        },
        buttons = {
            SwiftPaySecondaryButton(text = "Cancel", onClick = onCancel, modifier = Modifier.fillMaxWidth())
        }
    )
}
