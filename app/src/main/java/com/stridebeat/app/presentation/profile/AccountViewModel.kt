package com.stridebeat.app.presentation.profile

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stridebeat.app.data.remote.firebase.FirebaseSyncManager
import com.stridebeat.app.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val firebaseSyncManager: FirebaseSyncManager
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = firebaseSyncManager.userProfile
    val syncMessage: StateFlow<String?> = firebaseSyncManager.syncMessage

    private val _isSigningIn = MutableStateFlow(false)
    val isSigningIn: StateFlow<Boolean> = _isSigningIn.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val isFirebaseConfigured: Boolean
        get() = firebaseSyncManager.isFirebaseConfigured

    init {
        viewModelScope.launch {
            if (firebaseSyncManager.userProfile.value.isGoogleSignedIn) {
                firebaseSyncManager.syncAllOnSignIn()
            }
        }
    }

    fun launchGoogleSignIn(activityContext: Context, onIntentReady: (Intent) -> Unit) {
        if (_isSigningIn.value) return
        viewModelScope.launch {
            _isSigningIn.value = true
            _errorMessage.value = null
            val intentResult = try {
                firebaseSyncManager.createGoogleSignInIntent(activityContext)
            } catch (e: Exception) {
                Result.failure(e)
            }
            intentResult.onSuccess { intent ->
                try {
                    onIntentReady(intent)
                } catch (e: Exception) {
                    _errorMessage.value = e.localizedMessage ?: "Gagal membuka dialog akun Google."
                    _isSigningIn.value = false
                }
            }.onFailure { err ->
                _errorMessage.value = err.message
                _isSigningIn.value = false
            }
        }
    }

    fun handleGoogleSignInResult(resultCode: Int, data: Intent?) {
        viewModelScope.launch {
            _isSigningIn.value = true
            _errorMessage.value = null
            try {
                val result = firebaseSyncManager.handleGoogleSignInIntentResult(resultCode, data)
                result.onFailure { err ->
                    _errorMessage.value = err.message
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Terjadi kesalahan saat login Google."
            } finally {
                _isSigningIn.value = false
            }
        }
    }

    fun syncNow() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            _errorMessage.value = null
            try {
                firebaseSyncManager.syncAllOnSignIn()
                    .onFailure { _errorMessage.value = it.message }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Gagal sinkronisasi cloud."
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun updateDisplayName(newName: String) {
        viewModelScope.launch {
            _errorMessage.value = null
            firebaseSyncManager.updateDisplayName(newName)
                .onFailure { _errorMessage.value = it.message }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _errorMessage.value = null
            firebaseSyncManager.signOut()
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
