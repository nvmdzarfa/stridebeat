package com.stridebeat.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.stridebeat.app.domain.model.WorkoutSession
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Utilitas untuk mengekspor sesi olahraga ke format GPX 1.1 (GPS Exchange Format).
 * Format ini kompatibel secara universal untuk diunggah langsung ke Strava (strava.com/upload),
 * Garmin Connect, Komoot, dsb.
 */
object GpxExporter {

    fun generateGpxXml(session: WorkoutSession): String {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val startTimeStr = isoFormat.format(Date(session.startedAt))

        val builder = StringBuilder()
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        builder.append("<gpx version=\"1.1\" creator=\"StrideBeat Android Native\"\n")
        builder.append("  xmlns=\"http://www.topografix.com/GPX/1/1\"\n")
        builder.append("  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n")
        builder.append("  xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n")
        builder.append("  <metadata>\n")
        builder.append("    <name>Sesi ${session.type.displayName} StrideBeat</name>\n")
        builder.append("    <time>$startTimeStr</time>\n")
        builder.append("  </metadata>\n")
        builder.append("  <trk>\n")
        builder.append("    <name>StrideBeat ${session.type.displayName}</name>\n")
        builder.append("    <type>${session.type.name.lowercase()}</type>\n")
        builder.append("    <trkseg>\n")

        val totalPts = session.routePoints.size
        session.routePoints.forEachIndexed { index, pt ->
            val pointTime = if (session.durationMs > 0L && totalPts > 1) {
                session.startedAt + ((session.durationMs * index) / (totalPts - 1))
            } else {
                session.startedAt + (index * 2000L)
            }
            val pointTimeStr = isoFormat.format(Date(pointTime))
            builder.append("      <trkpt lat=\"${pt.latitude}\" lon=\"${pt.longitude}\">\n")
            builder.append("        <time>$pointTimeStr</time>\n")
            builder.append("      </trkpt>\n")
        }

        builder.append("    </trkseg>\n")
        builder.append("  </trk>\n")
        builder.append("</gpx>")

        return builder.toString()
    }

    /**
     * Menyimpan file GPX ke cache dan membuat Intent berbagi untuk diunduh / dikirim.
     */
    fun createShareGpxIntent(context: Context, session: WorkoutSession): Intent {
        val gpxData = generateGpxXml(session)
        val fileName = "StrideBeat_${session.type.name}_${session.startedAt}.gpx"
        val file = File(context.cacheDir, fileName).apply {
            writeText(gpxData)
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/gpx+xml"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Rute Olahraga StrideBeat")
            putExtra(Intent.EXTRA_TEXT, "Rute ${session.type.displayName} (${String.format(Locale.US, "%.2f km", session.distanceMeters / 1000)}) dari StrideBeat.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
