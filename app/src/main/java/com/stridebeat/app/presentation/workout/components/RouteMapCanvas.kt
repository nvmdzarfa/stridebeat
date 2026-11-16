package com.stridebeat.app.presentation.workout.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stridebeat.app.domain.model.LatLng
import com.stridebeat.app.presentation.theme.*

@Composable
fun RouteMapCanvas(
    routePoints: List<LatLng>,
    isGpsInterrupted: Boolean,
    modifier: Modifier = Modifier,
    isTrackingActive: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Gambar grid peta latar belakang bernuansa sporty dark
            val gridSpacing = 40.dp.toPx()
            var x = 0f
            while (x < width) {
                drawLine(
                    color = Color(0xFF26262A),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f
                )
                x += gridSpacing
            }
            var y = 0f
            while (y < height) {
                drawLine(
                    color = Color(0xFF26262A),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
                y += gridSpacing
            }

            // 2. Gambar rute polyline jika ada titik GPS
            if (routePoints.size >= 2) {
                var minLat = routePoints.minOf { it.latitude }
                var maxLat = routePoints.maxOf { it.latitude }
                var minLng = routePoints.minOf { it.longitude }
                var maxLng = routePoints.maxOf { it.longitude }

                // Berikan padding minimum agar tidak terpotong
                val latSpan = max(maxLat - minLat, 0.0005)
                val lngSpan = max(maxLng - minLng, 0.0005)

                val padding = 40f
                val drawWidth = width - (padding * 2)
                val drawHeight = height - (padding * 2)

                fun projectToCanvas(point: LatLng): Offset {
                    val nx = ((point.longitude - minLng) / lngSpan).toFloat()
                    val ny = 1f - ((point.latitude - minLat) / latSpan).toFloat()
                    return Offset(
                        x = padding + (nx * drawWidth),
                        y = padding + (ny * drawHeight)
                    )
                }

                val path = Path()
                val firstOffset = projectToCanvas(routePoints.first())
                path.moveTo(firstOffset.x, firstOffset.y)

                for (i in 1 until routePoints.size) {
                    val offset = projectToCanvas(routePoints[i])
                    path.lineTo(offset.x, offset.y)
                }

                // Gambar bayangan polyline (glow neon)
                drawPath(
                    path = path,
                    color = SportPrimary.copy(alpha = 0.3f),
                    style = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Garis rute utama hijau volt
                drawPath(
                    path = path,
                    color = SportPrimary,
                    style = Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Titik Mulai (Start Marker)
                drawCircle(
                    color = SportSecondary,
                    radius = 6f,
                    center = firstOffset
                )

                // Titik Terkini (Current Location Marker)
                val currentOffset = projectToCanvas(routePoints.last())
                drawCircle(
                    color = SportPrimary.copy(alpha = 0.35f),
                    radius = 16f,
                    center = currentOffset
                )
                drawCircle(
                    color = TextPrimaryDark,
                    radius = 7f,
                    center = currentOffset
                )
                drawCircle(
                    color = SportPrimary,
                    radius = 4f,
                    center = currentOffset
                )
            } else if (routePoints.size == 1) {
                // Satu titik awal
                val centerOffset = Offset(width / 2, height / 2)
                drawCircle(
                    color = SportPrimary.copy(alpha = 0.3f),
                    radius = 18f,
                    center = centerOffset
                )
                drawCircle(
                    color = SportPrimary,
                    radius = 6f,
                    center = centerOffset
                )
            }
        }

        // Overlay Status Sinyal GPS (hanya saat workout aktif)
        if (isTrackingActive) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isGpsInterrupted) StatusWarning.copy(alpha = 0.2f) else DarkSurface.copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isGpsInterrupted) StatusWarning.copy(alpha = 0.6f) else DarkBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = if (isGpsInterrupted) StatusWarning else SportPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isGpsInterrupted) "GPS Terputus" else "OpenStreetMap • GPS Aktif",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isGpsInterrupted) StatusWarning else SportPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Teks Bantuan jika belum ada titik polyline
        if (routePoints.isEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (isTrackingActive) "Menunggu Pergerakan..." else "Area Peta Live Tracking",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isTrackingActive) "Rute polyline live akan tergambar saat kamu mulai melangkah"
                    else "Rute latihan dan kecepatanmu akan terekam otomatis di sini",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMutedDark,
                    fontSize = 11.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

private fun max(a: Double, b: Double): Double = if (a > b) a else b
