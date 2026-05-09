package com.yourapp.obd.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkBackground = Color(0xFF0A0A0A)
val DarkSurface = Color(0xFF1A1A2E)
val AccentCyan = Color(0xFF00BCD4)
val AccentAmber = Color(0xFFFFC107)
val AlertRed = Color(0xFFFF1744)
val AlertOrange = Color(0xFFFF6D00)
val AlertYellow = Color(0xFFFFEA00)
val GreenOk = Color(0xFF00E676)

private val DarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = Color.Black,
    secondary = AccentAmber,
    onSecondary = Color.Black,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = Color.White,
    onSurface = Color.White,
    error = AlertRed,
    onError = Color.White
)

@Composable
fun KiaOBDTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
