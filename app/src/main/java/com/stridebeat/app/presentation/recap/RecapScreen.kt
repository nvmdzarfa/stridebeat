package com.stridebeat.app.presentation.recap

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stridebeat.app.domain.model.MusicSource
import com.stridebeat.app.domain.model.PlayedTrack
import com.stridebeat.app.domain.model.WorkoutSession
import com.stridebeat.app.presentation.history.HistoryViewModel
import com.stridebeat.app.presentation.theme.*
import com.stridebeat.app.presentation.workout.components.RouteMapCanvas
import com.stridebeat.app.util.GpxExporter
import com.stridebeat.app.util.TrackingUtils
import java.util.Locale

@Composable
fun RecapScreen(
    session: WorkoutSession? = null,
    onDoneClick: () -> Unit,
    historyViewModel: HistoryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val glass = StrideBeatGlass.colors
    val allSessions by historyViewModel.sessions.collectAsStateWithLifecycle()
    val currentSession = session ?: allSessions.firstOrNull()

    var showStravaDialog by remember { mutableStateOf(false) }

    // Simpan otomatis ke Room jika sesi baru selesai dan bukan dummy
    LaunchedEffect(currentSession?.id) {
        currentSession?.let {
            if (!it.id.startsWith("sample")) {
                historyViewModel.saveWorkoutSession(it)
            }
        }
    }

    if (currentSession == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Tidak Ada Sesi Olahraga",
                        style = MaterialTheme.typography.titleLarge,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Belum ada sesi olahraga yang tersimpan untuk ditampilkan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = glass.textSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    GlassPrimaryButton(
                        onClick = onDoneClick,
                        cornerRadius = 16.dp
                    ) {
                        Text("Kembali", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        return
    }

    if (showStravaDialog) {
        AlertDialog(
            onDismissRequest = { showStravaDialog = false },
            title = {
                Text(
                    text = "Upload ke Strava",
                    style = MaterialTheme.typography.titleLarge,
                    color = glass.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Kamu bisa mengekspor rute ini dalam format file GPX universal dan mengunggahnya secara manual ke Strava (https://www.strava.com/upload) atau aplikasi olahraga lainnya.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = glass.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStravaDialog = false
                        try {
                            val shareIntent = GpxExporter.createShareGpxIntent(context, currentSession)
                            context.startActivity(Intent.createChooser(shareIntent, "Ekspor File GPX untuk Strava"))
                        } catch (_: Exception) {}
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StravaBrand,
                        contentColor = Color.White
                    )
                ) {
                    Text("Ekspor File GPX", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStravaDialog = false }) {
                    Text("Tutup", color = glass.textSecondary)
                }
            },
            containerColor = glass.modalSurface,
            shape = RoundedCornerShape(22.dp)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Recap Olahraga",
                        style = MaterialTheme.typography.headlineMedium,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${currentSession.type.displayName} • ${String.format(Locale.US, "%.2f km", currentSession.distanceMeters / 1000.0)} selesai",
                        style = MaterialTheme.typography.bodyMedium,
                        color = glass.textSecondary
                    )
                }

                GlassPill(tint = glass.primary) {
                    Text(
                        text = "TERSIMPAN",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Live Route Map Canvas
        item {
            RouteMapCanvas(
                routePoints = currentSession.routePoints,
                isGpsInterrupted = currentSession.gpsInterrupted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            )
        }

        // Peringatan jika GPS sempat hilang
        if (currentSession.gpsInterrupted) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 14.dp,
                    accentBorderColor = StatusWarning
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Catatan: Sinyal GPS sempat terputus pada beberapa titik rute ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusWarning
                        )
                    }
                }
            }
        }

        // Metrics Grid Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 22.dp,
                elevated = true
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        RecapStat(
                            title = "JARAK",
                            value = String.format(Locale.US, "%.2f", currentSession.distanceMeters / 1000.0),
                            unit = "km"
                        )
                        RecapStat(
                            title = "DURASI",
                            value = TrackingUtils.formatDuration(currentSession.durationMs),
                            unit = ""
                        )
                    }

                    GlassDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        RecapStat(
                            title = "PACE RATA-RATA",
                            value = TrackingUtils.formatPace(currentSession.avgPaceMinPerKm),
                            unit = "/km"
                        )
                        RecapStat(
                            title = "KALORI (ESTIMASI)",
                            value = "${currentSession.caloriesKcal?.toInt() ?: 0}",
                            unit = "kcal"
                        )
                    }
                }
            }
        }

        // Section: Played Tracks Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lagu yang Menemanimu (${currentSession.playedTracks.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = glass.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Otomatis",
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.textMuted
                )
            }
        }

        // Track items
        items(currentSession.playedTracks) { track ->
            PlayedTrackItem(track = track)
        }

        // Action Buttons: Share, GPX, Strava, Done
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { shareWorkoutSummary(context, currentSession) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, glass.primary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = glass.primary)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Bagikan Ringkasan Lari", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { showStravaDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, StravaBrand),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StravaBrand)
                ) {
                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Ekspor GPX / Upload Strava", fontWeight = FontWeight.Bold)
                }

                GlassPrimaryButton(
                    onClick = onDoneClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    cornerRadius = 18.dp
                ) {
                    Text(
                        text = "Selesai & Simpan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun shareWorkoutSummary(context: Context, session: WorkoutSession) {
    val dist = String.format(Locale.US, "%.2f km", session.distanceMeters / 1000.0)
    val dur = TrackingUtils.formatDuration(session.durationMs)
    val pace = TrackingUtils.formatPace(session.avgPaceMinPerKm)
    val songsCount = session.playedTracks.size

    val text = """
        🏃 Sesi ${session.type.displayName} Selesai di StrideBeat!
        
        📍 Jarak: $dist
        ⏱ Waktu: $dur
        ⚡ Pace Rata-rata: $pace
        🔥 Kalori (estimasi): ${session.caloriesKcal?.toInt() ?: 0} kcal
        🎵 Lagu yang diputar: $songsCount lagu
        
        Dicatat dengan StrideBeat — Strava + Musik dalam Satu Aplikasi.
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Bagikan Hasil Lari"))
}

@Composable
private fun RecapStat(
    title: String,
    value: String,
    unit: String
) {
    val glass = StrideBeatGlass.colors
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = glass.textMuted
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                color = glass.primary,
                fontWeight = FontWeight.Bold
            )
            if (unit.isNotBlank()) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.titleMedium,
                    color = glass.textSecondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun PlayedTrackItem(track: PlayedTrack) {
    val glass = StrideBeatGlass.colors
    val brandColor = if (track.source == MusicSource.SPOTIFY) SpotifyBrand else YouTubeBrand
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                GlassPill(
                    tint = brandColor,
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = brandColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                }
            }

            GlassPill(tint = brandColor, shape = RoundedCornerShape(8.dp)) {
                Text(
                    text = if (track.source == MusicSource.SPOTIFY) "Spotify" else "Online",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = brandColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
