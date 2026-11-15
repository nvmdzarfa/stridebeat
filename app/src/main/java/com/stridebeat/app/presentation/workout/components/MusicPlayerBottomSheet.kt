package com.stridebeat.app.presentation.workout.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stridebeat.app.domain.model.MusicSource
import com.stridebeat.app.domain.model.PlayedTrack
import com.stridebeat.app.domain.model.Track
import com.stridebeat.app.domain.model.WorkoutPlaylist
import com.stridebeat.app.presentation.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerBottomSheet(
    activeSource: MusicSource,
    currentTrack: Track?,
    isPlaying: Boolean,
    availablePlaylists: List<WorkoutPlaylist>,
    activePlaylist: WorkoutPlaylist?,
    playedTracks: List<PlayedTrack>,
    searchResults: List<Track>,
    isSearching: Boolean,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSwitchSource: (MusicSource) -> Unit,
    onSelectPlaylist: (WorkoutPlaylist) -> Unit,
    onPlayTrack: (Track) -> Unit,
    onSearchOnline: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    // Trigger search dengan debounce 350ms agar tidak spam API saat mengetik cepat
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            delay(350)
            onSearchOnline(searchQuery)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = DarkBorder,
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
            // 1. Search Bar Luas & Modern di bagian atas
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
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
                            text = "Cari lagu asli: The Weeknd, Kanye, Phonk...",
                            fontSize = 13.sp,
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
                                    tint = TextSecondaryDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true
                )
            }

            // Jika sedang mengetik pencarian
            if (searchQuery.isNotBlank()) {
                if (isSearching) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = SportPrimary,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Mencari lagu asli di cloud...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondaryDark
                            )
                        }
                    }
                } else if (searchResults.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Lagu tidak ditemukan untuk \"$searchQuery\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondaryDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Coba ketik nama artis atau judul lagu lain",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMutedDark
                            )
                        }
                    }
                } else {
                    item {
                        Text(
                            text = "HASIL PENCARIAN LAGU ASLI (${searchResults.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = SportPrimary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    items(searchResults) { track ->
                        val isCurrent = track.id == currentTrack?.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlayTrack(track) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) DarkSurfaceElevated else DarkSurface
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isCurrent) SportPrimary else DarkBorder
                            )
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
                                            .size(40.dp)
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
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = track.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isCurrent) SportPrimary else TextPrimaryDark,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${track.artist} • ${track.album ?: "Audio Asli"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondaryDark,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SportPrimary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Putar",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SportPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // 2. Tampilan Normal (Now Playing Hero yang Ringkas & Proporsional)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (activeSource == MusicSource.SPOTIFY) SpotifyBrand.copy(alpha = 0.4f) else YouTubeBrand.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Baris Info Lagu & Cover
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (activeSource == MusicSource.SPOTIFY) SpotifyBrand.copy(alpha = 0.25f) else YouTubeBrand.copy(alpha = 0.25f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = if (activeSource == MusicSource.SPOTIFY) SpotifyBrand else YouTubeBrand,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentTrack?.title ?: "Pilih Lagu Workout",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimaryDark,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${currentTrack?.artist ?: "StrideBeat Audio"} • Putar Langsung di HP",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondaryDark,
                                        maxLines = 1
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (activeSource == MusicSource.SPOTIFY) SpotifyBrand.copy(alpha = 0.2f) else YouTubeBrand.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = if (activeSource == MusicSource.SPOTIFY) "Spotify" else "YouTube",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (activeSource == MusicSource.SPOTIFY) SpotifyBrand else YouTubeBrand,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Tombol Media Playback
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
                                        tint = TextPrimaryDark,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(20.dp))

                                FilledIconButton(
                                    onClick = onTogglePlayPause,
                                    modifier = Modifier.size(54.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = if (activeSource == MusicSource.SPOTIFY) SpotifyBrand else YouTubeBrand
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = TextPrimaryDark,
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
                                        tint = TextPrimaryDark,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Sumber Musik & Playlist Tabs
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DAFTAR LAGU OLAHRAGA",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMutedDark,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Spotify Pill
                            Surface(
                                modifier = Modifier.clickable { onSwitchSource(MusicSource.SPOTIFY) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (activeSource == MusicSource.SPOTIFY) SpotifyBrand.copy(alpha = 0.25f) else Color.Transparent,
                                border = if (activeSource == MusicSource.SPOTIFY) androidx.compose.foundation.BorderStroke(1.dp, SpotifyBrand) else null
                            ) {
                                Text(
                                    text = "Spotify",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (activeSource == MusicSource.SPOTIFY) SpotifyBrand else TextSecondaryDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // YTM Pill
                            Surface(
                                modifier = Modifier.clickable { onSwitchSource(MusicSource.YOUTUBE_MUSIC) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (activeSource == MusicSource.YOUTUBE_MUSIC) YouTubeBrand.copy(alpha = 0.25f) else Color.Transparent,
                                border = if (activeSource == MusicSource.YOUTUBE_MUSIC) androidx.compose.foundation.BorderStroke(1.dp, YouTubeBrand) else null
                            ) {
                                Text(
                                    text = "YouTube Music",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (activeSource == MusicSource.YOUTUBE_MUSIC) YouTubeBrand else TextSecondaryDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Daftar Lagu Bawaan Playlist
                items(activePlaylist?.tracks ?: emptyList()) { track ->
                    val isCurrent = track.id == currentTrack?.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayTrack(track) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) DarkSurfaceElevated else DarkSurface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isCurrent) SportPrimary else DarkBorder
                        )
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
                                        .size(38.dp)
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
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${track.artist} • ${track.album ?: "Audio Asli"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondaryDark,
                                        maxLines = 1
                                    )
                                }
                            }

                            Text(
                                text = "Putar",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCurrent) SportPrimary else TextMutedDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }
}
