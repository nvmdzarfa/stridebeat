package com.stridebeat.app.domain.model

/**
 * Catatan lagu yang diputar selama sesi workout tertentu dengan timestamp.
 */
data class PlayedTrack(
    val trackId: String,
    val title: String,
    val artist: String,
    val source: MusicSource,
    val playedAt: Long
)
