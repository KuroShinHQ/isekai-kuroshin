package com.example.isekaikuroshin.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.isekaikuroshin.data.*
import com.example.isekaikuroshin.data.rememberLocalizedText
import com.example.isekaikuroshin.sound.SoundManager
import com.example.isekaikuroshin.ui.theme.DashboardColorScheme

@Composable
fun GameSettingsSection(
    settings: SettingsData,
    dashboardColors: DashboardColorScheme,
    soundManager: SoundManager
) {
    SettingsCategory(
        title = rememberLocalizedText("game_settings"),
        icon = Icons.Default.Games,
        dashboardColors = dashboardColors
    ) {
        // Auto-save enabled
        SwitchSetting(
            title = rememberLocalizedText("auto_save"),
            subtitle = rememberLocalizedText("auto_save_desc"),
            checked = settings.gameSettings.autoSaveEnabled,
            onCheckedChange = { enabled ->
                PersistentDataManager.updateSettingsData { settingsData ->
                    settingsData.copy(
                        gameSettings = settingsData.gameSettings.copy(
                            autoSaveEnabled = enabled
                        )
                    )
                }
            }
        )

        // Language
        DropdownSetting(
            title = rememberLocalizedText("language"),
            subtitle = LanguageManager.getLanguageDisplayName(settings.gameSettings.language),
            options = listOf("TR", "EN"),
            selectedOption = settings.gameSettings.language,
            onOptionSelected = { language ->
                // Update LanguageManager first
                LanguageManager.setLanguage(language)
                // Update persistent storage
                PersistentDataManager.updateSettingsData { settingsData ->
                    settingsData.copy(
                        gameSettings = settingsData.gameSettings.copy(
                            language = language
                        )
                    )
                }
            },
            displayNameMapper = { LanguageManager.getLanguageDisplayName(it) }
        )

        // Hybrid AI Info Card (only for Turkish)
        if (settings.gameSettings.language == "TR") {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = dashboardColors.holographicBg.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    tint = dashboardColors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = rememberLocalizedText("information_title"),
                        style = MaterialTheme.typography.titleSmall,
                        color = dashboardColors.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = rememberLocalizedText("hybrid_ai_info_desc"),
                        style = MaterialTheme.typography.bodySmall,
                        color = dashboardColors.secondary.copy(alpha = 0.9f),
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Notifications
        SwitchSetting(
            title = rememberLocalizedText("notifications"),
            subtitle = rememberLocalizedText("notifications_desc"),
            checked = settings.gameSettings.enableNotifications,
            onCheckedChange = { enabled ->
                PersistentDataManager.updateSettingsData { settingsData ->
                    settingsData.copy(
                        gameSettings = settingsData.gameSettings.copy(
                            enableNotifications = enabled
                        )
                    )
                }
            }
        )

        // Sound & Music - FB#12: SoundManager ile entegre edildi
        SwitchSetting(
            title = rememberLocalizedText("sound_effects"),
            subtitle = rememberLocalizedText("sound_effects_desc"),
            checked = settings.gameSettings.soundEnabled,
            onCheckedChange = { enabled ->
                // SoundManager ile global mute kontrolü
                soundManager.setGlobalMute(!enabled)

                PersistentDataManager.updateSettingsData { settingsData ->
                    settingsData.copy(
                        gameSettings = settingsData.gameSettings.copy(
                            soundEnabled = enabled
                        )
                    )
                }
            }
        )

        SwitchSetting(
            title = rememberLocalizedText("background_music"),
            subtitle = rememberLocalizedText("background_music_desc"),
            checked = settings.gameSettings.musicEnabled,
            onCheckedChange = { enabled ->
                // Müziği SoundManager ile kontrol et
                if (enabled) {
                    // Müziği başlat (eğer bir track ayarlanmışsa)
                    soundManager.resumeMusic()
                } else {
                    soundManager.pauseMusic()
                }

                PersistentDataManager.updateSettingsData { settingsData ->
                    settingsData.copy(
                        gameSettings = settingsData.gameSettings.copy(
                            musicEnabled = enabled
                        )
                    )
                }
            }
        )
    }
}