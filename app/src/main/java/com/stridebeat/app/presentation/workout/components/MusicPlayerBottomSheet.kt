package com.stridebeat.app.presentation.workout.components

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.stridebeat.app.domain.model.MusicSource
import com.stridebeat.app.domain.model.PlayedTrack
import com.stridebeat.app.domain.model.Track
import com.stridebeat.app.domain.model.WorkoutPlaylist
import com.stridebeat.app.presentation.theme.*
import kotlinx.coroutines.delay

private fun formatPlaybackTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerBottomSheet(
    activeSource: MusicSource,
    currentTrack: Track?,
    isPlaying: Boolean,
    playbackPositionMs: Long = 0L,
    availablePlaylists: List<WorkoutPlaylist>,
    activePlaylist: WorkoutPlaylist?,
    playedTracks: List<PlayedTrack>,
    searchResults: List<Track>,
    isSearching: Boolean,
    youtubeMusicEnabled: Boolean = true,
    playbackNotice: String? = null,
    unavailableExternalUrl: String? = null,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit = {},
    onSwitchSource: (MusicSource) -> Unit,
    onSelectPlaylist: (WorkoutPlaylist) -> Unit,
    onPlayTrack: (Track) -> Unit,
    onSearchOnline: (String) -> Unit,
    onDismissNotice: () -> Unit = {},
    onPlayOnSoundCloud: () -> Unit = {},
    isImportingPlaylist: Boolean = false,
    onImportPlaylist: (String, (Boolean, String?) -> Unit) -> Unit = { _, _ -> },
    onDeletePlaylist: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val glass = StrideBeatGlass.colors
    var searchQuery by remember { mutableStateOf("") }

    var showImportDialog by remember { mutableStateOf(false) }
    var importUrlInput by remember { mutableStateOf("") }
    var importFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var isImportSuccess by remember { mutableStateOf(false) }
    var playlistToDelete by remember { mutableStateOf<WorkoutPlaylist?>(null) }

    val quickChips = listOf(
        "Rex Orange County",
        "The Weeknd",
        "Phonk Workout",
        "Kanye West",
        "Running Beats",
        "Pop Hits"
    )

    // Debounce pencarian 350ms
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            delay(350)
            onSearchOnline(searchQuery)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = glass.modalSurface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = glass.divider,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 0. Active Source Switcher (SoundCloud ↔ YouTube Music Beta)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "SUMBER MUSIK AKTIF",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.textMuted,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isScActive = activeSource == MusicSource.SOUNDCLOUD
                        GlassCard(
                            modifier = Modifier.weight(1f),
                            cornerRadius = 14.dp,
                            elevated = isScActive,
                            accentBorderColor = if (isScActive) SoundCloudBrand else null,
                            onClick = { onSwitchSource(MusicSource.SOUNDCLOUD) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isScActive) SoundCloudBrand else glass.textMuted)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SoundCloud",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isScActive) glass.textPrimary else glass.textSecondary,
                                    fontWeight = if (isScActive) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }

                        if (youtubeMusicEnabled) {
                            val isYtmActive = activeSource == MusicSource.YOUTUBE_MUSIC
                            GlassCard(
                                modifier = Modifier.weight(1f),
                                cornerRadius = 14.dp,
                                elevated = isYtmActive,
                                accentBorderColor = if (isYtmActive) YouTubeBrand else null,
                                onClick = { onSwitchSource(MusicSource.YOUTUBE_MUSIC) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(if (isYtmActive) YouTubeBrand else glass.textMuted)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "YouTube Music",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isYtmActive) glass.textPrimary else glass.textSecondary,
                                        fontWeight = if (isYtmActive) FontWeight.Bold else FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Beta",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = YouTubeBrand,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Notice Banner jika terjadi penolakan stream / notice
            if (!playbackNotice.isNullOrBlank()) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        accentBorderColor = StatusWarning
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = StatusWarning,
                                        modifier = Modifier.size(18.dp).padding(top = 2.dp)
                                    )
                                    Text(
                                        text = playbackNotice,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = glass.textPrimary
                                    )
                                }
                                IconButton(
                                    onClick = onDismissNotice,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Tutup",
                                        tint = glass.textMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (activeSource == MusicSource.YOUTUBE_MUSIC) {
                                    TextButton(
                                        onClick = onPlayOnSoundCloud,
                                        colors = ButtonDefaults.textButtonColors(contentColor = SoundCloudBrand)
                                    ) {
                                        Text("Putar di SoundCloud", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                                if (!unavailableExternalUrl.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    TextButton(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(unavailableExternalUrl)).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = YouTubeBrand)
                                    ) {
                                        Text("Buka di YT Music", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 1. Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = glass.glassElevatedTop,
                        unfocusedContainerColor = glass.glassSurfaceTop,
                        focusedBorderColor = glass.primary,
                        unfocusedBorderColor = glass.divider,
                        focusedTextColor = glass.textPrimary,
                        unfocusedTextColor = glass.textPrimary,
                        cursorColor = glass.primary
                    ),
                    placeholder = {
                        Text(
                            text = if (activeSource == MusicSource.YOUTUBE_MUSIC) "Cari lagu di YouTube Music..." else "Cari lagu di SoundCloud...",
                            fontSize = 13.sp,
                            color = glass.textMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = glass.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Hapus",
                                    tint = glass.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true
                )
            }

            if (searchQuery.isNotBlank()) {
                if (isSearching) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = glass.primary,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Mencari musik online...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = glass.textSecondary
                            )
                        }
                    }
                } else if (searchResults.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Lagu tidak ditemukan untuk \"$searchQuery\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = glass.textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Coba ketik nama artis atau judul lagu lain",
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.textMuted
                            )
                        }
                    }
                } else {
                    item {
                        Text(
                            text = "HASIL PENCARIAN (${searchResults.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    items(searchResults) { track ->
                        val isCurrent = track.id == currentTrack?.id
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 16.dp,
                            elevated = isCurrent,
                            accentBorderColor = if (isCurrent) glass.primary else null,
                            onClick = { onPlayTrack(track) }
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
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(glass.glassElevatedTop),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!track.coverUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = track.coverUrl,
                                                contentDescription = track.title,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                        if (track.coverUrl.isNullOrBlank()) {
                                            Icon(
                                                imageVector = Icons.Default.MusicNote,
                                                contentDescription = null,
                                                tint = glass.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = track.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = glass.textPrimary,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${track.artist} • ${track.album}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = glass.textSecondary,
                                            maxLines = 1
                                        )
                                    }
                                }

                                if (isCurrent && isPlaying) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = "Playing",
                                        tint = glass.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Putar",
                                        tint = glass.textSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // 2. Sekarang Memutar
                item {
                    Text(
                        text = "SEDANG MEMUTAR",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.textMuted,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                item {
                    val activeColor = if (activeSource == MusicSource.SOUNDCLOUD) SoundCloudBrand else YouTubeBrand
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 22.dp,
                        elevated = true,
                        accentBorderColor = activeColor.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (currentTrack != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(glass.glassElevatedTop),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!currentTrack.coverUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = currentTrack.coverUrl,
                                                contentDescription = currentTrack.title,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                        if (currentTrack.coverUrl.isNullOrBlank()) {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                                                contentDescription = null,
                                                tint = activeColor,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = currentTrack.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = glass.textPrimary,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${currentTrack.artist} • ${activeSource.displayName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = glass.textSecondary,
                                            maxLines = 1
                                        )
                                    }
                                }

                                var isSeeking by remember { mutableStateOf(false) }
                                var seekFraction by remember { mutableFloatStateOf(0f) }

                                val duration = currentTrack.durationMs.let { if (it > 0) it else 180_000L }
                                val currentPosition = if (isSeeking) (seekFraction * duration).toLong() else playbackPositionMs.coerceIn(0L, duration)
                                val progress = if (duration > 0) (currentPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Slider(
                                        value = if (isSeeking) seekFraction else progress,
                                        onValueChange = { fraction ->
                                            isSeeking = true
                                            seekFraction = fraction
                                        },
                                        onValueChangeFinished = {
                                            isSeeking = false
                                            onSeekTo((seekFraction * duration).toLong())
                                        },
                                        colors = SliderDefaults.colors(
                                            thumbColor = activeColor,
                                            activeTrackColor = activeColor,
                                            inactiveTrackColor = glass.trackBarInactive
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(28.dp)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = formatPlaybackTime(currentPosition),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = glass.textSecondary,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = formatPlaybackTime(duration),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = glass.textMuted,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = onSkipPrevious,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SkipPrevious,
                                            contentDescription = "Sebelumnya",
                                            tint = glass.textPrimary,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(20.dp))

                                    FilledIconButton(
                                        onClick = onTogglePlayPause,
                                        modifier = Modifier.size(54.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = activeColor,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play/Pause",
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(20.dp))

                                    IconButton(
                                        onClick = onSkipNext,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SkipNext,
                                            contentDescription = "Berikutnya",
                                            tint = glass.textPrimary,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    GlassPill(
                                        tint = activeColor,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.size(50.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = activeColor,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = "Belum Ada Lagu Dipilih",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = glass.textPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Pilih playlist di bawah atau cari lagu favorit",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = glass.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Playlist Workout (Impor dari YouTube)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PLAYLIST WORKOUT",
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.textMuted,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            TextButton(
                                onClick = {
                                    importUrlInput = ""
                                    importFeedbackMessage = null
                                    isImportSuccess = false
                                    showImportDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = YouTubeBrand,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Impor YouTube",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = YouTubeBrand,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (availablePlaylists.isEmpty()) {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 18.dp,
                                onClick = {
                                    importUrlInput = ""
                                    importFeedbackMessage = null
                                    isImportSuccess = false
                                    showImportDialog = true
                                }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(glass.glassElevatedTop),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlaylistAdd,
                                            contentDescription = null,
                                            tint = YouTubeBrand,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Text(
                                        text = "Belum Ada Playlist Tersimpan",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = glass.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Tempel link atau ID playlist dari YouTube / YouTube Music untuk memutar puluhan lagu workout tanpa ribet.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = glass.textSecondary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    GlassPrimaryButton(
                                        onClick = {
                                            importUrlInput = ""
                                            importFeedbackMessage = null
                                            isImportSuccess = false
                                            showImportDialog = true
                                        },
                                        modifier = Modifier.height(38.dp),
                                        cornerRadius = 12.dp
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Impor Playlist Sekarang", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            availablePlaylists.forEach { playlist ->
                                val isActive = activePlaylist?.id == playlist.id
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    cornerRadius = 16.dp,
                                    elevated = isActive,
                                    accentBorderColor = if (isActive) glass.primary else null,
                                    onClick = { onSelectPlaylist(playlist) }
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
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(glass.glassElevatedTop),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (!playlist.coverUrl.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = playlist.coverUrl,
                                                        contentDescription = playlist.name,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.PlaylistPlay,
                                                        contentDescription = null,
                                                        tint = YouTubeBrand,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }

                                            Column {
                                                Text(
                                                    text = playlist.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = glass.textPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "${playlist.tracks.size} Lagu • Diimpor dari YouTube",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = glass.textSecondary,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { playlistToDelete = playlist },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Hapus Playlist",
                                                    tint = glass.textMuted,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            FilledIconButton(
                                                onClick = { onSelectPlaylist(playlist) },
                                                modifier = Modifier.size(36.dp),
                                                colors = IconButtonDefaults.filledIconButtonColors(
                                                    containerColor = if (isActive) glass.primary else glass.glassElevatedTop,
                                                    contentColor = if (isActive) glass.onPrimary else glass.textPrimary
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = if (isActive && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                    contentDescription = "Putar Playlist",
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Inspirasi Musik Workout
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "INSPIRASI MUSIK WORKOUT",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textMuted,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            quickChips.take(3).forEach { chip ->
                                GlassCard(
                                    modifier = Modifier.weight(1f),
                                    cornerRadius = 12.dp,
                                    onClick = { searchQuery = chip }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = chip,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = glass.textPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            quickChips.drop(3).take(3).forEach { chip ->
                                GlassCard(
                                    modifier = Modifier.weight(1f),
                                    cornerRadius = 12.dp,
                                    onClick = { searchQuery = chip }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = chip,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = glass.textPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Baru Diputar
                if (playedTracks.isNotEmpty()) {
                    item {
                        Text(
                            text = "BARU DIPUTAR (${playedTracks.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textMuted,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    items(playedTracks.reversed()) { pt ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 16.dp,
                            onClick = {
                                onPlayTrack(
                                    Track(
                                        id = pt.trackId,
                                        isrc = null,
                                        title = pt.title,
                                        artist = pt.artist,
                                        album = "Baru Diputar",
                                        durationMs = 210000L,
                                        coverUrl = null,
                                        audioUrl = null,
                                        platformIds = emptyMap()
                                    )
                                )
                            }
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
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = glass.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = pt.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = glass.textPrimary,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = pt.artist,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = glass.textSecondary,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Text(
                                    text = "Putar Lagi",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = glass.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isImportingPlaylist) {
                    showImportDialog = false
                    importFeedbackMessage = null
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistAdd,
                        contentDescription = null,
                        tint = YouTubeBrand,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Impor Playlist YouTube",
                        style = MaterialTheme.typography.titleLarge,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Masukkan URL atau ID playlist YouTube / YouTube Music. Semua lagu akan diambil dan disimpan ke playlist lokal Anda.",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )

                    OutlinedTextField(
                        value = importUrlInput,
                        onValueChange = {
                            importUrlInput = it
                            importFeedbackMessage = null
                            isImportSuccess = false
                        },
                        placeholder = {
                            Text("https://music.youtube.com/playlist?list=...", color = glass.textMuted, fontSize = 12.sp)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = YouTubeBrand,
                            unfocusedBorderColor = glass.cardBorder,
                            focusedTextColor = glass.textPrimary,
                            unfocusedTextColor = glass.textPrimary,
                            cursorColor = YouTubeBrand
                        ),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            if (importUrlInput.isNotBlank()) {
                                IconButton(onClick = {
                                    importUrlInput = ""
                                    importFeedbackMessage = null
                                    isImportSuccess = false
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Hapus", tint = glass.textMuted)
                                }
                            }
                        }
                    )

                    // Quick test chip
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Contoh:",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textMuted
                        )
                        AssistChip(
                            onClick = {
                                importUrlInput = "https://music.youtube.com/playlist?list=PLMC9KNkIncKtPzgY-5rmhvj7fax8fdxoj"
                                importFeedbackMessage = null
                                isImportSuccess = false
                            },
                            label = { Text("Pop Hits 100 Lagu", fontSize = 11.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = glass.glassElevatedTop,
                                labelColor = glass.primary
                            ),
                            border = null,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    if (isImportingPlaylist) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = YouTubeBrand,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Sedang mengambil data lagu dari YouTube...",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary
                            )
                        }
                    }

                    if (importFeedbackMessage != null) {
                        Text(
                            text = importFeedbackMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isImportSuccess) glass.primary else StatusWarning,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                GlassPrimaryButton(
                    onClick = {
                        if (isImportSuccess) {
                            showImportDialog = false
                            importFeedbackMessage = null
                            isImportSuccess = false
                        } else {
                            onImportPlaylist(importUrlInput) { success, msg ->
                                isImportSuccess = success
                                importFeedbackMessage = msg
                            }
                        }
                    },
                    modifier = Modifier.height(42.dp),
                    cornerRadius = 12.dp
                ) {
                    Text(
                        text = if (isImportSuccess) "Selesai" else if (isImportingPlaylist) "Mengimpor..." else "Impor Sekarang",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                if (!isImportingPlaylist) {
                    TextButton(onClick = {
                        showImportDialog = false
                        importFeedbackMessage = null
                        isImportSuccess = false
                    }) {
                        Text("Batal", color = glass.textSecondary)
                    }
                }
            },
            containerColor = glass.modalSurface,
            shape = RoundedCornerShape(22.dp)
        )
    }

    if (playlistToDelete != null) {
        val target = playlistToDelete!!
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            title = {
                Text(
                    text = "Hapus Playlist?",
                    style = MaterialTheme.typography.titleMedium,
                    color = glass.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Hapus \"${target.name}\" dari daftar playlist Anda?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = glass.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeletePlaylist(target.id)
                    playlistToDelete = null
                }) {
                    Text("Hapus", color = StatusWarning, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) {
                    Text("Batal", color = glass.textSecondary)
                }
            },
            containerColor = glass.modalSurface,
            shape = RoundedCornerShape(18.dp)
        )
    }
}
