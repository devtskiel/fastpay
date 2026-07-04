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
                title = { Text("SwiftPay 开发者中心", color = textColor, fontWeight = FontWeight.Black) },
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
                title = "集成概览",
                subtitle = "使用 SwiftPay 企业级 API 快速构建您的支付系统。",
                textColor = textColor
            )

            DocSection(
                title = "1. API 密钥",
                icon = Icons.Rounded.Key,
                content = "使用您的 SwiftPay API 密钥来访问服务。请确保密钥安全，不要在客户端代码中泄露 Secret Key。",
                code = "Public Key: pk_live_...\nSecret Key: sk_live_...",
                cardColor = cardColor,
                textColor = textColor
            )

            DocSection(
                title = "2. 身份验证",
                icon = Icons.Rounded.Security,
                content = "所有 API 请求均通过 HTTP Basic Auth 进行验证。请将您的 Secret Key 拼接一个冒号后进行 Base64 编码。",
                code = "Authorization: Basic Base64(YOUR_SECRET_KEY:)",
                cardColor = cardColor,
                textColor = textColor
            )

            DocSection(
                title = "3. 收银台 (Checkout)",
                icon = Icons.Rounded.Description,
                content = "创建一键式支付页面。支持多种支付方式，包括银行卡、电子钱包等。",
                code = "POST /v1/collect/checkout\n{\n  \"totalAmount\": {\n    \"value\": 100.00,\n    \"currency\": \"PHP\"\n  },\n  \"requestReferenceNumber\": \"REF-1001\"\n}",
                cardColor = cardColor,
                textColor = textColor
            )

            DocSection(
                title = "4. 动态二维码 (QR Ph)",
                icon = Icons.Rounded.Code,
                content = "生成符合国家标准的动态 QR Ph。用户扫码后金额自动填入，实时到账。",
                code = "POST /v1/collect/qr/payments\n{\n  \"totalAmount\": {\n    \"value\": 50.00\n  }\n}",
                cardColor = cardColor,
                textColor = textColor
            )

            DocSection(
                title = "5. 回调通知 (Webhooks)",
                icon = Icons.Rounded.Webhook,
                content = "当支付状态发生变化时（如支付成功、过期），系统会实时向您的回调 URL 发送 POST 通知。",
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
                    Text("需要技术支持？", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "我们的技术团队全天候为您提供 SwiftPay 集成协助。请联系 support@swiftpay.ph 获取更多帮助。",
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
