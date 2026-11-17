package com.stridebeat.app

import com.stridebeat.app.domain.model.MusicSource
import com.stridebeat.app.domain.model.Track
import com.stridebeat.app.util.TrackMatchingEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    fun testFallbackTitleAndArtistMatchingWithinTwoSeconds() {
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
            durationMs = 231500L, // 1.5 detik beda (<= 2000L)
            platformIds = mapOf(MusicSource.YOUTUBE_MUSIC to "ytm_2")
        )

        assertTrue(TrackMatchingEngine.isMatchingTrack(trackA, trackB))
    }

    @Test
    fun testFallbackRejectsDurationDiffOverTwoSeconds() {
        val a = Track(id = "1", title = "Blinding Lights", artist = "The Weeknd", durationMs = 200_000L)
        val b = Track(id = "2", title = "Blinding Lights", artist = "The Weeknd", durationMs = 202_500L) // 2.5s beda
        assertFalse(TrackMatchingEngine.isMatchingTrack(a, b))
    }

    @Test
    fun testFindBestMatchPrefersIsrcOverTitleFallback() {
        val target = Track(
            id = "target",
            isrc = "ISRC_EXACT",
            title = "Run",
            artist = "OneRepublic",
            durationMs = 180000L
        )

        val candidate1 = Track(
            id = "c1",
            isrc = null,
            title = "Run",
            artist = "OneRepublic",
            durationMs = 180500L
        )

        val candidate2 = Track(
            id = "c2",
            isrc = "ISRC_EXACT",
            title = "Run - Official Video",
            artist = "OneRepublic",
            durationMs = 181000L
        )

        val matched = TrackMatchingEngine.findBestMatch(target, listOf(candidate1, candidate2))
        assertNotNull(matched)
        assertEquals("c2", matched?.id)
    }

    @Test
    fun testFindBestMatchReturnsNullWhenNoMatchFound() {
        val target = Track(
            id = "target",
            isrc = "ISRC_TARGET",
            title = "Levels",
            artist = "Avicii",
            durationMs = 200000L
        )

        val candidate = Track(
            id = "c1",
            isrc = "ISRC_OTHER",
            title = "Wake Me Up",
            artist = "Avicii",
            durationMs = 240000L
        )

        val matched = TrackMatchingEngine.findBestMatch(target, listOf(candidate))
        assertNull(matched)
    }

    @Test
    fun testLegacySoundCloudRemapping() {
        assertEquals(MusicSource.SOUNDCLOUD, TrackMatchingEngine.remapLegacySource("sc_12345", MusicSource.YOUTUBE_MUSIC))
        assertEquals(MusicSource.YOUTUBE_MUSIC, TrackMatchingEngine.remapLegacySource("ytm_abc", MusicSource.YOUTUBE_MUSIC))
        assertEquals(MusicSource.SOUNDCLOUD, TrackMatchingEngine.remapLegacySource("sc_999", MusicSource.SOUNDCLOUD))
        assertEquals(MusicSource.SPOTIFY, TrackMatchingEngine.remapLegacySource("sp_001", MusicSource.SPOTIFY))
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
            platformIds = mapOf(MusicSource.SOUNDCLOUD to "sc_uri")
        )

        assertTrue(TrackMatchingEngine.hasPlatformSupport(track, MusicSource.SOUNDCLOUD))
        assertFalse(TrackMatchingEngine.hasPlatformSupport(track, MusicSource.YOUTUBE_MUSIC))
    }

    @Test
    fun testKoreanAndUnicodeMatching() {
        val ytTrack = Track(
            id = "yt_1",
            title = "WA-R-R",
            artist = "Colde",
            durationMs = 210000L
        )

        val scCandidates = listOf(
            Track(
                id = "sc_1",
                title = "[원곡] 콜드 (Colde)- 와르르 (WA-R-R)",
                artist = "콜드",
                durationMs = 211000L
            ),
            Track(
                id = "sc_2",
                title = "Different Song",
                artist = "Someone Else",
                durationMs = 210000L
            )
        )

        val matched = TrackMatchingEngine.findBestMatch(ytTrack, scCandidates)
        assertNotNull(matched)
        assertEquals("sc_1", matched?.id)
    }
}
