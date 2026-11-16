package com.stridebeat.app.presentation.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stridebeat.app.BuildConfig
import com.stridebeat.app.data.local.AppThemeMode
import com.stridebeat.app.domain.model.SpotifyAuthState
import com.stridebeat.app.domain.model.StravaAuthState
import com.stridebeat.app.domain.model.YouTubeMusicAuthState
import com.stridebeat.app.presentation.theme.*
import com.stridebeat.app.presentation.update.UpdateViewModel

@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
    updateViewModel: UpdateViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val glass = StrideBeatGlass.colors

    val stravaState by authViewModel.stravaState.collectAsStateWithLifecycle()
    val spotifyState by authViewModel.spotifyState.collectAsStateWithLifecycle()
    val ytmState by authViewModel.youtubeMusicState.collectAsStateWithLifecycle()

    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
    val isCheckingUpdate by updateViewModel.isChecking.collectAsStateWithLifecycle()
    val updateStatusMessage by updateViewModel.statusMessage.collectAsStateWithLifecycle()

    var activeDialogPlatform by remember { mutableStateOf<String?>(null) }
    var showYtmInputModal by remember { mutableStateOf(false) }
    var ytmInputName by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isKmUnit by remember { mutableStateOf(true) }
    var isAutoPauseEnabled by remember { mutableStateOf(true) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        updateViewModel.triggerPreviewUpdatePopupAndNotif()
    }

    // Dialog untuk Kredensial API yang belum diisi di local.properties
    if (activeDialogPlatform != null) {
        AlertDialog(
            onDismissRequest = { activeDialogPlatform = null },
            title = {
                Text(
                    text = "Konfigurasi $activeDialogPlatform",
                    style = MaterialTheme.typography.titleLarge,
                    color = glass.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Kredensial API $activeDialogPlatform belum dikonfigurasi pada build ini. Fitur streaming musik SoundCloud/YouTube tetap bisa digunakan langsung tanpa login.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = glass.textSecondary
                )
            },
            confirmButton = {
                GlassPrimaryButton(
                    onClick = { activeDialogPlatform = null },
                    cornerRadius = 14.dp
                ) {
                    Text("Mengerti", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = glass.modalSurface,
            shape = RoundedCornerShape(22.dp)
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
                    color = glass.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Integrasi pencarian dan streaming musik langsung tanpa login akun Google.",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                    OutlinedTextField(
                        value = ytmInputName,
                        onValueChange = { ytmInputName = it },
                        label = { Text("Nama Pengguna") },
                        placeholder = { Text("Nama kamu") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = YouTubeBrand,
                            focusedLabelColor = YouTubeBrand,
                            focusedTextColor = glass.textPrimary,
                            unfocusedTextColor = glass.textPrimary,
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = YouTubeBrand,
                        contentColor = Color.White
                    )
                ) {
                    Text("Simpan Sesi", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showYtmInputModal = false }) {
                    Text("Batal", color = glass.textSecondary)
                }
            },
            containerColor = glass.modalSurface,
            shape = RoundedCornerShape(22.dp)
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Hapus Semua Sesi Akun?",
                    style = MaterialTheme.typography.titleLarge,
                    color = StatusError,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Semua token OAuth di penyimpanan terenkripsi akan dihapus dan status akun akan dikembalikan ke belum terhubung.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = glass.textSecondary
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusError,
                        contentColor = Color.White
                    )
                ) {
                    Text("Hapus Semua Token", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Batal", color = glass.textSecondary)
                }
            },
            containerColor = glass.modalSurface,
            shape = RoundedCornerShape(22.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Title
        Column {
            Text(
                text = "Pengaturan",
                style = MaterialTheme.typography.headlineMedium,
                color = glass.textPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tema, pembaruan aplikasi, akun terhubung & preferensi",
                style = MaterialTheme.typography.bodyMedium,
                color = glass.textSecondary
            )
        }

        // Section: TAMPILAN & TEMA (Dark / Light / System)
        Text(
            text = "TAMPILAN & TEMA",
            style = MaterialTheme.typography.labelMedium,
            color = glass.primary,
            fontWeight = FontWeight.Bold
        )

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 22.dp,
            elevated = true
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (glass.isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = null,
                        tint = glass.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Mode Tema Glassmorphism",
                            style = MaterialTheme.typography.titleMedium,
                            color = glass.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pilih tampilan gelap, terang, atau ikuti sistem HP",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppThemeMode.values().forEach { mode ->
                        val isSelected = mode == themeMode
                        val icon = when (mode) {
                            AppThemeMode.DARK -> Icons.Default.DarkMode
                            AppThemeMode.LIGHT -> Icons.Default.LightMode
                            AppThemeMode.SYSTEM -> Icons.Default.PhoneAndroid
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) glass.primary.copy(alpha = if (glass.isDark) 0.22f else 0.16f)
                                    else glass.glassSurfaceBottom
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) glass.primary else glass.divider,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { themeViewModel.setThemeMode(mode) },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) glass.primary else glass.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = mode.displayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) glass.primary else glass.textSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: PEMBARUAN APLIKASI
        Text(
            text = "PEMBARUAN APLIKASI",
            style = MaterialTheme.typography.labelMedium,
            color = glass.primary,
            fontWeight = FontWeight.Bold
        )

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 22.dp,
            elevated = true
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
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = glass.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "StrideBeat Android",
                                style = MaterialTheme.typography.titleMedium,
                                color = glass.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Versi terpasang: v${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary
                            )
                        }
                    }
                    GlassPill(tint = glass.primary) {
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (!updateStatusMessage.isNullOrBlank()) {
                    Text(
                        text = updateStatusMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassPrimaryButton(
                        onClick = { updateViewModel.checkForUpdates(isManual = true) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        cornerRadius = 14.dp,
                        enabled = !isCheckingUpdate
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = glass.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mengecek...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Cek Update", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                updateViewModel.triggerPreviewUpdatePopupAndNotif()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, glass.primary.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = glass.primary),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Preview Notif", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        GlassDivider()

        // Section: Akun Terhubung
        Text(
            text = "AKUN TERHUBUNG",
            style = MaterialTheme.typography.labelMedium,
            color = glass.primary,
            fontWeight = FontWeight.Bold
        )

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

        YouTubeMusicAccountCard(
            state = ytmState,
            onConnect = { showYtmInputModal = true },
            onDisconnect = { authViewModel.disconnectYouTubeMusic() }
        )

        GlassDivider()

        // Section: Preferensi Tracking
        Text(
            text = "PREFERENSI WORKOUT",
            style = MaterialTheme.typography.labelMedium,
            color = glass.primary,
            fontWeight = FontWeight.Bold
        )

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Satuan Jarak",
                            style = MaterialTheme.typography.titleMedium,
                            color = glass.textPrimary
                        )
                        Text(
                            text = if (isKmUnit) "Kilometer (km)" else "Mil (mi)",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )
                    }
                    Switch(
                        checked = isKmUnit,
                        onCheckedChange = { isKmUnit = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = glass.primary,
                            checkedTrackColor = glass.primary.copy(alpha = 0.3f),
                            uncheckedThumbColor = glass.textMuted,
                            uncheckedTrackColor = glass.trackBarInactive
                        )
                    )
                }

                GlassDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Pause Otomatis",
                            style = MaterialTheme.typography.titleMedium,
                            color = glass.textPrimary
                        )
                        Text(
                            text = "Jeda tracking saat berhenti sejenak di lampu merah",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )
                    }
                    Switch(
                        checked = isAutoPauseEnabled,
                        onCheckedChange = { isAutoPauseEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = glass.primary,
                            checkedTrackColor = glass.primary.copy(alpha = 0.3f),
                            uncheckedThumbColor = glass.textMuted,
                            uncheckedTrackColor = glass.trackBarInactive
                        )
                    )
                }
            }
        }

        // Section: Keamanan & Reset
        Text(
            text = "PENYIMPANAN TERENKRIPSI",
            style = MaterialTheme.typography.labelMedium,
            color = glass.primary,
            fontWeight = FontWeight.Bold
        )

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
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
                        color = glass.textSecondary
                    )
                    Text(
                        text = "AES-256 GCM",
                        style = MaterialTheme.typography.bodyMedium,
                        color = glass.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                GlassDivider()

                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, StatusError.copy(alpha = 0.7f)),
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

        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun StravaAccountCard(
    state: StravaAuthState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    val glass = StrideBeatGlass.colors
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp
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
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    GlassPill(tint = StravaBrand, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = "OAuth 2.0 PKCE",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = StravaBrand,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                when (state) {
                    is StravaAuthState.Connected -> {
                        Text(
                            text = "Terhubung: ${state.athleteName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.primary,
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
                            color = glass.primary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    is StravaAuthState.MissingConfig -> {
                        Text(
                            text = "Client ID belum diisi di local.properties",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    else -> {
                        Text(
                            text = "Belum terhubung",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            if (state is StravaAuthState.Connected) {
                OutlinedButton(
                    onClick = onDisconnect,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                    border = BorderStroke(1.dp, StatusError.copy(alpha = 0.5f))
                ) {
                    Text("Putuskan", style = MaterialTheme.typography.labelMedium)
                }
            } else {
                Button(
                    onClick = onConnect,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state is StravaAuthState.TokenExpired) StatusWarning else glass.primary,
                        contentColor = glass.onPrimary
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
    val glass = StrideBeatGlass.colors
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp
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
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    GlassPill(tint = SpotifyBrand, shape = RoundedCornerShape(8.dp)) {
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
                            color = glass.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        if (!state.isPremium) {
                            Text(
                                text = "Akun Free: playback penuh butuh Spotify Premium",
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
                            color = glass.textMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    else -> {
                        Text(
                            text = "Belum terhubung",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            if (state is SpotifyAuthState.Connected) {
                OutlinedButton(
                    onClick = onDisconnect,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                    border = BorderStroke(1.dp, StatusError.copy(alpha = 0.5f))
                ) {
                    Text("Putuskan", style = MaterialTheme.typography.labelMedium)
                }
            } else {
                Button(
                    onClick = onConnect,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state is SpotifyAuthState.TokenExpired) StatusWarning else glass.primary,
                        contentColor = glass.onPrimary
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
    val glass = StrideBeatGlass.colors
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp
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
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    GlassPill(tint = YouTubeBrand, shape = RoundedCornerShape(8.dp)) {
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
                            color = glass.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    is YouTubeMusicAuthState.Disabled -> {
                        Text(
                            text = "Dinonaktifkan oleh feature flag",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    else -> {
                        Text(
                            text = "Belum terhubung",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            if (state is YouTubeMusicAuthState.Connected) {
                OutlinedButton(
                    onClick = onDisconnect,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                    border = BorderStroke(1.dp, StatusError.copy(alpha = 0.5f))
                ) {
                    Text("Putuskan", style = MaterialTheme.typography.labelMedium)
                }
            } else if (state !is YouTubeMusicAuthState.Disabled) {
                Button(
                    onClick = onConnect,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = glass.primary,
                        contentColor = glass.onPrimary
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
