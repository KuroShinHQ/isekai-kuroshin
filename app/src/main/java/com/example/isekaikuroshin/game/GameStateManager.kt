package com.example.isekaikuroshin.game

import android.util.Log
import com.example.isekaikuroshin.data.GameStateZ7
import com.example.isekaikuroshin.data.PersistentDataManager
import com.example.isekaikuroshin.data.database.DynamicNPCDao
import com.example.isekaikuroshin.data.database.DynamicNPCEntity
import com.example.isekaikuroshin.data.npc.NPCMood
import com.example.isekaikuroshin.data.npc.NPCTemplatePool
import com.example.isekaikuroshin.engine.mind.LanguageProgressTracker
import com.example.isekaikuroshin.utils.GameLogger
import com.example.isekaikuroshin.utils.SystemOverlayNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TODO-HUB-14: Game State Manager
 *
 * Oyun durumunu yönetir ve oyuncu stat'larını günceller
 */
@Singleton
class GameStateManager @Inject constructor(
    private val persistentDataManager: PersistentDataManager,
    private val systemOverlayNotification: SystemOverlayNotification,
    private val dynamicNPCDao: DynamicNPCDao // G138: For NPC auto-spawn system
) {
    companion object {
        private const val TAG = "GameStateManager"
        private const val GAME_STATE_KEY = "current_game_state"
        // TODO-G96: Database save debouncing configuration
        private const val SAVE_DEBOUNCE_DELAY_MS = 500L
        // G138: NPC spawn fallback threshold
        private const val NPC_SPAWN_FALLBACK_THRESHOLD = 5
    }

    private val _gameState = MutableStateFlow<GameStateZ7?>(null)
    val gameState: StateFlow<GameStateZ7?> = _gameState.asStateFlow()

    // STRATEJI #3: Memory synthesis loading state
    private val _isSynthesizingMemory = MutableStateFlow(false)
    val isSynthesizingMemory: StateFlow<Boolean> = _isSynthesizingMemory.asStateFlow()

    // STRATEJI #3: Dynamic status message for synthesis
    private val _memorySynthesisStatus = MutableStateFlow("")
    val memorySynthesisStatus: StateFlow<String> = _memorySynthesisStatus.asStateFlow()

    // TODO-G96: Debouncing implementation for database saves
    private val saveScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var saveJob: Job? = null
    private var saveCount = 0 // For logging purposes

    // G138: NPC spawn fallback tracking
    private var entriesSinceLastNPCSpawn = 0
    private val managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * TODO-HUB-14: Oyun durumunu yükle
     */
    suspend fun loadGameState(): GameStateZ7? {
        return withContext(Dispatchers.IO) {
            try {
                val loadedState = persistentDataManager.loadData(GAME_STATE_KEY, GameStateZ7.serializer())
                _gameState.value = loadedState ?: GameStateZ7()
                GameLogger.logSystem("TODO-HUB-14: Game state loaded")
                _gameState.value
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load game state", e)
                GameLogger.logError(TAG, "TODO-HUB-14: Game state yüklenemedi", e)

                // Initialize with default state
                val defaultState = GameStateZ7()
                _gameState.value = defaultState
                defaultState
            }
        }
    }

    /**
     * TODO-HUB-14: Oyun durumunu kaydet (debounced)
     * TODO-G96: Bu fonksiyon artık debouncing kullanıyor
     * Hızlı art arda çağrılar tek bir write'a optimize edilir
     */
    suspend fun saveGameState(state: GameStateZ7) {
        // Update in-memory state immediately
        _gameState.value = state

        // Schedule debounced database write
        scheduleDatabaseSave(state)
    }

    /**
     * TODO-G96: Schedule a debounced database save
     * Cancels previous pending save and schedules a new one after delay
     *
     * @param state Game state to save
     */
    private fun scheduleDatabaseSave(state: GameStateZ7) {
        // Cancel previous save job
        saveJob?.cancel()

        // Schedule new save after debounce delay
        saveJob = saveScope.launch {
            delay(SAVE_DEBOUNCE_DELAY_MS)
            actuallyWriteToDatabase(state)
        }

        saveCount++
        if (saveCount % 10 == 0) {
            GameLogger.logSystem("TODO-G96: 💾 Save scheduled (debounced, count: $saveCount)")
        }
    }

    /**
     * TODO-G96: Actually write to database
     * This is the only function that performs actual I/O
     */
    private suspend fun actuallyWriteToDatabase(state: GameStateZ7) {
        withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                persistentDataManager.saveData(GAME_STATE_KEY, state, GameStateZ7.serializer())
                val duration = System.currentTimeMillis() - startTime

                GameLogger.logSystem("TODO-G96: 💾 Database saved (debounced, ${duration}ms)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save game state", e)
                GameLogger.logError(TAG, "TODO-G96: Database write failed", e)
            }
        }
    }

    /**
     * TODO-G96: Save game state immediately (bypass debouncing)
     * Use this for critical events: app pause, combat victory/defeat, etc.
     *
     * @param state Game state to save (if null, uses current state)
     */
    suspend fun saveGameStateImmediately(state: GameStateZ7? = null) {
        // Cancel any pending debounced save
        saveJob?.cancel()

        val stateToSave = state ?: _gameState.value ?: return

        withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                persistentDataManager.saveData(GAME_STATE_KEY, stateToSave, GameStateZ7.serializer())
                val duration = System.currentTimeMillis() - startTime

                GameLogger.logSystem("TODO-G96: ⚡ Database saved IMMEDIATELY (${duration}ms)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save game state immediately", e)
                GameLogger.logError(TAG, "TODO-G96: Immediate save failed", e)
            }
        }
    }

    /**
     * TODO-HUB-14: Dil öğrenim bonusunu karakter stat'ına uygula
     *
     * Kullanıcının dil öğrenimindeki başarısını zeka stat'ına yansıtır
     * ve bir sistem overlay bildirimi gösterir
     *
     * @param progress Kullanıcının dil öğrenimi ilerleme verisi
     */
    fun applyLanguageLearningBonus(progress: LanguageProgressTracker.LanguageProgress) {
        try {
            GameLogger.logSystem("TODO-HUB-14: Dil öğrenim bonusu uygulanıyor...")
            GameLogger.logSystem("TODO-HUB-14: Seviye: ${progress.cefrLevel}, İlerleme: ${progress.progressPercent.toInt()}%")

            val currentState = _gameState.value ?: GameStateZ7()
            val currentPlayerState = currentState.playerState

            // Önceki zeka değerini al
            val previousIntelligence = currentPlayerState.intelligence

            // TODO-HUB-14: Stat bonus hesaplama
            // Her CEFR seviyesi için +2 zeka bonusu
            val statBonus = progress.calculateStatBonus()

            // Yeni zeka değerini hesapla
            // Base intelligence (10) + stat bonus
            val newIntelligence = 10f + statBonus

            // Eğer değişiklik varsa güncelle
            if (newIntelligence > previousIntelligence) {
                val increase = newIntelligence - previousIntelligence

                // Player state'i güncelle
                val updatedPlayerState = currentPlayerState.copy(
                    intelligence = newIntelligence
                )

                // Game state'i güncelle
                val updatedGameState = currentState.copy(
                    playerState = updatedPlayerState
                )

                // State'i güncelle (asenkron kayıt için)
                _gameState.value = updatedGameState

                // Kaydet (fire and forget)
                kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                    saveGameState(updatedGameState)
                }

                GameLogger.logSystem("TODO-HUB-14: ✅ Zeka stat'ı güncellendi: $previousIntelligence → $newIntelligence (+${increase.toInt()})")

                // TODO-HUB-14: Kullanıcıya bildirim göster
                systemOverlayNotification.show(
                    title = "🧠 Zeka Arttı!",
                    message = "Dil öğrenimi sayesinde Zeka stat'ınız +${increase.toInt()} arttı!",
                    type = SystemOverlayNotification.NotificationType.STAT_INCREASE,
                    duration = 4000L
                )

                GameLogger.logSystem("TODO-HUB-14: ✅ Sistem bildirimi gösterildi")
            } else {
                GameLogger.logSystem("TODO-HUB-14: Zeka stat'ı zaten maksimum seviyede")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Apply language learning bonus failed", e)
            GameLogger.logError(TAG, "TODO-HUB-14: Dil öğrenim bonusu uygulanamadı", e)
        }
    }

    /**
     * TODO-HUB-14: Herhangi bir player stat'ını güncelle
     */
    suspend fun updatePlayerStat(statName: String, newValue: Float) {
        withContext(Dispatchers.IO) {
            try {
                val currentState = _gameState.value ?: GameStateZ7()
                val playerState = currentState.playerState

                val updatedPlayerState = when (statName.lowercase()) {
                    "strength" -> playerState.copy(strength = newValue)
                    "vitality" -> playerState.copy(vitality = newValue)
                    "agility" -> playerState.copy(agility = newValue)
                    "intelligence" -> playerState.copy(intelligence = newValue)
                    else -> playerState
                }

                val updatedGameState = currentState.copy(playerState = updatedPlayerState)
                saveGameState(updatedGameState)

                GameLogger.logSystem("TODO-HUB-14: $statName stat güncellendi: $newValue")
            } catch (e: Exception) {
                Log.e(TAG, "Update player stat failed", e)
                GameLogger.logError(TAG, "TODO-HUB-14: Stat güncellenemedi", e)
            }
        }
    }

    /**
     * TODO-HUB-14: Mevcut player state'i getir
     */
    fun getCurrentPlayerState() = _gameState.value?.playerState

    /**
     * STRATEJI #3: Memory synthesis loading state kontrolü
     * @param isActive Synthesis aktif mi
     * @param statusMessage Kullanıcıya gösterilecek durum mesajı (opsiyonel)
     */
    fun setMemorySynthesisState(isActive: Boolean, statusMessage: String = "") {
        _isSynthesizingMemory.value = isActive
        _memorySynthesisStatus.value = statusMessage

        if (isActive) {
            GameLogger.logSystem("🔄 Memory synthesis started - UI locked")
            if (statusMessage.isNotEmpty()) {
                GameLogger.logSystem("📊 Status: $statusMessage")
            }
        } else {
            GameLogger.logSystem("✅ Memory synthesis completed - UI unlocked")
            _memorySynthesisStatus.value = "" // Temizle
        }
    }

    // ========================================
    // G61b: GM RESPONSE APPLICATION
    // ========================================

    /**
     * G61b: Applies GM response to game state
     * Processes all GM commands and updates persistent data accordingly
     *
     * @param response GMResponse object from AI
     */
    fun applyGMResponse(response: com.example.isekaikuroshin.data.GMResponse) {
        try {
            GameLogger.logSystem("G61b: Applying GM response...")

            // 1. Stats değişiklikleri
            if (response.statsChanged.isNotEmpty()) {
                persistentDataManager.updatePlayerStats(response.statsChanged)
                GameLogger.logSystem("✅ Stats applied: ${response.statsChanged}")
            }

            // G142: NPC name validation
            response.npcStateChange?.npcId?.let { npcId ->
                val validatedName = com.example.isekaikuroshin.utils.NameValidator.validateOrSanitize(npcId)
                if (validatedName != npcId) {
                    GameLogger.logWarning("GameStateManager", "G142: Invalid NPC name sanitized: \"$npcId\" → \"$validatedName\"")
                }
            }

            // 2. Skill kazanımları
            response.npcStateChange?.skillsLearned?.forEach { skillId ->
                persistentDataManager.addSkill(skillId)
                GameLogger.logSystem("🎓 Skill learned: $skillId")
            }

            // 3. Title kazanımları
            response.npcStateChange?.titlesGained?.forEach { titleId ->
                persistentDataManager.addTitle(titleId)
                GameLogger.logSystem("🏆 Title gained: $titleId")
            }

            // 4. Badge kazanımları
            response.npcStateChange?.badgesEarned?.forEach { badgeId ->
                persistentDataManager.addBadge(badgeId)
                GameLogger.logSystem("🎖️ Badge earned: $badgeId")
            }

            // G146: Item consumption (crafting input items)
            if (response.itemsConsumed.isNotEmpty()) {
                _gameState.value?.let { currentState ->
                    response.itemsConsumed.forEach { itemName ->
                        // Find and remove item from inventory by display name
                        val itemToRemove = currentState.inventory.find { item ->
                            item.name.equals(itemName, ignoreCase = true)
                        }

                        if (itemToRemove != null) {
                            // Remove item from inventory
                            val updatedInventory = currentState.inventory.filter { it.id != itemToRemove.id }
                            _gameState.value = currentState.copy(inventory = updatedInventory)

                            GameLogger.logSystem("✂️ G146: Item consumed for crafting: $itemName")
                            GameLogger.logCritical("Crafting: Consumed $itemName")
                        } else {
                            GameLogger.logWarning("GameStateManager", "G146: Item to consume not found: $itemName")
                        }
                    }
                }
            }

            // 5. Item kazanımları (G135.2: Implemented)
            if (response.itemsGained.isNotEmpty()) {
                response.itemsGained.forEach { gmItem ->
                    GameLogger.logSystem("📦 G135.2: Item gained: ${gmItem.name} (${gmItem.rarity})")

                    // G135.2: Clean AI-generated name
                    val cleanedName = (gmItem.name ?: "Unknown Item").let { rawName ->
                        val cleaned = rawName
                            .removePrefix("Item_item_")
                            .removePrefix("item_")
                            .removePrefix("ITEM_")
                            .trim()

                        // If still looks like raw key (has underscores), title case it
                        if (cleaned.contains("_")) {
                            cleaned.split("_").joinToString(" ") { word ->
                                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                            }
                        } else {
                            cleaned
                        }
                    }

                    // Convert GMItemGained to EquippedItemZ6 (GMItemGained only has basic fields)
                    val item = com.example.isekaikuroshin.data.EquippedItemZ6(
                        id = System.currentTimeMillis().toString() + "_" + (Math.random() * 1000).toInt(),
                        name = cleanedName,
                        description = gmItem.description ?: "No description",
                        weight = 1.0f, // Default weight (GMItemGained doesn't have weight field)
                        rarity = gmItem.rarity ?: "COMMON",
                        statBonuses = emptyMap(), // GMItemGained doesn't have statBonuses yet
                        consumableEffects = emptyMap() // GMItemGained doesn't have consumableEffects yet
                    )

                    // Add to inventory using existing function
                    val added = addItemToInventory(item)
                    if (added) {
                        GameLogger.logSystem("✅ G135.2: Item added to inventory: ${item.name}")
                    } else {
                        GameLogger.logSystem("❌ G135.2: Failed to add item (inventory full?)")
                    }
                }
            }

            // G79: Yeni quest kazanımları
            if (response.questsGained.isNotEmpty()) {
                response.questsGained.forEach { gmQuest ->
                    val questEntry = com.example.isekaikuroshin.data.QuestEntry(
                        id = gmQuest.questId ?: "quest_${System.currentTimeMillis()}",
                        title = gmQuest.title ?: "Unknown Quest",
                        description = gmQuest.description ?: "",
                        giver = gmQuest.giver ?: "Unknown",
                        startDay = persistentDataManager.gameData.value.storyData.currentDay,
                        dueDay = gmQuest.dueDay,
                        rewards = gmQuest.rewards,
                        isDeveloperQuest = false // GM-generated quests are side quests
                    )
                    persistentDataManager.addQuestToActive(questEntry)
                    GameLogger.logSystem("✨ G79: New quest gained: ${questEntry.title}")
                }
            }

            // 6. Quest güncellemeleri (progress update)
            if (response.questsUpdated.isNotEmpty()) {
                response.questsUpdated.forEach { questId ->
                    GameLogger.logSystem("📜 Quest updated: $questId")
                    // TODO-G79: Implement quest progress update logic
                }
            }

            // 7. Location unlock (TODO-G114.2: Location discovery system)
            if (response.locationsUnlocked.isNotEmpty()) {
                response.locationsUnlocked.forEach { locationId ->
                    unlockLocation(locationId)
                    GameLogger.logCritical("Location unlocked: $locationId") // G141: Critical event highlight
                }
            }

            // 8. Weather değişikliği (TODO: WeatherManager integration needed)
            response.weatherChange?.let { weather ->
                GameLogger.logSystem("🌤️ Weather changed: $weather")
                // TODO-G65: WeatherManager.setWeather(weather) - Requires weather system
            }

            // 9. Time progression (G147: Implemented)
            response.timeShift?.let { timeString ->
                try {
                    val newTime = com.example.isekaikuroshin.data.TimeOfDay.valueOf(timeString)
                    _gameState.value?.let { currentState ->
                        _gameState.value = currentState.copy(currentTimeOfDay = newTime)
                        persistentDataManager.saveGameData()
                        GameLogger.logSystem("🕐 G147: Time shifted to: $timeString")
                    }
                } catch (e: IllegalArgumentException) {
                    GameLogger.logError("GameStateManager", "Invalid timeShift value: $timeString")
                }
            }

            // 10. Date change (G147: New)
            response.dateChange?.let { days ->
                if (days > 0) {
                    _gameState.value?.let { currentState ->
                        val newDay = currentState.currentDay + days
                        _gameState.value = currentState.copy(currentDay = newDay)
                        persistentDataManager.saveGameData()
                        GameLogger.logSystem("📅 G147: Date changed: +$days days (Day ${newDay})")
                    }
                }
            }

            // G77: Nemesis evolution processing
            response.nemesisEvolution?.let { nemesis ->
                if (nemesis.npcId != null && nemesis.newNemesisLevel != null) {
                    val nemesisEntry = com.example.isekaikuroshin.data.NemesisEntry(
                        npcId = nemesis.npcId,
                        nemesisLevel = nemesis.newNemesisLevel,
                        vendettaReason = nemesis.vendettaReason ?: "Unknown vendetta",
                        adaptationTraits = nemesis.adaptationTraits,
                        evolutionDay = persistentDataManager.gameData.value.storyData.currentDay,
                        evolutionDescription = nemesis.evolutionDescription ?: "A powerful enemy has emerged."
                    )
                    persistentDataManager.addNemesis(nemesisEntry)
                    GameLogger.logCritical("Nemesis activated! ${nemesis.npcId} (Level ${nemesis.newNemesisLevel})") // G141: Critical event
                    GameLogger.logSystem("💀 Vendetta: ${nemesis.vendettaReason}")
                } else {
                    GameLogger.logSystem("⚠️ G77: Incomplete nemesis data - missing npcId or level")
                }
            }

            // G82: Status Effects (Buff/Debuff) processing
            if (response.statusEffectsApplied.isNotEmpty()) {
                response.statusEffectsApplied.forEach { effect ->
                    persistentDataManager.applyStatusEffect(effect)
                    val icon = if (effect.type == com.example.isekaikuroshin.data.StatusEffectType.BUFF) "✨" else "💀"
                    GameLogger.logSystem("$icon G82: Status effect applied - ${effect.name} (${effect.type})")
                    GameLogger.logSystem("   Magnitude: ${effect.magnitude}, Duration: ${effect.durationTurns} turns")
                }
            }

            // G136: NPC State Change → Character Catalog integration
            response.npcStateChange?.let { npcState ->
                npcState.npcId?.let { npcId ->
                    // TODO-G136: Add NPC to Character Catalog (NPCEntry class + persistentDataManager.addNPCToCharacterCatalog needed)
                    // For now, just log the NPC interaction
                    GameLogger.logSystem("👤 G136: NPC interacted: $npcId (loyalty: ${npcState.loyaltyChange ?: 0})")
                    GameLogger.logCritical("Character met: $npcId") // G141: Critical event
                }
            }

            // G136: Morality Delta → Karma System integration
            response.moralityDelta?.let { delta ->
                if (delta != 0.0f) {
                    _gameState.value?.let { currentState ->
                        val currentMorality = currentState.playerState.moralityScore
                        val newMoralityScore = (currentMorality + delta).coerceIn(-1.0f, 1.0f) // Clamp to [-1.0, 1.0]
                        val updatedPlayerState = currentState.playerState.copy(moralityScore = newMoralityScore)
                        _gameState.value = currentState.copy(playerState = updatedPlayerState)
                        persistentDataManager.saveGameData()

                        val icon = when {
                            delta > 0 -> "😇"
                            delta < 0 -> "😈"
                            else -> "😐"
                        }
                        GameLogger.logSystem("$icon G136: Morality changed by $delta → $newMoralityScore")
                        GameLogger.logCritical("Morality: ${if (delta > 0) "Good deed" else "Evil act"} ($delta)") // G141: Critical event
                    }
                }
            }

            GameLogger.logSystem("G61b: ✅ GM response applied successfully")

            // G138: NPC spawn fallback tracking
            checkNPCSpawnFallback(response)

            // G63: Death check after stats update
            checkPlayerDeath()

        } catch (e: Exception) {
            Log.e(TAG, "G61b: Failed to apply GM response", e)
            GameLogger.logError(TAG, "G61b: GM response uygulanamadı", e)
        }
    }

    // ========================================
    // G135: ITEM MECHANICS SYSTEM
    // ========================================

    /**
     * G135 Phase 1: Add item to inventory
     * @param item Item to add
     * @return true if successful, false if weight capacity exceeded
     */
    fun addItemToInventory(item: com.example.isekaikuroshin.data.EquippedItemZ6): Boolean {
        val currentState = _gameState.value ?: return false

        // Check weight capacity (GameState extension functions in GameState.kt)
        val currentWeight = currentState.inventory.sumOf { it.weight.toDouble() }.toFloat()
        val maxCapacity = 50f // TODO-G135: Calculate based on player strength

        if (currentWeight + item.weight > maxCapacity) {
            GameLogger.logSystem("❌ G135: Cannot add item - weight capacity exceeded (${currentWeight + item.weight}/${maxCapacity} kg)")
            systemOverlayNotification.show(
                title = "⚠️ Envanter Dolu",
                message = "Taşıma kapasiten aşıldı!",
                type = SystemOverlayNotification.NotificationType.INFO,
                duration = 3000L
            )
            return false
        }

        val updatedInventory = currentState.inventory + item
        val updatedState = currentState.copy(inventory = updatedInventory)

        saveScope.launch {
            saveGameState(updatedState)
        }

        GameLogger.logSystem("✅ G135: Item added to inventory: ${item.name} (${item.weight} kg)")
        return true
    }

    /**
     * G135 Phase 2: Remove item from inventory
     * @param itemId Item ID to remove
     * @param amount Amount to remove (default: 1)
     * @return true if successful
     */
    fun removeItemFromInventory(itemId: String, amount: Int = 1): Boolean {
        val currentState = _gameState.value ?: return false

        val itemToRemove = currentState.inventory.firstOrNull { it.id == itemId }
        if (itemToRemove == null) {
            GameLogger.logSystem("❌ G135: Item not found in inventory: $itemId")
            return false
        }

        val updatedInventory = currentState.inventory.toMutableList()
        updatedInventory.remove(itemToRemove)

        val updatedState = currentState.copy(inventory = updatedInventory)

        saveScope.launch {
            saveGameState(updatedState)
        }

        GameLogger.logSystem("✅ G135: Item removed from inventory: ${itemToRemove.name}")
        return true
    }

    /**
     * G135 Phase 2: Consume item (food, potion, etc.)
     * KURAL 11: Metadata-based logic - uses item.consumableEffects field
     * @param itemId Item ID to consume
     * @return true if successful
     */
    fun consumeItem(itemId: String): Boolean {
        val currentState = _gameState.value ?: return false

        val item = currentState.inventory.firstOrNull { it.id == itemId }
        if (item == null) {
            GameLogger.logSystem("❌ G135: Cannot consume - item not found: $itemId")
            return false
        }

        // Check if item has consumable effects
        if (item.consumableEffects.isEmpty()) {
            GameLogger.logSystem("⚠️ G135: Item has no consumable effects: ${item.name}")
            // Still consume it (remove from inventory) but no stat changes
            removeItemFromInventory(itemId)
            systemOverlayNotification.show(
                title = "🍽️ ${item.name}",
                message = "Tüketildi (etki yok)",
                type = SystemOverlayNotification.NotificationType.INFO,
                duration = 2000L
            )
            return true
        }

        // Remove from inventory first
        if (!removeItemFromInventory(itemId)) {
            return false
        }

        // Apply consumable effects from metadata (KURAL 11)
        val playerState = currentState.playerState
        var updatedPlayerState = playerState
        val effectMessages = mutableListOf<String>()

        item.consumableEffects.forEach { (effectKey, value) ->
            when (effectKey) {
                "hungerRestore" -> {
                    updatedPlayerState = updatedPlayerState.copy(
                        hunger = (updatedPlayerState.hunger + value).coerceAtMost(playerState.maxHunger)
                    )
                    effectMessages.add("🍎 Açlık +$value")
                }
                "healthRestore" -> {
                    updatedPlayerState = updatedPlayerState.copy(
                        currentHealth = (updatedPlayerState.currentHealth + value).coerceAtMost(playerState.maxHealth)
                    )
                    effectMessages.add("❤️ Can +$value")
                }
                "manaRestore" -> {
                    updatedPlayerState = updatedPlayerState.copy(
                        currentMana = (updatedPlayerState.currentMana + value).coerceAtMost(playerState.maxMana)
                    )
                    effectMessages.add("✨ Mana +$value")
                }
                "staminaRestore" -> {
                    updatedPlayerState = updatedPlayerState.copy(
                        stamina = (updatedPlayerState.stamina + value).coerceAtMost(playerState.maxStamina)
                    )
                    effectMessages.add("💪 Stamina +$value")
                }
                "fatigueReduce" -> {
                    updatedPlayerState = updatedPlayerState.copy(
                        fatigue = (updatedPlayerState.fatigue - value).coerceAtLeast(0)
                    )
                    effectMessages.add("😌 Yorgunluk -$value")
                }
                else -> {
                    GameLogger.logSystem("⚠️ G135: Unknown consumable effect key: $effectKey")
                }
            }
        }

        val finalState = currentState.copy(playerState = updatedPlayerState)

        saveScope.launch {
            saveGameState(finalState)
        }

        val effectMessage = effectMessages.joinToString(", ")
        GameLogger.logSystem("✅ G135: Item consumed: ${item.name} - $effectMessage")
        systemOverlayNotification.show(
            title = "🍽️ ${item.name}",
            message = effectMessage,
            type = SystemOverlayNotification.NotificationType.ITEM_ACQUIRED,
            duration = 2000L
        )

        return true
    }

    /**
     * G135 Phase 2: Equip item to slot
     * @param itemId Item ID to equip
     * @param slotName Slot name (e.g., "Ana El", "Göğüs")
     * @return true if successful
     */
    fun equipItemToSlot(itemId: String, slotName: String): Boolean {
        val currentState = _gameState.value ?: return false

        val item = currentState.inventory.firstOrNull { it.id == itemId }
        if (item == null) {
            GameLogger.logSystem("❌ G135: Cannot equip - item not found: $itemId")
            return false
        }

        // Check if slot already has an item
        val previousItem = currentState.equippedItems[slotName]

        // Remove item from inventory
        val updatedInventory = currentState.inventory.toMutableList()
        updatedInventory.remove(item)

        // Add previous item back to inventory if exists
        if (previousItem != null) {
            updatedInventory.add(previousItem)
        }

        // Equip new item
        val updatedEquippedItems = currentState.equippedItems.toMutableMap()
        updatedEquippedItems[slotName] = item

        // G135 Phase 3: Apply stat bonuses
        var updatedPlayerState = currentState.playerState

        // Remove previous item bonuses
        if (previousItem != null) {
            updatedPlayerState = applyStatBonuses(updatedPlayerState, previousItem.statBonuses, remove = true)
        }

        // Apply new item bonuses
        updatedPlayerState = applyStatBonuses(updatedPlayerState, item.statBonuses, remove = false)

        val finalState = currentState.copy(
            inventory = updatedInventory,
            equippedItems = updatedEquippedItems,
            playerState = updatedPlayerState
        )

        saveScope.launch {
            saveGameState(finalState)
        }

        val bonusText = if (item.statBonuses.isNotEmpty()) {
            item.statBonuses.entries.joinToString(", ") { (stat, value) ->
                "$stat: +${value.toInt()}"
            }
        } else {
            "Bonus yok"
        }

        GameLogger.logSystem("✅ G135: Item equipped: ${item.name} → $slotName ($bonusText)")
        systemOverlayNotification.show(
            title = "⚔️ ${item.name} Kuşanıldı",
            message = bonusText,
            type = SystemOverlayNotification.NotificationType.STAT_INCREASE,
            duration = 2500L
        )

        return true
    }

    /**
     * G135 Phase 2: Unequip item from slot
     * @param slotName Slot name to unequip
     * @return true if successful
     */
    fun unequipItemFromSlot(slotName: String): Boolean {
        val currentState = _gameState.value ?: return false

        val item = currentState.equippedItems[slotName]
        if (item == null) {
            GameLogger.logSystem("⚠️ G135: Slot already empty: $slotName")
            return false
        }

        // Check weight capacity
        val currentWeight = currentState.inventory.sumOf { it.weight.toDouble() }.toFloat()
        val maxCapacity = 50f // TODO-G135: Calculate based on player strength

        if (currentWeight + item.weight > maxCapacity) {
            GameLogger.logSystem("❌ G135: Cannot unequip - inventory full")
            systemOverlayNotification.show(
                title = "⚠️ Envanter Dolu",
                message = "Önce başka bir eşyayı çıkar!",
                type = SystemOverlayNotification.NotificationType.INFO,
                duration = 3000L
            )
            return false
        }

        // Remove from equipped
        val updatedEquippedItems = currentState.equippedItems.toMutableMap()
        updatedEquippedItems.remove(slotName)

        // Add to inventory
        val updatedInventory = currentState.inventory + item

        // G135 Phase 3: Remove stat bonuses
        val updatedPlayerState = applyStatBonuses(currentState.playerState, item.statBonuses, remove = true)

        val finalState = currentState.copy(
            inventory = updatedInventory,
            equippedItems = updatedEquippedItems,
            playerState = updatedPlayerState
        )

        saveScope.launch {
            saveGameState(finalState)
        }

        GameLogger.logSystem("✅ G135: Item unequipped: ${item.name} ← $slotName")
        return true
    }

    /**
     * G135 Phase 3: Apply or remove stat bonuses from equipment
     * @param playerState Current player state
     * @param statBonuses Stat bonuses map
     * @param remove If true, remove bonuses; if false, add bonuses
     * @return Updated player state
     */
    private fun applyStatBonuses(
        playerState: com.example.isekaikuroshin.data.PlayerState,
        statBonuses: Map<String, Float>,
        remove: Boolean
    ): com.example.isekaikuroshin.data.PlayerState {
        if (statBonuses.isEmpty()) return playerState

        var updated = playerState
        val multiplier = if (remove) -1 else 1

        statBonuses.forEach { (stat, value) ->
            val bonus = value * multiplier

            updated = when (stat.uppercase()) {
                "STR", "STR_FLAT", "STRENGTH" -> updated.copy(strength = updated.strength + bonus)
                "AGI", "AGI_FLAT", "AGILITY" -> updated.copy(agility = updated.agility + bonus)
                "INT", "INT_FLAT", "INTELLIGENCE" -> updated.copy(intelligence = updated.intelligence + bonus)
                "VIT", "VIT_FLAT", "VITALITY" -> updated.copy(vitality = updated.vitality + bonus)
                "SPIRIT" -> updated.copy(spirit = (updated.spirit + bonus.toInt()).coerceAtLeast(0))
                "LUCK" -> updated.copy(luck = (updated.luck + bonus.toInt()).coerceAtLeast(0))
                "PHYSICAL_ATTACK", "ATTACK" -> updated.copy(physicalAttack = (updated.physicalAttack + bonus.toInt()).coerceAtLeast(0))
                "PHYSICAL_DEFENSE", "DEFENSE" -> updated.copy(defense = (updated.defense + bonus.toInt()).coerceAtLeast(0))
                "MAGIC_ATTACK", "MAGIC_POWER" -> updated.copy(magicPower = (updated.magicPower + bonus.toInt()).coerceAtLeast(0))
                // TODO-G135: PlayerState doesn't have separate magicDefense field (uses defense)
                "MAGIC_DEFENSE" -> {
                    GameLogger.logSystem("⚠️ G135: MAGIC_DEFENSE → using defense field")
                    updated.copy(defense = (updated.defense + bonus.toInt()).coerceAtLeast(0))
                }
                else -> updated // Unknown stat, skip
            }

            val action = if (remove) "removed" else "applied"
            GameLogger.logSystem("   G135: Stat bonus $action - $stat: ${if (remove) "-" else "+"}${value.toInt()}")
        }

        return updated
    }

    /**
     * G63: Death Mechanic Integration
     *
     * Checks if player has died (HP <= 0) after stats update
     * Triggers death flow if necessary
     */
    fun checkPlayerDeath(): Boolean {
        val playerData = persistentDataManager.gameData.value.playerData
        val currentHP = playerData.stats.hp

        if (currentHP <= 0) {
            GameLogger.logSystem("💀 G63: Player death detected! HP: $currentHP")

            // Mark player as dead
            persistentDataManager.updatePlayerData { player ->
                player.copy(isAlive = false)
            }

            // Log death event
            Log.w(TAG, "G63: Player has died - triggering death flow")
            GameLogger.logCritical("Player died! HP reached 0") // G141: Critical event highlight
            GameLogger.logSystem("💀 Current location: ${persistentDataManager.gameData.value.storyData.currentLocation}")

            // TODO-G63: Navigate to postdeath_karma screen
            // This requires navigation integration - for now just log
            GameLogger.logSystem("💀 Death flow should trigger → navigate to 'postdeath_karma'")

            return true
        }

        return false
    }

    /**
     * TODO-G114.2: Unlock a new location for player discovery
     * Creates a basic Location object and adds to knownLocations if not already unlocked
     *
     * @param locationId ID of the location to unlock
     */
    fun unlockLocation(locationId: String) {
        val currentState = _gameState.value ?: return

        // Check if location is already known
        if (currentState.knownLocations.any { it.id == locationId }) {
            GameLogger.logSystem("[G114] Location already known: $locationId")
            return
        }

        // Create basic location object (full data should come from LocationRegistry in future)
        // KURAL 9: Use locationId as key for LanguageManager (description will be localized)
        val newLocation = com.example.isekaikuroshin.data.Location(
            id = locationId,
            name = locationId, // Use ID as localization key (will be resolved by LanguageManager in UI)
            description = "${locationId}_desc", // Use ID_desc as localization key
            type = com.example.isekaikuroshin.data.LocationType.WILDERNESS, // Default type
            dangerLevel = 1, // Default danger level
            requiredLevel = 1,
            isDiscovered = true // Mark as discovered
        )

        // Add to known locations
        _gameState.value = currentState.copy(
            knownLocations = currentState.knownLocations + newLocation
        )

        GameLogger.logSystem("[G114] Location discovered: ${newLocation.name} (ID: $locationId)")

        // TODO-G114.3: Trigger UI notification for location discovery
        // systemOverlayNotification.showNotification("New Location: ${newLocation.name}")

        // G138: Check if location needs NPCs (auto-spawn if empty)
        managerScope.launch {
            checkAndSpawnNPCsForLocation(locationId, newLocation.type)
        }

        // Save updated state (this is game/GameStateManager, not data/GameStateManager)
        persistentDataManager.saveGameData()
    }

    // ========================================
    // G138: NPC AUTO-SPAWN SYSTEM
    // ========================================

    /**
     * G138: Check if location has NPCs, spawn defaults if empty
     *
     * This function:
     * 1. Queries NPCs at the given location
     * 2. If empty, spawns 1-2 default NPCs from template pool
     * 3. Logs the spawn action
     *
     * KURAL 15: ROOT CAUSE - AI doesn't always create NPCs, we need fallback system
     */
    private suspend fun checkAndSpawnNPCsForLocation(
        locationId: String,
        locationType: com.example.isekaikuroshin.data.LocationType
    ) {
        try {
            // Check how many NPCs are at this location
            val npcsAtLocation = dynamicNPCDao.getNpcsAtLocation(locationId)

            GameLogger.logSystem("[G138] Location: $locationId, NPCs: ${npcsAtLocation.size}")

            // If location has no NPCs, spawn defaults
            if (npcsAtLocation.isEmpty()) {
                GameLogger.logWarning("GameStateManager", "[G138] No NPCs at $locationId - spawning defaults")
                spawnDefaultNPCsForLocation(locationId, locationType)
            } else {
                GameLogger.logSystem("[G138] Location $locationId already has ${npcsAtLocation.size} NPC(s)")
            }
        } catch (e: Exception) {
            GameLogger.logSystem("[G138] Error checking NPCs for location $locationId: ${e.message}")
        }
    }

    /**
     * G138: Spawn 1-2 default NPCs for a location from template pool
     *
     * Phase 2: Auto-Spawn Logic
     * - Gets 1-2 random NPC templates suitable for location type
     * - Creates DynamicNPCEntity from template
     * - Inserts into database
     *
     * KURAL 11: Uses metadata-based logic (location type, not string comparison)
     */
    private suspend fun spawnDefaultNPCsForLocation(
        locationId: String,
        locationType: com.example.isekaikuroshin.data.LocationType
    ) {
        try {
            // Determine spawn count based on location type
            val spawnCount = when (locationType) {
                com.example.isekaikuroshin.data.LocationType.CITY -> 2  // Cities have more NPCs
                com.example.isekaikuroshin.data.LocationType.TOWN -> 2
                com.example.isekaikuroshin.data.LocationType.VILLAGE -> 1
                com.example.isekaikuroshin.data.LocationType.WILDERNESS -> 1
                com.example.isekaikuroshin.data.LocationType.DUNGEON -> 1
                com.example.isekaikuroshin.data.LocationType.RUIN -> 1
                com.example.isekaikuroshin.data.LocationType.CAVE -> 1
                com.example.isekaikuroshin.data.LocationType.SPECIAL -> 1
            }

            // Get random NPC templates for this location
            val templates = NPCTemplatePool.getRandomNPCsForLocation(locationType, spawnCount)

            // Create and insert NPC entities
            templates.forEachIndexed { index, template ->
                val npcId = "${template.baseRegistryId}_${locationId}_${System.currentTimeMillis()}_$index"

                val npcEntity = DynamicNPCEntity(
                    npcId = npcId,
                    baseRegistryId = template.baseRegistryId,
                    level = template.defaultLevel,
                    currentStrength = template.archetype.baseStrength,
                    currentAgility = template.archetype.baseAgility,
                    currentIntelligence = template.archetype.baseIntelligence,
                    currentVitality = template.archetype.baseVitality,
                    personalityTraits = template.personalityTraits,
                    currentMood = NPCMood.fromLoyalty(template.defaultLoyalty).name,
                    loyaltyToPlayer = template.defaultLoyalty,
                    historyWithPlayer = emptyList(),
                    interactionCount = 0,
                    lastSeenDate = System.currentTimeMillis(),
                    lastLocationId = locationId,
                    npcRelationships = emptyMap(),
                    nemesisLevel = 0,
                    vendettaReasons = emptyList(),
                    adaptationTraits = emptyList(),
                    currentLocationId = locationId,
                    isAlive = true,
                    isDead = false,
                    isExiled = false,
                    lastUpdateTime = System.currentTimeMillis(),
                    evolutionPoints = 0,
                    timesSeen = 0,
                    timesDefeated = 0,
                    timesHelped = 0,
                    skills = emptyList(),
                    titles = emptyList(),
                    badges = emptyList(),
                    age = (18..60).random(),
                    gender = listOf("Male", "Female", "Unknown").random()
                )

                // Insert into database
                dynamicNPCDao.insertNpcState(npcEntity)

                GameLogger.logCritical("[G138] Spawned NPC: ${template.archetype.name} at $locationId (ID: $npcId, Loyalty: ${template.defaultLoyalty})")
            }

            GameLogger.logSystem("[G138] Successfully spawned $spawnCount NPC(s) at $locationId")
        } catch (e: Exception) {
            GameLogger.logSystem("[G138] Error spawning NPCs for $locationId: ${e.message}")
        }
    }

    /**
     * G138 Phase 3: NPC Spawn Fallback System
     *
     * Tracks journal entries since last NPC spawn. If AI doesn't create NPCs
     * for 5+ entries, force spawn a default NPC at current location.
     *
     * This ensures players always have NPCs to interact with even if AI forgets.
     *
     * KURAL 15: ROOT CAUSE - AI is unreliable, need fallback system
     */
    private fun checkNPCSpawnFallback(response: com.example.isekaikuroshin.data.GMResponse) {
        // Check if AI spawned an NPC in this response
        val aiSpawnedNPC = response.npcStateChange != null

        if (aiSpawnedNPC) {
            // Reset counter - AI created an NPC
            entriesSinceLastNPCSpawn = 0
            GameLogger.logSystem("[G138] AI spawned NPC, counter reset")
        } else {
            // Increment counter - AI didn't spawn NPC
            entriesSinceLastNPCSpawn++
            GameLogger.logSystem("[G138] No NPC spawn, counter: $entriesSinceLastNPCSpawn")

            // Check if we hit threshold (5 entries without NPC)
            if (entriesSinceLastNPCSpawn >= NPC_SPAWN_FALLBACK_THRESHOLD) {
                GameLogger.logWarning("GameStateManager", "[G138] FALLBACK TRIGGERED! $entriesSinceLastNPCSpawn entries without NPC")

                // Force spawn NPC at current location
                val currentState = _gameState.value
                if (currentState != null) {
                    val locationId = currentState.currentLocationId
                    val locationType = currentState.knownLocations
                        .find { it.id == locationId }?.type
                        ?: com.example.isekaikuroshin.data.LocationType.WILDERNESS

                    GameLogger.logCritical("[G138] Force-spawning NPC at $locationId (AI inactive)")

                    // Spawn NPC asynchronously
                    managerScope.launch {
                        spawnDefaultNPCsForLocation(locationId, locationType)
                    }

                    // Reset counter
                    entriesSinceLastNPCSpawn = 0
                } else {
                    GameLogger.logSystem("[G138] Cannot force spawn - no game state")
                }
            }
        }
    }

    // ========================================
    // TODO-G140: DEATH SCREEN RESET - GAME STATE RESET
    // ========================================

    /**
     * G140: Resets the entire game state to default (for new game after death)
     * Called from DeathStatisticsScreen when user clicks "Yenile" or "Close" button
     *
     * This function:
     * 1. Resets GameState to default (clears inventory, quests, NPCs, karma, etc.)
     * 2. Saves the reset state to disk
     * 3. Logs the reset action
     *
     * KURAL 15: ROOT CAUSE - Butonlar sadece PlayerData temizliyordu, GameState untouched kalıyordu!
     */
    fun resetGameState() {
        GameLogger.logSystem("🔄 [G140] GameState sıfırlanıyor...")

        // Create fresh default GameState
        val defaultState = GameStateZ7()

        // Update in-memory state
        _gameState.value = defaultState

        // Save to disk
        persistentDataManager.saveGameData()

        GameLogger.logSystem("✅ [G140] GameState sıfırlandı - Inventory, Quests, NPCs, Karma temizlendi")
    }
}
