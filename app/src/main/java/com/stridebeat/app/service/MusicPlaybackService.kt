package com.stridebeat.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.stridebeat.app.MainActivity
import com.stridebeat.app.R
import com.stridebeat.app.domain.model.MusicSource
import com.stridebeat.app.domain.model.Track

/**
 * Service pemutar musik latar belakang dengan MediaSessionCompat & MediaPlayer.
 * Memutar audio musik ritme workout secara live langsung di dalam aplikasi,
 * dan terintegrasi dengan media controller sistem Android & lockscreen.
 */
class MusicPlaybackService : Service() {

    private lateinit var mediaSession: MediaSessionCompat
    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingUrl: String? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initMediaPlayer()

        mediaSession = MediaSessionCompat(this, "StrideBeatMediaSession").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )

            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    resumeAudio()
                }

                override fun onPause() {
                    pauseAudio()
                }

                override fun onStop() {
                    stopAudio()
                }
            })

            isActive = true
        }
    }

    private fun initMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setOnPreparedListener { mp ->
                    mp.start()
                }
                setOnCompletionListener { mp ->
                    // Putar ulang lagu secara otomatis untuk menjaga ritme olahraga
                    mp.start()
                }
                setOnErrorListener { _, _, _ ->
                    true // Cegah crash jika terjadi kendala jaringan stream
                }
            }
        }
    }

    private fun playAudioUrl(url: String?) {
        if (url.isNullOrBlank()) return
        initMediaPlayer()

        if (currentPlayingUrl != url) {
            currentPlayingUrl = url
            try {
                mediaPlayer?.reset()
                mediaPlayer?.setDataSource(url)
                mediaPlayer?.prepareAsync()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            resumeAudio()
        }
    }

    private fun resumeAudio() {
        try {
            if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun pauseAudio() {
        try {
            if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
                mediaPlayer?.pause()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAudio() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            currentPlayingUrl = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = intent?.getStringExtra(EXTRA_TRACK_TITLE) ?: "StrideBeat Music"
        val artist = intent?.getStringExtra(EXTRA_TRACK_ARTIST) ?: "Olahraga & Ritme"
        val isPlaying = intent?.getBooleanExtra(EXTRA_IS_PLAYING, true) ?: true
        val source = intent?.getStringExtra(EXTRA_SOURCE) ?: "SPOTIFY"
        val audioUrl = intent?.getStringExtra(EXTRA_AUDIO_URL)

        if (isPlaying) {
            if (!audioUrl.isNullOrBlank()) {
                playAudioUrl(audioUrl)
            } else {
                resumeAudio()
            }
        } else {
            pauseAudio()
        }

        updateMediaSession(title, artist, isPlaying)
        val notification = buildMediaNotification(title, artist, isPlaying, source)
        startForeground(NOTIFICATION_ID, notification)

        return START_NOT_STICKY
    }

    private fun updateMediaSession(title: String, artist: String, isPlaying: Boolean) {
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
            .build()
        mediaSession.setMetadata(metadata)

        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
            .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f)
            .build()
        mediaSession.setPlaybackState(playbackState)
    }

    private fun buildMediaNotification(
        title: String,
        artist: String,
        isPlaying: Boolean,
        source: String
    ): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPending = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sourceName = if (source == "SPOTIFY") "Spotify" else "YouTube Music"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("$artist • $sourceName")
            .setSubText("StrideBeat Workout Music")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentPending)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()
    }

    override fun onDestroy() {
        stopAudio()
        mediaPlayer?.release()
        mediaPlayer = null
        mediaSession.release()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pemutar Musik Olahraga",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifikasi pemutar musik selama sesi olahraga"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "stridebeat_music_channel"
        const val NOTIFICATION_ID = 2001

        const val EXTRA_TRACK_TITLE = "EXTRA_TRACK_TITLE"
        const val EXTRA_TRACK_ARTIST = "EXTRA_TRACK_ARTIST"
        const val EXTRA_IS_PLAYING = "EXTRA_IS_PLAYING"
        const val EXTRA_SOURCE = "EXTRA_SOURCE"
        const val EXTRA_AUDIO_URL = "EXTRA_AUDIO_URL"

        fun update(context: Context, track: Track?, isPlaying: Boolean, source: MusicSource) {
            if (track == null) return
            val intent = Intent(context, MusicPlaybackService::class.java).apply {
                putExtra(EXTRA_TRACK_TITLE, track.title)
                putExtra(EXTRA_TRACK_ARTIST, track.artist)
                putExtra(EXTRA_IS_PLAYING, isPlaying)
                putExtra(EXTRA_SOURCE, source.name)
                putExtra(EXTRA_AUDIO_URL, track.audioUrl)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MusicPlaybackService::class.java)
            context.stopService(intent)
        }
    }
}
