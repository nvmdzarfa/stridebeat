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
 * Mesin pencarian musik online live yang menghasilkan URL stream audio langsung
 * (.mp4 AAC 320kbps dari JioSaavn CDN & .m4a dari Apple Music)
 * yang dapat diputar 100% lancar oleh Android MediaPlayer.
 */
object OnlineMusicSearchEngine {

    suspend fun searchTracks(query: String): List<Track> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        val results = mutableListOf<Track>()
        val seenSignatures = mutableSetOf<String>()

        // 1. Prioritaskan JioSaavn untuk full 320kbps MP4/AAC streams
        val saavnTracks = searchJioSaavn(trimmed)
        for (track in saavnTracks) {
            val sig = (track.title + "_" + track.artist).lowercase()
            if (seenSignatures.add(sig)) {
                results.add(track)
            }
        }

        // 2. Jika hasil Saavn sedikit (< 3), tambahkan iTunes untuk kelengkapan katalog
        if (results.size < 8) {
            val appleTracks = searchAppleMusic(trimmed)
            for (track in appleTracks) {
                val sig = (track.title + "_" + track.artist).lowercase()
                if (seenSignatures.add(sig)) {
                    results.add(track)
                }
            }
        }

        return@withContext results
    }

    private fun searchJioSaavn(query: String): List<Track> {
        val list = mutableListOf<Track>()
        val endpoints = listOf(
            "https://saavn.sumit.co/api/search/songs?query=",
            "https://saavn.me/search/songs?query="
        )

        for (base in endpoints) {
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val url = URL(base + encoded)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 6000
                    readTimeout = 6000
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                    setRequestProperty("Accept", "application/json")
                }

                if (connection.responseCode == 200) {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val data = json.optJSONObject("data") ?: continue
                    val array = data.optJSONArray("results") ?: continue

                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        val id = item.optString("id")
                        val rawTitle = item.optString("name")
                        val duration = item.optInt("duration", 180) * 1000L

                        var artist = "Artis Musik"
                        val artistsObj = item.optJSONObject("artists")
                        val primaryArtists = artistsObj?.optJSONArray("primary")
                        if (primaryArtists != null && primaryArtists.length() > 0) {
                            artist = primaryArtists.optJSONObject(0)?.optString("name") ?: artist
                        }

                        val albumObj = item.optJSONObject("album")
                        val album = albumObj?.optString("name") ?: "Single"

                        var coverUrl = ""
                        val imgArray = item.optJSONArray("image")
                        if (imgArray != null && imgArray.length() > 0) {
                            coverUrl = imgArray.optJSONObject(imgArray.length() - 1)?.optString("url") ?: ""
                        }

                        var audioUrl = ""
                        val dlArray = item.optJSONArray("downloadUrl")
                        if (dlArray != null && dlArray.length() > 0) {
                            audioUrl = dlArray.optJSONObject(dlArray.length() - 1)?.optString("url") ?: ""
                        }

                        if (audioUrl.isNotBlank() && rawTitle.isNotBlank()) {
                            val cleanTitle = cleanHtmlEntities(rawTitle)
                            list.add(
                                Track(
                                    id = "saavn_$id",
                                    isrc = null,
                                    title = cleanTitle,
                                    artist = cleanHtmlEntities(artist),
                                    album = cleanHtmlEntities(album),
                                    durationMs = duration,
                                    coverUrl = coverUrl,
                                    audioUrl = audioUrl,
                                    platformIds = mapOf(
                                        MusicSource.YOUTUBE_MUSIC to id,
                                        MusicSource.SPOTIFY to id
                                    )
                                )
                            )
                        }
                    }

                    if (list.isNotEmpty()) {
                        break
                    }
                }
            } catch (e: Exception) {
                continue
            }
        }
        return list
    }

    private fun searchAppleMusic(query: String): List<Track> {
        val list = mutableListOf<Track>()
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val urlString = "https://itunes.apple.com/search?term=$encodedQuery&media=music&entity=song&limit=15"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }

            if (connection.responseCode == 200) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonString)
                val results = root.optJSONArray("results") ?: return emptyList()

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
                            id = "apple_$trackId",
                            isrc = null,
                            title = cleanHtmlEntities(title),
                            artist = cleanHtmlEntities(artist),
                            album = cleanHtmlEntities(album),
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
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun cleanHtmlEntities(text: String): String {
        return text
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()
    }
}
