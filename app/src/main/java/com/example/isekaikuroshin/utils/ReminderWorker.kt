package com.example.isekaikuroshin.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.isekaikuroshin.MainActivity
import com.example.isekaikuroshin.R
import com.example.isekaikuroshin.data.LanguageManager
import com.example.isekaikuroshin.data.PersistentDataManager
import kotlinx.coroutines.flow.first

/**
 * GÖREV I: ReminderWorker - Push notification gönderen WorkManager worker
 *
 * 3 tür hatırlatıcı:
 * - EXERCISE: 3 gündür egzersiz yapmadın
 * - MEDITATION: 2 gündür meditasyon yapmadın
 * - JOURNAL: 7 gündür günlük yazmadın
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val reminderType = inputData.getString("type") ?: return Result.failure()

        android.util.Log.d("ReminderWorker", "🔔 $reminderType hatırlatıcısı kontrol ediliyor...")

        // Ayarları kontrol et
        val gameData = PersistentDataManager.gameData.first()
        val settings = gameData.settingsData.notificationSettings

        if (!settings.smartRemindersEnabled) {
            android.util.Log.d("ReminderWorker", "❌ Akıllı hatırlatıcılar kapalı")
            return Result.success()
        }

        val now = System.currentTimeMillis()
        val dayInMillis = 24 * 60 * 60 * 1000L

        when (reminderType) {
            "EXERCISE" -> {
                if (!settings.exerciseRemindersEnabled) return Result.success()

                // TODO: Health Hub verisi eklendiğinde gerçek tarihi kullan
                // Şimdilik her zaman gönder (test için)
                sendNotification(
                    title = LanguageManager.getText("notification_exercise_reminder"),
                    message = LanguageManager.getText("notification_exercise_body"),
                    channelId = "health_reminders",
                    navigateTo = "health_hub"
                )
            }

            "MEDITATION" -> {
                if (!settings.meditationRemindersEnabled) return Result.success()

                sendNotification(
                    title = LanguageManager.getText("notification_meditation_reminder"),
                    message = LanguageManager.getText("notification_meditation_body"),
                    channelId = "health_reminders",
                    navigateTo = "health_hub"
                )
            }

            "JOURNAL" -> {
                if (!settings.journalRemindersEnabled) return Result.success()

                sendNotification(
                    title = LanguageManager.getText("notification_journal_reminder"),
                    message = LanguageManager.getText("notification_journal_body"),
                    channelId = "health_reminders",
                    navigateTo = "journal"
                )
            }
        }

        return Result.success()
    }

    private fun sendNotification(
        title: String,
        message: String,
        channelId: String,
        navigateTo: String
    ) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", navigateTo)
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)  // TODO: Uygun ikon ekle
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext)
                .notify(System.currentTimeMillis().toInt(), notification)

            android.util.Log.d("ReminderWorker", "✅ Bildirim gönderildi: $title")
        } catch (e: SecurityException) {
            android.util.Log.e("ReminderWorker", "❌ Bildirim izni yok!", e)
        }
    }
}
