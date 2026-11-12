package com.m1rai.downloadmanager

import android.app.job.JobInfo
import android.content.ComponentName
import android.os.Build
import android.os.PersistableBundle

/**
 * Constructs the Android 14+ scheduling specification only.
 * No JobService is registered or scheduled by this contract.
 */
internal object UserInitiatedTransferJobContract {
    const val EXTRA_TASK_ID = "engine_transfer_task_id"

    fun build(jobId: Int, service: ComponentName, taskId: String, sdkInt: Int = Build.VERSION.SDK_INT): JobInfo {
        require(sdkInt >= 34) { "User-initiated transfer jobs require API 34+." }
        require(jobId > 0) { "Job ID must be positive." }
        require(Regex("[A-Za-z0-9_-]{1,80}").matches(taskId)) { "Invalid engine transfer task ID." }

        val extras = PersistableBundle().apply { putString(EXTRA_TASK_ID, taskId) }
        return JobInfo.Builder(jobId, service)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setUserInitiated(true)
            .setExtras(extras)
            .build()
    }
}
