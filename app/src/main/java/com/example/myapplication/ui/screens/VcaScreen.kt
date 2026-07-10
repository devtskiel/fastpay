package com.example.myapplication.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VcaScreen(
    onBack: () -> Unit,
    viewModel: MiniAppViewModel = com.example.myapplication.LocalMiniAppViewModel.current
) {
    var showGenerateDialog by remember { mutableStateOf(false) }
    var accountName by remember { mutableStateOf("") }
    
    // Mock VCA
    val accounts = remember {
        mutableStateListOf(
            VcaItem("SwiftPay Operations", "77770000001", "Netbank", "ACTIVE"),
            VcaItem("Disbursement Float", "77770000002", "Netbank", "ACTIVE")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Virtual Accounts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SwiftPayBackground)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showGenerateDialog = true },
                containerColor = SwiftPayPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Rounded.AccountBalanceWallet, null) },
                text = { Text("Generate VCA") }
            )
        },
        containerColor = SwiftPayBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            items(accounts) { acc ->
                VcaCard(acc)
            }
        }
    }

    if (showGenerateDialog) {
        AlertDialog(
            onDismissRequest = { showGenerateDialog = false },
            title = { Text("Generate New Virtual Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enter a name to identify this collection account.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = accountName,
                        onValueChange = { accountName = it },
                        label = { Text("Account Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (accountName.isNotBlank()) {
                        accounts.add(0, VcaItem(accountName, "7777" + (1000..9999).random().toString(), "Netbank", "PENDING"))
                        viewModel.onGenerateVcaRequest(accountName)
                        showGenerateDialog = false
                    }
                }) {
                    Text("Generate", color = SwiftPayPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGenerateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

data class VcaItem(val name: String, val number: String, val bank: String, val status: String)

@Composable
fun VcaCard(account: VcaItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SwiftPaySurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SwiftPayBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(account.name, fontWeight = FontWeight.Bold, color = SwiftPayTextPrimary)
                Text(account.status, style = MaterialTheme.typography.labelSmall, color = SwiftPaySuccess)
            }
            Spacer(Modifier.height(16.dp))
            Text("ACCOUNT NUMBER", style = MaterialTheme.typography.labelSmall, color = SwiftPayTextDim)
            Text(account.number, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))
            Text(account.bank, style = MaterialTheme.typography.bodySmall, color = SwiftPayTextSecondary)
        }
    }
}
