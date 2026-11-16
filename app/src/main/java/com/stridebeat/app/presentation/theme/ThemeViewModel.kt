package com.stridebeat.app.presentation.theme

import androidx.lifecycle.ViewModel
import com.stridebeat.app.data.local.AppThemeMode
import com.stridebeat.app.data.local.ThemeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeManager: ThemeManager
) : ViewModel() {

    val themeMode: StateFlow<AppThemeMode> = themeManager.themeMode

    fun setThemeMode(mode: AppThemeMode) {
        themeManager.setThemeMode(mode)
    }

    fun toggleDarkLight(currentlyDark: Boolean) {
        themeManager.toggleDarkLight(currentlyDark)
    }
}
