package com.stridebeat.app.domain.model

/**
 * Model Track netral terhadap platform musik (Spotify / YouTube Music).
 * Menggunakan ISRC sebagai kunci pemersatu antar platform jika tersedia.
 */
data class Track(
    val id: String,
    val isrc: String? = null,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationMs: Long,
    val coverUrl: String? = null,
    val audioUrl: String? = null,
    val platformIds: Map<MusicSource, String> = emptyMap()
)
