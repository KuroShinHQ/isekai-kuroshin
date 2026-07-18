package com.example.isekaikuroshin.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Comprehensive Game Logger - Test edilen tüm özellikleri log dosyasına kaydeder
 * Internal storage'da game_logs.txt dosyası oluşturur
 *
 * G141: Log Level System - Catlog spam'ini önlemek için minimum log level
 */
object GameLogger {

    // G141: Minimum log level (Production'da INFO, Development'da DEBUG)
    private var minLogLevel = Log.DEBUG // TODO: Production'da Log.INFO yap

    private var context: Context? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val logFile: File?
        get() = context?.let { File(it.filesDir, "game_logs.txt") }

    fun initialize(appContext: Context) {
        context = appContext.applicationContext
        logSession("=== GAME LOGGER INITIALIZED ===")
    }

    // Ana kategoriler - Debug seviyesi (kritik loglar)
    fun logSession(message: String) = writeLog("SESSION", message, Log.DEBUG)
    fun logTimeSystem(message: String) = writeLog("TIME_SYSTEM", message, Log.DEBUG)
    fun logStorySystem(message: String) = writeLog("STORY_SYSTEM", message, Log.DEBUG)
    fun logAISystem(message: String) = writeLog("AI_SYSTEM", message, Log.DEBUG)
    fun logInventory(message: String) = writeLog("INVENTORY", message, Log.DEBUG)
    fun logStats(message: String) = writeLog("STATS", message, Log.DEBUG)
    fun logDiceSystem(message: String) = writeLog("DICE_SYSTEM", message, Log.DEBUG)
    fun logSystem(message: String) = writeLog("SYSTEM", message, Log.DEBUG)
    fun logSaveSystem(message: String) = writeLog("SAVE_SYSTEM", message, Log.DEBUG)
    fun logDebug(message: String) = writeLog("DEBUG", message, Log.DEBUG)
    fun logOnboarding(message: String) = writeLog("ONBOARDING", message, Log.DEBUG)
    fun logSealPractice(message: String) = writeLog("SEAL_PRACTICE", message, Log.DEBUG)

    // ⚡⚡⚡ BÖLÜM D: VFX Test Loglama
    fun logVfxTest(message: String) = writeLog("VFX_TEST", message, Log.DEBUG)
    fun logVfxTestWarning(message: String) = writeLog("VFX_TEST", message, Log.WARN)
    fun logVfxTestError(message: String, exception: Exception? = null) {
        val fullMessage = "ERROR: $message ${exception?.message ?: ""}"
        writeLog("VFX_TEST", fullMessage, Log.ERROR)
    }

    // Verbose seviyesi (UI recomposition, tekrarlayan loglar)
    fun logVerbose(category: String, message: String) = writeLog(category, message, Log.VERBOSE)

    // G141: INFO ve WARNING level fonksiyonları
    fun logInfo(category: String, message: String) = writeLog(category, message, Log.INFO)
    fun logWarning(category: String, message: String) = writeLog(category, message, Log.WARN)

    // G141: Kritik event'ler için vurgulanmış loglar
    fun logCritical(message: String) = writeLog("🎯 CRITICAL", message, Log.INFO)

    fun logError(category: String, error: String, exception: Exception? = null) {
        val fullMessage = "ERROR in $category: $error ${exception?.message ?: ""}"
        writeLog("ERROR", fullMessage, Log.ERROR)
    }

    private fun writeLog(category: String, message: String, logLevel: Int = Log.DEBUG) {
        // G141: Minimum log level kontrolü - spam'i önle
        if (logLevel < minLogLevel) return

        val timestamp = dateFormat.format(Date())
        val logEntry = "[$timestamp] [$category] $message\n"

        // Console'a log seviyesine göre yazdır
        when (logLevel) {
            Log.VERBOSE -> Log.v("GameLogger", "[$category] $message")
            Log.DEBUG -> Log.d("GameLogger", "[$category] $message")
            Log.INFO -> Log.i("GameLogger", "[$category] $message")
            Log.WARN -> Log.w("GameLogger", "[$category] $message")
            Log.ERROR -> Log.e("GameLogger", "[$category] $message")
            else -> Log.d("GameLogger", "[$category] $message")
        }

        // Dosyaya asenkron yaz (verbose hariç - dosya şişmesin)
        if (logLevel != Log.VERBOSE) {
            try {
                logFile?.appendText(logEntry)
            } catch (e: Exception) {
                Log.e("GameLogger", "Failed to write to log file: ${e.message}")
            }
        }
    }

    // Log dosyasını oku (test için)
    suspend fun readLogFile(): String = withContext(Dispatchers.IO) {
        return@withContext try {
            logFile?.readText() ?: "Log file not found"
        } catch (e: Exception) {
            "Error reading log file: ${e.message}"
        }
    }

    // Log dosyasını temizle
    fun clearLogs() {
        try {
            logFile?.writeText("")
            logSession("=== LOGS CLEARED ===")
        } catch (e: Exception) {
            Log.e("GameLogger", "Failed to clear log file: ${e.message}")
        }
    }

    // Test başlangıcını logla
    fun startTestSession(testName: String) {
        logSession("==========================================")
        logSession("TEST SESSION STARTED: $testName")
        logSession("==========================================")
    }

    // Test sonucunu logla
    fun endTestSession(testName: String, success: Boolean) {
        logSession("==========================================")
        logSession("TEST SESSION ENDED: $testName - ${if (success) "SUCCESS" else "FAILED"}")
        logSession("==========================================")
    }
}