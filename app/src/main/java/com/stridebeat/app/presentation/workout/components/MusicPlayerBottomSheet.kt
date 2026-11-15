package com.stridebeat.app.presentation.workout.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stridebeat.app.domain.model.MusicSource
import com.stridebeat.app.domain.model.PlayedTrack
import com.stridebeat.app.domain.model.Track
import com.stridebeat.app.domain.model.WorkoutPlaylist
import com.stridebeat.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerBottomSheet(
    activeSource: MusicSource,
    currentTrack: Track?,
    isPlaying: Boolean,
    availablePlaylists: List<WorkoutPlaylist>,
    activePlaylist: WorkoutPlaylist?,
    playedTracks: List<PlayedTrack>,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSwitchSource: (MusicSource) -> Unit,
    onSelectPlaylist: (WorkoutPlaylist) -> Unit,
    onPlayTrack: (Track) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val allTracks = remember(availablePlaylists) {
        availablePlaylists.flatMap { it.tracks }.distinctBy { it.id }
    }

    val displayedTracks = remember(searchQuery, activePlaylist, allTracks) {
        if (searchQuery.isBlank()) {
            activePlaylist?.tracks ?: allTracks
        } else {
            allTracks.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.artist.contains(searchQuery, ignoreCase = true) ||
                        (it.album?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = DarkBorder) },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Source Switcher Tabs
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "SUMBER MUSIK AKTIF",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMutedDark,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceElevated)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Spotify Tab
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSwitchSource(MusicSource.SPOTIFY) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (activeSource == MusicSource.SPOTIFY) SpotifyBrand.copy(alpha = 0.25f) else Color.Transparent,
                            border = if (activeSource == MusicSource.SPOTIFY) androidx.compose.foundation.BorderStroke(1.dp, SpotifyBrand) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = if (activeSource == MusicSource.SPOTIFY) SpotifyBrand else TextMutedDark,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Spotify",
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeSource == MusicSource.SPOTIFY) SpotifyBrand else TextSecondaryDark,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // YouTube Music Tab
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSwitchSource(MusicSource.YOUTUBE_MUSIC) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (activeSource == MusicSource.YOUTUBE_MUSIC) YouTubeBrand.copy(alpha = 0.25f) else Color.Transparent,
                            border = if (activeSource == MusicSource.YOUTUBE_MUSIC) androidx.compose.foundation.BorderStroke(1.dp, YouTubeBrand) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = if (activeSource == MusicSource.YOUTUBE_MUSIC) YouTubeBrand else TextMutedDark,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "YouTube Music",
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeSource == MusicSource.YOUTUBE_MUSIC) YouTubeBrand else TextSecondaryDark,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar untuk mencari lagu langsung di aplikasi
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated,
                        focusedBorderColor = SportPrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark,
                        cursorColor = SportPrimary
                    ),
                    placeholder = {
                        Text(
                            text = "Cari judul, artis, beat BPM (EDM, Phonk, Rock)...",
                            fontSize = 12.sp,
                            color = TextMutedDark
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = SportPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Hapus",
                                    tint = TextMutedDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true
                )
            }

            // Now Playing Card (Live Player di dalam aplikasi)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (activeSource == MusicSource.SPOTIFY) SpotifyBrand.copy(alpha = 0.5f) else YouTubeBrand.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Album Art Cover Badge
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (activeSource == MusicSource.SPOTIFY) SpotifyBrand.copy(alpha = 0.2f) else YouTubeBrand.copy(alpha = 0.2f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = if (activeSource == MusicSource.SPOTIFY) SpotifyBrand else YouTubeBrand,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Info Judul & Status Putar
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = currentTrack?.title ?: "Pilih Lagu Workout",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimaryDark,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${currentTrack?.artist ?: "StrideBeat Audio"} • Putar Langsung di HP",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryDark,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Tombol Playback (Prev, Play/Pause, Next)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            IconButton(
                                onClick = onSkipPrevious,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Sebelumnya",
                                    tint = TextPrimaryDark,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            FilledIconButton(
                                onClick = onTogglePlayPause,
                                modifier = Modifier.size(56.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = if (activeSource == MusicSource.SPOTIFY) SpotifyBrand else YouTubeBrand
                                )
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = TextPrimaryDark,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            IconButton(
                                onClick = onSkipNext,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Berikutnya",
                                    tint = TextPrimaryDark,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Playlist Selection Pills
            item {
                Text(
                    text = "PLAYLIST OLAHRAGA",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMutedDark,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availablePlaylists.forEach { pl ->
                        val isSel = pl.id == activePlaylist?.id
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelectPlaylist(pl) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) SportPrimary.copy(alpha = 0.2f) else DarkSurfaceElevated,
                            border = if (isSel) androidx.compose.foundation.BorderStroke(1.dp, SportPrimary) else null
                        ) {
                            Text(
                                text = pl.name.take(24) + if (pl.name.length > 24) "..." else "",
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSel) SportPrimary else TextSecondaryDark,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Daftar Lagu (Bisa langsung di-tap untuk putar audio langsung di aplikasi)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "DAFTAR LAGU DALAM PLAYLIST" else "HASIL PENCARIAN LAGU (${displayedTracks.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMutedDark,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap untuk putar",
                        style = MaterialTheme.typography.labelSmall,
                        color = SportPrimary,
                        fontSize = 11.sp
                    )
                }
            }

            items(displayedTracks) { track ->
                val isCurrent = track.id == currentTrack?.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlayTrack(track) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) DarkSurfaceElevated else DarkSurface
                    ),
                    border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.dp, SportPrimary.copy(alpha = 0.6f)) else null
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
                                        if (isCurrent && isPlaying) SportPrimary else DarkSurfaceElevated
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isCurrent && isPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = if (isCurrent && isPlaying) DarkBackground else TextPrimaryDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isCurrent) SportPrimary else TextPrimaryDark,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${track.artist} • ${track.album ?: "Workout"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondaryDark,
                                    maxLines = 1
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (activeSource == MusicSource.SPOTIFY) SpotifyBrand.copy(alpha = 0.15f) else YouTubeBrand.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (activeSource == MusicSource.SPOTIFY) "Spotify" else "YouTube",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (activeSource == MusicSource.SPOTIFY) SpotifyBrand else YouTubeBrand,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
