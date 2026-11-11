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
 * Durable app-owned metadata for transfers. Android DownloadManager remains the
 * transfer executor in this prototype; this table is the migration point for
 * the future app-controlled engine.
 */
internal class DownloadTaskStore(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "m1rai_downloads.db", null, 2) {

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
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE downloads ADD COLUMN transfer_status TEXT NOT NULL DEFAULT 'Queued'")
            db.execSQL("ALTER TABLE downloads ADD COLUMN downloaded_bytes INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE downloads ADD COLUMN total_bytes INTEGER")
        }
    }

    fun updateProgress(downloadId: Long, status: String, downloadedBytes: Long, totalBytes: Long) {
        val values = ContentValues().apply {
            put("transfer_status", status)
            put("downloaded_bytes", downloadedBytes.coerceAtLeast(0))
            if (totalBytes < 0) putNull("total_bytes") else put("total_bytes", totalBytes)
        }
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
}
