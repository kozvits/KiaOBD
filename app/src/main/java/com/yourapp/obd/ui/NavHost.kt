package com.yourapp.obd.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yourapp.obd.ui.dashboard.DashboardScreen
import com.yourapp.obd.ui.dashboard.DvrAdasScreen
import com.yourapp.obd.ui.dtc.DtcScreen
import com.yourapp.obd.ui.player.PlayerScreen
import com.yourapp.obd.ui.settings.SettingsScreen

object Routes {
    const val DVR_ADAS = "dvr_adas"
    const val COMPUTER = "computer"
    const val SETTINGS = "settings"
    const val DTC = "dtc"
    const val PLAYER = "player"
}

@Composable
fun KiaOBDNavHost() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF121212),
                contentColor = Color.White
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Videocam, contentDescription = null) },
                    label = { Text("DVR/ADAS") },
                    selected = currentRoute == Routes.DVR_ADAS,
                    onClick = {
                        navController.navigate(Routes.DVR_ADAS) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Speed, contentDescription = null) },
                    label = { Text("Бортовой ПК") },
                    selected = currentRoute == Routes.COMPUTER,
                    onClick = {
                        navController.navigate(Routes.COMPUTER) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Настройки") },
                    selected = currentRoute == Routes.SETTINGS,
                    onClick = {
                        navController.navigate(Routes.SETTINGS) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            NavHost(navController = navController, startDestination = Routes.DVR_ADAS) {
                composable(Routes.DVR_ADAS) {
                    DvrAdasScreen()
                }
                composable(Routes.COMPUTER) {
                    DashboardScreen(
                        onNavigateToDtc = { navController.navigate(Routes.DTC) },
                        onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                        onNavigateToPlayer = { navController.navigate(Routes.PLAYER) }
                    )
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.DTC) {
                    DtcScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.PLAYER) {
                    PlayerScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
