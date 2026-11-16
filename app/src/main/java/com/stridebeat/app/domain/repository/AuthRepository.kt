package com.stridebeat.app.domain.repository

import com.stridebeat.app.domain.model.SpotifyAuthState
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface repositori otentikasi akun musik (Spotify).
 */
interface AuthRepository {
    val spotifyAuthState: StateFlow<SpotifyAuthState>

    // Spotify
    fun getSpotifyAuthUrl(): String?
    suspend fun handleSpotifyCallback(code: String): Result<Unit>
    suspend fun disconnectSpotify()
}
