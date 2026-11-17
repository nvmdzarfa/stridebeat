package com.stridebeat.app.util

import com.stridebeat.app.domain.model.WorkoutType
import java.util.Locale

/**
 * Utilitas pembentuk teks ucapan bahasa Indonesia untuk Voice Audio Cues (TTS).
 * Didesain tanpa dependensi Android Framework agar mudah di-unit test.
 */
object VoiceCueFormatter {

    fun formatWorkoutStarted(type: WorkoutType): String {
        val typeIndo = when (type) {
            WorkoutType.RUN -> "Lari"
            WorkoutType.WALK -> "Jalan santai"
            WorkoutType.CYCLE -> "Bersepeda"
        }
        return "Sesi $typeIndo dimulai. Selamat berolahraga!"
    }

    fun formatWorkoutPaused(): String {
        return "Workout dijeda."
    }

    fun formatWorkoutResumed(): String {
        return "Workout dilanjutkan."
    }

    fun formatWorkoutCompleted(distanceMeters: Double, durationMs: Long): String {
        val distText = if (distanceMeters >= 1000.0) {
            val km = distanceMeters / 1000.0
            String.format(Locale.US, "Total jarak %.2f kilometer", km)
        } else {
            "${distanceMeters.toInt()} meter"
        }
        val durText = formatDurationIndonesian(durationMs)
        return "Workout selesai. $distText. Total waktu $durText. Kerja bagus!"
    }

    fun formatKmSplit(km: Int, splitDurationMs: Long, averagePaceMinPerKm: Double?): String {
        val splitDurText = formatDurationIndonesian(splitDurationMs)
        val sb = StringBuilder()
        sb.append("Kilometer $km. Waktu $splitDurText.")
        if (averagePaceMinPerKm != null && averagePaceMinPerKm > 0 && !averagePaceMinPerKm.isNaN() && !averagePaceMinPerKm.isInfinite()) {
            val paceText = formatPaceIndonesian(averagePaceMinPerKm)
            sb.append(" Rata-rata pace $paceText.")
        }
        return sb.toString()
    }

    fun formatDurationIndonesian(durationMs: Long): String {
        val totalSec = (durationMs / 1000).coerceAtLeast(0)
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60

        return when {
            hours > 0 -> {
                if (seconds > 0) {
                    "$hours jam $minutes menit $seconds detik"
                } else if (minutes > 0) {
                    "$hours jam $minutes menit"
                } else {
                    "$hours jam"
                }
            }
            minutes > 0 -> {
                if (seconds > 0) {
                    "$minutes menit $seconds detik"
                } else {
                    "$minutes menit"
                }
            }
            else -> "$seconds detik"
        }
    }

    fun formatPaceIndonesian(paceMinPerKm: Double): String {
        if (paceMinPerKm <= 0.0 || paceMinPerKm.isNaN() || paceMinPerKm.isInfinite()) {
            return "0 detik per kilometer"
        }
        val minutes = paceMinPerKm.toInt()
        val seconds = ((paceMinPerKm - minutes) * 60).toInt().coerceIn(0, 59)

        return if (minutes > 0) {
            if (seconds > 0) {
                "$minutes menit $seconds detik per kilometer"
            } else {
                "$minutes menit per kilometer"
            }
        } else {
            "$seconds detik per kilometer"
        }
    }
}
