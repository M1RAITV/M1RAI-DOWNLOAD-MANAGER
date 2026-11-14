package com.m1rai.downloadmanager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Android 14+ user-initiated transfer execution.
 * The scheduler owns the job lifecycle; the worker thread owns blocking HTTP work.
 */
internal class UserInitiatedTransferJobService : JobService() {
    private data class ActiveJob(
        val params: JobParameters,
        val taskId: String,
        val control: TransferControl = TransferControl(),
        val stopped: AtomicBoolean = AtomicBoolean(false),
        val finished: AtomicBoolean = AtomicBoolean(false)
    )

    private val executor = Executors.newCachedThreadPool()
    private val activeJobs = ConcurrentHashMap<Int, ActiveJob>()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartJob(params: JobParameters): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            jobFinished(params, false)
            return false
        }
        val taskId = params.extras.getString(UserInitiatedTransferJobContract.EXTRA_TASK_ID)
        if (taskId == null || !TASK_ID.matches(taskId)) {
            setNotification(params, params.jobId, notification("Download not started", "Invalid transfer task identifier."), JOB_END_NOTIFICATION_POLICY_REMOVE)
            jobFinished(params, false)
            return false
        }

        val active = ActiveJob(params, taskId)
        if (activeJobs.putIfAbsent(params.jobId, active) != null) {
            setNotification(params, params.jobId, notification("Download not started", "This transfer job is already active."), JOB_END_NOTIFICATION_POLICY_REMOVE)
            jobFinished(params, false)
            return false
        }

        // UIDT jobs must publish their notification immediately (within 10 seconds).
        setNotification(
            params,
            params.jobId,
            notification("Preparing download", "M1RAI Downloader is preparing your file."),
            JOB_END_NOTIFICATION_POLICY_REMOVE
        )
        executor.execute { runTransfer(active) }
        return true
    }

    private fun runTransfer(active: ActiveJob) {
        val store = DownloadTaskStore(this)
        try {
            val runner = EngineTransferRunner(
                appPrivateDirectory = filesDir,
                persistence = SqliteEngineTransferPersistence(store),
                engine = EngineTransferExecutor { url, partial, validators, control, onProgress ->
                    HttpFileTransferEngine().transfer(url, partial, validators, control) { progress ->
                        publishNotification(active, "Downloading", progressMessage(progress), progress)
                        onProgress(progress)
                    }
                },
                publisher = EngineTransferPublisher { file, fileName, mimeType ->
                    AndroidDownloadPublisher(this).publish(file, fileName, mimeType).toString()
                }
            )
            publishNotification(active, "Downloading", "M1RAI Downloader is transferring your file.", null)
            val result = runner.run(active.taskId, active.control)
            if (active.stopped.get()) return
            when (result) {
                is EngineTransferRunResult.Finished -> {
                    val complete = result.record.status == TransferStatus.COMPLETED.name
                    publishNotification(
                        active,
                        if (complete) "Download complete" else result.record.status.replace('_', ' '),
                        result.record.errorMessage ?: result.record.fileName,
                        null
                    )
                }
                is EngineTransferRunResult.NotStarted ->
                    publishNotification(active, "Download not started", result.reason, null)
            }
        } catch (error: Exception) {
            if (!active.stopped.get()) {
                publishNotification(active, "Download failed", error.localizedMessage ?: "The transfer could not be completed.", null)
            }
        } finally {
            store.close()
            activeJobs.remove(active.params.jobId, active)
            if (!active.stopped.get() && active.finished.compareAndSet(false, true)) {
                jobFinished(active.params, false)
            }
        }
    }

    override fun onStopJob(params: JobParameters): Boolean {
        val active = activeJobs[params.jobId] ?: return true
        active.stopped.set(true)
        active.control.pause()
        // Persist the partial transfer as paused; let JobScheduler retry if the system stopped it.
        return true
    }

    private fun publishNotification(active: ActiveJob, title: String, message: String, progress: TransferProgress?) {
        if (active.stopped.get() || Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        setNotification(
            active.params,
            active.params.jobId,
            notification(title, message, progress),
            JOB_END_NOTIFICATION_POLICY_REMOVE
        )
    }

    private fun progressMessage(progress: TransferProgress): String {
        val total = progress.totalBytes?.takeIf { it > 0L }
        return if (total != null) "${sizeText(progress.downloadedBytes)} of ${sizeText(total)}"
        else "${sizeText(progress.downloadedBytes)} downloaded"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "M1RAI downloads", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "Progress and status for active downloads" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun notification(title: String, message: String, progress: TransferProgress? = null): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(this, CHANNEL_ID)
        else @Suppress("DEPRECATION") Notification.Builder(this)
        return builder.setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(message)
            .setOnlyAlertOnce(true)
            .setOngoing(title == "Preparing download" || title == "Downloading")
            .apply {
                if (progress != null) {
                    val total = progress.totalBytes ?: -1L
                    val known = total > 0L
                    val percent = if (known) ((progress.downloadedBytes.toDouble() / total) * 100.0).toInt().coerceIn(0, 100) else 0
                    setProgress(100, percent, !known)
                }
            }
            .build()
    }

    override fun onDestroy() {
        activeJobs.values.forEach { it.stopped.set(true); it.control.pause() }
        executor.shutdownNow()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "m1rai_download_transfers"
        private val TASK_ID = Regex("[A-Za-z0-9_-]{1,80}")
    }
}
