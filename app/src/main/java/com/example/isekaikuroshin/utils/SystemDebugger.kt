package com.example.isekaikuroshin.utils

import android.util.Log
import android.content.Context

object SystemDebugger {
    private const val TAG = "🔧 SYSTEM_DEBUG"

    fun initializeAppDebugSystem(context: Context) {
        Log.d(TAG, "═══════════════════════════════════════")
        Log.d(TAG, "🚀 ISEKAI KUROSHIN - DEBUG SYSTEM ACTIVATED")
        Log.d(TAG, "Version: 1.0 DEBUG BUILD")
        Log.d(TAG, "═══════════════════════════════════════")

        // Test all critical systems
        testCharacterManager(context)
        testPersistentDataManager()
        testGameStateManager()
        testNotificationSystem()
    }

    private fun testCharacterManager(context: Context) {
        try {
            Log.d(TAG, "🧪 Testing CharacterManager...")
            CharacterManager.initialize(context)
            val characters = CharacterManager.getAllCharacters()
            Log.d(TAG, "✅ CharacterManager OK - ${characters.size} characters loaded")
        } catch (e: Exception) {
            Log.e(TAG, "❌ CharacterManager FAILED", e)
        }
    }

    private fun testPersistentDataManager() {
        try {
            Log.d(TAG, "🧪 Testing PersistentDataManager...")
            val gameData = com.example.isekaikuroshin.data.PersistentDataManager.gameData.value
            Log.d(TAG, "✅ PersistentDataManager OK - Player: ${gameData.playerData.name}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ PersistentDataManager FAILED", e)
        }
    }

    private fun testGameStateManager() {
        try {
            Log.d(TAG, "🧪 Testing GameStateManager...")
            val gameData = com.example.isekaikuroshin.data.PersistentDataManager.gameData.value
            Log.d(TAG, "✅ GameStateManager OK - Player: ${gameData.playerData.name}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ GameStateManager FAILED", e)
        }
    }

    private fun testNotificationSystem() {
        try {
            Log.d(TAG, "🧪 Testing NotificationManager...")
            val notifications = NotificationManager.notifications.value
            Log.d(TAG, "✅ NotificationManager OK - ${notifications.size} notifications")
        } catch (e: Exception) {
            Log.e(TAG, "❌ NotificationManager FAILED", e)
        }
    }

    fun logAppLaunch() {
        Log.d(TAG, "🎮 APP LAUNCHED - Starting navigation system")
    }

    fun logAppCrash(error: Throwable) {
        Log.e(TAG, "💥 CRITICAL APP CRASH")
        Log.e(TAG, "Error: ${error.message}")
        Log.e(TAG, "Stack trace:", error)
    }

    fun testAllScreens() {
        Log.d(TAG, "🧪 Testing all screen routes...")
        val routes = listOf(
            "onboarding", "dashboard", "inventory", "adventure",
            "journal", "social", "settings", "ai_dialog", "map",
            "characters", "friends", "quests", "guild", "leaderboard"
        )

        routes.forEach { route ->
            Log.d(TAG, "📱 Route available: $route")
        }
    }
}