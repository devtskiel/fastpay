package com.example.myapplication.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.*
import com.example.myapplication.ui.components.SwiftPayPrimaryButton
import com.example.myapplication.di.DIContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    onNavigateToRegistration: () -> Unit,
    onNavigateToCompliance: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    
    var isOtpSent by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isForgotMode by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    val authUseCase = remember { DIContainer.provideAuthenticateUseCase() }

    Scaffold(
        containerColor = SwiftPayBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = when {
                    isForgotMode -> Icons.Rounded.LockReset
                    isOtpSent -> Icons.Rounded.VerifiedUser
                    else -> Icons.Rounded.ElectricBolt
                },
                contentDescription = null,
                tint = SwiftPayPrimary,
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = when {
                    isForgotMode -> "Reset Password"
                    isOtpSent -> "Verify Identity"
                    else -> "Enterprise Access"
                },
                style = MaterialTheme.typography.headlineLarge,
                color = SwiftPayTextPrimary
            )
            
            Text(
                text = when {
                    isForgotMode -> "Enter your email to receive recovery instructions."
                    isOtpSent -> "Enter the 6-digit code sent to $email"
                    else -> "Authenticate to continue to terminal"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = SwiftPayTextSecondary,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            if (!isOtpSent) {
                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = null },
                    label = { Text("Merchant Email") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SwiftPayPrimary,
                        unfocusedBorderColor = SwiftPayBorder
                    )
                )
                
                if (!isForgotMode) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Access Key (Secret Key) Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = null },
                        label = { Text("Access Key") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SwiftPayPrimary,
                            unfocusedBorderColor = SwiftPayBorder
                        )
                    )
                    
                    TextButton(
                        onClick = { isForgotMode = true },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Forgot Key?", color = SwiftPayPrimary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                // OTP Field
                OutlinedTextField(
                    value = otpCode,
                    onValueChange = { 
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            otpCode = it
                            errorMessage = null
                        }
                    },
                    label = { Text("6-Digit OTP") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        letterSpacing = 8.sp,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SwiftPayPrimary,
                        unfocusedBorderColor = SwiftPayBorder
                    )
                )
            }
            
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            SwiftPayPrimaryButton(
                text = when {
                    isForgotMode -> "Send Reset Link"
                    isOtpSent -> "Verify & Login"
                    else -> "Initiate Access"
                },
                onClick = {
                    if (isForgotMode) {
                        // Handle forgot password
                        isForgotMode = false
                        errorMessage = "Recovery email sent."
                    } else {
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            if (!isOtpSent) {
                                authUseCase.requestAccess(email, password)
                                    .onSuccess {
                                        isOtpSent = true
                                        isLoading = false
                                    }
                                    .onFailure {
                                        errorMessage = it.message ?: "Access request failed"
                                        isLoading = false
                                    }
                            } else {
                                authUseCase.verifyAccess(email, otpCode)
                                    .onSuccess {
                                        onLoginSuccess(email)
                                    }
                                    .onFailure {
                                        errorMessage = it.message ?: "Verification failed"
                                        isLoading = false
                                    }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && (
                    if (isForgotMode) email.isNotBlank() 
                    else if (isOtpSent) otpCode.length == 6 
                    else email.isNotBlank() && password.isNotBlank()
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (!isOtpSent && !isForgotMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("New merchant?", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = onNavigateToRegistration) {
                        Text("Register Now", color = SwiftPayPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                TextButton(
                    onClick = { 
                        isOtpSent = false 
                        isForgotMode = false
                        otpCode = "" 
                    },
                    modifier = Modifier.padding(top = 8.dp),
                    enabled = !isLoading
                ) {
                    Text("Cancel", color = SwiftPayTextSecondary)
                }
            }
            
            if (!isOtpSent && !isForgotMode) {
                TextButton(onClick = onNavigateToCompliance) {
                    Text("Regulatory Compliance (BSP)", style = MaterialTheme.typography.labelSmall, color = SwiftPayTextDim)
                }
            }
            
            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(color = SwiftPayPrimary)
            }
        }
    }
}
