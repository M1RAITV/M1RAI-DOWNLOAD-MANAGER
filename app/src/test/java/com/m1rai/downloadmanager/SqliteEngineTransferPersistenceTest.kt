package com.m1rai.downloadmanager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SqliteEngineTransferPersistenceTest {
    private lateinit var context: Context
    private lateinit var store: DownloadTaskStore
    private lateinit var persistence: SqliteEngineTransferPersistence

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("m1rai_downloads.db")
        store = DownloadTaskStore(context)
        persistence = SqliteEngineTransferPersistence(store)
    }

    @After fun tearDown() {
        store.close()
        context.deleteDatabase("m1rai_downloads.db")
    }

    @Test fun insertsAndReadsBackCompleteEngineRecord() {
        val record = record("round-trip", downloadedBytes = 42L, etag = "\"strong-tag\"")

        assertTrue(persistence.insertIfAbsent(record))
        assertEquals(record, persistence.find(record.taskId))
    }

    @Test fun duplicateInsertDoesNotReplaceExistingRecord() {
        val original = record("same-task", url = "https://example.com/original.bin")
        val attemptedReplacement = record(
            "same-task",
            url = "https://example.com/replacement.bin",
            fileName = "replacement.bin",
            createdAt = 99L
        )

        assertTrue(persistence.insertIfAbsent(original))
        assertFalse(persistence.insertIfAbsent(attemptedReplacement))
        assertEquals(original, persistence.find("same-task"))
    }

    @Test fun updateChangesOnlyAnExistingEngineRecord() {
        val original = record("update-task")
        assertTrue(persistence.insertIfAbsent(original))
        val updated = original.copy(
            status = TransferStatus.DOWNLOADING.name,
            downloadedBytes = 64L,
            totalBytes = 128L,
            etag = "\"strong-tag\"",
            updatedAt = 2L
        )

        assertTrue(persistence.update(updated))
        assertEquals(updated, persistence.find("update-task"))
        assertFalse(persistence.update(updated.copy(taskId = "missing-task")))
        assertEquals(1, store.allEngineTransfers().size)
    }

    @Test fun engineInsertLeavesLegacyDownloadRowsUntouched() {
        val legacy = StoredDownload(
            downloadId = 77L,
            url = "https://example.com/legacy.zip",
            fileName = "legacy.zip",
            mimeType = "application/zip",
            expectedBytes = 100L,
            createdAt = 10L
        )
        store.save(legacy)
        assertNotNull(store.find(77L))

        assertTrue(persistence.insertIfAbsent(record("engine-task")))

        assertEquals(legacy, store.find(77L))
        assertEquals(1, store.all().size)
        assertEquals(1, store.allEngineTransfers().size)
    }

    private fun record(
        taskId: String,
        url: String = "https://example.com/file.bin",
        fileName: String = "file.bin",
        downloadedBytes: Long = 0L,
        etag: String? = null,
        createdAt: Long = 1L
    ) = EngineTransferRecord(
        taskId = taskId,
        url = url,
        partialPath = File(context.filesDir, "$taskId.part").absolutePath,
        fileName = fileName,
        status = TransferStatus.QUEUED.name,
        downloadedBytes = downloadedBytes,
        totalBytes = null,
        etag = etag,
        lastModified = null,
        errorMessage = null,
        createdAt = createdAt,
        updatedAt = createdAt
    )
}
