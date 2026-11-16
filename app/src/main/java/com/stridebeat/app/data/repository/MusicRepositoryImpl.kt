package com.stridebeat.app.data.repository

import android.content.Context
import com.stridebeat.app.data.remote.youtubemusic.YouTubeMusicConfig
import com.stridebeat.app.domain.model.MusicSource
import com.stridebeat.app.domain.model.PlayedTrack
import com.stridebeat.app.domain.model.Track
import com.stridebeat.app.domain.model.WorkoutPlaylist
import com.stridebeat.app.domain.repository.MusicRepository
import com.stridebeat.app.service.MusicPlaybackService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : MusicRepository {

    private val _activeSource = MutableStateFlow(MusicSource.YOUTUBE_MUSIC)
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

    // Antrean lagu dinamis dari hasil pencarian user (tanpa template dummy)
    private var currentQueue: List<Track> = emptyList()
    private var lastSearchResults: List<Track> = emptyList()

    init {
        MusicPlaybackService.onSkipNextListener = {
            CoroutineScope(Dispatchers.Main).launch {
                skipToNext()
            }
        }
        MusicPlaybackService.onSkipPreviousListener = {
            CoroutineScope(Dispatchers.Main).launch {
                skipToPrevious()
            }
        }
    }

    override suspend fun play() {
        _isPlaying.value = true
        _currentTrack.value?.let { track ->
            recordPlayedTrack(track)
            MusicPlaybackService.update(context, track, true, _activeSource.value)
        }
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
        val nextTrack = currentQueue[nextIndex]

        _currentTrack.value = nextTrack
        _playbackPositionMs.value = 0L

        if (_isPlaying.value) {
            recordPlayedTrack(nextTrack)
            MusicPlaybackService.update(context, nextTrack, true, _activeSource.value)
        }
    }

    override suspend fun skipToPrevious() {
        if (currentQueue.isEmpty()) return

        val currentIndex = currentQueue.indexOfFirst { it.id == _currentTrack.value?.id }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else currentQueue.size - 1
        val prevTrack = currentQueue[prevIndex]

        _currentTrack.value = prevTrack
        _playbackPositionMs.value = 0L

        if (_isPlaying.value) {
            recordPlayedTrack(prevTrack)
            MusicPlaybackService.update(context, prevTrack, true, _activeSource.value)
        }
    }

    override suspend fun seekTo(positionMs: Long) {
        _playbackPositionMs.value = positionMs
    }

    override suspend fun switchSource(newSource: MusicSource): Boolean {
        if (newSource == MusicSource.YOUTUBE_MUSIC && !YouTubeMusicConfig.isYouTubeMusicEnabled) {
            return false
        }

        _activeSource.value = newSource
        return true
    }

    override suspend fun selectPlaylist(playlist: WorkoutPlaylist) {
        _activePlaylist.value = playlist
        currentQueue = playlist.tracks
        if (playlist.tracks.isNotEmpty()) {
            _currentTrack.value = playlist.tracks.first()
            _playbackPositionMs.value = 0L
        }
    }

    override suspend fun playTrack(track: Track) {
        // Jika track yang dipilih berasal dari hasil pencarian terakhir, gunakan hasil pencarian sebagai queue
        if (lastSearchResults.any { it.id == track.id }) {
            currentQueue = lastSearchResults
        } else if (!currentQueue.any { it.id == track.id }) {
            currentQueue = listOf(track)
        }

        _currentTrack.value = track
        _playbackPositionMs.value = 0L
        play()
    }

    private fun recordPlayedTrack(track: Track) {
        val played = PlayedTrack(
            trackId = track.id,
            title = track.title,
            artist = track.artist,
            source = _activeSource.value,
            playedAt = System.currentTimeMillis()
        )

        val currentList = _playedTracksInSession.value
        if (currentList.isEmpty() || currentList.last().trackId != track.id) {
            _playedTracksInSession.value = currentList + played
        }
    }

    override suspend fun searchOnlineTracks(query: String): List<Track> {
        val results = com.stridebeat.app.data.remote.music.OnlineMusicSearchEngine.searchTracks(query)
        lastSearchResults = results
        return results
    }

    override suspend fun clearSessionTracks() {
        _playedTracksInSession.value = emptyList()
    }
}
