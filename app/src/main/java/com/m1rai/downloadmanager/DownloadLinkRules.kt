package com.m1rai.downloadmanager

import android.net.Uri

internal object DownloadLinkRules {
    fun parseHttpUrl(raw: String): Uri? {
        val value = raw.trim()
        if (value.isEmpty() || value.any { it.isWhitespace() }) return null
        val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return null
        if (uri.scheme?.lowercase() !in setOf("http", "https")) return null
        if (uri.host.isNullOrBlank()) return null
        if (uri.userInfo != null) return null
        return uri
    }
}
