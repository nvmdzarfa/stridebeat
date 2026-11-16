package com.stridebeat.app.domain.repository

import com.stridebeat.app.domain.model.SpotifyAuthState
import com.stridebeat.app.domain.model.StravaAuthState
import com.stridebeat.app.domain.model.YouTubeMusicAuthState
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface repositori otentikasi akun terhubung (Strava, Spotify, & YouTube Music).
 */
interface AuthRepository {
    val spotifyAuthState: StateFlow<SpotifyAuthState>
    val stravaAuthState: StateFlow<StravaAuthState>
    val youTubeMusicAuthState: StateFlow<YouTubeMusicAuthState>

    // Spotify
    fun getSpotifyAuthUrl(): String?
    suspend fun handleSpotifyCallback(code: String): Result<Unit>
    suspend fun disconnectSpotify()

    // Strava (M5)
    fun getStravaAuthUrl(): String?
    suspend fun handleStravaCallback(code: String): Result<Unit>
    fun handleStravaAuthCancelledOrDenied(error: String?)
    suspend fun disconnectStrava()
    fun markStravaTokenExpired()
    fun refreshStravaState()

    // YouTube Music (M6)
    fun getYouTubeMusicAuthUrl(): String?
    suspend fun handleYouTubeMusicCallback(code: String): Result<Unit>
    suspend fun disconnectYouTubeMusic()
    fun refreshYouTubeMusicFeatureFlag(enabled: Boolean)
}
