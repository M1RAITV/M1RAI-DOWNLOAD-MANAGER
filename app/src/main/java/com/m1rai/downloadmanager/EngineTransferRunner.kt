package com.m1rai.downloadmanager

import java.io.File

internal fun interface EngineTransferPublisher { fun publish(file: File, fileName: String, mimeType: String?): String }

internal fun interface EngineTransferExecutor {
    fun transfer(url: String, partialFile: File, previousValidators: TransferValidators, control: TransferControl, onProgress: (TransferProgress) -> Unit): HttpTransferResult
}

internal sealed interface EngineTransferRunResult {
    data class Finished(val record: EngineTransferRecord) : EngineTransferRunResult
    data class NotStarted(val reason: String) : EngineTransferRunResult
}

/** Blocking orchestration only. The caller owns the worker thread and platform lifecycle. */
internal class EngineTransferRunner(
    private val appPrivateDirectory: File,
    private val persistence: EngineTransferPersistence,
    private val engine: EngineTransferExecutor,
    private val publisher: EngineTransferPublisher,
    private val now: () -> Long = System::currentTimeMillis,
    private val checkpointBytes: Long = 512L * 1024L
) {
    private fun verifiedCompletedPartial(record: EngineTransferRecord): File? {
        val root = runCatching { appPrivateDirectory.canonicalFile }.getOrNull() ?: return null
        val file = runCatching { File(record.partialPath).canonicalFile }.getOrNull() ?: return null
        val prefix = root.path.trimEnd(File.separatorChar) + File.separator
        if (!file.path.startsWith(prefix) || !file.isFile || file.length() <= 0L) return null
        if (file.length() != record.downloadedBytes) return null
        if (record.totalBytes != null && file.length() != record.totalBytes) return null
        return file
    }

    fun run(taskId: String, control: TransferControl): EngineTransferRunResult {
        var record = persistence.find(taskId)
            ?: return EngineTransferRunResult.NotStarted("The transfer task does not exist.")
        if (record.status == TransferStatus.PUBLISH_FAILED.name) {
            val file = verifiedCompletedPartial(record) ?: return EngineTransferRunResult.NotStarted("Completed partial file is missing or inconsistent.")
            return try {
                publisher.publish(file, record.fileName, record.mimeType)
                record = record.copy(status = TransferStatus.COMPLETED.name, errorMessage = null, updatedAt = now())
                check(persistence.update(record))
                EngineTransferRunResult.Finished(record)
            } catch (error: Exception) {
                record = record.copy(errorMessage = error.localizedMessage ?: "Publication failed.", updatedAt = now())
                check(persistence.update(record))
                EngineTransferRunResult.Finished(record)
            }
        }
        val recovery = EngineTransferRecovery.assess(record, appPrivateDirectory)
        val partialFile = when (recovery) {
            is EngineRecoveryDecision.ResumeFromOffset -> recovery.partialFile
            is EngineRecoveryDecision.RestartFromZero -> recovery.partialFile
            is EngineRecoveryDecision.NotRecoverable -> return EngineTransferRunResult.NotStarted(recovery.reason)
        }
        val validators = when (recovery) {
            is EngineRecoveryDecision.ResumeFromOffset -> recovery.validators
            is EngineRecoveryDecision.RestartFromZero -> TransferValidators()
            is EngineRecoveryDecision.NotRecoverable -> TransferValidators()
        }
        var snapshot = TransferSnapshot(
            status = runCatching { TransferStatus.valueOf(record.status) }.getOrDefault(TransferStatus.QUEUED),
            downloadedBytes = if (recovery is EngineRecoveryDecision.RestartFromZero) 0L else record.downloadedBytes,
            totalBytes = if (recovery is EngineRecoveryDecision.RestartFromZero) null else record.totalBytes,
            error = record.errorMessage
        )
        snapshot = when (snapshot.status) {
            TransferStatus.QUEUED -> TransferStateReducer.reduce(snapshot, TransferEvent.Start)
            TransferStatus.PAUSED, TransferStatus.INTERRUPTED -> TransferStateReducer.reduce(snapshot, TransferEvent.Resume)
            TransferStatus.CONNECTING, TransferStatus.DOWNLOADING -> {
                val interrupted = TransferStateReducer.reduce(snapshot, TransferEvent.Interrupt)
                TransferStateReducer.reduce(interrupted, TransferEvent.Resume)
            }
            else -> return EngineTransferRunResult.NotStarted("This transfer is not runnable from its current state.")
        }

        fun save(value: TransferSnapshot, etag: String? = record.etag, lastModified: String? = record.lastModified) {
            record = record.copy(
                status = value.status.name,
                downloadedBytes = value.downloadedBytes,
                totalBytes = value.totalBytes,
                etag = etag,
                lastModified = lastModified,
                errorMessage = value.error,
                updatedAt = now()
            )
            check(persistence.update(record)) { "The transfer record could not be updated." }
        }

        save(snapshot, validators.etag, validators.lastModified)
        var lastCheckpointBytes = snapshot.downloadedBytes
        var resultValidators = validators
        val result = engine.transfer(record.url, partialFile, validators, control) { progress ->
            if (snapshot.status == TransferStatus.CONNECTING) {
                snapshot = TransferStateReducer.reduce(snapshot, TransferEvent.Connected(progress.totalBytes))
            }
            snapshot = TransferStateReducer.reduce(snapshot, TransferEvent.Progress(progress.downloadedBytes, progress.totalBytes))
            if (snapshot.downloadedBytes - lastCheckpointBytes >= checkpointBytes.coerceAtLeast(64L * 1024L)) {
                // Validators are updated from the final engine result. A crash before then
                // safely falls back to a zero-based restart rather than an unsafe append.
                save(snapshot, validators.etag, validators.lastModified)
                lastCheckpointBytes = snapshot.downloadedBytes
            }
        }

        when (result) {
            is HttpTransferResult.Completed -> {
                if (snapshot.status == TransferStatus.CONNECTING) snapshot = TransferStateReducer.reduce(snapshot, TransferEvent.Connected(result.totalBytes))
                snapshot = TransferStateReducer.reduce(snapshot, TransferEvent.Progress(result.downloadedBytes, result.totalBytes))
                resultValidators = result.validators
                try {
                    publisher.publish(partialFile, record.fileName, record.mimeType)
                    snapshot = TransferStateReducer.reduce(snapshot, TransferEvent.Complete)
                } catch (error: Exception) {
                    snapshot = snapshot.copy(status = TransferStatus.PUBLISH_FAILED, error = error.localizedMessage ?: "Publication failed.")
                }
            }
            is HttpTransferResult.Paused -> {
                if (snapshot.status == TransferStatus.CONNECTING) snapshot = TransferStateReducer.reduce(snapshot, TransferEvent.Connected(result.totalBytes))
                snapshot = TransferStateReducer.reduce(snapshot, TransferEvent.Progress(result.downloadedBytes, result.totalBytes))
                snapshot = TransferStateReducer.reduce(snapshot, TransferEvent.Pause)
                resultValidators = result.validators
            }
            is HttpTransferResult.Cancelled -> snapshot = TransferStateReducer.reduce(snapshot, TransferEvent.Cancel)
            is HttpTransferResult.Failed -> snapshot = TransferStateReducer.reduce(snapshot, TransferEvent.Fail(result.message))
        }
        save(snapshot, resultValidators.etag, resultValidators.lastModified)
        return EngineTransferRunResult.Finished(record)
    }
}
