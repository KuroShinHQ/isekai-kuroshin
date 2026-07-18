package com.example.isekaikuroshin.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.isekaikuroshin.R

/**
 * G129: System Notification Helper
 * Handles Android system notifications (appears in notification bar)
 *
 * FEATURES:
 * - Create notification channels (required for Android 8.0+)
 * - Show notifications with custom sound
 * - Handle notification tap (open specific app screen)
 * - Bypass Doze mode (high priority notifications)
 */
object SystemNotificationHelper {
    private const val TAG = "SystemNotificationHelper"

    // Notification channel IDs
    private const val CHANNEL_ALARM = "alarm_channel"
    private const val CHANNEL_REMINDER = "reminder_channel"
    private const val CHANNEL_GENERAL = "general_channel"

    /**
     * Create notification channels (Android 8.0+)
     * Call this once during app initialization
     *
     * @param context Application context
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Alarm Channel (High priority, bypass Doze mode)
            val alarmChannel = NotificationChannel(
                CHANNEL_ALARM,
                "Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily alarms and reminders"
                enableVibration(true)
                setShowBadge(true)
            }

            // Reminder Channel (Default priority)
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDER,
                "Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Practice reminders"
                enableVibration(true)
            }

            // General Channel (Low priority)
            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "General Notifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "App updates and info"
            }

            notificationManager.createNotificationChannel(alarmChannel)
            notificationManager.createNotificationChannel(reminderChannel)
            notificationManager.createNotificationChannel(generalChannel)

            GameLogger.logSystem("✅ Notification channels created")
        }
    }

    /**
     * Show alarm notification (triggered by AlarmReceiver)
     *
     * @param context Application context
     * @param notificationId Unique notification ID
     * @param title Notification title
     * @param message Notification message
     * @param soundUri Custom sound URI (null = default)
     * @param targetRoute App route to open when tapped
     */
    fun showAlarmNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        soundUri: Uri? = null,
        targetRoute: String = ""
    ) {
        // Create intent for opening app when notification is tapped
        val intent = Intent(context, com.example.isekaikuroshin.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_ROUTE", targetRoute)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_notification) // TODO-G129: Add notification icon to res/drawable
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority to bypass Doze mode
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true) // Dismiss when tapped
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Set custom sound if provided
        if (soundUri != null) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // For Android 8.0+, sound must be set on channel
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channel = notificationManager.getNotificationChannel(CHANNEL_ALARM)
                channel?.setSound(soundUri, audioAttributes)
            } else {
                // For older versions, set sound on notification
                notificationBuilder.setSound(soundUri, AudioAttributes.CONTENT_TYPE_SONIFICATION)
            }
        } else {
            // Default sound
            notificationBuilder.setDefaults(NotificationCompat.DEFAULT_SOUND)
        }

        // Show notification
        try {
            val notificationManagerCompat = NotificationManagerCompat.from(context)
            notificationManagerCompat.notify(notificationId, notificationBuilder.build())
            GameLogger.logSystem("✅ System notification shown: ID=$notificationId, Title=$title")
        } catch (e: SecurityException) {
            GameLogger.logError(TAG, "❌ POST_NOTIFICATIONS permission not granted: ${e.message}")
        }
    }

    /**
     * Show general reminder notification
     *
     * @param context Application context
     * @param title Notification title
     * @param message Notification message
     */
    fun showReminderNotification(
        context: Context,
        title: String,
        message: String
    ) {
        val notificationId = System.currentTimeMillis().toInt()

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            val notificationManagerCompat = NotificationManagerCompat.from(context)
            notificationManagerCompat.notify(notificationId, notificationBuilder.build())
        } catch (e: SecurityException) {
            GameLogger.logError(TAG, "❌ POST_NOTIFICATIONS permission not granted: ${e.message}")
        }
    }

    /**
     * Cancel a specific notification
     *
     * @param context Application context
     * @param notificationId Notification ID to cancel
     */
    fun cancelNotification(context: Context, notificationId: Int) {
        val notificationManagerCompat = NotificationManagerCompat.from(context)
        notificationManagerCompat.cancel(notificationId)
    }

    /**
     * Check if POST_NOTIFICATIONS permission is granted (Android 13+)
     *
     * @param context Application context
     * @return true if granted or not needed
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required before Android 13
        }
    }
}
