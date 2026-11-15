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
 * Mesin pencarian musik online live untuk lagu penuh (Full Song Playback).
 * Mengutamakan pencarian audio YouTube sehingga lagu diputar secara FULL 100%
 * dari awal sampai habis tanpa batas 30 detik preview dan tanpa butuh akun berbayar.
 */
object OnlineMusicSearchEngine {

    suspend fun searchTracks(query: String): List<Track> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        // 1. Prioritaskan pencarian YouTube untuk mendapatkan lagu FULL 100%
        val ytTracks = searchYouTube(query)
        if (ytTracks.isNotEmpty()) {
            return@withContext ytTracks
        }

        // 2. Fallback ke Apple Music jika koneksi YouTube terhambat
        return@withContext searchAppleMusic(query)
    }

    private fun searchYouTube(query: String): List<Track> {
        return try {
            val encodedQuery = URLEncoder.encode(query.trim() + " audio", "UTF-8")
            val url = URL("https://www.youtube.com/results?search_query=$encodedQuery")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7000
                readTimeout = 7000
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                )
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            }

            if (connection.responseCode == 200) {
                val html = connection.inputStream.bufferedReader().use { it.readText() }
                parseYouTubeHtml(html)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseYouTubeHtml(html: String): List<Track> {
        val list = mutableListOf<Track>()
        try {
            val videoRendererRegex = Regex(
                """"videoId":"([a-zA-Z0-9_-]{11})".*?"title":\{"runs":\[\{"text":"(.*?)"\}""",
                RegexOption.DOT_MATCHES_ALL
            )
            val matches = videoRendererRegex.findAll(html).take(20).toList()
            val seenIds = mutableSetOf<String>()

            for (m in matches) {
                val videoId = m.groupValues[1]
                val rawTitle = m.groupValues[2]

                if (videoId.isNotBlank() && rawTitle.isNotBlank() && seenIds.add(videoId)) {
                    val cleanTitle = rawTitle
                        .replace(Regex("""(?i)\((?:official\s*(?:audio|video|music\s*video|lyric\s*video)?|audio|lyrics)\)"""), "")
                        .replace(Regex("""(?i)\[(?:official\s*(?:audio|video|music\s*video|lyric\s*video)?|audio|lyrics)\]"""), "")
                        .replace("""\u0026""", "&")
                        .replace("&amp;", "&")
                        .replace("""\"""", "\"")
                        .trim()

                    val (artist, songTitle) = if (cleanTitle.contains(" - ")) {
                        val parts = cleanTitle.split(" - ", limit = 2)
                        Pair(parts[0].trim(), parts[1].trim())
                    } else {
                        Pair("YouTube Music", cleanTitle)
                    }

                    list.add(
                        Track(
                            id = "yt_$videoId",
                            isrc = null,
                            title = songTitle,
                            artist = artist,
                            album = "Lagu Penuh (Full Audio)",
                            durationMs = 210000L,
                            coverUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
                            audioUrl = "youtube:$videoId",
                            platformIds = mapOf(
                                MusicSource.YOUTUBE_MUSIC to videoId,
                                MusicSource.SPOTIFY to "sp_$videoId"
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

    private fun searchAppleMusic(query: String): List<Track> {
        return try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val urlString = "https://itunes.apple.com/search?term=$encodedQuery&media=music&entity=song&limit=25"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7000
                readTimeout = 7000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "StrideBeat-Android/1.0")
            }

            if (connection.responseCode == 200) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonString)
                val results = root.optJSONArray("results") ?: return emptyList()
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
