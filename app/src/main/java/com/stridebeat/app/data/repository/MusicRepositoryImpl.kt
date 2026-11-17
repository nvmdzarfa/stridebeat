package com.stridebeat.app.data.repository

import android.content.Context
import com.stridebeat.app.data.local.MusicPreferences
import com.stridebeat.app.data.local.PlaylistStorage
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
    private val playlistStorage: PlaylistStorage,
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

    private val _availablePlaylists = MutableStateFlow<List<WorkoutPlaylist>>(emptyList())
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
    private var currentQueueIndex: Int = 0
    private var lastSearchResults: List<Track> = emptyList()

    init {
        // Sinkronisasi playlist pengguna dari PlaylistStorage
        scope.launch {
            playlistStorage.userPlaylists.collectLatest { userPlaylists ->
                _availablePlaylists.value = userPlaylists
            }
        }

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

        // Resolusi audioUrl on-demand jika belum tersedia
        if (playableTrack.audioUrl.isNullOrBlank()) {
            var streamResolved = false

            // 1. Coba resolusi YouTube Music jika aktif
            if (_activeSource.value == MusicSource.YOUTUBE_MUSIC) {
                val videoId = playableTrack.platformIds[MusicSource.YOUTUBE_MUSIC] ?: playableTrack.id.removePrefix("ytm_")
                val streamResult = youTubeMusicClient.resolveAudioStreamUrl(videoId)
                if (streamResult is StreamResult.Playable) {
                    playableTrack = playableTrack.copy(audioUrl = streamResult.audioUrl)
                    streamResolved = true
                    clearPlaybackNotice()
                }
            }

            // 2. Fallback transparan / resolusi langsung ke SoundCloud
            if (!streamResolved) {
                val cleanArtist = playableTrack.artist.replace("YouTube", "", ignoreCase = true).trim()
                val query = "${playableTrack.title} $cleanArtist".trim()
                val scCandidates = OnlineMusicSearchEngine.searchTracks(query)
                val bestMatch = TrackMatchingEngine.findBestMatch(playableTrack, scCandidates) ?: scCandidates.firstOrNull()

                if (bestMatch != null && !bestMatch.audioUrl.isNullOrBlank()) {
                    playableTrack = playableTrack.copy(
                        audioUrl = bestMatch.audioUrl,
                        platformIds = playableTrack.platformIds + (MusicSource.SOUNDCLOUD to (bestMatch.platformIds[MusicSource.SOUNDCLOUD] ?: bestMatch.id))
                    )
                    streamResolved = true
                    clearPlaybackNotice()
                }
            }

            if (!streamResolved) {
                val failNotice = if (_activeSource.value == MusicSource.YOUTUBE_MUSIC) {
                    "Lagu ini tidak tersedia di YouTube Music"
                } else {
                    "\"${playableTrack.title}\" tidak ditemukan di SoundCloud"
                }
                val videoId = playableTrack.platformIds[MusicSource.YOUTUBE_MUSIC] ?: playableTrack.id.removePrefix("ytm_")
                _playbackNotice.value = failNotice
                _unavailableExternalUrl.value = "https://music.youtube.com/watch?v=$videoId"
                _isPlaying.value = false
                MusicPlaybackService.pause(context)
                return
            }

            _currentTrack.value = playableTrack
            coordinator.setCurrentTrackOnly(playableTrack)
            if (currentQueueIndex in currentQueue.indices) {
                val updatedQueue = currentQueue.toMutableList()
                updatedQueue[currentQueueIndex] = playableTrack
                currentQueue = updatedQueue
            }
        }

        _isPlaying.value = true
        clearPlaybackNotice()
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
        if (currentQueue.isEmpty()) {
            val pl = _activePlaylist.value
            if (pl != null && pl.tracks.isNotEmpty()) {
                currentQueue = pl.tracks
                currentQueueIndex = 0
            } else {
                return
            }
        }

        val foundIndex = currentQueue.indexOfFirst {
            it.id == _currentTrack.value?.id ||
            (it.title.equals(_currentTrack.value?.title, ignoreCase = true) && it.artist.equals(_currentTrack.value?.artist, ignoreCase = true))
        }
        val baseIndex = if (foundIndex != -1) foundIndex else currentQueueIndex
        currentQueueIndex = (baseIndex + 1) % currentQueue.size
        val candidate = currentQueue[currentQueueIndex]

        _currentTrack.value = candidate
        coordinator.setCurrentTrackOnly(candidate)
        _playbackPositionMs.value = 0L

        play()
    }

    override suspend fun skipToPrevious() {
        if (currentQueue.isEmpty()) {
            val pl = _activePlaylist.value
            if (pl != null && pl.tracks.isNotEmpty()) {
                currentQueue = pl.tracks
                currentQueueIndex = 0
            } else {
                return
            }
        }

        val foundIndex = currentQueue.indexOfFirst {
            it.id == _currentTrack.value?.id ||
            (it.title.equals(_currentTrack.value?.title, ignoreCase = true) && it.artist.equals(_currentTrack.value?.artist, ignoreCase = true))
        }
        val baseIndex = if (foundIndex != -1) foundIndex else currentQueueIndex
        currentQueueIndex = if (baseIndex > 0) baseIndex - 1 else currentQueue.size - 1
        val candidate = currentQueue[currentQueueIndex]

        _currentTrack.value = candidate
        coordinator.setCurrentTrackOnly(candidate)
        _playbackPositionMs.value = 0L

        play()
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
            val cleanArtist = activeTrack.artist.replace("YouTube", "", ignoreCase = true).trim()
            val query = "${activeTrack.title} $cleanArtist".trim()
            val searchResults = OnlineMusicSearchEngine.searchTracks(query)
            val matched = TrackMatchingEngine.findBestMatch(activeTrack, searchResults) ?: searchResults.firstOrNull()
            if (matched != null && !matched.audioUrl.isNullOrBlank()) {
                val updatedTrack = activeTrack.copy(
                    audioUrl = matched.audioUrl,
                    platformIds = activeTrack.platformIds + (MusicSource.SOUNDCLOUD to (matched.platformIds[MusicSource.SOUNDCLOUD] ?: matched.id))
                )
                _currentTrack.value = updatedTrack
                coordinator.setCurrentTrackOnly(updatedTrack)
                if (currentQueueIndex in currentQueue.indices) {
                    val updatedQueue = currentQueue.toMutableList()
                    updatedQueue[currentQueueIndex] = updatedTrack
                    currentQueue = updatedQueue
                }
                clearPlaybackNotice()
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
                        if (currentQueueIndex in currentQueue.indices) {
                            val updatedQueue = currentQueue.toMutableList()
                            updatedQueue[currentQueueIndex] = updated
                            currentQueue = updatedQueue
                        }
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
        currentQueueIndex = 0
        coordinator.setQueue(playlist.tracks)
        if (playlist.tracks.isNotEmpty()) {
            val first = playlist.tracks.first()
            _currentTrack.value = first
            coordinator.setCurrentTrackOnly(first)
            _playbackPositionMs.value = 0L
            play()
        }
    }

    override suspend fun playTrack(track: Track) {
        if (lastSearchResults.any { it.id == track.id }) {
            currentQueue = lastSearchResults
            currentQueueIndex = currentQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        } else {
            val idx = currentQueue.indexOfFirst {
                it.id == track.id || (it.title.equals(track.title, ignoreCase = true) && it.artist.equals(track.artist, ignoreCase = true))
            }
            if (idx != -1) {
                currentQueueIndex = idx
            } else {
                currentQueue = listOf(track)
                currentQueueIndex = 0
            }
        }
        coordinator.setQueue(currentQueue, startingIndex = currentQueueIndex)

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

    override suspend fun importYouTubePlaylist(playlistIdOrUrl: String): Result<WorkoutPlaylist> {
        val trimmed = playlistIdOrUrl.trim()
        val fetchResult = if (trimmed.contains("soundcloud.com", ignoreCase = true)) {
            OnlineMusicSearchEngine.fetchSoundCloudPlaylist(trimmed)
        } else {
            youTubeMusicClient.fetchPlaylist(trimmed)
        }

        if (fetchResult.isSuccess) {
            val playlist = fetchResult.getOrThrow()
            playlistStorage.savePlaylist(playlist)
            selectPlaylist(playlist)
        }
        return fetchResult
    }

    override suspend fun deletePlaylist(playlistId: String) {
        playlistStorage.deletePlaylist(playlistId)
        if (_activePlaylist.value?.id == playlistId) {
            _activePlaylist.value = null
        }
    }
}
