package com.m1rai.downloadmanager

import java.io.File

internal sealed interface EngineRecoveryDecision {
    data class ResumeFromOffset(val partialFile: File, val offset: Long, val validators: TransferValidators) : EngineRecoveryDecision
    data class RestartFromZero(val partialFile: File, val reason: String) : EngineRecoveryDecision
    data class NotRecoverable(val reason: String) : EngineRecoveryDecision
}

/**
 * Conservative process-restart gate. It never treats database metadata alone
 * as proof that a partial file is safe to append to.
 */
internal object EngineTransferRecovery {
    private val recoverableStatuses = setOf(
        TransferStatus.PAUSED.name,
        TransferStatus.INTERRUPTED.name,
        TransferStatus.CONNECTING.name,
        TransferStatus.DOWNLOADING.name
    )

    fun assess(record: EngineTransferRecord, appPrivateDirectory: File): EngineRecoveryDecision {
        if (record.status == TransferStatus.COMPLETED.name ||
            record.status == TransferStatus.CANCELLED.name) {
            return EngineRecoveryDecision.NotRecoverable("This transfer is already terminal.")
        }
        if (record.status !in recoverableStatuses && record.status != TransferStatus.QUEUED.name) {
            return EngineRecoveryDecision.NotRecoverable("This transfer is not eligible for automatic recovery.")
        }

        val root = runCatching { appPrivateDirectory.canonicalFile }.getOrNull()
            ?: return EngineRecoveryDecision.NotRecoverable("The app-private directory is unavailable.")
        val partial = runCatching { File(record.partialPath).canonicalFile }.getOrNull()
            ?: return EngineRecoveryDecision.NotRecoverable("The partial file path is invalid.")
        val rootPrefix = root.path.trimEnd(File.separatorChar) + File.separator
        if (!partial.path.startsWith(rootPrefix)) {
            return EngineRecoveryDecision.NotRecoverable("The partial file is outside app-private storage.")
        }
        if (partial.exists() && !partial.isFile) {
            return EngineRecoveryDecision.NotRecoverable("The partial path is not a regular file.")
        }

        val existingBytes = if (partial.isFile) partial.length() else 0L
        if (existingBytes == 0L) {
            return EngineRecoveryDecision.RestartFromZero(partial, "No verified partial bytes are available.")
        }
        if (!record.isPartialFileConsistent(existingBytes)) {
            return EngineRecoveryDecision.RestartFromZero("The partial file length does not match saved metadata.")
        }

        val etag = record.etag?.takeIf { it.isNotBlank() && !it.startsWith("W/", ignoreCase = true) }
        val lastModified = record.lastModified?.takeIf { it.isNotBlank() }
        if (etag == null && lastModified == null) {
            return EngineRecoveryDecision.RestartFromZero(partial, "No remote representation validator was saved.")
        }
        return EngineRecoveryDecision.ResumeFromOffset(
            partialFile = partial,
            offset = existingBytes,
            validators = TransferValidators(etag = etag, lastModified = lastModified)
        )
    }
}
