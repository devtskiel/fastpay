package com.example.myapplication.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.api.DepositResponse
import com.example.myapplication.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDepositsScreen(
    onBack: () -> Unit,
    viewModel: MiniAppViewModel = com.example.myapplication.LocalMiniAppViewModel.current
) {
    val deposits = viewModel.pendingDeposits

    LaunchedEffect(Unit) {
        viewModel.onFetchPendingDeposits()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deposit Approvals", fontWeight = FontWeight.Bold) },
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
        if (deposits.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.ReceiptLong, null, modifier = Modifier.size(64.dp), tint = SwiftPayTextDim)
                    Spacer(Modifier.height(16.dp))
                    Text("No pending deposits", color = SwiftPayTextDim)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(deposits) { deposit ->
                    DepositApprovalCard(
                        deposit = deposit,
                        onApprove = { viewModel.onUpdateDepositStatus(deposit.id ?: "", "APPROVED") },
                        onReject = { viewModel.onUpdateDepositStatus(deposit.id ?: "", "REJECTED") }
                    )
                }
            }
        }
    }
}

@Composable
fun DepositApprovalCard(
    deposit: DepositResponse,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SwiftPaySurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SwiftPayBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(deposit.userEmail ?: "Unknown User", fontWeight = FontWeight.Bold, color = SwiftPayTextPrimary)
                    Text("Ref: ${deposit.referenceNumber}", style = MaterialTheme.typography.labelSmall, color = SwiftPayTextDim)
                }
                Text(
                    "₱${"%,.2f".format(deposit.amount ?: 0.0)}",
                    fontWeight = FontWeight.Black,
                    color = SwiftPayPrimary,
                    fontSize = 18.sp
                )
            }
            
            Spacer(Modifier.height(12.dp))
            Text("Bank: ${deposit.bankName}", style = MaterialTheme.typography.bodySmall, color = SwiftPayTextSecondary)
            Text("Date: ${deposit.createdAt ?: "N/A"}", style = MaterialTheme.typography.bodySmall, color = SwiftPayTextDim)
            
            Spacer(Modifier.height(20.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Rounded.Close, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Reject")
                }
                
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = SwiftPaySuccess)
                ) {
                    Icon(Icons.Rounded.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Approve")
                }
            }
        }
    }
}
