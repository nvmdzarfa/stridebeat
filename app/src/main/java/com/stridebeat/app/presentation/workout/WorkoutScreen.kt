package com.stridebeat.app.presentation.workout

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.stridebeat.app.domain.model.MusicSource
import com.stridebeat.app.domain.model.WorkoutSession
import com.stridebeat.app.domain.model.WorkoutType
import com.stridebeat.app.presentation.theme.*
import com.stridebeat.app.presentation.workout.components.MusicPlayerBottomSheet
import com.stridebeat.app.presentation.workout.components.RouteMapCanvas
import com.stridebeat.app.util.TrackingUtils
import java.util.Locale

@Composable
fun WorkoutScreen(
    onFinishWorkout: (WorkoutSession) -> Unit,
    workoutViewModel: WorkoutViewModel = hiltViewModel(),
    musicViewModel: MusicViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val isTracking by workoutViewModel.isTracking.collectAsStateWithLifecycle()
    val isPaused by workoutViewModel.isPaused.collectAsStateWithLifecycle()
    val isAutoPaused by workoutViewModel.isAutoPaused.collectAsStateWithLifecycle()
    val durationMs by workoutViewModel.elapsedDurationMs.collectAsStateWithLifecycle()
    val distanceMeters by workoutViewModel.distanceMeters.collectAsStateWithLifecycle()
    val paceMinPerKm by workoutViewModel.currentPaceMinPerKm.collectAsStateWithLifecycle()
    val speedKmh by workoutViewModel.currentSpeedKmh.collectAsStateWithLifecycle()
    val calories by workoutViewModel.caloriesKcal.collectAsStateWithLifecycle()
    val routePoints by workoutViewModel.routePoints.collectAsStateWithLifecycle()
    val gpsInterrupted by workoutViewModel.gpsInterrupted.collectAsStateWithLifecycle()

    // Music State (M3)
    val activeSource by musicViewModel.activeSource.collectAsStateWithLifecycle()
    val currentTrack by musicViewModel.currentTrack.collectAsStateWithLifecycle()
    val isMusicPlaying by musicViewModel.isPlaying.collectAsStateWithLifecycle()
    val playbackPositionMs by musicViewModel.playbackPositionMs.collectAsStateWithLifecycle()
    val availablePlaylists by musicViewModel.availablePlaylists.collectAsStateWithLifecycle()
    val activePlaylist by musicViewModel.activePlaylist.collectAsStateWithLifecycle()
    val playedTracksInSession by musicViewModel.playedTracksInSession.collectAsStateWithLifecycle()
    val searchResults by musicViewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by musicViewModel.isSearching.collectAsStateWithLifecycle()

    var selectedWorkoutType by remember { mutableStateOf(WorkoutType.RUN) }
    var showFinishDialog by remember { mutableStateOf(false) }
    var showMusicSheet by remember { mutableStateOf(false) }

    // Permission launcher saat tombol "MULAI OLAHRAGA" ditekan dan izin belum ada
    val startWorkoutPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (fineLocationGranted && !isTracking) {
            workoutViewModel.startWorkout(context, selectedWorkoutType)
            musicViewModel.play()
        }
    }

    // Permission launcher saat pertama kali masuk tab Olahraga: HANYA minta izin agar GPS siap, TIDAK auto-start
    val initialPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Tidak melakukan apa-apa otomatis, layar persiapan tetap standby
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            initialPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Music Player Bottom Sheet (M3) dengan fitur pencarian & putar langsung
    if (showMusicSheet) {
        MusicPlayerBottomSheet(
            activeSource = activeSource,
            currentTrack = currentTrack,
            isPlaying = isMusicPlaying,
            playbackPositionMs = playbackPositionMs,
            availablePlaylists = availablePlaylists,
            activePlaylist = activePlaylist,
            playedTracks = playedTracksInSession,
            searchResults = searchResults,
            isSearching = isSearching,
            onTogglePlayPause = { musicViewModel.togglePlayPause() },
            onSkipNext = { musicViewModel.skipNext() },
            onSkipPrevious = { musicViewModel.skipPrevious() },
            onSeekTo = { posMs -> musicViewModel.seekTo(posMs) },
            onSwitchSource = { newSource -> musicViewModel.switchSource(newSource) },
            onSelectPlaylist = { playlist -> musicViewModel.selectPlaylist(playlist) },
            onPlayTrack = { track -> musicViewModel.playTrack(track) },
            onSearchOnline = { query -> musicViewModel.searchOnlineTracks(query) },
            onDismiss = { showMusicSheet = false }
        )
    }

    // Dialog Konfirmasi Selesai
    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = {
                Text(
                    text = "Selesaikan Sesi?",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimaryDark
                )
            },
            text = {
                Text(
                    text = "Rute GPS, durasi, jarak, dan ${playedTracksInSession.size} lagu yang diputar akan dirangkum ke layar Recap.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryDark
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFinishDialog = false
                        val session = workoutViewModel.finishWorkout(context)
                        val sessionWithMusic = session.copy(
                            musicSource = activeSource,
                            playedTracks = playedTracksInSession
                        )
                        onFinishWorkout(sessionWithMusic)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess)
                ) {
                    Text("Ya, Selesai", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text("Lanjut Olahraga", color = TextSecondaryDark)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (!isTracking) {
        // =========================================================================
        // LAYAR PERSIAPAN (PRE-WORKOUT) — Tidak langsung jalan sebelum tombol ditekan
        // Desain luas, modern, bernapas (tidak dempet), dan nyaman di semua ukuran layar
        // =========================================================================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Selector
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "PILIH AKTIVITAS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMutedDark,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WorkoutType.values().forEach { type ->
                        val isSelected = type == selectedWorkoutType
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp)
                                .clickable { selectedWorkoutType = type },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) SportPrimary.copy(alpha = 0.18f) else DarkSurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) SportPrimary else DarkBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (type) {
                                        WorkoutType.RUN -> Icons.Default.DirectionsRun
                                        WorkoutType.RIDE -> Icons.Default.DirectionsBike
                                        WorkoutType.WALK -> Icons.Default.DirectionsWalk
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) SportPrimary else TextMutedDark,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = type.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) SportPrimary else TextSecondaryDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // Map Box Preview (Ketinggian terukur dan proporsional)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
            ) {
                RouteMapCanvas(
                    routePoints = emptyList(),
                    isGpsInterrupted = false,
                    modifier = Modifier.fillMaxSize(),
                    isTrackingActive = false
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = DarkBackground.copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SportPrimary.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SportPrimary)
                        )
                        Text(
                            text = "GPS Siap",
                            style = MaterialTheme.typography.labelSmall,
                            color = SportPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Mini Music Bar di Pre-Workout (Spacious, album art icon, 2-baris rapi)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showMusicSheet = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
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
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!currentTrack?.coverUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = currentTrack?.coverUrl,
                                    contentDescription = currentTrack?.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            if (currentTrack?.coverUrl.isNullOrBlank()) {
                                Icon(
                                    imageVector = if (isMusicPlaying) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = SportPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = currentTrack?.title ?: "Cari Lagu Olahraga",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimaryDark,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentTrack?.let { "${it.artist} • Ketuk untuk ganti lagu" }
                                    ?: "Ketuk untuk cari & putar lagu favoritmu",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryDark,
                                maxLines = 1
                            )
                        }
                    }

                    FilledIconButton(
                        onClick = {
                            if (currentTrack != null) {
                                musicViewModel.togglePlayPause()
                            } else {
                                showMusicSheet = true
                            }
                        },
                        modifier = Modifier.size(42.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = SportPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (isMusicPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = DarkBackground,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Tombol MULAI OLAHRAGA (Floating Pill Modern dengan Margin Mewah)
            Button(
                onClick = {
                    val hasFine = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasFine) {
                        workoutViewModel.startWorkout(context, selectedWorkoutType)
                        musicViewModel.play()
                    } else {
                        startWorkoutPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SportPrimary,
                    contentColor = DarkBackground
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MULAI OLAHRAGA",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    } else {
        // =========================================================================
        // LAYAR WORKOUT AKTIF (SEDANG TRACKING)
        // =========================================================================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header: Type & Status
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
                        shape = RoundedCornerShape(8.dp),
                        color = SportPrimary.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SportPrimary.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = selectedWorkoutType.displayName.uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = SportPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // GPS & Status Indicator
                val statusColor = when {
                    isPaused -> StatusWarning
                    isAutoPaused -> StatusWarning
                    gpsInterrupted -> StatusWarning
                    else -> SportPrimary
                }
                val statusText = when {
                    isPaused -> "Dijeda"
                    isAutoPaused -> "Auto-Pause"
                    gpsInterrupted -> "Mencari GPS..."
                    else -> "GPS Aktif"
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Live Route Map Canvas
            RouteMapCanvas(
                routePoints = routePoints,
                isGpsInterrupted = gpsInterrupted,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Large Stats Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Large Duration Display
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = TrackingUtils.formatDuration(durationMs),
                            style = MaterialTheme.typography.displayMedium,
                            color = if (isPaused || isAutoPaused) StatusWarning else SportPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isAutoPaused) "DIJEDA OTOMATIS" else if (isPaused) "DIJEDA" else "WAKTU BERJALAN",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMutedDark,
                            letterSpacing = 1.sp
                        )
                    }

                    Divider(color = DarkBorder)

                    // Distance, Pace, Calories
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricItem(
                            value = String.format(Locale.US, "%.2f", distanceMeters / 1000.0),
                            unit = "km",
                            label = "Jarak"
                        )
                        MetricItem(
                            value = TrackingUtils.formatPace(paceMinPerKm),
                            unit = "",
                            label = "Pace"
                        )
                        MetricItem(
                            value = "${calories.toInt()}",
                            unit = "kcal",
                            label = "Kalori (estimasi)"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Controls: Pause/Resume + Selesai
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val pauseActive = isPaused || isAutoPaused
                OutlinedButton(
                    onClick = {
                        if (pauseActive) {
                            workoutViewModel.resumeWorkout(context)
                        } else {
                            workoutViewModel.pauseWorkout(context)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (pauseActive) SportPrimary else StatusWarning
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (pauseActive) SportPrimary else StatusWarning
                    )
                ) {
                    Icon(
                        imageVector = if (pauseActive) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (pauseActive) "Lanjut" else "Jeda",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Button(
                    onClick = { showFinishDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusError,
                        contentColor = TextPrimaryDark
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Selesai",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive Mini-Player at bottom
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showMusicSheet = true },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (activeSource == MusicSource.SPOTIFY) SpotifyBrand.copy(alpha = 0.5f) else YouTubeBrand.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!currentTrack?.coverUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = currentTrack?.coverUrl,
                                    contentDescription = currentTrack?.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            if (currentTrack?.coverUrl.isNullOrBlank()) {
                                Icon(
                                    imageVector = if (isMusicPlaying) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = if (activeSource == MusicSource.SPOTIFY) SpotifyBrand else YouTubeBrand,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = currentTrack?.title ?: "Pilih Lagu Workout",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimaryDark,
                                maxLines = 1
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = currentTrack?.artist ?: "Tap untuk cari lagu",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondaryDark,
                                    maxLines = 1
                                )
                                Text(text = "•", color = TextMutedDark, fontSize = 11.sp)
                                Text(
                                    text = if (activeSource == MusicSource.SPOTIFY) "Spotify" else "YouTube Music",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (activeSource == MusicSource.SPOTIFY) SpotifyBrand else YouTubeBrand,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { musicViewModel.skipPrevious() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Sebelumnya", tint = TextPrimaryDark)
                        }

                        IconButton(
                            onClick = { musicViewModel.togglePlayPause() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                if (isMusicPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = if (activeSource == MusicSource.SPOTIFY) SpotifyBrand else YouTubeBrand
                            )
                        }

                        IconButton(
                            onClick = { musicViewModel.skipNext() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Berikutnya", tint = TextPrimaryDark)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricItem(
    value: String,
    unit: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimaryDark,
                fontWeight = FontWeight.Bold
            )
            if (unit.isNotBlank()) {
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryDark,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMutedDark
        )
    }
}
