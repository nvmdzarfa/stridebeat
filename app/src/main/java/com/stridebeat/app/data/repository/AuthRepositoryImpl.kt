package com.stridebeat.app.data.repository

import com.stridebeat.app.data.local.SecureTokenStorage
import com.stridebeat.app.data.remote.spotify.SpotifyAuthManager
import com.stridebeat.app.data.remote.strava.StravaAuthManager
import com.stridebeat.app.data.remote.youtubemusic.YouTubeMusicConfig
import com.stridebeat.app.domain.model.SpotifyAuthState
import com.stridebeat.app.domain.model.StravaAuthState
import com.stridebeat.app.domain.model.YouTubeMusicAuthState
import com.stridebeat.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val tokenStorage: SecureTokenStorage,
    private val stravaAuthManager: StravaAuthManager,
    private val spotifyAuthManager: SpotifyAuthManager
) : AuthRepository {

    private val _stravaAuthState = MutableStateFlow<StravaAuthState>(StravaAuthState.NotConnected)
    override val stravaAuthState: StateFlow<StravaAuthState> = _stravaAuthState.asStateFlow()

    private val _spotifyAuthState = MutableStateFlow<SpotifyAuthState>(SpotifyAuthState.NotConnected)
    override val spotifyAuthState: StateFlow<SpotifyAuthState> = _spotifyAuthState.asStateFlow()

    private val _youtubeMusicAuthState = MutableStateFlow<YouTubeMusicAuthState>(YouTubeMusicAuthState.NotConnected)
    override val youtubeMusicAuthState: StateFlow<YouTubeMusicAuthState> = _youtubeMusicAuthState.asStateFlow()

    init {
        restoreStoredAuthStates()
    }

    private fun restoreStoredAuthStates() {
        // 1. Strava Check
        if (!stravaAuthManager.isConfigured()) {
            _stravaAuthState.value = StravaAuthState.MissingConfig
        } else {
            val token = tokenStorage.getStravaAccessToken()
            val athlete = tokenStorage.getStravaAthleteName()
            val expiresAt = tokenStorage.getStravaExpiresAt()

            if (token != null && athlete != null) {
                if (System.currentTimeMillis() > expiresAt && expiresAt > 0) {
                    _stravaAuthState.value = StravaAuthState.TokenExpired
                } else {
                    _stravaAuthState.value = StravaAuthState.Connected(athlete)
                }
            } else {
                _stravaAuthState.value = StravaAuthState.NotConnected
            }
        }

        // 2. Spotify Check
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

        // 3. YouTube Music Check (Feature Flagged)
        if (!YouTubeMusicConfig.isYouTubeMusicEnabled) {
            _youtubeMusicAuthState.value = YouTubeMusicAuthState.Disabled
        } else {
            val user = tokenStorage.getYouTubeMusicUserName()
            if (user != null) {
                _youtubeMusicAuthState.value = YouTubeMusicAuthState.Connected(user)
            } else {
                _youtubeMusicAuthState.value = YouTubeMusicAuthState.NotConnected
            }
        }
    }

    // --- STRAVA ---
    override fun getStravaAuthUrl(): String? {
        if (!stravaAuthManager.isConfigured()) {
            _stravaAuthState.value = StravaAuthState.MissingConfig
            return null
        }
        _stravaAuthState.value = StravaAuthState.Connecting
        return stravaAuthManager.createAuthorizationUrl()
    }

    override suspend fun handleStravaCallback(code: String): Result<Unit> {
        _stravaAuthState.value = StravaAuthState.Connecting
        val result = stravaAuthManager.exchangeCodeForTokens(code)
        return if (result.isSuccess) {
            val athleteName = result.getOrThrow()
            _stravaAuthState.value = StravaAuthState.Connected(athleteName)
            Result.success(Unit)
        } else {
            val err = result.exceptionOrNull()?.localizedMessage ?: "Gagal login Strava"
            _stravaAuthState.value = StravaAuthState.Error(err)
            Result.failure(Exception(err))
        }
    }

    override suspend fun disconnectStrava() {
        tokenStorage.clearStravaTokens()
        _stravaAuthState.value = if (stravaAuthManager.isConfigured()) StravaAuthState.NotConnected else StravaAuthState.MissingConfig
    }

    // --- SPOTIFY ---
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

    // --- YOUTUBE MUSIC ---
    override suspend fun connectYouTubeMusic(userName: String): Result<Unit> {
        if (!YouTubeMusicConfig.isYouTubeMusicEnabled) {
            _youtubeMusicAuthState.value = YouTubeMusicAuthState.Disabled
            return Result.failure(Exception("YouTube Music dinonaktifkan oleh feature flag"))
        }
        tokenStorage.saveYouTubeMusicSession(userName, "demo_session_token")
        _youtubeMusicAuthState.value = YouTubeMusicAuthState.Connected(userName)
        return Result.success(Unit)
    }

    override suspend fun disconnectYouTubeMusic() {
        tokenStorage.clearYouTubeMusicSession()
        _youtubeMusicAuthState.value = if (YouTubeMusicConfig.isYouTubeMusicEnabled) YouTubeMusicAuthState.NotConnected else YouTubeMusicAuthState.Disabled
    }
}
