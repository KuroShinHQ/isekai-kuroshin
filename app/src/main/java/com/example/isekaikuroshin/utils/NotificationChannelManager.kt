package com.example.isekaikuroshin.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.isekaikuroshin.data.LanguageManager

/**
 * GÖREV I: NotificationChannelManager - Bildirim kanallarını yönetir (Android 8.0+)
 */
object NotificationChannelManager {

    private const val HEALTH_REMINDERS_CHANNEL_ID = "health_reminders"
    private const val QUEST_UPDATES_CHANNEL_ID = "quest_updates"
    private const val GENERAL_CHANNEL_ID = "general_notifications"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Health Reminders Channel
            val healthChannel = NotificationChannel(
                HEALTH_REMINDERS_CHANNEL_ID,
                LanguageManager.getText("notification_channel_health"),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = LanguageManager.getText("notification_channel_health_desc")
                setSound(null, null) // SILENT - No notification sound
            }

            // Quest Updates Channel
            val questChannel = NotificationChannel(
                QUEST_UPDATES_CHANNEL_ID,
                LanguageManager.getText("notification_channel_quests"),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = LanguageManager.getText("notification_channel_quests_desc")
                setSound(null, null) // SILENT - No notification sound
            }

            // General Channel
            val generalChannel = NotificationChannel(
                GENERAL_CHANNEL_ID,
                LanguageManager.getText("notification_channel_general"),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = LanguageManager.getText("notification_channel_general_desc")
                setSound(null, null) // SILENT - No notification sound
            }

            notificationManager.createNotificationChannels(listOf(
                healthChannel, questChannel, generalChannel
            ))

            android.util.Log.d("NotificationChannels", "✅ Bildirim kanalları oluşturuldu")
        }
    }
}
