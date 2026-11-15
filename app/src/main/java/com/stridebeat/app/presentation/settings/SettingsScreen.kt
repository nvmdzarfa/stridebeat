package com.stridebeat.app.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stridebeat.app.domain.model.SpotifyAuthState
import com.stridebeat.app.domain.model.StravaAuthState
import com.stridebeat.app.domain.model.YouTubeMusicAuthState
import com.stridebeat.app.presentation.theme.*

@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val stravaState by authViewModel.stravaState.collectAsStateWithLifecycle()
    val spotifyState by authViewModel.spotifyState.collectAsStateWithLifecycle()
    val ytmState by authViewModel.youtubeMusicState.collectAsStateWithLifecycle()

    var activeDialogPlatform by remember { mutableStateOf<String?>(null) }
    var showYtmInputModal by remember { mutableStateOf(false) }
    var ytmInputName by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isKmUnit by remember { mutableStateOf(true) }
    var isAutoPauseEnabled by remember { mutableStateOf(true) }

    // Dialog untuk Kredensial API yang belum diisi di local.properties
    if (activeDialogPlatform != null) {
        AlertDialog(
            onDismissRequest = { activeDialogPlatform = null },
            title = {
                Text(
                    text = "Konfigurasi $activeDialogPlatform",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimaryDark
                )
            },
            text = {
                val keyName = if (activeDialogPlatform == "Strava") "STRAVA_CLIENT_ID" else "SPOTIFY_CLIENT_ID"
                Text(
                    text = "Kredensial API $keyName belum diisi di file local.properties.\n\nSesuai PRD §6.5, salin templat dari local.properties.example dan isi Client ID kamu agar bisa melakukan login live.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryDark
                )
            },
            confirmButton = {
                Button(
                    onClick = { activeDialogPlatform = null },
                    colors = ButtonDefaults.buttonColors(containerColor = SportPrimary)
                ) {
                    Text("Mengerti", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal input untuk koneksi YouTube Music (Beta)
    if (showYtmInputModal) {
        AlertDialog(
            onDismissRequest = { showYtmInputModal = false },
            title = {
                Text(
                    text = "Hubungkan YouTube Music",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimaryDark
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Integrasi ini berstatus Beta / Tidak Resmi (PRD §6.3). Tidak meminta kata sandi Google pengguna.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark
                    )
                    OutlinedTextField(
                        value = ytmInputName,
                        onValueChange = { ytmInputName = it },
                        label = { Text("Nama Pengguna / Akun") },
                        placeholder = { Text("Contoh: Geiza") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = YouTubeBrand,
                            focusedLabelColor = YouTubeBrand,
                            cursorColor = YouTubeBrand
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = ytmInputName.ifBlank { "Pengguna YouTube Music" }
                        authViewModel.connectYouTubeMusic(name)
                        showYtmInputModal = false
                        ytmInputName = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = YouTubeBrand)
                ) {
                    Text("Simpan Sesi", color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showYtmInputModal = false }) {
                    Text("Batal", color = TextSecondaryDark)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Hapus Semua Sesi Akun?",
                    style = MaterialTheme.typography.titleLarge,
                    color = StatusError
                )
            },
            text = {
                Text(
                    text = "Semua token OAuth di penyimpanan terenkripsi akan dihapus dan status akun akan dikembalikan ke belum terhubung.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryDark
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        authViewModel.disconnectStrava()
                        authViewModel.disconnectSpotify()
                        authViewModel.disconnectYouTubeMusic()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Hapus Semua Token", color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Batal", color = TextSecondaryDark)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Title
        Column {
            Text(
                text = "Pengaturan",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimaryDark
            )
            Text(
                text = "Kelola akun terhubung dan preferensi workout",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryDark
            )
        }

        // Section: Akun Terhubung
        Text(
            text = "AKUN TERHUBUNG (M1)",
            style = MaterialTheme.typography.labelMedium,
            color = SportPrimary,
            fontWeight = FontWeight.Bold
        )

        // 1. STRAVA CARD
        StravaAccountCard(
            state = stravaState,
            onConnect = {
                if (stravaState is StravaAuthState.MissingConfig) {
                    activeDialogPlatform = "Strava"
                } else {
                    authViewModel.startStravaLogin(context)
                }
            },
            onDisconnect = { authViewModel.disconnectStrava() }
        )

        // 2. SPOTIFY CARD
        SpotifyAccountCard(
            state = spotifyState,
            onConnect = {
                if (spotifyState is SpotifyAuthState.MissingConfig) {
                    activeDialogPlatform = "Spotify"
                } else {
                    authViewModel.startSpotifyLogin(context)
                }
            },
            onDisconnect = { authViewModel.disconnectSpotify() }
        )

        // 3. YOUTUBE MUSIC CARD (BETA)
        YouTubeMusicAccountCard(
            state = ytmState,
            onConnect = { showYtmInputModal = true },
            onDisconnect = { authViewModel.disconnectYouTubeMusic() }
        )

        Divider(color = DarkBorder)

        // Section: Preferensi Tracking
        Text(
            text = "PREFERENSI WORKOUT",
            style = MaterialTheme.typography.labelMedium,
            color = SportPrimary,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Satuan Jarak",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = if (isKmUnit) "Kilometer (km)" else "Mil (mi)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark
                        )
                    }
                    Switch(
                        checked = isKmUnit,
                        onCheckedChange = { isKmUnit = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SportPrimary,
                            checkedTrackColor = SportPrimary.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMutedDark,
                            uncheckedTrackColor = DarkSurfaceElevated
                        )
                    )
                }

                Divider(color = DarkBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Auto-Pause Otomatis",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Jeda tracking saat berhenti sejenak di lampu merah",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark
                        )
                    }
                    Switch(
                        checked = isAutoPauseEnabled,
                        onCheckedChange = { isAutoPauseEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SportPrimary,
                            checkedTrackColor = SportPrimary.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMutedDark,
                            uncheckedTrackColor = DarkSurfaceElevated
                        )
                    )
                }
            }
        }

        // Section: Keamanan & Reset
        Text(
            text = "PENYIMPANAN TERENKRIPSI",
            style = MaterialTheme.typography.labelMedium,
            color = SportPrimary,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Metode Enkripsi Token",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryDark
                    )
                    Text(
                        text = "AES-256 GCM (AndroidX Security)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SportPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Divider(color = DarkBorder)

                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusError.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Putuskan Semua Akun & Hapus Token", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun StravaAccountCard(
    state: StravaAuthState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Strava",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFC4C02).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "OAuth 2.0 PKCE",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFC4C02),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                when (state) {
                    is StravaAuthState.Connected -> {
                        Text(
                            text = "Terhubung: ${state.athleteName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusSuccess,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    is StravaAuthState.TokenExpired -> {
                        Text(
                            text = "Token kedaluwarsa. Perlu login ulang.",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusWarning,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    is StravaAuthState.Connecting -> {
                        Text(
                            text = "Sedang menghubungkan ke Strava...",
                            style = MaterialTheme.typography.bodySmall,
                            color = SportPrimary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    is StravaAuthState.MissingConfig -> {
                        Text(
                            text = "Client ID belum diisi di local.properties",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedDark,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    else -> {
                        Text(
                            text = "Belum terhubung",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedDark,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            if (state is StravaAuthState.Connected) {
                OutlinedButton(
                    onClick = onDisconnect,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusError.copy(alpha = 0.5f))
                ) {
                    Text("Putuskan", style = MaterialTheme.typography.labelMedium)
                }
            } else {
                Button(
                    onClick = onConnect,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state is StravaAuthState.TokenExpired) StatusWarning else SportPrimary,
                        contentColor = DarkBackground
                    )
                ) {
                    Text(
                        text = if (state is StravaAuthState.TokenExpired) "Login Ulang" else "Hubungkan",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SpotifyAccountCard(
    state: SpotifyAuthState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Spotify",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SpotifyBrand.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Web API & SDK",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = SpotifyBrand,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                when (state) {
                    is SpotifyAuthState.Connected -> {
                        Text(
                            text = "Terhubung: ${state.userName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusSuccess,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        if (!state.isPremium) {
                            Text(
                                text = "Akun Free: playback penuh butuh Spotify Premium (PRD §6.2)",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusWarning,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        } else {
                            Text(
                                text = "Spotify Premium Aktif",
                                style = MaterialTheme.typography.labelSmall,
                                color = SpotifyBrand,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }
                    }
                    is SpotifyAuthState.TokenExpired -> {
                        Text(
                            text = "Sesi Spotify kedaluwarsa. Login ulang.",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusWarning,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    is SpotifyAuthState.Connecting -> {
                        Text(
                            text = "Sedang menghubungkan ke Spotify...",
                            style = MaterialTheme.typography.bodySmall,
                            color = SpotifyBrand,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    is SpotifyAuthState.MissingConfig -> {
                        Text(
                            text = "Client ID belum diisi di local.properties",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedDark,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    else -> {
                        Text(
                            text = "Belum terhubung",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedDark,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            if (state is SpotifyAuthState.Connected) {
                OutlinedButton(
                    onClick = onDisconnect,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusError.copy(alpha = 0.5f))
                ) {
                    Text("Putuskan", style = MaterialTheme.typography.labelMedium)
                }
            } else {
                Button(
                    onClick = onConnect,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state is SpotifyAuthState.TokenExpired) StatusWarning else SportPrimary,
                        contentColor = DarkBackground
                    )
                ) {
                    Text(
                        text = if (state is SpotifyAuthState.TokenExpired) "Login Ulang" else "Hubungkan",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun YouTubeMusicAccountCard(
    state: YouTubeMusicAuthState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "YouTube Music",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = YouTubeBrand.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Beta / Tidak Resmi",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = YouTubeBrand,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                when (state) {
                    is YouTubeMusicAuthState.Connected -> {
                        Text(
                            text = "Sesi aktif: ${state.userName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusSuccess,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    is YouTubeMusicAuthState.Disabled -> {
                        Text(
                            text = "Dinonaktifkan oleh feature flag (PRD §6.3)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedDark,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    else -> {
                        Text(
                            text = "Belum terhubung",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedDark,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            if (state is YouTubeMusicAuthState.Connected) {
                OutlinedButton(
                    onClick = onDisconnect,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusError.copy(alpha = 0.5f))
                ) {
                    Text("Putuskan", style = MaterialTheme.typography.labelMedium)
                }
            } else if (state !is YouTubeMusicAuthState.Disabled) {
                Button(
                    onClick = onConnect,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SportPrimary,
                        contentColor = DarkBackground
                    )
                ) {
                    Text(
                        text = "Hubungkan",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
