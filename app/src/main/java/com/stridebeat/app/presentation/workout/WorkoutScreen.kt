package com.stridebeat.app.presentation.workout

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.stridebeat.app.domain.model.MusicSource
import com.stridebeat.app.domain.model.WorkoutSession
import com.stridebeat.app.domain.model.WorkoutTarget
import com.stridebeat.app.domain.model.WorkoutTargetType
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
    val glass = StrideBeatGlass.colors

    val isTracking by workoutViewModel.isTracking.collectAsStateWithLifecycle()
    val isPaused by workoutViewModel.isPaused.collectAsStateWithLifecycle()
    val isAutoPaused by workoutViewModel.isAutoPaused.collectAsStateWithLifecycle()
    val durationMs by workoutViewModel.elapsedDurationMs.collectAsStateWithLifecycle()
    val distanceMeters by workoutViewModel.distanceMeters.collectAsStateWithLifecycle()
    val paceMinPerKm by workoutViewModel.currentPaceMinPerKm.collectAsStateWithLifecycle()
    val calories by workoutViewModel.caloriesKcal.collectAsStateWithLifecycle()
    val routePoints by workoutViewModel.routePoints.collectAsStateWithLifecycle()
    val gpsInterrupted by workoutViewModel.gpsInterrupted.collectAsStateWithLifecycle()
    val selectedTarget by workoutViewModel.selectedTarget.collectAsStateWithLifecycle()
    val activeTarget by workoutViewModel.workoutTarget.collectAsStateWithLifecycle()

    // Music State
    val activeSource by musicViewModel.activeSource.collectAsStateWithLifecycle()
    val currentTrack by musicViewModel.currentTrack.collectAsStateWithLifecycle()
    val isMusicPlaying by musicViewModel.isPlaying.collectAsStateWithLifecycle()
    val playbackPositionMs by musicViewModel.playbackPositionMs.collectAsStateWithLifecycle()
    val availablePlaylists by musicViewModel.availablePlaylists.collectAsStateWithLifecycle()
    val activePlaylist by musicViewModel.activePlaylist.collectAsStateWithLifecycle()
    val playedTracksInSession by musicViewModel.playedTracksInSession.collectAsStateWithLifecycle()
    val searchResults by musicViewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by musicViewModel.isSearching.collectAsStateWithLifecycle()
    val youtubeMusicEnabled by musicViewModel.youtubeMusicEnabled.collectAsStateWithLifecycle()
    val playbackNotice by musicViewModel.playbackNotice.collectAsStateWithLifecycle()
    val unavailableExternalUrl by musicViewModel.unavailableExternalUrl.collectAsStateWithLifecycle()
    val isImportingPlaylist by musicViewModel.isImportingPlaylist.collectAsStateWithLifecycle()

    var selectedWorkoutType by remember { mutableStateOf(WorkoutType.RUN) }
    var showFinishDialog by remember { mutableStateOf(false) }
    var showMusicSheet by remember { mutableStateOf(false) }

    // Permission launcher saat tombol "MULAI OLAHRAGA" ditekan
    val startWorkoutPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (fineLocationGranted && !isTracking) {
            workoutViewModel.startWorkout(context, selectedWorkoutType, selectedTarget)
            musicViewModel.play()
        }
    }

    // Permission launcher saat pertama kali masuk tab Olahraga: HANYA minta izin agar GPS siap, TIDAK auto-start
    val initialPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

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
            youtubeMusicEnabled = youtubeMusicEnabled,
            playbackNotice = playbackNotice,
            unavailableExternalUrl = unavailableExternalUrl,
            onTogglePlayPause = { musicViewModel.togglePlayPause() },
            onSkipNext = { musicViewModel.skipNext() },
            onSkipPrevious = { musicViewModel.skipPrevious() },
            onSeekTo = { posMs -> musicViewModel.seekTo(posMs) },
            onSwitchSource = { newSource -> musicViewModel.switchSource(newSource) },
            onSelectPlaylist = { playlist -> musicViewModel.selectPlaylist(playlist) },
            onPlayTrack = { track -> musicViewModel.playTrack(track) },
            onSearchOnline = { query -> musicViewModel.searchOnlineTracks(query) },
            onDismissNotice = { musicViewModel.clearPlaybackNotice() },
            onPlayOnSoundCloud = { musicViewModel.tryPlayCurrentTrackOnSoundCloud() },
            isImportingPlaylist = isImportingPlaylist,
            onImportPlaylist = { urlOrId, cb -> musicViewModel.importYouTubePlaylist(urlOrId, cb) },
            onDeletePlaylist = { playlistId -> musicViewModel.deletePlaylist(playlistId) },
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
                    color = glass.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Rute GPS, durasi, jarak, dan ${playedTracksInSession.size} lagu yang diputar akan dirangkum ke layar Recap.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = glass.textSecondary
                )
            },
            confirmButton = {
                GlassPrimaryButton(
                    onClick = {
                        showFinishDialog = false
                        val session = workoutViewModel.finishWorkout(context)
                        val sessionWithMusic = session.copy(
                            musicSource = activeSource,
                            playedTracks = playedTracksInSession
                        )
                        onFinishWorkout(sessionWithMusic)
                    },
                    cornerRadius = 14.dp
                ) {
                    Text("Ya, Selesai", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text("Lanjut Olahraga", color = glass.textSecondary)
                }
            },
            containerColor = glass.modalSurface,
            shape = RoundedCornerShape(22.dp)
        )
    }

    if (!isTracking) {
        // =========================================================================
        // LAYAR PERSIAPAN (PRE-WORKOUT) — Glassmorphism
        // =========================================================================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Selector
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "PILIH AKTIVITAS",
                    style = MaterialTheme.typography.labelSmall,
                    color = glass.textMuted,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WorkoutType.values().forEach { type ->
                        val isSelected = type == selectedWorkoutType
                        GlassCard(
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp),
                            cornerRadius = 18.dp,
                            elevated = isSelected,
                            accentBorderColor = if (isSelected) glass.primary else null,
                            onClick = { selectedWorkoutType = type }
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
                                    tint = if (isSelected) glass.primary else glass.textMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = type.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) glass.primary else glass.textSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // TARGET LATIHAN (Feature B)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TARGET LATIHAN",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.textMuted,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (!selectedTarget.isActive) "Bebas"
                        else when (selectedTarget.type) {
                            WorkoutTargetType.DISTANCE -> "${String.format(Locale.US, "%.0f", selectedTarget.targetDistanceMeters / 1000.0)} km"
                            WorkoutTargetType.DURATION -> "${selectedTarget.targetDurationMs / 60000} mnt"
                            WorkoutTargetType.NONE -> "Bebas"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                val targetOptions = listOf(
                    WorkoutTarget() to "Bebas",
                    WorkoutTarget(WorkoutTargetType.DISTANCE, targetDistanceMeters = 1000.0) to "1 km",
                    WorkoutTarget(WorkoutTargetType.DISTANCE, targetDistanceMeters = 3000.0) to "3 km",
                    WorkoutTarget(WorkoutTargetType.DISTANCE, targetDistanceMeters = 5000.0) to "5 km",
                    WorkoutTarget(WorkoutTargetType.DISTANCE, targetDistanceMeters = 10000.0) to "10 km",
                    WorkoutTarget(WorkoutTargetType.DURATION, targetDurationMs = 15 * 60 * 1000L) to "15 mnt",
                    WorkoutTarget(WorkoutTargetType.DURATION, targetDurationMs = 30 * 60 * 1000L) to "30 mnt",
                    WorkoutTarget(WorkoutTargetType.DURATION, targetDurationMs = 45 * 60 * 1000L) to "45 mnt",
                    WorkoutTarget(WorkoutTargetType.DURATION, targetDurationMs = 60 * 60 * 1000L) to "60 mnt"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    targetOptions.forEach { (optTarget, label) ->
                        val isSelected = (optTarget.type == selectedTarget.type) &&
                            (optTarget.targetDistanceMeters == selectedTarget.targetDistanceMeters) &&
                            (optTarget.targetDurationMs == selectedTarget.targetDurationMs)

                        GlassCard(
                            modifier = Modifier.height(38.dp),
                            cornerRadius = 14.dp,
                            elevated = isSelected,
                            accentBorderColor = if (isSelected) glass.primary else null,
                            onClick = { workoutViewModel.selectTarget(optTarget) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) glass.primary else glass.textSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Map Box Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(235.dp)
            ) {
                RouteMapCanvas(
                    routePoints = emptyList(),
                    isGpsInterrupted = false,
                    modifier = Modifier.fillMaxSize(),
                    isTrackingActive = false
                )

                GlassPill(
                    tint = glass.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp),
                    shape = RoundedCornerShape(20.dp)
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
                                .background(glass.primary)
                        )
                        Text(
                            text = "GPS Siap",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Mini Music Bar di Pre-Workout
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                elevated = true,
                onClick = { showMusicSheet = true }
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
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(glass.glassElevatedTop),
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
                                    tint = glass.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = currentTrack?.title ?: "Cari Lagu Olahraga",
                                style = MaterialTheme.typography.bodyMedium,
                                color = glass.textPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentTrack?.let { "${it.artist} • Ketuk untuk ganti lagu" }
                                    ?: "Ketuk untuk cari & putar lagu favoritmu",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary,
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
                            containerColor = glass.primary,
                            contentColor = glass.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (isMusicPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Tombol MULAI OLAHRAGA
            GlassPrimaryButton(
                onClick = {
                    val hasFine = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasFine) {
                        workoutViewModel.startWorkout(context, selectedWorkoutType, selectedTarget)
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
                    .height(58.dp),
                cornerRadius = 24.dp
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
        // LAYAR WORKOUT AKTIF (SEDANG TRACKING) — Glassmorphism
        // =========================================================================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header: Type & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassPill(tint = glass.primary, shape = RoundedCornerShape(10.dp)) {
                    Text(
                        text = selectedWorkoutType.displayName.uppercase(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = glass.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                val statusColor = when {
                    isPaused || isAutoPaused || gpsInterrupted -> StatusWarning
                    else -> glass.primary
                }
                val statusText = when {
                    isPaused -> "Dijeda"
                    isAutoPaused -> "Auto-Pause"
                    gpsInterrupted -> "Mencari GPS..."
                    else -> "GPS Aktif"
                }

                GlassPill(tint = statusColor, shape = RoundedCornerShape(20.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
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
            }

            // Target Progress Card (Feature B)
            if (activeTarget.isActive) {
                val progress = activeTarget.calculateProgress(distanceMeters, durationMs)
                val progressPercent = (progress * 100).toInt()
                val targetDesc = when (activeTarget.type) {
                    WorkoutTargetType.DISTANCE -> "${String.format(Locale.US, "%.1f", activeTarget.targetDistanceMeters / 1000.0)} km"
                    WorkoutTargetType.DURATION -> "${activeTarget.targetDurationMs / 60000} mnt"
                    WorkoutTargetType.NONE -> ""
                }

                Spacer(modifier = Modifier.height(10.dp))
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    accentBorderColor = if (progress >= 1f) glass.primary else null
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (progress >= 1f) Icons.Default.CheckCircle else Icons.Default.Flag,
                                    contentDescription = null,
                                    tint = if (progress >= 1f) glass.primary else glass.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (progress >= 1f) "Target $targetDesc Tercapai! 🎉" else "Target: $targetDesc",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (progress >= 1f) glass.primary else glass.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "$progressPercent%",
                                style = MaterialTheme.typography.labelMedium,
                                color = glass.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = glass.primary,
                            trackColor = glass.glassElevatedTop
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 22.dp,
                elevated = true
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = TrackingUtils.formatDuration(durationMs),
                            style = MaterialTheme.typography.displayMedium,
                            color = if (isPaused || isAutoPaused) StatusWarning else glass.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isAutoPaused) "DIJEDA OTOMATIS" else if (isPaused) "DIJEDA" else "WAKTU BERJALAN",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textMuted,
                            letterSpacing = 1.sp
                        )
                    }

                    GlassDivider()

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
                            label = "Kalori (est.)"
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
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (pauseActive) glass.primary else StatusWarning
                    ),
                    border = BorderStroke(
                        1.5.dp,
                        if (pauseActive) glass.primary else StatusWarning
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
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusError,
                        contentColor = Color.White
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
            val (brandColor, brandName) = when (activeSource) {
                MusicSource.SOUNDCLOUD -> SoundCloudBrand to "SoundCloud"
                MusicSource.YOUTUBE_MUSIC -> YouTubeBrand to "YouTube Music"
                MusicSource.SPOTIFY -> SpotifyBrand to "Spotify"
            }
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 18.dp,
                elevated = true,
                accentBorderColor = brandColor.copy(alpha = 0.7f),
                onClick = { showMusicSheet = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
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
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(glass.glassElevatedTop),
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
                                    tint = brandColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = currentTrack?.title ?: "Pilih Lagu Workout",
                                style = MaterialTheme.typography.titleMedium,
                                color = glass.textPrimary,
                                maxLines = 1
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = currentTrack?.artist ?: "Tap untuk cari lagu",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.textSecondary,
                                    maxLines = 1
                                )
                                Text(text = "•", color = glass.textMuted, fontSize = 11.sp)
                                Text(
                                    text = brandName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = brandColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }


                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { musicViewModel.skipPrevious() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Sebelumnya", tint = glass.textPrimary)
                        }

                        IconButton(
                            onClick = { musicViewModel.togglePlayPause() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                if (isMusicPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = brandColor
                            )
                        }

                        IconButton(
                            onClick = { musicViewModel.skipNext() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Berikutnya", tint = glass.textPrimary)
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
    val glass = StrideBeatGlass.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = glass.textPrimary,
                fontWeight = FontWeight.Bold
            )
            if (unit.isNotBlank()) {
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.textSecondary,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = glass.textMuted
        )
    }
}
