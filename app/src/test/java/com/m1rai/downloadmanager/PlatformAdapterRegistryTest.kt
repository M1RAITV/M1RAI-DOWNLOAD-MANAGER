package com.m1rai.downloadmanager

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformAdapterRegistryTest {
    @Test fun detectedPlatformIsNotAutomaticallyTreatedAsDownloadable() = runBlocking {
        val registry = PlatformAdapterRegistry()
        val result = registry.analyze(MediaPlatform.FACEBOOK, "https://www.facebook.com/watch/123")

        assertTrue(result is AdapterAnalysis.Unsupported)
        assertEquals(emptySet<MediaPlatform>(), registry.registeredPlatforms())
    }

    @Test fun invokesOnlyTheAdapterRegisteredForThatPlatform() = runBlocking {
        val adapter = object : PlatformMediaAdapter {
            override val platform = MediaPlatform.VIMEO

            override suspend fun analyze(publicUrl: String): AdapterAnalysis =
                AdapterAnalysis.Ready(
                    platform = platform,
                    options = listOf(
                        MediaDownloadOption(
                            id = "video-720",
                            title = "Video",
                            mediaType = MediaType.VIDEO,
                            mimeType = "video/mp4",
                            qualityLabel = "720p",
                            downloadUrl = "https://cdn.example/video.mp4",
                            fileName = "video.mp4"
                        )
                    )
                )
        }
        val registry = PlatformAdapterRegistry(listOf(adapter))

        val supported = registry.analyze(MediaPlatform.VIMEO, "https://vimeo.com/123")
        val unsupported = registry.analyze(MediaPlatform.FACEBOOK, "https://facebook.com/video")

        assertTrue(supported is AdapterAnalysis.Ready)
        assertEquals(1, (supported as AdapterAnalysis.Ready).options.size)
        assertTrue(unsupported is AdapterAnalysis.Unsupported)
    }
}
