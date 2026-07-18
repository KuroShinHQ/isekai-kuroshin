package com.example.isekaikuroshin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.example.isekaikuroshin.data.PersistentDataManager

@Composable
fun getDashboardColors(): DashboardColorScheme {
    val gameData by PersistentDataManager.gameData.collectAsState()
    val themeSettings = gameData.settingsData.uiSettings.theme
    val isSystemDark = isSystemInDarkTheme()

    return when (themeSettings) {
        "DARK" -> DarkDashboardColors
        "LIGHT" -> LightDashboardColors
        "PINK" -> PinkDashboardColors
        "AUTO" -> if (isSystemDark) DarkDashboardColors else LightDashboardColors
        else -> DarkDashboardColors
    }
}

data class DashboardColorScheme(
    val primary: Color,
    val backgroundDark: Color,
    val holographicBg: Color,
    val holographicBorder: Color,
    val surface: Color,
    val secondary: Color = primary,
    val accent: Color = primary
)

object DashboardColors {
    val Primary = Color(0xFF00FFFF)
    val Secondary = Color(0xFF00FF80)
    val BackgroundDark = Color(0xFF0D1B2A)
    val HolographicBg = Color(0xFF1B263B)
    val Accent = Color(0xFFFF6B6B)
}

val DarkDashboardColors = DashboardColorScheme(
    primary = Color(0xFF00BFFF), // #00bfff from HTML (Blue theme primary)
    backgroundDark = Color(0xFF0B1426), // #0B1426 from HTML
    holographicBg = Color(0x0D00BFFF), // rgba(0, 191, 255, 0.05)
    holographicBorder = Color(0x4D00BFFF), // rgba(0, 191, 255, 0.3)
    surface = Color(0xFF1A2332), // Lighter dark surface for cards
    secondary = DashboardColors.Secondary,
    accent = DashboardColors.Accent
)

val LightDashboardColors = DashboardColorScheme(
    primary = Color(0xFF0088CC), // Darker blue for light theme
    backgroundDark = Color(0xFFF5F5F5), // Light gray background
    holographicBg = Color(0x0D0088CC), // rgba(0, 136, 204, 0.05)
    holographicBorder = Color(0x4D0088CC), // rgba(0, 136, 204, 0.3)
    surface = Color(0xFFFFFFFF), // White surface for light theme
    secondary = Color(0xFF006666),
    accent = Color(0xFFCC4444)
)

val PinkDashboardColors = DashboardColorScheme(
    primary = Color(0xFFFF69B4), // Hot Pink
    backgroundDark = Color(0xFF1A0F2E), // Dark purple background
    holographicBg = Color(0x1AFF69B4), // rgba(255, 105, 180, 0.1)
    holographicBorder = Color(0x4DFF69B4), // rgba(255, 105, 180, 0.3)
    surface = Color(0xFF2A1A3E), // Lighter purple surface
    secondary = Color(0xFFFF1493), // Deep Pink
    accent = Color(0xFFFF85C1) // Light pink accent
)