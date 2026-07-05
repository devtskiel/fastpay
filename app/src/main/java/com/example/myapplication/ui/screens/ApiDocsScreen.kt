package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Webhook
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.FastPayBlack
import com.example.myapplication.ui.theme.FastPayBlue
import com.example.myapplication.ui.theme.FastPayNavy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiDocsScreen() {
    val isDarkTheme = MaterialTheme.colorScheme.background == FastPayBlack
    val bgColor = if (isDarkTheme) Color(0xFF121212) else Color(0xFFF8F9FA)
    val textColor = if (isDarkTheme) Color.White else FastPayNavy
    val cardColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("SwiftPay Developer Center", color = textColor, fontWeight = FontWeight.Black) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = bgColor)
            )
        },
        containerColor = bgColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            DocHeader(
                title = "Integration Overview",
                subtitle = "Build your payment system quickly using SwiftPay enterprise-grade APIs.",
                textColor = textColor
            )

            DocSection(
                title = "1. API Keys",
                icon = Icons.Rounded.Key,
                content = "Use your SwiftPay API keys to access services. Ensure keys are kept secure and never expose the Secret Key in client-side code.",
                code = "Public Key: pk_live_...\nSecret Key: sk_live_...",
                cardColor = cardColor,
                textColor = textColor
            )

            DocSection(
                title = "2. Authentication",
                icon = Icons.Rounded.Security,
                content = "All API requests are authenticated via HTTP Basic Auth. Base64 encode your Secret Key followed by a colon.",
                code = "Authorization: Basic Base64(YOUR_SECRET_KEY:)",
                cardColor = cardColor,
                textColor = textColor
            )

            DocSection(
                title = "3. Checkout",
                icon = Icons.Rounded.Description,
                content = "Create one-click payment pages. Supports multiple methods including cards and digital wallets.",
                code = "POST /v1/collect/checkout\n{\n  \"totalAmount\": {\n    \"value\": 100.00,\n    \"currency\": \"PHP\"\n  },\n  \"requestReferenceNumber\": \"REF-1001\"\n}",
                cardColor = cardColor,
                textColor = textColor
            )

            DocSection(
                title = "4. Dynamic QR (QR Ph)",
                icon = Icons.Rounded.Code,
                content = "Generate standardized dynamic QR Ph codes. Scanned amounts are auto-filled for real-time settlement.",
                code = "POST /v1/collect/qr/payments\n{\n  \"totalAmount\": {\n    \"value\": 50.00\n  }\n}",
                cardColor = cardColor,
                textColor = textColor
            )

            DocSection(
                title = "5. Webhook Notifications",
                icon = Icons.Rounded.Webhook,
                content = "Receive real-time POST notifications to your callback URL when payment status changes.",
                code = "{\n  \"status\": \"PAYMENT_SUCCESS\",\n  \"id\": \"PAY-9921\",\n  \"amount\": \"100.00\"\n}",
                cardColor = cardColor,
                textColor = textColor
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = FastPayBlue),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Need technical support?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Our technical team is available 24/7 for SwiftPay integration assistance. Contact support@swiftpay.ph for more help.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }
            
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun DocHeader(title: String, subtitle: String, textColor: Color) {
    Column {
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Black, color = textColor)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, fontSize = 14.sp, color = Color.Gray, lineHeight = 22.sp)
    }
}

@Composable
fun DocSection(
    title: String,
    icon: ImageVector,
    content: String,
    code: String,
    cardColor: Color,
    textColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = FastPayBlue, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = textColor)
            }
            Spacer(Modifier.height(12.dp))
            Text(content, fontSize = 13.sp, color = Color.Gray, lineHeight = 20.sp)
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E2F), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = code,
                    color = Color(0xFFCE9178),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
