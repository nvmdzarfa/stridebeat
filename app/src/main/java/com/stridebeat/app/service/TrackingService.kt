package com.stridebeat.app.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.stridebeat.app.MainActivity
import com.stridebeat.app.R
import com.stridebeat.app.domain.model.WorkoutType
import com.stridebeat.app.util.TrackingUtils
import kotlinx.coroutines.*
import java.util.Locale

/**
 * Foreground Service untuk melacak lokasi GPS dan menghitung durasi olahraga.
 * Sesuai PRD §5 (M2) & §7.4:
 * Service berjalan terus di latar belakang (foreground notification) sehingga
 * tracking olahraga TIDAK MATI saat layar HP dimatikan atau pengguna membuka aplikasi lain.
 */
class TrackingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    TrackingStateManager.onNewLocation(
                        lat = location.latitude,
                        lng = location.longitude,
                        speedMetersPerSec = location.speed,
                        accuracyMeters = location.accuracy
                    )
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val typeName = intent.getStringExtra(EXTRA_WORKOUT_TYPE) ?: WorkoutType.RUN.name
                val workoutType = try { WorkoutType.valueOf(typeName) } catch (_: Exception) { WorkoutType.RUN }
                startForegroundTracking(workoutType)
            }
            ACTION_PAUSE -> {
                TrackingStateManager.pauseSession(manual = true)
                updateNotification()
            }
            ACTION_RESUME -> {
                TrackingStateManager.resumeSession()
                updateNotification()
            }
            ACTION_STOP -> {
                stopTrackingService()
            }
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startForegroundTracking(workoutType: WorkoutType) {
        TrackingStateManager.startSession(workoutType)

        val notification = buildTrackingNotification("Mempersiapkan GPS...", "00:00")
        startForeground(NOTIFICATION_ID, notification)

        // Request high accuracy location updates
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L).apply {
            setMinUpdateIntervalMillis(1000L)
            setMinUpdateDistanceMeters(1.0f)
            setWaitForAccurateLocation(false)
        }.build()

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (_: SecurityException) {
            // Izin lokasi belum diberikan
        }

        // Timer ticker setiap detik
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                TrackingStateManager.addDurationTick(1000L)
                updateNotification()
            }
        }
    }

    private fun updateNotification() {
        if (!TrackingStateManager.isTracking.value) return

        val durationStr = TrackingUtils.formatDuration(TrackingStateManager.elapsedDurationMs.value)
        val distanceKm = TrackingStateManager.distanceMeters.value / 1000.0
        val paceStr = TrackingUtils.formatPace(TrackingStateManager.currentPaceMinPerKm.value)
        val status = if (TrackingStateManager.isPaused.value) " (Dijeda)" else if (TrackingStateManager.isAutoPaused.value) " (Auto-Pause)" else ""

        val content = String.format(Locale.US, "%.2f km • Pace %s%s", distanceKm, paceStr, status)
        val notification = buildTrackingNotification(content, durationStr)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildTrackingNotification(content: String, duration: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeAction = if (TrackingStateManager.isPaused.value || TrackingStateManager.isAutoPaused.value) {
            val resumeIntent = Intent(this, TrackingService::class.java).apply { action = ACTION_RESUME }
            val resumePending = PendingIntent.getService(this, 1, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action.Builder(0, "Lanjut", resumePending).build()
        } else {
            val pauseIntent = Intent(this, TrackingService::class.java).apply { action = ACTION_PAUSE }
            val pausePending = PendingIntent.getService(this, 2, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action.Builder(0, "Jeda", pausePending).build()
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("StrideBeat — $duration")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(pauseResumeAction)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .build()
    }

    private fun stopTrackingService() {
        timerJob?.cancel()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopTrackingService()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sesi Olahraga Berjalan",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifikasi aktif saat sesi olahraga sedang direkam dengan GPS"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "stridebeat_tracking_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ACTION_START_TRACKING"
        const val ACTION_PAUSE = "ACTION_PAUSE_TRACKING"
        const val ACTION_RESUME = "ACTION_RESUME_TRACKING"
        const val ACTION_STOP = "ACTION_STOP_TRACKING"

        const val EXTRA_WORKOUT_TYPE = "EXTRA_WORKOUT_TYPE"

        fun start(context: Context, type: WorkoutType) {
            val intent = Intent(context, TrackingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_WORKOUT_TYPE, type.name)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pause(context: Context) {
            val intent = Intent(context, TrackingService::class.java).apply { action = ACTION_PAUSE }
            context.startService(intent)
        }

        fun resume(context: Context) {
            val intent = Intent(context, TrackingService::class.java).apply { action = ACTION_RESUME }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, TrackingService::class.java).apply { action = ACTION_STOP }
            context.startService(intent)
        }
    }
}
