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
import androidx.compose.runtime.Composable
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
    onDismiss: () -> Unit
) {
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Source Switcher Tabs (PRD §1.1 & §7.5)
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

                        // YouTube Music Tab (Beta)
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
                                    imageVector = Icons.Default.MusicNote,
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

                    // Notice: Seamless Switch
                    Text(
                        text = "✓ Ganti sumber musik tidak akan memutus atau mereset GPS tracking lari kamu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SportPrimary,
                        fontSize = 11.sp
                    )
                }
            }

            // Current Track Big Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (activeSource == MusicSource.SPOTIFY) SpotifyBrand.copy(alpha = 0.4f) else YouTubeBrand.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Stylized Artwork
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (activeSource == MusicSource.SPOTIFY) SpotifyBrand.copy(alpha = 0.15f)
                                    else YouTubeBrand.copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = if (activeSource == MusicSource.SPOTIFY) SpotifyBrand else YouTubeBrand,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = currentTrack?.title ?: "Tidak ada lagu",
                                style = MaterialTheme.typography.headlineSmall,
                                color = TextPrimaryDark,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = currentTrack?.artist ?: "-",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondaryDark,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Playback Controls (Play/Pause, Next, Prev)
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
                                modifier = Modifier.size(58.dp),
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

            // Playlist Selection Section
            item {
                Text(
                    text = "PILIH PLAYLIST WORKOUT",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMutedDark,
                    fontWeight = FontWeight.Bold
                )
            }

            items(availablePlaylists) { playlist ->
                val isSelected = activePlaylist?.id == playlist.id
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPlaylist(playlist) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) DarkSurfaceElevated else DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) SportPrimary else DarkBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isSelected) SportPrimary else TextPrimaryDark,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${playlist.tracks.size} lagu • ${playlist.description}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryDark
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Aktif",
                                tint = SportPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Section: History Lagu yang Terputar Sesi Ini
            item {
                Text(
                    text = "LAGU TERCATAT SESI INI (${playedTracks.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMutedDark,
                    fontWeight = FontWeight.Bold
                )
            }

            items(playedTracks) { pt ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceElevated)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = pt.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = pt.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (pt.source == MusicSource.SPOTIFY) SpotifyBrand.copy(alpha = 0.2f) else YouTubeBrand.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (pt.source == MusicSource.SPOTIFY) "Spotify" else "YouTube Music",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (pt.source == MusicSource.SPOTIFY) SpotifyBrand else YouTubeBrand,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
