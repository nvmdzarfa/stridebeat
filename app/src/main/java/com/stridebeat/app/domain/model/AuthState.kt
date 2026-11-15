package com.stridebeat.app.domain.model

sealed interface StravaAuthState {
    object NotConnected : StravaAuthState
    object MissingConfig : StravaAuthState // Saat STRAVA_CLIENT_ID belum diisi di local.properties
    object Connecting : StravaAuthState
    data class Connected(val athleteName: String, val athleteId: Long? = null) : StravaAuthState
    object TokenExpired : StravaAuthState
    data class Error(val message: String) : StravaAuthState
}

sealed interface SpotifyAuthState {
    object NotConnected : SpotifyAuthState
    object MissingConfig : SpotifyAuthState // Saat SPOTIFY_CLIENT_ID belum diisi di local.properties
    object Connecting : SpotifyAuthState
    data class Connected(val userName: String, val isPremium: Boolean) : SpotifyAuthState
    object TokenExpired : SpotifyAuthState
    data class Error(val message: String) : SpotifyAuthState
}

sealed interface YouTubeMusicAuthState {
    object Disabled : YouTubeMusicAuthState // Jika feature flag youtubeMusicEnabled dimatikan
    object NotConnected : YouTubeMusicAuthState
    object Connecting : YouTubeMusicAuthState
    data class Connected(val userName: String) : YouTubeMusicAuthState
    data class Error(val message: String) : YouTubeMusicAuthState
}
