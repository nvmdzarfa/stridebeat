package com.stridebeat.app.data.remote.strava

import com.stridebeat.app.domain.model.StravaUploadStatus
import com.stridebeat.app.domain.model.WorkoutSession
import com.stridebeat.app.util.GpxExporter
import com.stridebeat.app.util.StravaFormatter
import com.stridebeat.app.util.TrackingUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

class StravaRateLimitException(message: String) : Exception(message)
class StravaTransientException(message: String) : Exception(message)

/**
 * Client resmi Strava API v3 (tanpa menyimpan/mencetak token ke log).
 *
 * Keputusan jalur upload utama (PRD §6.1):
 * - Jika sesi memiliki >= 2 titik GPS (`routePoints`), gunakan `POST /api/v3/uploads`
 *   dengan file GPX 1.1 agar rute peta GPS, elevasi/waktu, jarak, dan pace ikut terunggah
 *   ke Strava (karena `POST /api/v3/activities` tidak menerima koordinat rute GPS).
 * - Jika sesi tidak memiliki rute GPS (< 2 titik), gunakan `POST /api/v3/activities`
 *   agar metrik jarak/durasi/daftar lagu tetap dapat dicatat di Strava.
 */
@Singleton
class StravaApiClient @Inject constructor() {

    suspend fun uploadSession(
        accessToken: String,
        session: WorkoutSession,
        activityName: String,
        description: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (session.routePoints.size >= 2) {
            uploadViaGpx(accessToken, session, activityName, description)
        } else {
            createActivityWithoutGpx(accessToken, session, activityName, description)
        }
    }

    private suspend fun uploadViaGpx(
        accessToken: String,
        session: WorkoutSession,
        activityName: String,
        description: String
    ): Result<String> {
        val boundary = "StrideBeatBoundary${System.currentTimeMillis()}"
        val lineEnd = "\r\n"
        val twoHyphens = "--"

        return try {
            val gpxXml = GpxExporter.generateGpxXml(session)
            val baos = ByteArrayOutputStream()
            DataOutputStream(baos).use { out ->
                fun writeTextPart(fieldName: String, value: String) {
                    out.writeBytes("$twoHyphens$boundary$lineEnd")
                    out.writeBytes("Content-Disposition: form-data; name=\"$fieldName\"$lineEnd")
                    out.writeBytes("Content-Type: text/plain; charset=UTF-8$lineEnd$lineEnd")
                    out.write(value.toByteArray(StandardCharsets.UTF_8))
                    out.writeBytes(lineEnd)
                }

                writeTextPart("data_type", "gpx")
                writeTextPart("name", activityName)
                writeTextPart("description", description)
                writeTextPart("sport_type", StravaFormatter.toStravaSportType(session.type))
                writeTextPart("external_id", "stridebeat_${session.id}")

                // File GPX part
                out.writeBytes("$twoHyphens$boundary$lineEnd")
                out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"stridebeat_${session.id}.gpx\"$lineEnd")
                out.writeBytes("Content-Type: application/gpx+xml$lineEnd$lineEnd")
                out.write(gpxXml.toByteArray(StandardCharsets.UTF_8))
                out.writeBytes(lineEnd)

                out.writeBytes("$twoHyphens$boundary$twoHyphens$lineEnd")
                out.flush()
            }

            val payload = baos.toByteArray()
            val conn = (URL("$BASE_URL/uploads").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                setFixedLengthStreamingMode(payload.size)
                connectTimeout = 20000
                readTimeout = 25000
            }

            conn.outputStream.use { it.write(payload) }

            val code = conn.responseCode
            val body = readResponseBody(conn, code)

            when {
                code == 401 -> return Result.failure(StravaTokenExpiredException("Token Strava kedaluwarsa. Silakan login ulang."))
                code == 429 -> return Result.failure(StravaRateLimitException("Batas permintaan Strava tercapai. Coba lagi nanti."))
                code in 500..599 -> return Result.failure(StravaTransientException("Server Strava sedang sibuk (HTTP $code)."))
                code !in 200..299 -> {
                    val errMsg = parseErrorMessage(body)
                    extractDuplicateActivityId(errMsg)?.let { existingId ->
                        return Result.success(existingId)
                    }
                    return Result.failure(Exception("Upload Strava gagal ($code): $errMsg"))
                }
            }

            val uploadJson = JSONObject(body)
            val immediateActivityId = uploadJson.optLong("activity_id", 0L)
            if (immediateActivityId > 0L) {
                return Result.success(immediateActivityId.toString())
            }

            val uploadError = uploadJson.optString("error", "").takeIf { it.isNotBlank() && it != "null" }
            if (uploadError != null) {
                extractDuplicateActivityId(uploadError)?.let { existingId ->
                    return Result.success(existingId)
                }
                return Result.failure(Exception("Strava menolak file rute: $uploadError"))
            }

            val uploadId = uploadJson.optLong("id", 0L)
            if (uploadId <= 0L) {
                return Result.failure(Exception("Respons upload Strava tidak valid."))
            }

            // Polling status pemrosesan GPX di Strava
            for (attempt in 1..8) {
                delay(1400L)
                val statusResult = pollUploadStatus(accessToken, uploadId)
                if (statusResult.isSuccess) {
                    val actId = statusResult.getOrNull()
                    if (!actId.isNullOrBlank()) {
                        return Result.success(actId)
                    }
                } else {
                    val ex = statusResult.exceptionOrNull()
                    if (ex is StravaTokenExpiredException || ex is StravaRateLimitException) {
                        return Result.failure(ex)
                    }
                    if (ex !is StravaTransientException) {
                        return Result.failure(ex ?: Exception("Upload Strava gagal."))
                    }
                }
            }

            Result.failure(StravaTransientException("Strava masih memproses rute GPX. Coba cek atau ulangi sebentar lagi."))
        } catch (e: IOException) {
            Result.failure(StravaTransientException("Koneksi internet terputus saat mengunggah ke Strava."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun pollUploadStatus(accessToken: String, uploadId: Long): Result<String?> {
        val conn = (URL("$BASE_URL/uploads/$uploadId").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $accessToken")
            connectTimeout = 15000
            readTimeout = 15000
        }
        val code = conn.responseCode
        val body = readResponseBody(conn, code)

        if (code == 401) return Result.failure(StravaTokenExpiredException("Token Strava kedaluwarsa."))
        if (code == 429) return Result.failure(StravaRateLimitException("Batas permintaan Strava tercapai."))
        if (code in 500..599) return Result.failure(StravaTransientException("Server Strava sibuk ($code)."))
        if (code !in 200..299) return Result.failure(Exception("Gagal mengecek status upload ($code): ${parseErrorMessage(body)}"))

        val json = JSONObject(body)
        val activityId = json.optLong("activity_id", 0L)
        if (activityId > 0L) {
            return Result.success(activityId.toString())
        }
        val error = json.optString("error", "").takeIf { it.isNotBlank() && it != "null" }
        if (error != null) {
            extractDuplicateActivityId(error)?.let { existingId ->
                return Result.success(existingId)
            }
            return Result.failure(Exception("Strava gagal memproses aktivitas: $error"))
        }
        return Result.success(null) // Masih diproses
    }

    private fun createActivityWithoutGpx(
        accessToken: String,
        session: WorkoutSession,
        activityName: String,
        description: String
    ): Result<String> {
        return try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val startDateUtc = isoFormat.format(Date(session.startedAt))
            val elapsedSecs = (session.durationMs / 1000L).coerceAtLeast(1L)

            val payloadObj = JSONObject().apply {
                put("name", activityName)
                put("sport_type", StravaFormatter.toStravaSportType(session.type))
                put("start_date_local", startDateUtc)
                put("elapsed_time", elapsedSecs)
                put("distance", session.distanceMeters.toFloat())
                put("description", description)
            }
            val bytes = payloadObj.toString().toByteArray(StandardCharsets.UTF_8)

            val conn = (URL("$BASE_URL/activities").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setFixedLengthStreamingMode(bytes.size)
                connectTimeout = 15000
                readTimeout = 20000
            }
            conn.outputStream.use { it.write(bytes) }

            val code = conn.responseCode
            val body = readResponseBody(conn, code)

            when {
                code == 401 -> Result.failure(StravaTokenExpiredException("Token Strava kedaluwarsa. Silakan login ulang."))
                code == 429 -> Result.failure(StravaRateLimitException("Batas permintaan Strava tercapai. Coba lagi nanti."))
                code in 500..599 -> Result.failure(StravaTransientException("Server Strava sibuk ($code)."))
                code !in 200..299 -> Result.failure(Exception("Upload Strava gagal ($code): ${parseErrorMessage(body)}"))
                else -> {
                    val json = JSONObject(body)
                    val id = json.optLong("id", 0L)
                    if (id > 0L) Result.success(id.toString())
                    else Result.failure(Exception("ID aktivitas Strava tidak ditemukan pada respons."))
                }
            }
        } catch (e: IOException) {
            Result.failure(StravaTransientException("Koneksi internet terputus saat mengunggah ke Strava."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mengimpor maksimal 30 aktivitas terbaru dari Strava (`GET /api/v3/athlete/activities`).
     */
    suspend fun fetchRecentActivities(
        accessToken: String,
        perPage: Int = 30
    ): Result<List<WorkoutSession>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val conn = (URL("$BASE_URL/athlete/activities?per_page=$perPage&page=1").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $accessToken")
                connectTimeout = 15000
                readTimeout = 20000
            }

            val code = conn.responseCode
            val body = readResponseBody(conn, code)

            when {
                code == 401 -> return@withContext Result.failure(StravaTokenExpiredException("Token Strava kedaluwarsa. Silakan login ulang."))
                code == 429 -> return@withContext Result.failure(StravaRateLimitException("Batas permintaan Strava tercapai. Coba lagi nanti."))
                code !in 200..299 -> return@withContext Result.failure(Exception("Gagal memuat riwayat Strava ($code): ${parseErrorMessage(body)}"))
            }

            val arr = JSONArray(body)
            val sessions = mutableListOf<WorkoutSession>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val actId = obj.optLong("id", 0L)
                if (actId <= 0L) continue

                val name = obj.optString("name", "").takeIf { it.isNotBlank() }
                val sportTypeRaw = obj.optString("sport_type", obj.optString("type", "Run"))
                val workoutType = StravaFormatter.fromStravaSportType(sportTypeRaw)

                val startDateStr = obj.optString("start_date", "")
                val startedAt = parseIsoTimestamp(startDateStr)
                val elapsedSec = obj.optLong("elapsed_time", obj.optLong("moving_time", 0L)).coerceAtLeast(0L)
                val durationMs = elapsedSec * 1000L
                val endedAt = startedAt + durationMs
                val distanceMeters = obj.optDouble("distance", 0.0).coerceAtLeast(0.0)

                val avgSpeed = obj.optDouble("average_speed", 0.0)
                val avgPace = if (avgSpeed > 0.0) {
                    val pace = 1000.0 / (avgSpeed * 60.0)
                    if (pace in 2.0..25.0) pace else TrackingUtils.calculatePaceMinPerKm(durationMs, distanceMeters)
                } else {
                    TrackingUtils.calculatePaceMinPerKm(durationMs, distanceMeters)
                }

                val caloriesRaw = obj.optDouble("calories", Double.NaN)
                val calories = if (!caloriesRaw.isNaN() && caloriesRaw > 0.0) {
                    caloriesRaw
                } else {
                    TrackingUtils.estimateCalories(durationMs, distanceMeters)
                }

                val mapObj = obj.optJSONObject("map")
                val polyline = mapObj?.optString("summary_polyline", "")
                val routePoints = StravaFormatter.decodePolyline(polyline)

                sessions.add(
                    WorkoutSession(
                        id = "strava_$actId",
                        type = workoutType,
                        startedAt = startedAt,
                        endedAt = endedAt,
                        distanceMeters = distanceMeters,
                        durationMs = durationMs,
                        avgPaceMinPerKm = avgPace,
                        caloriesKcal = calories,
                        routePoints = routePoints,
                        musicSource = null,
                        playedTracks = emptyList(),
                        stravaActivityId = actId.toString(),
                        stravaUploadStatus = StravaUploadStatus.UPLOADED,
                        isImportedFromStrava = true,
                        stravaActivityName = name,
                        gpsInterrupted = false
                    )
                )
            }
            Result.success(sessions)
        } catch (e: IOException) {
            Result.failure(IOException("Tidak bisa terhubung ke Strava. Periksa koneksi internetmu.", e))
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Gagal memproses data riwayat Strava."))
        }
    }

    private fun readResponseBody(conn: HttpURLConnection, code: Int): String {
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        return stream?.bufferedReader()?.use { it.readText() } ?: ""
    }

    private fun parseErrorMessage(body: String): String {
        return try {
            val json = JSONObject(body)
            val msg = json.optString("message", "")
            val errors = json.optJSONArray("errors")
            if (errors != null && errors.length() > 0) {
                val first = errors.optJSONObject(0)
                val field = first?.optString("field", "").orEmpty()
                val errCode = first?.optString("code", "").orEmpty()
                if (msg.isNotBlank()) "$msg ($field $errCode)".trim() else "$field $errCode".trim()
            } else {
                msg.ifBlank { "Error tidak diketahui" }
            }
        } catch (_: Exception) {
            body.take(160).ifBlank { "Error jaringan" }
        }
    }

    private fun extractDuplicateActivityId(message: String): String? {
        if (!message.contains("duplicate", ignoreCase = true) && !message.contains("already exists", ignoreCase = true)) {
            return null
        }
        val match = Regex("""(?:activity|activities/)\s*[:#]?\s*(\d{5,})""", RegexOption.IGNORE_CASE).find(message)
        return match?.groupValues?.getOrNull(1)
    }

    private fun parseIsoTimestamp(iso: String): Long {
        if (iso.isBlank()) return System.currentTimeMillis()
        val formats = listOf("yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                sdf.parse(iso)?.time?.let { return it }
            } catch (_: Exception) {
            }
        }
        return System.currentTimeMillis()
    }

    companion object {
        private const val BASE_URL = "https://www.strava.com/api/v3"
    }
}
