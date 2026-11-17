package com.stridebeat.app.presentation.recap

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stridebeat.app.domain.model.MusicSource
import com.stridebeat.app.domain.model.PlayedTrack
import com.stridebeat.app.domain.model.StravaAuthState
import com.stridebeat.app.domain.model.StravaUploadStatus
import com.stridebeat.app.domain.model.WorkoutSession
import com.stridebeat.app.presentation.history.HistoryViewModel
import com.stridebeat.app.presentation.strava.ImportedFromStravaBadge
import com.stridebeat.app.presentation.strava.StravaStatusBadge
import com.stridebeat.app.presentation.strava.StravaViewModel
import com.stridebeat.app.presentation.strava.openOrCopyStravaActivityLink
import com.stridebeat.app.presentation.theme.*
import com.stridebeat.app.presentation.workout.components.RouteMapCanvas
import com.stridebeat.app.util.GpxExporter
import com.stridebeat.app.util.TrackingUtils
import java.util.Locale

@Composable
fun RecapScreen(
    session: WorkoutSession? = null,
    isJustFinished: Boolean = false,
    onDoneClick: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    historyViewModel: HistoryViewModel = hiltViewModel(),
    stravaViewModel: StravaViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val glass = StrideBeatGlass.colors
    val allSessions by historyViewModel.sessions.collectAsStateWithLifecycle()
    val liveFromDb = session?.let { s -> allSessions.find { it.id == s.id } }
    val currentSession = liveFromDb ?: session ?: allSessions.firstOrNull()

    val stravaAuthState by stravaViewModel.stravaAuthState.collectAsStateWithLifecycle()
    val stravaStatusMessage by stravaViewModel.statusMessage.collectAsStateWithLifecycle()
    val autoUploadNotice by stravaViewModel.autoUploadNotice.collectAsStateWithLifecycle()

    var showStravaPreviewDialog by remember { mutableStateOf(false) }
    var showStravaConnectPromptDialog by remember { mutableStateOf(false) }
    var showStoryPreviewDialog by remember { mutableStateOf(false) }
    var editActivityName by remember { mutableStateOf("") }
    var editActivityDescription by remember { mutableStateOf("") }

    // Simpan otomatis ke Room jika sesi baru selesai dan bukan dummy
    LaunchedEffect(session?.id) {
        session?.let {
            if (!it.id.startsWith("sample")) {
                historyViewModel.saveWorkoutSession(it)
            }
        }
    }

    // Evaluasi auto-upload Strava setelah sesi baru dipastikan tersimpan di Room (US-12)
    LaunchedEffect(isJustFinished, liveFromDb?.id) {
        if (isJustFinished && liveFromDb != null && !liveFromDb.id.startsWith("sample")) {
            stravaViewModel.tryAutoUploadIfEligible(liveFromDb)
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

    // Dialog Preview & Edit sebelum Upload Strava (PRD §7.1 US-11)
    if (showStravaPreviewDialog) {
        AlertDialog(
            onDismissRequest = { showStravaPreviewDialog = false },
            title = {
                Text(
                    text = "Preview Upload ke Strava",
                    style = MaterialTheme.typography.titleLarge,
                    color = glass.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (currentSession.routePoints.size >= 2)
                            "Rute GPX (${currentSession.routePoints.size} titik) beserta ringkasan & daftar lagu akan diunggah."
                        else
                            "Ringkasan aktivitas (tanpa rute GPX) beserta daftar lagu akan diunggah.",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )

                    OutlinedTextField(
                        value = editActivityName,
                        onValueChange = { editActivityName = it },
                        label = { Text("Nama Aktivitas") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StravaBrand,
                            focusedLabelColor = StravaBrand,
                            focusedTextColor = glass.textPrimary,
                            unfocusedTextColor = glass.textPrimary,
                            cursorColor = StravaBrand
                        )
                    )

                    OutlinedTextField(
                        value = editActivityDescription,
                        onValueChange = { editActivityDescription = it },
                        label = { Text("Deskripsi & Lagu") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StravaBrand,
                            focusedLabelColor = StravaBrand,
                            focusedTextColor = glass.textPrimary,
                            unfocusedTextColor = glass.textPrimary,
                            cursorColor = StravaBrand
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStravaPreviewDialog = false
                        stravaViewModel.uploadSession(
                            sessionId = currentSession.id,
                            customName = editActivityName,
                            customDescription = editActivityDescription
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StravaBrand,
                        contentColor = Color.White
                    )
                ) {
                    Text("Kirim ke Strava", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStravaPreviewDialog = false }) {
                    Text("Batal", color = glass.textSecondary)
                }
            },
            containerColor = glass.modalSurface,
            shape = RoundedCornerShape(22.dp)
        )
    }

    // Dialog saat Strava belum terhubung / butuh konfigurasi / token expired
    if (showStravaConnectPromptDialog) {
        val message = when (stravaAuthState) {
            is StravaAuthState.MissingConfig ->
                "STRAVA_CLIENT_ID / STRAVA_CLIENT_SECRET belum dikonfigurasi pada build ini. Kamu tetap bisa mengekspor file GPX manual."
            is StravaAuthState.TokenExpired ->
                "Sesi Strava kamu kedaluwarsa. Silakan login ulang di Pengaturan, atau ekspor file GPX manual."
            else ->
                "Akun Strava belum terhubung. Hubungkan di Pengaturan → Akun Terhubung untuk upload langsung, atau gunakan ekspor GPX manual."
        }
        AlertDialog(
            onDismissRequest = { showStravaConnectPromptDialog = false },
            title = {
                Text(
                    text = "Strava Belum Terhubung",
                    style = MaterialTheme.typography.titleLarge,
                    color = glass.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = glass.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStravaConnectPromptDialog = false
                        onNavigateToSettings()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StravaBrand,
                        contentColor = Color.White
                    )
                ) {
                    Text("Buka Pengaturan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showStravaConnectPromptDialog = false
                    try {
                        val shareIntent = GpxExporter.createShareGpxIntent(context, currentSession)
                        context.startActivity(Intent.createChooser(shareIntent, "Ekspor File GPX"))
                    } catch (_: Exception) {}
                }) {
                    Text("Ekspor GPX", color = glass.textSecondary)
                }
            },
            containerColor = glass.modalSurface,
            shape = RoundedCornerShape(22.dp)
        )
    }

    // Dialog Preview Story Card 9:16 (Feature D)
    if (showStoryPreviewDialog) {
        StoryCardPreviewDialog(
            session = currentSession,
            context = context,
            onDismiss = { showStoryPreviewDialog = false }
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSession.stravaActivityName?.takeIf { it.isNotBlank() } ?: "Recap Olahraga",
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

        // Strava Upload Status Card (US-11, US-12, US-13, US-14)
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 18.dp,
                accentBorderColor = when (currentSession.stravaUploadStatus) {
                    StravaUploadStatus.UPLOADED -> StravaBrand
                    StravaUploadStatus.FAILED -> StatusError
                    StravaUploadStatus.PENDING -> StatusWarning
                    StravaUploadStatus.NOT_UPLOADED -> null
                }
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (currentSession.isImportedFromStrava) {
                                ImportedFromStravaBadge()
                            }
                            StravaStatusBadge(
                                status = currentSession.stravaUploadStatus,
                                stravaActivityId = currentSession.stravaActivityId,
                                onClick = if (currentSession.stravaUploadStatus == StravaUploadStatus.UPLOADED) {
                                    { openOrCopyStravaActivityLink(context, currentSession.stravaActivityId) }
                                } else null
                            )
                        }

                        if (currentSession.stravaUploadStatus == StravaUploadStatus.UPLOADED &&
                            !currentSession.stravaActivityId.isNullOrBlank()
                        ) {
                            Text(
                                text = "Buka di Strava →",
                                style = MaterialTheme.typography.labelMedium,
                                color = StravaBrand,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    openOrCopyStravaActivityLink(context, currentSession.stravaActivityId)
                                }
                            )
                        }
                    }

                    val detailText = when (currentSession.stravaUploadStatus) {
                        StravaUploadStatus.UPLOADED ->
                            "Sudah diunggah ke Strava (ID: ${currentSession.stravaActivityId ?: "-"})."
                        StravaUploadStatus.PENDING ->
                            "Menunggu upload ke Strava. Jika offline, otomatis dikirim saat koneksi tersedia."
                        StravaUploadStatus.FAILED ->
                            "Belum diunggah — coba lagi. Data sesi tetap aman tersimpan lokal."
                        StravaUploadStatus.NOT_UPLOADED ->
                            "Belum diunggah ke Strava."
                    }

                    Text(
                        text = detailText,
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )

                    if (!autoUploadNotice.isNullOrBlank()) {
                        Text(
                            text = autoUploadNotice!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusWarning
                        )
                    }

                    if (!stravaStatusMessage.isNullOrBlank()) {
                        Text(
                            text = stravaStatusMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = StravaBrand
                        )
                    }
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

        // Action Buttons: Upload ke Strava, Bagikan Ringkasan, Ekspor GPX (cadangan), Selesai
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!currentSession.isImportedFromStrava &&
                    currentSession.stravaUploadStatus != StravaUploadStatus.UPLOADED
                ) {
                    val uploadLabel = when (currentSession.stravaUploadStatus) {
                        StravaUploadStatus.PENDING -> "Unggah Ulang ke Strava"
                        StravaUploadStatus.FAILED -> "Coba Lagi Upload ke Strava"
                        else -> "Upload ke Strava"
                    }
                    Button(
                        onClick = {
                            if (stravaAuthState is StravaAuthState.Connected) {
                                editActivityName = currentSession.stravaActivityName
                                    ?: stravaViewModel.buildDefaultActivityName(currentSession)
                                editActivityDescription = stravaViewModel.buildDefaultDescription(currentSession)
                                showStravaPreviewDialog = true
                            } else {
                                showStravaConnectPromptDialog = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .semantics { contentDescription = uploadLabel },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StravaBrand,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = uploadLabel, fontWeight = FontWeight.Bold)
                    }
                }

                // Bagikan ke Story 9:16 (Feature D)
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    elevated = true,
                    accentBorderColor = glass.primary,
                    onClick = { showStoryPreviewDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(glass.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = glass.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Bagikan Story Card (9:16)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = glass.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Kartu visual siap post ke Instagram & WhatsApp",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = glass.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                OutlinedButton(
                    onClick = { shareWorkoutSummary(context, currentSession) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, glass.primary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = glass.primary)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Bagikan Teks Ringkasan Lari", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        try {
                            val shareIntent = GpxExporter.createShareGpxIntent(context, currentSession)
                            context.startActivity(Intent.createChooser(shareIntent, "Ekspor File GPX untuk Strava"))
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, glass.divider),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = glass.textSecondary)
                ) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Ekspor File GPX", fontWeight = FontWeight.SemiBold)
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
    val (brandColor, brandLabel) = when (track.source) {
        MusicSource.SOUNDCLOUD -> SoundCloudBrand to "SoundCloud"
        MusicSource.YOUTUBE_MUSIC -> YouTubeBrand to "YouTube Music"
        MusicSource.SPOTIFY -> SpotifyBrand to "Spotify"
    }
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
                    text = brandLabel,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = brandColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

