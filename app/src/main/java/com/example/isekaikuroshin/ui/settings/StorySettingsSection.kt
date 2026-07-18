package com.example.isekaikuroshin.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.isekaikuroshin.data.*
import com.example.isekaikuroshin.data.rememberLocalizedText
import com.example.isekaikuroshin.ui.theme.DashboardColorScheme

@Composable
fun StorySettingsSection(
    settings: SettingsData,
    dashboardColors: DashboardColorScheme
) {
    SettingsCategory(
        title = rememberLocalizedText("story_settings"),
        icon = Icons.AutoMirrored.Filled.MenuBook,
        dashboardColors = dashboardColors
    ) {
        // AI Story Mode Selection
        DropdownSetting(
            title = "AI Hikaye Modu",
            subtitle = settings.storySettings.aiStoryMode,
            options = listOf("PDF'e Bağlı", "Tamamen İnteraktif"),
            selectedOption = if (settings.storySettings.aiStoryMode == "PDF_BOUND") "PDF'e Bağlı" else "Tamamen İnteraktif",
            onOptionSelected = { selected ->
                val mode = if (selected == "PDF'e Bağlı") "PDF_BOUND" else "INTERACTIVE"
                PersistentDataManager.updateSettingsData { settingsData ->
                    settingsData.copy(
                        storySettings = settingsData.storySettings.copy(
                            aiStoryMode = mode
                        )
                    )
                }
            }
        )

        SwitchSetting(
            title = rememberLocalizedText("hide_json"),
            subtitle = rememberLocalizedText("hide_json_desc"),
            checked = settings.storySettings.hideJSON,
            onCheckedChange = { enabled ->
                PersistentDataManager.updateSettingsData { settingsData ->
                    settingsData.copy(
                        storySettings = settingsData.storySettings.copy(
                            hideJSON = enabled
                        )
                    )
                }
            }
        )

        SwitchSetting(
            title = rememberLocalizedText("character_hints"),
            subtitle = rememberLocalizedText("character_hints_desc"),
            checked = settings.storySettings.showCharacterHints,
            onCheckedChange = { enabled ->
                PersistentDataManager.updateSettingsData { settingsData ->
                    settingsData.copy(
                        storySettings = settingsData.storySettings.copy(
                            showCharacterHints = enabled
                        )
                    )
                }
            }
        )

        // External Story Source
        OutlinedTextField(
            value = settings.storySettings.externalStorySource,
            onValueChange = { newText ->
                PersistentDataManager.updateSettingsData { settingsData ->
                    settingsData.copy(
                        storySettings = settingsData.storySettings.copy(
                            externalStorySource = newText
                        )
                    )
                }
            },
            label = { Text("Hikaye Kaynağı") },
            placeholder = { Text("Oyunun atmosferini ve anlatısını şekillendirmek için buraya bir metin yapıştırın...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        )
    }
}