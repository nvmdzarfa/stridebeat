package com.stridebeat.app

import com.stridebeat.app.domain.model.LatLng
import com.stridebeat.app.domain.model.MusicSource
import com.stridebeat.app.domain.model.PlayedTrack
import com.stridebeat.app.domain.model.Track
import com.stridebeat.app.domain.model.WorkoutSession
import com.stridebeat.app.domain.model.WorkoutType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainModelTest {

    @Test
    fun testTrackNeutralityAndPlatformMapping() {
        val track = Track(
            id = "sb_001",
            isrc = "USUM71703861",
            title = "Midnight City",
            artist = "M83",
            durationMs = 244000L,
            platformIds = mapOf(
                MusicSource.SPOTIFY to "spotify_uri_123",
                MusicSource.YOUTUBE_MUSIC to "ytm_video_456"
            )
        )

        assertEquals("Midnight City", track.title)
        assertEquals("spotify_uri_123", track.platformIds[MusicSource.SPOTIFY])
        assertEquals("ytm_video_456", track.platformIds[MusicSource.YOUTUBE_MUSIC])
    }

    @Test
    fun testWorkoutSessionInitialization() {
        val session = WorkoutSession(
            id = "sess_001",
            type = WorkoutType.RUN,
            startedAt = 1700000000000L,
            endedAt = 1700001920000L,
            distanceMeters = 5000.0,
            durationMs = 1920000L,
            avgPaceMinPerKm = 6.4,
            caloriesKcal = 350.0,
            routePoints = listOf(
                LatLng(-6.2088, 106.8456),
                LatLng(-6.2140, 106.8510)
            ),
            musicSource = MusicSource.SPOTIFY,
            playedTracks = listOf(
                PlayedTrack("track_1", "Midnight City", "M83", MusicSource.SPOTIFY, 1700000100000L)
            ),
            stravaActivityId = null,
            gpsInterrupted = false
        )

        assertEquals(2, session.routePoints.size)
        assertEquals(WorkoutType.RUN, session.type)
        assertEquals(5000.0, session.distanceMeters, 0.001)
        assertEquals(1, session.playedTracks.size)
    }

    @Test
    fun testMusicSourceEnumValuesAndDisplayNames() {
        assertEquals("SoundCloud", MusicSource.SOUNDCLOUD.displayName)
        assertEquals("Spotify", MusicSource.SPOTIFY.displayName)
        assertEquals("YouTube Music", MusicSource.YOUTUBE_MUSIC.displayName)
        assertEquals(3, MusicSource.values().size)
    }
}

