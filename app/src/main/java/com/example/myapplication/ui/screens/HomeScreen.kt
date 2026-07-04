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

    LaunchedEffect(Unit) {
        viewModel.refreshBalance()
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
                onSend = {
                    android.widget.Toast.makeText(
                        context,
                        "即时提现提示：您的余额必须至少达到 ₱100,000.00 才能获得即时结算资格。",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                },
                onScan = { onLaunchMiniApp("qr-page") }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 4. Secondary Tool Grid
            SecondaryToolGrid(onLaunchMiniApp)

            Spacer(modifier = Modifier.height(24.dp))

            // 5. Promotional Banner
            PromotionalBanner()

            Spacer(modifier = Modifier.height(24.dp))

            // 6. Transaction History Section
            TransactionHistorySection(onNavigateToWallet)

            Spacer(modifier = Modifier.height(24.dp))

            // 7. Rewards Hub Section
            RewardsHubSection()
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
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
                    text = "总资产",
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
        PrimaryActionButton(icon = Icons.Rounded.AccountBalance, label = "充值", onClick = onCashIn)
        PrimaryActionButton(icon = Icons.Rounded.FileUpload, label = "转账", onClick = onSend)
        PrimaryActionButton(icon = Icons.Rounded.QrCodeScanner, label = "扫码支付", onClick = onScan)
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
        Triple(Icons.Rounded.Description, "缴纳账单", null),
        Triple(Icons.Rounded.PhonelinkRing, "购买话费", null),
        Triple(Icons.Rounded.Token, "购买加密货币", null),
        Triple(Icons.AutoMirrored.Rounded.CompareArrows, "现货交易", null),
        Triple(Icons.Rounded.CardGiftcard, "奖励中心", null),
        Triple(Icons.Rounded.AccountBalanceWallet, "触碰支付", "card-payment-page"),
        Triple(Icons.Rounded.Terminal, "API 中心", "api-docs-page"),
        Triple(Icons.Rounded.GridView, "更多", null)
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
fun PromotionalBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFEFF6FF)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
            // Placeholder for background illustration
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(color = Color(0xFFDBEAFE), center = Offset(size.width * 0.9f, size.height * 0.5f), radius = size.width * 0.3f)
            }
            
            Column(modifier = Modifier.padding(20.dp).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                Text(
                    text = "coins.ph",
                    style = MaterialTheme.typography.labelSmall,
                    color = FastPayTextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "会员忠诚计划\n现已上线！",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = FastPayTextPrimary,
                    lineHeight = 24.sp
                )
                Text(
                    text = "由 ShareTreats 提供支持",
                    style = MaterialTheme.typography.labelSmall,
                    color = FastPayTextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Button(
                    onClick = { /* Get Started */ },
                    colors = ButtonDefaults.buttonColors(containerColor = FastPayActionIcon),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("立即开始", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            // Image overlay (donut/coins from screenshot)
            Box(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp)) {
                Icon(Icons.Rounded.DonutLarge, null, tint = Color(0xFFF97316).copy(alpha = 0.8f), modifier = Modifier.size(80.dp))
            }
        }
    }
}

@Composable
fun TransactionHistorySection(onClick: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(4.dp).height(20.dp).clip(CircleShape).background(Color(0xFF818CF8)))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "交易历史",
                    style = MaterialTheme.typography.titleMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                    fontWeight = FontWeight.ExtraBold,
                    color = FastPayTextPrimary
                )
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sample History Item 1
        TransactionListItem(label = "扫码支付", amount = -1.0, isOutflow = true)
        Spacer(modifier = Modifier.height(12.dp))
        // Sample History Item 2
        TransactionListItem(label = "扫码支付", amount = -300.0, isOutflow = true)
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Featured Payment Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Coins.ph 支付", style = MaterialTheme.typography.labelSmall, color = FastPayTextSecondary)
                    Text(
                        "使用 Coins.ph QR\n实现快速便捷的支付",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = FastPayTextPrimary,
                        lineHeight = 22.sp
                )
            }
            Icon(Icons.AutoMirrored.Rounded.Send, null, tint = FastPayActionIcon, modifier = Modifier.size(48.dp).alpha(0.2f))
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

@Composable
fun RewardsHubSection() {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(4.dp).height(20.dp).clip(CircleShape).background(Color(0xFF2DD4BF)))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "奖励中心",
                    style = MaterialTheme.typography.titleMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                    fontWeight = FontWeight.ExtraBold,
                    color = FastPayTextPrimary
                )
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFF0FDFA)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("积分奖励", style = MaterialTheme.typography.labelSmall, color = Color(0xFF0D9488), fontWeight = FontWeight.Bold)
                Text(
                    "完成任务即可在积分商店\n获得史诗级奖励。",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FastPayTextPrimary,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Icon(Icons.Rounded.CardGiftcard, null, tint = Color(0xFF2DD4BF), modifier = Modifier.size(60.dp).alpha(0.3f).align(Alignment.End))
            }
        }
    }
}
