package com.example.isekaikuroshin.utils

import android.content.Context
import androidx.work.*
import com.example.isekaikuroshin.data.PersistentDataManager
import com.example.isekaikuroshin.data.UserSessionLog
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * GÖREV I: SmartScheduler - Akıllı bildirim zamanlama sistemi
 *
 * İstatistiksel analiz ile kullanıcının en aktif olduğu saat aralığını (peak hours) belirler
 * ve bildirimleri bu aralıkta gönderir.
 *
 * Algoritma:
 * 1. Son 100 session kaydını analiz et
 * 2. Her saat için session sayısını hesapla
 * 3. 3 saatlik sliding window ile en yoğun aralığı bul
 * 4. Bildirimler bu aralıkta rastgele bir zamanda gönderilir
 */
object SmartScheduler {

    /**
     * Kullanıcının son session verilerini analiz ederek peak hours hesaplar
     *
     * @param sessions Son 100 session kaydı
     * @return Pair(startHour, endHour) - Örn: (18, 21) → 18:00-21:00
     */
    fun analyzePeakHours(sessions: List<UserSessionLog>): Pair<Int, Int> {
        if (sessions.isEmpty()) {
            // Default: Akşam saatleri
            return Pair(18, 21)
        }

        // Her saat için session sayısını say (0-23)
        val hourCounts = IntArray(24) { 0 }

        sessions.forEach { session ->
            val calendar = Calendar.getInstance().apply {
                timeInMillis = session.sessionStart
            }
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            hourCounts[hour]++
        }

        // 3 saatlik sliding window ile en yoğun aralığı bul
        var maxCount = 0
        var peakStartHour = 18

        for (i in 0..21) {
            val windowCount = hourCounts[i] + hourCounts[i + 1] + hourCounts[i + 2]
            if (windowCount > maxCount) {
                maxCount = windowCount
                peakStartHour = i
            }
        }

        android.util.Log.d("SmartScheduler", "📊 Peak hours analizi: ${peakStartHour}:00 - ${peakStartHour + 3}:00 (${maxCount} session)")

        return Pair(peakStartHour, peakStartHour + 3)
    }

    /**
     * Akıllı bildirim zamanlaması yapar
     * Peak hours içinde rastgele bir zaman seçer
     *
     * @param context Android context
     * @param reminderType "EXERCISE", "MEDITATION", "JOURNAL"
     * @param peakHours Peak hours aralığı
     */
    fun scheduleSmartReminder(
        context: Context,
        reminderType: String,
        peakHours: Pair<Int, Int>
    ) {
        val (startHour, endHour) = peakHours

        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)

        val targetCalendar = Calendar.getInstance().apply {
            if (currentHour >= endHour) {
                // Bugün peak hours geçti, yarın gönder
                add(Calendar.DAY_OF_YEAR, 1)
            }

            // Peak hours içinde rastgele bir saat ve dakika seç
            val randomHour = Random.nextInt(startHour, endHour)
            val randomMinute = Random.nextInt(0, 60)

            set(Calendar.HOUR_OF_DAY, randomHour)
            set(Calendar.MINUTE, randomMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val delay = targetCalendar.timeInMillis - System.currentTimeMillis()

        android.util.Log.d("SmartScheduler", "⏰ $reminderType bildirimi ${targetCalendar.time} için zamanlandı")

        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("type" to reminderType))
            .addTag("smart_reminder_$reminderType")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "reminder_$reminderType",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    /**
     * Session başlangıcını kaydet
     * MainActivity.onCreate()'de çağrılır
     */
    fun logSessionStart() {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        val session = UserSessionLog(
            sessionStart = now,
            sessionEnd = 0,  // Henüz bitmedi
            dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK),
            hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        )

        PersistentDataManager.updateGameData { data ->
            val updatedSessions = data.analyticsData.sessionLogs + session

            data.copy(
                analyticsData = data.analyticsData.copy(
                    sessionLogs = updatedSessions.takeLast(100)  // Son 100 session sakla
                )
            )
        }

        android.util.Log.d("SmartScheduler", "📱 Session başladı: ${calendar.time}")
    }

    /**
     * Session bitişini kaydet
     * MainActivity.onDestroy()'de veya onPause()'da çağrılır
     */
    fun logSessionEnd() {
        val now = System.currentTimeMillis()

        PersistentDataManager.updateGameData { data ->
            val sessions = data.analyticsData.sessionLogs

            if (sessions.isNotEmpty()) {
                val lastSession = sessions.last()

                if (lastSession.sessionEnd == 0L) {
                    // Son session'ı güncelle
                    val updatedSession = lastSession.copy(sessionEnd = now)
                    val updatedSessions = sessions.dropLast(1) + updatedSession

                    android.util.Log.d("SmartScheduler", "📴 Session bitti. Süre: ${(now - lastSession.sessionStart) / 1000 / 60} dakika")

                    data.copy(
                        analyticsData = data.analyticsData.copy(
                            sessionLogs = updatedSessions
                        )
                    )
                } else {
                    data
                }
            } else {
                data
            }
        }
    }

    /**
     * Peak hours'ı yeniden hesapla ve güncelle
     * Günde 1 kez çalıştırılır
     */
    fun updatePeakHours(context: Context) {
        val gameData = PersistentDataManager.gameData.value
        val sessions = gameData.analyticsData.sessionLogs

        val (newStart, newEnd) = analyzePeakHours(sessions)

        PersistentDataManager.updateGameData { data ->
            data.copy(
                analyticsData = data.analyticsData.copy(
                    peakHoursStart = newStart,
                    peakHoursEnd = newEnd,
                    lastAnalysisDate = System.currentTimeMillis()
                )
            )
        }

        android.util.Log.d("SmartScheduler", "🔄 Peak hours güncellendi: $newStart:00 - $newEnd:00")

        // Tüm hatırlatıcıları yeni peak hours'a göre yeniden zamanla
        rescheduleAllReminders(context, Pair(newStart, newEnd))
    }

    /**
     * Tüm hatırlatıcıları yeniden zamanlar
     */
    private fun rescheduleAllReminders(context: Context, peakHours: Pair<Int, Int>) {
        val settings = PersistentDataManager.gameData.value.settingsData.notificationSettings

        if (!settings.smartRemindersEnabled) {
            android.util.Log.d("SmartScheduler", "❌ Akıllı hatırlatıcılar kapalı, zamanlanmadı")
            return
        }

        if (settings.exerciseRemindersEnabled) {
            scheduleSmartReminder(context, "EXERCISE", peakHours)
        }

        if (settings.meditationRemindersEnabled) {
            scheduleSmartReminder(context, "MEDITATION", peakHours)
        }

        if (settings.journalRemindersEnabled) {
            scheduleSmartReminder(context, "JOURNAL", peakHours)
        }
    }

    /**
     * Uygulama başlangıcında tüm hatırlatıcıları zamanla
     */
    fun initializeScheduler(context: Context) {
        val gameData = PersistentDataManager.gameData.value
        val peakHours = Pair(
            gameData.analyticsData.peakHoursStart,
            gameData.analyticsData.peakHoursEnd
        )

        android.util.Log.d("SmartScheduler", "🚀 SmartScheduler initialize ediliyor. Peak hours: $peakHours")

        rescheduleAllReminders(context, peakHours)
    }
}
