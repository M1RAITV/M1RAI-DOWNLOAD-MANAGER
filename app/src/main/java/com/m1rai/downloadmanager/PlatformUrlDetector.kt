package com.m1rai.downloadmanager

import java.net.URI
import java.util.Locale

internal enum class MediaPlatform(
    val displayName: String,
    private val domains: Set<String>
) {
    YOUTUBE("YouTube", setOf("youtube.com", "youtu.be", "youtube-nocookie.com")),
    FACEBOOK("Facebook", setOf("facebook.com", "fb.com", "fb.watch")),
    INSTAGRAM("Instagram", setOf("instagram.com", "instagr.am")),
    TIKTOK("TikTok", setOf("tiktok.com")),
    X("X", setOf("x.com", "twitter.com", "t.co")),
    SNAPCHAT("Snapchat", setOf("snapchat.com")),
    PINTEREST("Pinterest", setOf("pinterest.com", "pin.it")),
    REDDIT("Reddit", setOf("reddit.com", "redd.it")),
    THREADS("Threads", setOf("threads.net")),
    LINKEDIN("LinkedIn", setOf("linkedin.com", "lnkd.in")),
    TWITCH("Twitch", setOf("twitch.tv")),
    DISCORD("Discord", setOf("discord.com", "discord.gg")),
    VIMEO("Vimeo", setOf("vimeo.com")),
    DAILYMOTION("Dailymotion", setOf("dailymotion.com", "dai.ly")),
    RUMBLE("Rumble", setOf("rumble.com")),
    TUMBLR("Tumblr", setOf("tumblr.com")),
    DOUYIN("Douyin", setOf("douyin.com")),
    KUAISHOU("Kuaishou", setOf("kuaishou.com", "gifshow.com")),
    BILIBILI("Bilibili", setOf("bilibili.com", "b23.tv")),
    WEIBO("Weibo", setOf("weibo.com", "weibo.cn")),
    VK("VK", setOf("vk.com", "vk.ru")),
    OK("OK.ru", setOf("ok.ru")),
    LIKEE("Likee", setOf("likee.video")),
    SNACKVIDEO("SnackVideo", setOf("snackvideo.com")),
    BLUESKY("Bluesky", setOf("bsky.app"));

    fun matches(host: String): Boolean =
        domains.any { host == it || host.endsWith(".$it") }

    companion object {
        fun fromHost(host: String): MediaPlatform? =
            entries.firstOrNull { it.matches(host) }
    }
}

internal sealed interface PlatformDetection {
    data class Detected(val platform: MediaPlatform) : PlatformDetection
    data class Excluded(val platformName: String) : PlatformDetection
    data object NotRecognized : PlatformDetection
    data object InvalidUrl : PlatformDetection
}

/**
 * Identifies a URL's platform only. Detection does not imply that media can
 * be extracted or downloaded; that is decided by a separately tested adapter.
 */
internal object PlatformUrlDetector {
    private val excludedDomains = mapOf(
        "whatsapp.com" to "WhatsApp",
        "wa.me" to "WhatsApp",
        "telegram.org" to "Telegram",
        "t.me" to "Telegram",
        "telegram.me" to "Telegram",
        "messenger.com" to "Messenger"
    )

    fun detect(rawUrl: String): PlatformDetection {
        val uri = runCatching { URI(rawUrl.trim()) }.getOrNull()
            ?: return PlatformDetection.InvalidUrl
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        val host = uri.host?.lowercase(Locale.ROOT)?.trimEnd('.')
            ?: return PlatformDetection.InvalidUrl

        if (scheme !in setOf("http", "https") || uri.userInfo != null) {
            return PlatformDetection.InvalidUrl
        }

        excludedDomains.entries.firstOrNull { (domain, _) ->
            host == domain || host.endsWith(".$domain")
        }?.let { return PlatformDetection.Excluded(it.value) }

        val platform = MediaPlatform.fromHost(host)
            ?: return PlatformDetection.NotRecognized
        return PlatformDetection.Detected(platform)
    }
}
