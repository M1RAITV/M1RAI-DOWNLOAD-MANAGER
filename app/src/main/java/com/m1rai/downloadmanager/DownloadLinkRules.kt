package com.m1rai.downloadmanager

import java.net.URI

internal object DownloadLinkRules {
    fun isValidHttpUrl(raw: String): Boolean {
        val value = raw.trim()
        if (value.isEmpty() || value.any { it.isWhitespace() }) return false
        return runCatching {
            val uri = URI(value)
            uri.scheme?.lowercase() in setOf("http", "https") &&
                !uri.host.isNullOrBlank() && uri.userInfo == null
        }.getOrDefault(false)
    }
}
