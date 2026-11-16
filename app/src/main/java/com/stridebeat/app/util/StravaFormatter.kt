package com.stridebeat.app.util

import com.stridebeat.app.domain.model.LatLng
import com.stridebeat.app.domain.model.WorkoutSession
import com.stridebeat.app.domain.model.WorkoutType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

/**
 * Utilitas pemformatan aktivitas Strava, validasi auto-upload,
 * dekode Google Encoded Polyline, dan deduplikasi impor riwayat Strava (M5).
 */
object StravaFormatter {

    const val MAX_SONGS_IN_DESCRIPTION = 10
    const val MIN_AUTO_UPLOAD_DURATION_MS = 60_000L

    fun sportDisplayLabel(type: WorkoutType): String = when (type) {
        WorkoutType.RUN -> "Run"
        WorkoutType.WALK -> "Walk"
        WorkoutType.RIDE -> "Ride"
    }

    fun toStravaSportType(type: WorkoutType): String = when (type) {
        WorkoutType.RUN -> "Run"
        WorkoutType.WALK -> "Walk"
        WorkoutType.RIDE -> "Ride"
    }

    fun fromStravaSportType(sportType: String?): WorkoutType {
        return when (sportType?.trim()?.lowercase(Locale.US)) {
            "walk", "hike", "snowshoe" -> WorkoutType.WALK
            "ride", "virtualride", "ebikeride", "gravelride", "mountainbikeride", "handcycle" -> WorkoutType.RIDE
            else -> WorkoutType.RUN
        }
    }

    /**
     * Format nama aktivitas default sesuai PRD §6.1:
     * "StrideBeat Run — <tanggal> <jam>" (atau Walk / Ride).
     */
    fun buildDefaultActivityName(
        session: WorkoutSession,
        customPrefix: String = "StrideBeat"
    ): String {
        val cleanPrefix = customPrefix.trim().ifBlank { "StrideBeat" }
        val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID"))
        val dateTime = dateFormat.format(Date(session.startedAt))
        val typeSuffix = sportDisplayLabel(session.type)
        return if (cleanPrefix.equals("StrideBeat", ignoreCase = true)) {
            "StrideBeat $typeSuffix — $dateTime"
        } else {
            "$cleanPrefix ($typeSuffix) — $dateTime"
        }
    }

    /**
     * Format deskripsi aktivitas Strava sesuai PRD §6.1:
     * - Baris 1: ringkasan — jarak, durasi, pace rata-rata.
     * - Baris berikut: "🎵 Diputar di StrideBeat:" + daftar lagu (judul — artis) max 10,
     *   lalu "…dan X lagu lainnya" jika > 10.
     * - Tidak pernah menyertakan token, email, atau data pribadi.
     */
    fun buildActivityDescription(session: WorkoutSession): String {
        val distKm = String.format(Locale.US, "%.2f km", session.distanceMeters / 1000.0)
        val duration = TrackingUtils.formatDuration(session.durationMs)
        val pace = TrackingUtils.formatPace(session.avgPaceMinPerKm)
        val summaryLine = "Jarak: $distKm • Durasi: $duration • Pace: $pace/km"

        if (session.playedTracks.isEmpty()) {
            return summaryLine
        }

        val sb = StringBuilder()
        sb.append(summaryLine)
        sb.append("\n\n🎵 Diputar di StrideBeat:")

        val displayedTracks = session.playedTracks.take(MAX_SONGS_IN_DESCRIPTION)
        displayedTracks.forEachIndexed { index, track ->
            val title = track.title.trim().ifBlank { "Tanpa Judul" }
            val artist = track.artist.trim().ifBlank { "Artis Tidak Diketahui" }
            sb.append("\n${index + 1}. $title — $artist")
        }

        val remaining = session.playedTracks.size - MAX_SONGS_IN_DESCRIPTION
        if (remaining > 0) {
            sb.append("\n…dan $remaining lagu lainnya")
        }

        return sb.toString()
    }

    /**
     * Guard auto-upload sesuai PRD §7.1 US-12:
     * Auto-upload tidak berjalan jika durasi < 1 menit atau jarak <= 0.
     */
    fun isEligibleForAutoUpload(session: WorkoutSession): Pair<Boolean, String?> {
        if (session.isImportedFromStrava) {
            return false to "Sesi ini berasal dari impor Strava."
        }
        if (session.durationMs < MIN_AUTO_UPLOAD_DURATION_MS) {
            return false to "Auto-upload dilewati: durasi sesi kurang dari 1 menit."
        }
        if (session.distanceMeters <= 0.0) {
            return false to "Auto-upload dilewati: jarak tempuh masih 0 meter."
        }
        return true to null
    }

    fun buildStravaActivityUrl(activityId: String): String {
        return "https://www.strava.com/activities/${activityId.trim()}"
    }

    /**
     * Deduplikasi aktivitas impor Strava terhadap sesi lokal (PRD §7.1 US-13):
     * Cocokkan berdasar `stravaActivityId` ATAU kombinasi waktu mulai (±2 menit),
     * jarak (±5% atau 50m), dan durasi (±60 detik).
     */
    fun filterNonDuplicateImports(
        existingSessions: List<WorkoutSession>,
        importedSessions: List<WorkoutSession>
    ): List<WorkoutSession> {
        val result = mutableListOf<WorkoutSession>()
        for (candidate in importedSessions) {
            val isDup = existingSessions.any { existing -> isSameWorkout(existing, candidate) } ||
                result.any { accepted -> isSameWorkout(accepted, candidate) }
            if (!isDup) {
                result.add(candidate)
            }
        }
        return result
    }

    fun isSameWorkout(a: WorkoutSession, b: WorkoutSession): Boolean {
        if (!a.stravaActivityId.isNullOrBlank() && a.stravaActivityId == b.stravaActivityId) {
            return true
        }
        val timeDiff = abs(a.startedAt - b.startedAt)
        val maxDistance = max(a.distanceMeters, b.distanceMeters)
        val distanceTolerance = max(50.0, maxDistance * 0.05)
        val distanceDiff = abs(a.distanceMeters - b.distanceMeters)
        val durationDiff = abs(a.durationMs - b.durationMs)

        return timeDiff <= 120_000L && distanceDiff <= distanceTolerance && durationDiff <= 60_000L
    }

    /**
     * Dekode Google Encoded Polyline dari Strava (`map.summary_polyline`) menjadi List<LatLng>.
     */
    fun decodePolyline(encoded: String?): List<LatLng> {
        if (encoded.isNullOrBlank()) return emptyList()
        val poly = mutableListOf<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                if (index >= len) break
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dLat = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lat += dLat

            if (index >= len) break
            shift = 0
            result = 0
            do {
                if (index >= len) break
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dLng = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lng += dLng

            poly.add(LatLng(lat / 1E5, lng / 1E5))
        }
        return poly
    }
}
