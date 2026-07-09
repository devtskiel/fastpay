package com.example.myapplication.util

import android.util.Log
import com.example.myapplication.BuildConfig

object DebugLogger {
    private const val TAG = "FastPay_DEBUG"
    private val isDebug = BuildConfig.DEBUG

    fun logAuthCheck(isLoggedIn: Boolean, email: String? = null) {
        if (isDebug) Log.d(TAG, "AUTH_CHECK: isLoggedIn=$isLoggedIn, email=$email")
    }

    fun logBuildConfigKeys(publicKey: String?, secretKey: String?, mid: String?, terminalId: String?) {
        if (isDebug) {
            val publicMasked = publicKey?.take(10) + "..."
            val secretMasked = secretKey?.take(8) + "..."
            Log.d(TAG, "BUILDCONFIG_KEYS: public=$publicMasked, secret=$secretMasked, mid=$mid, terminalId=$terminalId")
        }
    }

    fun logStoredKeys(publicKey: String?, secretKey: String?, mid: String?, terminalId: String?) {
        if (isDebug) {
            val publicMasked = publicKey?.take(10) + "..."
            val secretMasked = secretKey?.take(8) + "..."
            Log.d(TAG, "STORED_KEYS: public=$publicMasked, secret=$secretMasked, mid=$mid, terminalId=$terminalId")
        }
    }

    fun logLoginSuccess(email: String) {
        if (isDebug) Log.d(TAG, "LOGIN_SUCCESS: email=$email, logging in user...")
    }

    fun logLoginFailure(email: String, errorMessage: String) {
        Log.e(TAG, "LOGIN_FAILURE: email=$email, error=$errorMessage")
    }

    fun logLogout(email: String) {
        if (isDebug) Log.d(TAG, "LOGOUT: email=$email, clearing session...")
    }

    fun logCredentialsLoaded(hasSecret: Boolean, hasPublic: Boolean) {
        if (isDebug) Log.d(TAG, "CREDENTIALS_LOADED: hasSecret=$hasSecret, hasPublic=$hasPublic")
    }

    fun logDeviceInfo(deviceId: String = android.os.Build.DEVICE) {
        if (isDebug) Log.d(TAG, "DEVICE_INFO: device=$deviceId, app_version=${android.os.Build.VERSION.SDK_INT}")
    }
    
    fun logSessionCreated(email: String, deviceId: String, isPrimary: Boolean) {
        if (isDebug) Log.d(TAG, "SESSION_CREATED: email=$email, device=$deviceId, primary=$isPrimary")
    }
    
    fun logSessionRestored(email: String, deviceId: String) {
        if (isDebug) Log.d(TAG, "SESSION_RESTORED: email=$email, device=$deviceId (auto-login without OTP)")
    }
    
    fun logSessionCleared() {
        if (isDebug) Log.i(TAG, "SESSION_CLEARED: All device sessions invalidated, login required")
    }

    fun logApprovalRequested(email: String, deviceId: String) {
        if (isDebug) Log.d(TAG, "APPROVAL_REQUESTED: email=$email, device=$deviceId")
    }

    fun logApprovalApproved(requestId: String) {
        if (isDebug) Log.d(TAG, "APPROVAL_APPROVED: requestId=$requestId")
    }

    fun logApprovalDenied(requestId: String) {
        if (isDebug) Log.d(TAG, "APPROVAL_DENIED: requestId=$requestId")
    }
}


