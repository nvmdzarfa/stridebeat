package com.stridebeat.app

import com.stridebeat.app.domain.model.MusicSource
import com.stridebeat.app.domain.model.PlayedTrack
import com.stridebeat.app.domain.model.StravaUploadStatus
import com.stridebeat.app.domain.model.WorkoutSession
import com.stridebeat.app.domain.model.WorkoutType
import com.stridebeat.app.util.StravaFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StravaIntegrationTest {

    private fun buildSession(
        id: String = "sess_1",
        type: WorkoutType = WorkoutType.RUN,
        startedAt: Long = 1731713400000L, // 16 Nov 2024
        distanceMeters: Double = 5120.0,
        durationMs: Long = 1800_000L, // 30 menit
        playedTracksCount: Int = 3,
        stravaActivityId: String? = null,
        isImported: Boolean = false
    ): WorkoutSession {
        val tracks = (1..playedTracksCount).map { idx ->
            PlayedTrack(
                trackId = "t_$idx",
                title = "Lagu $idx",
                artist = "Artis $idx",
                source = MusicSource.SPOTIFY,
                playedAt = startedAt + idx * 60_000L
            )
        }
        return WorkoutSession(
            id = id,
            type = type,
            startedAt = startedAt,
            endedAt = startedAt + durationMs,
            distanceMeters = distanceMeters,
            durationMs = durationMs,
            avgPaceMinPerKm = 5.86,
            caloriesKcal = 340.0,
            routePoints = emptyList(),
            musicSource = MusicSource.SPOTIFY,
            playedTracks = tracks,
            stravaActivityId = stravaActivityId,
            isImportedFromStrava = isImported
        )
    }

    @Test
    fun buildDefaultActivityName_supportsSportTypesAndCustomPrefix() {
        val runSession = buildSession(type = WorkoutType.RUN)
        val walkSession = buildSession(type = WorkoutType.WALK)
        val rideSession = buildSession(type = WorkoutType.RIDE)

        assertTrue(StravaFormatter.buildDefaultActivityName(runSession).startsWith("StrideBeat Run — "))
        assertTrue(StravaFormatter.buildDefaultActivityName(walkSession).startsWith("StrideBeat Walk — "))
        assertTrue(StravaFormatter.buildDefaultActivityName(rideSession).startsWith("StrideBeat Ride — "))

        val custom = StravaFormatter.buildDefaultActivityName(runSession, "Lari Pagi")
        assertTrue(custom.startsWith("Lari Pagi (Run) — "))
    }

    @Test
    fun buildActivityDescription_capsAt10SongsAndAppendsRemainingCount() {
        val session3 = buildSession(playedTracksCount = 3)
        val desc3 = StravaFormatter.buildActivityDescription(session3)
        assertTrue(desc3.contains("Jarak: 5.12 km"))
        assertTrue(desc3.contains("🎵 Diputar di StrideBeat:"))
        assertTrue(desc3.contains("1. Lagu 1 — Artis 1"))
        assertTrue(desc3.contains("3. Lagu 3 — Artis 3"))
        assertFalse(desc3.contains("lagu lainnya"))

        val session14 = buildSession(playedTracksCount = 14)
        val desc14 = StravaFormatter.buildActivityDescription(session14)
        assertTrue(desc14.contains("10. Lagu 10 — Artis 10"))
        assertFalse(desc14.contains("11. Lagu 11"))
        assertTrue(desc14.contains("…dan 4 lagu lainnya"))
    }

    @Test
    fun buildActivityDescription_withNoTracks_containsSummaryOnly() {
        val session0 = buildSession(playedTracksCount = 0)
        val desc = StravaFormatter.buildActivityDescription(session0)
        assertTrue(desc.startsWith("Jarak: 5.12 km"))
        assertFalse(desc.contains("🎵 Diputar di StrideBeat:"))
    }

    @Test
    fun isEligibleForAutoUpload_rejectsShortOrZeroDistanceOrImported() {
        val shortSession = buildSession(durationMs = 45_000L, distanceMeters = 300.0)
        val (shortOk, shortReason) = StravaFormatter.isEligibleForAutoUpload(shortSession)
        assertFalse(shortOk)
        assertTrue(shortReason!!.contains("1 menit"))

        val zeroDistSession = buildSession(durationMs = 120_000L, distanceMeters = 0.0)
        val (zeroOk, zeroReason) = StravaFormatter.isEligibleForAutoUpload(zeroDistSession)
        assertFalse(zeroOk)
        assertTrue(zeroReason!!.contains("0 meter"))

        val importedSession = buildSession(isImported = true)
        val (impOk, _) = StravaFormatter.isEligibleForAutoUpload(importedSession)
        assertFalse(impOk)

        val validSession = buildSession(durationMs = 60_000L, distanceMeters = 150.0)
        val (validOk, validReason) = StravaFormatter.isEligibleForAutoUpload(validSession)
        assertTrue(validOk)
        assertEquals(null, validReason)
    }

    @Test
    fun decodePolyline_parsesGoogleEncodedPolylineCorrectly() {
        // Standard Google Encoded Polyline sample: "_p~iF~ps|U_ulLnnqC_mqNvxq`@"
        val points = StravaFormatter.decodePolyline("_p~iF~ps|U_ulLnnqC_mqNvxq`@")
        assertEquals(3, points.size)
        assertEquals(38.5, points[0].latitude, 0.001)
        assertEquals(-120.2, points[0].longitude, 0.001)
        assertEquals(40.7, points[1].latitude, 0.001)
        assertEquals(-120.95, points[1].longitude, 0.001)
        assertEquals(43.252, points[2].latitude, 0.001)
        assertEquals(-126.453, points[2].longitude, 0.001)
    }

    @Test
    fun filterNonDuplicateImports_deduplicatesByStravaIdAndByMetrics() {
        val existing1 = buildSession(id = "local_1", startedAt = 1731713400000L, distanceMeters = 5000.0, durationMs = 1800_000L, stravaActivityId = "999001")
        val existing2 = buildSession(id = "local_2", startedAt = 1731800000000L, distanceMeters = 3200.0, durationMs = 1200_000L, stravaActivityId = null)

        val importSameId = buildSession(id = "strava_999001", startedAt = 1731713400000L, distanceMeters = 5000.0, durationMs = 1800_000L, stravaActivityId = "999001", isImported = true)
        val importSameMetrics = buildSession(id = "strava_999002", startedAt = 1731800030000L, distanceMeters = 3220.0, durationMs = 1210_000L, stravaActivityId = "999002", isImported = true)
        val importBrandNew = buildSession(id = "strava_999003", startedAt = 1731900000000L, distanceMeters = 10000.0, durationMs = 3300_000L, stravaActivityId = "999003", isImported = true)

        val filtered = StravaFormatter.filterNonDuplicateImports(
            existingSessions = listOf(existing1, existing2),
            importedSessions = listOf(importSameId, importSameMetrics, importBrandNew)
        )

        assertEquals(1, filtered.size)
        assertEquals("999003", filtered.first().stravaActivityId)
        assertEquals(StravaUploadStatus.UPLOADED, buildSession(stravaActivityId = "999003").stravaUploadStatus)
    }
}
