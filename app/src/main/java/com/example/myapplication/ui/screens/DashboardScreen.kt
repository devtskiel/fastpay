package com.example.myapplication.ui.screens

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.myapplication.BuildConfig
import com.example.myapplication.ui.theme.SwiftPayBackground
import com.example.myapplication.ui.theme.SwiftPayBorder
import com.example.myapplication.ui.theme.SwiftPayPrimary
import com.example.myapplication.ui.theme.SwiftPaySurface
import com.example.myapplication.ui.theme.SwiftPayTextDim
import com.example.myapplication.ui.theme.SwiftPayTextPrimary
import com.example.myapplication.ui.theme.SwiftPayTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onBack: () -> Unit,
    viewModel: MiniAppViewModel = com.example.myapplication.LocalMiniAppViewModel.current
) {
    val dashboardUrl = remember {
        val configured = BuildConfig.APP_SERVER_URL.takeIf { !it.isNullOrBlank() } ?: "http://10.0.2.2:3000"
        configured.removeSuffix("/").removeSuffix("/api").trimEnd('/') + "/dashboard"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Management Console", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SwiftPayBackground)
            )
        },
        containerColor = SwiftPayBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = SwiftPaySurface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Operations Dashboard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SwiftPayTextPrimary)
                    Text(
                        "Purpose-built for onboarding, implementation, and administrative control teams to review queues, approvals, and settlement activity.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SwiftPayTextSecondary
                    )
                    Button(
                        onClick = { /* no-op, the embedded console below is already live */ },
                        colors = ButtonDefaults.buttonColors(containerColor = SwiftPayPrimary)
                    ) {
                        Icon(Icons.Rounded.OpenInNew, null, modifier = Modifier.size(18.dp))
                        androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                        Text("Live management console")
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SwiftPaySurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SwiftPayBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Connected console", style = MaterialTheme.typography.labelMedium, color = SwiftPayTextDim)
                    Text(dashboardUrl, style = MaterialTheme.typography.bodySmall, color = SwiftPayTextSecondary)
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SwiftPaySurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SwiftPayBorder)
            ) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(560.dp),
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = WebViewClient()
                            loadUrl(dashboardUrl)
                        }
                    }
                )
            }
        }
    }
}
