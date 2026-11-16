package com.stridebeat.app.data.repository

import com.stridebeat.app.data.local.SecureTokenStorage
import com.stridebeat.app.data.remote.spotify.SpotifyAuthManager
import com.stridebeat.app.domain.model.SpotifyAuthState
import com.stridebeat.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val tokenStorage: SecureTokenStorage,
    private val spotifyAuthManager: SpotifyAuthManager
) : AuthRepository {

    private val _spotifyAuthState = MutableStateFlow<SpotifyAuthState>(SpotifyAuthState.NotConnected)
    override val spotifyAuthState: StateFlow<SpotifyAuthState> = _spotifyAuthState.asStateFlow()

    init {
        restoreStoredAuthStates()
    }

    private fun restoreStoredAuthStates() {
        if (!spotifyAuthManager.isConfigured()) {
            _spotifyAuthState.value = SpotifyAuthState.MissingConfig
        } else {
            val token = tokenStorage.getSpotifyAccessToken()
            val user = tokenStorage.getSpotifyUserName()
            val isPremium = tokenStorage.isSpotifyPremium()
            val expiresAt = tokenStorage.getSpotifyExpiresAt()

            if (token != null && user != null) {
                if (System.currentTimeMillis() > expiresAt && expiresAt > 0) {
                    _spotifyAuthState.value = SpotifyAuthState.TokenExpired
                } else {
                    _spotifyAuthState.value = SpotifyAuthState.Connected(user, isPremium)
                }
            } else {
                _spotifyAuthState.value = SpotifyAuthState.NotConnected
            }
        }
    }

    override fun getSpotifyAuthUrl(): String? {
        if (!spotifyAuthManager.isConfigured()) {
            _spotifyAuthState.value = SpotifyAuthState.MissingConfig
            return null
        }
        _spotifyAuthState.value = SpotifyAuthState.Connecting
        return spotifyAuthManager.createAuthorizationUrl()
    }

    override suspend fun handleSpotifyCallback(code: String): Result<Unit> {
        _spotifyAuthState.value = SpotifyAuthState.Connecting
        val result = spotifyAuthManager.exchangeCodeForTokens(code)
        return if (result.isSuccess) {
            val (userName, isPremium) = result.getOrThrow()
            _spotifyAuthState.value = SpotifyAuthState.Connected(userName, isPremium)
            Result.success(Unit)
        } else {
            val err = result.exceptionOrNull()?.localizedMessage ?: "Gagal login Spotify"
            _spotifyAuthState.value = SpotifyAuthState.Error(err)
            Result.failure(Exception(err))
        }
    }

    override suspend fun disconnectSpotify() {
        tokenStorage.clearSpotifyTokens()
        _spotifyAuthState.value = if (spotifyAuthManager.isConfigured()) SpotifyAuthState.NotConnected else SpotifyAuthState.MissingConfig
    }
}
