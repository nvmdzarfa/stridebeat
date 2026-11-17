package com.stridebeat.app.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import androidx.core.content.FileProvider
import com.stridebeat.app.domain.model.LatLng
import com.stridebeat.app.domain.model.WorkoutSession
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generator Story Card 9:16 (1080x1920) siap bagikan ke Instagram Story, WhatsApp Status,
 * Telegram, dsb.
 *
 * Menggambar card visual estetis bergaya StrideBeat Glassmorphism:
 * - Latar belakang gelap futuristik dengan radial glow neon
 * - Visual rute lari (GPS polyline) berpendar
 * - Angka metrik utama (Jarak, Waktu, Pace, Kalori) berukuran besar dan jelas
 * - Kartu "Soundtrack of this Run" menampilkan lagu yang didengarkan selama latihan
 * - Watermark branding StrideBeat
 */
object WorkoutStoryCardGenerator {

    const val CARD_WIDTH = 1080
    const val CARD_HEIGHT = 1920

    fun generateStoryBitmap(session: WorkoutSession): Bitmap {
        val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawBackground(canvas)
        drawHeader(canvas, session)
        drawRouteMap(canvas, session.routePoints)
        drawMetrics(canvas, session)
        drawMusicSection(canvas, session)
        drawFooter(canvas)

        return bitmap
    }

    private fun drawBackground(canvas: Canvas) {
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, CARD_HEIGHT.toFloat(),
                intArrayOf(
                    Color.parseColor("#090D16"),
                    Color.parseColor("#0F172A"),
                    Color.parseColor("#060A11")
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(), bgPaint)

        // Glow aksen Cyan di pojok kanan atas
        val cyanGlowPaint = Paint().apply {
            isAntiAlias = true
            shader = RadialGradient(
                CARD_WIDTH * 0.85f, CARD_HEIGHT * 0.15f,
                550f,
                Color.parseColor("#3300E5FF"),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(CARD_WIDTH * 0.85f, CARD_HEIGHT * 0.15f, 550f, cyanGlowPaint)

        // Glow aksen Emerald di pojok kiri tengah
        val emeraldGlowPaint = Paint().apply {
            isAntiAlias = true
            shader = RadialGradient(
                CARD_WIDTH * 0.15f, CARD_HEIGHT * 0.55f,
                500f,
                Color.parseColor("#2210B981"),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(CARD_WIDTH * 0.15f, CARD_HEIGHT * 0.55f, 500f, emeraldGlowPaint)
    }

    private fun drawHeader(canvas: Canvas, session: WorkoutSession) {
        val margin = 80f
        var currentY = 130f

        // Brand Title
        val brandPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#00E5FF")
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.2f
        }
        canvas.drawText("STRIDEBEAT", margin, currentY, brandPaint)

        // Activity Badge
        val badgeText = "🏃 ${session.type.displayName.uppercase()}"
        val badgePaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#E0F2FE")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val badgeBgPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#2600E5FF")
            style = Paint.Style.FILL
        }
        val badgeBorderPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#6600E5FF")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        val textWidth = badgePaint.measureText(badgeText)
        val badgeRect = RectF(
            CARD_WIDTH - margin - textWidth - 40f,
            currentY - 38f,
            CARD_WIDTH - margin,
            currentY + 14f
        )
        canvas.drawRoundRect(badgeRect, 20f, 20f, badgeBgPaint)
        canvas.drawRoundRect(badgeRect, 20f, 20f, badgeBorderPaint)
        canvas.drawText(badgeText, badgeRect.left + 20f, currentY - 2f, badgePaint)

        currentY += 46f

        // Date & Time subtitle
        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy • HH:mm", Locale("id", "ID"))
        val dateStr = try {
            dateFormat.format(Date(session.startedAt))
        } catch (_: Exception) {
            "Sesi Olahraga"
        }
        val datePaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#94A3B8")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText(dateStr, margin, currentY, datePaint)
    }

    private fun drawRouteMap(canvas: Canvas, points: List<LatLng>) {
        val mapRect = RectF(80f, 240f, CARD_WIDTH - 80f, 760f)

        // Glass background card
        val cardBgPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#1A1E293B")
            style = Paint.Style.FILL
        }
        val cardBorderPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#3338BDF8")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRoundRect(mapRect, 36f, 36f, cardBgPaint)
        canvas.drawRoundRect(mapRect, 36f, 36f, cardBorderPaint)

        if (points.size >= 2) {
            var minLat = points.minOf { it.latitude }
            var maxLat = points.maxOf { it.latitude }
            var minLng = points.minOf { it.longitude }
            var maxLng = points.maxOf { it.longitude }

            val latSpan = (maxLat - minLat).coerceAtLeast(0.0001)
            val lngSpan = (maxLng - minLng).coerceAtLeast(0.0001)

            val padding = 60f
            val drawWidth = mapRect.width() - (padding * 2)
            val drawHeight = mapRect.height() - (padding * 2)

            val path = Path()
            val mappedPoints = points.map { pt ->
                val x = mapRect.left + padding + (((pt.longitude - minLng) / lngSpan) * drawWidth).toFloat()
                // Latitudes increase upwards, but screen coordinates increase downwards
                val y = mapRect.bottom - padding - (((pt.latitude - minLat) / latSpan) * drawHeight).toFloat()
                PointF(x, y)
            }

            path.moveTo(mappedPoints[0].x, mappedPoints[0].y)
            for (i in 1 until mappedPoints.size) {
                path.lineTo(mappedPoints[i].x, mappedPoints[i].y)
            }

            // Glow stroke
            val glowPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 18f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = Color.parseColor("#4400E5FF")
            }
            canvas.drawPath(path, glowPaint)

            // Main polyline stroke with gradient
            val linePaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 8f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                shader = LinearGradient(
                    mapRect.left, mapRect.top, mapRect.right, mapRect.bottom,
                    Color.parseColor("#00E5FF"),
                    Color.parseColor("#10B981"),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawPath(path, linePaint)

            // Start Dot (Green)
            val startPt = mappedPoints.first()
            val startPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#10B981")
            }
            val whiteDotPaint = Paint().apply {
                isAntiAlias = true
                color = Color.WHITE
            }
            canvas.drawCircle(startPt.x, startPt.y, 14f, startPaint)
            canvas.drawCircle(startPt.x, startPt.y, 6f, whiteDotPaint)

            // End Dot (Cyan / Amber)
            val endPt = mappedPoints.last()
            val endPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#00E5FF")
            }
            canvas.drawCircle(endPt.x, endPt.y, 14f, endPaint)
            canvas.drawCircle(endPt.x, endPt.y, 6f, whiteDotPaint)

        } else {
            // Elegant placeholder wave if no GPS points recorded
            val textPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#64748B")
                textSize = 30f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            canvas.drawText("Rute Olahraga", mapRect.centerX(), mapRect.centerY(), textPaint)
        }
    }

    private fun drawMetrics(canvas: Canvas, session: WorkoutSession) {
        val startY = 800f
        val cardWidth = (CARD_WIDTH - 160f - 24f) / 2f
        val cardHeight = 170f

        val distKm = session.distanceMeters / 1000.0
        val distStr = String.format(Locale.US, "%.2f", distKm)
        val durStr = TrackingUtils.formatDuration(session.durationMs)
        val paceStr = TrackingUtils.formatPace(session.avgPaceMinPerKm)
        val calStr = "${session.caloriesKcal?.toInt() ?: 0}"

        // Row 1: Jarak & Durasi
        drawMetricCard(canvas, 80f, startY, cardWidth, cardHeight, "JARAK", distStr, "KM", isPrimary = true)
        drawMetricCard(canvas, 80f + cardWidth + 24f, startY, cardWidth, cardHeight, "DURASI", durStr, "")

        // Row 2: Pace & Kalori
        val row2Y = startY + cardHeight + 20f
        drawMetricCard(canvas, 80f, row2Y, cardWidth, cardHeight, "PACE RATA-RATA", paceStr, "/KM")
        drawMetricCard(canvas, 80f + cardWidth + 24f, row2Y, cardWidth, cardHeight, "KALORI", calStr, "KCAL")
    }

    private fun drawMetricCard(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        label: String,
        value: String,
        unit: String,
        isPrimary: Boolean = false
    ) {
        val rect = RectF(x, y, x + width, y + height)

        val bgPaint = Paint().apply {
            isAntiAlias = true
            color = if (isPrimary) Color.parseColor("#2600E5FF") else Color.parseColor("#151E293B")
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            isAntiAlias = true
            color = if (isPrimary) Color.parseColor("#6600E5FF") else Color.parseColor("#22475569")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(rect, 28f, 28f, bgPaint)
        canvas.drawRoundRect(rect, 28f, 28f, borderPaint)

        // Label
        val labelPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#94A3B8")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        canvas.drawText(label, x + 28f, y + 46f, labelPaint)

        // Value
        val valuePaint = Paint().apply {
            isAntiAlias = true
            color = if (isPrimary) Color.parseColor("#00E5FF") else Color.WHITE
            textSize = if (value.length > 5) 54f else 62f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(value, x + 28f, y + 128f, valuePaint)

        // Unit
        if (unit.isNotBlank()) {
            val valWidth = valuePaint.measureText(value)
            val unitPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#64748B")
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(unit, x + 34f + valWidth, y + 124f, unitPaint)
        }
    }

    private fun drawMusicSection(canvas: Canvas, session: WorkoutSession) {
        val y = 1200f
        val rect = RectF(80f, y, CARD_WIDTH - 80f, y + 250f)

        val bgPaint = Paint().apply {
            isAntiAlias = true
            shader = LinearGradient(
                rect.left, rect.top, rect.right, rect.bottom,
                Color.parseColor("#251E293B"),
                Color.parseColor("#150F172A"),
                Shader.TileMode.CLAMP
            )
        }
        val borderPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#3338BDF8")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(rect, 32f, 32f, bgPaint)
        canvas.drawRoundRect(rect, 32f, 32f, borderPaint)

        // Section Title: "🎵 SOUNDTRACK OF THIS RUN"
        val titlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#38BDF8")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.15f
        }
        canvas.drawText("🎵 SOUNDTRACK OF THIS RUN", rect.left + 32f, rect.top + 52f, titlePaint)

        val track = session.playedTracks.firstOrNull()
        if (track != null) {
            // Vinyl icon / circle
            val discPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#00E5FF")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(rect.left + 70f, rect.top + 145f, 36f, discPaint)

            val innerDisc = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#090D16")
            }
            canvas.drawCircle(rect.left + 70f, rect.top + 145f, 12f, innerDisc)

            // Track Title
            val trackTitlePaint = Paint().apply {
                isAntiAlias = true
                color = Color.WHITE
                textSize = 34f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val titleMax = 32
            val titleDisplay = if (track.title.length > titleMax) track.title.take(titleMax) + "..." else track.title
            canvas.drawText(titleDisplay, rect.left + 128f, rect.top + 135f, trackTitlePaint)

            // Artist & Source
            val artistPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#94A3B8")
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val sourceName = track.source.displayName
            val artistDisplay = "${track.artist} • via $sourceName"
            canvas.drawText(artistDisplay, rect.left + 128f, rect.top + 180f, artistPaint)

            // Songs count pill
            if (session.playedTracks.size > 1) {
                val moreCount = "+${session.playedTracks.size - 1} lagu lainnya"
                val morePaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.parseColor("#64748B")
                    textSize = 22f
                    textAlign = Paint.Align.RIGHT
                }
                canvas.drawText(moreCount, rect.right - 32f, rect.top + 52f, morePaint)
            }
        } else {
            val emptyPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#64748B")
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            canvas.drawText("Fokus berolahraga dengan irama langkah", rect.left + 32f, rect.top + 145f, emptyPaint)
        }
    }

    private fun drawFooter(canvas: Canvas) {
        val y = CARD_HEIGHT - 120f

        val linePaint = Paint().apply {
            color = Color.parseColor("#22334155")
            strokeWidth = 2f
        }
        canvas.drawLine(80f, y - 40f, CARD_WIDTH - 80f, y - 40f, linePaint)

        val footerBrandPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#F8FAFC")
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("StrideBeat", 80f, y + 10f, footerBrandPaint)

        val taglinePaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#64748B")
            textSize = 24f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("Run to the Rhythm • Stride with Soul", CARD_WIDTH - 80f, y + 10f, taglinePaint)
    }

    /**
     * Menyimpan bitmap story card ke cacheDir dan memicu Intent ACTION_SEND
     * untuk langsung dibagikan ke Instagram Stories, WhatsApp Status, dsb.
     */
    fun shareStoryCard(context: Context, session: WorkoutSession) {
        try {
            val bitmap = generateStoryBitmap(session)
            val cacheDir = File(context.cacheDir, "story_cards").apply { mkdirs() }
            val storyFile = File(cacheDir, "stridebeat_story_${session.id}.png")

            FileOutputStream(storyFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                storyFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Sesi Olahraga StrideBeat")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Lari ${String.format(Locale.US, "%.2f km", session.distanceMeters / 1000.0)} bersama StrideBeat! 🏃🎵"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Bagikan ke Instagram / WhatsApp"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
