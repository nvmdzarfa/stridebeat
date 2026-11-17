package com.stridebeat.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.stridebeat.app.domain.model.MusicSource
import com.stridebeat.app.domain.model.Track
import com.stridebeat.app.domain.model.WorkoutPlaylist
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private val Context.playlistDataStore: DataStore<Preferences> by preferencesDataStore(name = "stridebeat_user_playlists")

/**
 * Penyimpanan playlist lokal pengguna menggunakan DataStore Preferences.
 * Menyimpan playlist hasil impor dari YouTube/YouTube Music secara aman tanpa memerlukan
 * migrasi skema SQLite Room.
 */
@Singleton
class PlaylistStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val userPlaylists: Flow<List<WorkoutPlaylist>> = context.playlistDataStore.data.map { prefs ->
        val json = prefs[KEY_USER_PLAYLISTS]
        if (json.isNullOrBlank()) {
            emptyList()
        } else {
            deserializePlaylists(json)
        }
    }

    suspend fun getUserPlaylistsOnce(): List<WorkoutPlaylist> = userPlaylists.first()

    suspend fun savePlaylist(playlist: WorkoutPlaylist) {
        val current = getUserPlaylistsOnce().toMutableList()
        val existingIndex = current.indexOfFirst { it.id == playlist.id }
        if (existingIndex >= 0) {
            current[existingIndex] = playlist
        } else {
            current.add(0, playlist)
        }
        val serialized = serializePlaylists(current)
        context.playlistDataStore.edit { prefs ->
            prefs[KEY_USER_PLAYLISTS] = serialized
        }
    }

    suspend fun deletePlaylist(playlistId: String) {
        val current = getUserPlaylistsOnce().toMutableList()
        current.removeAll { it.id == playlistId }
        val serialized = serializePlaylists(current)
        context.playlistDataStore.edit { prefs ->
            prefs[KEY_USER_PLAYLISTS] = serialized
        }
    }

    companion object {
        private val KEY_USER_PLAYLISTS = stringPreferencesKey("user_playlists_json")

        fun serializePlaylists(playlists: List<WorkoutPlaylist>): String {
            val rootArray = JSONArray()
            playlists.forEach { pl ->
                val plObj = JSONObject().apply {
                    put("id", pl.id)
                    put("name", pl.name)
                    put("description", pl.description)
                    put("source", pl.source?.name)
                    put("coverUrl", pl.coverUrl)
                    val tracksArray = JSONArray()
                    pl.tracks.forEach { tr ->
                        val trObj = JSONObject().apply {
                            put("id", tr.id)
                            put("isrc", tr.isrc)
                            put("title", tr.title)
                            put("artist", tr.artist)
                            put("album", tr.album)
                            put("durationMs", tr.durationMs)
                            put("coverUrl", tr.coverUrl)
                            put("audioUrl", tr.audioUrl)
                            val platformObj = JSONObject()
                            tr.platformIds.forEach { (src, sid) ->
                                platformObj.put(src.name, sid)
                            }
                            put("platformIds", platformObj)
                        }
                        tracksArray.put(trObj)
                    }
                    put("tracks", tracksArray)
                }
                rootArray.put(plObj)
            }
            return rootArray.toString()
        }

        fun deserializePlaylists(json: String): List<WorkoutPlaylist> {
            val result = mutableListOf<WorkoutPlaylist>()
            try {
                val rootArray = JSONArray(json)
                for (i in 0 until rootArray.length()) {
                    val plObj = rootArray.optJSONObject(i) ?: continue
                    val id = plObj.optString("id")
                    val name = plObj.optString("name")
                    val desc = plObj.optString("description")
                    val sourceStr = if (plObj.has("source") && !plObj.isNull("source")) plObj.optString("source") else null
                    val source = sourceStr?.let { try { MusicSource.valueOf(it) } catch (_: Exception) { null } }
                    val coverUrl = if (plObj.has("coverUrl") && !plObj.isNull("coverUrl")) plObj.optString("coverUrl") else null

                    val tracksArray = plObj.optJSONArray("tracks")
                    val tracks = mutableListOf<Track>()
                    if (tracksArray != null) {
                        for (j in 0 until tracksArray.length()) {
                            val trObj = tracksArray.optJSONObject(j) ?: continue
                            val trId = trObj.optString("id")
                            val isrc = if (trObj.has("isrc") && !trObj.isNull("isrc")) trObj.optString("isrc") else null
                            val title = trObj.optString("title")
                            val artist = trObj.optString("artist")
                            val album = if (trObj.has("album") && !trObj.isNull("album")) trObj.optString("album") else null
                            val durationMs = trObj.optLong("durationMs", 180000L)
                            val trCover = if (trObj.has("coverUrl") && !trObj.isNull("coverUrl")) trObj.optString("coverUrl") else null
                            val audioUrl = if (trObj.has("audioUrl") && !trObj.isNull("audioUrl")) trObj.optString("audioUrl") else null

                            val platformMap = mutableMapOf<MusicSource, String>()
                            val pObj = trObj.optJSONObject("platformIds")
                            if (pObj != null) {
                                val keys = pObj.keys()
                                while (keys.hasNext()) {
                                    val key = keys.next()
                                    try {
                                        val src = MusicSource.valueOf(key)
                                        platformMap[src] = pObj.getString(key)
                                    } catch (_: Exception) {}
                                }
                            }
                            tracks.add(
                                Track(
                                    id = trId,
                                    isrc = isrc,
                                    title = title,
                                    artist = artist,
                                    album = album,
                                    durationMs = durationMs,
                                    coverUrl = trCover,
                                    audioUrl = audioUrl,
                                    platformIds = platformMap
                                )
                            )
                        }
                    }
                    result.add(
                        WorkoutPlaylist(
                            id = id,
                            name = name,
                            description = desc,
                            source = source,
                            coverUrl = coverUrl,
                            tracks = tracks
                        )
                    )
                }
            } catch (_: Exception) {
                // Kembalikan daftar kosong jika format rusak
            }
            return result
        }
    }
}
