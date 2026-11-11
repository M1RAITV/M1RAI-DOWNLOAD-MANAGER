package com.m1rai.downloadmanager

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineTransferRecordTest {
    private fun record(bytes: Long) = EngineTransferRecord(
        taskId = "task-1",
        url = "https://example.com/archive.zip",
        partialPath = "/app/cache/task-1.part",
        fileName = "archive.zip",
        downloadedBytes = bytes,
        createdAt = 1L,
        updatedAt = 2L
    )

    @Test fun acceptsOnlyAnExactDatabaseAndPartialFileLengthMatch() {
        assertTrue(record(128L).isPartialFileConsistent(128L))
        assertFalse(record(128L).isPartialFileConsistent(127L))
        assertFalse(record(128L).isPartialFileConsistent(129L))
    }

    @Test fun rejectsNegativePartialFileLengths() {
        assertFalse(record(0L).isPartialFileConsistent(-1L))
    }
}
