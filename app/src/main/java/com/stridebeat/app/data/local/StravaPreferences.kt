package com.stridebeat.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.stravaDataStore: DataStore<Preferences> by preferencesDataStore(name = "stridebeat_strava_prefs")

/**
 * Preferensi Strava (DataStore):
 * - autoUploadEnabled: default MATI (false) sesuai PRD §7.1 US-12 & §12.
 * - defaultActivityPrefix: default "StrideBeat" (dipakai untuk format "StrideBeat Run — <tanggal> <jam>").
 */
@Singleton
class StravaPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val autoUploadEnabled: Flow<Boolean> = context.stravaDataStore.data.map { prefs ->
        prefs[KEY_AUTO_UPLOAD_ENABLED] ?: false
    }

    val defaultActivityPrefix: Flow<String> = context.stravaDataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_ACTIVITY_PREFIX]?.takeIf { it.isNotBlank() } ?: DEFAULT_PREFIX
    }

    suspend fun isAutoUploadEnabledOnce(): Boolean = autoUploadEnabled.first()

    suspend fun getDefaultActivityPrefixOnce(): String = defaultActivityPrefix.first()

    suspend fun setAutoUploadEnabled(enabled: Boolean) {
        context.stravaDataStore.edit { prefs ->
            prefs[KEY_AUTO_UPLOAD_ENABLED] = enabled
        }
    }

    suspend fun setDefaultActivityPrefix(prefix: String) {
        val clean = prefix.trim().ifBlank { DEFAULT_PREFIX }
        context.stravaDataStore.edit { prefs ->
            prefs[KEY_DEFAULT_ACTIVITY_PREFIX] = clean
        }
    }

    companion object {
        const val DEFAULT_PREFIX = "StrideBeat"
        private val KEY_AUTO_UPLOAD_ENABLED = booleanPreferencesKey("strava_auto_upload_enabled")
        private val KEY_DEFAULT_ACTIVITY_PREFIX = stringPreferencesKey("strava_default_activity_prefix")
    }
}
