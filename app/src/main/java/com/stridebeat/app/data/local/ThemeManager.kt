package com.stridebeat.app.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class AppThemeMode(val displayName: String) {
    DARK("Gelap"),
    LIGHT("Terang"),
    SYSTEM("Sistem")
}

@Singleton
class ThemeManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("stridebeat_ui_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadSavedMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private fun loadSavedMode(): AppThemeMode {
        val raw = prefs.getString(KEY_THEME_MODE, AppThemeMode.DARK.name)
        return try {
            AppThemeMode.valueOf(raw ?: AppThemeMode.DARK.name)
        } catch (_: Exception) {
            AppThemeMode.DARK
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun toggleDarkLight(currentlyDark: Boolean) {
        setThemeMode(if (currentlyDark) AppThemeMode.LIGHT else AppThemeMode.DARK)
    }

    companion object {
        private const val KEY_THEME_MODE = "app_theme_mode"
    }
}
