package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.*

/**
 * Unified SwiftPay Logo component.
 */
@Composable
fun SwiftPayLogo(
    modifier: Modifier = Modifier,
    isDark: Boolean = false,
    scale: Float = 1f
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size((36 * scale).dp)
                .background(
                    color = SwiftPayPrimary,
                    shape = RoundedCornerShape((10 * scale).dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.ElectricBolt,
                contentDescription = null,
                modifier = Modifier.size((22 * scale).dp),
                tint = Color.White
            )
        }
        
        Spacer(modifier = Modifier.width((12 * scale).dp))
        
        Text(
            text = "SwiftPay",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = (24 * scale).sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            ),
            color = if (isDark) Color.White else Color.Black
        )
    }
}

/**
 * Fintech Standard Info Card.
 */
@Composable
fun SwiftPayInfoCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, SwiftPayBorder),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodySmall,
                    color = SwiftPayTextSecondary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    value,
                    style = MaterialTheme.typography.titleLarge,
                    color = contentColor
                )
            }
            if (icon != null) {
                icon()
            }
        }
    }
}

/**
 * Standardized Status Badge.
 */
@Composable
fun SwiftPayStatusBadge(
    status: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    textColor: Color? = null
) {
    val isSuccess = status.uppercase() == "SUCCESS" || status.uppercase() == "EXECUTED"
    val defaultBg = if (isSuccess) SwiftPaySuccess.copy(alpha = 0.1f) else SwiftPayWarning.copy(alpha = 0.1f)
    val defaultText = if (isSuccess) SwiftPaySuccess else SwiftPayWarning

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(99.dp),
        color = backgroundColor ?: defaultBg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                status.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = textColor ?: defaultText
            )
        }
    }
}
