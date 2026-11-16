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
            // 1. Search Bar Luas & Responsif
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
                            text = "Ketik judul lagu atau artis favorit...",
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
                                .padding(vertical = 28.dp),
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
                                text = "Mencari musik online...",
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
                                .padding(vertical = 28.dp),
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
                            text = "HASIL PENCARIAN (${searchResults.size})",
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
                                        text = if (isCurrent && isPlaying) "Memutar" else "Putar",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
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
                // 2. Tampilan Normal Saat Tidak Mengetik Search
                // Card Now Playing Hero (atau Welcoming Card jika belum ada lagu)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (currentTrack != null) SportPrimary.copy(alpha = 0.4f) else DarkBorder
                        )
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
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(SportPrimary.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = SportPrimary,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = currentTrack.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimaryDark,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${currentTrack.artist} • Diputar Langsung",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondaryDark,
                                            maxLines = 1
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
                                            tint = TextPrimaryDark,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(20.dp))

                                    FilledIconButton(
                                        onClick = onTogglePlayPause,
                                        modifier = Modifier.size(54.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = SportPrimary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play/Pause",
                                            tint = DarkBackground,
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
                            } else {
                                // Status Belum Ada Lagu Diputar
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(SportPrimary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = SportPrimary,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = "Belum Ada Lagu Dipilih",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimaryDark,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Gunakan kolom pencarian di atas untuk memutar lagu",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondaryDark
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Rekomendasi / Saran Pencarian Cepat
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "INSPIRASI MUSIK WORKOUT",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMutedDark,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            quickChips.take(3).forEach { chip ->
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { searchQuery = chip },
                                    shape = RoundedCornerShape(12.dp),
                                    color = DarkSurfaceElevated,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                                ) {
                                    Text(
                                        text = chip,
                                        modifier = Modifier
                                            .padding(vertical = 10.dp, horizontal = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextPrimaryDark,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            quickChips.drop(3).take(3).forEach { chip ->
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { searchQuery = chip },
                                    shape = RoundedCornerShape(12.dp),
                                    color = DarkSurfaceElevated,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                                ) {
                                    Text(
                                        text = chip,
                                        modifier = Modifier
                                            .padding(vertical = 10.dp, horizontal = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextPrimaryDark,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Riwayat Lagu yang Baru Diputar (Jika ada)
                if (playedTracks.isNotEmpty()) {
                    item {
                        Text(
                            text = "BARU DIPUTAR (${playedTracks.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMutedDark,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    items(playedTracks.reversed()) { pt ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
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
                                },
                            shape = RoundedCornerShape(14.dp),
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
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = SportPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = pt.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimaryDark,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = pt.artist,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondaryDark,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Text(
                                    text = "Putar Lagi",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SportPrimary,
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
}
