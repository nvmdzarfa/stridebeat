package com.stridebeat.app.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Penyimpanan token OAuth terenkripsi menggunakan EncryptedSharedPreferences (AndroidX Security).
 * Sesuai PRD §0 aturan #5 & §6.5:
 * Token OAuth WAJIB disimpan terenkripsi, JANGAN pernah disimpan di SharedPreferences biasa,
 * dan JANGAN pernah di-print/log ke Logcat.
 */
@Singleton
class SecureTokenStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_FILENAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // --- STRAVA TOKENS ---
    fun saveStravaTokens(
        accessToken: String,
        refreshToken: String,
        expiresAt: Long,
        athleteName: String?,
        athleteId: Long? = null
    ) {
        val editor = prefs.edit()
            .putString(KEY_STRAVA_ACCESS_TOKEN, accessToken)
            .putString(KEY_STRAVA_REFRESH_TOKEN, refreshToken)
            .putLong(KEY_STRAVA_EXPIRES_AT, expiresAt)
            .putString(KEY_STRAVA_ATHLETE_NAME, athleteName)
        if (athleteId != null && athleteId > 0L) {
            editor.putLong(KEY_STRAVA_ATHLETE_ID, athleteId)
        }
        editor.apply()
    }

    fun getStravaAccessToken(): String? = prefs.getString(KEY_STRAVA_ACCESS_TOKEN, null)
    fun getStravaRefreshToken(): String? = prefs.getString(KEY_STRAVA_REFRESH_TOKEN, null)
    fun getStravaExpiresAt(): Long = prefs.getLong(KEY_STRAVA_EXPIRES_AT, 0L)
    fun getStravaAthleteName(): String? = prefs.getString(KEY_STRAVA_ATHLETE_NAME, null)
    fun getStravaAthleteId(): Long? {
        val id = prefs.getLong(KEY_STRAVA_ATHLETE_ID, -1L)
        return if (id > 0L) id else null
    }

    fun clearStravaTokens() {
        prefs.edit()
            .remove(KEY_STRAVA_ACCESS_TOKEN)
            .remove(KEY_STRAVA_REFRESH_TOKEN)
            .remove(KEY_STRAVA_EXPIRES_AT)
            .remove(KEY_STRAVA_ATHLETE_NAME)
            .remove(KEY_STRAVA_ATHLETE_ID)
            .apply()
    }

    // --- SPOTIFY TOKENS ---
    fun saveSpotifyTokens(accessToken: String, refreshToken: String?, expiresAt: Long, userName: String?, isPremium: Boolean) {
        val editor = prefs.edit()
            .putString(KEY_SPOTIFY_ACCESS_TOKEN, accessToken)
            .putLong(KEY_SPOTIFY_EXPIRES_AT, expiresAt)
            .putString(KEY_SPOTIFY_USER_NAME, userName)
            .putBoolean(KEY_SPOTIFY_IS_PREMIUM, isPremium)
        if (refreshToken != null) {
            editor.putString(KEY_SPOTIFY_REFRESH_TOKEN, refreshToken)
        }
        editor.apply()
    }

    fun getSpotifyAccessToken(): String? = prefs.getString(KEY_SPOTIFY_ACCESS_TOKEN, null)
    fun getSpotifyRefreshToken(): String? = prefs.getString(KEY_SPOTIFY_REFRESH_TOKEN, null)
    fun getSpotifyExpiresAt(): Long = prefs.getLong(KEY_SPOTIFY_EXPIRES_AT, 0L)
    fun getSpotifyUserName(): String? = prefs.getString(KEY_SPOTIFY_USER_NAME, null)
    fun isSpotifyPremium(): Boolean = prefs.getBoolean(KEY_SPOTIFY_IS_PREMIUM, false)

    fun clearSpotifyTokens() {
        prefs.edit()
            .remove(KEY_SPOTIFY_ACCESS_TOKEN)
            .remove(KEY_SPOTIFY_REFRESH_TOKEN)
            .remove(KEY_SPOTIFY_EXPIRES_AT)
            .remove(KEY_SPOTIFY_USER_NAME)
            .remove(KEY_SPOTIFY_IS_PREMIUM)
            .apply()
    }

    // --- YOUTUBE MUSIC (SESSION / TOKEN) ---
    fun saveYouTubeMusicSession(userName: String?, sessionToken: String?) {
        prefs.edit()
            .putString(KEY_YTM_USER_NAME, userName)
            .putString(KEY_YTM_SESSION_TOKEN, sessionToken)
            .apply()
    }

    fun getYouTubeMusicUserName(): String? = prefs.getString(KEY_YTM_USER_NAME, null)
    fun getYouTubeMusicSessionToken(): String? = prefs.getString(KEY_YTM_SESSION_TOKEN, null)

    fun clearYouTubeMusicSession() {
        prefs.edit()
            .remove(KEY_YTM_USER_NAME)
            .remove(KEY_YTM_SESSION_TOKEN)
            .apply()
    }

    // PKCE Temporary Verifiers
    fun saveCodeVerifier(key: String, verifier: String) {
        prefs.edit().putString(KEY_PKCE_PREFIX + key, verifier).apply()
    }

    fun getAndClearCodeVerifier(key: String): String? {
        val verifier = prefs.getString(KEY_PKCE_PREFIX + key, null)
        prefs.edit().remove(KEY_PKCE_PREFIX + key).apply()
        return verifier
    }

    companion object {
        private const val PREFS_FILENAME = "stridebeat_secure_tokens"

        private const val KEY_STRAVA_ACCESS_TOKEN = "strava_access_token"
        private const val KEY_STRAVA_REFRESH_TOKEN = "strava_refresh_token"
        private const val KEY_STRAVA_EXPIRES_AT = "strava_expires_at"
        private const val KEY_STRAVA_ATHLETE_NAME = "strava_athlete_name"
        private const val KEY_STRAVA_ATHLETE_ID = "strava_athlete_id"

        private const val KEY_SPOTIFY_ACCESS_TOKEN = "spotify_access_token"
        private const val KEY_SPOTIFY_REFRESH_TOKEN = "spotify_refresh_token"
        private const val KEY_SPOTIFY_EXPIRES_AT = "spotify_expires_at"
        private const val KEY_SPOTIFY_USER_NAME = "spotify_user_name"
        private const val KEY_SPOTIFY_IS_PREMIUM = "spotify_is_premium"

        private const val KEY_YTM_USER_NAME = "ytm_user_name"
        private const val KEY_YTM_SESSION_TOKEN = "ytm_session_token"

        private const val KEY_PKCE_PREFIX = "pkce_verifier_"
    }
}
