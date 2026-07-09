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
fun InvoicesScreen(
    onBack: () -> Unit,
    viewModel: MiniAppViewModel = com.example.myapplication.LocalMiniAppViewModel.current
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var amountText by remember { mutableStateOf("") }
    var descText by remember { mutableStateOf("") }

    // Mock invoices
    val invoices = remember {
        mutableStateListOf(
            InvoiceItem("INV-2024-001", 4500.0, "Consulting Services", "PAID"),
            InvoiceItem("INV-2024-002", 1250.0, "Domain Renewal", "PENDING")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invoices", fontWeight = FontWeight.Bold) },
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
                onClick = { showCreateDialog = true },
                containerColor = SwiftPayPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text("Create Invoice") }
            )
        },
        containerColor = SwiftPayBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            items(invoices) { invoice ->
                InvoiceCard(invoice)
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Invoice") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount (PHP)") },
                        modifier = Modifier.fillMaxWidth(),
                        prefix = { Text("₱ ") }
                    )
                    OutlinedTextField(
                        value = descText,
                        onValueChange = { descText = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        invoices.add(0, InvoiceItem("INV-${System.currentTimeMillis().toString().takeLast(4)}", amt, descText, "PENDING"))
                        viewModel.onCreateInvoiceRequest(amt, descText)
                        showCreateDialog = false
                    }
                }) {
                    Text("Create", color = SwiftPayPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

data class InvoiceItem(val id: String, val amount: Double, val description: String, val status: String)

@Composable
fun InvoiceCard(invoice: InvoiceItem) {
    val statusColor = if (invoice.status == "PAID") SwiftPaySuccess else SwiftPayPrimary

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SwiftPaySurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SwiftPayBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(invoice.id, fontWeight = FontWeight.Bold, color = SwiftPayTextPrimary)
                Text(
                    invoice.status,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    modifier = Modifier.background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(invoice.description, style = MaterialTheme.typography.bodyMedium, color = SwiftPayTextSecondary)
            Spacer(Modifier.height(16.dp))
            Text(
                "₱${"%,.2f".format(invoice.amount)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = SwiftPayTextPrimary
            )
        }
    }
}
