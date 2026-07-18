package com.example.isekaikuroshin.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.isekaikuroshin.data.PersistentDataManager
import com.example.isekaikuroshin.data.SettingsData
import com.example.isekaikuroshin.data.rememberLocalizedText
import com.example.isekaikuroshin.ui.theme.DashboardColorScheme

/**
 * GÖREV I: NotificationSettingsSection - Bildirim ayarları UI
 *
 * 3 ana toggle:
 * 1. Oyun İlerlemesi Bildirimleri (Mikro geri bildirimler)
 * 2. Görev Güncellemeleri Bildirimleri (System Overlay)
 * 3. Akıllı Hatırlatıcılar (Push notifications)
 *    - Alt toggles: Exercise, Meditation, Journal
 */
@Composable
fun NotificationSettingsSection(
    settings: SettingsData,
    dashboardColors: DashboardColorScheme
) {
    SettingsCategory(
        title = rememberLocalizedText("notification_settings"),
        icon = Icons.Default.Notifications,
        dashboardColors = dashboardColors
    ) {
        // 1. Oyun İlerlemesi Bildirimleri (Mikro Geri Bildirimler)
        SwitchSetting(
            title = rememberLocalizedText("game_progress_notifications"),
            subtitle = rememberLocalizedText("game_progress_notifications_desc"),
            checked = settings.notificationSettings.gameProgressNotifications,
            onCheckedChange = { enabled ->
                PersistentDataManager.updateSettingsData { settingsData ->
                    settingsData.copy(
                        notificationSettings = settingsData.notificationSettings.copy(
                            gameProgressNotifications = enabled
                        )
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Görev Güncellemeleri Bildirimleri (System Overlay)
        SwitchSetting(
            title = rememberLocalizedText("quest_update_notifications"),
            subtitle = rememberLocalizedText("quest_update_notifications_desc"),
            checked = settings.notificationSettings.questUpdateNotifications,
            onCheckedChange = { enabled ->
                PersistentDataManager.updateSettingsData { settingsData ->
                    settingsData.copy(
                        notificationSettings = settingsData.notificationSettings.copy(
                            questUpdateNotifications = enabled
                        )
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Akıllı Hatırlatıcılar (Push Notifications - Ana Toggle)
        SwitchSetting(
            title = rememberLocalizedText("smart_reminders"),
            subtitle = rememberLocalizedText("smart_reminders_desc"),
            checked = settings.notificationSettings.smartRemindersEnabled,
            onCheckedChange = { enabled ->
                PersistentDataManager.updateSettingsData { settingsData ->
                    settingsData.copy(
                        notificationSettings = settingsData.notificationSettings.copy(
                            smartRemindersEnabled = enabled
                        )
                    )
                }
            }
        )

        // Alt toggles (sadece smartRemindersEnabled = true ise göster)
        if (settings.notificationSettings.smartRemindersEnabled) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, top = 8.dp)
            ) {
                // 3a. Egzersiz Hatırlatıcıları (3 gün)
                SwitchSetting(
                    title = rememberLocalizedText("exercise_reminders"),
                    subtitle = rememberLocalizedText("exercise_reminders_desc"),
                    checked = settings.notificationSettings.exerciseRemindersEnabled,
                    onCheckedChange = { enabled ->
                        PersistentDataManager.updateSettingsData { settingsData ->
                            settingsData.copy(
                                notificationSettings = settingsData.notificationSettings.copy(
                                    exerciseRemindersEnabled = enabled
                                )
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 3b. Meditasyon Hatırlatıcıları (2 gün)
                SwitchSetting(
                    title = rememberLocalizedText("meditation_reminders"),
                    subtitle = rememberLocalizedText("meditation_reminders_desc"),
                    checked = settings.notificationSettings.meditationRemindersEnabled,
                    onCheckedChange = { enabled ->
                        PersistentDataManager.updateSettingsData { settingsData ->
                            settingsData.copy(
                                notificationSettings = settingsData.notificationSettings.copy(
                                    meditationRemindersEnabled = enabled
                                )
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 3c. Günlük Hatırlatıcıları (7 gün)
                SwitchSetting(
                    title = rememberLocalizedText("journal_reminders"),
                    subtitle = rememberLocalizedText("journal_reminders_desc"),
                    checked = settings.notificationSettings.journalRemindersEnabled,
                    onCheckedChange = { enabled ->
                        PersistentDataManager.updateSettingsData { settingsData ->
                            settingsData.copy(
                                notificationSettings = settingsData.notificationSettings.copy(
                                    journalRemindersEnabled = enabled
                                )
                            )
                        }
                    }
                )
            }
        }
    }
}
