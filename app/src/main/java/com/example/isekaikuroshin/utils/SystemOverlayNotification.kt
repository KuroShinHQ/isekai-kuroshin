package com.example.isekaikuroshin.utils

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TODO-HUB-14: System Overlay Notification Manager
 *
 * Oyun içi bildirimler için merkezi yönetici
 * (Stat artışı, başarı kazanımı, vb.)
 */
@Singleton
class SystemOverlayNotification @Inject constructor() {

    companion object {
        private const val TAG = "SystemOverlayNotification"
    }

    /**
     * Bildirim tipi
     */
    enum class NotificationType {
        STAT_INCREASE,      // Stat artışı
        ACHIEVEMENT,        // Başarı
        LEVEL_UP,          // Seviye atlama
        ITEM_ACQUIRED,     // Eşya kazanımı
        QUEST_UPDATE,      // Görev güncellemesi
        INFO               // Genel bilgi
    }

    /**
     * Bildirim verisi
     */
    data class Notification(
        val title: String,
        val message: String,
        val type: NotificationType,
        val duration: Long = 3000L, // ms
        val timestamp: Long = System.currentTimeMillis()
    )

    private val _currentNotification = MutableStateFlow<Notification?>(null)
    val currentNotification: StateFlow<Notification?> = _currentNotification.asStateFlow()

    /**
     * TODO-HUB-14: Bildirim göster
     *
     * @param title Bildirim başlığı
     * @param message Bildirim mesajı
     * @param type Bildirim tipi
     * @param duration Gösterim süresi (ms)
     */
    fun show(
        title: String,
        message: String,
        type: NotificationType = NotificationType.INFO,
        duration: Long = 3000L
    ) {
        try {
            val notification = Notification(
                title = title,
                message = message,
                type = type,
                duration = duration
            )

            _currentNotification.value = notification

            GameLogger.logSystem("TODO-HUB-14: Bildirim gösterildi: $title - $message")
            Log.d(TAG, "Showing notification: $title - $message")

            // Auto dismiss after duration
            CoroutineScope(Dispatchers.Main).launch {
                delay(duration)
                dismiss()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to show notification", e)
            GameLogger.logError(TAG, "TODO-HUB-14: Bildirim gösterilemedi", e)
        }
    }

    /**
     * TODO-HUB-14: Bildirimi kapat
     */
    fun dismiss() {
        _currentNotification.value = null
        GameLogger.logSystem("TODO-HUB-14: Bildirim kapatıldı")
    }

    /**
     * TODO-HUB-14: Stat artışı bildirimi (kısayol)
     */
    fun showStatIncrease(statName: String, increase: Int) {
        show(
            title = "📊 $statName Arttı!",
            message = "$statName stat'ınız +$increase arttı!",
            type = NotificationType.STAT_INCREASE,
            duration = 3500L
        )
    }

    /**
     * TODO-HUB-14: Başarı bildirimi (kısayol)
     */
    fun showAchievement(achievementName: String) {
        show(
            title = "🏆 Başarı Kazanıldı!",
            message = achievementName,
            type = NotificationType.ACHIEVEMENT,
            duration = 4000L
        )
    }

    /**
     * TODO-HUB-14: Seviye atlama bildirimi (kısayol)
     */
    fun showLevelUp(newLevel: Int) {
        show(
            title = "⬆️ Seviye Atladınız!",
            message = "Tebrikler! Seviye $newLevel'e ulaştınız!",
            type = NotificationType.LEVEL_UP,
            duration = 5000L
        )
    }
}
