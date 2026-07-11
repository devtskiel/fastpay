package com.example.myapplication.ui.screens

import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.api.WebhookRequest
import com.example.myapplication.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebhooksScreen(
    onBack: () -> Unit,
    viewModel: MiniAppViewModel = com.example.myapplication.LocalMiniAppViewModel.current
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var webhookName by remember { mutableStateOf("") }
    var webhookUrl by remember { mutableStateOf("") }

    val webhooks by remember { derivedStateOf { viewModel.webhooks } }

    LaunchedEffect(Unit) {
        viewModel.onWebhooksRequest()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Webhooks", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SwiftPayBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = SwiftPayPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Rounded.Add, null)
            }
        },
        containerColor = SwiftPayBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            items(webhooks) { wh ->
                WebhookCard(wh) {
                    viewModel.onDeleteWebhookRequest(wh.id ?: "")
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Webhook") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = webhookName,
                        onValueChange = { webhookName = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = webhookUrl,
                        onValueChange = { webhookUrl = it },
                        label = { Text("Endpoint URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (webhookName.isNotBlank() && webhookUrl.isNotBlank()) {
                        viewModel.onAddWebhookRequest(webhookName, webhookUrl)
                        webhookName = ""
                        webhookUrl = ""
                        showAddDialog = false
                    }
                }) {
                    Text("Add", color = SwiftPayPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun WebhookCard(webhook: WebhookRequest, onDelete: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SwiftPaySurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SwiftPayBorder)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(webhook.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SwiftPayTextPrimary)
                Text(webhook.callbackUrl, style = MaterialTheme.typography.bodySmall, color = SwiftPayTextSecondary, maxLines = 1)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(SwiftPaySuccess, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text("ACTIVE", style = MaterialTheme.typography.labelSmall, color = SwiftPaySuccess, fontWeight = FontWeight.Bold)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, null, tint = Color(0xFFE91E63))
            }
        }
    }
}
