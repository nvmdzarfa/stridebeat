package com.stridebeat.app.presentation.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stridebeat.app.domain.model.SpotifyAuthState
import com.stridebeat.app.domain.model.StravaAuthState
import com.stridebeat.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val spotifyState: StateFlow<SpotifyAuthState> = authRepository.spotifyAuthState
    val stravaState: StateFlow<StravaAuthState> = authRepository.stravaAuthState

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

    fun handleAuthCallback(uri: Uri) {
        val host = uri.host
        val code = uri.getQueryParameter("code")
        val error = uri.getQueryParameter("error")

        when (host) {
            "spotify-callback" -> {
                if (code != null) {
                    viewModelScope.launch {
                        authRepository.handleSpotifyCallback(code)
                    }
                }
            }
            "strava-callback" -> {
                if (code != null) {
                    viewModelScope.launch {
                        authRepository.handleStravaCallback(code)
                    }
                } else {
                    authRepository.handleStravaAuthCancelledOrDenied(error)
                }
            }
        }
    }
}
