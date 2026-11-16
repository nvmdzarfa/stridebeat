package com.stridebeat.app.presentation.recap

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Tidak Ada Sesi Olahraga",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimaryDark
                )
                Text(
                    text = "Belum ada sesi olahraga yang tersimpan untuk ditampilkan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryDark,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Button(
                    onClick = onDoneClick,
                    colors = ButtonDefaults.buttonColors(containerColor = SportPrimary)
                ) {
                    Text("Kembali", color = DarkBackground, fontWeight = FontWeight.Bold)
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
                    color = TextPrimaryDark
                )
            },
            text = {
                Text(
                    text = "Kamu bisa mengekspor rute ini dalam format GPX dan mengunggahnya secara gratis ke https://www.strava.com/upload tanpa membutuhkan langganan API berbayar.\n\nAtau hubungkan akun di Pengaturan jika kamu memiliki Strava API Key pribadi.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryDark
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFC4C02))
                ) {
                    Text("Ekspor File GPX", color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStravaDialog = false }) {
                    Text("Tutup", color = TextSecondaryDark)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
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
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "${currentSession.type.displayName} • ${String.format(Locale.US, "%.2f km", currentSession.distanceMeters / 1000.0)} selesai",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryDark
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SportPrimary.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SportPrimary.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "TERSIMPAN OFFLINE",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = SportPrimary,
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
                    .height(200.dp)
            )
        }

        // Peringatan jika GPS sempat hilang (PRD §7.6)
        if (currentSession.gpsInterrupted) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = StatusWarning.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusWarning.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
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

                    Divider(color = DarkBorder)

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
                    color = TextPrimaryDark
                )
                Text(
                    text = "Disinkronkan otomatis",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMutedDark
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
                // Share Summary Text Button
                OutlinedButton(
                    onClick = {
                        shareWorkoutSummary(context, currentSession)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, SportPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SportPrimary)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Bagikan Ringkasan Lari", fontWeight = FontWeight.Bold)
                }

                // Upload to Strava / Export GPX
                OutlinedButton(
                    onClick = { showStravaDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFC4C02)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFC4C02))
                ) {
                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Ekspor GPX / Upload Strava", fontWeight = FontWeight.Bold)
                }

                // Done & Back to Home
                Button(
                    onClick = onDoneClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SportPrimary,
                        contentColor = DarkBackground
                    )
                ) {
                    Text(
                        text = "Selesai & Simpan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DarkBackground
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
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = TextMutedDark
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                color = SportPrimary,
                fontWeight = FontWeight.Bold
            )
            if (unit.isNotBlank()) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondaryDark,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun PlayedTrackItem(track: PlayedTrack) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (track.source == MusicSource.SPOTIFY) SpotifyBrand.copy(alpha = 0.2f)
                            else YouTubeBrand.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = if (track.source == MusicSource.SPOTIFY) SpotifyBrand else YouTubeBrand,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (track.source == MusicSource.SPOTIFY) SpotifyBrand.copy(alpha = 0.15f) else YouTubeBrand.copy(alpha = 0.15f)
            ) {
                Text(
                    text = if (track.source == MusicSource.SPOTIFY) "Spotify" else "YouTube Music",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (track.source == MusicSource.SPOTIFY) SpotifyBrand else YouTubeBrand,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
