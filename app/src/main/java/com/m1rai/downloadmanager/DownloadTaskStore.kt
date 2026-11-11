package com.m1rai.downloadmanager

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal data class StoredDownload(
    val downloadId: Long,
    val url: String,
    val fileName: String,
    val mimeType: String?,
    val expectedBytes: Long?,
    val createdAt: Long,
    val transferStatus: String = "Queued",
    val downloadedBytes: Long = 0,
    val totalBytes: Long? = null
)

/**
 * Durable metadata for the legacy Android DownloadManager queue and the
 * app-owned HTTP transfer engine. Engine records live in a separate table:
 * legacy download IDs and their meanings are never rewritten.
 */
internal class DownloadTaskStore(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "m1rai_downloads.db", null, 3) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE downloads (
                download_id INTEGER PRIMARY KEY,
                url TEXT NOT NULL,
                file_name TEXT NOT NULL,
                mime_type TEXT,
                expected_bytes INTEGER,
                created_at INTEGER NOT NULL,
                transfer_status TEXT NOT NULL DEFAULT 'Queued',
                downloaded_bytes INTEGER NOT NULL DEFAULT 0,
                total_bytes INTEGER
            )
            """.trimIndent()
        )
        createEngineTransfersTable(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE downloads ADD COLUMN transfer_status TEXT NOT NULL DEFAULT 'Queued'")
            db.execSQL("ALTER TABLE downloads ADD COLUMN downloaded_bytes INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE downloads ADD COLUMN total_bytes INTEGER")
        }
        if (oldVersion < 3) createEngineTransfersTable(db)
    }

    private fun createEngineTransfersTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS engine_transfers (
                task_id TEXT PRIMARY KEY NOT NULL,
                url TEXT NOT NULL,
                partial_path TEXT NOT NULL,
                file_name TEXT NOT NULL,
                mime_type TEXT,
                status TEXT NOT NULL,
                downloaded_bytes INTEGER NOT NULL DEFAULT 0 CHECK(downloaded_bytes >= 0),
                total_bytes INTEGER,
                etag TEXT,
                last_modified TEXT,
                error_message TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_engine_transfers_updated_at ON engine_transfers(updated_at DESC)")
    }

    fun updateProgress(downloadId: Long, status: String, downloadedBytes: Long, totalBytes: Long) {
        val values = ContentValues().apply {
            put("transfer_status", status)
            put("downloaded_bytes", downloadedBytes.coerceAtLeast(0))
            if (totalBytes < 0) putNull("total_bytes") else put("total_bytes", totalBytes)
        }
        writableDatabase.update("downloads", values, "download_id = ?", arrayOf(downloadId.toString()))
    }

    fun markStatus(downloadId: Long, status: String) {
        val values = ContentValues().apply { put("transfer_status", status) }
        writableDatabase.update("downloads", values, "download_id = ?", arrayOf(downloadId.toString()))
    }

    fun save(item: StoredDownload) {
        val values = ContentValues().apply {
            put("download_id", item.downloadId)
            put("url", item.url)
            put("file_name", item.fileName)
            put("mime_type", item.mimeType)
            if (item.expectedBytes == null) putNull("expected_bytes") else put("expected_bytes", item.expectedBytes)
            put("created_at", item.createdAt)
        }
        writableDatabase.insertWithOnConflict("downloads", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    /** Insert or atomically replace only an app-engine task; legacy rows are untouched. */
    fun saveEngineTransfer(item: EngineTransferRecord) {
        val values = engineTransferValues(item)
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.insertWithOnConflict("engine_transfers", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Atomically inserts a new engine task without replacing an existing task.
     * SQLite's primary-key constraint and CONFLICT_IGNORE make this safe against
     * concurrent creators; a conflict returns false and leaves the old row intact.
     */
    fun insertEngineTransferIfAbsent(item: EngineTransferRecord): Boolean {
        val values = engineTransferValues(item)
        return writableDatabase.insertWithOnConflict(
            "engine_transfers",
            null,
            values,
            SQLiteDatabase.CONFLICT_IGNORE
        ) != -1L
    }

    private fun engineTransferValues(item: EngineTransferRecord) = ContentValues().apply {
        require(item.taskId.isNotBlank())
        require(DownloadLinkRules.isValidHttpUrl(item.url))
        require(item.partialPath.isNotBlank() && item.fileName.isNotBlank())
        require(item.downloadedBytes >= 0L)
        require(item.totalBytes == null || item.totalBytes >= 0L)
        put("task_id", item.taskId)
        put("url", item.url)
        put("partial_path", item.partialPath)
        put("file_name", item.fileName)
        put("mime_type", item.mimeType)
        put("status", item.status)
        put("downloaded_bytes", item.downloadedBytes)
        if (item.totalBytes == null) putNull("total_bytes") else put("total_bytes", item.totalBytes)
        put("etag", item.etag)
        put("last_modified", item.lastModified)
        put("error_message", item.errorMessage)
        put("created_at", item.createdAt)
        put("updated_at", item.updatedAt)
    }

    fun findEngineTransfer(taskId: String): EngineTransferRecord? =
        readableDatabase.query(
            "engine_transfers",
            ENGINE_TRANSFER_COLUMNS,
            "task_id = ?",
            arrayOf(taskId),
            null, null, null
        ).use { cursor ->
            if (!cursor.moveToFirst()) null else cursor.toEngineTransferRecord()
        }

    fun allEngineTransfers(): List<EngineTransferRecord> =
        readableDatabase.query(
            "engine_transfers",
            ENGINE_TRANSFER_COLUMNS,
            null, null, null, null, "updated_at DESC"
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.toEngineTransferRecord())
            }
        }

    fun deleteEngineTransfer(taskId: String): Int =
        writableDatabase.delete("engine_transfers", "task_id = ?", arrayOf(taskId))

    fun find(downloadId: Long): StoredDownload? =
        readableDatabase.query(
            "downloads",
            arrayOf("download_id", "url", "file_name", "mime_type", "expected_bytes", "created_at", "transfer_status", "downloaded_bytes", "total_bytes"),
            "download_id = ?",
            arrayOf(downloadId.toString()),
            null, null, null
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            StoredDownload(
                downloadId = cursor.getLong(0),
                url = cursor.getString(1),
                fileName = cursor.getString(2),
                mimeType = cursor.getString(3),
                expectedBytes = if (cursor.isNull(4)) null else cursor.getLong(4),
                createdAt = cursor.getLong(5),
                transferStatus = cursor.getString(6),
                downloadedBytes = cursor.getLong(7),
                totalBytes = if (cursor.isNull(8)) null else cursor.getLong(8)
            )
        }

    fun all(): List<StoredDownload> =
        readableDatabase.query(
            "downloads",
            arrayOf("download_id", "url", "file_name", "mime_type", "expected_bytes", "created_at", "transfer_status", "downloaded_bytes", "total_bytes"),
            null, null, null, null, "created_at DESC"
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(
                    StoredDownload(
                        cursor.getLong(0), cursor.getString(1), cursor.getString(2),
                        cursor.getString(3), if (cursor.isNull(4)) null else cursor.getLong(4),
                        cursor.getLong(5), cursor.getString(6), cursor.getLong(7),
                        if (cursor.isNull(8)) null else cursor.getLong(8)
                    )
                )
            }
        }

    private fun android.database.Cursor.toEngineTransferRecord() = EngineTransferRecord(
        taskId = getString(0),
        url = getString(1),
        partialPath = getString(2),
        fileName = getString(3),
        mimeType = getString(4),
        status = getString(5),
        downloadedBytes = getLong(6),
        totalBytes = if (isNull(7)) null else getLong(7),
        etag = getString(8),
        lastModified = getString(9),
        errorMessage = getString(10),
        createdAt = getLong(11),
        updatedAt = getLong(12)
    )

    companion object {
        private val ENGINE_TRANSFER_COLUMNS = arrayOf(
            "task_id", "url", "partial_path", "file_name", "mime_type", "status",
            "downloaded_bytes", "total_bytes", "etag", "last_modified",
            "error_message", "created_at", "updated_at"
        )
    }
}

internal data class EngineTransferRecord(
    val taskId: String,
    val url: String,
    val partialPath: String,
    val fileName: String,
    val mimeType: String? = null,
    val status: String = TransferStatus.QUEUED.name,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long? = null,
    val etag: String? = null,
    val lastModified: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long,
    val updatedAt: Long
) {
    /** Never resume from a DB offset that disagrees with the actual partial file. */
    fun isPartialFileConsistent(actualFileLength: Long): Boolean =
        actualFileLength >= 0L && actualFileLength == downloadedBytes
}
