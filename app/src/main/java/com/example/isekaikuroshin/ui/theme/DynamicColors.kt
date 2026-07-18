package com.example.isekaikuroshin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.example.isekaikuroshin.data.PersistentDataManager

/**
 * Dynamic color system that respects user theme settings
 * Provides unified colors for all screens
 */
@Composable
fun getAppColors(): AppColorScheme {
    val gameData by PersistentDataManager.gameData.collectAsState()
    val themeSettings = gameData.settingsData.uiSettings.theme
    val isSystemDark = isSystemInDarkTheme()

    val isDarkTheme = when (themeSettings) {
        "DARK" -> true
        "LIGHT" -> false
        "AUTO" -> isSystemDark
        else -> isSystemDark
    }

    return if (isDarkTheme) {
        DarkAppColors
    } else {
        LightAppColors
    }
}

data class AppColorScheme(
    val primary: Color,
    val background: Color,
    val surface: Color,
    val onPrimary: Color,
    val onBackground: Color,
    val onSurface: Color,
    val holographicBg: Color,
    val holographicBorder: Color,
    val accent: Color,
    val error: Color
)

// Dark theme app colors
val DarkAppColors = AppColorScheme(
    primary = Color(0xFF00BFFF), // Cyan blue (Blue theme primary)
    background = Color(0xFF0B1426), // Dark navy
    surface = Color(0xFF1A2332), // Lighter dark surface
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    holographicBg = Color(0x0D00BFFF), // Semi-transparent cyan
    holographicBorder = Color(0x4D00BFFF), // More opaque cyan
    accent = Color(0xFF00FF7F), // Spring green
    error = Color(0xFFFF4444) // Red
)

// Light theme app colors
val LightAppColors = AppColorScheme(
    primary = Color(0xFF0088CC), // Darker blue for contrast
    background = Color(0xFFF5F5F5), // Light gray
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Color(0xFF1C1B1F), // Dark text
    onSurface = Color(0xFF1C1B1F), // Dark text
    holographicBg = Color(0x0D0088CC), // Semi-transparent blue
    holographicBorder = Color(0x4D0088CC), // More opaque blue
    accent = Color(0xFF2E7D32), // Dark green
    error = Color(0xFFCC0000) // Dark red
)

/**
 * Provides current theme-aware colors as a local composition
 */
@Composable
fun ProvideAppColors(
    content: @Composable () -> Unit
) {
    val colors = getAppColors()
    CompositionLocalProvider(
        LocalAppColors provides colors,
        content = content
    )
}

val LocalAppColors = compositionLocalOf<AppColorScheme> {
    error("No AppColors provided")
}

/**
 * Hook to access current app colors from any composable
 */
@Composable
fun appColors(): AppColorScheme = LocalAppColors.current