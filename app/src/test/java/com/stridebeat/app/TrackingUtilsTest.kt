package com.stridebeat.app

import com.stridebeat.app.domain.model.LatLng
import com.stridebeat.app.util.TrackingUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingUtilsTest {

    @Test
    fun testHaversineDistanceCalculation() {
        // Jarak antara Monas (-6.1754, 106.8272) dan Bundaran HI (-6.1950, 106.8230) ~ 2.2 km
        val monas = LatLng(-6.1754, 106.8272)
        val bundaranHi = LatLng(-6.1950, 106.8230)

        val distance = TrackingUtils.calculateHaversineDistance(monas, bundaranHi)

        // Verifikasi jarak berada di rentang 2100 - 2300 meter
        assertTrue("Distance should be ~2.2km", distance in 2100.0..2300.0)
    }

    @Test
    fun testPaceCalculationAndFormatting() {
        // Durasi 30 menit (1800000 ms) untuk 5000 meter -> Pace 6.00 min/km (6'00")
        val pace = TrackingUtils.calculatePaceMinPerKm(30 * 60 * 1000L, 5000.0)
        assertNotNull(pace)
        assertEquals(6.0, pace!!, 0.01)

        val formatted = TrackingUtils.formatPace(pace)
        assertEquals("6'00\"", formatted)
    }

    @Test
    fun testDurationFormatting() {
        // 15 menit 24 detik = 924,000 ms -> "15:24"
        val durationFormatted = TrackingUtils.formatDuration(924000L)
        assertEquals("15:24", durationFormatted)

        // 1 jam 5 menit 10 detik = 3910,000 ms -> "01:05:10"
        val longDurationFormatted = TrackingUtils.formatDuration(3910000L)
        assertEquals("01:05:10", longDurationFormatted)
    }

    @Test
    fun testCaloriesEstimation() {
        // Lari 5 km dalam 30 menit (kecepatan 10 km/h) berat 68kg -> ~330 kcal
        val calories = TrackingUtils.estimateCalories(30 * 60 * 1000L, 5000.0, 68.0)
        assertTrue("Calories should be estimated reasonable for running 5k", calories in 300.0..400.0)
    }
}
