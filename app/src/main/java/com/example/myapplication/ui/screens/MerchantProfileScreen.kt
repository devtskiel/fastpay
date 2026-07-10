package com.example.myapplication.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantProfileScreen(onBack: () -> Unit) {
    var businessName by remember { mutableStateOf("Click Store") }
    var email by remember { mutableStateOf("admin@clickstore.com") }
    var phone by remember { mutableStateOf("+63 912 345 6789") }
    var address by remember { mutableStateOf("123 Business Park, Makati City, Metro Manila") }
    var tin by remember { mutableStateOf("000-123-456-000") }
    
    var isEditing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Merchant Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null) }
                },
                actions = {
                    TextButton(onClick = { isEditing = !isEditing }) {
                        Text(if (isEditing) "Save" else "Edit", color = SwiftPayPrimary, fontWeight = FontWeight.Bold)
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Header
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(SwiftPayPrimary.copy(alpha = 0.1f), CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Business, null, modifier = Modifier.size(50.dp), tint = SwiftPayPrimary)
                if (isEditing) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.CameraAlt, null, tint = Color.White)
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            Text(businessName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Verified Merchant", color = SwiftPaySuccess, style = MaterialTheme.typography.labelSmall)
            
            Spacer(Modifier.height(32.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ProfileInputField("Business Name", businessName, isEditing) { businessName = it }
                ProfileInputField("Support Email", email, isEditing) { email = it }
                ProfileInputField("Contact Number", phone, isEditing) { phone = it }
                ProfileInputField("Business Address", address, isEditing, singleLine = false) { address = it }
                ProfileInputField("TIN Number", tin, isEditing) { tin = it }
            }
            
            Spacer(Modifier.height(32.dp))
            
            Text("KYC COMPLIANCE", style = MaterialTheme.typography.labelMedium, color = SwiftPayTextDim)
            Spacer(Modifier.height(12.dp))
            
            ComplianceStatusItem("SEC/DTI Registration", "VERIFIED", SwiftPaySuccess)
            ComplianceStatusItem("BIR 2303 Certificate", "VERIFIED", SwiftPaySuccess)
            ComplianceStatusItem("Business Permit", "VERIFIED", SwiftPaySuccess)
            ComplianceStatusItem("Primary ID (Passport)", "VERIFIED", SwiftPaySuccess)
        }
    }
}

@Composable
fun ProfileInputField(label: String, value: String, isEditing: Boolean, singleLine: Boolean = true, onValueChange: (String) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = SwiftPayTextDim)
        if (isEditing) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = singleLine
            )
        } else {
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
            Divider(modifier = Modifier.padding(top = 12.dp), color = SwiftPayBorder)
        }
    }
}

@Composable
fun ComplianceStatusItem(label: String, status: String, color: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        color = SwiftPaySurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SwiftPayBorder)
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(status, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
        }
    }
}
