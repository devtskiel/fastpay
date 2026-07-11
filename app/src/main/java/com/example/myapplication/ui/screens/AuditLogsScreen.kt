package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.*

data class AuditLog(
    val id: String,
    val action: String,
    val description: String,
    val timestamp: String,
    val user: String,
    val status: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogsScreen(
    onBack: () -> Unit,
    viewModel: MiniAppViewModel = com.example.myapplication.LocalMiniAppViewModel.current
) {
    val transactions by viewModel.transactions.collectAsState()
    
    val auditLogs = remember(transactions) {
        transactions.mapIndexed { index, tx ->
            AuditLog(
                id = tx.transactionId,
                action = if (tx.amount > 0) "Cash-In" else "Payout",
                description = tx.description,
                timestamp = tx.date,
                user = "System",
                status = tx.status
            )
        }.take(100) // Show last 100 transactions
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audit Logs", fontWeight = FontWeight.Bold) },
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
        if (auditLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.History,
                        null,
                        modifier = Modifier.size(64.dp),
                        tint = SwiftPayTextDim
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("No audit logs available", color = SwiftPayTextDim)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(auditLogs) { log ->
                    AuditLogCard(log)
                }
            }
        }
    }
}

@Composable
fun AuditLogCard(log: AuditLog) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SwiftPaySurface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SwiftPayBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        when {
                            log.status == "SUCCESS" -> SwiftPaySuccess.copy(alpha = 0.1f)
                            log.status == "PENDING" -> SwiftPayPrimary.copy(alpha = 0.1f)
                            else -> Color(0xFFE91E63).copy(alpha = 0.1f)
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.History,
                    null,
                    modifier = Modifier.size(20.dp),
                    tint = when {
                        log.status == "SUCCESS" -> SwiftPaySuccess
                        log.status == "PENDING" -> SwiftPayPrimary
                        else -> Color(0xFFE91E63)
                    }
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    log.action,
                    fontWeight = FontWeight.SemiBold,
                    color = SwiftPayTextPrimary,
                    fontSize = 14.sp
                )
                Text(
                    log.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = SwiftPayTextSecondary,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    log.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = SwiftPayTextDim,
                    fontSize = 11.sp
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when {
                    log.status == "SUCCESS" -> SwiftPaySuccess.copy(alpha = 0.1f)
                    log.status == "PENDING" -> SwiftPayPrimary.copy(alpha = 0.1f)
                    else -> Color(0xFFE91E63).copy(alpha = 0.1f)
                }
            ) {
                Text(
                    log.status,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        log.status == "SUCCESS" -> SwiftPaySuccess
                        log.status == "PENDING" -> SwiftPayPrimary
                        else -> Color(0xFFE91E63)
                    },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp
                )
            }
        }
    }
}
