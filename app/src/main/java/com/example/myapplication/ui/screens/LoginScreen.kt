package com.example.myapplication.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.components.FastPayLogo
import com.example.myapplication.data.SettingsManager
import com.example.myapplication.data.SessionManager
import com.example.myapplication.data.LoginApprovalManager
import com.example.myapplication.data.createMayaService
import com.example.myapplication.data.ApprovalService
import com.example.myapplication.ui.theme.*
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

enum class LoginStage {
    Landing, Login, Otp, WaitingApproval
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: MiniAppViewModel = viewModel()
) {
    var stage by remember { mutableStateOf(LoginStage.Landing) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var generatedCode by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var resendTimer by remember { mutableIntStateOf(0) }
    
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val settings = remember(context) { SettingsManager(context) }
    val sessionManager = remember(context) { SessionManager(context, settings) }
    val approvalService: ApprovalService = remember { ApprovalService() }
    var approvalRequestId by remember { mutableStateOf<String?>(null) }

    // Poll approval status when an approval has been requested
    LaunchedEffect(approvalRequestId) {
        val id = approvalRequestId
        if (id.isNullOrBlank()) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(3000)
            try {
                val approval = approvalService.getApproval(id)
                if (approval != null) {
                    when (approval.status.uppercase()) {
                        "APPROVED" -> {
                            // create session and login
                            settings.setLoggedIn(username.trim(), true)
                            sessionManager.createSession(username.trim(), isPrimary = false)
                            approvalRequestId = null
                            onLoginSuccess()
                            break
                        }
                        "DENIED" -> {
                            errorMessage = "Login denied by trusted device"
                            approvalRequestId = null
                            stage = LoginStage.Login
                            break
                        }
                        "EXPIRED" -> {
                            errorMessage = "Approval request expired"
                            approvalRequestId = null
                            stage = LoginStage.Login
                            break
                        }
                        else -> {
                            // still pending
                        }
                    }
                } else {
                    // network or server error - continue polling
                }
            } catch (e: Exception) {
                // ignore and retry
            }
        }
    }

    LaunchedEffect(resendTimer) {
        if (resendTimer > 0) {
            kotlinx.coroutines.delay(1000)
            resendTimer--
        }
    }

    fun triggerCustomOtp() {
        isLoading = true
        val cleanEmail = username.trim().lowercase()
        val newCode = (100000..999999).random().toString()
        generatedCode = newCode

        scope.launch {
            val mayaService = settings.createMayaService()
            val result = mayaService.sendCustomOtp(cleanEmail, newCode)
            result.onSuccess {
                stage = LoginStage.Otp
                resendTimer = 60
                isLoading = false
                errorMessage = null
            }.onFailure {
                errorMessage = it.message ?: "Failed to send verification code"
                isLoading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = stage,
            transitionSpec = {
                (fadeIn(tween(400)) + slideInHorizontally(tween(400)) { it / 3 }) togetherWith
                    (fadeOut(tween(300)) + slideOutHorizontally(tween(300)) { -it / 3 })
            },
            label = "LoginStageTransition"
        ) { currentStage ->
            when (currentStage) {
                LoginStage.Landing -> {
                    LandingContent(
                        onLoginClick = { stage = LoginStage.Login }
                    )
                }
                LoginStage.Login -> {
                    LoginContent(
                        username = username,
                        password = password,
                        isPasswordVisible = isPasswordVisible,
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        onUsernameChange = { username = it; errorMessage = null },
                        onPasswordChange = { password = it; errorMessage = null },
                        onTogglePassword = { isPasswordVisible = !isPasswordVisible },
                        onBack = { stage = LoginStage.Landing },
                        onLoginClick = {
                            if (username.isNotBlank() && password.isNotBlank()) {
                                isLoading = true
                                scope.launch {
                                    val mayaService = settings.createMayaService()
                                    val result = mayaService.onlineLogin(username.trim(), password)
                                    result.onSuccess {
                                        // If approval server is configured, use approval flow
                                        val serverUrl = com.example.myapplication.BuildConfig.APP_SERVER_URL
                                        if (!serverUrl.isNullOrBlank()) {
                                            // create approval request and wait
                                            val deviceId = android.os.Build.DEVICE ?: "unknown_device"
                                            val deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
                                            val created = approvalService.createApproval(username.trim(), deviceId, deviceName)
                                            if (created != null) {
                                                approvalRequestId = created.requestId
                                                stage = LoginStage.WaitingApproval
                                                isLoading = false
                                            } else {
                                                // fallback to sending OTP
                                                triggerCustomOtp()
                                            }
                                        } else {
                                            triggerCustomOtp()
                                        }
                                    }.onFailure {
                                        errorMessage = it.message ?: "Invalid merchant credentials"
                                        isLoading = false
                                    }
                                }
                            } else {
                                errorMessage = "Please enter both credentials"
                            }
                        }
                    )
                }
                LoginStage.Otp -> {
                    OtpContent(
                        otpCode = otpCode,
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        resendTimer = resendTimer,
                        onOtpChange = { otpCode = it; errorMessage = null },
                        onBack = { stage = LoginStage.Login },
                        onResendClick = { triggerCustomOtp() },
                        onVerifyClick = {
                            if (otpCode == generatedCode) {
                                scope.launch {
                                    settings.setLoggedIn(username.trim(), true)
                                    // Create multi-device session
                                    sessionManager.createSession(username.trim(), isPrimary = true)
                                    onLoginSuccess()
                                }
                            } else {
                                errorMessage = "Invalid verification code"
                            }
                        }
                    )
                }
                LoginStage.WaitingApproval -> {
                    WaitingApprovalContent(
                        onCancel = {
                            approvalRequestId = null
                            stage = LoginStage.Login
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WaitingApprovalContent(onCancel: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("正在等待您的受信任设备的授权...", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onCancel) { Text("取消") }
    }
}

@Composable
fun LandingContent(onLoginClick: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(800, easing = LinearOutSlowInEasing),
        label = "contentAlpha"
    )
    val contentOffset by animateFloatAsState(
        targetValue = if (visible) 0f else 50f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 100f),
        label = "contentOffset"
    )

    val isDarkTheme = isSystemInDarkTheme()
    val bgColors = if (isDarkTheme) {
        listOf(Color(0xFF0F172A), Color(0xFF020617)) // Deeper dark slate
    } else {
        listOf(FastPayNavy, FastPayDarkNavy) // Original navy for light mode
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(bgColors))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp, vertical = 64.dp)
                .graphicsLayer {
                    alpha = contentAlpha
                    translationY = contentOffset
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Logo at the top
            FastPayLogo(scale = 1.5f, isDark = true)

            Spacer(modifier = Modifier.weight(1f))

            // Center: Tagline with enhanced styling
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "随时随地，\n开启收款。",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 42.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "二维码 · NFC · 支付链接",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom: Log In Button only
            Button(
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FastPayBlue),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Login, null, modifier = Modifier.size(20.dp), tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("登录", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun AnimatedLandingFeatureIcon(icon: ImageVector, label: String, visible: Boolean, delayMillis: Int) {
    var itemVisible by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            kotlinx.coroutines.delay(300L + delayMillis)
            itemVisible = true
        }
    }
    val scale by animateFloatAsState(
        targetValue = if (itemVisible) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "featureScale"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.07f),
            modifier = Modifier.size(58.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = FastPayBlue, modifier = Modifier.size(26.dp))
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

@Composable
fun LoginContent(
    username: String,
    password: String,
    isPasswordVisible: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onBack: () -> Unit,
    onLoginClick: () -> Unit
) {
    val bgColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val secondaryTextColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.Close, null, tint = textColor)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text("帮助中心", color = textColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelLarge)
        }

        Spacer(modifier = Modifier.height(40.dp))
        FastPayLogo(scale = 1.3f, isDark = isSystemInDarkTheme())

        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = "商户登录",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = textColor
        )
        Text(
            text = "输入您的凭据以继续",
            style = MaterialTheme.typography.bodyMedium,
            color = secondaryTextColor
        )

        Spacer(modifier = Modifier.height(52.dp))

        CustomTextField(
            value = username,
            onValueChange = onUsernameChange,
            placeholder = "电子邮件或手机号码",
            leadingIcon = Icons.Rounded.AlternateEmail,
            isDarkTheme = isSystemInDarkTheme()
        )

        Spacer(modifier = Modifier.height(16.dp))

        CustomTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = "密码",
            leadingIcon = Icons.Rounded.Lock,
            isPassword = true,
            isPasswordVisible = isPasswordVisible,
            onTogglePassword = onTogglePassword,
            isDarkTheme = isSystemInDarkTheme()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                text = "忘记密码？",
                color = FastPayBlue,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable { /* Handle forgot */ }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if (errorMessage != null) {
            Surface(
                color = Color(0xFFFFEBEE),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFD32F2F),
                    modifier = Modifier.padding(14.dp),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = onLoginClick,
            modifier = Modifier.fillMaxWidth().height(62.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = FastPayBlue,
                disabledContainerColor = FastPayBlue.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(18.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("登录账户", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    isDarkTheme: Boolean = false
) {
    val isFocused = remember { mutableStateOf(false) }

    val bgColor = if (isDarkTheme) Color(0xFF1E1E1E) else FastPayTextField
    val iconColor = if (isDarkTheme) Color.LightGray.copy(alpha = 0.7f) else FastPayNavy.copy(alpha = 0.4f)
    val textColor = if (isDarkTheme) Color.White else FastPayNavy

    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = if (isDarkTheme) Color.Gray else Color.Gray.copy(alpha = 0.5f)) },
        leadingIcon = {
            Icon(
                leadingIcon,
                null,
                tint = if (isFocused.value) FastPayBlue else iconColor,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = onTogglePassword ?: {}) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused.value = it.isFocused },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = bgColor,
            unfocusedContainerColor = bgColor,
            disabledContainerColor = bgColor,
            focusedIndicatorColor = FastPayBlue,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = FastPayBlue,
            focusedTextColor = textColor,
            unfocusedTextColor = textColor
        ),
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        keyboardOptions = if (isPassword) KeyboardOptions(keyboardType = KeyboardType.Password) else KeyboardOptions.Default
    )
}

@Composable
fun OtpContent(
    otpCode: String,
    isLoading: Boolean,
    errorMessage: String?,
    resendTimer: Int,
    onOtpChange: (String) -> Unit,
    onBack: () -> Unit,
    onResendClick: () -> Unit,
    onVerifyClick: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val bgColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val secondaryTextColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    val inputBgColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color(0xFFF2F4F7)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = textColor)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Surface(
            shape = CircleShape,
            color = if (isDarkTheme) FastPayBlue.copy(alpha = 0.15f) else FastPayNavy.copy(alpha = 0.05f),
            modifier = Modifier.size(84.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Security, null, tint = if (isDarkTheme) FastPayBlue else FastPayNavy, modifier = Modifier.size(42.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("安全验证", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = textColor)
        Text(
            text = "我们向您的邮箱发送了一个 6 位数代码。\n请检查您的收件箱。",
            color = secondaryTextColor,
            modifier = Modifier.padding(top = 12.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(56.dp))

        TextField(
            value = otpCode,
            onValueChange = onOtpChange,
            placeholder = { Text("000000", color = Color.Gray.copy(alpha = 0.5f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = inputBgColor,
                unfocusedContainerColor = inputBgColor,
                focusedIndicatorColor = FastPayBlue,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor
            ),
            shape = RoundedCornerShape(18.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                textAlign = TextAlign.Center,
                letterSpacing = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor
            )
        )

        if (errorMessage != null) {
            Text(errorMessage, color = Color(0xFFEF5350), modifier = Modifier.padding(top = 20.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onVerifyClick,
            modifier = Modifier.fillMaxWidth().height(62.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FastPayBlue),
            shape = RoundedCornerShape(18.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("验证并访问门户", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("没有收到代码？ ", color = secondaryTextColor, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = if (resendTimer > 0) "等待 ${resendTimer}秒" else "现在重发",
                color = FastPayBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(enabled = resendTimer == 0) { onResendClick() }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
