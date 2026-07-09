package com.example.myapplication.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.myapplication.ui.components.shimmerEffect
import com.example.myapplication.ui.theme.*
import com.example.myapplication.data.SettingsManager
import com.example.myapplication.data.TransactionStore
import com.example.myapplication.data.createSwiftPayService
import com.example.myapplication.data.mergeTransactions
import com.example.myapplication.data.api.InternalTransaction
import kotlinx.coroutines.flow.first

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
    val context = androidx.compose.ui.platform.LocalContext.current
    
    var transactions by remember { mutableStateOf<List<InternalTransaction>>(emptyList()) }

    LaunchedEffect(Unit) {
        viewModel.refreshBalance()
        
        val settings = SettingsManager(context)
        val store = TransactionStore(context)
        val service = settings.createSwiftPayService()
        
        try {
            val local = store.transactions.first()
            val remote = service.getInternalTransactions().getOrNull() ?: emptyList()
            transactions = mergeTransactions(local, remote).take(5)
        } catch (e: Exception) {
            // ignore
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.White)) {
        // 1. Mesh Background
        MeshBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .padding(bottom = 80.dp) // Bottom nav space
        ) {
            // 2. Header: Balance and Top Actions
            HomeHeader(
                balance = balance,
                isLoading = isLoadingBalance,
                onProfileClick = { /* Navigate Profile */ },
                onHelpClick = { /* Navigate Help */ }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Primary Action Row
            PrimaryActionRow(
                onCashIn = { onLaunchMiniApp(null) },
                onSend = { onLaunchMiniApp("disbursement-page") },
                onScan = { onLaunchMiniApp("qr-page") }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 4. Secondary Tool Grid
            SecondaryToolGrid(onLaunchMiniApp)

            Spacer(modifier = Modifier.height(32.dp))

            // 5. Transaction History Section
            TransactionHistorySection(
                transactions = transactions,
                onNavigateToWallet = onNavigateToWallet
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    </div>
}

@Composable
fun MeshBackground() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .blur(80.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = FastPayMeshBlue.copy(alpha = 0.6f),
                center = Offset(size.width * 0.2f, size.height * 0.2f),
                radius = size.width * 0.6f
            )
            drawCircle(
                color = FastPayMeshTeal.copy(alpha = 0.5f),
                center = Offset(size.width * 0.8f, size.height * 0.1f),
                radius = size.width * 0.5f
            )
            drawCircle(
                color = Color.White,
                center = Offset(size.width * 0.5f, size.height * 0.4f),
                radius = size.width * 0.4f
            )
        }
    }
}

@Composable
fun HomeHeader(
    balance: Double,
    isLoading: Boolean,
    onProfileClick: () -> Unit,
    onHelpClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            if (isLoading) {
                Box(modifier = Modifier.width(120.dp).height(32.dp).clip(RoundedCornerShape(8.dp)).shimmerEffect())
            } else {
                Text(
                    text = "₱${"%,.2f".format(balance)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = FastPayTextPrimary,
                    fontSize = 26.sp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                Text(
                    text = "Total Assets",
                    style = MaterialTheme.typography.bodySmall,
                    color = FastPayTextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    Icons.Rounded.ArrowDropDown,
                    null,
                    tint = FastPayTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onProfileClick) {
                Icon(Icons.Rounded.AccountCircle, null, tint = FastPayTextPrimary, modifier = Modifier.size(28.dp))
            }
            IconButton(onClick = onHelpClick) {
                Icon(Icons.AutoMirrored.Rounded.HelpOutline, null, tint = FastPayTextPrimary, modifier = Modifier.size(26.dp))
            }
        }
    }
}

@Composable
fun PrimaryActionRow(
    onCashIn: () -> Unit,
    onSend: () -> Unit,
    onScan: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        PrimaryActionButton(icon = Icons.Rounded.AccountBalance, label = "Cash In", onClick = onCashIn)
        PrimaryActionButton(icon = Icons.Rounded.FileUpload, label = "Send", onClick = onSend)
        PrimaryActionButton(icon = Icons.Rounded.QrCodeScanner, label = "Scan to Pay", onClick = onScan)
    }
}

@Composable
fun PrimaryActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = FastPayTextPrimary, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = FastPayTextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SecondaryToolGrid(onLaunch: (String?) -> Unit) {
    val tools = listOf(
        Triple(Icons.Rounded.AccountBalance, "Payouts", "disbursement-page"),
        Triple(Icons.Rounded.AccountBox, "Virtual Acc", "vca-page"),
        Triple(Icons.Rounded.Group, "Team", "members-page"),
        Triple(Icons.Rounded.PhonelinkRing, "Buy Load", null),
        Triple(Icons.Rounded.Description, "Pay Bills", null),
        Triple(Icons.Rounded.AccountBalanceWallet, "Tap to Pay", "card-payment-page"),
        Triple(Icons.Rounded.Terminal, "API Hub", "api-docs-page"),
        Triple(Icons.Rounded.GridView, "More", null)
    )

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        for (i in 0 until 2) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                for (j in 0 until 4) {
                    val index = i * 4 + j
                    val (icon, label, path) = tools[index]
                    ToolItem(icon, label) { onLaunch(path) }
                }
            }
            if (i == 0) Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ToolItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = Color(0xFF475569), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = FastPayTextSecondary,
            textAlign = TextAlign.Center,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun TransactionHistorySection(
    transactions: List<InternalTransaction>,
    onNavigateToWallet: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onNavigateToWallet() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(4.dp).height(20.dp).clip(CircleShape).background(Color(0xFF818CF8)))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = FastPayTextPrimary
                )
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (transactions.isEmpty()) {
            Text(
                "No recent activity",
                style = MaterialTheme.typography.bodySmall,
                color = FastPayTextSecondary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            transactions.forEach { tx ->
                TransactionListItem(
                    label = if (tx.amount < 0) "Payout" else "Payment Received",
                    amount = tx.amount,
                    isOutflow = tx.amount < 0
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun TransactionListItem(label: String, amount: Double, isOutflow: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (isOutflow) Icons.Rounded.NorthEast else Icons.Rounded.SouthWest,
                    null,
                    tint = FastPayTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = FastPayTextPrimary, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "${if(isOutflow) "-" else "+"}₱${Math.abs(amount).toInt()}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = FastPayTextPrimary
        )
    }
}
