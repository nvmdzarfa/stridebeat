package com.stridebeat.app.domain.model

data class UserProfile(
    val uid: String? = null,
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val isGoogleSignedIn: Boolean = false
) {
    val firstName: String
        get() = displayName.trim().substringBefore(" ").ifBlank { "Pelari" }
}
