package com.m1rai.downloadmanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class EngineTransferRunnerTest {
    private class MemoryPersistence(initial: EngineTransferRecord) : EngineTransferPersistence {
        var record: EngineTransferRecord? = initial
        override fun find(taskId: String) = record?.takeIf { it.taskId == taskId }
        override fun insertIfAbsent(record: EngineTransferRecord) = false
        override fun update(record: EngineTransferRecord): Boolean {
            if (this.record?.taskId != record.taskId) return false
            this.record = record
            return true
        }
    }

    @Test fun completedTransferPersistsTerminalStateAndValidators() {
        val root = Files.createTempDirectory("m1rai-runner-complete").toFile()
        try {
            val partial = File(EnginePartialFileStore(root).prepare("task-1").absolutePath)
            val initial = EngineTransferRecord("task-1", "https://example.com/a.bin", partial.absolutePath, "a.bin", createdAt = 1L, updatedAt = 1L)
            val persistence = MemoryPersistence(initial)
            val runner = EngineTransferRunner(root, persistence, EngineTransferExecutor { _, file, _, _, progress ->
                file.writeBytes(byteArrayOf(1, 2, 3))
                progress(TransferProgress(3L, 3L))
                HttpTransferResult.Completed(file, 3L, 3L, TransferValidators(etag = "\"v1\""))
            }, EngineTransferPublisher { _, _, _ -> "published" }, now = { 99L })

            val result = runner.run("task-1", TransferControl())
            assertTrue(result is EngineTransferRunResult.Finished)
            val saved = persistence.record!!
            assertEquals(TransferStatus.COMPLETED.name, saved.status)
            assertEquals(3L, saved.downloadedBytes)
            assertEquals(3L, saved.totalBytes)
            assertEquals("\"v1\"", saved.etag)
            assertEquals(99L, saved.updatedAt)
        } finally { root.deleteRecursively() }
    }

    @Test fun failedTransferPersistsFailureWithoutChangingLegacyRecords() {
        val root = Files.createTempDirectory("m1rai-runner-failed").toFile()
        try {
            val partial = EnginePartialFileStore(root).prepare("task-2")
            val initial = EngineTransferRecord("task-2", "https://example.com/b.bin", partial.absolutePath, "b.bin", createdAt = 1L, updatedAt = 1L)
            val persistence = MemoryPersistence(initial)
            val runner = EngineTransferRunner(root, persistence, EngineTransferExecutor { _, _, _, _, _ ->
                HttpTransferResult.Failed("network unavailable")
            }, EngineTransferPublisher { _, _, _ -> "published" }, now = { 100L })

            runner.run("task-2", TransferControl())
            assertEquals(TransferStatus.FAILED.name, persistence.record!!.status)
            assertEquals("network unavailable", persistence.record!!.errorMessage)
        } finally { root.deleteRecursively() }
    }

    @Test fun publicationFailureCanRetryWithoutRunningNetworkEngineAgain() {
        val root = Files.createTempDirectory("m1rai-publish-retry").toFile()
        try {
            val partial = EnginePartialFileStore(root).prepare("task-publish")
            partial.writeBytes(byteArrayOf(4, 5, 6))
            val initial = EngineTransferRecord("task-publish", "https://example.com/file.bin", partial.absolutePath, "file.bin", status = TransferStatus.PUBLISH_FAILED.name, downloadedBytes = 3L, totalBytes = 3L, createdAt = 1L, updatedAt = 1L)
            val persistence = MemoryPersistence(initial)
            var engineCalls = 0
            val runner = EngineTransferRunner(root, persistence, EngineTransferExecutor { _, _, _, _, _ -> engineCalls++; throw AssertionError("Must not redownload") }, EngineTransferPublisher { _, _, _ -> "published" })
            val result = runner.run("task-publish", TransferControl())
            assertTrue(result is EngineTransferRunResult.Finished)
            assertEquals(0, engineCalls)
            assertEquals(TransferStatus.COMPLETED.name, persistence.record!!.status)
        } finally { root.deleteRecursively() }
    }

    @Test fun failedPublicationDoesNotMarkTransferCompleted() {
        val root = Files.createTempDirectory("m1rai-publish-failed").toFile()
        try {
            val partial = EnginePartialFileStore(root).prepare("task-publish-fail")
            val persistence = MemoryPersistence(EngineTransferRecord("task-publish-fail", "https://example.com/file.bin", partial.absolutePath, "file.bin", createdAt = 1L, updatedAt = 1L))
            val runner = EngineTransferRunner(root, persistence, EngineTransferExecutor { _, file, _, _, progress -> file.writeBytes(byteArrayOf(1, 2, 3)); progress(TransferProgress(3L, 3L)); HttpTransferResult.Completed(file, 3L, 3L, TransferValidators(etag = "\"v1\"")) }, EngineTransferPublisher { _, _, _ -> throw SecurityException("Storage permission required") })
            runner.run("task-publish-fail", TransferControl())
            assertEquals(TransferStatus.PUBLISH_FAILED.name, persistence.record!!.status)
            assertEquals(3L, persistence.record!!.downloadedBytes)
            assertTrue(partial.isFile)
        } finally { root.deleteRecursively() }
    }

    @Test fun unknownTaskDoesNotInvokeEngine() {
        val root = Files.createTempDirectory("m1rai-runner-missing").toFile()
        try {
            val partial = EnginePartialFileStore(root).prepare("task-3")
            val initial = EngineTransferRecord("task-3", "https://example.com/c.bin", partial.absolutePath, "c.bin", createdAt = 1L, updatedAt = 1L)
            val persistence = MemoryPersistence(initial)
            val runner = EngineTransferRunner(root, persistence, EngineTransferExecutor { _, _, _, _, _ ->
                throw AssertionError("Engine must not run")
            }, EngineTransferPublisher { _, _, _ -> "unused" })
            assertTrue(runner.run("missing", TransferControl()) is EngineTransferRunResult.NotStarted)
        } finally { root.deleteRecursively() }
    }
}
