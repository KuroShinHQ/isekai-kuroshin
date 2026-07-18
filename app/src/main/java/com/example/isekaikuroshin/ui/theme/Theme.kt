package com.example.isekaikuroshin.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.isekaikuroshin.ui.theme.DarkNavyGray
import com.example.isekaikuroshin.data.PersistentDataManager

// ============= BLUE THEME (HOLOGRAPHIC NEON) =============
private val BlueColorScheme = darkColorScheme(
    primary = BluePrimary,                       // Neon cyan ana vurgu
    secondary = BlueSecondary,                   // Turkuaz ikincil
    tertiary = BlueTertiary,                     // Açık mavi subtle
    background = BlueBackground,                 // Neredeyse siyah arka plan
    surface = BlueSurface,                       // Çok koyu mavi surface
    onPrimary = Color.Black,                     // Neon üzerinde siyah metin
    onSecondary = Color.Black,
    onBackground = BlueOnSurface,                // Açık mavi metin (yüksek kontrast)
    onSurface = BlueOnSurface,
    error = Color(0xFFEF4444),                  // HP bar color
    primaryContainer = Color(0xFF133E7C),       // Orta mavi container
    secondaryContainer = Color(0xFF091833),     // Koyu mavi container
    onPrimaryContainer = BlueOnSurface,
    onSecondaryContainer = BlueOnSurface,
    surfaceVariant = Color(0xFF0F1F3A),         // Surface variant
    onSurfaceVariant = BlueAccent                // Elektrik mavisi vurgu
)

// ============= GREY THEME (PROFESSIONAL NEUTRAL) =============
private val GreyColorScheme = darkColorScheme(
    primary = GreyPrimary,                       // Yumuşak mavi-gri ana vurgu
    secondary = GreySecondary,                   // Orta mavi-gri ikincil
    tertiary = GreyTertiary,                     // Nötr gri subtle
    background = GreyBackground,                 // Çok koyu gri arka plan
    surface = GreySurface,                       // Koyu gri surface
    onPrimary = Color.Black,                     // Açık renk üzerinde siyah
    onSecondary = Color.White,
    onBackground = GreyOnSurface,                // Beyaz-gri metin (yüksek kontrast)
    onSurface = GreyOnSurface,
    error = Color(0xFFEF4444),                  // HP bar color
    primaryContainer = Color(0xFF2D3748),       // Orta gri container
    secondaryContainer = Color(0xFF1A202C),     // Koyu gri container
    onPrimaryContainer = GreyOnSurface,
    onSecondaryContainer = GreyOnSurface,
    surfaceVariant = Color(0xFF252D3A),         // Surface variant
    onSurfaceVariant = GreyAccent                // Açık gri vurgu
)

// ============= PINK THEME (ELEGANT PURPLE) =============
private val PinkColorScheme = darkColorScheme(
    primary = PinkPrimary,                       // Parlak pembe-mor ana vurgu
    secondary = PinkSecondary,                   // Koyu kırmızı-mor ikincil
    tertiary = PinkTertiary,                     // Neon pembe subtle
    background = PinkBackground,                 // Neredeyse siyah mor arka plan
    surface = PinkSurface,                       // Koyu mor surface
    onPrimary = Color.Black,                     // Parlak renk üzerinde siyah
    onSecondary = Color.White,
    onBackground = PinkOnSurface,                // Açık pembe-beyaz metin (yüksek kontrast)
    onSurface = PinkOnSurface,
    error = Color(0xFFEF4444),                  // HP bar color
    primaryContainer = Color(0xFF4C1D95),       // Orta mor container
    secondaryContainer = Color(0xFF2E1065),     // Koyu mor container
    onPrimaryContainer = PinkOnSurface,
    onSecondaryContainer = PinkOnSurface,
    surfaceVariant = Color(0xFF1E1034),         // Surface variant
    onSurfaceVariant = PinkAccent                // Elektrik pembe vurgu
)

// ============= HELPER: Create Custom Theme from Hex Colors =============
private fun createCustomTheme(
    primaryHex: String,
    surfaceHex: String,
    backgroundHex: String
): androidx.compose.material3.ColorScheme {
    val primary = try { Color(android.graphics.Color.parseColor(primaryHex)) } catch (e: Exception) { BlueAccent }
    val surface = try { Color(android.graphics.Color.parseColor(surfaceHex)) } catch (e: Exception) { BlueSurface }
    val background = try { Color(android.graphics.Color.parseColor(backgroundHex)) } catch (e: Exception) { BlueBackground }

    return darkColorScheme(
        primary = primary,
        secondary = primary,
        tertiary = primary,
        background = background,
        surface = surface,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color.White,
        onSurface = Color.White,
        error = Color(0xFFEF4444),
        primaryContainer = primary.copy(alpha = 0.6f),      // Custom theme ile uyumlu header
        secondaryContainer = surface.copy(alpha = 0.8f),    // Custom theme ile uyumlu cards
        onPrimaryContainer = Color.White,
        onSecondaryContainer = Color.White
    )
}

@Composable
fun IsekaiKuroshinTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Custom themes kullanıldığı için dynamic color kapatıldı
    content: @Composable () -> Unit
) {
    // Safely get settings
    val gameData by PersistentDataManager.gameData.collectAsState()
    val uiSettings = gameData.settingsData.uiSettings
    val themeSettings = uiSettings.theme

    val colorScheme = when (themeSettings) {
        "BLUE" -> BlueColorScheme
        "GREY" -> GreyColorScheme
        "PINK" -> PinkColorScheme
        "CUSTOM1" -> createCustomTheme(
            uiSettings.customTheme1.primary,
            uiSettings.customTheme1.surface,
            uiSettings.customTheme1.background
        )
        "CUSTOM2" -> createCustomTheme(
            uiSettings.customTheme2.primary,
            uiSettings.customTheme2.surface,
            uiSettings.customTheme2.background
        )
        "CUSTOM3" -> createCustomTheme(
            uiSettings.customTheme3.primary,
            uiSettings.customTheme3.surface,
            uiSettings.customTheme3.background
        )
        // Backward compatibility
        "DARK" -> BlueColorScheme
        "LIGHT" -> GreyColorScheme
        "AUTO" -> if (darkTheme) BlueColorScheme else GreyColorScheme
        else -> BlueColorScheme // Default to Blue
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}