package com.m1rai.downloadmanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class EngineTransferCoordinatorTest {
    private class FakePersistence : EngineTransferPersistence {
        private val records = linkedMapOf<String, EngineTransferRecord>()
        override fun find(taskId: String) = records[taskId]
        override fun insertIfAbsent(record: EngineTransferRecord): Boolean {
            if (records.containsKey(record.taskId)) return false
            records[record.taskId] = record
            return true
        }
        override fun update(record: EngineTransferRecord): Boolean {
            if (!records.containsKey(record.taskId)) return false
            records[record.taskId] = record
            return true
        }
    }

    @Test fun createsQueuedRecordWithPrivatePartialPathAndStableTimestamp() {
        val root = Files.createTempDirectory("m1rai-coordinator").toFile()
        try {
            val persistence = FakePersistence()
            val coordinator = EngineTransferCoordinator(EnginePartialFileStore(root), root, persistence) { 1234L }
            val record = coordinator.createQueuedTransfer("task_01", "https://example.com/video.mp4", "video.mp4", "video/mp4")
            assertEquals(TransferStatus.QUEUED.name, record.status)
            assertEquals(0L, record.downloadedBytes)
            assertEquals(1234L, record.createdAt)
            assertEquals(1234L, record.updatedAt)
            assertTrue(File(record.partialPath).isFile)
            assertTrue(File(record.partialPath).canonicalPath.startsWith(root.canonicalPath + File.separator))
            assertEquals(record, persistence.find("task_01"))
        } finally { root.deleteRecursively() }
    }

    @Test fun refusesDuplicateTaskIdsWithoutReplacingExistingRecord() {
        val root = Files.createTempDirectory("m1rai-coordinator-duplicate").toFile()
        try {
            val persistence = FakePersistence()
            val coordinator = EngineTransferCoordinator(EnginePartialFileStore(root), root, persistence) { 1L }
            val first = coordinator.createQueuedTransfer("same-id", "https://example.com/a", "a.bin")
            val error = runCatching { coordinator.createQueuedTransfer("same-id", "https://example.com/b", "b.bin") }.exceptionOrNull()
            assertTrue(error is IllegalArgumentException)
            assertEquals(first, persistence.find("same-id"))
        } finally { root.deleteRecursively() }
    }

    @Test fun missingTaskIsNotRecoverable() {
        val root = Files.createTempDirectory("m1rai-coordinator-missing").toFile()
        try {
            val coordinator = EngineTransferCoordinator(EnginePartialFileStore(root), root, FakePersistence())
            assertTrue(coordinator.assessRecovery("absent") is EngineRecoveryDecision.NotRecoverable)
        } finally { root.deleteRecursively() }
    }
}
