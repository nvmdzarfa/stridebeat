package com.stridebeat.app.util

import com.stridebeat.app.domain.model.MusicSource
import com.stridebeat.app.domain.model.Track
import kotlin.math.abs

/**
 * Mesin pencocokan lagu antar platform (Spotify <-> YouTube Music).
 * Sesuai PRD §6.4:
 * 1. Kunci Utama: ISRC (International Standard Recording Code).
 * 2. Fallback: Normalisasi judul + artis + toleransi durasi (±2 detik).
 */
object TrackMatchingEngine {

    /**
     * Memeriksa apakah dua lagu identik secara lintas platform.
     */
    fun isMatchingTrack(trackA: Track, trackB: Track): Boolean {
        // 1. Kunci utama: ISRC matching
        if (!trackA.isrc.isNullOrBlank() && !trackB.isrc.isNullOrBlank()) {
            if (trackA.isrc.equals(trackB.isrc, ignoreCase = true)) {
                return true
            }
        }

        // 2. Fallback: Normalisasi judul + artis + toleransi durasi
        val titleA = normalizeString(trackA.title)
        val titleB = normalizeString(trackB.title)
        val artistA = normalizeString(trackA.artist)
        val artistB = normalizeString(trackB.artist)

        val titlesMatch = titleA.contains(titleB) || titleB.contains(titleA)
        val artistsMatch = artistA.contains(artistB) || artistB.contains(artistA)

        val durationDiffMs = abs(trackA.durationMs - trackB.durationMs)
        val durationMatches = durationDiffMs <= 3000L // toleransi ±3 detik

        return titlesMatch && artistsMatch && durationMatches
    }

    /**
     * Memeriksa apakah sebuah lagu memiliki identifier untuk platform target.
     */
    fun hasPlatformSupport(track: Track, targetSource: MusicSource): Boolean {
        return track.platformIds.containsKey(targetSource) && !track.platformIds[targetSource].isNullOrBlank()
    }

    private fun normalizeString(input: String): String {
        return input.lowercase()
            .replace(Regex("\\[.*?\\]|\\(.*?\\)"), "") // Hapus teks dalam tanda kurung seperti (Remastered)
            .replace(Regex("[^a-zA-Z0-9]"), "")       // Hanya sisakan alfanumerik
            .trim()
    }
}
