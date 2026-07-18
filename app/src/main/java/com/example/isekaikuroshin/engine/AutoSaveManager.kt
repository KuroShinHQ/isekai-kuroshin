package com.example.isekaikuroshin.engine

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import com.example.isekaikuroshin.data.GameStateManager
import com.example.isekaikuroshin.data.PersistentDataManager
import android.util.Log
import com.example.isekaikuroshin.data.PlayerState // Ensure PlayerState is imported if not covered by wildcard
import com.example.isekaikuroshin.data.GameStateZ7 // Explicit import for clarity
import com.example.isekaikuroshin.data.CollectedItemsZ5 // Explicit import for clarity
import com.example.isekaikuroshin.data.PlayerStatsPersistent // Assuming this is the correct path
import com.example.isekaikuroshin.data.InventoryItemPersistent // Assuming this is the correct path
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoSaveManager @Inject constructor(
    private val gameStateManager: GameStateManager,
    @ApplicationContext private val context: Context
) {
    private var autoSaveJob: Job? = null
    private var isInitialized = false
    private var lastSaveTime = 0L
    private var currentDay = 1

    fun initialize(scope: CoroutineScope) {
        if (isInitialized) return

        PersistentDataManager.initialize(context)
        isInitialized = true

        startAutoSaveLoop(scope)
        Log.d("AutoSaveManager", "AutoSaveManager initialized")
    }

    private fun startAutoSaveLoop(scope: CoroutineScope) {
        autoSaveJob?.cancel()

        autoSaveJob = scope.launch {
            // Combine game state and settings to monitor changes
            combine(
                gameStateManager.gameState,
                PersistentDataManager.gameData
            ) { gameState, persistentData ->
                Triple(gameState, persistentData.settingsData.gameSettings.autoSaveEnabled, persistentData.settingsData.gameSettings.autoSaveInterval)
            }.collect { (gameState, autoSaveEnabled, autoSaveInterval) ->
                val currentTime = System.currentTimeMillis()

                // Check if day has changed (day completion auto-save) - always enabled
                if (gameState.currentDay != currentDay) {
                    Log.d("AutoSaveManager", "Day changed from $currentDay to ${gameState.currentDay}, triggering day completion save")
                    performDayCompletionSave(gameState)
                    currentDay = gameState.currentDay
                }

                // Check if enough time has passed for interval auto-save (only if enabled)
                if (autoSaveEnabled && currentTime - lastSaveTime >= autoSaveInterval * 1000) {
                    Log.d("AutoSaveManager", "Auto-save interval reached, saving game data")
                    performIntervalSave(gameState)
                    lastSaveTime = currentTime
                } else if (!autoSaveEnabled) {
                    // Reset timer when auto-save is disabled
                    lastSaveTime = currentTime
                }
            }
        }
    }

    private suspend fun performDayCompletionSave(gameState: GameStateZ7) {
        try {
            // Update persistent data with current game state
            PersistentDataManager.updatePlayerData { playerData ->
                playerData.copy(
                    name = gameState.playerState.playerName,
                    level = calculatePlayerLevel((gameState.playerState.strength + gameState.playerState.agility + gameState.playerState.intelligence).toLong()),
                    stats = PlayerStatsPersistent(
                        hp = gameState.playerState.currentHealth.toLong(),      // CORRECTED with .toLong()
                        mp = gameState.playerState.currentMana.toLong(),        // CORRECTED with .toLong()
                        stamina = gameState.playerState.stamina.toLong(),   // CORRECTED with .toLong()
                        hunger = gameState.playerState.hunger.toLong(),     // CORRECTED with .toLong()
                        fatigue = gameState.playerState.fatigue.toLong(),   // CORRECTED with .toLong()
                        strength = gameState.playerState.strength.toLong(),      // CORRECTED with .toLong()
                        agility = gameState.playerState.agility.toLong(),       // CORRECTED with .toLong()
                        intelligence = gameState.playerState.intelligence.toLong(),// CORRECTED with .toLong()
                        vitality = gameState.playerState.vitality.toLong(),      // CORRECTED with .toLong()
                        spirit = gameState.playerState.spirit.toLong(),     // CORRECTED with .toLong()
                        luck = gameState.playerState.luck.toLong()          // CORRECTED with .toLong()
                    ),
                    inventory = convertInventoryToPersistent(gameState.collectedItems),
                    equippedItems = gameState.equippedItems.mapValues { it.value.name }
                )
            }

                PersistentDataManager.updateStoryData { storyData ->
                val currentLocation = gameState.knownLocations.find { it.id == gameState.currentLocationId }
                storyData.copy(
                    currentDay = gameState.currentDay,
                    currentPage = gameState.currentPage,
                    storyPages = gameState.storyPages,
                    currentLocation = currentLocation?.name ?: gameState.currentLocationId,
                    currentSeason = gameState.currentSeason.name,
                    currentWeather = gameState.currentWeather.name,
                    totalPlayTime = storyData.totalPlayTime + calculateSessionTime()
                )
            }

            Log.d("AutoSaveManager", "Day completion auto-save completed for day ${gameState.currentDay}")

        } catch (e: Exception) {
            Log.e("AutoSaveManager", "Error during day completion auto-save: ${e.message}")
        }
    }

    private fun performIntervalSave(gameState: GameStateZ7) {
        try {
            // Light-weight interval save (only critical data)
            PersistentDataManager.updatePlayerData { playerData ->
                playerData.copy(
                    stats = PlayerStatsPersistent(
                        hp = gameState.playerState.currentHealth.toLong(),      // CORRECTED with .toLong()
                        mp = gameState.playerState.currentMana.toLong(),        // CORRECTED with .toLong()
                        stamina = gameState.playerState.stamina.toLong(),   // CORRECTED with .toLong()
                        hunger = gameState.playerState.hunger.toLong(),     // CORRECTED with .toLong()
                        fatigue = gameState.playerState.fatigue.toLong(),   // CORRECTED with .toLong()
                        strength = gameState.playerState.strength.toLong(),      // CORRECTED with .toLong()
                        agility = gameState.playerState.agility.toLong(),       // CORRECTED with .toLong()
                        intelligence = gameState.playerState.intelligence.toLong(),// CORRECTED with .toLong()
                        vitality = gameState.playerState.vitality.toLong(),      // CORRECTED with .toLong()
                        spirit = gameState.playerState.spirit.toLong(),     // CORRECTED with .toLong()
                        luck = gameState.playerState.luck.toLong()          // CORRECTED with .toLong()
                    )
                )
            }

            PersistentDataManager.updateStoryData { storyData ->
                storyData.copy(
                    currentPage = gameState.currentPage,
                    totalPlayTime = storyData.totalPlayTime + calculateSessionTime()
                )
            }

            Log.d("AutoSaveManager", "Interval auto-save completed")

        } catch (e: Exception) {
            Log.e("AutoSaveManager", "Error during interval auto-save: ${e.message}")
        }
    }

    private fun convertInventoryToPersistent(collectedItems: CollectedItemsZ5): List<InventoryItemPersistent> {
        // Simple conversion using basic properties
        // This likely needs a more robust implementation based on actual ItemZ1 structure
        return listOf(
            InventoryItemPersistent(
                id = "dal_collected", // Example ID
                name = "Dal", // Example Name
                description = "Collected wood branches",
                rarity = "COMMON",
                type = "MATERIAL",
                slot = "Material",
                weight = 0.1f,
                durability = 100,
                stats = emptyMap()
            )
        ).take(collectedItems.totalItems()) // Ensure we don't create more items than exist
    }

    private fun calculatePlayerLevel(totalStats: Long): Int {
        // Simple level calculation based on total stats
        return when {
            totalStats < 100 -> 1
            totalStats < 200 -> 2
            totalStats < 400 -> 3
            totalStats < 800 -> 4
            totalStats < 1600 -> 5
            else -> ((totalStats / 300) + 1).toInt().coerceAtMost(100)
        }
    }

    private fun calculateSessionTime(): Long {
        // Calculate time since last save (simplified)
        val currentTime = System.currentTimeMillis()
        val sessionTime = currentTime - lastSaveTime
        // lastSaveTime = currentTime // Update lastSaveTime here if it represents the start of the session being calculated
        return sessionTime
    }

    fun forceSave() {
        if (!isInitialized) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val gameState = gameStateManager.gameState.value
                performDayCompletionSave(gameState)
                Log.d("AutoSaveManager", "Force save completed")
            } catch (e: Exception) {
                Log.e("AutoSaveManager", "Error during force save: ${e.message}")
            }
        }
    }

    fun updateAutoSaveInterval(intervalSeconds: Int) {
        Log.d("AutoSaveManager", "Auto-save interval updated to $intervalSeconds seconds")
        // The loop will automatically pick up the new interval from settings
    }

    fun pauseAutoSave() {
        autoSaveJob?.cancel()
        Log.d("AutoSaveManager", "Auto-save paused")
    }

    fun resumeAutoSave(scope: CoroutineScope) {
        if (isInitialized) {
            startAutoSaveLoop(scope)
            Log.d("AutoSaveManager", "Auto-save resumed")
        }
    }

    fun cleanup() {
        autoSaveJob?.cancel()
        isInitialized = false
        Log.d("AutoSaveManager", "AutoSaveManager cleaned up")
    }

    // Save trigger points for specific game events
    fun triggerCharacterMeetSave(characterName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("AutoSaveManager", "Character meet auto-save triggered for: $characterName")
            val gameState = gameStateManager.gameState.value
            performIntervalSave(gameState)
        }
    }

    fun triggerQuestUpdateSave(questTitle: String) {
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("AutoSaveManager", "Quest update auto-save triggered for: $questTitle")
            val gameState = gameStateManager.gameState.value
            performIntervalSave(gameState)
        }
    }

    fun triggerDeathSave(
        deathCause: String,
        wasUmbrosOfferAccepted: Boolean = false,
        screenshotPath: String? = null
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("AutoSaveManager", "Death auto-save triggered: $deathCause")
                val gameState = gameStateManager.gameState.value

                // Record death in archive with additional info
                PersistentDataManager.recordDeath(
                    deathCause = deathCause,
                    wasUmbrosOfferAccepted = wasUmbrosOfferAccepted,
                    screenshotPath = screenshotPath
                )

                // Force save all data
                performDayCompletionSave(gameState)

                Log.d("AutoSaveManager", "Death save completed")
            } catch (e: Exception) {
                Log.e("AutoSaveManager", "Error during death save: ${e.message}")
            }
        }
    }

    fun getSaveStatus(): SaveStatus {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastSave = currentTime - lastSaveTime
        val autoSaveInterval = PersistentDataManager.gameData.value.settingsData.gameSettings.autoSaveInterval * 1000

        return SaveStatus(
            lastSaveTime = lastSaveTime,
            timeSinceLastSave = timeSinceLastSave,
            nextSaveIn = (autoSaveInterval - timeSinceLastSave).coerceAtLeast(0),
            autoSaveEnabled = isInitialized && autoSaveJob?.isActive == true // More accurate autoSaveEnabled status
        )
    }

    data class SaveStatus(
        val lastSaveTime: Long,
        val timeSinceLastSave: Long,
        val nextSaveIn: Long,
        val autoSaveEnabled: Boolean
    )
}