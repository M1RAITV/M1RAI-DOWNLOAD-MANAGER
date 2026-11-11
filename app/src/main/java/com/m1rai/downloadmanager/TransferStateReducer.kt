package com.m1rai.downloadmanager

internal enum class TransferStatus { QUEUED, CONNECTING, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED, INTERRUPTED }

internal data class TransferSnapshot(
    val status: TransferStatus = TransferStatus.QUEUED,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long? = null,
    val error: String? = null
) {
    val progressPercent: Int
        get() = totalBytes?.takeIf { it > 0L }?.let {
            ((downloadedBytes.coerceAtLeast(0L).toDouble() / it) * 100.0).toInt().coerceIn(0, 100)
        } ?: 0
}

internal sealed interface TransferEvent {
    data object Start : TransferEvent
    data class Connected(val totalBytes: Long?) : TransferEvent
    data class Progress(val downloadedBytes: Long, val totalBytes: Long?) : TransferEvent
    data object Pause : TransferEvent
    data object Resume : TransferEvent
    data object Complete : TransferEvent
    data class Fail(val reason: String) : TransferEvent
    data object Cancel : TransferEvent
    data object Interrupt : TransferEvent
    data object Retry : TransferEvent
}

internal object TransferStateReducer {
    fun reduce(current: TransferSnapshot, event: TransferEvent): TransferSnapshot = when (event) {
        TransferEvent.Start -> if (current.status == TransferStatus.QUEUED) current.copy(status = TransferStatus.CONNECTING, error = null) else current
        is TransferEvent.Connected -> if (current.status == TransferStatus.CONNECTING) current.copy(status = TransferStatus.DOWNLOADING, totalBytes = event.totalBytes?.takeIf { it >= 0L }, error = null) else current
        is TransferEvent.Progress -> if (current.status == TransferStatus.DOWNLOADING) {
            val safeTotal = event.totalBytes?.takeIf { it >= 0L }
            val nextBytes = event.downloadedBytes.coerceAtLeast(current.downloadedBytes).let { bytes ->
                safeTotal?.takeIf { it > 0L }?.let { bytes.coerceAtMost(it) } ?: bytes
            }
            current.copy(downloadedBytes = nextBytes, totalBytes = safeTotal ?: current.totalBytes)
        } else current
        TransferEvent.Pause -> if (current.status == TransferStatus.DOWNLOADING) current.copy(status = TransferStatus.PAUSED) else current
        TransferEvent.Resume -> if (current.status == TransferStatus.PAUSED || current.status == TransferStatus.INTERRUPTED) current.copy(status = TransferStatus.CONNECTING, error = null) else current
        TransferEvent.Complete -> if (current.status == TransferStatus.DOWNLOADING) current.copy(status = TransferStatus.COMPLETED, downloadedBytes = current.totalBytes?.takeIf { it > 0L } ?: current.downloadedBytes, error = null) else current
        is TransferEvent.Fail -> if (current.status in ACTIVE_STATUSES) current.copy(status = TransferStatus.FAILED, error = event.reason) else current
        TransferEvent.Cancel -> if (current.status in CANCELLABLE_STATUSES) current.copy(status = TransferStatus.CANCELLED, error = null) else current
        TransferEvent.Interrupt -> if (current.status in ACTIVE_STATUSES) current.copy(status = TransferStatus.INTERRUPTED) else current
        TransferEvent.Retry -> if (current.status == TransferStatus.FAILED || current.status == TransferStatus.INTERRUPTED) current.copy(status = TransferStatus.QUEUED, downloadedBytes = 0L, error = null) else current
    }
    private val ACTIVE_STATUSES = setOf(TransferStatus.CONNECTING, TransferStatus.DOWNLOADING)
    private val CANCELLABLE_STATUSES = setOf(TransferStatus.QUEUED, TransferStatus.CONNECTING, TransferStatus.DOWNLOADING, TransferStatus.PAUSED)
}
