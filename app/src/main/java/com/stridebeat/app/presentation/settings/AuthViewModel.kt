package com.stridebeat.app.presentation.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stridebeat.app.domain.model.SpotifyAuthState
import com.stridebeat.app.domain.model.StravaAuthState
import com.stridebeat.app.domain.model.YouTubeMusicAuthState
import com.stridebeat.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val stravaState: StateFlow<StravaAuthState> = authRepository.stravaAuthState
    val spotifyState: StateFlow<SpotifyAuthState> = authRepository.spotifyAuthState
    val youtubeMusicState: StateFlow<YouTubeMusicAuthState> = authRepository.youtubeMusicAuthState

    fun startStravaLogin(context: Context) {
        val authUrl = authRepository.getStravaAuthUrl()
        if (authUrl != null) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun disconnectStrava() {
        viewModelScope.launch {
            authRepository.disconnectStrava()
        }
    }

    fun startSpotifyLogin(context: Context) {
        val authUrl = authRepository.getSpotifyAuthUrl()
        if (authUrl != null) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun disconnectSpotify() {
        viewModelScope.launch {
            authRepository.disconnectSpotify()
        }
    }

    fun connectYouTubeMusic(userName: String) {
        viewModelScope.launch {
            authRepository.connectYouTubeMusic(userName)
        }
    }

    fun disconnectYouTubeMusic() {
        viewModelScope.launch {
            authRepository.disconnectYouTubeMusic()
        }
    }

    fun handleAuthCallback(uri: Uri) {
        val host = uri.host
        val code = uri.getQueryParameter("code")

        if (code != null) {
            viewModelScope.launch {
                when (host) {
                    "strava-callback" -> authRepository.handleStravaCallback(code)
                    "spotify-callback" -> authRepository.handleSpotifyCallback(code)
                }
            }
        }
    }
}
