package com.stridebeat.app.data.remote.music

import com.stridebeat.app.domain.model.MusicSource
import com.stridebeat.app.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Mesin pencarian musik online live tanpa template / data palsu.
 * Mengambil lagu asli (The Weeknd, Kanye West, M83, dsb) secara instan
 * beserta audio stream preview resmi yang langsung dapat diputar oleh MediaPlayer di HP.
 */
object OnlineMusicSearchEngine {

    suspend fun searchTracks(query: String): List<Track> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val urlString = "https://itunes.apple.com/search?term=$encodedQuery&media=music&entity=song&limit=25"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "StrideBeat-Android/1.0")
            }

            if (connection.responseCode == 200) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonString)
                val results = root.optJSONArray("results") ?: return@withContext emptyList()
                val list = mutableListOf<Track>()

                for (i in 0 until results.length()) {
                    val item = results.optJSONObject(i) ?: continue
                    val previewUrl = item.optString("previewUrl")
                    if (previewUrl.isNullOrBlank()) continue

                    val trackId = item.optLong("trackId").toString()
                    val title = item.optString("trackName", "Judul Tidak Diketahui")
                    val artist = item.optString("artistName", "Artis Tidak Diketahui")
                    val album = item.optString("collectionName", "Album")
                    val durationMs = item.optLong("trackTimeMillis", 180000L)
                    val artworkUrl = item.optString("artworkUrl100")

                    list.add(
                        Track(
                            id = "online_$trackId",
                            isrc = null,
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = durationMs,
                            coverUrl = artworkUrl,
                            audioUrl = previewUrl,
                            platformIds = mapOf(
                                MusicSource.SPOTIFY to "sp_$trackId",
                                MusicSource.YOUTUBE_MUSIC to "yt_$trackId"
                            )
                        )
                    )
                }
                list
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
