package com.stridebeat.app.data.remote.youtubemusic

import com.stridebeat.app.domain.model.MusicSource
import com.stridebeat.app.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

sealed interface StreamResult {
    data class Playable(val audioUrl: String) : StreamResult
    data class Unavailable(val reason: String, val externalUri: String) : StreamResult
}

/**
 * Klien YouTube Music berbasis InnerTube API tanpa login & tanpa bypass DRM.
 * Mendukung pencarian katalog musik publik dan resolusi stream audio langsung on-demand.
 */
@Singleton
class YouTubeMusicClient @Inject constructor() {

    suspend fun searchTracks(query: String): Result<List<Track>> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext Result.success(emptyList())

        try {
            val endpoint = URL("https://music.youtube.com/youtubei/v1/search")
            val connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 7000
                readTimeout = 7000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                setRequestProperty("Referer", "https://music.youtube.com/")
            }

            val requestBody = JSONObject().apply {
                val context = JSONObject().apply {
                    val client = JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20230628.01.00")
                        put("hl", "id")
                        put("gl", "ID")
                    }
                    put("client", client)
                }
                put("context", context)
                put("query", trimmed)
                put("params", "EgWKAQIIAWoMEAMQBBAJEAoQBRAV") // Filter kategori lagu
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val tracks = parseSearchResponse(responseText)
                Result.success(tracks)
            } else {
                Result.failure(Exception("YouTube Music search returned HTTP ${connection.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resolveAudioStreamUrl(videoId: String): StreamResult = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) {
            return@withContext StreamResult.Unavailable("Video ID tidak valid", "https://music.youtube.com/")
        }

        // Coba client ANDROID_MUSIC terlebih dahulu
        val androidResult = requestPlayerEndpoint(videoId, clientName = "ANDROID_MUSIC", clientVersion = "6.20.51")
        if (androidResult is StreamResult.Playable) {
            return@withContext androidResult
        }

        // Fallback ke client IOS
        val iosResult = requestPlayerEndpoint(videoId, clientName = "IOS", clientVersion = "19.29.1")
        if (iosResult is StreamResult.Playable) {
            return@withContext iosResult
        }

        // Jika keduanya tidak playable, kembalikan alasan penolakan
        return@withContext androidResult
    }

    private fun requestPlayerEndpoint(videoId: String, clientName: String, clientVersion: String): StreamResult {
        return try {
            val endpoint = URL("https://music.youtube.com/youtubei/v1/player")
            val connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 7000
                readTimeout = 7000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }

            val requestBody = JSONObject().apply {
                val context = JSONObject().apply {
                    val client = JSONObject().apply {
                        put("clientName", clientName)
                        put("clientVersion", clientVersion)
                        put("hl", "id")
                        put("gl", "ID")
                    }
                    put("client", client)
                }
                put("context", context)
                put("videoId", videoId)
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            val code = connection.responseCode
            if (code == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                parsePlayerAudioUrl(responseText, videoId)
            } else {
                StreamResult.Unavailable(
                    reason = "YouTube Music player menolak permintaan (HTTP $code)",
                    externalUri = "https://music.youtube.com/watch?v=$videoId"
                )
            }
        } catch (e: Exception) {
            StreamResult.Unavailable(
                reason = "Gagal menghubungi layanan audio YouTube Music (${e.message ?: "koneksi terputus"})",
                externalUri = "https://music.youtube.com/watch?v=$videoId"
            )
        }
    }

    companion object {

        /**
         * Parsing response JSON pencarian InnerTube WEB_REMIX menjadi list Track netral platform.
         */
        fun parseSearchResponse(json: String): List<Track> {
            val tracks = mutableListOf<Track>()
            try {
                val root = JSONObject(json)
                findMusicResponsiveItems(root, tracks)
            } catch (_: Exception) {
                // Jangan crash bila struktur JSON berubah
            }
            return tracks
        }

        private fun findMusicResponsiveItems(node: Any?, outTracks: MutableList<Track>) {
            when (node) {
                is JSONObject -> {
                    if (node.has("musicResponsiveListItemRenderer")) {
                        val item = node.getJSONObject("musicResponsiveListItemRenderer")
                        parseSingleTrackItem(item)?.let { outTracks.add(it) }
                    } else {
                        val keys = node.keys()
                        while (keys.hasNext()) {
                            findMusicResponsiveItems(node.opt(keys.next()), outTracks)
                        }
                    }
                }
                is JSONArray -> {
                    for (i in 0 until node.length()) {
                        findMusicResponsiveItems(node.opt(i), outTracks)
                    }
                }
            }
        }

        private fun parseSingleTrackItem(item: JSONObject): Track? {
            try {
                val flexColumns = item.optJSONArray("flexColumns") ?: return null
                if (flexColumns.length() < 2) return null

                // Kolom 1: Title & VideoId
                val col0 = flexColumns.optJSONObject(0)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                val text0 = col0?.optJSONObject("text")
                val runs0 = text0?.optJSONArray("runs") ?: return null
                if (runs0.length() == 0) return null

                val titleRun = runs0.optJSONObject(0)
                val title = titleRun?.optString("text", "") ?: ""
                if (title.isBlank()) return null

                var videoId = ""
                val nav = titleRun?.optJSONObject("navigationEndpoint")
                val watch = nav?.optJSONObject("watchEndpoint")
                if (watch != null) {
                    videoId = watch.optString("videoId", "")
                }

                // Kolom 2: Artis, Album, Durasi
                val col1 = flexColumns.optJSONObject(1)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                val text1 = col1?.optJSONObject("text")
                val runs1 = text1?.optJSONArray("runs")

                var artist = "YouTube Music"
                var album = "YouTube Music"
                var durationMs = 0L

                if (runs1 != null) {
                    val meaningfulRuns = mutableListOf<String>()
                    for (i in 0 until runs1.length()) {
                        val runObj = runs1.optJSONObject(i) ?: continue
                        val txt = runObj.optString("text", "").trim()
                        if (txt.isNotEmpty() && txt != "•") {
                            meaningfulRuns.add(txt)
                        }
                    }

                    if (meaningfulRuns.isNotEmpty()) {
                        artist = meaningfulRuns[0]
                    }
                    if (meaningfulRuns.size > 2) {
                        album = meaningfulRuns[1]
                        durationMs = parseDurationTextToMs(meaningfulRuns.last())
                    } else if (meaningfulRuns.size == 2) {
                        val second = meaningfulRuns[1]
                        if (second.contains(":")) {
                            durationMs = parseDurationTextToMs(second)
                        } else {
                            album = second
                        }
                    }
                }

                // Thumbnail
                val thumbRenderer = item.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer")
                val thumbArray = thumbRenderer?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                var coverUrl: String? = null
                if (thumbArray != null && thumbArray.length() > 0) {
                    coverUrl = thumbArray.optJSONObject(thumbArray.length() - 1)?.optString("url", null)
                }

                if (videoId.isBlank()) {
                    // Coba ekstrak videoId dari overlay jika navigasi utama tidak ada
                    val overlay = item.optJSONObject("overlay")
                    val playBtn = overlay?.optJSONObject("musicItemThumbnailOverlayRenderer")
                        ?.optJSONObject("content")?.optJSONObject("musicPlayButtonRenderer")
                    val watchEp = playBtn?.optJSONObject("playNavigationEndpoint")?.optJSONObject("watchEndpoint")
                    videoId = watchEp?.optString("videoId", "") ?: ""
                }

                if (videoId.isBlank()) return null

                return Track(
                    id = "ytm_$videoId",
                    isrc = null,
                    title = title,
                    artist = artist,
                    album = album,
                    durationMs = if (durationMs > 0L) durationMs else 180000L,
                    coverUrl = coverUrl,
                    audioUrl = null,
                    platformIds = mapOf(MusicSource.YOUTUBE_MUSIC to videoId)
                )
            } catch (_: Exception) {
                return null
            }
        }

        private fun parseDurationTextToMs(text: String): Long {
            return try {
                val parts = text.split(":")
                when (parts.size) {
                    2 -> {
                        val mins = parts[0].trim().toLong()
                        val secs = parts[1].trim().toLong()
                        (mins * 60 + secs) * 1000L
                    }
                    3 -> {
                        val hours = parts[0].trim().toLong()
                        val mins = parts[1].trim().toLong()
                        val secs = parts[2].trim().toLong()
                        ((hours * 3600) + (mins * 60) + secs) * 1000L
                    }
                    else -> 0L
                }
            } catch (_: Exception) {
                0L
            }
        }

        /**
         * Parsing response JSON player InnerTube untuk menemukan format audio langsung.
         */
        fun parsePlayerAudioUrl(json: String, videoId: String): StreamResult {
            val externalUrl = "https://music.youtube.com/watch?v=$videoId"
            return try {
                val root = JSONObject(json)
                val playability = root.optJSONObject("playabilityStatus")
                val status = playability?.optString("status", "") ?: ""

                if (status != "OK") {
                    val reason = playability?.optString("reason", "") ?: "Audio tidak tersedia langsung di YouTube Music"
                    val friendlyReason = if (reason.isNotBlank()) "Lagu ini tidak tersedia di YouTube Music ($reason)" else "Audio tidak tersedia di YouTube Music"
                    return StreamResult.Unavailable(friendlyReason, externalUrl)
                }

                val streamingData = root.optJSONObject("streamingData")
                    ?: return StreamResult.Unavailable("Data streaming audio tidak ditemukan", externalUrl)

                val formats = streamingData.optJSONArray("adaptiveFormats")
                    ?: return StreamResult.Unavailable("Format audio adaptif tidak ditemukan", externalUrl)

                var hasCipher = false
                var bestAudioUrl: String? = null
                var maxBitrate = 0

                for (i in 0 until formats.length()) {
                    val fmt = formats.optJSONObject(i) ?: continue
                    val mimeType = fmt.optString("mimeType", "")
                    if (!mimeType.startsWith("audio/")) continue

                    val directUrl = fmt.optString("url", "")
                    val cipher = fmt.optString("signatureCipher", "").ifBlank { fmt.optString("cipher", "") }

                    if (directUrl.isNotBlank()) {
                        val bitrate = fmt.optInt("bitrate", 0)
                        if (bitrate > maxBitrate) {
                            maxBitrate = bitrate
                            bestAudioUrl = directUrl
                        }
                    } else if (cipher.isNotBlank()) {
                        hasCipher = true
                    }
                }

                if (!bestAudioUrl.isNullOrBlank()) {
                    StreamResult.Playable(bestAudioUrl)
                } else if (hasCipher) {
                    StreamResult.Unavailable(
                        reason = "Audio YouTube Music dilindungi cipher langsung",
                        externalUri = externalUrl
                    )
                } else {
                    StreamResult.Unavailable(
                        reason = "Tidak ada stream audio yang kompatibel",
                        externalUri = externalUrl
                    )
                }
            } catch (_: Exception) {
                StreamResult.Unavailable(
                    reason = "Gagal memproses data audio YouTube Music",
                    externalUri = externalUrl
                )
            }
        }
    }
}
