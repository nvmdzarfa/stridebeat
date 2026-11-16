package com.stridebeat.app.data.remote.music

import com.stridebeat.app.BuildConfig
import com.stridebeat.app.domain.model.MusicSource
import com.stridebeat.app.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Mesin pencarian musik global menggunakan SoundCloud API.
 * Menghapus ketergantungan pada API regional India (JioSaavn) sehingga hasil pencarian
 * 100% akurat dengan apa yang dicari pengguna (Rex Orange County, The Weeknd, Kanye, Phonk, Pop Hits, Hindia, dll.).
 * Secara ketat memfilter klip pendek sehingga hanya lagu FULL SONG (durasi > 45 detik) yang ditampilkan.
 */
object OnlineMusicSearchEngine {

    private const val DEFAULT_SOUNDCLOUD_CLIENT_ID = "qLcgHhXgeEFFaSolTB9I71LBsqncsD93"
    private const val MIN_TRACK_DURATION_MS = 45000L // Minimal 45 detik (menolak snippet 30 detik)

    private val soundCloudClientId: String
        get() = runCatching { BuildConfig.SOUNDCLOUD_CLIENT_ID }.getOrNull()?.ifBlank { DEFAULT_SOUNDCLOUD_CLIENT_ID }
            ?: DEFAULT_SOUNDCLOUD_CLIENT_ID

    suspend fun searchTracks(query: String): List<Track> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        return@withContext searchSoundCloud(trimmed)
    }

    private fun searchSoundCloud(query: String): List<Track> {
        val list = mutableListOf<Track>()
        val seenSignatures = mutableSetOf<String>()
        val clientId = soundCloudClientId

        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "https://api-v2.soundcloud.com/search/tracks?q=$encoded&client_id=$clientId&limit=20"
            val connection = (URL(searchUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode == 200) {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(body)
                val collection = root.optJSONArray("collection") ?: return emptyList()

                for (i in 0 until collection.length()) {
                    val item = collection.optJSONObject(i) ?: continue
                    val durationMs = item.optLong("duration", 0L)

                    // HANYA ambil lagu dengan durasi > 45 detik (menolak snippet preview 30 detik)
                    if (durationMs <= MIN_TRACK_DURATION_MS) continue

                    val id = item.optString("id", "")
                    val rawTitle = item.optString("title", "")
                    if (rawTitle.isBlank()) continue

                    val userObj = item.optJSONObject("user")
                    val artist = if (userObj == null || userObj.isNull("username")) "Artis" else userObj.optString("username", "Artis")
                    val artwork = if (item.isNull("artwork_url")) "" else item.optString("artwork_url", "")

                    val cleanTitle = cleanHtmlEntities(rawTitle)
                    val cleanArtist = cleanHtmlEntities(artist)
                    val sig = (cleanTitle + "_" + cleanArtist).lowercase()

                    if (!seenSignatures.add(sig)) continue

                    val mediaObj = item.optJSONObject("media")
                    val transcodings = mediaObj?.optJSONArray("transcodings") ?: continue

                    var streamEndpoint = ""
                    for (j in 0 until transcodings.length()) {
                        val t = transcodings.optJSONObject(j) ?: continue
                        val format = t.optJSONObject("format")
                        val protocol = format?.optString("protocol", "") ?: ""
                        val mimeType = format?.optString("mime_type", "") ?: ""

                        if (protocol == "hls" && mimeType.contains("mp4")) {
                            streamEndpoint = t.optString("url", "")
                            break
                        } else if (protocol == "progressive") {
                            streamEndpoint = t.optString("url", "")
                        }
                    }

                    val rawAvatar = if (userObj == null || userObj.isNull("avatar_url")) "" else userObj.optString("avatar_url", "")
                    val rawCover = artwork.ifBlank { rawAvatar }
                    val highResArtwork = rawCover
                        .replace("-large.", "-t500x500.")
                        .replace("http://", "https://")

                    if (streamEndpoint.isNotBlank()) {
                        val directUrl = resolveSoundCloudStream(streamEndpoint, clientId)
                        if (directUrl.isNotBlank()) {
                            list.add(
                                Track(
                                    id = "sc_$id",
                                    isrc = null,
                                    title = cleanTitle,
                                    artist = cleanArtist,
                                    album = "SoundCloud • Full Track",
                                    durationMs = durationMs,
                                    coverUrl = highResArtwork.ifBlank { null },
                                    audioUrl = directUrl,
                                    platformIds = mapOf(
                                        MusicSource.SOUNDCLOUD to id
                                    )
                                )
                            )
                        }
                    }

                    // Cukup ambil 10-12 lagu terbaik agar respon cepat
                    if (list.size >= 12) break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun resolveSoundCloudStream(endpoint: String, clientId: String): String {
        return try {
            val url = URL("$endpoint?client_id=$clientId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                json.optString("url", "")
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
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
