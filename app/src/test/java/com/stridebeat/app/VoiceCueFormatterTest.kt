package com.stridebeat.app

import com.stridebeat.app.domain.model.WorkoutType
import com.stridebeat.app.util.VoiceCueFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCueFormatterTest {

    @Test
    fun testFormatWorkoutStarted() {
        assertEquals("Sesi Lari dimulai. Selamat berolahraga!", VoiceCueFormatter.formatWorkoutStarted(WorkoutType.RUN))
        assertEquals("Sesi Jalan santai dimulai. Selamat berolahraga!", VoiceCueFormatter.formatWorkoutStarted(WorkoutType.WALK))
        assertEquals("Sesi Bersepeda dimulai. Selamat berolahraga!", VoiceCueFormatter.formatWorkoutStarted(WorkoutType.RIDE))
    }

    @Test
    fun testFormatWorkoutPausedAndResumed() {
        assertEquals("Workout dijeda.", VoiceCueFormatter.formatWorkoutPaused())
        assertEquals("Workout dilanjutkan.", VoiceCueFormatter.formatWorkoutResumed())
    }

    @Test
    fun testFormatDurationIndonesian() {
        assertEquals("45 detik", VoiceCueFormatter.formatDurationIndonesian(45_000L))
        assertEquals("5 menit 30 detik", VoiceCueFormatter.formatDurationIndonesian(330_000L))
        assertEquals("5 menit", VoiceCueFormatter.formatDurationIndonesian(300_000L))
        assertEquals("1 jam 1 menit 5 detik", VoiceCueFormatter.formatDurationIndonesian(3_665_000L))
        assertEquals("1 jam 1 menit", VoiceCueFormatter.formatDurationIndonesian(3_660_000L))
        assertEquals("1 jam", VoiceCueFormatter.formatDurationIndonesian(3_600_000L))
    }

    @Test
    fun testFormatPaceIndonesian() {
        assertEquals("5 menit 30 detik per kilometer", VoiceCueFormatter.formatPaceIndonesian(5.5))
        assertEquals("6 menit per kilometer", VoiceCueFormatter.formatPaceIndonesian(6.0))
        assertEquals("30 detik per kilometer", VoiceCueFormatter.formatPaceIndonesian(0.5))
        assertEquals("0 detik per kilometer", VoiceCueFormatter.formatPaceIndonesian(0.0))
    }

    @Test
    fun testFormatKmSplit() {
        val splitTextWithPace = VoiceCueFormatter.formatKmSplit(
            km = 1,
            splitDurationMs = 330_000L,
            averagePaceMinPerKm = 5.5
        )
        assertEquals("Kilometer 1. Waktu 5 menit 30 detik. Rata-rata pace 5 menit 30 detik per kilometer.", splitTextWithPace)

        val splitTextNoPace = VoiceCueFormatter.formatKmSplit(
            km = 2,
            splitDurationMs = 300_000L,
            averagePaceMinPerKm = null
        )
        assertEquals("Kilometer 2. Waktu 5 menit.", splitTextNoPace)
    }

    @Test
    fun testFormatWorkoutCompleted() {
        val completedKm = VoiceCueFormatter.formatWorkoutCompleted(5_230.0, 1_800_000L)
        assertTrue(completedKm.contains("Total jarak 5.23 kilometer"))
        assertTrue(completedKm.contains("Total waktu 30 menit"))
        assertTrue(completedKm.contains("Kerja bagus!"))

        val completedMeters = VoiceCueFormatter.formatWorkoutCompleted(850.0, 45_000L)
        assertTrue(completedMeters.contains("850 meter"))
        assertTrue(completedMeters.contains("Total waktu 45 detik"))
    }

    @Test
    fun testMilestoneCalculationLogic() {
        val intervalKm = 1.0
        val intervalMeters = intervalKm * 1000.0

        assertEquals(0, (999.0 / intervalMeters).toInt())
        assertEquals(1, (1000.0 / intervalMeters).toInt())
        assertEquals(1, (1500.0 / intervalMeters).toInt())
        assertEquals(2, (2000.0 / intervalMeters).toInt())
        assertEquals(3, (3000.0 / intervalMeters).toInt())

        val intervalHalfKm = 0.5
        val intervalHalfMeters = intervalHalfKm * 1000.0
        assertEquals(0, (499.0 / intervalHalfMeters).toInt())
        assertEquals(1, (500.0 / intervalHalfMeters).toInt())
        assertEquals(2, (1000.0 / intervalHalfMeters).toInt())
    }

    @Test
    fun testFormatTargetCues() {
        val halfway5k = VoiceCueFormatter.formatTargetHalfway("5.0 kilometer")
        assertEquals("Setengah target 5.0 kilometer tercapai! Pertahankan ritmemu.", halfway5k)

        val reached5k = VoiceCueFormatter.formatTargetReached("5.0 kilometer")
        assertEquals("Target 5.0 kilometer tercapai! Kerja luar biasa!", reached5k)

        val halfway30m = VoiceCueFormatter.formatTargetHalfway("30 menit")
        assertEquals("Setengah target 30 menit tercapai! Pertahankan ritmemu.", halfway30m)

        val reached30m = VoiceCueFormatter.formatTargetReached("30 menit")
        assertEquals("Target 30 menit tercapai! Kerja luar biasa!", reached30m)
    }

    @Test
    fun testWorkoutTargetProgress() {
        val targetNone = com.stridebeat.app.domain.model.WorkoutTarget()
        assertEquals(false, targetNone.isActive)
        assertEquals(0f, targetNone.calculateProgress(1000.0, 60000L), 0.001f)

        val targetDist5k = com.stridebeat.app.domain.model.WorkoutTarget(
            type = com.stridebeat.app.domain.model.WorkoutTargetType.DISTANCE,
            targetDistanceMeters = 5000.0
        )
        assertEquals(true, targetDist5k.isActive)
        assertEquals(0f, targetDist5k.calculateProgress(0.0, 0L), 0.001f)
        assertEquals(0.5f, targetDist5k.calculateProgress(2500.0, 900000L), 0.001f)
        assertEquals(1.0f, targetDist5k.calculateProgress(5000.0, 1800000L), 0.001f)
        // Coerced at 1f
        assertEquals(1.0f, targetDist5k.calculateProgress(6000.0, 2000000L), 0.001f)

        val targetDur30m = com.stridebeat.app.domain.model.WorkoutTarget(
            type = com.stridebeat.app.domain.model.WorkoutTargetType.DURATION,
            targetDurationMs = 30 * 60 * 1000L
        )
        assertEquals(true, targetDur30m.isActive)
        assertEquals(0f, targetDur30m.calculateProgress(0.0, 0L), 0.001f)
        assertEquals(0.5f, targetDur30m.calculateProgress(2500.0, 15 * 60 * 1000L), 0.001f)
        assertEquals(1.0f, targetDur30m.calculateProgress(5000.0, 30 * 60 * 1000L), 0.001f)
        assertEquals(1.0f, targetDur30m.calculateProgress(6000.0, 45 * 60 * 1000L), 0.001f)
    }
}
