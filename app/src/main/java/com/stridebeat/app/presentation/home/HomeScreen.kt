package com.stridebeat.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stridebeat.app.domain.model.WorkoutSession
import com.stridebeat.app.domain.model.WorkoutType
import com.stridebeat.app.presentation.theme.*
import com.stridebeat.app.util.TrackingUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onStartWorkoutClick: () -> Unit,
    onViewLastSessionClick: (WorkoutSession?) -> Unit,
    onNavigateToSettings: () -> Unit,
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val weekly = uiState.weeklyStats
    val lastSession = uiState.lastSession

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header: Greeting + Subtitle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Halo, Pelari!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimaryDark
                )
                Text(
                    text = "Siap taklukkan rute dan ritme hari ini?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryDark
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceElevated)
                    .border(1.dp, SportPrimary.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsRun,
                    contentDescription = "Profil",
                    tint = SportPrimary
                )
            }
        }

        // Account status quick indicators
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToSettings() },
            shape = RoundedCornerShape(12.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = StatusWarning,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Kelola Akun Musik & Strava",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimaryDark
                    )
                }
                Text(
                    text = "Atur",
                    style = MaterialTheme.typography.labelLarge,
                    color = SportPrimary
                )
            }
        }

        // Weekly Summary Card (Real data from Room Database)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ringkasan Minggu Ini",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimaryDark
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SummaryMetric(
                        value = String.format(Locale.US, "%.1f", weekly.totalDistanceKm),
                        unit = "km",
                        label = "Total Jarak"
                    )
                    SummaryMetric(
                        value = "${weekly.totalDurationMinutes}",
                        unit = "mnt",
                        label = "Total Waktu"
                    )
                    SummaryMetric(
                        value = "${weekly.sessionCount}",
                        unit = "sesi",
                        label = "Aktivitas"
                    )
                }
            }
        }

        // Primary Action: Big "Mulai Olahraga" CTA
        Button(
            onClick = onStartWorkoutClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SportPrimary,
                contentColor = DarkBackground
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = DarkBackground
                )
                Text(
                    text = "MULAI OLAHRAGA",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = DarkBackground
                )
            }
        }

        // Last Session Section
        Text(
            text = "Sesi Terakhir",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimaryDark
        )

        if (lastSession != null) {
            val sportIcon = when (lastSession.type) {
                WorkoutType.RUN -> Icons.Default.DirectionsRun
                WorkoutType.WALK -> Icons.Default.DirectionsWalk
                WorkoutType.BIKE -> Icons.Default.DirectionsBike
            }
            val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(lastSession.startedAt))
            val durationMinutes = (lastSession.durationMs / (60 * 1000L)).coerceAtLeast(1)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onViewLastSessionClick(lastSession) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = SportPrimary.copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = sportIcon,
                                        contentDescription = null,
                                        tint = SportPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = lastSession.type.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimaryDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "$dateStr • $durationMinutes menit",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondaryDark
                                )
                            }
                        }
                    }

                    Divider(color = DarkBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = String.format(Locale.US, "%.2f km", lastSession.distanceMeters / 1000.0),
                                style = MaterialTheme.typography.headlineSmall,
                                color = TextPrimaryDark,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Jarak",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMutedDark
                            )
                        }

                        Column {
                            Text(
                                text = TrackingUtils.formatPace(lastSession.avgPaceMinPerKm),
                                style = MaterialTheme.typography.headlineSmall,
                                color = TextPrimaryDark,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Pace Rata-rata",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMutedDark
                            )
                        }

                        Column {
                            Text(
                                text = "${lastSession.caloriesKcal?.toInt() ?: 0} kcal",
                                style = MaterialTheme.typography.headlineSmall,
                                color = TextPrimaryDark,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Kalori (estimasi)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMutedDark
                            )
                        }
                    }

                    // Music track preview
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceElevated)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = SportPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (lastSession.playedTracks.isNotEmpty()) {
                                "Diputar: ${lastSession.playedTracks.size} lagu selama olahraga"
                            } else {
                                "Tanpa riwayat musik"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryDark,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Buka Detail",
                            tint = TextMutedDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        } else {
            // Clean empty state card when user has not recorded any workouts yet
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsRun,
                        contentDescription = null,
                        tint = SportPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "Belum Ada Sesi Olahraga",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "Tekan tombol MULAI OLAHRAGA di atas untuk mulai mencatat rute, kecepatan, dan musik kamu!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryDark,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    value: String,
    unit: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.Start) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = SportPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = unit,
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondaryDark,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMutedDark
        )
    }
}
