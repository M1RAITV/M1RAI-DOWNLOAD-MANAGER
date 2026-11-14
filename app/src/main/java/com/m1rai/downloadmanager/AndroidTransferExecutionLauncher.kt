package com.m1rai.downloadmanager

import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build

/** Android platform adapter. Invoke only from a visible user-initiated action. */
internal class AndroidTransferExecutionLauncher(
    context: Context
) : TransferExecutionLauncher {
    private val appContext = context.applicationContext

    override fun startForegroundService(taskId: String) {
        EngineTransferForegroundService.start(appContext, taskId)
    }

    override fun scheduleUserInitiatedJob(taskId: String): Boolean {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            "User-initiated transfer jobs require Android 14 or later."
        }
        val scheduler = appContext.getSystemService(JobScheduler::class.java)
            ?: error("Android JobScheduler is unavailable.")
        val component = ComponentName(appContext, UserInitiatedTransferJobService::class.java)
        val jobId = stableJobId(taskId)
        val pending = scheduler.getPendingJob(jobId)
        if (pending != null) {
            val existingTaskId = pending.extras.getString(UserInitiatedTransferJobContract.EXTRA_TASK_ID)
            check(existingTaskId == taskId) {
                "A different transfer already owns this job identifier."
            }
            return true
        }
        val job = UserInitiatedTransferJobContract.build(jobId, component, taskId)
        return scheduler.schedule(job) == JobScheduler.RESULT_SUCCESS
    }

    private fun stableJobId(taskId: String): Int {
        val value = taskId.hashCode() and Int.MAX_VALUE
        return if (value == 0) 1 else value
    }
}
