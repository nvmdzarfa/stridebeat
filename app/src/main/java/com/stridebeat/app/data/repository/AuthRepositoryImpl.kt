package com.stridebeat.app.data.repository

import com.stridebeat.app.data.local.SecureTokenStorage
import com.stridebeat.app.data.remote.spotify.SpotifyAuthManager
import com.stridebeat.app.data.remote.strava.StravaAuthManager
import com.stridebeat.app.domain.model.SpotifyAuthState
import com.stridebeat.app.domain.model.StravaAuthState
import com.stridebeat.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val tokenStorage: SecureTokenStorage,
    private val spotifyAuthManager: SpotifyAuthManager,
    private val stravaAuthManager: StravaAuthManager
) : AuthRepository {

    private val _spotifyAuthState = MutableStateFlow<SpotifyAuthState>(SpotifyAuthState.NotConnected)
    override val spotifyAuthState: StateFlow<SpotifyAuthState> = _spotifyAuthState.asStateFlow()

    private val _stravaAuthState = MutableStateFlow<StravaAuthState>(StravaAuthState.NotConnected)
    override val stravaAuthState: StateFlow<StravaAuthState> = _stravaAuthState.asStateFlow()

    init {
        restoreStoredAuthStates()
    }

    private fun restoreStoredAuthStates() {
        restoreSpotifyState()
        restoreStravaState()
    }

    private fun restoreSpotifyState() {
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

    private fun restoreStravaState() {
        if (!stravaAuthManager.isConfigured()) {
            _stravaAuthState.value = StravaAuthState.MissingConfig
            return
        }
        val accessToken = tokenStorage.getStravaAccessToken()
        val refreshToken = tokenStorage.getStravaRefreshToken()
        val athleteName = tokenStorage.getStravaAthleteName()
        val athleteId = tokenStorage.getStravaAthleteId()
        val expiresAt = tokenStorage.getStravaExpiresAt()

        if (!accessToken.isNullOrBlank()) {
            val expiredWithoutRefresh = System.currentTimeMillis() > expiresAt && expiresAt > 0L && refreshToken.isNullOrBlank()
            if (expiredWithoutRefresh) {
                _stravaAuthState.value = StravaAuthState.TokenExpired
            } else {
                _stravaAuthState.value = StravaAuthState.Connected(
                    athleteName = athleteName ?: "Atlet Strava",
                    athleteId = athleteId
                )
            }
        } else {
            _stravaAuthState.value = StravaAuthState.NotConnected
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
            val (athleteName, athleteId) = result.getOrThrow()
            _stravaAuthState.value = StravaAuthState.Connected(athleteName, athleteId)
            Result.success(Unit)
        } else {
            val err = result.exceptionOrNull()?.localizedMessage ?: "Gagal login Strava"
            _stravaAuthState.value = StravaAuthState.Error(err)
            Result.failure(Exception(err))
        }
    }

    override fun handleStravaAuthCancelledOrDenied(error: String?) {
        if (!stravaAuthManager.isConfigured()) {
            _stravaAuthState.value = StravaAuthState.MissingConfig
            return
        }
        _stravaAuthState.value = if (error == "access_denied") {
            StravaAuthState.NotConnected
        } else {
            StravaAuthState.Error("Otorisasi Strava dibatalkan atau ditolak.")
        }
    }

    override suspend fun disconnectStrava() {
        // Hanya menghapus token Strava — tidak menyentuh Google/Spotify/SoundCloud/YouTube Music
        tokenStorage.clearStravaTokens()
        _stravaAuthState.value = if (stravaAuthManager.isConfigured()) {
            StravaAuthState.NotConnected
        } else {
            StravaAuthState.MissingConfig
        }
    }

    override fun markStravaTokenExpired() {
        if (stravaAuthManager.isConfigured()) {
            _stravaAuthState.value = StravaAuthState.TokenExpired
        }
    }

    override fun refreshStravaState() {
        restoreStravaState()
    }
}
