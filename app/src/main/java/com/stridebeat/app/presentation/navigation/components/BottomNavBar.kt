package com.stridebeat.app.presentation.navigation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.stridebeat.app.presentation.navigation.Screen
import com.stridebeat.app.presentation.theme.StrideBeatGlass

@Composable
fun BottomNavBar(navController: NavController) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route
    val glass = StrideBeatGlass.colors

    val dockShape = RoundedCornerShape(28.dp)
    val dockBrush = remember(glass) {
        Brush.verticalGradient(
            colors = listOf(
                glass.glassNavBackground,
                if (glass.isDark) glass.glassNavBackground.copy(alpha = 0.92f) else glass.glassNavBackground.copy(alpha = 0.78f)
            )
        )
    }
    val borderBrush = remember(glass) {
        Brush.linearGradient(
            colors = listOf(glass.glassBorderStart, glass.glassBorderEnd)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .shadow(
                    elevation = if (glass.isDark) 18.dp else 14.dp,
                    shape = dockShape,
                    ambientColor = Color.Black.copy(alpha = if (glass.isDark) 0.45f else 0.14f),
                    spotColor = Color.Black.copy(alpha = if (glass.isDark) 0.45f else 0.12f)
                )
                .clip(dockShape)
                .background(dockBrush)
                .border(1.dp, borderBrush, dockShape)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Screen.bottomNavScreens.forEach { screen ->
                val isSelected = currentRoute == screen.route
                val contentColor = if (isSelected) glass.primary else glass.textSecondary
                val pillColor = if (isSelected) glass.primary.copy(alpha = if (glass.isDark) 0.16f else 0.14f) else Color.Transparent

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(22.dp))
                        .background(pillColor)
                        .then(
                            if (isSelected) {
                                Modifier.border(
                                    width = 1.dp,
                                    color = glass.primary.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(22.dp)
                                )
                            } else {
                                Modifier
                            }
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        screen.icon?.let { icon ->
                            Icon(
                                imageVector = icon,
                                contentDescription = screen.title,
                                tint = contentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = screen.title,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = contentColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
