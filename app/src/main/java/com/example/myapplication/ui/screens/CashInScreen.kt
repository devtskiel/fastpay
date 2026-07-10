package com.example.myapplication.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.*
import com.example.myapplication.data.SettingsManager
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashInScreen(
    onBack: () -> Unit,
    viewModel: MiniAppViewModel = com.example.myapplication.LocalMiniAppViewModel.current
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var showSubmitDialog by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }
    var referenceNumber by remember { mutableStateOf("") }
    var selectedBankForDeposit by remember { mutableStateOf("") }
    
    val uiState = viewModel.uiState
    val isSubmitting = uiState is MiniAppUiState.Processing


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cash In / Deposit", fontWeight = FontWeight.Bold) },
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
            Text(
                "STANDARD DEPOSIT ACCOUNTS",
                style = MaterialTheme.typography.labelMedium,
                color = SwiftPayTextDim,
                letterSpacing = 1.sp
            )
            
            Text(
                "Please deposit funds to any of the accounts below. After depositing, submit your proof of payment for approval.",
                style = MaterialTheme.typography.bodySmall,
                color = SwiftPayTextSecondary
            )

            BankDepositCard(
                bankName = "Security Bank Corporation",
                accountNumber = "0000068888173",
                accountName = "Click Store",
                onCopy = {
                    clipboardManager.setText(AnnotatedString("0000068888173"))
                    Toast.makeText(context, "Account number copied", Toast.LENGTH_SHORT).show()
                },
                onSelect = {
                    selectedBankForDeposit = "Security Bank"
                    showSubmitDialog = true
                }
            )

            BankDepositCard(
                bankName = "Asia United Bank/Hellomoney",
                accountNumber = "934105321485",
                accountName = "Click Store",
                onCopy = {
                    clipboardManager.setText(AnnotatedString("934105321485"))
                    Toast.makeText(context, "Account number copied", Toast.LENGTH_SHORT).show()
                },
                onSelect = {
                    selectedBankForDeposit = "AUB/Hellomoney"
                    showSubmitDialog = true
                }
            )

            Spacer(Modifier.height(40.dp))
        }
    }

    if (showSubmitDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showSubmitDialog = false },
            title = { Text("Submit Deposit Proof") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Depositing to: $selectedBankForDeposit", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount Deposited (PHP)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = referenceNumber,
                        onValueChange = { referenceNumber = it },
                        label = { Text("Reference / Trans ID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onSubmitDeposit(
                            amount.toDoubleOrNull() ?: 0.0,
                            referenceNumber,
                            selectedBankForDeposit
                        ) {
                            Toast.makeText(context, "Deposit submitted for approval", Toast.LENGTH_LONG).show()
                            showSubmitDialog = false
                        }
                    },
                    enabled = !isSubmitting && amount.isNotBlank() && referenceNumber.isNotBlank()
                ) {
                    if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    else Text("Submit for Approval")
                }
            },

            dismissButton = {
                TextButton(onClick = { showSubmitDialog = false }, enabled = !isSubmitting) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BankDepositCard(
    bankName: String,
    accountNumber: String,
    accountName: String,
    onCopy: () -> Unit,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SwiftPaySurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SwiftPayBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(bankName, fontWeight = FontWeight.ExtraBold, color = SwiftPayTextPrimary, fontSize = 18.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = onCopy) {
                    Icon(Icons.Rounded.ContentCopy, null, tint = SwiftPayPrimary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            
            Column {
                Text("ACCOUNT NUMBER", style = MaterialTheme.typography.labelSmall, color = SwiftPayTextDim)
                Text(accountNumber, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            
            Spacer(Modifier.height(12.dp))
            
            Text("ACCOUNT NAME", style = MaterialTheme.typography.labelSmall, color = SwiftPayTextDim)
            Text(accountName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = SwiftPayTextPrimary)
            
            Spacer(Modifier.height(20.dp))
            
            Button(
                onClick = onSelect,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SwiftPayPrimary)
            ) {
                Text("I have deposited to this account", fontSize = 12.sp)
            }
        }
    }
}
