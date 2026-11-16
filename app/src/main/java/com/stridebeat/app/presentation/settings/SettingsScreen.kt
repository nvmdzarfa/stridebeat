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
import com.stridebeat.app.BuildConfig
import com.stridebeat.app.data.local.AppThemeMode
import com.stridebeat.app.domain.model.StravaAuthState
import com.stridebeat.app.domain.model.YouTubeMusicAuthState
import com.stridebeat.app.presentation.profile.AccountViewModel
import com.stridebeat.app.presentation.strava.StravaViewModel
import com.stridebeat.app.presentation.theme.*
import com.stridebeat.app.presentation.update.UpdateViewModel
import com.stridebeat.app.presentation.workout.MusicViewModel

@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
    updateViewModel: UpdateViewModel = hiltViewModel(),
    accountViewModel: AccountViewModel = hiltViewModel(),
    stravaViewModel: StravaViewModel = hiltViewModel(),
    musicViewModel: MusicViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val glass = StrideBeatGlass.colors

    val youTubeMusicState by authViewModel.youTubeMusicState.collectAsStateWithLifecycle()
    val youtubeMusicEnabled by musicViewModel.youtubeMusicEnabled.collectAsStateWithLifecycle()
    val stravaState by authViewModel.stravaState.collectAsStateWithLifecycle()
    val autoUploadStrava by stravaViewModel.autoUploadEnabled.collectAsStateWithLifecycle()
    val defaultActivityPrefix by stravaViewModel.defaultActivityPrefix.collectAsStateWithLifecycle()
    val isImportingStrava by stravaViewModel.isImporting.collectAsStateWithLifecycle()
    val stravaStatusMessage by stravaViewModel.statusMessage.collectAsStateWithLifecycle()


    val userProfile by accountViewModel.userProfile.collectAsStateWithLifecycle()
    val isSigningIn by accountViewModel.isSigningIn.collectAsStateWithLifecycle()
    val isSyncing by accountViewModel.isSyncing.collectAsStateWithLifecycle()
    val syncMessage by accountViewModel.syncMessage.collectAsStateWithLifecycle()
    val accountError by accountViewModel.errorMessage.collectAsStateWithLifecycle()

    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
    val isCheckingUpdate by updateViewModel.isChecking.collectAsStateWithLifecycle()
    val updateStatusMessage by updateViewModel.statusMessage.collectAsStateWithLifecycle()

    var showStravaConfigDialog by remember { mutableStateOf(false) }

    var showEditStravaPrefixDialog by remember { mutableStateOf(false) }
    var stravaPrefixInput by remember { mutableStateOf("") }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isKmUnit by remember { mutableStateOf(true) }
    var isAutoPauseEnabled by remember { mutableStateOf(true) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        updateViewModel.triggerPreviewUpdatePopupAndNotif()
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        accountViewModel.handleGoogleSignInResult(result.resultCode, result.data)
    }

    if (showStravaConfigDialog) {
        AlertDialog(
            onDismissRequest = { showStravaConfigDialog = false },
            title = {
                Text(
                    text = "Konfigurasi Strava API",
                    style = MaterialTheme.typography.titleLarge,
                    color = glass.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "STRAVA_CLIENT_ID dan STRAVA_CLIENT_SECRET belum dikonfigurasi pada build ini (local.properties / GitHub Secrets). Kamu tetap bisa melacak olahraga dan mengekspor file GPX secara manual.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = glass.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { showStravaConfigDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = StravaBrand, contentColor = Color.White)
                ) {
                    Text("Mengerti", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = glass.modalSurface,
            shape = RoundedCornerShape(22.dp)
        )
    }

    if (showEditStravaPrefixDialog) {
        AlertDialog(
            onDismissRequest = { showEditStravaPrefixDialog = false },
            title = {
                Text(
                    text = "Nama Aktivitas Default Strava",
                    style = MaterialTheme.typography.titleLarge,
                    color = glass.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Prefix dipakai untuk membentuk judul aktivitas otomatis (contoh: \"StrideBeat Run — 16 Nov 2026 06:30\").",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                    OutlinedTextField(
                        value = stravaPrefixInput,
                        onValueChange = { stravaPrefixInput = it },
                        label = { Text("Prefix Judul") },
                        placeholder = { Text("StrideBeat") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StravaBrand,
                            focusedLabelColor = StravaBrand,
                            focusedTextColor = glass.textPrimary,
                            unfocusedTextColor = glass.textPrimary,
                            cursorColor = StravaBrand
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        stravaViewModel.setDefaultActivityPrefix(stravaPrefixInput)
                        showEditStravaPrefixDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StravaBrand, contentColor = Color.White)
                ) {
                    Text("Simpan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditStravaPrefixDialog = false }) {
                    Text("Batal", color = glass.textSecondary)
                }
            },
            containerColor = glass.modalSurface,
            shape = RoundedCornerShape(22.dp)
        )
    }

    if (showEditNameDialog) {

        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = {
                Text(
                    text = "Ubah Nama Profil",
                    style = MaterialTheme.typography.titleLarge,
                    color = glass.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = editNameInput,
                    onValueChange = { editNameInput = it },
                    label = { Text("Nama Tampilan") },
                    placeholder = { Text("Masukkan nama kamu") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = glass.primary,
                        focusedLabelColor = glass.primary,
                        focusedTextColor = glass.textPrimary,
                        unfocusedTextColor = glass.textPrimary,
                        cursorColor = glass.primary
                    )
                )
            },
            confirmButton = {
                GlassPrimaryButton(
                    onClick = {
                        if (editNameInput.isNotBlank()) {
                            accountViewModel.updateDisplayName(editNameInput)
                        }
                        showEditNameDialog = false
                    },
                    cornerRadius = 14.dp
                ) {
                    Text("Simpan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
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
                    text = "Putuskan Semua Sesi Akun?",
                    style = MaterialTheme.typography.titleLarge,
                    color = StatusError,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Sesi Google, Strava & sesi musik di perangkat ini akan diputuskan. Data yang sudah disinkronkan di Cloud Firestore tetap aman.",
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
                        accountViewModel.signOut()
                        showDeleteConfirmDialog = false
                    },

                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusError,
                        contentColor = Color.White
                    )
                ) {
                    Text("Putuskan Semua", fontWeight = FontWeight.Bold)
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
                text = "Akun Google Cloud, tema, musik & pembaruan",
                style = MaterialTheme.typography.bodyMedium,
                color = glass.textSecondary
            )
        }

        // Section: AKUN GOOGLE & CLOUD SYNC
        Text(
            text = "AKUN GOOGLE & CLOUD SYNC",
            style = MaterialTheme.typography.labelMedium,
            color = glass.primary,
            fontWeight = FontWeight.Bold
        )

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 22.dp,
            elevated = true,
            accentBorderColor = if (userProfile.isGoogleSignedIn) glass.primary else null
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(glass.primary.copy(alpha = 0.16f))
                            .border(1.5.dp, glass.primary.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!userProfile.photoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = userProfile.photoUrl,
                                contentDescription = userProfile.displayName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (userProfile.displayName.isNotBlank()) {
                            Text(
                                text = userProfile.firstName.first().uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                color = glass.primary,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = glass.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = userProfile.displayName.ifBlank { "Pelari StrideBeat" },
                                style = MaterialTheme.typography.titleMedium,
                                color = glass.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    editNameInput = userProfile.displayName
                                    showEditNameDialog = true
                                },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Nama",
                                    tint = glass.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        if (userProfile.isGoogleSignedIn) {
                            Text(
                                text = userProfile.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary
                            )
                        } else {
                            Text(
                                text = if (accountViewModel.isFirebaseConfigured)
                                    "Login Google untuk simpan nama & riwayat di Cloud"
                                else
                                    "Menunggu google-services.json Firebase untuk sync Cloud",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textMuted
                            )
                        }
                    }

                    if (userProfile.isGoogleSignedIn) {
                        GlassPill(tint = glass.primary) {
                            Text(
                                text = "CLOUD",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (!syncMessage.isNullOrBlank()) {
                    Text(
                        text = syncMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.primary
                    )
                }

                if (!accountError.isNullOrBlank()) {
                    Text(
                        text = accountError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusError
                    )
                }

                if (userProfile.isGoogleSignedIn) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlassPrimaryButton(
                            onClick = { accountViewModel.syncNow() },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            cornerRadius = 14.dp,
                            enabled = !isSyncing
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = glass.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Menyinkronkan...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Sync Cloud", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = { accountViewModel.signOut() },
                            modifier = Modifier
                                .weight(0.8f)
                                .height(44.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, StatusError.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError)
                        ) {
                            Text("Keluar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    GlassPrimaryButton(
                        onClick = {
                            accountViewModel.launchGoogleSignIn(context) { intent ->
                                googleSignInLauncher.launch(intent)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        cornerRadius = 14.dp,
                        enabled = !isSigningIn
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = glass.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Menghubungkan ke Google...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Masuk dengan Akun Google", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
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

        // Section: Akun Terhubung — Strava (M5)
        Text(
            text = "AKUN TERHUBUNG (STRAVA)",
            style = MaterialTheme.typography.labelMedium,
            color = glass.primary,
            fontWeight = FontWeight.Bold
        )

        StravaAccountCard(
            state = stravaState,
            onConnect = {
                if (stravaState is StravaAuthState.MissingConfig) {
                    showStravaConfigDialog = true
                } else {
                    authViewModel.startStravaLogin(context)
                }
            },
            onDisconnect = { authViewModel.disconnectStrava() }
        )

        StravaPreferencesCard(
            stravaState = stravaState,
            autoUploadEnabled = autoUploadStrava,
            defaultPrefix = defaultActivityPrefix,
            isImporting = isImportingStrava,
            statusMessage = stravaStatusMessage,
            onToggleAutoUpload = { stravaViewModel.setAutoUploadEnabled(it) },
            onEditDefaultPrefix = {
                stravaPrefixInput = defaultActivityPrefix
                showEditStravaPrefixDialog = true
            },
            onImportHistory = {
                if (stravaState is StravaAuthState.MissingConfig) {
                    showStravaConfigDialog = true
                } else {
                    stravaViewModel.importRecentActivities()
                }
            }
        )

        GlassDivider()

        // Section: Akun Musik
        Text(
            text = "AKUN MUSIK",
            style = MaterialTheme.typography.labelMedium,
            color = glass.primary,
            fontWeight = FontWeight.Bold
        )

        SoundCloudInfoCard()

        YouTubeMusicAccountCard(
            enabled = youtubeMusicEnabled,
            onEnabledChanged = { enabled ->
                musicViewModel.setYouTubeMusicEnabled(enabled)
                authViewModel.setYouTubeMusicFeatureFlag(enabled)
            },
            state = youTubeMusicState,
            onConnect = { authViewModel.startYouTubeMusicLogin(context) },
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
            text = "PENYIMPANAN & KEAMANAN",
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
                    Text("Putuskan Semua Akun & Token", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun YouTubeMusicAccountCard(
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
    state: YouTubeMusicAuthState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    val glass = StrideBeatGlass.colors
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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

                    Text(
                        text = if (enabled) {
                            when (state) {
                                is YouTubeMusicAuthState.Connected -> "Terhubung: ${state.userName}"
                                is YouTubeMusicAuthState.Connecting -> "Menghubungkan ke Google..."
                                is YouTubeMusicAuthState.Error -> "Gagal: ${state.message}"
                                else -> "Mode Pencarian Publik (Beta) — Aktif tanpa login"
                            }
                        } else {
                            "Nonaktif (pemutar hanya memakai SoundCloud)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) glass.textSecondary else glass.textMuted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChanged,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = YouTubeBrand,
                        uncheckedThumbColor = glass.textMuted,
                        uncheckedTrackColor = glass.trackBarInactive
                    )
                )
            }

            if (enabled && BuildConfig.YOUTUBE_MUSIC_OAUTH_CLIENT_ID.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (state is YouTubeMusicAuthState.Connected) {
                        OutlinedButton(
                            onClick = onDisconnect,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                            border = BorderStroke(1.dp, StatusError.copy(alpha = 0.5f))
                        ) {
                            Text("Putuskan Akun", style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        Button(
                            onClick = onConnect,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = YouTubeBrand, contentColor = Color.White)
                        ) {
                            Text("Hubungkan Akun Google", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SoundCloudInfoCard() {
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
                        text = "SoundCloud",
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    GlassPill(tint = SoundCloudBrand, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = "Sumber Utama",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = SoundCloudBrand,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Text(
                    text = "Aktif otomatis tanpa login. Akses katalog musik global penuh secara stabil.",
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.textSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
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
    val glass = StrideBeatGlass.colors
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        accentBorderColor = if (state is StravaAuthState.Connected) StravaBrand else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                                text = "API v3",
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
                                color = StravaBrand,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        is StravaAuthState.TokenExpired -> {
                            Text(
                                text = "Token expired — Login ulang Strava",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusWarning,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        is StravaAuthState.Connecting -> {
                            Text(
                                text = "Sedang menghubungkan ke Strava...",
                                style = MaterialTheme.typography.bodySmall,
                                color = StravaBrand,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        is StravaAuthState.MissingConfig -> {
                            Text(
                                text = "Belum terhubung (Client ID belum dikonfigurasi)",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textMuted,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        is StravaAuthState.Error -> {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusError,
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
                            containerColor = if (state is StravaAuthState.TokenExpired) StatusWarning else StravaBrand,
                            contentColor = Color.White
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

            Text(
                text = "Scope: read, activity:read_all, activity:write — untuk profil atlet, impor riwayat, dan upload sesi olahraga.",
                style = MaterialTheme.typography.labelSmall,
                color = glass.textMuted
            )
        }
    }
}

@Composable
private fun StravaPreferencesCard(
    stravaState: StravaAuthState,
    autoUploadEnabled: Boolean,
    defaultPrefix: String,
    isImporting: Boolean,
    statusMessage: String?,
    onToggleAutoUpload: (Boolean) -> Unit,
    onEditDefaultPrefix: () -> Unit,
    onImportHistory: () -> Unit
) {
    val glass = StrideBeatGlass.colors
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-upload ke Strava",
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.textPrimary
                    )
                    Text(
                        text = "Otomatis unggah sesi ≥ 1 menit & jarak > 0 m setelah selesai",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                }
                Switch(
                    checked = autoUploadEnabled,
                    onCheckedChange = onToggleAutoUpload,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = StravaBrand,
                        checkedTrackColor = StravaBrand.copy(alpha = 0.3f),
                        uncheckedThumbColor = glass.textMuted,
                        uncheckedTrackColor = glass.trackBarInactive
                    )
                )
            }

            GlassDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEditDefaultPrefix() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Nama Aktivitas Default",
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.textPrimary
                    )
                    Text(
                        text = "$defaultPrefix Run — <tanggal> <jam>",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                }
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Ubah Nama Aktivitas Default",
                    tint = StravaBrand,
                    modifier = Modifier.size(18.dp)
                )
            }

            GlassDivider()

            OutlinedButton(
                onClick = onImportHistory,
                enabled = stravaState is StravaAuthState.Connected && !isImporting,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    1.dp,
                    if (stravaState is StravaAuthState.Connected) StravaBrand.copy(alpha = 0.7f) else glass.divider
                ),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StravaBrand)
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = StravaBrand,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mengimpor 30 aktivitas terbaru...", fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Impor Riwayat Strava (30 Terbaru)", fontWeight = FontWeight.SemiBold)
                }
            }

            if (!statusMessage.isNullOrBlank()) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = StravaBrand
                )
            }
        }
    }
}

