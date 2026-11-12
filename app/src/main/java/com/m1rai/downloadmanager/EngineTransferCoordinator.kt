package com.m1rai.downloadmanager

import java.io.File

/**
 * Narrow persistence boundary for coordinating engine task metadata.
 * Implementations must make insertIfAbsent atomic for a task ID.
 */
internal interface EngineTransferPersistence {
    fun find(taskId: String): EngineTransferRecord?
    fun insertIfAbsent(record: EngineTransferRecord): Boolean
    fun update(record: EngineTransferRecord): Boolean
}

/**
 * Coordinates task identity, private partial-file allocation, metadata creation,
 * and the conservative recovery gate. It never starts network work or schedules jobs.
 */
internal class EngineTransferCoordinator(
    private val partialFiles: EnginePartialFileStore,
    private val appPrivateDirectory: File,
    private val persistence: EngineTransferPersistence,
    private val now: () -> Long = System::currentTimeMillis
) {
    fun createQueuedTransfer(
        taskId: String,
        url: String,
        fileName: String,
        mimeType: String? = null
    ): EngineTransferRecord {
        require(DownloadLinkRules.isValidHttpUrl(url)) {
            "Only valid HTTP and HTTPS URLs are supported."
        }
        require(fileName.isNotBlank()) { "A destination filename is required." }
        require(fileName != "." && fileName != ".." && !fileName.contains('/') && !fileName.contains('\\')) {
            "The destination filename must not contain a path."
        }
        require(persistence.find(taskId) == null) {
            "A transfer with this task ID already exists."
        }

        val timestamp = now()
        val partialFile = partialFiles.prepare(taskId)
        val record = EngineTransferRecord(
            taskId = taskId,
            url = url,
            partialPath = partialFile.absolutePath,
            fileName = fileName,
            mimeType = mimeType,
            status = TransferStatus.QUEUED.name,
            downloadedBytes = 0L,
            totalBytes = null,
            etag = null,
            lastModified = null,
            errorMessage = null,
            createdAt = timestamp,
            updatedAt = timestamp
        )
        check(persistence.insertIfAbsent(record)) {
            "A transfer with this task ID was created concurrently."
        }
        return record
    }

    fun assessRecovery(taskId: String): EngineRecoveryDecision {
        val record = persistence.find(taskId)
            ?: return EngineRecoveryDecision.NotRecoverable("The transfer task does not exist.")
        return EngineTransferRecovery.assess(record, appPrivateDirectory)
    }
}
