package com.example.myapplication.ui.screens

import android.widget.Toast
import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.SettingsManager
import com.example.myapplication.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeysScreen() {
    val context = LocalContext.current
    val settings = SettingsManager(context)
    val scope = rememberCoroutineScope()

    val secretFlow = settings.secretKey.collectAsState(initial = null)
    val publicFlow = settings.publicKey.collectAsState(initial = null)
    val midFlow = settings.mid.collectAsState(initial = null)
    val terminalFlow = settings.terminalId.collectAsState(initial = null)
    val aliasFlow = settings.merchantAlias.collectAsState(initial = null)
    val envFlow = settings.environment.collectAsState(initial = "PRODUCTION")

    var editingAlias by remember { mutableStateOf(false) }
    var aliasInput by remember(aliasFlow.value) { mutableStateOf(aliasFlow.value ?: "") }

    val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager

    val isDarkTheme = MaterialTheme.colorScheme.background == SwiftPayBackground
    val bgColor = if (isDarkTheme) Color(0xFF121212) else SwiftPaySurface
    val surfaceColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkTheme) Color.White else SwiftPayPrimary
    val secondaryTextColor = if (isDarkTheme) Color.LightGray else Color.Gray

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("SwiftPay Configuration", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = SwiftPayPrimary
                )
            )
        },
        containerColor = bgColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(20.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            // Environment Toggle
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Environment", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = textColor)
                        Text(envFlow.value ?: "PRODUCTION", color = if (envFlow.value == "SANDBOX") SwiftPayPrimary else SwiftPaySuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Switch(
                        checked = envFlow.value == "SANDBOX",
                        onCheckedChange = { isSandbox ->
                            scope.launch {
                                settings.saveEnvironment(if (isSandbox) "SANDBOX" else "PRODUCTION")
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = SwiftPayPrimary)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Surface(
                color = Color(0xFFFFF3CD),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Rounded.Warning, null, tint = Color(0xFFF57C00), modifier = Modifier.size(20.dp))
                    Text(
                        "Please keep your SwiftPay keys confidential and secure.",
                        color = Color(0xFFF57C00),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    CredentialRow(
                        label = "Secret Key",
                        value = secretFlow.value,
                        onCopy = {
                            secretFlow.value?.let {
                                clipboardManager.setPrimaryClip(ClipData.newPlainText("Secret Key", it))
                                Toast.makeText(context, "Secret Key copied", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.3f))

                    CredentialRow(
                        label = "Public Key",
                        value = publicFlow.value,
                        onCopy = {
                            publicFlow.value?.let {
                                clipboardManager.setPrimaryClip(ClipData.newPlainText("Public Key", it))
                                Toast.makeText(context, "Public Key copied", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.3f))

                    CredentialRow(
                        label = "Merchant ID (MID)",
                        value = midFlow.value,
                        onCopy = {
                            midFlow.value?.let {
                                clipboardManager.setPrimaryClip(ClipData.newPlainText("MID", it))
                                Toast.makeText(context, "MID copied", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Surface(
                color = if (isDarkTheme) Color.LightGray.copy(alpha = 0.1f) else SwiftPayPrimary.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Warning, null, tint = textColor, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Security Notice",
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Use these credentials to authenticate calls to the SwiftPay gateway API. Never share your keys with anyone.",
                        fontSize = 12.sp,
                        color = textColor.copy(alpha = 0.7f),
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CredentialRow(
    label: String,
    value: String?,
    onCopy: () -> Unit
) {
    val isDarkTheme = MaterialTheme.colorScheme.background == SwiftPayBackground
    val textColor = if (isDarkTheme) Color.White else SwiftPayPrimary
    val secondaryTextColor = if (isDarkTheme) Color.LightGray else Color.Gray

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = textColor)
            Spacer(Modifier.height(4.dp))
            Text(
                if (value.isNullOrEmpty()) "Not Configured" else value,
                color = secondaryTextColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = if (isDarkTheme) Color.LightGray else SwiftPayPrimary.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
        }
    }
}
