package com.m1rai.downloadmanager

/**
 * Production persistence adapter for the engine coordinator.
 * The insert path delegates to SQLite's atomic primary-key conflict handling.
 */
internal class SqliteEngineTransferPersistence(
    private val store: DownloadTaskStore
) : EngineTransferPersistence {
    override fun find(taskId: String): EngineTransferRecord? =
        store.findEngineTransfer(taskId)

    override fun insertIfAbsent(record: EngineTransferRecord): Boolean =
        store.insertEngineTransferIfAbsent(record)
}
