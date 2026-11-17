package com.stridebeat.app.presentation.recap

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.stridebeat.app.domain.model.WorkoutSession
import com.stridebeat.app.presentation.theme.*
import com.stridebeat.app.presentation.workout.components.RouteMapCanvas
import com.stridebeat.app.util.TrackingUtils
import com.stridebeat.app.util.WorkoutStoryCardGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StoryCardPreviewDialog(
    session: WorkoutSession,
    context: Context,
    onDismiss: () -> Unit
) {
    val glass = StrideBeatGlass.colors

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF0F172A))
                .border(1.5.dp, glass.primary.copy(alpha = 0.4f), RoundedCornerShape(28.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Dialog
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = glass.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Story Card 9:16",
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = glass.textMuted
                    )
                }
            }

            // Preview Container (9:16 Ratio)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(390.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF090D16),
                                Color(0xFF0F172A),
                                Color(0xFF060A11)
                            )
                        )
                    )
                    .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(14.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top: Brand & Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "STRIDEBEAT",
                                style = MaterialTheme.typography.labelMedium,
                                color = glass.primary,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                            val dateStr = try {
                                SimpleDateFormat("d MMM yyyy • HH:mm", Locale("id", "ID")).format(Date(session.startedAt))
                            } catch (_: Exception) { "Sesi Olahraga" }
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.textMuted,
                                fontSize = 9.sp
                            )
                        }

                        GlassPill(tint = glass.primary, shape = RoundedCornerShape(8.dp)) {
                            Text(
                                text = "🏃 ${session.type.displayName.uppercase()}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Route Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B).copy(alpha = 0.4f))
                            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    ) {
                        RouteMapCanvas(
                            routePoints = session.routePoints,
                            isGpsInterrupted = session.gpsInterrupted,
                            modifier = Modifier.fillMaxSize(),
                            isTrackingActive = false
                        )
                    }

                    // Metrics 2x2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val distKm = session.distanceMeters / 1000.0
                        MiniMetric(
                            modifier = Modifier.weight(1f),
                            label = "JARAK",
                            value = String.format(Locale.US, "%.2f", distKm),
                            unit = "km",
                            isPrimary = true
                        )
                        MiniMetric(
                            modifier = Modifier.weight(1f),
                            label = "DURASI",
                            value = TrackingUtils.formatDuration(session.durationMs),
                            unit = ""
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MiniMetric(
                            modifier = Modifier.weight(1f),
                            label = "PACE",
                            value = TrackingUtils.formatPace(session.avgPaceMinPerKm),
                            unit = "/km"
                        )
                        MiniMetric(
                            modifier = Modifier.weight(1f),
                            label = "KALORI",
                            value = "${session.caloriesKcal?.toInt() ?: 0}",
                            unit = "kcal"
                        )
                    }

                    // Soundtrack preview
                    val firstTrack = session.playedTracks.firstOrNull()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E293B).copy(alpha = 0.6f))
                            .border(0.5.dp, Color(0xFF38BDF8).copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(glass.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = firstTrack?.title ?: "Fokus Berolahraga",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                fontSize = 11.sp
                            )
                            Text(
                                text = firstTrack?.let { "${it.artist} • ${it.source.displayName}" } ?: "StrideBeat Audio Engine",
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.textSecondary,
                                maxLines = 1,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }

            // Share Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, glass.divider),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = glass.textSecondary)
                ) {
                    Text("Tutup", fontWeight = FontWeight.SemiBold)
                }

                GlassPrimaryButton(
                    onClick = {
                        WorkoutStoryCardGenerator.shareStoryCard(context, session)
                    },
                    modifier = Modifier
                        .weight(1.8f)
                        .height(48.dp),
                    cornerRadius = 14.dp
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bagikan ke Story", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun MiniMetric(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    unit: String,
    isPrimary: Boolean = false
) {
    val glass = StrideBeatGlass.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPrimary) glass.primary.copy(alpha = 0.12f) else Color(0xFF1E293B).copy(alpha = 0.5f))
            .border(
                0.5.dp,
                if (isPrimary) glass.primary.copy(alpha = 0.35f) else Color(0xFF475569).copy(alpha = 0.2f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = glass.textMuted,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isPrimary) glass.primary else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                if (unit.isNotBlank()) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.textMuted,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}
