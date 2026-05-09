package com.yourapp.obd.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yourapp.obd.ui.dashboard.DashboardScreen
import com.yourapp.obd.ui.dtc.DtcScreen
import com.yourapp.obd.ui.player.PlayerScreen
import com.yourapp.obd.ui.settings.SettingsScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val DTC = "dtc"
    const val SETTINGS = "settings"
    const val PLAYER = "player"
}

@Composable
fun KiaOBDNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.DASHBOARD) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onNavigateToDtc = { navController.navigate(Routes.DTC) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToPlayer = { navController.navigate(Routes.PLAYER) }
            )
        }
        composable(Routes.DTC) {
            DtcScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PLAYER) {
            PlayerScreen(onBack = { navController.popBackStack() })
        }
    }
}
