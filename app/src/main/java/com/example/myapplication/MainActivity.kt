package com.example.myapplication

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.example.myapplication.navigation.Route
import com.example.myapplication.ui.screens.*
import com.example.myapplication.di.DIContainer
import com.example.myapplication.ui.theme.*
import com.example.myapplication.data.SettingsManager
import com.example.myapplication.data.SessionManager
import com.example.myapplication.util.DeepLinkBus
import com.example.myapplication.util.DeepLinkExtractor
import com.example.myapplication.util.DebugLogger
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

val LocalMiniAppViewModel = compositionLocalOf<MiniAppViewModel> { error("No MiniAppViewModel provided") }
val LocalNavController = compositionLocalOf<NavController> { error("No NavController provided") }

interface NavController {
    fun navigate(route: Route)
    fun pop()
    fun logout()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Security: Prevent screenshots and screen recording
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
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        DeepLinkExtractor.bundleToMap(intent.extras)?.let { map ->
            lifecycleScope.launch { DeepLinkBus.flow.emit(map) }
        }
    }
}

@Composable
fun SwiftPayApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember { SettingsManager(context) }
    val sessionManager = remember { SessionManager(context, settings) }
    val isLoggedIn by settings.isLoggedIn.collectAsState(initial = false)
    val loggedInEmail by settings.loggedInEmail.collectAsState(initial = null)

    val backStack = remember { mutableStateListOf<Route>(if (isLoggedIn) Route.Home else Route.Login) }
    val currentRoute = backStack.lastOrNull() ?: Route.Login
    
    val miniAppViewModel: MiniAppViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    // Handle Deep Links
    LaunchedEffect(Unit) {
        DeepLinkBus.flow.collectLatest { payload ->
            if (payload != null) {
                val (linkId, status) = DeepLinkExtractor.extractPaymentInfo(payload)
                if (linkId.isNotBlank()) {
                    backStack.clear()
                    backStack.add(Route.Home)
                    miniAppViewModel.handleDeepLink(linkId, status.ifBlank { null })
                    Toast.makeText(context, "Payment Update: $status", Toast.LENGTH_SHORT).show()
                }
            }
        }
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
            override fun pop() { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) }
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
        if (currentRoute == Route.Login || currentRoute == Route.Registration || currentRoute == Route.Terms || currentRoute == Route.Compliance) {
            NavDisplay(
                modifier = Modifier.fillMaxSize(),
                backStack = backStack,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) }
            ) { key ->
                when (key) {
                    Route.Login -> NavEntry(key) { LoginScreen(onLoginSuccess = { e -> scope.launch { settings.setLoggedIn(e, true) } }, onNavigateToRegistration = { navController.navigate(Route.Registration) }, onNavigateToCompliance = { navController.navigate(Route.Compliance) }) }
                    Route.Registration -> NavEntry(key) { RegistrationScreen(onSuccess = { _,_,_,_,_,_,_,_,_,_,_ -> navController.navigate(Route.Home) }, onBack = { navController.pop() }, onNavigateToTerms = { navController.navigate(Route.Terms) }) }
                    Route.Terms -> NavEntry(key) { TermsAndConditionsScreen(onBack = { navController.pop() }) }
                    Route.Compliance -> NavEntry(key) { ComplianceScreen(onBack = { navController.pop() }) }
                    else -> NavEntry(key) {}
                }
            }
        } else {
            NavigationSuiteScaffold(
                containerColor = MaterialTheme.colorScheme.background,
                navigationSuiteItems = {
                    item(selected = currentRoute == Route.Home, onClick = { navController.navigate(Route.Home) }, icon = { Icon(Icons.Rounded.Home, null) }, label = { Text("Home") })
                    item(selected = currentRoute == Route.Wallet, onClick = { navController.navigate(Route.Wallet) }, icon = { Icon(Icons.Rounded.AccountBalanceWallet, null) }, label = { Text("Wallet") })
                    item(selected = currentRoute == Route.Hub, onClick = { navController.navigate(Route.Hub) }, icon = { Icon(Icons.Rounded.Widgets, null) }, label = { Text("Hub") })
                }
            ) {
                NavDisplay(
                    modifier = Modifier.fillMaxSize(),
                    backStack = backStack,
                    transitionSpec = { (fadeIn(tween(300)) + slideInHorizontally(tween(400)) { it }).togetherWith(fadeOut(tween(300)) + slideOutHorizontally(tween(400)) { -it }) },
                    popTransitionSpec = { (fadeIn(tween(300)) + slideInHorizontally(tween(400)) { -it }).togetherWith(fadeOut(tween(300)) + slideOutHorizontally(tween(400)) { it }) },
                    onBack = { navController.pop() }
                ) { key ->
                    when (key) {
                        Route.Home -> NavEntry(key) { HomeScreen(onNavigateToPayout = { navController.navigate(Route.Payout) }, onNavigateToHub = { navController.navigate(Route.Hub) }, onNavigateToWallet = { navController.navigate(Route.Wallet) }, onNavigateToSettings = { navController.navigate(Route.Settings) }, onNavigateToCashIn = { navController.navigate(Route.CashIn) }) }
                        Route.CashIn -> NavEntry(key) { CashInScreen(onBack = { navController.pop() }) }
                        Route.Dashboard -> NavEntry(key) { DashboardScreen(onBack = { navController.pop() }) }
                        Route.Payout -> NavEntry(key) { PayoutScreen(onBack = { navController.pop() }) }
                        Route.Hub -> NavEntry(key) { HubScreen(onBack = { navController.pop() }, onNavigateToWebhooks = { navController.navigate(Route.Webhooks) }, onNavigateToInvoices = { navController.navigate(Route.Invoices) }, onNavigateToVca = { navController.navigate(Route.Vca) }, onNavigateToMembers = { navController.navigate(Route.Members) }, onNavigateToDashboard = { navController.navigate(Route.Dashboard) }, onNavigateToAdminDeposits = { navController.navigate(Route.AdminDeposits) }, onNavigateToAuditLogs = { navController.navigate(Route.AuditLogs) }) }
                        Route.AdminDeposits -> NavEntry(key) { AdminDepositsScreen(onBack = { navController.pop() }) }
                        Route.AuditLogs -> NavEntry(key) { AuditLogsScreen(onBack = { navController.pop() }) }
                        Route.Members -> NavEntry(key) { MembersScreen(onBack = { navController.pop() }) }
                        Route.Webhooks -> NavEntry(key) { WebhooksScreen(onBack = { navController.pop() }) }
                        Route.Invoices -> NavEntry(key) { InvoicesScreen(onBack = { navController.pop() }) }
                        Route.Vca -> NavEntry(key) { VcaScreen(onBack = { navController.pop() }) }
                        Route.Settings -> NavEntry(key) { ProfileScreen(onBack = { navController.pop() }) }
                        Route.Wallet -> NavEntry(key) { WalletScreen() }
                        else -> NavEntry(key) {}
                    }
                }
            }
        }
    }
}
