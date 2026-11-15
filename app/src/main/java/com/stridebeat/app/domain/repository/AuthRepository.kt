package com.stridebeat.app.domain.repository

import com.stridebeat.app.domain.model.SpotifyAuthState
import com.stridebeat.app.domain.model.StravaAuthState
import com.stridebeat.app.domain.model.YouTubeMusicAuthState
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface repositori otentikasi akun Strava, Spotify, dan YouTube Music.
 */
interface AuthRepository {
    val stravaAuthState: StateFlow<StravaAuthState>
    val spotifyAuthState: StateFlow<SpotifyAuthState>
    val youtubeMusicAuthState: StateFlow<YouTubeMusicAuthState>

    // Strava
    fun getStravaAuthUrl(): String?
    suspend fun handleStravaCallback(code: String): Result<Unit>
    suspend fun disconnectStrava()

    // Spotify
    fun getSpotifyAuthUrl(): String?
    suspend fun handleSpotifyCallback(code: String): Result<Unit>
    suspend fun disconnectSpotify()

    // YouTube Music (Feature-Flagged)
    suspend fun connectYouTubeMusic(userName: String): Result<Unit>
    suspend fun disconnectYouTubeMusic()
}
