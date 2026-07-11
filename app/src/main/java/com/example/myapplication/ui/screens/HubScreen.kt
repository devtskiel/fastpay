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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HubScreen(
    onBack: () -> Unit,
    onNavigateToWebhooks: () -> Unit,
    onNavigateToInvoices: () -> Unit,
    onNavigateToVca: () -> Unit = {},
    onNavigateToMembers: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToAdminDeposits: () -> Unit = {},
    onNavigateToAuditLogs: () -> Unit = {},
    viewModel: MiniAppViewModel = com.example.myapplication.LocalMiniAppViewModel.current
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settings = remember { com.example.myapplication.data.SettingsManager(context) }
    val loggedInEmail by settings.loggedInEmail.collectAsState(initial = null)
    val isAdmin = loggedInEmail == "drltechgroup2024@gmail.com"

    Scaffold(

        topBar = {
            TopAppBar(
                title = { Text("Operations Hub", fontWeight = FontWeight.Bold) },
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
            HubItem(
                title = "Performance Dashboard",
                description = "View volume trends and revenue split.",
                icon = Icons.Rounded.BarChart,
                color = SwiftPaySuccess,
                onClick = onNavigateToDashboard
            )

            Spacer(Modifier.height(12.dp))
            Text("TREASURY SERVICES", style = MaterialTheme.typography.labelMedium, color = SwiftPayTextDim)
            
            HubItem(
                title = "Invoices",
                description = "Generate one-time payment requests for clients.",
                icon = Icons.Rounded.Description,
                color = Color(0xFF6200EE),
                onClick = onNavigateToInvoices
            )

            HubItem(
                title = "Webhooks",
                description = "Manage real-time payment notifications.",
                icon = Icons.Rounded.Webhook,
                color = Color(0xFF03DAC6),
                onClick = onNavigateToWebhooks
            )

            HubItem(
                title = "Virtual Accounts (VCA)",
                description = "Static collection accounts for bank transfers.",
                icon = Icons.Rounded.AccountBalance,
                color = Color(0xFFFF9800),
                onClick = onNavigateToVca
            )

            if (isAdmin) {
                Spacer(Modifier.height(12.dp))
                Text("ADMINISTRATION", style = MaterialTheme.typography.labelMedium, color = SwiftPayTextDim)

                HubItem(
                    title = "Members",
                    description = "Manage team access and roles.",
                    icon = Icons.Rounded.Group,
                    color = SwiftPayPrimary,
                    onClick = onNavigateToMembers
                )

                HubItem(
                    title = "Deposit Approvals",
                    description = "Review and approve manual cash-in requests.",
                    icon = Icons.Rounded.AssignmentTurnedIn,
                    color = SwiftPaySuccess,
                    onClick = onNavigateToAdminDeposits
                )

                HubItem(
                    title = "Audit Logs",
                    description = "View system activity and access history.",
                    icon = Icons.Rounded.History,
                    color = SwiftPayTextDim,
                    onClick = onNavigateToAuditLogs
                )
            }
        }
    }
}


@Composable
fun HubItem(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SwiftPaySurface,
        border = BorderStroke(1.dp, SwiftPayBorder)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(color.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SwiftPayTextPrimary)
                Text(description, style = MaterialTheme.typography.bodySmall, color = SwiftPayTextSecondary)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = SwiftPayTextDim)
        }
    }
}
