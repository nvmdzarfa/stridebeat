package com.stridebeat.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.workoutDataStore: DataStore<Preferences> by preferencesDataStore(name = "stridebeat_workout_prefs")

/**
 * Preferensi Pengguna untuk sesi Workout & Panduan Suara (Voice Audio Cue TTS):
 * - voiceCueEnabled: Apakah audio cue diaktifkan (default true)
 * - voiceCueIntervalKm: Interval laporan KM split (default 1.0 km)
 * - autoPauseEnabled: Apakah auto-pause aktif saat pelari berhenti (default true)
 * - isKmUnit: Satuan jarak km vs miles (default true)
 */
@Singleton
class WorkoutPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val voiceCueEnabled: Flow<Boolean> = context.workoutDataStore.data.map { prefs ->
        prefs[KEY_VOICE_CUE_ENABLED] ?: true
    }

    val voiceCueIntervalKm: Flow<Double> = context.workoutDataStore.data.map { prefs ->
        prefs[KEY_VOICE_CUE_INTERVAL_KM] ?: 1.0
    }

    val autoPauseEnabled: Flow<Boolean> = context.workoutDataStore.data.map { prefs ->
        prefs[KEY_AUTO_PAUSE_ENABLED] ?: true
    }

    val isKmUnit: Flow<Boolean> = context.workoutDataStore.data.map { prefs ->
        prefs[KEY_IS_KM_UNIT] ?: true
    }

    suspend fun isVoiceCueEnabledOnce(): Boolean = voiceCueEnabled.first()

    suspend fun getVoiceCueIntervalKmOnce(): Double = voiceCueIntervalKm.first()

    suspend fun setVoiceCueEnabled(enabled: Boolean) {
        context.workoutDataStore.edit { prefs ->
            prefs[KEY_VOICE_CUE_ENABLED] = enabled
        }
    }

    suspend fun setVoiceCueIntervalKm(interval: Double) {
        context.workoutDataStore.edit { prefs ->
            prefs[KEY_VOICE_CUE_INTERVAL_KM] = interval
        }
    }

    suspend fun setAutoPauseEnabled(enabled: Boolean) {
        context.workoutDataStore.edit { prefs ->
            prefs[KEY_AUTO_PAUSE_ENABLED] = enabled
        }
    }

    suspend fun setIsKmUnit(isKm: Boolean) {
        context.workoutDataStore.edit { prefs ->
            prefs[KEY_IS_KM_UNIT] = isKm
        }
    }

    companion object {
        private val KEY_VOICE_CUE_ENABLED = booleanPreferencesKey("voice_cue_enabled")
        private val KEY_VOICE_CUE_INTERVAL_KM = doublePreferencesKey("voice_cue_interval_km")
        private val KEY_AUTO_PAUSE_ENABLED = booleanPreferencesKey("auto_pause_enabled")
        private val KEY_IS_KM_UNIT = booleanPreferencesKey("is_km_unit")
    }
}
