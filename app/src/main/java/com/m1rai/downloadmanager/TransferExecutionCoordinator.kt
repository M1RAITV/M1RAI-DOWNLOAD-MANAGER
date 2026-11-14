package com.m1rai.downloadmanager

/**
 * Routes an already-persisted engine task to the Android execution mechanism.
 *
 * Call only as the direct result of a visible user action. API 21–25 deliberately
 * stay on the existing DownloadManager path; this class does not enqueue legacy
 * downloads or change the current UI flow.
 */
internal class TransferExecutionCoordinator(
    private val sdkInt: Int,
    private val launcher: TransferExecutionLauncher
) {
    fun launch(taskId: String): TransferLaunchResult {
        require(TASK_ID.matches(taskId)) { "Invalid transfer task ID." }
        return when (TransferExecutionPolicy.plan(sdkInt).mode) {
            TransferExecutionMode.LEGACY_DOWNLOAD_MANAGER ->
                TransferLaunchResult.LegacyDownloadManagerRequired
            TransferExecutionMode.DATA_SYNC_FOREGROUND_SERVICE ->
                runCatching {
                    launcher.startForegroundService(taskId)
                    TransferLaunchResult.StartedForegroundService
                }.getOrElse { TransferLaunchResult.Failed(it.message ?: "Could not start the transfer service.") }
            TransferExecutionMode.USER_INITIATED_DATA_TRANSFER_JOB ->
                runCatching {
                    if (launcher.scheduleUserInitiatedJob(taskId)) {
                        TransferLaunchResult.ScheduledUserInitiatedJob
                    } else {
                        TransferLaunchResult.Failed("Android did not accept the user-initiated transfer job.")
                    }
                }.getOrElse { TransferLaunchResult.Failed(it.message ?: "Could not schedule the transfer job.") }
        }
    }

    companion object {
        private val TASK_ID = Regex("[A-Za-z0-9_-]{1,80}")
    }
}

internal interface TransferExecutionLauncher {
    fun startForegroundService(taskId: String)
    fun scheduleUserInitiatedJob(taskId: String): Boolean
}

internal sealed interface TransferLaunchResult {
    data object LegacyDownloadManagerRequired : TransferLaunchResult
    data object StartedForegroundService : TransferLaunchResult
    data object ScheduledUserInitiatedJob : TransferLaunchResult
    data class Failed(val reason: String) : TransferLaunchResult
}
