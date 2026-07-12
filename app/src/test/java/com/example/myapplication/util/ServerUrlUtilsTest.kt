package com.example.myapplication.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ServerUrlUtilsTest {
    @Test
    fun buildBackendApiUrlAvoidsDoubleApiSegments() {
        assertEquals(
            "https://example.com/api/",
            ServerUrlUtils.buildBackendApiUrl("https://example.com/api")
        )
    }

    @Test
    fun normalizeBaseUrlRemovesTrailingSlashAndApiSuffix() {
        assertEquals(
            "https://example.com",
            ServerUrlUtils.normalizeBaseUrl("https://example.com/api/")
        )
    }
}
