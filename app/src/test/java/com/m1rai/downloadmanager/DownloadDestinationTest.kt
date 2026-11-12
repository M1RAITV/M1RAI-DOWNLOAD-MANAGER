package com.m1rai.downloadmanager

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadDestinationTest {
    @Test fun placesDownloadsInsideDedicatedFolder() {
        assertEquals("M1RAI DOWNLOADS/video.mp4", DownloadDestination.relativePath("video.mp4"))
    }

    @Test fun supportsFutureCustomFolderName() {
        assertEquals("My Files/archive.zip", DownloadDestination.relativePath("archive.zip", "My Files"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsPathTraversalInFileName() {
        DownloadDestination.relativePath("../outside.mp4")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsPathTraversalInFolderName() {
        DownloadDestination.relativePath("video.mp4", "../outside")
    }
}
