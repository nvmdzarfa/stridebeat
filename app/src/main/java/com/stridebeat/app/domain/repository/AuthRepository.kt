package com.stridebeat.app.domain.repository

import com.stridebeat.app.domain.model.SpotifyAuthState
import com.stridebeat.app.domain.model.StravaAuthState
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface repositori otentikasi akun terhubung (Strava & Spotify).
 */
interface AuthRepository {
    val spotifyAuthState: StateFlow<SpotifyAuthState>
    val stravaAuthState: StateFlow<StravaAuthState>

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
}
