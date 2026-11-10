package com.m1rai.downloadmanager

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

internal object DownloadMetadataParser {
    fun fileName(contentDisposition: String?, fallback: String): String {
        val header = contentDisposition.orEmpty()
        val encoded = Regex("(?i)filename\\\\*\\\\s*=\\\\s*UTF-8''([^;]+)")
            .find(header)?.groupValues?.getOrNull(1)
        val plain = Regex("(?i)filename\\\\s*=\\\\s*(?:\\\"([^\\\"]+)\\\"|([^;]+))")
            .find(header)?.let { it.groupValues[1].ifBlank { it.groupValues[2] }.trim() }
        val candidate = encoded?.let { runCatching { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }.getOrNull() }
            ?: plain
            ?: fallback.substringAfterLast('/').substringBefore('?').ifBlank { "download" }
        val leaf = candidate.substringAfterLast('/').substringAfterLast(92.toChar())
        val safe = leaf.map { ch -> if (ch.code < 32 || ch in "<>:\"/\\\\|?*") '_' else ch }.joinToString("")
        return safe.trim().take(180).ifBlank { "download" }
    }

    fun contentLength(raw: String?): Long? = raw?.trim()?.toLongOrNull()?.takeIf { it >= 0 }
}
