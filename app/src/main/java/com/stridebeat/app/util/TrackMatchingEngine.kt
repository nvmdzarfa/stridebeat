package com.stridebeat.app.util

import com.stridebeat.app.domain.model.MusicSource
import com.stridebeat.app.domain.model.Track
import kotlin.math.abs

/**
 * Mesin pencocokan lagu antar platform (SoundCloud <-> YouTube Music / Spotify).
 * Sesuai PRD §6.4:
 * 1. Kunci Utama: ISRC (International Standard Recording Code).
 * 2. Fallback: Normalisasi judul + artis + toleransi durasi (±2 detik / 2000 ms).
 */
object TrackMatchingEngine {

    private const val DURATION_TOLERANCE_MS = 2000L

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

        // 2. Fallback: Normalisasi judul + artis + toleransi durasi (±2 detik)
        val titleA = normalizeString(trackA.title)
        val titleB = normalizeString(trackB.title)
        val artistA = normalizeString(trackA.artist)
        val artistB = normalizeString(trackB.artist)

        val titlesMatch = titleA.contains(titleB) || titleB.contains(titleA)
        val artistsMatch = artistA.contains(artistB) || artistB.contains(artistA)

        val durationDiffMs = abs(trackA.durationMs - trackB.durationMs)
        val durationMatches = durationDiffMs <= DURATION_TOLERANCE_MS

        return titlesMatch && artistsMatch && durationMatches
    }

    /**
     * Mencari kecocokan terbaik untuk target track dari daftar kandidat.
     * Mengutamakan ISRC match, kemudian fallback normalisasi judul + artis + durasi.
     */
    fun findBestMatch(target: Track, candidates: List<Track>): Track? {
        if (candidates.isEmpty()) return null

        // 1. Prioritas utama: ISRC matching
        if (!target.isrc.isNullOrBlank()) {
            val isrcMatch = candidates.firstOrNull { candidate ->
                !candidate.isrc.isNullOrBlank() && candidate.isrc.equals(target.isrc, ignoreCase = true)
            }
            if (isrcMatch != null) return isrcMatch
        }

        // 2. Fallback: Title + Artist + Duration (±2s)
        return candidates.firstOrNull { candidate ->
            isMatchingTrack(target, candidate)
        }
    }

    /**
     * Remapping riwayat sesi lama:
     * Di v0.1.2 dan sebelumnya, pencarian online SoundCloud sempat disimpan dengan platformIds
     * YOUTUBE_MUSIC dan ID berawalan "sc_". Remapping ini mengembalikan ke MusicSource.SOUNDCLOUD
     * secara transparan tanpa mengubah skema Room DB / Firestore.
     */
    fun remapLegacySource(trackId: String, source: MusicSource): MusicSource {
        if (trackId.startsWith("sc_") && source == MusicSource.YOUTUBE_MUSIC) {
            return MusicSource.SOUNDCLOUD
        }
        return source
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
