package com.example.isekaikuroshin.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.isekaikuroshin.data.*
import com.example.isekaikuroshin.data.rememberLocalizedText
import com.example.isekaikuroshin.ui.theme.DashboardColorScheme

@Composable
fun UISettingsSection(
    settings: SettingsData,
    dashboardColors: DashboardColorScheme
) {
    SettingsCategory(
        title = rememberLocalizedText("ui_settings"),
        icon = Icons.Default.Palette,
        dashboardColors = dashboardColors
    ) {
        // GÖREV: Language duplicate kaldırıldı - GameSettings'de var
        // Language artık sadece "Game Settings" bölümünde ayarlanıyor

        // Theme - Önce tüm display name'leri hesapla
        val themeOptions = listOf("BLUE", "GREY", "PINK", "CUSTOM1", "CUSTOM2", "CUSTOM3")
        val themeDisplayNames = themeOptions.associateWith { option ->
            rememberLocalizedText("theme_" + option.lowercase())
        }

        DropdownSetting(
            title = rememberLocalizedText("theme"),
            subtitle = rememberLocalizedText("theme_" + settings.uiSettings.theme.lowercase()),
            options = themeOptions,
            selectedOption = settings.uiSettings.theme,
            onOptionSelected = { theme ->
                PersistentDataManager.updateSettingsData { settingsData ->
                    settingsData.copy(
                        uiSettings = settingsData.uiSettings.copy(
                            theme = theme
                        )
                    )
                }
            },
            displayNameMapper = { option ->
                themeDisplayNames[option] ?: option
            }
        )

        // Animations & Haptic
        SwitchSetting(
            title = rememberLocalizedText("animations"),
            subtitle = rememberLocalizedText("animations_desc"),
            checked = settings.uiSettings.enableAnimations,
            onCheckedChange = { enabled ->
                PersistentDataManager.updateSettingsData { settingsData ->
                    settingsData.copy(
                        uiSettings = settingsData.uiSettings.copy(
                            enableAnimations = enabled
                        )
                    )
                }
            }
        )

        SwitchSetting(
            title = rememberLocalizedText("haptic_feedback"),
            subtitle = rememberLocalizedText("haptic_feedback_desc"),
            checked = settings.uiSettings.enableHapticFeedback,
            onCheckedChange = { enabled ->
                PersistentDataManager.updateSettingsData { settingsData ->
                    settingsData.copy(
                        uiSettings = settingsData.uiSettings.copy(
                            enableHapticFeedback = enabled
                        )
                    )
                }
            }
        )

        SwitchSetting(
            title = rememberLocalizedText("ambient_sound"),
            subtitle = rememberLocalizedText("ambient_sound_desc"),
            checked = settings.uiSettings.enableAmbientSound,
            onCheckedChange = { enabled ->
                PersistentDataManager.updateSettingsData { settingsData ->
                    settingsData.copy(
                        uiSettings = settingsData.uiSettings.copy(
                            enableAmbientSound = enabled
                        )
                    )
                }
            }
        )

        // GÖREV 4: Element Affinity Display Toggle
        SwitchSetting(
            title = rememberLocalizedText("element_affinity_show"),
            subtitle = rememberLocalizedText("element_affinity_show_desc"),
            checked = settings.uiSettings.showElementAffinity,
            onCheckedChange = { enabled ->
                PersistentDataManager.updateSettingsData { settingsData ->
                    settingsData.copy(
                        uiSettings = settingsData.uiSettings.copy(
                            showElementAffinity = enabled
                        )
                    )
                }
            }
        )

        // GÖREV #18: İskelet Grid Çizgi Kalınlığı
        SliderSetting(
            title = rememberLocalizedText("skeleton_stroke_width"),
            subtitle = "%.1f dp".format(settings.uiSettings.skeletonGridStrokeWidth),
            value = settings.uiSettings.skeletonGridStrokeWidth,
            valueRange = 0.5f..5.0f,
            onValueChange = { width ->
                PersistentDataManager.updateSettingsData { settingsData ->
                    settingsData.copy(
                        uiSettings = settingsData.uiSettings.copy(
                            skeletonGridStrokeWidth = width
                        )
                    )
                }
            }
        )

        // GÖREV #18: İskelet Grid Şeffaflık
        SliderSetting(
            title = rememberLocalizedText("skeleton_alpha"),
            subtitle = "${(settings.uiSettings.skeletonGridAlpha * 100).toInt()}%",
            value = settings.uiSettings.skeletonGridAlpha,
            valueRange = 0.0f..1.0f,
            onValueChange = { alpha ->
                PersistentDataManager.updateSettingsData { settingsData ->
                    settingsData.copy(
                        uiSettings = settingsData.uiSettings.copy(
                            skeletonGridAlpha = alpha
                        )
                    )
                }
            }
        )

        // GÖREV #18: İskelet Grid Renk Seçici
        SkeletonColorPicker(
            selectedColor = Color(settings.uiSettings.skeletonGridColor),
            onColorSelected = { color ->
                PersistentDataManager.updateSettingsData { settingsData ->
                    settingsData.copy(
                        uiSettings = settingsData.uiSettings.copy(
                            skeletonGridColor = color.value.toLong()
                        )
                    )
                }
            }
        )
    }

    // Custom Theme Editor (sadece CUSTOM1/2/3 seçiliyse göster)
    CustomThemeEditorSection(
        settings = settings,
        dashboardColors = dashboardColors
    )
}

// GÖREV #18: İskelet Renk Seçici Composable
@Composable
private fun SkeletonColorPicker(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = rememberLocalizedText("skeleton_color"),
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Preset Renkler
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val presetColors = listOf(
                Color.White to "White",
                Color.Red to "Red",
                Color.Green to "Green",
                Color.Blue to "Blue",
                Color.Cyan to "Cyan",
                Color.Magenta to "Magenta",
                Color.Yellow to "Yellow"
            )

            presetColors.forEach { (color, name) ->
                ColorCircle(
                    color = color,
                    isSelected = color.value == selectedColor.value,
                    onClick = { onColorSelected(color) }
                )
            }
        }
    }
}

@Composable
private fun ColorCircle(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color.White else Color.Gray,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Text(text = "✓", color = if (color == Color.White) Color.Black else Color.White)
        }
    }
}