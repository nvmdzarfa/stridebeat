package com.stridebeat.app.util

import com.stridebeat.app.domain.model.LatLng
import com.stridebeat.app.domain.model.MusicSource
import com.stridebeat.app.domain.model.PlayedTrack
import com.stridebeat.app.domain.model.WorkoutSession
import com.stridebeat.app.domain.model.WorkoutType

/**
 * Data contoh (SAMPLE) untuk memverifikasi UI pada Milestone M0.
 * Sesuai PRD aturan #7: Semua data contoh di UI dan kode WAJIB diberi label jelas "CONTOH"
 * dan dapat diganti dengan data live Room/API tanpa mengubah struktur UI.
 */
object SampleData {

    const val SAMPLE_LABEL = "CONTOH"

    data class WeeklySummary(
        val totalDistanceKm: Double,
        val totalDurationMinutes: Int,
        val sessionCount: Int,
        val isSample: Boolean = true
    )

    val sampleWeeklySummary = WeeklySummary(
        totalDistanceKm = 14.8,
        totalDurationMinutes = 88,
        sessionCount = 3,
        isSample = true
    )

    val samplePlayedTracks = listOf(
        PlayedTrack(
            trackId = "sample_track_1",
            title = "Midnight City",
            artist = "M83",
            source = MusicSource.SPOTIFY,
            playedAt = System.currentTimeMillis() - 25 * 60 * 1000
        ),
        PlayedTrack(
            trackId = "sample_track_2",
            title = "Starboy",
            artist = "The Weeknd, Daft Punk",
            source = MusicSource.SPOTIFY,
            playedAt = System.currentTimeMillis() - 21 * 60 * 1000
        ),
        PlayedTrack(
            trackId = "sample_track_3",
            title = "Run Boy Run",
            artist = "Woodkid",
            source = MusicSource.YOUTUBE_MUSIC,
            playedAt = System.currentTimeMillis() - 17 * 60 * 1000
        ),
        PlayedTrack(
            trackId = "sample_track_4",
            title = "Stronger",
            artist = "Kanye West",
            source = MusicSource.SPOTIFY,
            playedAt = System.currentTimeMillis() - 13 * 60 * 1000
        )
    )

    val sampleLastSession = WorkoutSession(
        id = "sample_session_1",
        type = WorkoutType.RUN,
        startedAt = System.currentTimeMillis() - 2 * 3600 * 1000,
        endedAt = System.currentTimeMillis() - 2 * 3600 * 1000 + 32 * 60 * 1000,
        distanceMeters = 5240.0,
        durationMs = 32 * 60 * 1000L,
        avgPaceMinPerKm = 6.11,
        caloriesKcal = 385.0,
        routePoints = listOf(
            LatLng(-6.2088, 106.8456),
            LatLng(-6.2095, 106.8468),
            LatLng(-6.2110, 106.8480),
            LatLng(-6.2125, 106.8495),
            LatLng(-6.2140, 106.8510)
        ),
        musicSource = MusicSource.SPOTIFY,
        playedTracks = samplePlayedTracks,
        stravaActivityId = null,
        gpsInterrupted = false
    )

    val sampleSessionHistory = listOf(
        sampleLastSession,
        WorkoutSession(
            id = "sample_session_2",
            type = WorkoutType.RUN,
            startedAt = System.currentTimeMillis() - 26 * 3600 * 1000,
            endedAt = System.currentTimeMillis() - 26 * 3600 * 1000 + 45 * 60 * 1000,
            distanceMeters = 6800.0,
            durationMs = 45 * 60 * 1000L,
            avgPaceMinPerKm = 6.62,
            caloriesKcal = 490.0,
            routePoints = emptyList(),
            musicSource = MusicSource.YOUTUBE_MUSIC,
            playedTracks = samplePlayedTracks.take(2),
            stravaActivityId = null,
            gpsInterrupted = false
        ),
        WorkoutSession(
            id = "sample_session_3",
            type = WorkoutType.RIDE,
            startedAt = System.currentTimeMillis() - 72 * 3600 * 1000,
            endedAt = System.currentTimeMillis() - 72 * 3600 * 1000 + 20 * 60 * 1000,
            distanceMeters = 2760.0,
            durationMs = 20 * 60 * 1000L,
            avgPaceMinPerKm = 7.24,
            caloriesKcal = 160.0,
            routePoints = emptyList(),
            musicSource = MusicSource.SPOTIFY,
            playedTracks = samplePlayedTracks.take(1),
            stravaActivityId = null,
            gpsInterrupted = false
        )
    )
}
