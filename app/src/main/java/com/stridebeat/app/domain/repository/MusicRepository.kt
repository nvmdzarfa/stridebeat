package com.stridebeat.app.domain.repository

import com.stridebeat.app.domain.model.MusicSource
import com.stridebeat.app.domain.model.PlayedTrack
import com.stridebeat.app.domain.model.Track
import com.stridebeat.app.domain.model.WorkoutPlaylist
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface repositori kontrol musik independen platform (Spotify & YouTube Music).
 * Sesuai PRD §1.1 & §7.5:
 * Berperan sebagai hub / switcher musik antar platform tanpa menghentikan tracking olahraga.
 */
interface MusicRepository {
    val activeSource: StateFlow<MusicSource>
    val currentTrack: StateFlow<Track?>
    val isPlaying: StateFlow<Boolean>
    val playbackPositionMs: StateFlow<Long>
    val availablePlaylists: StateFlow<List<WorkoutPlaylist>>
    val activePlaylist: StateFlow<WorkoutPlaylist?>
    val playedTracksInSession: StateFlow<List<PlayedTrack>>

    suspend fun play()
    suspend fun pause()
    suspend fun togglePlayPause()
    suspend fun skipToNext()
    suspend fun skipToPrevious()
    suspend fun seekTo(positionMs: Long)

    suspend fun switchSource(newSource: MusicSource): Boolean
    suspend fun selectPlaylist(playlist: WorkoutPlaylist)
    suspend fun playTrack(track: Track)
    suspend fun clearSessionTracks()
}
