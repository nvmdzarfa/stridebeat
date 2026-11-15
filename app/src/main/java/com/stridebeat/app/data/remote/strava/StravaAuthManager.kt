package com.stridebeat.app.data.remote.strava

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
class StravaAuthManager @Inject constructor(
    private val tokenStorage: SecureTokenStorage
) {
    fun isConfigured(): Boolean {
        return BuildConfig.STRAVA_CLIENT_ID.isNotBlank()
    }

    /**
     * Membangun URL otorisasi Strava OAuth 2.0 dengan PKCE.
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
     */
    suspend fun exchangeCodeForTokens(code: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val clientId = BuildConfig.STRAVA_CLIENT_ID
            val clientSecret = BuildConfig.STRAVA_CLIENT_SECRET
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
                val errorStream = conn.errorStream?.bufferedReader()?.use { it.readText() }
                return@withContext Result.failure(Exception("Strava API Error ($responseCode): $errorStream"))
            }

            val json = JSONObject(responseText)
            val accessToken = json.getString("access_token")
            val refreshToken = json.getString("refresh_token")
            val expiresAt = json.optLong("expires_at", System.currentTimeMillis() / 1000 + 21600) * 1000

            var athleteName = "Atlet Strava"
            if (json.has("athlete")) {
                val athlete = json.getJSONObject("athlete")
                val firstname = athlete.optString("firstname", "")
                val lastname = athlete.optString("lastname", "")
                athleteName = "$firstname $lastname".trim().ifBlank { "Atlet Strava" }
            }

            tokenStorage.saveStravaTokens(accessToken, refreshToken, expiresAt, athleteName)
            Result.success(athleteName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
