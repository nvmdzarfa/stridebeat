package com.stridebeat.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.stridebeat.app.MainActivity
import com.stridebeat.app.R
import com.stridebeat.app.domain.model.MusicSource
import com.stridebeat.app.domain.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Service pemutar musik latar belakang menggunakan AndroidX Media3 ExoPlayer.
 * Mendukung penuh pemutaran audio HLS (fMP4 / AAC), progressive MP3, dan streaming cloud lainnya
 * serta menampilkan cover art lagu pada media notification bar & lockscreen Android.
 */
class MusicPlaybackService : Service() {

    private lateinit var mediaSession: MediaSessionCompat
    private var exoPlayer: ExoPlayer? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var currentPlayingUrl: String? = null
    private var currentTitle: String = "StrideBeat Music"
    private var currentArtist: String = "Olahraga & Ritme"
    private var currentCoverUrl: String? = null
    private var currentCoverBitmap: Bitmap? = null
    private var isPlayingState: Boolean = false
    private var currentDurationMs: Long = 0L

    override fun onCreate() {
        super.onCreate()
        activeServiceInstance = this
        createNotificationChannel()
        initExoPlayer()

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

                override fun onSkipToNext() {
                    onSkipNextListener?.invoke()
                }

                override fun onSkipToPrevious() {
                    onSkipPreviousListener?.invoke()
                }

                override fun onSeekTo(pos: Long) {
                    exoPlayer?.seekTo(pos)
                    updateMediaSession(currentTitle, currentArtist, isPlayingState, pos, currentDurationMs)
                }
            })

            isActive = true
        }
    }

    private fun initExoPlayer() {
        if (exoPlayer == null) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build()

            exoPlayer = ExoPlayer.Builder(this)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build().apply {
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            when (playbackState) {
                                Player.STATE_READY -> {
                                    val dur = duration
                                    if (dur > 0) {
                                        currentDurationMs = dur
                                        onDurationUpdatedListener?.invoke(dur)
                                    }
                                    updateMediaSession(
                                        title = currentTitle,
                                        artist = currentArtist,
                                        isPlaying = isPlaying,
                                        positionMs = currentPosition,
                                        durationMs = currentDurationMs
                                    )
                                    updateNotification(isPlaying = isPlaying)
                                }
                                Player.STATE_ENDED -> {
                                    isPlayingState = false
                                    updateMediaSession(currentTitle, currentArtist, false, currentDurationMs, currentDurationMs)
                                    updateNotification(isPlaying = false)
                                    onSkipNextListener?.invoke()
                                }
                                else -> {}
                            }
                        }

                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            isPlayingState = isPlaying
                            val pos = currentPosition
                            onPositionUpdatedListener?.invoke(pos)
                            updateMediaSession(
                                title = currentTitle,
                                artist = currentArtist,
                                isPlaying = isPlaying,
                                positionMs = pos,
                                durationMs = currentDurationMs
                            )
                            updateNotification(isPlaying = isPlaying)
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            error.printStackTrace()
                            isPlayingState = false
                            updateNotification(isPlaying = false)
                        }
                    })
                }
        }
    }

    fun seekToPosition(posMs: Long) {
        initExoPlayer()
        exoPlayer?.seekTo(posMs)
        updateMediaSession(currentTitle, currentArtist, isPlayingState, posMs, currentDurationMs)
    }

    private fun playAudio(url: String?) {
        if (url.isNullOrBlank()) return

        initExoPlayer()
        if (currentPlayingUrl != url) {
            currentPlayingUrl = url
            try {
                val mediaItem = MediaItem.fromUri(Uri.parse(url))
                exoPlayer?.apply {
                    setMediaItem(mediaItem)
                    prepare()
                    play()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            resumeAudio()
        }
    }

    private fun resumeAudio() {
        initExoPlayer()
        exoPlayer?.play()
    }

    private fun pauseAudio() {
        exoPlayer?.pause()
    }

    private fun stopAudio() {
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        currentPlayingUrl = null
        isPlayingState = false
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action != null) {
            when (action) {
                ACTION_PLAY -> resumeAudio()
                ACTION_PAUSE -> pauseAudio()
                ACTION_SKIP_NEXT -> onSkipNextListener?.invoke()
                ACTION_SKIP_PREVIOUS -> onSkipPreviousListener?.invoke()
                ACTION_SEEK_TO -> {
                    val pos = intent.getLongExtra(EXTRA_SEEK_POSITION, 0L)
                    seekToPosition(pos)
                }
            }
            return START_NOT_STICKY
        }

        currentTitle = intent?.getStringExtra(EXTRA_TRACK_TITLE) ?: "StrideBeat Music"
        currentArtist = intent?.getStringExtra(EXTRA_TRACK_ARTIST) ?: "Olahraga & Ritme"
        val isPlaying = intent?.getBooleanExtra(EXTRA_IS_PLAYING, true) ?: true
        val audioUrl = intent?.getStringExtra(EXTRA_AUDIO_URL)
        val coverUrl = intent?.getStringExtra(EXTRA_COVER_URL)
        val trackDuration = intent?.getLongExtra(EXTRA_TRACK_DURATION, 0L) ?: 0L

        isPlayingState = isPlaying
        if (trackDuration > 0) {
            currentDurationMs = trackDuration
        }

        val isSameTrack = !audioUrl.isNullOrBlank() && audioUrl == currentPlayingUrl
        if (isPlaying) {
            if (!audioUrl.isNullOrBlank()) {
                playAudio(audioUrl)
            } else {
                resumeAudio()
            }
        } else {
            pauseAudio()
        }

        if (coverUrl != currentCoverUrl) {
            currentCoverUrl = coverUrl
            currentCoverBitmap = null
            if (!coverUrl.isNullOrBlank()) {
                fetchAndApplyCoverArt(coverUrl)
            }
        }

        val currentPos = if (isSameTrack) (exoPlayer?.currentPosition ?: 0L) else 0L
        updateMediaSession(currentTitle, currentArtist, isPlaying, currentPos, currentDurationMs)
        val notification = buildMediaNotification(currentTitle, currentArtist, isPlaying)
        startForeground(NOTIFICATION_ID, notification)

        return START_NOT_STICKY
    }

    private fun fetchAndApplyCoverArt(coverUrl: String) {
        serviceScope.launch {
            val bitmap = loadCoverBitmap(coverUrl) ?: return@launch
            withContext(Dispatchers.Main) {
                if (currentCoverUrl == coverUrl) {
                    currentCoverBitmap = bitmap
                    val pos = exoPlayer?.currentPosition ?: 0L
                    val dur = exoPlayer?.duration?.takeIf { it > 0 } ?: currentDurationMs
                    updateMediaSession(currentTitle, currentArtist, isPlayingState, pos, dur)
                    updateNotification(isPlayingState)
                }
            }
        }
    }

    private suspend fun loadCoverBitmap(coverUrl: String): Bitmap? {
        // 1. Coba muat menggunakan Coil dengan allowHardware(false) agar aman dikirim ke Notification/Binder
        try {
            val loader = ImageLoader(applicationContext)
            val request = ImageRequest.Builder(applicationContext)
                .data(coverUrl)
                .allowHardware(false)
                .size(512, 512)
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                val bmp = (result.drawable as? BitmapDrawable)?.bitmap
                if (bmp != null) {
                    return bmp.copy(Bitmap.Config.ARGB_8888, false)
                }
            }
        } catch (_: Exception) {
        }

        // 2. Fallback manual HTTP stream decode jika Coil gagal
        return try {
            val conn = (URL(coverUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            if (conn.responseCode == 200) {
                conn.inputStream.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun updateMediaSession(
        title: String,
        artist: String,
        isPlaying: Boolean,
        positionMs: Long = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
        durationMs: Long = 0L
    ) {
        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
        if (durationMs > 0) {
            metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationMs)
        }
        currentCoverUrl?.takeIf { it.isNotBlank() }?.let { uri ->
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, uri)
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ART_URI, uri)
        }
        currentCoverBitmap?.let { bmp ->
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bmp)
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bmp)
        }
        mediaSession.setMetadata(metadataBuilder.build())

        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SEEK_TO
            )
            .setState(state, positionMs, 1.0f)
            .build()
        mediaSession.setPlaybackState(playbackState)
    }

    private fun updateNotification(isPlaying: Boolean) {
        val notification = buildMediaNotification(currentTitle, currentArtist, isPlaying)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, notification)
    }

    private fun buildMediaNotification(
        title: String,
        artist: String,
        isPlaying: Boolean
    ): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPending = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = Intent(this, MusicPlaybackService::class.java).apply {
            action = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
        }
        val playPausePending = PendingIntent.getService(
            this, 1, playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        val skipNextIntent = Intent(this, MusicPlaybackService::class.java).apply {
            action = ACTION_SKIP_NEXT
        }
        val skipNextPending = PendingIntent.getService(
            this, 2, skipNextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val skipPrevIntent = Intent(this, MusicPlaybackService::class.java).apply {
            action = ACTION_SKIP_PREVIOUS
        }
        val skipPrevPending = PendingIntent.getService(
            this, 3, skipPrevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText("StrideBeat Music")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentPending)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_media_previous, "Previous", skipPrevPending)
            .addAction(playPauseIcon, playPauseTitle, playPausePending)
            .addAction(android.R.drawable.ic_media_next, "Next", skipNextPending)
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )

        currentCoverBitmap?.let { bmp ->
            builder.setLargeIcon(bmp)
        }

        return builder.build()
    }

    override fun onDestroy() {
        if (activeServiceInstance == this) {
            activeServiceInstance = null
        }
        serviceScope.cancel()
        stopAudio()
        exoPlayer?.release()
        exoPlayer = null
        mediaSession.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "StrideBeat Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Kontrol audio musik saat sesi olahraga"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "stridebeat_music_channel"
        const val NOTIFICATION_ID = 2002

        const val EXTRA_TRACK_TITLE = "extra_track_title"
        const val EXTRA_TRACK_ARTIST = "extra_track_artist"
        const val EXTRA_IS_PLAYING = "extra_is_playing"
        const val EXTRA_SOURCE = "extra_source"
        const val EXTRA_AUDIO_URL = "extra_audio_url"
        const val EXTRA_COVER_URL = "extra_cover_url"
        const val EXTRA_TRACK_DURATION = "extra_track_duration"

        const val ACTION_PLAY = "com.stridebeat.app.action.PLAY"
        const val ACTION_PAUSE = "com.stridebeat.app.action.PAUSE"
        const val ACTION_SKIP_NEXT = "com.stridebeat.app.action.SKIP_NEXT"
        const val ACTION_SKIP_PREVIOUS = "com.stridebeat.app.action.SKIP_PREVIOUS"
        const val ACTION_SEEK_TO = "com.stridebeat.app.action.SEEK_TO"
        const val EXTRA_SEEK_POSITION = "extra_seek_position"

        var onSkipNextListener: (() -> Unit)? = null
        var onSkipPreviousListener: (() -> Unit)? = null
        var onPositionUpdatedListener: ((Long) -> Unit)? = null
        var onDurationUpdatedListener: ((Long) -> Unit)? = null

        private var activeServiceInstance: MusicPlaybackService? = null

        fun getCurrentPosition(): Long {
            return activeServiceInstance?.exoPlayer?.currentPosition ?: 0L
        }

        fun seekTo(context: Context, positionMs: Long) {
            activeServiceInstance?.seekToPosition(positionMs) ?: run {
                val intent = Intent(context, MusicPlaybackService::class.java).apply {
                    action = ACTION_SEEK_TO
                    putExtra(EXTRA_SEEK_POSITION, positionMs)
                }
                context.startService(intent)
            }
        }

        fun update(
            context: Context,
            track: Track,
            isPlaying: Boolean,
            source: MusicSource
        ) {
            val intent = Intent(context, MusicPlaybackService::class.java).apply {
                putExtra(EXTRA_TRACK_TITLE, track.title)
                putExtra(EXTRA_TRACK_ARTIST, track.artist)
                putExtra(EXTRA_IS_PLAYING, isPlaying)
                putExtra(EXTRA_SOURCE, source.name)
                putExtra(EXTRA_AUDIO_URL, track.audioUrl)
                putExtra(EXTRA_COVER_URL, track.coverUrl)
                putExtra(EXTRA_TRACK_DURATION, track.durationMs)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
