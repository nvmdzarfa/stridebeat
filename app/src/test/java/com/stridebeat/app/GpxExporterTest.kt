package com.stridebeat.app

import com.stridebeat.app.domain.model.LatLng
import com.stridebeat.app.domain.model.WorkoutSession
import com.stridebeat.app.domain.model.WorkoutType
import com.stridebeat.app.util.GpxExporter
import org.junit.Assert.assertTrue
import org.junit.Test

class GpxExporterTest {

    @Test
    fun testGenerateGpxXml_producesValidGpxStructure() {
        val session = WorkoutSession(
            id = "test-session-123",
            type = WorkoutType.RUN,
            startedAt = 1715670000000L,
            endedAt = 1715671800000L,
            distanceMeters = 5000.0,
            durationMs = 1800000L,
            routePoints = listOf(
                LatLng(-6.200000, 106.816666),
                LatLng(-6.201000, 106.817666),
                LatLng(-6.202000, 106.818666)
            )
        )

        val gpxXml = GpxExporter.generateGpxXml(session)

        // Verifikasi header XML dan schema GPX 1.1
        assertTrue(gpxXml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"))
        assertTrue(gpxXml.contains("<gpx version=\"1.1\" creator=\"StrideBeat Android Native\""))
        assertTrue(gpxXml.contains("http://www.topografix.com/GPX/1/1"))

        // Verifikasi metadata dan track
        assertTrue(gpxXml.contains("<metadata>"))
        assertTrue(gpxXml.contains("<name>Sesi Lari StrideBeat</name>"))
        assertTrue(gpxXml.contains("<trk>"))
        assertTrue(gpxXml.contains("<type>run</type>"))
        assertTrue(gpxXml.contains("<trkseg>"))

        // Verifikasi titik koordinat trek
        assertTrue(gpxXml.contains("<trkpt lat=\"-6.2\" lon=\"106.816666\">"))
        assertTrue(gpxXml.contains("<trkpt lat=\"-6.201\" lon=\"106.817666\">"))
        assertTrue(gpxXml.contains("<trkpt lat=\"-6.202\" lon=\"106.818666\">"))
        assertTrue(gpxXml.contains("</trkseg>"))
        assertTrue(gpxXml.contains("</trk>"))
        assertTrue(gpxXml.endsWith("</gpx>"))
    }
}
