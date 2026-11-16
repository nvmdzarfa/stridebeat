package com.stridebeat.app.presentation.strava

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stridebeat.app.data.local.StravaPreferences
import com.stridebeat.app.data.repository.StravaRepository
import com.stridebeat.app.domain.model.StravaAuthState
import com.stridebeat.app.domain.model.StravaUploadStatus
import com.stridebeat.app.domain.model.WorkoutSession
import com.stridebeat.app.domain.repository.AuthRepository
import com.stridebeat.app.util.StravaFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StravaViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val stravaRepository: StravaRepository,
    private val stravaPreferences: StravaPreferences
) : ViewModel() {

    val stravaAuthState: StateFlow<StravaAuthState> = authRepository.stravaAuthState

    val autoUploadEnabled: StateFlow<Boolean> = stravaPreferences.autoUploadEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val defaultActivityPrefix: StateFlow<String> = stravaPreferences.defaultActivityPrefix
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StravaPreferences.DEFAULT_PREFIX)

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _autoUploadNotice = MutableStateFlow<String?>(null)
    val autoUploadNotice: StateFlow<String?> = _autoUploadNotice.asStateFlow()

    private val autoUploadHandledSessionIds = mutableSetOf<String>()

    fun setAutoUploadEnabled(enabled: Boolean) {
        viewModelScope.launch {
            stravaPreferences.setAutoUploadEnabled(enabled)
        }
    }

    fun setDefaultActivityPrefix(prefix: String) {
        viewModelScope.launch {
            stravaPreferences.setDefaultActivityPrefix(prefix)
        }
    }

    fun buildDefaultActivityName(session: WorkoutSession): String {
        return StravaFormatter.buildDefaultActivityName(session, defaultActivityPrefix.value)
    }

    fun buildDefaultDescription(session: WorkoutSession): String {
        return StravaFormatter.buildActivityDescription(session)
    }

    fun uploadSession(
        sessionId: String,
        customName: String,
        customDescription: String
    ) {
        viewModelScope.launch {
            _statusMessage.value = null
            val res = stravaRepository.queueUpload(sessionId, customName, customDescription)
            _statusMessage.value = if (res.isSuccess) {
                "Upload dijadwalkan ke Strava. Jika offline, otomatis diunggah saat tersambung."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Gagal menjadwalkan upload Strava."
            }
        }
    }

    /**
     * Evaluasi auto-upload ketika sesi baru selesai dan masuk ke layar Recap (US-12).
     */
    fun tryAutoUploadIfEligible(session: WorkoutSession) {
        if (autoUploadHandledSessionIds.contains(session.id)) return
        if (stravaAuthState.value !is StravaAuthState.Connected) return
        if (session.stravaUploadStatus != StravaUploadStatus.NOT_UPLOADED) return

        autoUploadHandledSessionIds.add(session.id)

        viewModelScope.launch {
            val isAutoEnabled = stravaPreferences.isAutoUploadEnabledOnce()
            if (!isAutoEnabled) return@launch

            val (eligible, reason) = StravaFormatter.isEligibleForAutoUpload(session)
            if (!eligible) {
                _autoUploadNotice.value = reason
                return@launch
            }

            val name = buildDefaultActivityName(session)
            val desc = buildDefaultDescription(session)
            val res = stravaRepository.queueUpload(session.id, name, desc)
            _autoUploadNotice.value = if (res.isSuccess) {
                "Auto-upload aktif: sesi dijadwalkan ke Strava."
            } else {
                res.exceptionOrNull()?.localizedMessage
            }
        }
    }

    fun importRecentActivities() {
        if (_isImporting.value) return
        viewModelScope.launch {
            _isImporting.value = true
            _statusMessage.value = null
            val result = stravaRepository.importRecentActivities()
            _statusMessage.value = if (result.isSuccess) {
                val count = result.getOrDefault(0)
                if (count > 0) "Berhasil mengimpor $count aktivitas baru dari Strava."
                else "Semua 30 aktivitas Strava terbaru sudah ada di riwayatmu."
            } else {
                result.exceptionOrNull()?.localizedMessage ?: "Gagal mengimpor riwayat dari Strava."
            }
            _isImporting.value = false
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
