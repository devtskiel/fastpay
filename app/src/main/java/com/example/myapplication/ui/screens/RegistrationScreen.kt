package com.example.myapplication.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.*

enum class RegistrationStep { BASIC, BUSINESS, KYC, DOCUMENTS, REVIEW }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    onSuccess: (String, String, String, String, String, String, String, String, Boolean, Boolean, Boolean) -> Unit,
    onBack: () -> Unit,
    onNavigateToTerms: () -> Unit
) {
    var currentStep by remember { mutableStateOf(RegistrationStep.BASIC) }
    
    // Form Data
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf("") }
    var businessAddress by remember { mutableStateOf("") }
    var businessType by remember { mutableStateOf("Sole Proprietorship") }
    var idType by remember { mutableStateOf("Passport") }
    var idNumber by remember { mutableStateOf("") }
    
    var idUploaded by remember { mutableStateOf(false) }
    var selfieCaptured by remember { mutableStateOf(false) }
    var documentsUploaded by remember { mutableStateOf(false) }
    var hasAcceptedTerms by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SwiftPay Onboarding", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep == RegistrationStep.BASIC) onBack()
                        else currentStep = RegistrationStep.entries[currentStep.ordinal - 1]
                    }) {
                        Icon(Icons.Rounded.ArrowBack, null)
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
                .padding(24.dp)
        ) {
            // Step Progress
            StepIndicator(currentStep)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "RegistrationSteps"
            ) { step ->
                when (step) {
                    RegistrationStep.BASIC -> BasicInfoStep(
                        email, { email = it },
                        password, { password = it },
                        fullName, { fullName = it }
                    )
                    RegistrationStep.BUSINESS -> BusinessDetailsStep(
                        businessName, { businessName = it },
                        businessAddress, { businessAddress = it },
                        businessType, { businessType = it }
                    )
                    RegistrationStep.KYC -> KycStep(
                        idType, { idType = it },
                        idNumber, { idNumber = it },
                        idUploaded, { idUploaded = it },
                        selfieCaptured, { selfieCaptured = it }
                    )
                    RegistrationStep.DOCUMENTS -> DocumentsStep(
                        documentsUploaded, { documentsUploaded = it }
                    )
                    RegistrationStep.REVIEW -> ReviewStep(
                        email, fullName, businessName, businessAddress, businessType, idType, idNumber,
                        hasAcceptedTerms, { hasAcceptedTerms = it }, onNavigateToTerms
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Button(
                onClick = {
                    if (currentStep == RegistrationStep.REVIEW) {
                        isLoading = true
                        onSuccess(email, password, fullName, businessName, businessAddress, businessType, idType, idNumber, selfieCaptured, documentsUploaded, hasAcceptedTerms)
                    } else {
                        currentStep = RegistrationStep.entries[currentStep.ordinal + 1]
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading && isStepValid(currentStep, email, password, fullName, businessName, idNumber, idUploaded, selfieCaptured, documentsUploaded, hasAcceptedTerms),
                colors = ButtonDefaults.buttonColors(containerColor = SwiftPayPrimary)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(if (currentStep == RegistrationStep.REVIEW) "Submit Application" else "Continue", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StepIndicator(currentStep: RegistrationStep) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RegistrationStep.entries.forEachIndexed { index, step ->
            val isActive = step == currentStep
            val isCompleted = step.ordinal < currentStep.ordinal
            
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        if (isActive || isCompleted) SwiftPayPrimary else SwiftPayBorder,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                } else {
                    Text(
                        (index + 1).toString(),
                        color = if (isActive) Color.White else SwiftPayTextDim,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            
            if (index < RegistrationStep.entries.size - 1) {
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .weight(1f)
                        .background(if (isCompleted) SwiftPayPrimary else SwiftPayBorder)
                )
            }
        }
    }
}

@Composable
fun BasicInfoStep(email: String, onEmail: (String) -> Unit, pass: String, onPass: (String) -> Unit, name: String, onName: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Account Information", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Create your primary administrator account for the SwiftPay terminal.", color = SwiftPayTextSecondary)
        
        OutlinedTextField(value = name, onValueChange = onName, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        OutlinedTextField(value = email, onValueChange = onEmail, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        OutlinedTextField(value = pass, onValueChange = onPass, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), visualTransformation = PasswordVisualTransformation())
    }
}

@Composable
fun BusinessDetailsStep(name: String, onName: (String) -> Unit, addr: String, onAddr: (String) -> Unit, type: String, onType: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Business Profile", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Tell us about your registered business entity.", color = SwiftPayTextSecondary)
        
        OutlinedTextField(value = name, onValueChange = onName, label = { Text("Registered Business Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        OutlinedTextField(value = addr, onValueChange = onAddr, label = { Text("Business Address") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        
        val types = listOf("Sole Proprietorship", "Partnership", "Corporation", "Cooperative")
        Text("Business Type", style = MaterialTheme.typography.labelMedium, color = SwiftPayTextDim)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            types.forEach { t ->
                FilterChip(
                    selected = type == t,
                    onClick = { onType(t) },
                    label = { Text(t) }
                )
            }
        }
    }
}

@Composable
fun KycStep(
    type: String, onType: (String) -> Unit, 
    number: String, onNumber: (String) -> Unit, 
    idUploaded: Boolean, onIdUploaded: (Boolean) -> Unit,
    selfieCaptured: Boolean, onSelfieCaptured: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Identity Verification", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Government-mandated identity check (KYC/AML compliance).", color = SwiftPayTextSecondary)
        
        val ids = listOf("Passport", "UMID", "Driver's License", "PhilID")
        Text("Select ID Type", style = MaterialTheme.typography.labelMedium, color = SwiftPayTextDim)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ids.forEach { t ->
                FilterChip(selected = type == t, onClick = { onType(t) }, label = { Text(t) })
            }
        }
        
        OutlinedTextField(value = number, onValueChange = onNumber, label = { Text("ID Number") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                modifier = Modifier.weight(1f).height(120.dp),
                color = SwiftPaySurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (idUploaded) SwiftPaySuccess else SwiftPayBorder),
                onClick = { onIdUploaded(true) }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(if (idUploaded) Icons.Rounded.CheckCircle else Icons.Rounded.AddAPhoto, null, tint = if (idUploaded) SwiftPaySuccess else SwiftPayPrimary)
                    Text("Upload ID", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                }
            }
            Surface(
                modifier = Modifier.weight(1f).height(120.dp),
                color = SwiftPaySurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (selfieCaptured) SwiftPaySuccess else SwiftPayBorder),
                onClick = { onSelfieCaptured(true) }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(if (selfieCaptured) Icons.Rounded.CheckCircle else Icons.Rounded.Face, null, tint = if (selfieCaptured) SwiftPaySuccess else SwiftPayPrimary)
                    Text("Face Scan", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

@Composable
fun DocumentsStep(documentsUploaded: Boolean, onDocumentsUploaded: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Business Permits", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Required documents for commercial bank account settlement.", color = SwiftPayTextSecondary)
        
        DocumentUploadRow("BIR Certificate of Registration (2303)", Icons.Rounded.Description)
        DocumentUploadRow("Mayor's Business Permit", Icons.Rounded.Description)
        DocumentUploadRow("DTI or SEC Certificate", Icons.Rounded.Description)
        
        Button(
            onClick = { onDocumentsUploaded(true) }, 
            modifier = Modifier.fillMaxWidth(), 
            colors = ButtonDefaults.buttonColors(containerColor = if (documentsUploaded) SwiftPaySuccess else SwiftPayPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (documentsUploaded) "All Documents Uploaded" else "Mark as Ready", color = Color.White)
        }
    }
}

@Composable
fun DocumentUploadRow(label: String, icon: ImageVector) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SwiftPaySurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SwiftPayBorder),
        onClick = { /* Upload Simulation */ }
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = SwiftPayPrimary)
            Spacer(Modifier.width(16.dp))
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            Icon(Icons.Rounded.FileUpload, null, tint = SwiftPayTextDim, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun ReviewStep(
    email: String, name: String, bName: String, bAddr: String, bType: String, idType: String, idNum: String,
    accepted: Boolean, onAccepted: (Boolean) -> Unit, onTerms: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Final Review", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        
        ReviewItem("Merchant Admin", name)
        ReviewItem("Login Email", email)
        ReviewItem("Business Entity", bName)
        ReviewItem("Registration Type", bType)
        ReviewItem("ID Verification", "$idType ($idNum)")
        
        Spacer(Modifier.height(16.dp))
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SwiftPayPrimary.copy(alpha = 0.05f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, SwiftPayPrimary.copy(alpha = 0.1f))
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = accepted, onCheckedChange = onAccepted)
                Text(
                    text = "I hereby certify that all information provided is true and correct. I accept the SwiftPay Merchant Agreement.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable { onTerms() }.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ReviewItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = SwiftPayTextDim)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), thickness = 0.5.dp, color = SwiftPayBorder)
    }
}

fun isStepValid(
    step: RegistrationStep,
    email: String,
    pass: String,
    name: String,
    bName: String,
    idNumber: String,
    idUploaded: Boolean,
    selfieCaptured: Boolean,
    documentsUploaded: Boolean,
    terms: Boolean
): Boolean {
    return when (step) {
        RegistrationStep.BASIC -> email.contains("@") && pass.length >= 6 && name.isNotBlank()
        RegistrationStep.BUSINESS -> bName.isNotBlank()
        RegistrationStep.KYC -> idNumber.isNotBlank() && idUploaded && selfieCaptured
        RegistrationStep.DOCUMENTS -> documentsUploaded
        RegistrationStep.REVIEW -> terms
    }
}
