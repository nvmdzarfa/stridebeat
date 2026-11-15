package com.stridebeat.app.presentation.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stridebeat.app.domain.model.MusicSource
import com.stridebeat.app.domain.model.PlayedTrack
import com.stridebeat.app.domain.model.Track
import com.stridebeat.app.domain.model.WorkoutPlaylist
import com.stridebeat.app.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MusicViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    val activeSource: StateFlow<MusicSource> = musicRepository.activeSource
    val currentTrack: StateFlow<Track?> = musicRepository.currentTrack
    val isPlaying: StateFlow<Boolean> = musicRepository.isPlaying
    val playbackPositionMs: StateFlow<Long> = musicRepository.playbackPositionMs
    val availablePlaylists: StateFlow<List<WorkoutPlaylist>> = musicRepository.availablePlaylists
    val activePlaylist: StateFlow<WorkoutPlaylist?> = musicRepository.activePlaylist
    val playedTracksInSession: StateFlow<List<PlayedTrack>> = musicRepository.playedTracksInSession

    fun play() {
        viewModelScope.launch {
            musicRepository.play()
        }
    }

    fun pause() {
        viewModelScope.launch {
            musicRepository.pause()
        }
    }

    fun togglePlayPause() {
        viewModelScope.launch {
            musicRepository.togglePlayPause()
        }
    }

    fun skipNext() {
        viewModelScope.launch {
            musicRepository.skipToNext()
        }
    }

    fun skipPrevious() {
        viewModelScope.launch {
            musicRepository.skipToPrevious()
        }
    }

    fun switchSource(newSource: MusicSource) {
        viewModelScope.launch {
            musicRepository.switchSource(newSource)
        }
    }

    fun selectPlaylist(playlist: WorkoutPlaylist) {
        viewModelScope.launch {
            musicRepository.selectPlaylist(playlist)
        }
    }

    private val _searchResults = MutableStateFlow<List<Track>>(emptyList())
    val searchResults: StateFlow<List<Track>> = _searchResults

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    fun searchOnlineTracks(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            val results = musicRepository.searchOnlineTracks(query)
            _searchResults.value = results
            _isSearching.value = false
        }
    }

    fun playTrack(track: Track) {
        viewModelScope.launch {
            musicRepository.playTrack(track)
        }
    }
}
