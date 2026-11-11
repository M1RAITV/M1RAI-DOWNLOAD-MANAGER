package com.m1rai.downloadmanager

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

class EngineTransferRecoveryTest {
    private fun record(file: File, bytes: Long, status: String = TransferStatus.INTERRUPTED.name,
                       etag: String? = "\"v1\"", lastModified: String? = null) =
        EngineTransferRecord(
            taskId = "recover-1",
            url = "https://example.com/video.mp4",
            partialPath = file.absolutePath,
            fileName = "video.mp4",
            status = status,
            downloadedBytes = bytes,
            etag = etag,
            lastModified = lastModified,
            createdAt = 1L,
            updatedAt = 2L
        )

    @Test fun resumesOnlyWhenFileLengthAndValidatorAreVerified() {
        val root = Files.createTempDirectory("m1rai-recovery").toFile()
        try {
            val partial = File(root, "video.part").apply { writeBytes(ByteArray(32)) }
            val decision = EngineTransferRecovery.assess(record(partial, 32L), root)
            assertTrue(decision is EngineRecoveryDecision.ResumeFromOffset)
            assertEquals(32L, (decision as EngineRecoveryDecision.ResumeFromOffset).offset)
            assertEquals("\"v1\"", decision.validators.etag)
        } finally { root.deleteRecursively() }
    }

    @Test fun mismatchedLengthRequiresRestartFromZero() {
        val root = Files.createTempDirectory("m1rai-recovery").toFile()
        try {
            val partial = File(root, "video.part").apply { writeBytes(ByteArray(31)) }
            assertTrue(EngineTransferRecovery.assess(record(partial, 32L), root) is EngineRecoveryDecision.RestartFromZero)
        } finally { root.deleteRecursively() }
    }

    @Test fun missingValidatorRequiresRestartFromZero() {
        val root = Files.createTempDirectory("m1rai-recovery").toFile()
        try {
            val partial = File(root, "video.part").apply { writeBytes(ByteArray(32)) }
            assertTrue(EngineTransferRecovery.assess(record(partial, 32L, etag = null), root) is EngineRecoveryDecision.RestartFromZero)
        } finally { root.deleteRecursively() }
    }

    @Test fun refusesPathsOutsideAppPrivateDirectory() {
        val root = Files.createTempDirectory("m1rai-recovery-root").toFile()
        val outside = Files.createTempFile("m1rai-outside", ".part").toFile()
        try {
            assertTrue(EngineTransferRecovery.assess(record(outside, outside.length()), root) is EngineRecoveryDecision.NotRecoverable)
        } finally { root.deleteRecursively(); outside.delete() }
    }

    @Test fun terminalTransfersAreNotRecovered() {
        val root = Files.createTempDirectory("m1rai-recovery").toFile()
        try {
            val partial = File(root, "done.part").apply { writeBytes(ByteArray(5)) }
            assertTrue(EngineTransferRecovery.assess(record(partial, 5L, TransferStatus.COMPLETED.name), root) is EngineRecoveryDecision.NotRecoverable)
        } finally { root.deleteRecursively() }
    }
}
