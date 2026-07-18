package com.example.isekaikuroshin.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * G129: Alarm Broadcast Receiver
 * Receives alarm broadcasts and triggers system notifications
 *
 * FEATURES:
 * - Receives alarm intents from AlarmManager
 * - Triggers system notification with custom sound
 * - Handles BOOT_COMPLETED to reschedule alarms after reboot
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                // Reschedule alarms after device reboot
                // TODO-G129: Load saved alarms from SharedPreferences and reschedule
                GameLogger.logSystem("📱 Device rebooted - rescheduling alarms...")
                rescheduleAlarms(context)
            }

            else -> {
                // Regular alarm triggered
                handleAlarm(context, intent)
            }
        }
    }

    private fun handleAlarm(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        val title = intent.getStringExtra("ALARM_TITLE") ?: "Reminder"
        val message = intent.getStringExtra("ALARM_MESSAGE") ?: "Time to practice!"
        val soundUriString = intent.getStringExtra("ALARM_SOUND_URI")
        val targetRoute = intent.getStringExtra("ALARM_TARGET_ROUTE") ?: ""

        val soundUri = soundUriString?.let { Uri.parse(it) }

        GameLogger.logSystem("⏰ Alarm triggered: ID=$alarmId, Title=$title")

        // Show system notification
        SystemNotificationHelper.showAlarmNotification(
            context = context,
            notificationId = alarmId,
            title = title,
            message = message,
            soundUri = soundUri,
            targetRoute = targetRoute
        )
    }

    private fun rescheduleAlarms(context: Context) {
        // TODO-G129: Load alarm preferences from SharedPreferences
        // and reschedule all active alarms using AlarmScheduler

        // Example:
        // val prefs = context.getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)
        // val languageLearningEnabled = prefs.getBoolean("language_learning_alarm_enabled", false)
        // if (languageLearningEnabled) {
        //     val hour = prefs.getInt("language_learning_hour", 9)
        //     val minute = prefs.getInt("language_learning_minute", 0)
        //     AlarmScheduler.scheduleDailyAlarm(context, AlarmScheduler.AlarmIds.LANGUAGE_LEARNING_DAILY, hour, minute, ...)
        // }

        GameLogger.logSystem("✅ Alarms rescheduled after boot")
    }
}
