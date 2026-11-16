package com.stridebeat.app.data.remote.strava

import android.net.Uri
import com.stridebeat.app.BuildConfig
import com.stridebeat.app.data.local.SecureTokenStorage
import com.stridebeat.app.util.PkceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

class StravaTokenExpiredException(message: String) : Exception(message)

@Singleton
class StravaAuthManager @Inject constructor(
    private val tokenStorage: SecureTokenStorage
) {
    private val refreshMutex = Mutex()

    fun isConfigured(): Boolean {
        return BuildConfig.STRAVA_CLIENT_ID.isNotBlank() && BuildConfig.STRAVA_CLIENT_SECRET.isNotBlank()
    }

    fun isClientIdPresent(): Boolean = BuildConfig.STRAVA_CLIENT_ID.isNotBlank()

    /**
     * Membangun URL otorisasi Strava OAuth 2.0 + PKCE.
     * Scope minimal sesuai PRD §6.1: read,activity:read_all,activity:write
     */
    fun createAuthorizationUrl(): String? {
        val clientId = BuildConfig.STRAVA_CLIENT_ID
        if (clientId.isBlank()) return null

        val verifier = PkceHelper.generateCodeVerifier()
        val challenge = PkceHelper.generateCodeChallenge(verifier)
        tokenStorage.saveCodeVerifier("strava", verifier)

        val redirectUri = "stridebeat://strava-callback"
        val scope = "read,activity:read_all,activity:write"

        return Uri.parse("https://www.strava.com/oauth/mobile/authorize")
            .buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("approval_prompt", "auto")
            .appendQueryParameter("scope", scope)
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .build()
            .toString()
    }

    /**
     * Menukar Authorization Code dari Strava dengan Access Token & Refresh Token.
     * Token disimpan terenkripsi AES-256 GCM dan TIDAK PERNAH dicetak ke log.
     */
    suspend fun exchangeCodeForTokens(code: String): Result<Pair<String, Long?>> = withContext(Dispatchers.IO) {
        try {
            val clientId = BuildConfig.STRAVA_CLIENT_ID
            val clientSecret = BuildConfig.STRAVA_CLIENT_SECRET
            if (clientId.isBlank() || clientSecret.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("STRAVA_CLIENT_ID / STRAVA_CLIENT_SECRET belum dikonfigurasi di build ini.")
                )
            }

            val verifier = tokenStorage.getAndClearCodeVerifier("strava") ?: ""

            val url = URL("https://www.strava.com/oauth/token")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connectTimeout = 15000
                readTimeout = 15000
            }

            val bodyBuilder = StringBuilder()
                .append("client_id=").append(Uri.encode(clientId))
                .append("&client_secret=").append(Uri.encode(clientSecret))
                .append("&code=").append(Uri.encode(code))
                .append("&grant_type=authorization_code")

            if (verifier.isNotBlank()) {
                bodyBuilder.append("&code_verifier=").append(Uri.encode(verifier))
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(bodyBuilder.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            val responseText = if (responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                val errorText = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                return@withContext Result.failure(
                    Exception(parseStravaAuthError(responseCode, errorText))
                )
            }

            val json = JSONObject(responseText)
            val accessToken = json.getString("access_token")
            val refreshToken = json.getString("refresh_token")
            val expiresAt = json.optLong("expires_at", System.currentTimeMillis() / 1000 + 21600) * 1000L

            var athleteName = "Atlet Strava"
            var athleteId: Long? = null
            if (json.has("athlete")) {
                val athlete = json.getJSONObject("athlete")
                val idVal = athlete.optLong("id", -1L)
                if (idVal > 0L) athleteId = idVal
                val firstname = athlete.optString("firstname", "")
                val lastname = athlete.optString("lastname", "")
                val username = athlete.optString("username", "")
                athleteName = "$firstname $lastname".trim()
                    .ifBlank { username }
                    .ifBlank { "Atlet Strava" }
            }

            tokenStorage.saveStravaTokens(
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresAt = expiresAt,
                athleteName = athleteName,
                athleteId = athleteId
            )
            Result.success(athleteName to athleteId)
        } catch (e: IOException) {
            Result.failure(IOException("Tidak bisa terhubung ke Strava. Periksa koneksi internetmu.", e))
        } catch (e: Exception) {
            Result.failure(Exception("Gagal menghubungkan akun Strava."))
        }
    }

    /**
     * Mengambil access token Strava yang masih valid.
     * Jika sudah expired (atau < 60 detik lagi), otomatis refresh token.
     * Jika refresh token invalid/revoked -> kembalikan StravaTokenExpiredException.
     */
    suspend fun getValidAccessToken(): Result<String> = refreshMutex.withLock {
        val currentToken = tokenStorage.getStravaAccessToken()
        val refreshToken = tokenStorage.getStravaRefreshToken()
        val expiresAt = tokenStorage.getStravaExpiresAt()

        if (currentToken.isNullOrBlank()) {
            return@withLock Result.failure(
                StravaTokenExpiredException("Akun Strava belum terhubung. Silakan hubungkan di Pengaturan.")
            )
        }

        val needsRefresh = expiresAt <= 0L || System.currentTimeMillis() >= (expiresAt - 60_000L)
        if (!needsRefresh) {
            return@withLock Result.success(currentToken)
        }

        if (refreshToken.isNullOrBlank()) {
            return@withLock Result.failure(
                StravaTokenExpiredException("Sesi Strava kedaluwarsa. Silakan login ulang.")
            )
        }

        return@withLock refreshAccessTokenLocked(refreshToken)
    }

    suspend fun refreshAccessToken(): Result<String> = refreshMutex.withLock {
        val refreshToken = tokenStorage.getStravaRefreshToken()
            ?: return@withLock Result.failure(
                StravaTokenExpiredException("Sesi Strava kedaluwarsa. Silakan login ulang.")
            )
        return@withLock refreshAccessTokenLocked(refreshToken)
    }

    private suspend fun refreshAccessTokenLocked(refreshToken: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val clientId = BuildConfig.STRAVA_CLIENT_ID
            val clientSecret = BuildConfig.STRAVA_CLIENT_SECRET
            if (clientId.isBlank() || clientSecret.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("Konfigurasi Strava belum lengkap pada build ini.")
                )
            }

            val url = URL("https://www.strava.com/oauth/token")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connectTimeout = 15000
                readTimeout = 15000
            }

            val body = StringBuilder()
                .append("client_id=").append(Uri.encode(clientId))
                .append("&client_secret=").append(Uri.encode(clientSecret))
                .append("&grant_type=refresh_token")
                .append("&refresh_token=").append(Uri.encode(refreshToken))
                .toString()

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(body)
                writer.flush()
            }

            val responseCode = conn.responseCode
            val responseText = if (responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                val errBody = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                if (responseCode == 400 || responseCode == 401) {
                    // Refresh ditolak/dicabut -> stop tanpa loop retry, minta login ulang
                    tokenStorage.clearStravaTokens()
                    return@withContext Result.failure(
                        StravaTokenExpiredException("Sesi Strava berakhir atau dicabut. Silakan login ulang.")
                    )
                }
                return@withContext Result.failure(Exception("Gagal memperbarui sesi Strava (HTTP $responseCode). $errBody"))
            }

            val json = JSONObject(responseText)
            val newAccess = json.getString("access_token")
            val newRefresh = json.optString("refresh_token", refreshToken)
            val newExpiresAt = json.optLong("expires_at", System.currentTimeMillis() / 1000 + 21600) * 1000L

            tokenStorage.saveStravaTokens(
                accessToken = newAccess,
                refreshToken = newRefresh,
                expiresAt = newExpiresAt,
                athleteName = tokenStorage.getStravaAthleteName(),
                athleteId = tokenStorage.getStravaAthleteId()
            )
            Result.success(newAccess)
        } catch (e: IOException) {
            Result.failure(IOException("Tidak ada koneksi internet saat memperbarui token Strava.", e))
        } catch (e: Exception) {
            Result.failure(Exception("Gagal memperbarui sesi Strava."))
        }
    }

    /**
     * Membaca profil atlet (GET /api/v3/athlete) untuk memastikan profil sinkron.
     */
    suspend fun fetchAthleteProfile(): Result<Pair<String, Long?>> = withContext(Dispatchers.IO) {
        val tokenResult = getValidAccessToken()
        val token = tokenResult.getOrElse { return@withContext Result.failure(it) }

        try {
            val conn = (URL("https://www.strava.com/api/v3/athlete").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $token")
                connectTimeout = 15000
                readTimeout = 15000
            }
            val code = conn.responseCode
            if (code == 401) {
                return@withContext Result.failure(
                    StravaTokenExpiredException("Token Strava tidak valid. Silakan login ulang.")
                )
            }
            if (code !in 200..299) {
                return@withContext Result.failure(Exception("Gagal membaca profil Strava (HTTP $code)."))
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val id = json.optLong("id", -1L).takeIf { it > 0L }
            val first = json.optString("firstname", "")
            val last = json.optString("lastname", "")
            val username = json.optString("username", "")
            val name = "$first $last".trim().ifBlank { username }.ifBlank { "Atlet Strava" }
            Result.success(name to id)
        } catch (e: IOException) {
            Result.failure(IOException("Koneksi gagal saat membaca profil Strava.", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseStravaAuthError(code: Int, errorJson: String): String {
        val msg = try {
            JSONObject(errorJson).optString("message", "")
        } catch (_: Exception) {
            ""
        }
        return when (code) {
            400, 401 -> "Otorisasi Strava ditolak atau kode kedaluwarsa. Silakan coba login lagi."
            403 -> "Aplikasi Strava belum memiliki izin yang sesuai ($msg)."
            429 -> "Batas permintaan Strava tercapai. Tunggu beberapa menit lalu coba lagi."
            else -> "Strava API error ($code). ${msg.ifBlank { "Silakan coba lagi." }}"
        }
    }
}
