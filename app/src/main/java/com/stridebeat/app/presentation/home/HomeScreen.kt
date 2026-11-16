package com.stridebeat.app.presentation.home

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
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
import com.stridebeat.app.data.local.AppThemeMode
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
    homeViewModel: HomeViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
    val glass = StrideBeatGlass.colors
    val systemDark = isSystemInDarkTheme()
    val isCurrentlyDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> systemDark
    }

    val weekly = uiState.weeklyStats
    val lastSession = uiState.lastSession

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header: Greeting + Theme Quick Toggle + Profile Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Halo, Pelari!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = glass.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Siap taklukkan rute dan ritme hari ini?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = glass.textSecondary
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1-Tap Dark/Light Switch
                GlassCard(
                    modifier = Modifier.size(44.dp),
                    cornerRadius = 22.dp,
                    elevated = true,
                    onClick = { themeViewModel.toggleDarkLight(isCurrentlyDark) }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCurrentlyDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Ganti Tema Gelap/Terang",
                            tint = if (isCurrentlyDark) StatusWarning else glass.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                GlassCard(
                    modifier = Modifier.size(44.dp),
                    cornerRadius = 22.dp,
                    elevated = true,
                    accentBorderColor = glass.primary
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsRun,
                            contentDescription = "Profil",
                            tint = glass.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Account status quick banner
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 18.dp,
            onClick = onNavigateToSettings
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = StatusWarning,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Kelola Akun Musik, Tema & Update",
                        style = MaterialTheme.typography.bodyMedium,
                        color = glass.textPrimary
                    )
                }
                GlassPill(tint = glass.primary) {
                    Text(
                        text = "Atur",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = glass.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Weekly Summary Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp,
            elevated = true
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ringkasan Minggu Ini",
                        style = MaterialTheme.typography.titleLarge,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    GlassPill(tint = glass.secondary) {
                        Text(
                            text = "7 HARI",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
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

        // Primary Action: Big "Mulai Olahraga" Gradient Glass CTA
        GlassPrimaryButton(
            onClick = onStartWorkoutClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp),
            cornerRadius = 22.dp
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "MULAI OLAHRAGA",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 0.8.sp
            )
        }

        // Last Session Section
        Text(
            text = "Sesi Terakhir",
            style = MaterialTheme.typography.titleLarge,
            color = glass.textPrimary,
            fontWeight = FontWeight.Bold
        )

        if (lastSession != null) {
            val sportIcon = when (lastSession.type) {
                WorkoutType.RUN -> Icons.Default.DirectionsRun
                WorkoutType.WALK -> Icons.Default.DirectionsWalk
                WorkoutType.RIDE -> Icons.Default.DirectionsBike
            }
            val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(lastSession.startedAt))
            val durationMinutes = (lastSession.durationMs / (60 * 1000L)).coerceAtLeast(1)

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 22.dp,
                onClick = { onViewLastSessionClick(lastSession) }
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GlassPill(
                                tint = glass.primary,
                                shape = CircleShape,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = sportIcon,
                                    contentDescription = null,
                                    tint = glass.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = lastSession.type.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = glass.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$dateStr • $durationMinutes menit",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.textSecondary
                                )
                            }
                        }
                    }

                    GlassDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = String.format(Locale.US, "%.2f km", lastSession.distanceMeters / 1000.0),
                                style = MaterialTheme.typography.titleLarge,
                                color = glass.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Jarak",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textMuted
                            )
                        }

                        Column {
                            Text(
                                text = TrackingUtils.formatPace(lastSession.avgPaceMinPerKm),
                                style = MaterialTheme.typography.titleLarge,
                                color = glass.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Pace Rata-rata",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textMuted
                            )
                        }

                        Column {
                            Text(
                                text = "${lastSession.caloriesKcal?.toInt() ?: 0} kcal",
                                style = MaterialTheme.typography.titleLarge,
                                color = glass.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Kalori",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textMuted
                            )
                        }
                    }

                    GlassPill(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = glass.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (lastSession.playedTracks.isNotEmpty()) {
                                    "Diputar: ${lastSession.playedTracks.size} lagu selama olahraga"
                                } else {
                                    "Tanpa riwayat musik"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Buka Detail",
                                tint = glass.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        } else {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 22.dp
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
                        tint = glass.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "Belum Ada Sesi Olahraga",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = glass.textPrimary
                    )
                    Text(
                        text = "Tekan tombol MULAI OLAHRAGA di atas untuk mulai mencatat rute, kecepatan, dan musik kamu!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = glass.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun SummaryMetric(
    value: String,
    unit: String,
    label: String
) {
    val glass = StrideBeatGlass.colors
    Column(horizontalAlignment = Alignment.Start) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = glass.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = unit,
                style = MaterialTheme.typography.titleMedium,
                color = glass.textSecondary,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = glass.textMuted
        )
    }
}
