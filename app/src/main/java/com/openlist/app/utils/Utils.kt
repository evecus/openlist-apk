package com.openlist.app.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import com.openlist.app.data.service.DownloadService

object DownloadHelper {

    fun startDownload(context: Context, url: String, filename: String) {
        val intent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_DOWNLOAD
            putExtra(DownloadService.EXTRA_URL, url)
            putExtra(DownloadService.EXTRA_FILENAME, filename)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}

fun Context.isTablet(): Boolean {
    val config = resources.configuration
    val screenLayout = config.screenLayout and android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK
    return screenLayout >= android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE
}

fun Context.isLandscape(): Boolean {
    return resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
}
