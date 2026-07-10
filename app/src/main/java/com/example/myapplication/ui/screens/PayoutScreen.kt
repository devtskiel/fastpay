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
                title = { Text("Send Payout", fontWeight = FontWeight.Bold) },
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
            Text("RECIPIENT DETAILS", style = MaterialTheme.typography.labelMedium, color = SwiftPayTextDim)
            
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount (PHP)") },
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("₱ ") },
                shape = RoundedCornerShape(12.dp)
            )

            Surface(
                onClick = { showBankSheet = true },
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, SwiftPayBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Destination Bank", style = MaterialTheme.typography.labelSmall, color = SwiftPayTextDim)
                        Text(selectedBank ?: "Select Bank", style = MaterialTheme.typography.bodyLarge, color = if (selectedBank == null) SwiftPayTextDim else SwiftPayTextPrimary)
                    }
                    Icon(Icons.Rounded.KeyboardArrowDown, null, tint = SwiftPayTextDim)
                }
            }

            OutlinedTextField(
                value = accountNumber,
                onValueChange = { accountNumber = it },
                label = { Text("Account Number") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            OutlinedTextField(
                value = remarks,
                onValueChange = { remarks = it },
                label = { Text("Remarks (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
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
                Text("Confirm & Disburse", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }

    if (showBankSheet) {
        ModalBottomSheet(onDismissRequest = { showBankSheet = false }) {
            Column(modifier = Modifier.padding(bottom = 40.dp)) {
                Text("Select Bank", modifier = Modifier.padding(20.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                banks.forEach { (name, _) ->
                    ListItem(
                        headlineContent = { Text(name) },
                        leadingContent = { Icon(Icons.Rounded.AccountBalance, null, tint = SwiftPayPrimary) },
                        modifier = Modifier.clickable {
                            selectedBank = name
                            showBankSheet = false
                        }
                    )
                }
            }
        }
    }

    // Reuse overlay for processing/error dialogs
    MiniAppOverlay(viewModel)
}
