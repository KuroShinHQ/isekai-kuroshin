package com.example.isekaikuroshin.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import java.util.Calendar

/**
 * G129: Alarm Scheduler
 * Handles scheduling and canceling alarms using AlarmManager
 *
 * FEATURES:
 * - Critical alarms (bypass Doze mode)
 * - Custom alarm sound (user-selected from device)
 * - Repeating daily alarms
 * - Persistent across reboots (BOOT_COMPLETED receiver)
 */
object AlarmScheduler {
    private const val TAG = "AlarmScheduler"

    /**
     * Schedule a daily repeating alarm
     *
     * @param context Application context
     * @param alarmId Unique alarm ID (for cancellation)
     * @param hourOfDay 0-23
     * @param minute 0-59
     * @param title Alarm title (for notification)
     * @param message Alarm message (for notification)
     * @param soundUri Custom sound URI (null = default sound)
     * @param targetRoute App route to open when tapped (e.g., "health_hub")
     */
    fun scheduleDailyAlarm(
        context: Context,
        alarmId: Int,
        hourOfDay: Int,
        minute: Int,
        title: String,
        message: String,
        soundUri: Uri? = null,
        targetRoute: String = ""
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Check permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                GameLogger.logError("AlarmScheduler", "❌ SCHEDULE_EXACT_ALARM permission not granted!")
                return
            }
        }

        // Create intent for AlarmReceiver
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_TITLE", title)
            putExtra("ALARM_MESSAGE", message)
            putExtra("ALARM_SOUND_URI", soundUri?.toString())
            putExtra("ALARM_TARGET_ROUTE", targetRoute)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calculate first trigger time (today or tomorrow at specified hour:minute)
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If time has passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // Schedule repeating alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0+ (Marshmallow): Use setExactAndAllowWhileIdle for Doze mode compatibility
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY, // 24 hours
                pendingIntent
            )
        } else {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        }

        GameLogger.logSystem("✅ Alarm scheduled: ID=$alarmId, Time=${hourOfDay}:${minute}")
    }

    /**
     * Cancel a scheduled alarm
     *
     * @param context Application context
     * @param alarmId Unique alarm ID
     */
    fun cancelAlarm(context: Context, alarmId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()

        GameLogger.logSystem("🔕 Alarm canceled: ID=$alarmId")
    }

    /**
     * Check if exact alarms can be scheduled (Android 12+)
     *
     * @param context Application context
     * @return true if permission granted or not needed
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    /**
     * Open system settings to grant SCHEDULE_EXACT_ALARM permission (Android 12+)
     *
     * @param context Application context
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun openAlarmPermissionSettings(context: Context) {
        val intent = Intent(
            android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:${context.packageName}")
        )
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    /**
     * Predefined alarm IDs (to avoid conflicts)
     */
    object AlarmIds {
        const val LANGUAGE_LEARNING_DAILY = 1001
        const val EXERCISE_DAILY = 1002
        const val MEDITATION_DAILY = 1003
        const val CUSTOM_REMINDER_1 = 2001
        const val CUSTOM_REMINDER_2 = 2002
        const val CUSTOM_REMINDER_3 = 2003
    }
}
