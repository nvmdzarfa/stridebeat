package com.stridebeat.app.util

import com.stridebeat.app.domain.model.MusicSource
import com.stridebeat.app.domain.model.PlayedTrack
import com.stridebeat.app.domain.model.Track

/**
 * Koordinator antrean musik & penukar sumber (SoundCloud <-> YouTube Music).
 * Mencegah infinite loop saat lagu tidak tersedia, mengawal guard Spotify legacy,
 * dan memastikan remapping track warisan berjalan mulus tanpa mengganggu tracking olahraga.
 */
class MusicQueueCoordinator(
    initialSource: MusicSource = MusicSource.SOUNDCLOUD,
    var youtubeMusicEnabled: Boolean = true
) {
    var activeSource: MusicSource = initialSource
        private set

    var currentTrack: Track? = null
        private set

    var queue: List<Track> = emptyList()
        private set

    var playbackNotice: String? = null
        private set

    var unavailableExternalUrl: String? = null
        private set

    private val _playedTracks = mutableListOf<PlayedTrack>()
    val playedTracks: List<PlayedTrack> get() = _playedTracks.toList()

    fun clearNotice() {
        playbackNotice = null
        unavailableExternalUrl = null
    }

    fun setNotice(message: String?, externalUrl: String? = null) {
        playbackNotice = message
        unavailableExternalUrl = externalUrl
    }

    fun switchSource(newSource: MusicSource): Boolean {
        if (newSource == MusicSource.SPOTIFY) {
            // Guard: Spotify telah dihapus dari UI aktif, abaikan panggilan legacy secara aman
            return false
        }
        if (newSource == MusicSource.YOUTUBE_MUSIC && !youtubeMusicEnabled) {
            playbackNotice = "YouTube Music sedang dinonaktifkan di Pengaturan"
            return false
        }
        activeSource = newSource
        clearNotice()
        return true
    }

    fun setQueue(newQueue: List<Track>, startingIndex: Int = 0) {
        queue = newQueue
        currentTrack = if (newQueue.isNotEmpty() && startingIndex in newQueue.indices) {
            newQueue[startingIndex]
        } else if (newQueue.isNotEmpty()) {
            newQueue[0]
        } else null
    }

    fun setCurrentTrackOnly(track: Track?) {
        currentTrack = track
    }

    fun recordCurrentTrackPlay(timestamp: Long = System.currentTimeMillis()) {
        val track = currentTrack ?: return
        val remappedSource = TrackMatchingEngine.remapLegacySource(track.id, activeSource)
        val played = PlayedTrack(
            trackId = track.id,
            title = track.title,
            artist = track.artist,
            source = remappedSource,
            playedAt = timestamp
        )
        if (_playedTracks.isEmpty() || _playedTracks.last().trackId != track.id) {
            _playedTracks.add(played)
        }
    }

    /**
     * Maju ke lagu berikutnya di antrean dengan batas maksimal queue.size percobaan.
     * Mengembalikan lagu yang playable, atau null jika seluruh antrean gagal.
     */
    fun advanceToNextPlayable(
        isPlayable: (Track, MusicSource) -> Boolean
    ): Track? {
        if (queue.isEmpty()) return null

        val startIndex = queue.indexOfFirst { it.id == currentTrack?.id }
        val effectiveStart = if (startIndex != -1) startIndex else 0

        for (step in 1..queue.size) {
            val candidateIndex = (effectiveStart + step) % queue.size
            val candidate = queue[candidateIndex]
            if (isPlayable(candidate, activeSource)) {
                currentTrack = candidate
                return candidate
            } else {
                playbackNotice = "\"${candidate.title}\" tidak tersedia di ${activeSource.displayName}"
                if (activeSource == MusicSource.YOUTUBE_MUSIC) {
                    val vid = candidate.platformIds[MusicSource.YOUTUBE_MUSIC] ?: candidate.id.removePrefix("ytm_")
                    unavailableExternalUrl = "https://music.youtube.com/watch?v=$vid"
                }
            }
        }

        playbackNotice = "Tidak ada lagu yang dapat diputar di ${activeSource.displayName}"
        return null
    }

    fun clearSessionTracks() {
        _playedTracks.clear()
    }
}
