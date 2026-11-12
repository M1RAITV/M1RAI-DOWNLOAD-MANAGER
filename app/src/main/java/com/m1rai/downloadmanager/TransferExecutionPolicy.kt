package com.m1rai.downloadmanager

/**
 * Selects the platform execution contract for a user-started transfer.
 * Pure policy only: it does not start services, schedule jobs, request permissions,
 * or alter the existing Android DownloadManager path.
 */
internal enum class TransferExecutionMode {
    LEGACY_DOWNLOAD_MANAGER,
    DATA_SYNC_FOREGROUND_SERVICE,
    USER_INITIATED_DATA_TRANSFER_JOB
}

internal data class TransferExecutionPlan(
    val mode: TransferExecutionMode,
    val requiresNotificationChannel: Boolean,
    val requiresForegroundServiceType: Boolean,
    val requiresUserInitiatedJob: Boolean
)

internal object TransferExecutionPolicy {
    const val API_O = 26
    const val API_34 = 34

    fun plan(sdkInt: Int): TransferExecutionPlan {
        require(sdkInt >= 1) { "Android SDK version must be positive." }
        return when {
            sdkInt >= API_34 -> TransferExecutionPlan(
                TransferExecutionMode.USER_INITIATED_DATA_TRANSFER_JOB,
                requiresNotificationChannel = false,
                requiresForegroundServiceType = false,
                requiresUserInitiatedJob = true
            )
            sdkInt >= API_O -> TransferExecutionPlan(
                TransferExecutionMode.DATA_SYNC_FOREGROUND_SERVICE,
                requiresNotificationChannel = true,
                requiresForegroundServiceType = true,
                requiresUserInitiatedJob = false
            )
            else -> TransferExecutionPlan(
                TransferExecutionMode.LEGACY_DOWNLOAD_MANAGER,
                requiresNotificationChannel = false,
                requiresForegroundServiceType = false,
                requiresUserInitiatedJob = false
            )
        }
    }
}
