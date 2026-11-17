package com.stridebeat.app.data.remote.youtubemusic

import android.net.Uri
import com.stridebeat.app.BuildConfig
import com.stridebeat.app.data.local.SecureTokenStorage
import com.stridebeat.app.util.PkceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manajer otentikasi opsional YouTube Music berbasis Google OAuth 2.0 PKCE.
 *
 * CATATAN ARSITEKTUR & KEPUTUSAN PEMILIK (M6 Celah 1 — Opsi A):
 * Alur login Google OAuth dinonaktifkan dari UI (SettingsScreen & AuthViewModel) karena:
 * 1. InnerTube API web-client (/search & /player) tidak mengonsumsi token Bearer Google Data API v3 tanpa cookie sesi browser.
 * 2. YouTube Music di StrideBeat sepenuhnya difungsikan dalam "Mode Pencarian Publik (Beta)" zero-config tanpa meminta kredensial akun pengguna.
 * 3. Kelas ini dipertahankan secara internal tanpa tombol semu/mati suri di UI, dan tidak pernah meminta form password Google.
 */
@Singleton
class YouTubeMusicAuthManager @Inject constructor(
    private val tokenStorage: SecureTokenStorage
) {
    fun isConfigured(): Boolean {
        return BuildConfig.YOUTUBE_MUSIC_OAUTH_CLIENT_ID.isNotBlank()
    }

    fun createAuthorizationUrl(): String? {
        val clientId = BuildConfig.YOUTUBE_MUSIC_OAUTH_CLIENT_ID
        if (clientId.isBlank()) return null

        val verifier = PkceHelper.generateCodeVerifier()
        val challenge = PkceHelper.generateCodeChallenge(verifier)
        tokenStorage.saveCodeVerifier("ytmusic", verifier)

        val redirectUri = "stridebeat://ytmusic-callback"
        val scope = "https://www.googleapis.com/auth/youtube.readonly"

        return Uri.parse("https://accounts.google.com/o/oauth2/v2/auth")
            .buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("scope", scope)
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("access_type", "offline")
            .appendQueryParameter("prompt", "consent")
            .build()
            .toString()
    }

    suspend fun handleCallback(code: String): Result<String> = withContext(Dispatchers.IO) {
        val clientId = BuildConfig.YOUTUBE_MUSIC_OAUTH_CLIENT_ID
        if (clientId.isBlank()) {
            return@withContext Result.failure(Exception("YouTube Music OAuth Client ID belum dikonfigurasi"))
        }

        val verifier = tokenStorage.getAndClearCodeVerifier("ytmusic")
        if (verifier.isNullOrBlank()) {
            return@withContext Result.failure(Exception("Kode verifier PKCE tidak ditemukan"))
        }

        try {
            val endpoint = URL("https://oauth2.googleapis.com/token")
            val connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10000
                readTimeout = 10000
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }

            val redirectUri = "stridebeat://ytmusic-callback"
            val postData = "client_id=" + URLEncoder.encode(clientId, "UTF-8") +
                "&grant_type=authorization_code" +
                "&code=" + URLEncoder.encode(code, "UTF-8") +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, "UTF-8") +
                "&code_verifier=" + URLEncoder.encode(verifier, "UTF-8")

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(postData)
                writer.flush()
            }

            if (connection.responseCode == 200) {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val accessToken = json.getString("access_token")
                val userName = fetchUserName(accessToken) ?: "Akun Google"
                tokenStorage.saveYouTubeMusicSession(userName, accessToken)
                Result.success(userName)
            } else {
                val err = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Result.failure(Exception("Gagal menukar token OAuth Google ($err)"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchUserName(accessToken: String): String? {
        return try {
            val url = URL("https://www.googleapis.com/oauth2/v3/userinfo")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Authorization", "Bearer $accessToken")
            }
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                json.optString("name", json.optString("email", "Pengguna YouTube Music"))
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun disconnect() {
        tokenStorage.clearYouTubeMusicSession()
    }
}
