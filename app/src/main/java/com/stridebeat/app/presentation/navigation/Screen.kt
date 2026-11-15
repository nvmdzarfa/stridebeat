package com.stridebeat.app.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector? = null
) {
    object Home : Screen("home", "Beranda", Icons.Default.Home)
    object Workout : Screen("workout", "Olahraga", Icons.Default.DirectionsRun)
    object History : Screen("history", "Riwayat", Icons.Default.History)
    object Settings : Screen("settings", "Pengaturan", Icons.Default.Settings)
    object Recap : Screen("recap", "Recap Sesi", null)

    companion object {
        val bottomNavScreens = listOf(Home, Workout, History, Settings)
    }
}
