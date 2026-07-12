package com.example.myapplication.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(SwiftPayBackground, Color(0xFF000000))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // SwiftPay Logo/Wordmark
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.ElectricBolt,
                    contentDescription = null,
                    tint = SwiftPayPrimary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "SWIFTPAY",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 4.sp
                )
            }
            
            Text(
                text = "ENTERPRISE BANKING",
                style = MaterialTheme.typography.labelSmall,
                color = SwiftPayPrimary,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = if (isOtpSent) "Identity Verification" else if (isForgotMode) "Security Reset" else "Secure Access",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = SwiftPayTextPrimary
            )
            
            Text(
                text = if (isOtpSent) "A verification code was sent to $email" else "Authorized Merchant Access Terminal",
                style = MaterialTheme.typography.bodySmall,
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
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    leadingIcon = { Icon(Icons.Rounded.Email, null, tint = SwiftPayTextDim) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SwiftPayPrimary,
                        unfocusedBorderColor = SwiftPayBorder,
                        focusedContainerColor = SwiftPayCard.copy(alpha = 0.5f),
                        unfocusedContainerColor = SwiftPayCard.copy(alpha = 0.3f)
                    )
                )
                
                if (!isForgotMode) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = null },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = SwiftPayTextDim) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SwiftPayPrimary,
                            unfocusedBorderColor = SwiftPayBorder,
                            focusedContainerColor = SwiftPayCard.copy(alpha = 0.5f),
                            unfocusedContainerColor = SwiftPayCard.copy(alpha = 0.3f)
                        )
                    )
                    
                    TextButton(
                        onClick = { isForgotMode = true },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Forgot Password?", color = SwiftPayPrimary, style = MaterialTheme.typography.bodySmall)
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
                    label = { Text("6-Digit Verification Code") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        letterSpacing = 8.sp,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = SwiftPayPrimary
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SwiftPayPrimary,
                        unfocusedBorderColor = SwiftPayBorder,
                        focusedContainerColor = SwiftPayCard
                    )
                )
            }
            
            if (errorMessage != null) {
                Surface(
                    color = SwiftPayError.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(top = 16.dp).fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage!!,
                        color = SwiftPayError,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            SwiftPayPrimaryButton(
                text = when {
                    isForgotMode -> "Request Reset"
                    isOtpSent -> "Verify Identity"
                    else -> "Initiate Login"
                },
                onClick = {
                    isLoading = true
                    errorMessage = null
                    scope.launch {
                        if (isForgotMode) {
                            authUseCase.resetPassword(email)
                                .onSuccess {
                                    errorMessage = "If this email is registered, reset instructions were sent."
                                    isLoading = false
                                }
                                .onFailure {
                                    errorMessage = it.message ?: "Failed to request password reset"
                                    isLoading = false
                                }
                        } else if (!isOtpSent) {
                            authUseCase.login(email, password)
                                .onSuccess {
                                    isOtpSent = true
                                    otpCode = ""
                                    errorMessage = null
                                    isLoading = false
                                }
                                .onFailure {
                                    errorMessage = it.message ?: "Authentication failed"
                                    isLoading = false
                                }
                        } else {
                            authUseCase.verifyAccess(email, otpCode)
                                .onSuccess {
                                    onLoginSuccess(email)
                                    isLoading = false
                                }
                                .onFailure {
                                    errorMessage = it.message ?: "Verification failed"
                                    isLoading = false
                                }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isLoading && (
                    if (isForgotMode) email.isNotBlank() 
                    else if (isOtpSent) otpCode.length == 6 
                    else email.isNotBlank() && password.isNotBlank()
                )
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (!isOtpSent && !isForgotMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("New Enterprise Partner?", style = MaterialTheme.typography.bodySmall, color = SwiftPayTextSecondary)
                    TextButton(onClick = onNavigateToRegistration) {
                        Text("Onboard Now", color = SwiftPayPrimary, fontWeight = FontWeight.Bold)
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
                    Text("Return to Login", color = SwiftPayTextSecondary)
                }
            }
        }

        // Compliance Footer
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Regulated by the Bangko Sentral ng Pilipinas",
                style = MaterialTheme.typography.labelSmall,
                color = SwiftPayTextDim,
                fontSize = 10.sp
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onNavigateToCompliance) {
                Text("Compliance & Privacy Policy", style = MaterialTheme.typography.labelSmall, color = SwiftPayPrimary)
            }
        }
        
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SwiftPayPrimary)
            }
        }
    }
}
