package com.example.myapplication.data

import com.example.myapplication.BuildConfig
import com.example.myapplication.util.DebugLogger
import kotlinx.coroutines.flow.first

data class SwiftPayCredentials(
    val secretKey: String?,
    val publicKey: String?,
    val mid: String?,
    val terminalId: String?,
    val cardMid: String? = null
) {
    val hasSecretKey: Boolean = !secretKey.isNullOrBlank() && secretKey != MISSING_KEY
    val hasPublicKey: Boolean = !publicKey.isNullOrBlank() && publicKey != MISSING_KEY

    companion object {
        const val MISSING_KEY = "MISSING_KEY"
    }
}

suspend fun SettingsManager.loadSwiftPayCredentials(): SwiftPayCredentials {
    val storedSecret = secretKey.first()
    val storedPublic = publicKey.first()
    val storedMid = mid.first()
    val storedCardMid = cardMid.first()
    val storedTerminalId = terminalId.first()

    // Log stored values
    DebugLogger.logStoredKeys(storedPublic, storedSecret, storedMid, storedTerminalId)
    // Log BuildConfig values
    DebugLogger.logBuildConfigKeys(BuildConfig.SWIFTPAY_PUBLIC_KEY, BuildConfig.SWIFTPAY_SECRET_KEY, BuildConfig.SWIFTPAY_QR_MID, BuildConfig.SWIFTPAY_TERMINAL_ID)

    val credentials = SwiftPayCredentials(
        secretKey = storedSecret.takeUnless { it.isNullOrBlank() } ?: BuildConfig.SWIFTPAY_SECRET_KEY,
        publicKey = storedPublic.takeUnless { it.isNullOrBlank() } ?: BuildConfig.SWIFTPAY_PUBLIC_KEY,
        mid = storedMid.takeUnless { it.isNullOrBlank() } ?: BuildConfig.SWIFTPAY_QR_MID,
        terminalId = storedTerminalId.takeUnless { it.isNullOrBlank() } ?: BuildConfig.SWIFTPAY_TERMINAL_ID,
        cardMid = (storedCardMid ?: storedMid).takeUnless { it.isNullOrBlank() } ?: BuildConfig.SWIFTPAY_CARD_MID
    )

    DebugLogger.logCredentialsLoaded(credentials.hasSecretKey, credentials.hasPublicKey)
    return credentials
}

suspend fun SettingsManager.createSwiftPayService(): SwiftPayService {
    val credentials = loadSwiftPayCredentials()
    return SwiftPayService(
        customSecretKey = credentials.secretKey,
        customPublicKey = credentials.publicKey,
        customMid = credentials.mid,
        customTerminalId = credentials.terminalId,
        customCardMid = credentials.cardMid
    )
}
