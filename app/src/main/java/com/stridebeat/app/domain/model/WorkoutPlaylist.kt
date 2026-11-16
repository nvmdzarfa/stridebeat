package com.stridebeat.app.domain.model

/**
 * Model Playlist olahraga yang berisi daftar Track netral platform.
 * Sesuai PRD §6.4: Lagu yang sama dapat diputar di Spotify atau YouTube Music
 * menggunakan pencocokan ISRC atau normalisasi judul/artis.
 */
data class WorkoutPlaylist(
    val id: String,
    val name: String,
    val description: String,
    val source: MusicSource? = null,
    val tracks: List<Track>,
    val coverUrl: String? = null
)

