package com.m1rai.downloadmanager

/**
 * Pure rules for presenting durable download records that are no longer
 * present in Android DownloadManager.
 */
internal object DownloadHistoryPolicy {
    fun statusWhenMissing(savedStatus: String): String = when (savedStatus) {
        "Queued", "Downloading", "Paused" -> "Interrupted"
        else -> savedStatus
    }

    fun progressPercent(downloadedBytes: Long, totalBytes: Long): Int {
        if (totalBytes <= 0L) return 0
        val safeDownloaded = downloadedBytes.coerceAtLeast(0L)
        return ((safeDownloaded.toDouble() / totalBytes.toDouble()) * 100.0)
            .toInt()
            .coerceIn(0, 100)
    }
}
