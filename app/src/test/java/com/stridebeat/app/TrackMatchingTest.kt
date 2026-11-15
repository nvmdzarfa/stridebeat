package com.stridebeat.app

import com.stridebeat.app.domain.model.MusicSource
import com.stridebeat.app.domain.model.Track
import com.stridebeat.app.util.TrackMatchingEngine
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackMatchingTest {

    @Test
    fun testIsrcMatchingExact() {
        val trackSpotify = Track(
            id = "t1",
            isrc = "USUM71703861",
            title = "Midnight City",
            artist = "M83",
            durationMs = 244000L,
            platformIds = mapOf(MusicSource.SPOTIFY to "sp_1")
        )

        val trackYtm = Track(
            id = "t2",
            isrc = "USUM71703861",
            title = "Midnight City - Official Audio",
            artist = "M83",
            durationMs = 245000L,
            platformIds = mapOf(MusicSource.YOUTUBE_MUSIC to "ytm_1")
        )

        assertTrue(TrackMatchingEngine.isMatchingTrack(trackSpotify, trackYtm))
    }

    @Test
    fun testFallbackTitleAndArtistMatching() {
        val trackA = Track(
            id = "t1",
            isrc = null,
            title = "Starboy (feat. Daft Punk)",
            artist = "The Weeknd",
            durationMs = 230000L,
            platformIds = mapOf(MusicSource.SPOTIFY to "sp_2")
        )

        val trackB = Track(
            id = "t2",
            isrc = null,
            title = "Starboy",
            artist = "The Weeknd, Daft Punk",
            durationMs = 231500L, // 1.5 detik beda
            platformIds = mapOf(MusicSource.YOUTUBE_MUSIC to "ytm_2")
        )

        assertTrue(TrackMatchingEngine.isMatchingTrack(trackA, trackB))
    }

    @Test
    fun testDifferentTracksDoNotMatch() {
        val trackA = Track(
            id = "t1",
            isrc = "ISRC111",
            title = "Stronger",
            artist = "Kanye West",
            durationMs = 311000L
        )

        val trackB = Track(
            id = "t2",
            isrc = "ISRC222",
            title = "Run Boy Run",
            artist = "Woodkid",
            durationMs = 213000L
        )

        assertFalse(TrackMatchingEngine.isMatchingTrack(trackA, trackB))
    }

    @Test
    fun testPlatformSupportCheck() {
        val track = Track(
            id = "t1",
            title = "Test Track",
            artist = "Artist",
            durationMs = 200000L,
            platformIds = mapOf(MusicSource.SPOTIFY to "spotify:uri")
        )

        assertTrue(TrackMatchingEngine.hasPlatformSupport(track, MusicSource.SPOTIFY))
        assertFalse(TrackMatchingEngine.hasPlatformSupport(track, MusicSource.YOUTUBE_MUSIC))
    }
}
