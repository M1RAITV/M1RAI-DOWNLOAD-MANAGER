package com.m1rai.downloadmanager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import java.util.concurrent.Executors

/**
 * Executes app-owned HTTP transfers in a user-visible foreground service.
 *
 * This service is intentionally limited to API 26–33. API 34+ uses the
 * user-initiated data-transfer JobService path; API 21–25 keep DownloadManager.
 * The service is implemented but is not yet wired to the download UI.
 */
internal class EngineTransferForegroundService : Service() {
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val taskId = intent?.getStringExtra(EXTRA_TASK_ID)
        startForeground(NOTIFICATION_ID, notification("Preparing download", null))

        executor.execute {
            if (Build.VERSION.SDK_INT !in MIN_API..MAX_API) {
                notifyStatus("Unsupported execution mode", "This service is reserved for Android 8–13.")
            } else if (taskId == null || !TASK_ID.matches(taskId)) {
                notifyStatus("Download not started", "The transfer task identifier is invalid.")
            } else {
                runTransfer(taskId)
            }
            stopSelfResult(startId)
        }
        return START_NOT_STICKY
    }

    private fun runTransfer(taskId: String) {
        val store = DownloadTaskStore(this)
        try {
            val runner = EngineTransferRunner(
                appPrivateDirectory = filesDir,
                persistence = SqliteEngineTransferPersistence(store),
                engine = EngineTransferExecutor { url, partial, validators, control, onProgress ->
                    HttpFileTransferEngine().transfer(url, partial, validators, control, onProgress)
                },
                publisher = EngineTransferPublisher { file, fileName, mimeType ->
                    AndroidDownloadPublisher(this).publish(file, fileName, mimeType).toString()
                }
            )
            notifyStatus("Downloading", "M1RAI Downloader is transferring your file.")
            when (val result = runner.run(taskId, TransferControl())) {
                is EngineTransferRunResult.Finished -> {
                    val completed = result.record.status == TransferStatus.COMPLETED.name
                    notifyStatus(
                        if (completed) "Download complete" else result.record.status.replace('_', ' '),
                        result.record.errorMessage ?: result.record.fileName
                    )
                }
                is EngineTransferRunResult.NotStarted -> notifyStatus("Download not started", result.reason)
            }
        } catch (error: Exception) {
            notifyStatus("Download failed", error.localizedMessage ?: "The transfer could not be completed.")
        } finally {
            store.close()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "M1RAI downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Progress and status for active downloads" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun notification(title: String, message: String?): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(message ?: "Preparing your download")
            .setOngoing(title == "Preparing download" || title == "Downloading")
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun notifyStatus(title: String, message: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification(title, message))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        executor.shutdownNow()
        stopForeground(true)
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "m1rai_download_transfers"
        private const val NOTIFICATION_ID = 8301
        private const val EXTRA_TASK_ID = "engine_transfer_task_id"
        private const val MIN_API = 26
        private const val MAX_API = 33
        private val TASK_ID = Regex("[A-Za-z0-9_-]{1,80}")

        /** Call only from a visible user action; API 34+ must use the job contract. */
        fun start(context: Context, taskId: String) {
            require(Build.VERSION.SDK_INT in MIN_API..MAX_API) {
                "Foreground transfer service is supported only on Android 8–13."
            }
            require(TASK_ID.matches(taskId)) { "Invalid engine transfer task ID." }
            val intent = Intent(context, EngineTransferForegroundService::class.java)
                .putExtra(EXTRA_TASK_ID, taskId)
            context.startForegroundService(intent)
        }
    }
}
