package com.stridebeat.app.data.repository

import android.content.Context
import com.stridebeat.app.data.local.MusicPreferences
import com.stridebeat.app.data.remote.music.NeutralWorkoutPlaylists
import com.stridebeat.app.data.remote.music.OnlineMusicSearchEngine
import com.stridebeat.app.data.remote.youtubemusic.StreamResult
import com.stridebeat.app.data.remote.youtubemusic.YouTubeMusicClient
import com.stridebeat.app.data.remote.youtubemusic.YouTubeMusicConfig
import com.stridebeat.app.domain.model.MusicSource
import com.stridebeat.app.domain.model.PlayedTrack
import com.stridebeat.app.domain.model.Track
import com.stridebeat.app.domain.model.WorkoutPlaylist
import com.stridebeat.app.domain.repository.MusicRepository
import com.stridebeat.app.service.MusicPlaybackService
import com.stridebeat.app.util.MusicQueueCoordinator
import com.stridebeat.app.util.TrackMatchingEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicPreferences: MusicPreferences,
    private val youTubeMusicClient: YouTubeMusicClient
) : MusicRepository {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val coordinator = MusicQueueCoordinator()

    private val _activeSource = MutableStateFlow(MusicSource.SOUNDCLOUD)
    override val activeSource: StateFlow<MusicSource> = _activeSource.asStateFlow()

    private val _currentTrack = MutableStateFlow<Track?>(null)
    override val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackPositionMs = MutableStateFlow(0L)
    override val playbackPositionMs: StateFlow<Long> = _playbackPositionMs.asStateFlow()

    private val _availablePlaylists = MutableStateFlow<List<WorkoutPlaylist>>(NeutralWorkoutPlaylists.playlists)
    override val availablePlaylists: StateFlow<List<WorkoutPlaylist>> = _availablePlaylists.asStateFlow()

    private val _activePlaylist = MutableStateFlow<WorkoutPlaylist?>(null)
    override val activePlaylist: StateFlow<WorkoutPlaylist?> = _activePlaylist.asStateFlow()

    private val _playedTracksInSession = MutableStateFlow<List<PlayedTrack>>(emptyList())
    override val playedTracksInSession: StateFlow<List<PlayedTrack>> = _playedTracksInSession.asStateFlow()

    private val _youtubeMusicEnabled = MutableStateFlow(true)
    override val youtubeMusicEnabled: StateFlow<Boolean> = _youtubeMusicEnabled.asStateFlow()

    private val _playbackNotice = MutableStateFlow<String?>(null)
    override val playbackNotice: StateFlow<String?> = _playbackNotice.asStateFlow()

    private val _unavailableExternalUrl = MutableStateFlow<String?>(null)
    override val unavailableExternalUrl: StateFlow<String?> = _unavailableExternalUrl.asStateFlow()

    private var currentQueue: List<Track> = emptyList()
    private var lastSearchResults: List<Track> = emptyList()

    init {
        // Sinkronisasi feature flag YouTube Music dari DataStore
        scope.launch {
            musicPreferences.youtubeMusicEnabled.collectLatest { enabled ->
                _youtubeMusicEnabled.value = enabled
                YouTubeMusicConfig.isYouTubeMusicEnabled = enabled
                coordinator.youtubeMusicEnabled = enabled
                if (!enabled && _activeSource.value == MusicSource.YOUTUBE_MUSIC) {
                    switchSource(MusicSource.SOUNDCLOUD)
                }
            }
        }

        // Sinkronisasi sumber favorit yang tersimpan
        scope.launch {
            musicPreferences.preferredMusicSource.collectLatest { preferred ->
                if (_activeSource.value != preferred && (preferred != MusicSource.YOUTUBE_MUSIC || _youtubeMusicEnabled.value)) {
                    _activeSource.value = preferred
                    coordinator.switchSource(preferred)
                }
            }
        }

        MusicPlaybackService.onSkipNextListener = {
            scope.launch { skipToNext() }
        }
        MusicPlaybackService.onSkipPreviousListener = {
            scope.launch { skipToPrevious() }
        }
        MusicPlaybackService.onPositionUpdatedListener = { pos ->
            _playbackPositionMs.value = pos
        }
        MusicPlaybackService.onDurationUpdatedListener = { dur ->
            val track = _currentTrack.value
            if (track != null && dur > 0 && track.durationMs != dur) {
                _currentTrack.value = track.copy(durationMs = dur)
            }
        }

        // Ticker periodik setiap 500ms saat memutar untuk kelancaran scrubber UI
        scope.launch {
            while (true) {
                delay(500)
                if (_isPlaying.value) {
                    val realPos = MusicPlaybackService.getCurrentPosition()
                    if (realPos > 0) {
                        _playbackPositionMs.value = realPos
                    }
                }
            }
        }
    }

    override suspend fun play() {
        val track = _currentTrack.value ?: return
        var playableTrack = track

        // Jika memutar di YouTube Music dan audioUrl belum ada, resolve on-demand
        if (_activeSource.value == MusicSource.YOUTUBE_MUSIC && playableTrack.audioUrl.isNullOrBlank()) {
            val videoId = playableTrack.platformIds[MusicSource.YOUTUBE_MUSIC] ?: playableTrack.id.removePrefix("ytm_")
            val streamResult = youTubeMusicClient.resolveAudioStreamUrl(videoId)
            when (streamResult) {
                is StreamResult.Playable -> {
                    playableTrack = playableTrack.copy(audioUrl = streamResult.audioUrl)
                    _currentTrack.value = playableTrack
                    coordinator.setCurrentTrackOnly(playableTrack)
                    clearPlaybackNotice()
                }
                is StreamResult.Unavailable -> {
                    _playbackNotice.value = streamResult.reason
                    _unavailableExternalUrl.value = streamResult.externalUri
                    _isPlaying.value = false
                    return
                }
            }
        }

        _isPlaying.value = true
        recordPlayedTrack(playableTrack)
        MusicPlaybackService.update(context, playableTrack, true, _activeSource.value)
    }

    override suspend fun pause() {
        _isPlaying.value = false
        _currentTrack.value?.let { track ->
            MusicPlaybackService.update(context, track, false, _activeSource.value)
        }
    }

    override suspend fun togglePlayPause() {
        if (_isPlaying.value) pause() else play()
    }

    override suspend fun skipToNext() {
        if (currentQueue.isEmpty()) return

        val currentIndex = currentQueue.indexOfFirst { it.id == _currentTrack.value?.id }
        val nextIndex = if (currentIndex != -1) (currentIndex + 1) % currentQueue.size else 0
        var candidate = currentQueue[nextIndex]

        // Jika di YouTube Music, pastikan track playable (lewati jika terhalang cipher / 403)
        if (_activeSource.value == MusicSource.YOUTUBE_MUSIC && candidate.audioUrl.isNullOrBlank()) {
            var found = false
            for (step in 0 until currentQueue.size) {
                val idx = (nextIndex + step) % currentQueue.size
                val t = currentQueue[idx]
                val vid = t.platformIds[MusicSource.YOUTUBE_MUSIC] ?: t.id.removePrefix("ytm_")
                val stream = youTubeMusicClient.resolveAudioStreamUrl(vid)
                if (stream is StreamResult.Playable) {
                    candidate = t.copy(audioUrl = stream.audioUrl)
                    found = true
                    clearPlaybackNotice()
                    break
                }
            }
            if (!found) {
                _playbackNotice.value = "Lagu di antrean tidak tersedia di YouTube Music"
                _unavailableExternalUrl.value = "https://music.youtube.com/"
                return
            }
        }

        _currentTrack.value = candidate
        coordinator.setCurrentTrackOnly(candidate)
        _playbackPositionMs.value = 0L

        if (_isPlaying.value) {
            play()
        }
    }

    override suspend fun skipToPrevious() {
        if (currentQueue.isEmpty()) return

        val currentIndex = currentQueue.indexOfFirst { it.id == _currentTrack.value?.id }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else currentQueue.size - 1
        var candidate = currentQueue[prevIndex]

        if (_activeSource.value == MusicSource.YOUTUBE_MUSIC && candidate.audioUrl.isNullOrBlank()) {
            val vid = candidate.platformIds[MusicSource.YOUTUBE_MUSIC] ?: candidate.id.removePrefix("ytm_")
            val stream = youTubeMusicClient.resolveAudioStreamUrl(vid)
            if (stream is StreamResult.Playable) {
                candidate = candidate.copy(audioUrl = stream.audioUrl)
            }
        }

        _currentTrack.value = candidate
        coordinator.setCurrentTrackOnly(candidate)
        _playbackPositionMs.value = 0L

        if (_isPlaying.value) {
            play()
        }
    }

    override suspend fun seekTo(positionMs: Long) {
        _playbackPositionMs.value = positionMs
        MusicPlaybackService.seekTo(context, positionMs)
    }

    override suspend fun switchSource(newSource: MusicSource): Boolean {
        if (!coordinator.switchSource(newSource)) {
            _playbackNotice.value = coordinator.playbackNotice
            return false
        }

        _activeSource.value = newSource
        clearPlaybackNotice()
        scope.launch { musicPreferences.setPreferredMusicSource(newSource) }

        val active = _currentTrack.value
        if (active != null) {
            matchAndContinuePlayback(active, newSource)
        }
        return true
    }

    private suspend fun matchAndContinuePlayback(activeTrack: Track, targetSource: MusicSource) {
        if (targetSource == MusicSource.SOUNDCLOUD) {
            if (activeTrack.id.startsWith("sc_") && !activeTrack.audioUrl.isNullOrBlank()) {
                if (_isPlaying.value) play()
                return
            }

            // Cari lagu yang setara di SoundCloud
            val query = "${activeTrack.title} ${activeTrack.artist}"
            val searchResults = OnlineMusicSearchEngine.searchTracks(query)
            val matched = TrackMatchingEngine.findBestMatch(activeTrack, searchResults)
            if (matched != null && !matched.audioUrl.isNullOrBlank()) {
                _currentTrack.value = matched
                coordinator.setCurrentTrackOnly(matched)
                if (_isPlaying.value) play()
            } else {
                _playbackNotice.value = "\"${activeTrack.title}\" tidak ditemukan di SoundCloud"
            }
        } else if (targetSource == MusicSource.YOUTUBE_MUSIC) {
            val existingYtmId = activeTrack.platformIds[MusicSource.YOUTUBE_MUSIC]
            val videoId = if (!existingYtmId.isNullOrBlank()) {
                existingYtmId
            } else {
                val query = "${activeTrack.title} ${activeTrack.artist}"
                val searchRes = youTubeMusicClient.searchTracks(query).getOrDefault(emptyList())
                val matched = TrackMatchingEngine.findBestMatch(activeTrack, searchRes)
                matched?.platformIds?.get(MusicSource.YOUTUBE_MUSIC)
            }

            if (!videoId.isNullOrBlank()) {
                when (val stream = youTubeMusicClient.resolveAudioStreamUrl(videoId)) {
                    is StreamResult.Playable -> {
                        val updated = activeTrack.copy(
                            id = "ytm_$videoId",
                            audioUrl = stream.audioUrl,
                            platformIds = activeTrack.platformIds + (MusicSource.YOUTUBE_MUSIC to videoId)
                        )
                        _currentTrack.value = updated
                        coordinator.setCurrentTrackOnly(updated)
                        clearPlaybackNotice()
                        if (_isPlaying.value) play()
                    }
                    is StreamResult.Unavailable -> {
                        _playbackNotice.value = stream.reason
                        _unavailableExternalUrl.value = stream.externalUri
                    }
                }
            } else {
                _playbackNotice.value = "\"${activeTrack.title}\" tidak ditemukan di YouTube Music"
            }
        }
    }

    override suspend fun selectPlaylist(playlist: WorkoutPlaylist) {
        _activePlaylist.value = playlist
        currentQueue = playlist.tracks
        coordinator.setQueue(playlist.tracks)
        if (playlist.tracks.isNotEmpty()) {
            val first = playlist.tracks.first()
            _currentTrack.value = first
            coordinator.setCurrentTrackOnly(first)
            _playbackPositionMs.value = 0L
        }
    }

    override suspend fun playTrack(track: Track) {
        if (lastSearchResults.any { it.id == track.id }) {
            currentQueue = lastSearchResults
        } else if (!currentQueue.any { it.id == track.id }) {
            currentQueue = listOf(track)
        }
        coordinator.setQueue(currentQueue, startingIndex = currentQueue.indexOfFirst { it.id == track.id })

        _currentTrack.value = track
        coordinator.setCurrentTrackOnly(track)
        _playbackPositionMs.value = 0L
        play()
    }

    private fun recordPlayedTrack(track: Track) {
        coordinator.recordCurrentTrackPlay()
        _playedTracksInSession.value = coordinator.playedTracks
    }

    override suspend fun searchOnlineTracks(query: String): List<Track> {
        val results = if (_activeSource.value == MusicSource.YOUTUBE_MUSIC) {
            youTubeMusicClient.searchTracks(query).getOrDefault(emptyList())
        } else {
            OnlineMusicSearchEngine.searchTracks(query)
        }
        lastSearchResults = results
        return results
    }

    override suspend fun clearSessionTracks() {
        coordinator.clearSessionTracks()
        _playedTracksInSession.value = emptyList()
    }

    override suspend fun clearPlaybackNotice() {
        _playbackNotice.value = null
        _unavailableExternalUrl.value = null
        coordinator.clearNotice()
    }

    override suspend fun setYouTubeMusicEnabled(enabled: Boolean) {
        musicPreferences.setYouTubeMusicEnabled(enabled)
        _youtubeMusicEnabled.value = enabled
        YouTubeMusicConfig.isYouTubeMusicEnabled = enabled
        coordinator.youtubeMusicEnabled = enabled
        if (!enabled && _activeSource.value == MusicSource.YOUTUBE_MUSIC) {
            switchSource(MusicSource.SOUNDCLOUD)
        }
    }

    override suspend fun tryPlayCurrentTrackOnSoundCloud() {
        clearPlaybackNotice()
        switchSource(MusicSource.SOUNDCLOUD)
    }
}
