package com.stridebeat.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.stridebeat.app.domain.model.MusicSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.musicDataStore: DataStore<Preferences> by preferencesDataStore(name = "stridebeat_music_prefs")

/**
 * Preferensi Musik pengguna menggunakan DataStore:
 * - youtubeMusicEnabled: Feature flag YouTube Music Beta (default true sesuai PRD & persetujuan pemilik).
 * - preferredMusicSource: Sumber musik utama (default SOUNDCLOUD).
 */
@Singleton
class MusicPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val youtubeMusicEnabled: Flow<Boolean> = context.musicDataStore.data.map { prefs ->
        prefs[KEY_YOUTUBE_MUSIC_ENABLED] ?: true
    }

    val preferredMusicSource: Flow<MusicSource> = context.musicDataStore.data.map { prefs ->
        val savedName = prefs[KEY_PREFERRED_MUSIC_SOURCE]
        if (savedName.isNullOrBlank()) {
            MusicSource.SOUNDCLOUD
        } else {
            try {
                val source = MusicSource.valueOf(savedName)
                if (source == MusicSource.SPOTIFY) MusicSource.SOUNDCLOUD else source
            } catch (_: Exception) {
                MusicSource.SOUNDCLOUD
            }
        }
    }

    suspend fun isYouTubeMusicEnabledOnce(): Boolean = youtubeMusicEnabled.first()

    suspend fun getPreferredMusicSourceOnce(): MusicSource = preferredMusicSource.first()

    suspend fun setYouTubeMusicEnabled(enabled: Boolean) {
        context.musicDataStore.edit { prefs ->
            prefs[KEY_YOUTUBE_MUSIC_ENABLED] = enabled
        }
    }

    suspend fun setPreferredMusicSource(source: MusicSource) {
        context.musicDataStore.edit { prefs ->
            prefs[KEY_PREFERRED_MUSIC_SOURCE] = source.name
        }
    }

    companion object {
        private val KEY_YOUTUBE_MUSIC_ENABLED = booleanPreferencesKey("youtube_music_enabled")
        private val KEY_PREFERRED_MUSIC_SOURCE = stringPreferencesKey("preferred_music_source")
    }
}
