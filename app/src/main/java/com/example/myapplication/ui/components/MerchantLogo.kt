package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.SwiftPayPrimary

@Composable
fun MerchantLogo(
    modifier: Modifier = Modifier,
    size: Int = 48
) {
    Surface(
        modifier = modifier.size(size.dp),
        shape = CircleShape,
        color = SwiftPayPrimary.copy(alpha = 0.1f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.ElectricBolt,
                contentDescription = null,
                tint = SwiftPayPrimary,
                modifier = Modifier.size((size * 0.6).dp)
            )
        }
    }
}
