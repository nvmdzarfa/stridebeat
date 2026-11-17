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

    suspend fun fetchSoundCloudPlaylist(playlistUrl: String): Result<com.stridebeat.app.domain.model.WorkoutPlaylist> = withContext(Dispatchers.IO) {
        val trimmed = playlistUrl.trim().removeSurrounding("\"", "\"").removeSurrounding("'", "'")
        val clientId = soundCloudClientId
        try {
            val encoded = URLEncoder.encode(trimmed, "UTF-8")
            val resolveUrl = "https://api-v2.soundcloud.com/resolve?url=$encoded&client_id=$clientId"
            val conn = (URL(resolveUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7000
                readTimeout = 7000
                setRequestProperty("User-Agent", "Mozilla/5.0")
                setRequestProperty("Accept", "application/json")
            }

            if (conn.responseCode != 200) {
                return@withContext Result.failure(Exception("Playlist SoundCloud tidak ditemukan atau disetel privat (HTTP ${conn.responseCode})"))
            }

            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(body)
            val title = root.optString("title", "Playlist SoundCloud").ifBlank { "Playlist SoundCloud" }
            val id = root.optString("id", System.currentTimeMillis().toString())
            val artworkUrl = root.optString("artwork_url", "")
            val rawTracks = root.optJSONArray("tracks") ?: return@withContext Result.failure(Exception("Tidak ada lagu di playlist ini."))

            val tracks = mutableListOf<Track>()
            val missingIds = mutableListOf<String>()

            for (i in 0 until rawTracks.length()) {
                val item = rawTracks.optJSONObject(i) ?: continue
                if (item.has("title") && item.has("media")) {
                    parseSoundCloudTrackItem(item, clientId)?.let { tracks.add(it) }
                } else if (item.has("id")) {
                    missingIds.add(item.optString("id"))
                }
            }

            // Jika ada missing track stubs, ambil dalam batch 50
            if (missingIds.isNotEmpty()) {
                val chunk = missingIds.take(50).joinToString(",")
                val batchUrl = "https://api-v2.soundcloud.com/tracks?ids=$chunk&client_id=$clientId"
                try {
                    val batchConn = (URL(batchUrl).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 7000
                        readTimeout = 7000
                        setRequestProperty("User-Agent", "Mozilla/5.0")
                        setRequestProperty("Accept", "application/json")
                    }
                    if (batchConn.responseCode == 200) {
                        val batchBody = batchConn.inputStream.bufferedReader().use { it.readText() }
                        val batchArr = org.json.JSONArray(batchBody)
                        for (k in 0 until batchArr.length()) {
                            val item = batchArr.optJSONObject(k) ?: continue
                            parseSoundCloudTrackItem(item, clientId)?.let { tracks.add(it) }
                        }
                    }
                } catch (_: Exception) {}
            }

            if (tracks.isEmpty()) {
                return@withContext Result.failure(Exception("Tidak ada lagu yang dapat diputar dalam playlist ini."))
            }

            val cover = artworkUrl.ifBlank { tracks.firstOrNull()?.coverUrl ?: "" }
                .replace("-large.", "-t500x500.")
                .replace("http://", "https://")

            Result.success(
                com.stridebeat.app.domain.model.WorkoutPlaylist(
                    id = "scpl_$id",
                    name = title,
                    description = "${tracks.size} lagu diimpor dari SoundCloud",
                    source = MusicSource.SOUNDCLOUD,
                    coverUrl = cover.ifBlank { null },
                    tracks = tracks
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseSoundCloudTrackItem(item: JSONObject, clientId: String): Track? {
        val durationMs = item.optLong("duration", 0L)
        if (durationMs <= MIN_TRACK_DURATION_MS) return null

        val id = item.optString("id", "")
        val rawTitle = item.optString("title", "")
        if (rawTitle.isBlank()) return null

        val userObj = item.optJSONObject("user")
        val artist = if (userObj == null || userObj.isNull("username")) "Artis" else userObj.optString("username", "Artis")
        val artwork = if (item.isNull("artwork_url")) "" else item.optString("artwork_url", "")

        val cleanTitle = cleanHtmlEntities(rawTitle)
        val cleanArtist = cleanHtmlEntities(artist)

        val mediaObj = item.optJSONObject("media")
        val transcodings = mediaObj?.optJSONArray("transcodings") ?: return null

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

        if (streamEndpoint.isBlank()) return null
        val directUrl = resolveSoundCloudStream(streamEndpoint, clientId)
        if (directUrl.isBlank()) return null

        val rawAvatar = if (userObj == null || userObj.isNull("avatar_url")) "" else userObj.optString("avatar_url", "")
        val rawCover = artwork.ifBlank { rawAvatar }
        val highResArtwork = rawCover
            .replace("-large.", "-t500x500.")
            .replace("http://", "https://")

        return Track(
            id = "sc_$id",
            isrc = null,
            title = cleanTitle,
            artist = cleanArtist,
            album = "SoundCloud • Full Track",
            durationMs = durationMs,
            coverUrl = highResArtwork.ifBlank { null },
            audioUrl = directUrl,
            platformIds = mapOf(MusicSource.SOUNDCLOUD to id)
        )
    }
}
