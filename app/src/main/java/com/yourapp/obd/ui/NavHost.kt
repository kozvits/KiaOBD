package com.yourapp.obd.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yourapp.obd.ui.dashboard.DashboardScreen
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
                containerColor = androidx.compose.ui.graphics.Color(0xFF121212),
                contentColor = androidx.compose.ui.graphics.Color.White
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination.route

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Videocam, contentDescription = null) },
                    label = { androidx.compose.material3.Text("DVR/ADAS") },
                    selected = currentRoute == Routes.DVR_ADAS,
                    onClick = { navController.navigate(Routes.DVR_ADAS) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Speed, contentDescription = null) },
                    label = { androidx.compose.material3.Text("Бортовой ПК") },
                    selected = currentRoute == Routes.COMPUTER,
                    onClick = { navController.navigate(Routes.COMPUTER) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { androidx.compose.material3.Text("Настройки") },
                    selected = currentRoute == Routes.SETTINGS,
                    onClick = { navController.navigate(Routes.SETTINGS) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            NavHost(navController = navController, startDestination = Routes.DVR_ADAS) {
                composable(Routes.DVR_ADAS) {
                    DashboardScreen(
                        onNavigateToDtc = { navController.navigate(Routes.DTC) },
                        onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                        onNavigateToPlayer = { navController.navigate(Routes.PLAYER) }
                    )
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
