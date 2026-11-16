package com.stridebeat.app.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.stridebeat.app.data.local.AppThemeMode

@Immutable
data class GlassPalette(
    val isDark: Boolean,
    val backgroundBase: Color,
    val auroraTopRight: Color,
    val auroraBottomLeft: Color,
    val auroraCenter: Color,
    val glassSurfaceTop: Color,
    val glassSurfaceBottom: Color,
    val glassElevatedTop: Color,
    val glassElevatedBottom: Color,
    val glassBorderStart: Color,
    val glassBorderEnd: Color,
    val glassNavBackground: Color,
    val modalSurface: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val mapBackground: Color,
    val mapGrid: Color,
    val trackBarInactive: Color
)

private val DarkGlassPalette = GlassPalette(
    isDark = true,
    backgroundBase = Color(0xFF070B12),
    auroraTopRight = Color(0xFF00E676).copy(alpha = 0.16f),
    auroraBottomLeft = Color(0xFF06B6D4).copy(alpha = 0.13f),
    auroraCenter = Color(0xFF3B82F6).copy(alpha = 0.08f),
    glassSurfaceTop = Color.White.copy(alpha = 0.085f),
    glassSurfaceBottom = Color.White.copy(alpha = 0.035f),
    glassElevatedTop = Color.White.copy(alpha = 0.13f),
    glassElevatedBottom = Color.White.copy(alpha = 0.06f),
    glassBorderStart = Color.White.copy(alpha = 0.24f),
    glassBorderEnd = Color.White.copy(alpha = 0.05f),
    glassNavBackground = Color(0xFF0F1622).copy(alpha = 0.82f),
    modalSurface = Color(0xFF101824),
    divider = Color.White.copy(alpha = 0.10f),
    textPrimary = TextPrimaryDark,
    textSecondary = TextSecondaryDark,
    textMuted = TextMutedDark,
    primary = SportPrimary,
    onPrimary = Color(0xFF04130B),
    secondary = SportCyan,
    mapBackground = Color(0xFF0B121D),
    mapGrid = Color.White.copy(alpha = 0.05f),
    trackBarInactive = Color.White.copy(alpha = 0.12f)
)

private val LightGlassPalette = GlassPalette(
    isDark = false,
    backgroundBase = Color(0xFFEAF2F1),
    auroraTopRight = Color(0xFF10B981).copy(alpha = 0.20f),
    auroraBottomLeft = Color(0xFF38BDF8).copy(alpha = 0.18f),
    auroraCenter = Color(0xFF818CF8).copy(alpha = 0.10f),
    glassSurfaceTop = Color.White.copy(alpha = 0.78f),
    glassSurfaceBottom = Color.White.copy(alpha = 0.56f),
    glassElevatedTop = Color.White.copy(alpha = 0.90f),
    glassElevatedBottom = Color.White.copy(alpha = 0.72f),
    glassBorderStart = Color.White.copy(alpha = 0.95f),
    glassBorderEnd = Color(0xFF94A3B8).copy(alpha = 0.28f),
    glassNavBackground = Color.White.copy(alpha = 0.84f),
    modalSurface = Color(0xFFF6FAF9),
    divider = Color(0xFF0F172A).copy(alpha = 0.08f),
    textPrimary = TextPrimaryLight,
    textSecondary = TextSecondaryLight,
    textMuted = TextMutedLight,
    primary = SportPrimaryLight,
    onPrimary = Color.White,
    secondary = Color(0xFF0284C7),
    mapBackground = Color(0xFFE6F0EE),
    mapGrid = Color(0xFF0F172A).copy(alpha = 0.06f),
    trackBarInactive = Color(0xFF0F172A).copy(alpha = 0.10f)
)

val LocalGlassPalette = staticCompositionLocalOf { DarkGlassPalette }

object StrideBeatGlass {
    val colors: GlassPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalGlassPalette.current
}

private val DarkColorScheme = darkColorScheme(
    primary = SportPrimary,
    onPrimary = Color(0xFF04130B),
    primaryContainer = SportPrimaryDark,
    onPrimaryContainer = TextPrimaryDark,
    secondary = SportCyan,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceElevated,
    onSecondaryContainer = SportSecondary,
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkBorder,
    error = StatusError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = SportPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF065F46),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF075985),
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = Color.White,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626),
    onError = Color.White
)

@Composable
fun StrideBeatTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> systemDark
    }

    val colorScheme = if (useDark) DarkColorScheme else LightColorScheme
    val glassPalette = if (useDark) DarkGlassPalette else LightGlassPalette
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !useDark
                controller.isAppearanceLightNavigationBars = !useDark
            }
        }
    }

    CompositionLocalProvider(LocalGlassPalette provides glassPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = StrideBeatTypography,
            shapes = StrideBeatShapes,
            content = content
        )
    }
}
