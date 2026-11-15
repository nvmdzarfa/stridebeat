package com.stridebeat.app.presentation.history

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stridebeat.app.domain.model.WorkoutSession
import com.stridebeat.app.domain.model.WorkoutType
import com.stridebeat.app.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    onSessionClick: (WorkoutSession) -> Unit,
    historyViewModel: HistoryViewModel = hiltViewModel()
) {
    val sessionList by historyViewModel.sessions.collectAsStateWithLifecycle()
    var sessionToDelete by remember { mutableStateOf<WorkoutSession?>(null) }

    // Dialog Konfirmasi Hapus Sesi (PRD §7.7)
    if (sessionToDelete != null) {
        val s = sessionToDelete!!
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = {
                Text(
                    text = "Hapus Sesi Ini?",
                    style = MaterialTheme.typography.titleLarge,
                    color = StatusError
                )
            },
            text = {
                Text(
                    text = "Data latihan ${s.type.displayName} tanggal ${SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date(s.startedAt))} akan dihapus dari penyimpanan offline perangkat ini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryDark
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        historyViewModel.deleteSession(s.id)
                        sessionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Ya, Hapus", color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Batal", color = TextSecondaryDark)
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Riwayat Olahraga",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "Tersimpan offline di Room Database (${sessionList.size} sesi)",
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
                        text = "ROOM OFFLINE",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = SportPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (sessionList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsRun,
                            contentDescription = null,
                            tint = TextMutedDark,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "Belum Ada Riwayat Olahraga",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Mulai olahraga pertamamu dari Beranda untuk mencatat rute dan lagu!",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark
                        )
                    }
                }
            }
        } else {
            items(sessionList, key = { it.id }) { session ->
                HistoryCardItem(
                    session = session,
                    onClick = { onSessionClick(session) },
                    onDelete = { sessionToDelete = session }
                )
            }
        }
    }
}

@Composable
private fun HistoryCardItem(
    session: WorkoutSession,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
    val dateString = dateFormat.format(Date(session.startedAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SportPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (session.type) {
                                WorkoutType.RUN -> Icons.Default.DirectionsRun
                                WorkoutType.WALK -> Icons.Default.DirectionsWalk
                                WorkoutType.RIDE -> Icons.Default.DirectionsBike
                            },
                            contentDescription = null,
                            tint = SportPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = session.type.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = dateString,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Hapus Sesi",
                            tint = TextMutedDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Detail",
                        tint = TextMutedDark,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Divider(color = DarkBorder)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = String.format(Locale.US, "%.2f km", session.distanceMeters / 1000.0),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Jarak",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMutedDark
                    )
                }

                Column {
                    val minutes = session.durationMs / 60000
                    val seconds = (session.durationMs % 60000) / 1000
                    Text(
                        text = "$minutes mnt",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Durasi",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMutedDark
                    )
                }

                Column {
                    val pace = session.avgPaceMinPerKm ?: 0.0
                    val paceMin = pace.toInt()
                    val paceSec = ((pace - paceMin) * 60).toInt()
                    Text(
                        text = if (pace > 0) String.format(Locale.US, "%d'%02d\"", paceMin, paceSec) else "--'--\"",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pace Rata-rata",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMutedDark
                    )
                }

                Column {
                    Text(
                        text = "${session.playedTracks.size} lagu",
                        style = MaterialTheme.typography.titleLarge,
                        color = SpotifyBrand,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Musik",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMutedDark
                    )
                }
            }
        }
    }
}
