package com.stridebeat.app.presentation.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Latar belakang aurora mesh lembut untuk mendukung efek frosted glassmorphism
 * baik pada Dark Mode maupun Light Mode.
 */
@Composable
fun GlassMeshBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = StrideBeatGlass.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(glass.backgroundBase)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val w = size.width
                    val h = size.height
                    val topRightCenter = Offset(w * 0.88f, h * 0.10f)
                    val topRightRadius = w * 0.85f
                    val topRightBrush = Brush.radialGradient(
                        colors = listOf(glass.auroraTopRight, Color.Transparent),
                        center = topRightCenter,
                        radius = topRightRadius
                    )
                    val middleCenter = Offset(w * 0.12f, h * 0.45f)
                    val middleRadius = w * 0.75f
                    val middleBrush = Brush.radialGradient(
                        colors = listOf(glass.auroraCenter, Color.Transparent),
                        center = middleCenter,
                        radius = middleRadius
                    )
                    val bottomCenter = Offset(w * 0.20f, h * 0.88f)
                    val bottomRadius = w * 0.90f
                    val bottomBrush = Brush.radialGradient(
                        colors = listOf(glass.auroraBottomLeft, Color.Transparent),
                        center = bottomCenter,
                        radius = bottomRadius
                    )
                    onDrawBehind {
                        drawCircle(brush = topRightBrush, center = topRightCenter, radius = topRightRadius)
                        drawCircle(brush = middleBrush, center = middleCenter, radius = middleRadius)
                        drawCircle(brush = bottomBrush, center = bottomCenter, radius = bottomRadius)
                    }
                }
        )

        content()
    }
}

/**
 * Kartu frosted glass modern dengan gradient permukaan transparan dan border catchlight specular.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    elevated: Boolean = false,
    accentBorderColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val glass = StrideBeatGlass.colors
    val shape = RoundedCornerShape(cornerRadius)

    val fillBrush = Brush.verticalGradient(
        colors = if (elevated) {
            listOf(glass.glassElevatedTop, glass.glassElevatedBottom)
        } else {
            listOf(glass.glassSurfaceTop, glass.glassSurfaceBottom)
        }
    )

    val borderBrush = if (accentBorderColor != null) {
        Brush.linearGradient(
            colors = listOf(
                accentBorderColor.copy(alpha = 0.65f),
                accentBorderColor.copy(alpha = 0.18f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(glass.glassBorderStart, glass.glassBorderEnd)
        )
    }

    val baseModifier = modifier
        .then(
            if (!glass.isDark) {
                Modifier.shadow(
                    elevation = 8.dp,
                    shape = shape,
                    ambientColor = Color(0xFF0F172A).copy(alpha = 0.08f),
                    spotColor = Color(0xFF0F172A).copy(alpha = 0.06f)
                )
            } else {
                Modifier
            }
        )
        .clip(shape)
        .background(fillBrush)
        .border(width = 1.dp, brush = borderBrush, shape = shape)
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)

    Column(
        modifier = baseModifier,
        content = content
    )
}

/**
 * Container pill / badge glassmorphism.
 */
@Composable
fun GlassPill(
    modifier: Modifier = Modifier,
    tint: Color? = null,
    shape: Shape = RoundedCornerShape(50),
    content: @Composable BoxScope.() -> Unit
) {
    val glass = StrideBeatGlass.colors
    val bgColor = tint?.copy(alpha = if (glass.isDark) 0.16f else 0.14f) ?: glass.glassElevatedTop
    val borderColor = tint?.copy(alpha = if (glass.isDark) 0.45f else 0.35f) ?: glass.glassBorderStart

    Surface(
        modifier = modifier.border(1.dp, borderColor, shape),
        shape = shape,
        color = bgColor
    ) {
        Box(contentAlignment = Alignment.Center, content = content)
    }
}

/**
 * Tombol utama dengan gradient emerald-cyan modern yang menyala elegan di dark & light mode.
 */
@Composable
fun GlassPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val glass = StrideBeatGlass.colors
    val shape = RoundedCornerShape(cornerRadius)
    val gradient = Brush.horizontalGradient(
        colors = if (glass.isDark) {
            listOf(Color(0xFF00E676), Color(0xFF00C9A7))
        } else {
            listOf(Color(0xFF059669), Color(0xFF0EA5E9))
        }
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .shadow(
                elevation = if (glass.isDark) 12.dp else 8.dp,
                shape = shape,
                ambientColor = glass.primary.copy(alpha = 0.45f),
                spotColor = glass.primary.copy(alpha = 0.45f)
            )
            .clip(shape)
            .background(gradient),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = glass.onPrimary,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = glass.onPrimary.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        content = content
    )
}

@Composable
fun GlassDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 1.dp,
        color = StrideBeatGlass.colors.divider
    )
}
