package com.example.myapplication.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import com.example.myapplication.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onBack: () -> Unit,
    viewModel: MiniAppViewModel = com.example.myapplication.LocalMiniAppViewModel.current
) {
    val transactions by viewModel.transactions.collectAsState()
    
    val totalCollected = transactions.filter { it.amount > 0 && it.status == "SUCCESS" }.sumOf { it.amount }
    val totalDisbursed = transactions.filter { it.amount < 0 && it.status == "SUCCESS" }.sumOf { Math.abs(it.amount) }
    val pendingCount = transactions.count { it.status == "PENDING" }
    val failedCount = transactions.count { it.status == "FAILED" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Performance & Analytics", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("SUMMARY CARDS", style = MaterialTheme.typography.labelMedium, color = SwiftPayTextDim)
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AnalyticsCard(
                    modifier = Modifier.weight(1f),
                    label = "Total Collected",
                    value = "₱${"%,.0f".format(totalCollected)}",
                    icon = Icons.Rounded.TrendingUp,
                    color = SwiftPaySuccess
                )
                AnalyticsCard(
                    modifier = Modifier.weight(1f),
                    label = "Total Disbursed",
                    value = "₱${"%,.0f".format(totalDisbursed)}",
                    icon = Icons.Rounded.TrendingDown,
                    color = Color(0xFFE91E63)
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AnalyticsCard(
                    modifier = Modifier.weight(1f),
                    label = "Pending Ops",
                    value = pendingCount.toString(),
                    icon = Icons.Rounded.Schedule,
                    color = SwiftPayPrimary
                )
                AnalyticsCard(
                    modifier = Modifier.weight(1f),
                    label = "Failed Trans",
                    value = failedCount.toString(),
                    icon = Icons.Rounded.ErrorOutline,
                    color = Color.Gray
                )
            }
            
            Spacer(Modifier.height(12.dp))
            Text("VOLUME TREND (7 DAYS)", style = MaterialTheme.typography.labelMedium, color = SwiftPayTextDim)
            
            // Mock Chart
            Surface(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                color = SwiftPaySurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SwiftPayBorder)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.BarChart, null, modifier = Modifier.size(48.dp), tint = SwiftPayPrimary.copy(alpha = 0.3f))
                        Text("Volume Visualization", color = SwiftPayTextDim, style = MaterialTheme.typography.bodySmall)
                    }
                    // Simple simulated bar chart
                    Row(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        listOf(40, 70, 45, 90, 60, 80, 50).forEach { h ->
                            Box(modifier = Modifier.width(20.dp).height(h.dp).background(SwiftPayPrimary.copy(alpha = 0.6f), RoundedCornerShape(4.dp)))
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))
            Text("REVENUE SPLIT", style = MaterialTheme.typography.labelMedium, color = SwiftPayTextDim)
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SwiftPaySurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SwiftPayBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    RevenueRow("SwiftPay Wallet", 0.65f, SwiftPayPrimary)
                    RevenueRow("Direct Bank Transfer", 0.20f, SwiftPaySuccess)
                    RevenueRow("Cards / Vault", 0.15f, Color(0xFF6200EE))
                }
            }
        }
    }
}

@Composable
fun AnalyticsCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = SwiftPaySurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SwiftPayBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = SwiftPayTextPrimary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = SwiftPayTextDim)
        }
    }
}

@Composable
fun RevenueRow(label: String, percentage: Float, color: Color) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = SwiftPayTextPrimary)
            Text("${(percentage * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = percentage,
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            color = color,
            trackColor = SwiftPayBorder
        )
    }
}
