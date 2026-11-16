package com.stridebeat.app.presentation.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.stridebeat.app.presentation.history.HistoryScreen
import com.stridebeat.app.presentation.home.HomeScreen
import com.stridebeat.app.presentation.navigation.components.BottomNavBar
import com.stridebeat.app.presentation.profile.AccountViewModel
import com.stridebeat.app.presentation.recap.RecapScreen
import com.stridebeat.app.presentation.settings.SettingsScreen
import com.stridebeat.app.presentation.theme.GlassMeshBackground
import com.stridebeat.app.presentation.theme.ThemeViewModel
import com.stridebeat.app.presentation.update.UpdateAvailableDialog
import com.stridebeat.app.presentation.update.UpdateViewModel
import com.stridebeat.app.presentation.workout.WorkoutScreen

@Composable
fun StrideBeatNavGraph(
    navController: NavHostController = rememberNavController(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
    updateViewModel: UpdateViewModel = hiltViewModel(),
    accountViewModel: AccountViewModel = hiltViewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val pendingUpdate by updateViewModel.pendingUpdate.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        updateViewModel.checkOnStartupIfNeeded()
    }

    pendingUpdate?.let { info ->
        UpdateAvailableDialog(
            updateInfo = info,
            onDismiss = { updateViewModel.dismissUpdateDialog() }
        )
    }

    val bottomNavRoutes = remember { Screen.bottomNavScreens.map { it.route }.toSet() }
    val shouldShowBottomBar = currentRoute in bottomNavRoutes

    var activeFinishedSession by remember { mutableStateOf<com.stridebeat.app.domain.model.WorkoutSession?>(null) }
    var isJustFinishedSession by remember { mutableStateOf(false) }

    GlassMeshBackground {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            bottomBar = {
                if (shouldShowBottomBar) {
                    BottomNavBar(navController = navController)
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding),
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None }
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        onStartWorkoutClick = {
                            navController.navigate(Screen.Workout.route)
                        },
                        onViewLastSessionClick = { session ->
                            if (session != null) {
                                activeFinishedSession = session
                                isJustFinishedSession = false
                                navController.navigate(Screen.Recap.route)
                            } else {
                                navController.navigate(Screen.History.route)
                            }
                        },
                        onNavigateToSettings = {
                            navController.navigate(Screen.Settings.route)
                        },
                        themeViewModel = themeViewModel,
                        accountViewModel = accountViewModel
                    )
                }

                composable(Screen.Workout.route) {
                    WorkoutScreen(
                        onFinishWorkout = { finishedSession ->
                            activeFinishedSession = finishedSession
                            isJustFinishedSession = true
                            navController.navigate(Screen.Recap.route) {
                                popUpTo(Screen.Workout.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Recap.route) {
                    RecapScreen(
                        session = activeFinishedSession,
                        isJustFinished = isJustFinishedSession,
                        onDoneClick = {
                            activeFinishedSession = null
                            isJustFinishedSession = false
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        },
                        onNavigateToSettings = {
                            navController.navigate(Screen.Settings.route)
                        }
                    )
                }

                composable(Screen.History.route) {
                    HistoryScreen(
                        onSessionClick = { clickedSession ->
                            activeFinishedSession = clickedSession
                            isJustFinishedSession = false
                            navController.navigate(Screen.Recap.route)
                        },
                        onNavigateToSettings = {
                            navController.navigate(Screen.Settings.route)
                        }
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        themeViewModel = themeViewModel,
                        updateViewModel = updateViewModel,
                        accountViewModel = accountViewModel
                    )
                }
            }
        }
    }
}
