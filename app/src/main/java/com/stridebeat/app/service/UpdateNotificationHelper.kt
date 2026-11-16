package com.stridebeat.app.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.stridebeat.app.R
import com.stridebeat.app.data.remote.update.AppUpdateInfo

object UpdateNotificationHelper {

    private const val CHANNEL_ID = "stridebeat_update_channel"
    private const val NOTIFICATION_ID = 3003

    fun showUpdateNotification(context: Context, info: AppUpdateInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pembaruan Aplikasi",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifikasi saat versi terbaru StrideBeat tersedia"
            }
            manager.createNotificationChannel(channel)
        }

        val downloadIntent = Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            downloadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val shortSummary = "Versi v${info.latestVersion} sudah siap diunduh! Ketuk untuk update dari v${info.currentVersion}."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Update StrideBeat v${info.latestVersion} Tersedia")
            .setContentText(shortSummary)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$shortSummary\n\n${info.releaseNotes.take(240)}")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }
}
