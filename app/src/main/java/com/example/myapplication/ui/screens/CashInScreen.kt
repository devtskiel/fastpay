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
import androidx.compose.runtime.Composable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashInScreen(onBack: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

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
                "Please deposit funds to any of the accounts below. Once deposited, your balance will be updated automatically.",
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
                }
            )

            BankDepositCard(
                bankName = "Asia United Bank",
                accountNumber = "934105321485",
                accountName = "Click Store",
                onCopy = {
                    clipboardManager.setText(AnnotatedString("934105321485"))
                    Toast.makeText(context, "Account number copied", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(Modifier.height(20.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SwiftPayPrimary.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SwiftPayPrimary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.ContentCopy, null, tint = SwiftPayPrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Click the copy icon to copy the account number.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SwiftPayPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun BankDepositCard(
    bankName: String,
    accountNumber: String,
    accountName: String,
    onCopy: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SwiftPaySurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SwiftPayBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(bankName, fontWeight = FontWeight.ExtraBold, color = SwiftPayTextPrimary, fontSize = 18.sp)
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("ACCOUNT NUMBER", style = MaterialTheme.typography.labelSmall, color = SwiftPayTextDim)
                    Text(accountNumber, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                IconButton(onClick = onCopy) {
                    Icon(Icons.Rounded.ContentCopy, null, tint = SwiftPayPrimary)
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            Text("ACCOUNT NAME", style = MaterialTheme.typography.labelSmall, color = SwiftPayTextDim)
            Text(accountName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = SwiftPayTextPrimary)
        }
    }
}
