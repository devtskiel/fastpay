package com.example.myapplication

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.example.myapplication.navigation.Route
import com.example.myapplication.ui.screens.HomeScreen
import com.example.myapplication.ui.screens.LoginScreen
import com.example.myapplication.ui.screens.ProfileScreen
import com.example.myapplication.ui.screens.WalletScreen
import com.example.myapplication.ui.screens.ApiKeysScreen
import com.example.myapplication.ui.screens.ApiDocsScreen
import com.example.myapplication.di.DIContainer
import com.example.myapplication.ui.theme.SwiftPayTheme
import com.example.myapplication.ui.theme.SwiftPayPrimary
import com.example.myapplication.data.SettingsManager
import com.example.myapplication.data.SessionManager
import com.example.myapplication.data.createSwiftPayService
import com.example.myapplication.data.ApprovalService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import android.widget.Toast
import com.example.myapplication.util.DeepLinkBus
import androidx.compose.ui.platform.LocalContext
import com.example.myapplication.util.DeepLinkExtractor
import com.example.myapplication.util.DebugLogger
import com.example.myapplication.ui.screens.MiniAppViewModel
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope

val LocalMiniAppViewModel = compositionLocalOf<MiniAppViewModel> { error("No MiniAppViewModel provided") }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Security: Prevent screenshots and screen recording of sensitive payment info
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        
        com.example.myapplication.di.DIContainer.initialize(applicationContext)
        
        setContent {
            val settings = remember { SettingsManager(application) }
            val themeMode by settings.themeMode.collectAsState(initial = "SYSTEM")
            val isDarkTheme = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            SwiftPayTheme(darkTheme = isDarkTheme) {
                SwiftPayApp()
            }
        }
        intent?.extras?.let { extras ->
            val map = DeepLinkExtractor.bundleToMap(extras)
            if (map != null) {
                lifecycleScope.launch {
                    DeepLinkBus.flow.emit(map)
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val map = DeepLinkExtractor.bundleToMap(intent.extras)
        if (map != null) {
            lifecycleScope.launch {
                DeepLinkBus.flow.emit(map)
            }
        }
    }
}

val LocalNavController = compositionLocalOf<NavController> { error("No NavController provided") }

interface NavController {
    fun navigate(route: Route)
    fun pop()
    fun logout()
}

@Composable
fun SwiftPayApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val settings = remember { SettingsManager(context) }
    val sessionManager = remember { SessionManager(context, settings) }
    val approvalService: ApprovalService = remember { ApprovalService() }
    val isLoggedIn by settings.isLoggedIn.collectAsState(initial = false)
    val loggedInEmail by settings.loggedInEmail.collectAsState(initial = null)

    DebugLogger.logAuthCheck(isLoggedIn, loggedInEmail)
    DebugLogger.logDeviceInfo()

    val backStack = remember { mutableStateListOf<Route>(if (isLoggedIn) Route.Home else Route.Login) }
    val currentRoute = backStack.lastOrNull() ?: Route.Login
    
    val restoredEmail = loggedInEmail
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!isLoggedIn) {
            val currentSession = sessionManager.getCurrentSessionToken()
            if (currentSession != null && System.currentTimeMillis() < currentSession.expiresAt) {
                settings.setLoggedIn(currentSession.email, true)
                DebugLogger.logSessionRestored(currentSession.email, currentSession.deviceId)
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            val sess = sessionManager.getCurrentSessionToken()
            if (sess != null && sess.isPrimaryDevice) {
                while (true) {
                    kotlinx.coroutines.delay(3000)
                    try {
                        val pending = approvalService.getApprovals(sess.email)
                        if (pending.isNotEmpty()) {
                            val req = pending.first()
                            DebugLogger.logApprovalRequested(req.email, req.deviceId)
                        }
                    } catch (e: Exception) {
                    }
                }
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(isLoggedIn) {
        if (isLoggedIn && backStack.contains(Route.Login)) {
            backStack.clear()
            backStack.add(Route.Home)
        } else if (!isLoggedIn && !backStack.contains(Route.Login)) {
            backStack.clear()
            backStack.add(Route.Login)
        }
    }

    val miniAppViewModel: MiniAppViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    
    // Security: Idle Timeout Session Management
    androidx.compose.runtime.DisposableEffect(Unit) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                // App moved to background - could store timestamp
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_START) {
                // App moved to foreground - could check timestamp and force logout if too long
            }
        }
        // This is a simplified placeholder for session timeout logic
        onDispose { }
    }

    val navController = remember {
        object : NavController {
            override fun navigate(route: Route) {
                if (route == Route.Home) {
                    backStack.clear()
                    backStack.add(Route.Home)
                } else {
                    backStack.removeAll { it == route }
                    backStack.add(route)
                }
            }
            override fun pop() {
                if (backStack.size > 1) backStack.removeAt(backStack.size - 1)
            }
            override fun logout() {
                scope.launch {
                    settings.clearSession()
                    sessionManager.clearSession()
                    backStack.clear()
                    backStack.add(Route.Login)
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalNavController provides navController,
        LocalMiniAppViewModel provides miniAppViewModel
    ) {
        val context = LocalContext.current
        androidx.compose.runtime.LaunchedEffect(Unit) {
            DeepLinkBus.flow.collectLatest { payload ->
                if (payload != null) {
                    navController.navigate(Route.Home)
                    val (linkId, status) = DeepLinkExtractor.extractPaymentInfo(payload)
                    if (linkId.isNotBlank()) {
                        miniAppViewModel.handleDeepLink(linkId, status.ifBlank { null })
                    }
                    if (linkId.isNotEmpty() || status.isNotEmpty()) {
                        val message = DeepLinkExtractor.formatPaymentMessage(linkId, status)
                        android.widget.Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        if (currentRoute == Route.Login) {
            NavDisplay(
                modifier = Modifier.fillMaxSize(),
                backStack = backStack,
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                }
            ) { key ->
                when (key) {
                    Route.Login -> NavEntry(key) {
                        LoginScreen(
                            onLoginSuccess = { email ->
                                scope.launch {
                                    settings.setLoggedIn(email, true)
                                }
                            },
                            onNavigateToRegistration = { navController.navigate(Route.Registration) },
                            onNavigateToCompliance = { navController.navigate(Route.Compliance) }
                        )
                    }
                    Route.Registration -> NavEntry(key) {
                        com.example.myapplication.ui.screens.RegistrationScreen(
                            onSuccess = { email, password, fullName, businessName, businessAddress, businessType, idType, idNumber, selfieCaptured, documentsUploaded, acceptedTerms ->
                                scope.launch {
                                    val result = DIContainer.provideAuthenticateUseCase().registerAdmin(
                                        email = email,
                                        password = password,
                                        fullName = fullName,
                                        businessName = businessName,
                                        businessAddress = businessAddress,
                                        businessType = businessType,
                                        idType = idType,
                                        idNumber = idNumber,
                                        selfieCaptured = selfieCaptured,
                                        documentsUploaded = documentsUploaded,
                                        acceptedTerms = acceptedTerms
                                    )
                                    if (result.isSuccess) {
                                        navController.navigate(Route.Home)
                                    }
                                }
                            },
                            onBack = { navController.pop() },
                            onNavigateToTerms = { navController.navigate(Route.Terms) }
                        )
                    }
                    Route.Terms -> NavEntry(key) {
                        com.example.myapplication.ui.screens.TermsAndConditionsScreen(onBack = { navController.pop() })
                    }
                    Route.Compliance -> NavEntry(key) {
                        com.example.myapplication.ui.screens.ComplianceScreen(onBack = { navController.pop() })
                    }
                    else -> NavEntry(key) {}
                }
            }
        } else {
            NavigationSuiteScaffold(
                containerColor = MaterialTheme.colorScheme.background,
                navigationSuiteItems = {
                    val activeColor = SwiftPayPrimary
                    val inactiveColor = Color.Gray.copy(alpha = 0.6f)
                    
                    item(
                        selected = currentRoute == Route.Home,
                        onClick = { navController.navigate(Route.Home) },
                        icon = { 
                            Icon(
                                if (currentRoute == Route.Home) Icons.Rounded.Home else Icons.Rounded.Home, 
                                contentDescription = "Home",
                                tint = if (currentRoute == Route.Home) activeColor else inactiveColor
                            ) 
                        },
                        label = { 
                            Text(
                                "Home", 
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (currentRoute == Route.Home) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (currentRoute == Route.Home) activeColor else inactiveColor
                            ) 
                        },
                    )
                    item(
                        selected = currentRoute == Route.Wallet,
                        onClick = { navController.navigate(Route.Wallet) },
                        icon = { 
                            Icon(
                                Icons.Rounded.AccountBalanceWallet, 
                                contentDescription = "Wallet",
                                tint = if (currentRoute == Route.Wallet) activeColor else inactiveColor
                            ) 
                        },
                        label = { 
                            Text(
                                "Wallet",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (currentRoute == Route.Wallet) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (currentRoute == Route.Wallet) activeColor else inactiveColor
                            ) 
                        },
                    )
                }
            ) {
                NavDisplay(
                    modifier = Modifier.fillMaxSize(),
                    backStack = backStack,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(300)) + slideInHorizontally(animationSpec = tween(400), initialOffsetX = { it }))
                            .togetherWith(fadeOut(animationSpec = tween(300)) + slideOutHorizontally(animationSpec = tween(400), targetOffsetX = { -it }))
                    },
                    popTransitionSpec = {
                        (fadeIn(animationSpec = tween(300)) + slideInHorizontally(animationSpec = tween(400), initialOffsetX = { -it }))
                            .togetherWith(fadeOut(animationSpec = tween(300)) + slideOutHorizontally(animationSpec = tween(400), targetOffsetX = { it }))
                    },
                    onBack = {
                        navController.pop()
                    }
                ) { key ->
                    when (key) {
                        Route.Home -> NavEntry(key) {
                            HomeScreen(
                                onNavigateToPayout = { navController.navigate(Route.Payout) },
                                onNavigateToHub = { navController.navigate(Route.Hub) },
                                onNavigateToWallet = { navController.navigate(Route.Wallet) },
                                onNavigateToSettings = { navController.navigate(Route.Settings) },
                                onNavigateToCashIn = { navController.navigate(Route.CashIn) }
                            )
                        }
                        Route.CashIn -> NavEntry(key) {
                            com.example.myapplication.ui.screens.CashInScreen(onBack = { navController.pop() })
                        }
                        Route.Dashboard -> NavEntry(key) {
                            com.example.myapplication.ui.screens.DashboardScreen(onBack = { navController.pop() })
                        }
                        Route.Payout -> NavEntry(key) {
                            com.example.myapplication.ui.screens.PayoutScreen(onBack = { navController.pop() })
                        }
                        Route.Hub -> NavEntry(key) {
                            com.example.myapplication.ui.screens.HubScreen(
                                onBack = { navController.pop() },
                                onNavigateToWebhooks = { navController.navigate(Route.Webhooks) },
                                onNavigateToInvoices = { navController.navigate(Route.Invoices) },
                                onNavigateToVca = { navController.navigate(Route.Vca) },
                                onNavigateToMembers = { navController.navigate(Route.Members) },
                                onNavigateToDashboard = { navController.navigate(Route.Dashboard) },
                                onNavigateToAdminDeposits = { navController.navigate(Route.AdminDeposits) },
                                onNavigateToAuditLogs = { navController.navigate(Route.AuditLogs) }
                            )
                        }
                        Route.AdminDeposits -> NavEntry(key) {
                            com.example.myapplication.ui.screens.AdminDepositsScreen(onBack = { navController.pop() })
                        }
                        Route.AuditLogs -> NavEntry(key) {
                            com.example.myapplication.ui.screens.AuditLogsScreen(onBack = { navController.pop() })
                        }
                        Route.Members -> NavEntry(key) {

                            com.example.myapplication.ui.screens.MembersScreen(onBack = { navController.pop() })
                        }
                        Route.Webhooks -> NavEntry(key) {
                            com.example.myapplication.ui.screens.WebhooksScreen(onBack = { navController.pop() })
                        }
                        Route.Invoices -> NavEntry(key) {
                            com.example.myapplication.ui.screens.InvoicesScreen(onBack = { navController.pop() })
                        }
                        Route.Vca -> NavEntry(key) {
                            com.example.myapplication.ui.screens.VcaScreen(onBack = { navController.pop() })
                        }
                        Route.Profile -> NavEntry(key) {
                            com.example.myapplication.ui.screens.MerchantProfileScreen(onBack = { navController.pop() })
                        }
                        Route.Settings -> NavEntry(key) {
                            com.example.myapplication.ui.screens.ProfileScreen(onBack = { navController.pop() })
                        }
                        Route.ApiKeys -> NavEntry(key) {
                            ApiKeysScreen()
                        }
                        Route.ApiDocs -> NavEntry(key) {
                            ApiDocsScreen()
                        }
                        Route.Approvals -> NavEntry(key) {
                            com.example.myapplication.ui.screens.ApprovalsScreen()
                        }
                        Route.Wallet -> NavEntry(key) {
                            WalletScreen()
                        }
                        else -> NavEntry(key) {}
                    }
                }
            }
        }
    }
}
