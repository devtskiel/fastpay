package com.example.myapplication.ui.screens

import android.app.Application
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.api.InternalTransaction
import com.example.myapplication.data.SettingsManager
import com.example.myapplication.data.TransactionStore
import com.example.myapplication.data.createSwiftPayService
import com.example.myapplication.data.mergeTransactions
import com.example.myapplication.ui.theme.*
import com.example.myapplication.ui.components.FastPayStatusBadge
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    modifier: Modifier = Modifier,
    viewModel: MiniAppViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var transactions by remember { mutableStateOf<List<InternalTransaction>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val context = viewModel.getApplication<Application>()
            val settings = SettingsManager(context)
            val transactionStore = TransactionStore(context)
            val service = settings.createSwiftPayService()
            
            transactionStore.transactions.collectLatest { localTransactions ->
                val remoteTransactions = service.getInternalTransactions().getOrNull() ?: emptyList()
                transactions = mergeTransactions(localTransactions, remoteTransactions)
            }
        } catch (e: Exception) {
            Log.e("WalletScreen", "Error", e)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SwiftPayBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("SETTLEMENT LEDGER", style = MaterialTheme.typography.labelLarge, color = SwiftPayTextPrimary, letterSpacing = 2.sp) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = SwiftPayBackground),
                actions = {
                    AnimatedRefreshButton(isLoading = isLoading) {
                        scope.launch {
                            isLoading = true
                            try {
                                val context = viewModel.getApplication<Application>()
                                val settings = SettingsManager(context)
                                val service = settings.createSwiftPayService()
                                val remote = service.getInternalTransactions().getOrNull() ?: emptyList()
                                transactions = remote.map { InternalTransaction(it.id ?: "", it.amount?.toDoubleOrNull() ?: 0.0, it.status ?: "", it.timestamp ?: "") }
                            } finally { isLoading = false }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
        ) {
            if (transactions.isEmpty()) {
                item { EmptyLedgerState() }
            } else {
                items(transactions) { tx ->
                    TransactionRow(tx)
                }
            }
        }
    }
}

@Composable
fun TransactionRow(tx: InternalTransaction) {
    val isExpense = tx.amount < 0
    val color = if (isExpense) SwiftPayPrimary else SwiftPaySuccess

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SwiftPaySurface,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SwiftPayBorder)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(SwiftPayCard, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isExpense) Icons.Rounded.NorthEast else Icons.Rounded.SouthWest,
                    null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tx.transactionId.take(12), style = MaterialTheme.typography.titleMedium, color = SwiftPayTextPrimary)
                Text(tx.date, style = MaterialTheme.typography.bodySmall, color = SwiftPayTextDim)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "₱${"%,.2f".format(abs(tx.amount))}",
                    style = MaterialTheme.typography.titleLarge,
                    color = SwiftPayTextPrimary
                )
                FastPayStatusBadge(status = tx.status)
            }
        }
    }
}

@Composable
fun AnimatedRefreshButton(isLoading: Boolean, onClick: () -> Unit) {
    val rotation by animateFloatAsState(
        targetValue = if (isLoading) 360f else 0f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "rotate"
    )
    IconButton(onClick = onClick) {
        Icon(Icons.Rounded.Refresh, null, tint = SwiftPayPrimary, modifier = Modifier.rotate(rotation))
    }
}

@Composable
fun EmptyLedgerState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Rounded.Inbox, null, modifier = Modifier.size(64.dp), tint = SwiftPayBorder)
        Spacer(Modifier.height(16.dp))
        Text("No settlements found", style = MaterialTheme.typography.bodyLarge, color = SwiftPayTextSecondary)
    }
}
