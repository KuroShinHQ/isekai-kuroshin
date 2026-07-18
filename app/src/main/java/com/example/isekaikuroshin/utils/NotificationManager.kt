package com.example.isekaikuroshin.utils

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.example.isekaikuroshin.data.PersistentDataManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val targetRoute: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

enum class NotificationType {
    QUEST_UPDATE,
    CHARACTER_INTERACTION,
    ITEM_COLLECTED,
    LEVEL_UP,
    DEATH_WARNING,
    AUTO_SAVE,
    AI_STATUS,
    GENERAL
}

object NotificationManager {

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _notificationCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val notificationCounts: StateFlow<Map<String, Int>> = _notificationCounts.asStateFlow()

    private val routeNotificationMap = mapOf(
        "journal" to listOf(NotificationType.AUTO_SAVE, NotificationType.GENERAL),
        "inventory" to listOf(NotificationType.ITEM_COLLECTED),
        "character_catalog" to listOf(NotificationType.CHARACTER_INTERACTION),
        "friends" to listOf(NotificationType.CHARACTER_INTERACTION),
        "quests" to listOf(NotificationType.QUEST_UPDATE),
        "ai_dialog" to listOf(NotificationType.AI_STATUS),
        "settings" to listOf(NotificationType.GENERAL)
    )

    suspend fun addNotification(
        id: String,
        title: String,
        message: String,
        type: NotificationType,
        targetRoute: String = ""
    ) {
        // Check if notifications are enabled
        val settings = PersistentDataManager.gameData.first().settingsData
        if (!settings.gameSettings.enableNotifications) {
            return
        }

        val notification = NotificationItem(
            id = id,
            title = title,
            message = message,
            type = type,
            targetRoute = targetRoute
        )

        val currentNotifications = _notifications.value.toMutableList()

        // Remove existing notification with same ID if exists
        currentNotifications.removeAll { it.id == id }

        // Add new notification
        currentNotifications.add(0, notification) // Add to top

        // Keep only last 50 notifications
        if (currentNotifications.size > 50) {
            currentNotifications.subList(50, currentNotifications.size).clear()
        }

        _notifications.value = currentNotifications
        updateNotificationCounts()

        GameLogger.logSession("Notification added: $title")
    }

    fun markAsRead(notificationId: String) {
        val currentNotifications = _notifications.value.toMutableList()
        val index = currentNotifications.indexOfFirst { it.id == notificationId }
        if (index != -1) {
            currentNotifications[index] = currentNotifications[index].copy(isRead = true)
            _notifications.value = currentNotifications
            updateNotificationCounts()
        }
    }

    fun markAllAsRead() {
        val currentNotifications = _notifications.value.map { it.copy(isRead = true) }
        _notifications.value = currentNotifications
        updateNotificationCounts()
    }

    fun clearNotification(notificationId: String) {
        val currentNotifications = _notifications.value.toMutableList()
        currentNotifications.removeAll { it.id == notificationId }
        _notifications.value = currentNotifications
        updateNotificationCounts()
    }

    fun clearAllNotifications() {
        _notifications.value = emptyList()
        updateNotificationCounts()
    }

    private fun updateNotificationCounts() {
        val unreadNotifications = _notifications.value.filter { !it.isRead }
        val counts = mutableMapOf<String, Int>()

        // Count notifications per route
        routeNotificationMap.forEach { (route, types) ->
            val count = unreadNotifications.count { notification ->
                types.contains(notification.type) || notification.targetRoute == route
            }
            if (count > 0) {
                counts[route] = count
            }
        }

        _notificationCounts.value = counts
    }

    fun getNotificationCountForRoute(route: String): Int {
        return _notificationCounts.value[route] ?: 0
    }

    fun hasNotificationForRoute(route: String): Boolean {
        return getNotificationCountForRoute(route) > 0
    }

    fun getNotificationsForRoute(route: String): List<NotificationItem> {
        val types = routeNotificationMap[route] ?: emptyList()
        return _notifications.value.filter { notification ->
            types.contains(notification.type) || notification.targetRoute == route
        }
    }

    fun getTotalUnreadCount(): Int {
        return _notifications.value.count { !it.isRead }
    }

    // Predefined notification creators
    suspend fun notifyItemCollected(itemName: String, quantity: Int = 1) {
        addNotification(
            id = "item_${itemName}_${System.currentTimeMillis()}",
            title = "Item Collected!",
            message = if (quantity > 1) "+$quantity $itemName collected" else "+$itemName collected",
            type = NotificationType.ITEM_COLLECTED,
            targetRoute = "inventory"
        )
    }

    suspend fun notifyCharacterMet(characterName: String) {
        addNotification(
            id = "char_met_${characterName}",
            title = "New Character Met!",
            message = "You've met $characterName",
            type = NotificationType.CHARACTER_INTERACTION,
            targetRoute = "character_catalog"
        )
    }

    suspend fun notifyLevelUp(newLevel: Int) {
        addNotification(
            id = "level_up_$newLevel",
            title = "Level Up!",
            message = "Reached level $newLevel!",
            type = NotificationType.LEVEL_UP
        )
    }

    suspend fun notifyAutoSave() {
        addNotification(
            id = "auto_save_${System.currentTimeMillis()}",
            title = "Auto-saved",
            message = "Progress saved automatically",
            type = NotificationType.AUTO_SAVE,
            targetRoute = "settings"
        )
    }

    suspend fun notifyLowHealth() {
        addNotification(
            id = "low_health",
            title = "⚠️ Critical Health!",
            message = "Your health is critically low!",
            type = NotificationType.DEATH_WARNING
        )
    }

    suspend fun notifyAIStatusChange(status: String) {
        addNotification(
            id = "ai_status_${System.currentTimeMillis()}",
            title = "AI Status Update",
            message = status,
            type = NotificationType.AI_STATUS,
            targetRoute = "ai_dialog"
        )
    }

    suspend fun notifyQuestUpdate(questTitle: String, isCompleted: Boolean = false) {
        addNotification(
            id = "quest_${questTitle}",
            title = if (isCompleted) "Quest Completed!" else "Quest Updated",
            message = questTitle,
            type = NotificationType.QUEST_UPDATE,
            targetRoute = "quests"
        )
    }
}