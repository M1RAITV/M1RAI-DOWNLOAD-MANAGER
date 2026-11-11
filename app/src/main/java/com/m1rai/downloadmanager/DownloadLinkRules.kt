package com.m1rai.downloadmanager

import java.net.URI

internal object DownloadLinkRules {
    private val sharedUrlPattern = Regex("""(?i)https?://[^\\s<>\\"']+""")

    fun isValidHttpUrl(raw: String): Boolean {
        val value = raw.trim()
        if (value.isEmpty() || value.any { it.isWhitespace() }) return false
        return runCatching {
            val uri = URI(value)
            uri.scheme?.lowercase() in setOf("http", "https") &&
                !uri.host.isNullOrBlank() && uri.userInfo == null
        }.getOrDefault(false)
    }

    /**
     * Extracts the first valid HTTP(S) URL from text shared by another app.
     * Trailing sentence punctuation is removed before URL validation.
     */
    fun extractHttpUrl(raw: String): String? {
        val text = raw.trim()
        if (isValidHttpUrl(text)) return text

        return sharedUrlPattern.findAll(text)
            .map { match ->
                match.value.trimEnd('.', ',', ';', ':', '!', '?', ')', ']', '}')
            }
            .firstOrNull(::isValidHttpUrl)
    }
}
