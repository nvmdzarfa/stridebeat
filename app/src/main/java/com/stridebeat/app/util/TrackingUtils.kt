package com.stridebeat.app.util

import com.stridebeat.app.domain.model.LatLng
import kotlin.math.*

/**
 * Utilitas kalkulasi GPS, jarak geospasial, pace, dan estimasi kalori olahraga.
 */
object TrackingUtils {

    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Menghitung jarak antara dua koordinat LatLng menggunakan rumus Haversine (meter).
     */
    fun calculateHaversineDistance(start: LatLng, end: LatLng): Double {
        val dLat = Math.toRadians(end.latitude - start.latitude)
        val dLon = Math.toRadians(end.longitude - start.longitude)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(start.latitude)) * cos(Math.toRadians(end.latitude)) *
                sin(dLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    /**
     * Menghitung pace rata-rata (menit per kilometer).
     */
    fun calculatePaceMinPerKm(durationMs: Long, distanceMeters: Double): Double? {
        if (distanceMeters < 10.0 || durationMs < 1000) return null
        val distanceKm = distanceMeters / 1000.0
        val durationMinutes = durationMs / 60000.0
        val pace = durationMinutes / distanceKm
        // Batas wajar: pace pelari/pesepeda antara 2 hingga 25 menit/km
        return if (pace in 2.0..25.0) pace else null
    }

    /**
     * Memformat pace numerik (misal 6.25) menjadi format string sporty: 6'15"
     */
    fun formatPace(paceMinPerKm: Double?): String {
        if (paceMinPerKm == null || paceMinPerKm <= 0.0) return "--'--\""
        val minutes = paceMinPerKm.toInt()
        val seconds = ((paceMinPerKm - minutes) * 60).toInt()
        return String.format("%d'%02d\"", minutes, seconds)
    }

    /**
     * Memformat durasi milidetik menjadi format HH:mm:ss atau mm:ss.
     */
    fun formatDuration(durationMs: Long): String {
        val totalSecs = durationMs / 1000
        val hours = totalSecs / 3600
        val minutes = (totalSecs % 3600) / 60
        val seconds = totalSecs % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Estimasi kalori menggunakan Metabolic Equivalent of Task (MET).
     * Standar berat badan pelari: 68 kg.
     * Sesuai PRD §7.6: Kalori WAJIB diberi label "(estimasi)" di UI.
     */
    fun estimateCalories(durationMs: Long, distanceMeters: Double, userWeightKg: Double = 68.0): Double {
        if (durationMs <= 0 || distanceMeters <= 0) return 0.0
        val hours = durationMs / 3600000.0
        val speedKmh = (distanceMeters / 1000.0) / hours

        val met = when {
            speedKmh < 6.0 -> 4.0   // Jalan santai / jalan cepat
            speedKmh < 8.0 -> 8.3   // Jogging lambat
            speedKmh < 10.0 -> 9.8  // Lari sedang
            speedKmh < 12.0 -> 11.5 // Lari cepat
            else -> 12.8            // Sprint
        }

        return met * userWeightKg * hours
    }
}
