package com.example.isekaikuroshin.utils

import android.annotation.SuppressLint
import android.util.Log
import androidx.navigation.NavController

object NavigationDebugger {
    private const val TAG = "🧭 NAVIGATION_DEBUG"

    private val routeNames = mapOf(
        "onboarding" to "🎬 Onboarding Screen",
        "transition" to "⚡ Transition Screen",
        "dashboard" to "🏠 Dashboard Screen",
        "inventory" to "🎒 Inventory Screen",
        "adventure" to "⚔️ Adventure Screen",
        "journal" to "📖 Journal Screen",
        "social" to "👥 Social Menu",
        "settings" to "⚙️ Settings Screen",
        "ai_dialog" to "🤖 AI Dialog Screen",
        "map" to "🗺️ Map Screen",
        "characters" to "👤 Character Catalog",
        "character_catalog" to "👤 Character Catalog Alt",
        "friends" to "👫 Friends Screen",
        "quests" to "📋 Quests Screen",
        "guild" to "🏰 Guild Screen",
        "leaderboard" to "🏆 Leaderboard Screen",
        "death_archive" to "💀 Death Archive"
    )

    fun logNavigationAttempt(from: String, to: String, navController: NavController) {
        val fromName = routeNames[from] ?: "❓ $from"
        val toName = routeNames[to] ?: "❓ $to"

        Log.d(TAG, "═══════════════════════════════════════")
        Log.d(TAG, "🚀 NAVIGATION ATTEMPT")
        Log.d(TAG, "FROM: $fromName")
        Log.d(TAG, "TO: $toName")
        Log.d(TAG, "Current Stack Size: ${getCurrentStackSize(navController)}")
        Log.d(TAG, "═══════════════════════════════════════")
    }

    fun logNavigationSuccess(route: String) {
        val routeName = routeNames[route] ?: "❓ $route"
        Log.d(TAG, "✅ NAVIGATION SUCCESS: $routeName")
    }

    fun logNavigationError(route: String, error: Exception) {
        val routeName = routeNames[route] ?: "❓ $route"
        Log.e(TAG, "❌ NAVIGATION ERROR to $routeName")
        Log.e(TAG, "Error: ${error.message}")
        Log.e(TAG, "Stack trace:", error)
    }

    fun logScreenComposition(screenName: String) {
        val routeName = routeNames[screenName] ?: "❓ $screenName"
        Log.d(TAG, "🎨 COMPOSING: $routeName")
    }

    fun logScreenDisposal(screenName: String) {
        val routeName = routeNames[screenName] ?: "❓ $screenName"
        Log.d(TAG, "🗑️ DISPOSING: $routeName")
    }

    @SuppressLint("RestrictedApi")
    fun logCurrentBackStack(navController: NavController) {
        Log.d(TAG, "📚 CURRENT BACK STACK:")
        try {
            val backStack = navController.currentBackStack.value
            backStack.forEachIndexed { index, entry ->
                val routeName = routeNames[entry.destination.route] ?: entry.destination.route
                Log.d(TAG, "  [$index] $routeName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading back stack: ${e.message}")
        }
    }

    @SuppressLint("RestrictedApi")
    private fun getCurrentStackSize(navController: NavController): Int {
        return try {
            navController.currentBackStack.value.size
        } catch (e: Exception) {
            -1
        }
    }

    fun logButtonClick(buttonName: String, targetRoute: String) {
        val routeName = routeNames[targetRoute] ?: "❓ $targetRoute"
        Log.d(TAG, "🔘 BUTTON CLICKED: '$buttonName' → $routeName")
    }

    fun logSocialMenuToggle(isExpanded: Boolean) {
        Log.d(TAG, "🔄 SOCIAL MENU: ${if (isExpanded) "EXPANDED" else "COLLAPSED"}")
    }

    fun logCriticalError(location: String, error: String, exception: Exception? = null) {
        Log.e(TAG, "🚨 CRITICAL ERROR in $location")
        Log.e(TAG, "Error: $error")
        exception?.let {
            Log.e(TAG, "Exception:", it)
        }
    }
}