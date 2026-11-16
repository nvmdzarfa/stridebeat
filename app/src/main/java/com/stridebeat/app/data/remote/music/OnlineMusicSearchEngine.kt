package com.stridebeat.app.data.remote.music

import com.stridebeat.app.domain.model.MusicSource
import com.stridebeat.app.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Mesin pencarian musik online live untuk lagu penuh (Full Song Playback).
 * Mengintegrasikan JioSaavn CDN (320kbps MP4/AAC) dan SoundCloud API (HLS/MP3),
 * secara ketat memfilter klip pendek sehingga hanya lagu berdurasi PENUH (full track)
 * yang ditampilkan dan dapat diputar 100% tanpa batas 30 detik.
 */
object OnlineMusicSearchEngine {

    private const val SOUNDCLOUD_CLIENT_ID = "qLcgHhXgeEFFaSolTB9I71LBsqncsD93"
    private const val MIN_TRACK_DURATION_MS = 45000L // Minimal 45 detik (menolak preview 30 detik)

    suspend fun searchTracks(query: String): List<Track> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        val results = mutableListOf<Track>()
        val seenSignatures = mutableSetOf<String>()

        // Jalankan pencarian JioSaavn dan SoundCloud secara paralel (concurrent)
        coroutineScope {
            val saavnDeferred = async { searchJioSaavn(trimmed) }
            val soundCloudDeferred = async { searchSoundCloud(trimmed) }

            val saavnTracks = saavnDeferred.await()
            val soundCloudTracks = soundCloudDeferred.await()

            // 1. Masukkan hasil Saavn terlebih dahulu (kualitas studio 320kbps)
            for (track in saavnTracks) {
                if (track.durationMs > MIN_TRACK_DURATION_MS) {
                    val sig = (track.title + "_" + track.artist).lowercase()
                    if (seenSignatures.add(sig)) {
                        results.add(track)
                    }
                }
            }

            // 2. Gabungkan hasil SoundCloud (untuk indie, Rex Orange County, pop, phonk full length)
            for (track in soundCloudTracks) {
                if (track.durationMs > MIN_TRACK_DURATION_MS) {
                    val sig = (track.title + "_" + track.artist).lowercase()
                    if (seenSignatures.add(sig)) {
                        results.add(track)
                    }
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
                    connectTimeout = 5000
                    readTimeout = 5000
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
                        val duration = item.optInt("duration", 0) * 1000L
                        // Lewati jika lagu pendek / preview
                        if (duration <= MIN_TRACK_DURATION_MS) continue

                        val id = item.optString("id")
                        val rawTitle = item.optString("name")
                        if (rawTitle.isBlank()) continue

                        var artist = "Artis Musik"
                        val artistsObj = item.optJSONObject("artists")
                        val primaryArtists = artistsObj?.optJSONArray("primary")
                        if (primaryArtists != null && primaryArtists.length() > 0) {
                            artist = primaryArtists.optJSONObject(0)?.optString("name") ?: artist
                        }

                        val albumObj = item.optJSONObject("album")
                        val album = albumObj?.optString("name") ?: "Lagu Penuh"

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

                        if (audioUrl.isNotBlank()) {
                            list.add(
                                Track(
                                    id = "saavn_$id",
                                    isrc = null,
                                    title = cleanHtmlEntities(rawTitle),
                                    artist = cleanHtmlEntities(artist),
                                    album = cleanHtmlEntities(album),
                                    durationMs = duration,
                                    coverUrl = coverUrl.ifBlank { null },
                                    audioUrl = audioUrl,
                                    platformIds = mapOf(
                                        MusicSource.YOUTUBE_MUSIC to id,
                                        MusicSource.SPOTIFY to id
                                    )
                                )
                            )
                        }
                    }

                    if (list.isNotEmpty()) break
                }
            } catch (e: Exception) {
                continue
            }
        }
        return list
    }

    private fun searchSoundCloud(query: String): List<Track> {
        val list = mutableListOf<Track>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "https://api-v2.soundcloud.com/search/tracks?q=$encoded&client_id=$SOUNDCLOUD_CLIENT_ID&limit=8"
            val connection = (URL(searchUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "Mozilla/5.0")
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode == 200) {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(body)
                val collection = root.optJSONArray("collection") ?: return emptyList()

                for (i in 0 until collection.length()) {
                    val item = collection.optJSONObject(i) ?: continue
                    val durationMs = item.optLong("duration", 0L)
                    // HANYA ambil lagu dengan durasi > 45 detik (menolak preview 30 detik)
                    if (durationMs <= MIN_TRACK_DURATION_MS) continue

                    val id = item.optString("id", "")
                    val rawTitle = item.optString("title", "")
                    if (rawTitle.isBlank()) continue

                    val userObj = item.optJSONObject("user")
                    val artist = userObj?.optString("username", "Artis") ?: "Artis"
                    val artwork = item.optString("artwork_url", "")

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

                    if (streamEndpoint.isNotBlank()) {
                        val directUrl = resolveSoundCloudStream(streamEndpoint, SOUNDCLOUD_CLIENT_ID)
                        if (directUrl.isNotBlank()) {
                            list.add(
                                Track(
                                    id = "sc_$id",
                                    isrc = null,
                                    title = cleanHtmlEntities(rawTitle),
                                    artist = cleanHtmlEntities(artist),
                                    album = "SoundCloud Full",
                                    durationMs = durationMs,
                                    coverUrl = artwork.ifBlank { null },
                                    audioUrl = directUrl,
                                    platformIds = mapOf(
                                        MusicSource.YOUTUBE_MUSIC to id,
                                        MusicSource.SPOTIFY to id
                                    )
                                )
                            )
                        }
                    }
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
