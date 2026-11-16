package com.stridebeat.app.presentation.history

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
    val glass = StrideBeatGlass.colors
    var sessionToDelete by remember { mutableStateOf<WorkoutSession?>(null) }

    // Dialog Konfirmasi Hapus Sesi
    if (sessionToDelete != null) {
        val s = sessionToDelete!!
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = {
                Text(
                    text = "Hapus Sesi Ini?",
                    style = MaterialTheme.typography.titleLarge,
                    color = StatusError,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Data latihan ${s.type.displayName} tanggal ${SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date(s.startedAt))} akan dihapus dari penyimpanan offline perangkat ini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = glass.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        historyViewModel.deleteSession(s.id)
                        sessionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusError,
                        contentColor = androidx.compose.ui.graphics.Color.White
                    )
                ) {
                    Text("Ya, Hapus", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Batal", color = glass.textSecondary)
                }
            },
            containerColor = glass.modalSurface,
            shape = RoundedCornerShape(22.dp)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 16.dp),
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
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tersimpan offline di perangkat (${sessionList.size} sesi)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = glass.textSecondary
                    )
                }

                GlassPill(tint = glass.primary) {
                    Text(
                        text = "OFFLINE",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (sessionList.isEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 22.dp
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
                            tint = glass.textMuted,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "Belum Ada Riwayat Olahraga",
                            style = MaterialTheme.typography.titleMedium,
                            color = glass.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Mulai olahraga pertamamu dari Beranda untuk mencatat rute dan lagu!",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
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
    val glass = StrideBeatGlass.colors
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
    val dateString = dateFormat.format(Date(session.startedAt))

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassPill(
                        tint = glass.primary,
                        shape = CircleShape,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = when (session.type) {
                                WorkoutType.RUN -> Icons.Default.DirectionsRun
                                WorkoutType.WALK -> Icons.Default.DirectionsWalk
                                WorkoutType.RIDE -> Icons.Default.DirectionsBike
                            },
                            contentDescription = null,
                            tint = glass.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = session.type.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = glass.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = dateString,
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Hapus Sesi",
                            tint = glass.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Detail",
                        tint = glass.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            GlassDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = String.format(Locale.US, "%.2f km", session.distanceMeters / 1000.0),
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Jarak",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.textMuted
                    )
                }

                Column {
                    val minutes = session.durationMs / 60000
                    Text(
                        text = "$minutes mnt",
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Durasi",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.textMuted
                    )
                }

                Column {
                    val pace = session.avgPaceMinPerKm ?: 0.0
                    val paceMin = pace.toInt()
                    val paceSec = ((pace - paceMin) * 60).toInt()
                    Text(
                        text = if (pace > 0) String.format(Locale.US, "%d'%02d\"", paceMin, paceSec) else "--'--\"",
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pace",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.textMuted
                    )
                }

                Column {
                    Text(
                        text = "${session.playedTracks.size} lagu",
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Musik",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.textMuted
                    )
                }
            }
        }
    }
}
