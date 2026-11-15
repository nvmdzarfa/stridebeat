package com.stridebeat.app.data.remote.spotify

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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpotifyAuthManager @Inject constructor(
    private val tokenStorage: SecureTokenStorage
) {
    fun isConfigured(): Boolean {
        return BuildConfig.SPOTIFY_CLIENT_ID.isNotBlank()
    }

    /**
     * Membangun URL otorisasi Spotify OAuth 2.0 PKCE.
     */
    fun createAuthorizationUrl(): String? {
        val clientId = BuildConfig.SPOTIFY_CLIENT_ID
        if (clientId.isBlank()) return null

        val verifier = PkceHelper.generateCodeVerifier()
        val challenge = PkceHelper.generateCodeChallenge(verifier)
        tokenStorage.saveCodeVerifier("spotify", verifier)

        val redirectUri = BuildConfig.SPOTIFY_REDIRECT_URI.ifBlank { "stridebeat://spotify-callback" }
        val scopes = listOf(
            "user-read-playback-state",
            "user-modify-playback-state",
            "user-read-currently-playing",
            "playlist-read-private",
            "user-library-read"
        ).joinToString(" ")

        return Uri.parse("https://accounts.spotify.com/authorize")
            .buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("scope", scopes)
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .build()
            .toString()
    }

    /**
     * Menukar Authorization Code dari Spotify dengan Token.
     */
    suspend fun exchangeCodeForTokens(code: String): Result<Pair<String, Boolean>> = withContext(Dispatchers.IO) {
        try {
            val clientId = BuildConfig.SPOTIFY_CLIENT_ID
            val verifier = tokenStorage.getAndClearCodeVerifier("spotify") ?: ""
            val redirectUri = BuildConfig.SPOTIFY_REDIRECT_URI.ifBlank { "stridebeat://spotify-callback" }

            val url = URL("https://accounts.spotify.com/api/token")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connectTimeout = 15000
                readTimeout = 15000
            }

            val body = "client_id=${Uri.encode(clientId)}" +
                    "&grant_type=authorization_code" +
                    "&code=${Uri.encode(code)}" +
                    "&redirect_uri=${Uri.encode(redirectUri)}" +
                    "&code_verifier=${Uri.encode(verifier)}"

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(body)
                writer.flush()
            }

            val responseCode = conn.responseCode
            val responseText = if (responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                val errorStream = conn.errorStream?.bufferedReader()?.use { it.readText() }
                return@withContext Result.failure(Exception("Spotify API Error ($responseCode): $errorStream"))
            }

            val json = JSONObject(responseText)
            val accessToken = json.getString("access_token")
            val refreshToken = json.optString("refresh_token", null)
            val expiresIn = json.optLong("expires_in", 3600)
            val expiresAt = System.currentTimeMillis() + (expiresIn * 1000)

            // Ambil info profil & status Premium dari Spotify Web API
            val profile = fetchUserProfile(accessToken)
            val userName = profile.first
            val isPremium = profile.second

            tokenStorage.saveSpotifyTokens(accessToken, refreshToken, expiresAt, userName, isPremium)
            Result.success(Pair(userName, isPremium))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchUserProfile(accessToken: String): Pair<String, Boolean> {
        return try {
            val url = URL("https://api.spotify.com/v1/me")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $accessToken")
                connectTimeout = 10000
                readTimeout = 10000
            }
            if (conn.responseCode in 200..299) {
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val displayName = json.optString("display_name", "Spotify User")
                val product = json.optString("product", "free")
                val isPremium = product.equals("premium", ignoreCase = true)
                Pair(displayName, isPremium)
            } else {
                Pair("Spotify User", false)
            }
        } catch (_: Exception) {
            Pair("Spotify User", false)
        }
    }
}
