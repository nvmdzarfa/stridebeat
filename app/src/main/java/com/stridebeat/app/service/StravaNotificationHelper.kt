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
import com.stridebeat.app.MainActivity
import com.stridebeat.app.R
import com.stridebeat.app.util.StravaFormatter

/**
 * Helper notifikasi hasil upload Strava di latar belakang (PRD §8.4).
 */
object StravaNotificationHelper {

    private const val CHANNEL_ID = "stridebeat_strava_upload_channel"

    fun showUploadSuccess(context: Context, activityName: String, activityId: String) {
        if (!hasPostNotificationPermission(context)) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        ensureChannel(manager)

        val url = StravaFormatter.buildStravaActivityUrl(activityId)
        val openIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            activityId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Upload Strava Selesai")
            .setContentText("\"$activityName\" berhasil diunggah ke Strava. Ketuk untuk membuka.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(activityId.hashCode(), notification)
    }

    fun showUploadFailed(context: Context, activityName: String, reason: String) {
        if (!hasPostNotificationPermission(context)) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        ensureChannel(manager)

        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            activityName.hashCode(),
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Upload Strava Gagal")
            .setContentText("\"$activityName\": $reason")
            .setStyle(NotificationCompat.BigTextStyle().bigText("\"$activityName\" belum terunggah.\n$reason"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(("fail_$activityName").hashCode(), notification)
    }

    private fun hasPostNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Upload Strava",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Status unggah aktivitas StrideBeat ke Strava"
            }
            manager.createNotificationChannel(channel)
        }
    }
}
