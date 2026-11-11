package com.m1rai.downloadmanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformUrlDetectorTest {
    @Test fun detectsMainPlatformsAndCommonShortLinks() {
        val cases = mapOf(
            "https://www.youtube.com/watch?v=abc" to MediaPlatform.YOUTUBE,
            "https://youtu.be/abc" to MediaPlatform.YOUTUBE,
            "https://m.facebook.com/watch/" to MediaPlatform.FACEBOOK,
            "https://fb.watch/abc" to MediaPlatform.FACEBOOK,
            "https://www.instagram.com/reel/abc/" to MediaPlatform.INSTAGRAM,
            "https://vm.tiktok.com/abc" to MediaPlatform.TIKTOK,
            "https://x.com/user/status/1" to MediaPlatform.X,
            "https://pin.it/abc" to MediaPlatform.PINTEREST,
            "https://redd.it/abc" to MediaPlatform.REDDIT,
            "https://www.threads.net/@user/post/1" to MediaPlatform.THREADS,
            "https://lnkd.in/abc" to MediaPlatform.LINKEDIN,
            "https://www.twitch.tv/channel" to MediaPlatform.TWITCH,
            "https://discord.gg/invite" to MediaPlatform.DISCORD,
            "https://b23.tv/abc" to MediaPlatform.BILIBILI
        )

        cases.forEach { (url, expected) ->
            assertEquals(url, PlatformDetection.Detected(expected), PlatformUrlDetector.detect(url))
        }
    }

    @Test fun excludesPlatformsWithNativeDownloadFlows() {
        listOf(
            "https://web.whatsapp.com/",
            "https://wa.me/123",
            "https://t.me/channel/123",
            "https://telegram.org/",
            "https://www.messenger.com/"
        ).forEach { url ->
            assertTrue("$url should be excluded", PlatformUrlDetector.detect(url) is PlatformDetection.Excluded)
        }
    }

    @Test fun rejectsLookalikeHostsAndUnsupportedSchemes() {
        assertEquals(
            PlatformDetection.NotRecognized,
            PlatformUrlDetector.detect("https://youtube.com.attacker.example/video")
        )
        assertEquals(
            PlatformDetection.InvalidUrl,
            PlatformUrlDetector.detect("file:///sdcard/video.mp4")
        )
        assertEquals(
            PlatformDetection.InvalidUrl,
            PlatformUrlDetector.detect("https://user:pass@youtube.com/video")
        )
    }

    @Test fun normalizesHostCaseAndSubdomains() {
        assertEquals(
            PlatformDetection.Detected(MediaPlatform.FACEBOOK),
            PlatformUrlDetector.detect("HTTPS://WWW.FACEBOOK.COM/video")
        )
    }
}
