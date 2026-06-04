package com.openlist.app.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.openlist.app.R
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class DownloadService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val notificationManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // No timeout for downloads
        .build()

    companion object {
        const val ACTION_DOWNLOAD = "com.openlist.app.DOWNLOAD"
        const val ACTION_CANCEL = "com.openlist.app.CANCEL"
        const val EXTRA_URL = "url"
        const val EXTRA_FILENAME = "filename"
        const val CHANNEL_ID = "openlist_downloads"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DOWNLOAD -> {
                val url = intent.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
                val filename = intent.getStringExtra(EXTRA_FILENAME) ?: "download"
                startDownload(url, filename, startId)
            }
            ACTION_CANCEL -> {
                serviceScope.cancel()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startDownload(url: String, filename: String, startId: Int) {
        val notification = buildProgressNotification(filename, 0, -1)
        startForeground(NOTIFICATION_ID + startId, notification)

        serviceScope.launch {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    showErrorNotification(filename, "HTTP ${response.code}", startId)
                    return@launch
                }

                val body = response.body ?: run {
                    showErrorNotification(filename, "Empty response", startId)
                    return@launch
                }

                val totalBytes = body.contentLength()
                val destDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                destDir.mkdirs()

                var safeFilename = filename
                var destFile = File(destDir, safeFilename)
                var count = 1
                while (destFile.exists()) {
                    val nameWithoutExt = filename.substringBeforeLast(".")
                    val ext = filename.substringAfterLast(".", "")
                    safeFilename = if (ext.isEmpty()) "${nameWithoutExt}($count)" else "${nameWithoutExt}($count).$ext"
                    destFile = File(destDir, safeFilename)
                    count++
                }

                body.byteStream().use { input ->
                    FileOutputStream(destFile).use { output ->
                        val buffer = ByteArray(8192)
                        var downloadedBytes = 0L
                        var bytes: Int
                        var lastNotifyTime = 0L

                        while (input.read(buffer).also { bytes = it } != -1) {
                            output.write(buffer, 0, bytes)
                            downloadedBytes += bytes

                            val now = System.currentTimeMillis()
                            if (now - lastNotifyTime > 500) {
                                lastNotifyTime = now
                                val progress = if (totalBytes > 0) {
                                    (downloadedBytes * 100 / totalBytes).toInt()
                                } else -1
                                val notification = buildProgressNotification(safeFilename, progress, totalBytes)
                                notificationManager.notify(NOTIFICATION_ID + startId, notification)
                            }
                        }
                    }
                }

                showCompletedNotification(safeFilename, destFile, startId)

            } catch (e: Exception) {
                showErrorNotification(filename, e.message ?: "Download failed", startId)
            } finally {
                stopSelf(startId)
            }
        }
    }

    private fun buildProgressNotification(filename: String, progress: Int, totalBytes: Long) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(getString(R.string.downloading))
            .setContentText(filename)
            .setProgress(100, progress.coerceIn(0, 100), progress < 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                R.drawable.ic_close,
                getString(R.string.cancel),
                PendingIntent.getService(
                    this, 0,
                    Intent(this, DownloadService::class.java).apply { action = ACTION_CANCEL },
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    private fun showCompletedNotification(filename: String, file: File, startId: Int) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_check)
            .setContentTitle(getString(R.string.download_complete))
            .setContentText(filename)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID + startId + 1000, notification)
    }

    private fun showErrorNotification(filename: String, error: String, startId: Int) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_error)
            .setContentTitle(getString(R.string.download_failed))
            .setContentText("$filename: $error")
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID + startId + 2000, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Downloads",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "OpenList file downloads"
        }
        notificationManager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
