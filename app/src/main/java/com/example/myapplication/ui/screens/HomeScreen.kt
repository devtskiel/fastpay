package com.example.myapplication.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.components.shimmerEffect
import com.example.myapplication.ui.theme.*

@Composable
fun HomeScreen(
    onLaunchMiniApp: (String?) -> Unit,
    onNavigateToWallet: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MiniAppViewModel = com.example.myapplication.LocalMiniAppViewModel.current
) {
    val balance by viewModel.walletBalance.collectAsState()
    val isLoadingBalance = viewModel.isLoadingBalance
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.refreshBalance()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SwiftPayBackground,
        topBar = { HomeTopBar() }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Balance Terminal
            BalanceCard(balance, isLoadingBalance)

            Spacer(modifier = Modifier.height(32.dp))

            // 2. Core Treasury Actions
            Text(
                text = "TREASURY",
                style = MaterialTheme.typography.labelMedium,
                color = SwiftPayTextDim,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            ActionGrid(onLaunchMiniApp)

            Spacer(modifier = Modifier.height(40.dp))

            // 3. Activity Ledger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT LEDGER",
                    style = MaterialTheme.typography.labelMedium,
                    color = SwiftPayTextDim,
                    letterSpacing = 1.sp
                )
                TextButton(onClick = onNavigateToWallet) {
                    Text("VIEW ALL", style = MaterialTheme.typography.labelSmall, color = SwiftPayPrimary)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            ActivityLedgerPreview()

            Spacer(modifier = Modifier.height(40.dp))

            // 4. Infrastructure Links
            PromoCard()

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar() {
    CenterAlignedTopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.ElectricBolt, null, tint = SwiftPayPrimary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("SWIFTPAY", style = MaterialTheme.typography.labelLarge, color = SwiftPayTextPrimary, letterSpacing = 2.sp)
            }
        },
        actions = {
            IconButton(onClick = { /* Help */ }) {
                Icon(Icons.AutoMirrored.Rounded.HelpOutline, null, tint = SwiftPayTextSecondary)
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = SwiftPayBackground)
    )
}

@Composable
fun BalanceCard(balance: Double, isLoading: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SwiftPayCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, SwiftPayBorder)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "AVAILABLE SETTLEMENT",
                style = MaterialTheme.typography.labelSmall,
                color = SwiftPayTextSecondary,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(8.dp))
            if (isLoading) {
                Box(modifier = Modifier.width(160.dp).height(44.dp).clip(RoundedCornerShape(8.dp)).shimmerEffect())
            } else {
                Text(
                    text = "₱${"%,.2f".format(balance)}",
                    style = MaterialTheme.typography.displayMedium,
                    color = SwiftPayTextPrimary
                )
            }
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, null, tint = SwiftPaySuccess, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Verified by Netbank", style = MaterialTheme.typography.bodySmall, color = SwiftPaySuccess)
            }
        }
    }
}

@Composable
fun ActionGrid(onLaunch: (String?) -> Unit) {
    val items = listOf(
        Triple(Icons.Rounded.AccountBalance, "CASH IN", null),
        Triple(Icons.Rounded.FileUpload, "PAYOUT", "disbursement-page"),
        Triple(Icons.Rounded.QrCodeScanner, "SCAN", "qr-page"),
        Triple(Icons.Rounded.Link, "LINKS", "payment-link-page")
    )

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        items.forEach { (icon, label, path) ->
            ActionItem(icon, label) { onLaunch(path) }
        }
    }
}

@Composable
fun ActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(20.dp),
            color = SwiftPayCard,
            border = BorderStroke(1.dp, SwiftPayBorder),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = SwiftPayPrimary, modifier = Modifier.size(26.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = SwiftPayTextSecondary)
    }
}

@Composable
fun ActivityLedgerPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(2) {
            LedgerItem(
                id = "TX-8293${it}X",
                amount = 1250.0,
                status = "EXECUTED"
            )
        }
    }
}

@Composable
fun LedgerItem(id: String, amount: Double, status: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SwiftPaySurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SwiftPayBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(SwiftPayCard, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Wallet, null, tint = SwiftPayTextDim, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(id, style = MaterialTheme.typography.bodyMedium, color = SwiftPayTextPrimary, fontWeight = FontWeight.SemiBold)
                Text("SwiftPay Settlement", style = MaterialTheme.typography.bodySmall, color = SwiftPayTextDim)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("₱${"%,.2f".format(amount)}", style = MaterialTheme.typography.bodyLarge, color = SwiftPayTextPrimary, fontWeight = FontWeight.Bold)
                Text(status, style = MaterialTheme.typography.labelSmall, color = SwiftPaySuccess)
            }
        }
    }
}

@Composable
fun PromoCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SwiftPayPrimary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, SwiftPayPrimary.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("ENTERPRISE HUB", style = MaterialTheme.typography.labelSmall, color = SwiftPayPrimary)
                Spacer(Modifier.height(4.dp))
                Text("Direct API Integration", style = MaterialTheme.typography.titleMedium, color = SwiftPayTextPrimary)
                Text("Connect your POS system directly to our terminal infrastructure.", style = MaterialTheme.typography.bodySmall, color = SwiftPayTextSecondary)
            }
            Icon(Icons.Rounded.Terminal, null, tint = SwiftPayPrimary, modifier = Modifier.size(40.dp).alpha(0.3f))
        }
    }
}
