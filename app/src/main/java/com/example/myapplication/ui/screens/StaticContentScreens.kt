package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsAndConditionsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terms and Conditions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null) }
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
                .padding(24.dp)
        ) {
            Text(
                "SwiftPay Service Agreement",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = SwiftPayPrimary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "1. Introduction\n" +
                "Welcome to SwiftPay. By registering for a merchant account, you agree to comply with and be bound by the following terms and conditions of use, which together with our privacy policy govern SwiftPay's relationship with you in relation to this terminal.\n\n" +
                "2. Eligibility\n" +
                "To be eligible for a merchant account, you must be a legally registered business entity in the Philippines and pass our Know Your Customer (KYC) requirements.\n\n" +
                "3. Transaction Processing\n" +
                "SwiftPay acts as a payment aggregator. We process payments via Netbank and other financial partners. We reserve the right to hold funds for risk management purposes.\n\n" +
                "4. Prohibited Activities\n" +
                "You may not use SwiftPay for any illegal transactions, including but not limited to unauthorized gambling, illegal drugs, or fraudulent activities.\n\n" +
                "5. Fees\n" +
                "Processing fees are deducted from each successful transaction. Fees are subject to change with prior notice.\n\n" +
                "6. Data Privacy\n" +
                "We collect and process your data in accordance with the Data Privacy Act of 2012. Your business information is used solely for compliance and transaction processing.",
                style = MaterialTheme.typography.bodyMedium,
                color = SwiftPayTextPrimary,
                lineHeight = 24.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplianceScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Regulatory Compliance", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null) }
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
                .padding(24.dp)
        ) {
            Text(
                "Regulatory Oversight",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = SwiftPayPrimary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "SwiftPay operations are conducted in partnership with Netbank, a Bangko Sentral ng Pilipinas (BSP) regulated financial institution.\n\n" +
                "Our platform adheres to the following regulatory frameworks:\n\n" +
                "• AML/CTF Compliance: We implement strict Anti-Money Laundering and Counter-Terrorism Financing protocols.\n\n" +
                "• Data Privacy Act: Full compliance with the Data Privacy Act of 2012 (RA 10173).\n\n" +
                "• PCI-DSS: Our payment processing partners maintain PCI-DSS Level 1 certification for secure card handling.\n\n" +
                "• BSP Regulations: Adherence to circulars regarding electronic money and digital payments.",
                style = MaterialTheme.typography.bodyMedium,
                color = SwiftPayTextPrimary,
                lineHeight = 24.sp
            )
        }
    }
}
