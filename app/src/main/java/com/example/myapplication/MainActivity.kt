package com.example.myapplication

import android.os.Bundle
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
import com.example.myapplication.ui.screens.MiniAppScreen
import com.example.myapplication.ui.screens.ProfileScreen
import com.example.myapplication.ui.screens.WalletScreen
import com.example.myapplication.ui.screens.ApiKeysScreen
import com.example.myapplication.ui.screens.ApiDocsScreen
import com.example.myapplication.ui.theme.FastPayTheme
import com.example.myapplication.ui.theme.FastPayBlue
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
        
        setContent {
            val settings = remember { SettingsManager(application) }
            val themeMode by settings.themeMode.collectAsState(initial = "SYSTEM")
            val isDarkTheme = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            FastPayTheme(darkTheme = isDarkTheme) {
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
    val settings = remember { SettingsManager(context) }
    val sessionManager = remember { SessionManager(context, settings) }
    val approvalService: ApprovalService = remember { ApprovalService() }
    val isLoggedIn by settings.isLoggedIn.collectAsState(initial = false)
    val loggedInEmail by settings.loggedInEmail.collectAsState(initial = null)

    DebugLogger.logAuthCheck(isLoggedIn, loggedInEmail)
    DebugLogger.logDeviceInfo()

    val backStack = remember { mutableStateListOf<Route>(if (isLoggedIn) Route.Home else Route.Login) }
    val currentRoute = backStack.lastOrNull() ?: Route.Login
    
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!isLoggedIn) {
            val currentSession = sessionManager.getCurrentSessionToken()
            if (currentSession != null) {
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
                (context as? androidx.activity.ComponentActivity)?.lifecycleScope?.launch {
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
                        LoginScreen(onLoginSuccess = { navController.navigate(Route.Home) })
                    }
                    else -> NavEntry(key) {}
                }
            }
        } else {
            NavigationSuiteScaffold(
                containerColor = MaterialTheme.colorScheme.background,
                navigationSuiteItems = {
                    val activeColor = FastPayBlue
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
                                "首页", 
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
                                contentDescription = "资产",
                                tint = if (currentRoute == Route.Wallet) activeColor else inactiveColor
                            ) 
                        },
                        label = { 
                            Text(
                                "资产",
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
                                onLaunchMiniApp = { path -> navController.navigate(Route.MiniApp(path)) },
                                onNavigateToWallet = { navController.navigate(Route.Wallet) }
                            )
                        }
                        is Route.MiniApp -> NavEntry(key) {
                            MiniAppScreen(initialPath = key.initialPath)
                        }
                        Route.Profile -> NavEntry(key) {
                            ProfileScreen()
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
