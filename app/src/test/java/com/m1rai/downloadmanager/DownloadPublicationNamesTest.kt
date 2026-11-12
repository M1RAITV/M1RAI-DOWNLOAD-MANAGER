package com.m1rai.downloadmanager

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.Rule

class DownloadPublicationNamesTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test fun usesProfessionalDefaultFolderName() {
        assertEquals("M1RAI DOWNLOADS", AndroidDownloadPublisher.DEFAULT_FOLDER_NAME)
    }

    @Test fun acceptsCustomFolderNamesForFutureUserSettings() {
        assertEquals("My Downloads", DownloadPublicationNames.validateFolder(" My Downloads "))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsFolderTraversal() { DownloadPublicationNames.validateFolder("../outside") }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsFilePathInsteadOfFileName() { DownloadPublicationNames.validateFileName("folder/video.mp4") }

    @Test fun createsNonOverwritingNames() {
        val directory = temporaryFolder.newFolder()
        File(directory, "movie.mp4").writeText("existing")
        val next = DownloadPublicationNames.uniqueFile(directory, "movie.mp4")
        assertEquals("movie (1).mp4", next.name)
        assertFalse(next.exists())
    }

    @Test fun preservesExtensionWhenResolvingMultipleCollisions() {
        val directory = temporaryFolder.newFolder()
        File(directory, "archive.zip").writeText("one")
        File(directory, "archive (1).zip").writeText("two")
        assertEquals("archive (2).zip", DownloadPublicationNames.uniqueFile(directory, "archive.zip").name)
        assertTrue(directory.isDirectory)
    }
}
