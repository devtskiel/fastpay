package com.example.myapplication.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun PayoutScreen(
    onBack: () -> Unit,
    viewModel: MiniAppViewModel = com.example.myapplication.LocalMiniAppViewModel.current
) {
    var amount by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var selectedBank by remember { mutableStateOf<String?>(null) }
    var remarks by remember { mutableStateOf("") }
    
    var showBankSheet by remember { mutableStateOf(false) }
    
    val dynamicBanks by viewModel::institutions
    
    LaunchedEffect(Unit) {
        viewModel.onBanksRequest()
    }

    val banks = if (dynamicBanks.isNotEmpty()) {
        dynamicBanks.map { it.name to it.code }
    } else {
        listOf(
            "GCASH" to "GXCPHM2XXX",
            "MAYA" to "MYDBPHM2XXX",
            "BDO" to "BNORPHMMXXX",
            "BPI" to "BOPIPHMMXXX",
            "RCBC" to "RCBCPHMMXXX"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transfer Funds", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("BENEFICIARY INFORMATION", style = MaterialTheme.typography.labelMedium, color = SwiftPayTextDim, letterSpacing = 1.sp)
            
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Disbursement Amount (PHP)") },
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("₱ ") },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SwiftPayPrimary,
                    unfocusedBorderColor = SwiftPayBorder
                )
            )

            Surface(
                onClick = { showBankSheet = true },
                shape = RoundedCornerShape(12.dp),
                color = SwiftPayCard.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, SwiftPayBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Destination Institution", style = MaterialTheme.typography.labelSmall, color = SwiftPayTextDim)
                        Text(selectedBank ?: "Select Bank or E-Wallet", style = MaterialTheme.typography.bodyLarge, color = if (selectedBank == null) SwiftPayTextDim else SwiftPayTextPrimary, fontWeight = FontWeight.Bold)
                    }
                    Icon(Icons.Rounded.KeyboardArrowDown, null, tint = SwiftPayTextDim)
                }
            }

            OutlinedTextField(
                value = accountNumber,
                onValueChange = { accountNumber = it },
                label = { Text("Account Number") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SwiftPayPrimary,
                    unfocusedBorderColor = SwiftPayBorder
                )
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SwiftPayPrimary,
                        unfocusedBorderColor = SwiftPayBorder
                    )
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SwiftPayPrimary,
                        unfocusedBorderColor = SwiftPayBorder
                    )
                )
            }

            OutlinedTextField(
                value = remarks,
                onValueChange = { remarks = it },
                label = { Text("Purpose of Transfer (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SwiftPayPrimary,
                    unfocusedBorderColor = SwiftPayBorder
                )
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val code = banks.find { it.first == selectedBank }?.second
                    viewModel.onDisburseRequest(
                        amount.toDoubleOrNull() ?: 0.0,
                        accountNumber,
                        firstName,
                        lastName,
                        bankCode = code,
                        remarks = remarks
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SwiftPayPrimary),
                enabled = amount.toDoubleOrNull() != null && accountNumber.isNotBlank() && selectedBank != null
            ) {
                Text("Authorize Disbursement", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            
            Text(
                "Funds will be deducted from your settled balance immediately upon authorization.",
                style = MaterialTheme.typography.labelSmall,
                color = SwiftPayTextDim,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            )
        }
    }

    if (showBankSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBankSheet = false },
            containerColor = SwiftPaySurface
        ) {
            Column(modifier = Modifier.padding(bottom = 40.dp)) {
                Text("Select Institution", modifier = Modifier.padding(20.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = SwiftPayTextPrimary)
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(banks) { (name, _) ->
                        ListItem(
                            headlineContent = { Text(name, color = SwiftPayTextPrimary) },
                            leadingContent = { 
                                Box(modifier = Modifier.size(32.dp).background(SwiftPayPrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.AccountBalance, null, tint = SwiftPayPrimary, modifier = Modifier.size(18.dp)) 
                                }
                            },
                            modifier = Modifier.clickable {
                                selectedBank = name
                                showBankSheet = false
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }
    }

    // Reuse overlay for processing/error dialogs
    MiniAppOverlay(viewModel)
}
