package com.stridebeat.app.util

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Utilitas untuk menghasilkan Code Verifier dan Code Challenge (PKCE RFC 7636).
 * Diperlukan untuk OAuth 2.0 Strava & Spotify tanpa membocorkan secret di client side.
 */
object PkceHelper {

    fun generateCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val code = ByteArray(64)
        secureRandom.nextBytes(code)
        return Base64.encodeToString(code, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun generateCodeChallenge(codeVerifier: String): String {
        val bytes = codeVerifier.toByteArray(Charsets.US_ASCII)
        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(bytes, 0, bytes.size)
        val digest = messageDigest.digest()
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
