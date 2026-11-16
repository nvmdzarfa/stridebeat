package com.stridebeat.app

import com.stridebeat.app.domain.model.MusicSource
import com.stridebeat.app.domain.model.Track
import com.stridebeat.app.util.MusicQueueCoordinator
import com.stridebeat.app.util.TrackMatchingEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicSwitcherTest {

    @Test
    fun testSwitchToYouTubeMusicFailsWhenFeatureFlagIsDisabled() {
        val coordinator = MusicQueueCoordinator(
            initialSource = MusicSource.SOUNDCLOUD,
            youtubeMusicEnabled = false
        )

        val switched = coordinator.switchSource(MusicSource.YOUTUBE_MUSIC)
        assertFalse(switched)
        assertEquals(MusicSource.SOUNDCLOUD, coordinator.activeSource)
        assertNotNull(coordinator.playbackNotice)
        assertTrue(coordinator.playbackNotice!!.contains("dinonaktifkan"))
    }

    @Test
    fun testSwitchToSpotifySafelyIgnored() {
        val coordinator = MusicQueueCoordinator(
            initialSource = MusicSource.SOUNDCLOUD,
            youtubeMusicEnabled = true
        )

        val switched = coordinator.switchSource(MusicSource.SPOTIFY)
        assertFalse(switched)
        assertEquals(MusicSource.SOUNDCLOUD, coordinator.activeSource)
    }

    @Test
    fun testSwitchBetweenSoundCloudAndYouTubeMusicSucceeds() {
        val coordinator = MusicQueueCoordinator(
            initialSource = MusicSource.SOUNDCLOUD,
            youtubeMusicEnabled = true
        )

        val switchedToYtm = coordinator.switchSource(MusicSource.YOUTUBE_MUSIC)
        assertTrue(switchedToYtm)
        assertEquals(MusicSource.YOUTUBE_MUSIC, coordinator.activeSource)

        val switchedToSc = coordinator.switchSource(MusicSource.SOUNDCLOUD)
        assertTrue(switchedToSc)
        assertEquals(MusicSource.SOUNDCLOUD, coordinator.activeSource)
    }

    @Test
    fun testRecordPlayedTrackRemapsLegacyScTrackCleanly() {
        val coordinator = MusicQueueCoordinator(
            initialSource = MusicSource.YOUTUBE_MUSIC,
            youtubeMusicEnabled = true
        )

        val legacyTrack = Track(
            id = "sc_123456",
            title = "Midnight City",
            artist = "M83",
            durationMs = 240000L
        )

        coordinator.setQueue(listOf(legacyTrack))
        coordinator.recordCurrentTrackPlay(1000L)

        val played = coordinator.playedTracks
        assertEquals(1, played.size)
        // sc_ track should remap to SOUNDCLOUD even if activeSource was YOUTUBE_MUSIC in legacy session
        assertEquals(MusicSource.SOUNDCLOUD, played[0].source)
        assertEquals("sc_123456", played[0].trackId)
    }

    @Test
    fun testQueueSkipOnUnavailableStopsAfterOneFullPassWithoutInfiniteLoop() {
        val coordinator = MusicQueueCoordinator(
            initialSource = MusicSource.YOUTUBE_MUSIC,
            youtubeMusicEnabled = true
        )

        val track1 = Track(id = "ytm_1", title = "Track 1", artist = "A", durationMs = 180000L)
        val track2 = Track(id = "ytm_2", title = "Track 2", artist = "B", durationMs = 190000L)
        val track3 = Track(id = "ytm_3", title = "Track 3", artist = "C", durationMs = 200000L)

        coordinator.setQueue(listOf(track1, track2, track3), startingIndex = 0)

        // Skenario: semua track tidak playable (misal terkena 403 / cipher)
        var checkCount = 0
        val next = coordinator.advanceToNextPlayable { _, _ ->
            checkCount++
            false
        }

        // Harus dicek tepat 3 kali (queue.size) lalu berhenti dengan notice yang jujur
        assertNull(next)
        assertEquals(3, checkCount)
        assertNotNull(coordinator.playbackNotice)
        assertTrue(coordinator.playbackNotice!!.contains("Tidak ada lagu"))
    }

    @Test
    fun testQueueSkipFindsNextPlayableTrackCorrectly() {
        val coordinator = MusicQueueCoordinator(
            initialSource = MusicSource.SOUNDCLOUD,
            youtubeMusicEnabled = true
        )

        val track1 = Track(id = "sc_1", title = "Broken Stream Track", artist = "A", durationMs = 180000L)
        val track2 = Track(id = "sc_2", title = "Valid Stream Track", artist = "B", durationMs = 190000L)

        coordinator.setQueue(listOf(track1, track2), startingIndex = 0)

        val next = coordinator.advanceToNextPlayable { track, _ ->
            track.id == "sc_2"
        }

        assertNotNull(next)
        assertEquals("sc_2", next?.id)
        assertEquals("sc_2", coordinator.currentTrack?.id)
    }
}
