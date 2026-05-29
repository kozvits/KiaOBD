package com.yourapp.obd.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yourapp.obd.ui.dashboard.DashboardScreen
import com.yourapp.obd.ui.dashboard.DvrAdasScreen
import com.yourapp.obd.ui.dtc.DtcScreen
import com.yourapp.obd.ui.menu.MenuScreen
import com.yourapp.obd.ui.player.PlayerScreen
import com.yourapp.obd.ui.settings.SettingsScreen

object Routes {
    const val DVR_ADAS = "dvr_adas"
    const val COMPUTER = "computer"
    const val MENU = "menu"
    const val SETTINGS = "settings"
    const val DTC = "dtc"
    const val PLAYER = "player"
}

private data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.DVR_ADAS, Icons.Default.Videocam, "DVR"),
    BottomNavItem(Routes.COMPUTER, Icons.Default.Dashboard, "Панель"),
    BottomNavItem(Routes.MENU, Icons.Default.Menu, "Меню")
)

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

                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(Routes.DVR_ADAS) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            NavHost(navController = navController, startDestination = Routes.DVR_ADAS) {
                composable(Routes.DVR_ADAS) {
                    DvrAdasScreen()
                }
                composable(Routes.COMPUTER) {
                    DashboardScreen()
                }
                composable(Routes.MENU) {
                    MenuScreen(
                        onNavigateToDtc = { navController.navigate(Routes.DTC) },
                        onNavigateToPlayer = { navController.navigate(Routes.PLAYER) },
                        onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                        onNavigateToDvr = {
                            navController.navigate(Routes.DVR_ADAS) {
                                popUpTo(Routes.DVR_ADAS) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onNavigateToDashboard = {
                            navController.navigate(Routes.COMPUTER) {
                                popUpTo(Routes.COMPUTER) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
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
