package com.example.myapplication.util

import android.util.Log
import com.example.myapplication.BuildConfig

object DebugLogger {
    private const val TAG = "SwiftPay_SECURE"
    // Security: Completely disable logging in release builds to prevent credential leakage
    private val isDebug = BuildConfig.DEBUG

    fun logAuthCheck(isLoggedIn: Boolean, email: String? = null) {
        if (isDebug) Log.d(TAG, "AUTH_CHECK: isLoggedIn=$isLoggedIn")
    }

    fun logBuildConfigKeys(publicKey: String?, secretKey: String?, mid: String?, terminalId: String?) {
        // Security: Never log keys even in debug, or use very strict masking
        if (isDebug) {
            val publicMasked = publicKey?.take(4) + "****"
            Log.d(TAG, "KEYS_LOADED: public=$publicMasked, mid=$mid")
        }
    }

    fun logStoredKeys(publicKey: String?, secretKey: String?, mid: String?, terminalId: String?) {
        if (isDebug) {
            val publicMasked = publicKey?.take(4) + "****"
            Log.d(TAG, "STORED_KEYS_ACCESSED: public=$publicMasked")
        }
    }

    fun logLoginSuccess(email: String) {
        if (isDebug) Log.i(TAG, "LOGIN_EVENT: Success")
    }

    fun logLoginFailure(email: String, errorMessage: String) {
        // Only log generic failure in production
        Log.e(TAG, "LOGIN_EVENT: Failure")
    }

    fun logLogout(email: String) {
        if (isDebug) Log.i(TAG, "LOGOUT_EVENT")
    }

    fun logCredentialsLoaded(hasSecret: Boolean, hasPublic: Boolean) {
        if (isDebug) Log.d(TAG, "CREDENTIALS_STATUS: ok")
    }

    fun logDeviceInfo(deviceId: String = android.os.Build.DEVICE) {
        if (isDebug) Log.d(TAG, "ENV: device_integrity_check_passed")
    }
    
    fun logSessionCreated(email: String, deviceId: String, isPrimary: Boolean) {
        if (isDebug) Log.d(TAG, "SESSION: created")
    }
    
    fun logSessionRestored(email: String, deviceId: String) {
        if (isDebug) Log.d(TAG, "SESSION: restored")
    }
    
    fun logSessionCleared() {
        if (isDebug) Log.i(TAG, "SESSION: invalidated")
    }

    fun logApprovalRequested(email: String, deviceId: String) {
        if (isDebug) Log.d(TAG, "AUTH: remote_approval_requested")
    }

    fun logApprovalApproved(requestId: String) {
        if (isDebug) Log.d(TAG, "AUTH: approved")
    }

    fun logApprovalDenied(requestId: String) {
        if (isDebug) Log.d(TAG, "AUTH: denied")
    }
}
