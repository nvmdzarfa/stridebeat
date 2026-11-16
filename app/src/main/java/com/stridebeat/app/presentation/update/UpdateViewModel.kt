package com.stridebeat.app.presentation.update

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stridebeat.app.BuildConfig
import com.stridebeat.app.data.remote.update.AppUpdateChecker
import com.stridebeat.app.data.remote.update.AppUpdateInfo
import com.stridebeat.app.data.remote.update.UpdateCheckResult
import com.stridebeat.app.service.UpdateNotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _pendingUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val pendingUpdate: StateFlow<AppUpdateInfo?> = _pendingUpdate.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private var hasCheckedOnStartup = false

    fun checkOnStartupIfNeeded() {
        if (hasCheckedOnStartup) return
        hasCheckedOnStartup = true
        checkForUpdates(isManual = false)
    }

    fun checkForUpdates(isManual: Boolean = true) {
        if (_isChecking.value) return
        viewModelScope.launch {
            _isChecking.value = true
            if (isManual) {
                _statusMessage.value = "Memeriksa pembaruan..."
            }
            when (val result = AppUpdateChecker.checkLatestRelease()) {
                is UpdateCheckResult.UpdateAvailable -> {
                    _pendingUpdate.value = result.info
                    _statusMessage.value = "Versi baru v${result.info.latestVersion} tersedia!"
                    UpdateNotificationHelper.showUpdateNotification(context, result.info)
                }
                is UpdateCheckResult.UpToDate -> {
                    if (isManual) {
                        _statusMessage.value = "Kamu sudah menggunakan versi terbaru (v${result.currentVersion})."
                    }
                }
                is UpdateCheckResult.Unavailable -> {
                    if (isManual) {
                        _statusMessage.value = result.reason
                    }
                }
            }
            _isChecking.value = false
        }
    }

    fun triggerPreviewUpdatePopupAndNotif() {
        val previewInfo = AppUpdateInfo(
            currentVersion = BuildConfig.VERSION_NAME,
            latestVersion = "0.0.11",
            releaseNotes = "• Contoh tampilan notifikasi & popup update otomatis.\n• Saat rilis baru tersedia di GitHub Releases, popup & notif bar akan muncul seperti ini.",
            downloadUrl = AppUpdateChecker.RELEASES_PAGE_URL,
            releasePageUrl = AppUpdateChecker.RELEASES_PAGE_URL,
            isPreview = true
        )
        _pendingUpdate.value = previewInfo
        UpdateNotificationHelper.showUpdateNotification(context, previewInfo)
    }

    fun dismissUpdateDialog() {
        _pendingUpdate.value = null
    }
}
