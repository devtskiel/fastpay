package com.example.myapplication.util

import java.util.TreeMap
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object SwiftPaySignatureHelper {

    fun calculateSignature(params: Map<String, String>, secretKey: String): String {
        // 1. Select elements with x_ prefix
        val xElements = params.filterKeys { it.startsWith("x_") }
        
        // 2. Sort by keys
        val sortedElements = TreeMap(xElements)
        
        // 3. Concatenate params
        val payload = StringBuilder()
        for ((key, value) in sortedElements) {
            payload.append(key).append(value)
        }
        
        // 4. HMAC-SHA256
        return hmacSha256(payload.toString(), secretKey)
    }

    private fun hmacSha256(data: String, key: String): String {
        val sha256Hmac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256")
        sha256Hmac.init(secretKeySpec)
        val hash = sha256Hmac.doFinal(data.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
