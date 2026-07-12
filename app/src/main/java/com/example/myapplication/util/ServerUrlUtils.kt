package com.example.myapplication.util

object ServerUrlUtils {
    fun normalizeBaseUrl(rawUrl: String?): String {
        val trimmed = rawUrl?.trim().orEmpty()
        if (trimmed.isEmpty()) return ""

        val withoutTrailingSlash = trimmed.removeSuffix("/")
        return withoutTrailingSlash
            .removeSuffix("/api")
            .removeSuffix("/API")
            .trimEnd('/')
    }

    fun buildBackendApiUrl(rawUrl: String?): String {
        val baseUrl = normalizeBaseUrl(rawUrl)
        return if (baseUrl.isEmpty()) {
            "http://10.0.2.2:3000/api/"
        } else if (baseUrl.endsWith("/api")) {
            "$baseUrl/"
        } else {
            "$baseUrl/api/"
        }
    }
}
