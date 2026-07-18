package com.example.isekaikuroshin.data

import com.example.isekaikuroshin.data.database.GameStateDao
import com.example.isekaikuroshin.data.database.DynamicNPCDao
import com.example.isekaikuroshin.data.database.DynamicNPCEntity
import com.example.isekaikuroshin.data.database.toDynamicNPCState
import com.example.isekaikuroshin.data.database.toEntity
import com.example.isekaikuroshin.data.database.toGameState
import com.example.isekaikuroshin.data.npc.NPCMemoryEvent
import com.example.isekaikuroshin.data.npc.NPCMood
import com.example.isekaikuroshin.data.combat.addPracticeWithXP
import com.example.isekaikuroshin.engine.*
import com.example.isekaikuroshin.utils.GameLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import dagger.Lazy
import kotlin.math.roundToInt
import kotlinx.serialization.Serializable


/**
 * TODO-G62: Combat History Entry
 * Records combat results for statistics and achievements
 */
@Serializable
data class CombatHistoryEntry(
    val timestamp: Long,
    val result: String, // "victory" or "defeat" (localization keys)
    val enemiesKilled: Int,
    val xpGained: Int,
    val goldGained: Int,
    val xpLost: Int = 0, // For defeats
    val goldLost: Int = 0, // For defeats
    val playerLevel: Int,
    val locationId: String // Where combat happened
)

@Serializable
data class GameStateZ7(
    // Time and World
    val currentDay: Int = 1,
    val currentTimeOfDay: TimeOfDay = TimeOfDay.MORNING,
    val currentSeason: Season = Season.SPRING,
    val currentWeather: Weather = Weather.SUNNY, 
    val currentLocationId: String = WorldLocations.FOREST_CAMP.id,
    val knownLocations: List<Location> = listOf(WorldLocations.FOREST_CAMP), 
    val currentDirection: Direction = Direction.DOGU, 
    val travelDescription: String = "Yeşil ovalardan doğuya doğru ilerliyor",

    // Social System
    val npcRelationships: Map<String, NPCRelationship> = emptyMap(),

    // Player Data
    val playerState: PlayerState = PlayerState(),
    val resources: ResourcesZ4 = ResourcesZ4(),
    val collectedItems: CollectedItemsZ5 = CollectedItemsZ5(),

    // Equipment System
    val equippedItems: Map<String, EquippedItemZ6> = emptyMap(),
    val inventory: List<EquippedItemZ6> = emptyList(),

    // Character Progression System
    val activeBadges: List<Badge> = emptyList(),
    val activeSkills: List<Skill> = emptyList(),
    val unlockedSeals: List<Seal> = emptyList(),  // Kadim Mühür Teknikleri 

    // Story Progress
    val storyPages: List<String> = listOf(LanguageManager.getText("chosen_one_story")),
    val currentPage: Int = 1,

    // Quest System
    val activeQuests: List<RegistryQuest> = emptyList(),
    val completedQuests: List<RegistryQuest> = emptyList(),

    // Game Mechanics
    val isGameActive: Boolean = true,
    val lastActionTime: Long = System.currentTimeMillis(),
    val umbrosContract: UmbrosContract? = null,

    // Combat State
    val inCombat: Boolean = false,
    val currentEnemies: List<Enemy> = emptyList(),

    // G113.4: Status Effect System
    val activeStatusEffects: List<ActiveStatusEffect> = emptyList(), 

    // Class Quest System
    val triggeredClassQuests: Set<String> = emptySet(),

    // Threat Management System
    val threatCounter: Int = 0,

    // TODO-G63.1: Death Tracking System
    val totalDeaths: Int = 0,
    val lastDeathCause: String = "", // Localization key (e.g., "death_cause_combat")
    val lastDeathTimestamp: Long = 0L,

    // TODO-G62: Combat History Tracking
    val combatHistory: List<CombatHistoryEntry> = emptyList(),
    val totalCombatsWon: Int = 0,
    val totalCombatsLost: Int = 0,
    val totalEnemiesKilled: Int = 0
)


@Singleton
class GameStateManager @Inject constructor(
    private val gameStateDao: GameStateDao,
    private val dynamicNPCDao: DynamicNPCDao,
    private val observerEngine: com.example.isekaikuroshin.engine.ObserverEngine,
    private val promptEngine: com.example.isekaikuroshin.engine.PromptEngine,
    private val directorEngine: com.example.isekaikuroshin.engine.DirectorEngine,
    private val dynamicPlaylistEngine: com.example.isekaikuroshin.engine.DynamicPlaylistEngine,
    // KRM-SYS-22: KarmaBasedContentEngine removed (now using IntelligentContentEngine)
    private val worldUpdateEngine: Lazy<com.example.isekaikuroshin.engine.WorldUpdateEngine>
) {
    private val _gameState = MutableStateFlow(GameStateZ7())
    val gameState: StateFlow<GameStateZ7> = _gameState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val deathManager = DeathManager(this, scope)

    // TODO-G112.1: StaminaSystem singleton instance
    private val staminaSystem = StaminaSystem(this)

    // FIX-JOURNAL-SPAM: Debounce mekanizması için değişkenler
    private var saveJob: kotlinx.coroutines.Job? = null
    private val saveDebounceMs = 500L // 500ms bekle, sonra kaydet

    companion object {
        private const val CARRY_CAPACITY_MULTIPLIER = 5.0f
    }

    init {
        loadGameStateFromDatabase()
    }

    private fun loadGameStateFromDatabase() {
        scope.launch {
            val savedState = gameStateDao.getGameStateSync()?.toGameState()
            if (savedState != null) {
                _gameState.value = savedState
                GameLogger.logSystem("[DB-LOAD] Game state loaded from database")
                GameLogger.logSystem("[DB-LOAD] Story pages count: ${savedState.storyPages.size}")
                GameLogger.logSystem("[DB-LOAD] Current page: ${savedState.currentPage}")
                savedState.storyPages.forEachIndexed { index, page ->
                    GameLogger.logSystem("[DB-LOAD] Page ${index + 1} preview: ${page.take(80)}...")
                }
            } else {
                GameLogger.logSystem("[DB-LOAD] No saved game state found, using default state")
                GameLogger.logSystem("[DB-LOAD] Default state pages: ${_gameState.value.storyPages.size}")
                saveGameStateToDatabase()
            }
        }
    }

    private fun saveGameStateToDatabase() {
        // FIX-DEATH-INVENTORY: Oyuncu ölüyse veya oyun aktif değilse kaydetme
        if (_gameState.value.playerState.currentHealth <= 0 || !_gameState.value.isGameActive) {
            GameLogger.logSystem("Player is dead or game inactive, skipping game state save")
            return
        }

        // FIX-JOURNAL-SPAM: Önceki kaydetme işini iptal et
        saveJob?.cancel()

        // FIX-JOURNAL-SPAM: Yeni bir kaydetme işi başlat (500ms sonra çalışacak)
        saveJob = scope.launch {
            kotlinx.coroutines.delay(saveDebounceMs)

            // Yeniden kontrol et (delay sırasında oyuncu ölmüş olabilir)
            if (_gameState.value.playerState.currentHealth <= 0 || !_gameState.value.isGameActive) {
                GameLogger.logSystem("Player died or game became inactive during save delay, cancelling save")
                return@launch
            }

            dynamicPlaylistEngine.generatePlaylist()
            gameStateDao.insertOrUpdate(_gameState.value.toEntity())
            GameLogger.logSystem("Game state saved to database successfully")

            // KRM-SYS-22: KarmaBasedContentEngine removed
            // All users now use IntelligentContentEngine (handled in UserEntryViewModel)
            GameLogger.logVerbose("KARMA-ENGINE", "Legacy KARMA system disabled - using IntelligentContentEngine for all users")
        }
    }

    fun updatePlayerState(newState: PlayerState) {
        _gameState.update { it.copy(playerState = newState) }
        saveGameStateToDatabase()
        deathManager.checkForDeath(newState)
    }

    /**
     * G113.4: Update active status effects
     */
    fun updateStatusEffects(updatedEffects: List<ActiveStatusEffect>) {
        _gameState.update { it.copy(activeStatusEffects = updatedEffects) }
        saveGameStateToDatabase()
        GameLogger.logSystem("G113.4: Status effects updated (${updatedEffects.size} active)")
    }

    /**
     * G113.1: Apply status effect to player
     *
     * @param effectId Status effect ID (poison, burn, regeneration, etc.)
     * @param magnitude Optional magnitude override (null = use default)
     * @param duration Optional duration override (null = use default)
     */
    fun applyStatusEffect(effectId: String, magnitude: Int? = null, duration: Int? = null) {
        val statusEffectManager = StatusEffectManager()
        val currentState = _gameState.value
        val currentTurn = currentState.currentDay

        // Get effect from CommonStatusEffects
        val effect = when (effectId.lowercase()) {
            "poison" -> CommonStatusEffects.poison(magnitude ?: 5, duration ?: 3)
            "burn" -> CommonStatusEffects.burn(magnitude ?: 5, duration ?: 2)
            "slow" -> CommonStatusEffects.slow(magnitude ?: 3, duration ?: 3)
            "weakened" -> CommonStatusEffects.weakened(magnitude ?: 3, duration ?: 3)
            "regeneration" -> CommonStatusEffects.regeneration(magnitude ?: 3, duration ?: 5)
            "shield" -> CommonStatusEffects.shield(magnitude ?: 5, duration ?: 3)
            else -> {
                GameLogger.logError("GameStateManager", "Unknown status effect ID: $effectId")
                return
            }
        }

        // Apply effect
        val updatedEffects = statusEffectManager.applyEffect(
            effect = effect,
            currentTurn = currentTurn,
            activeEffects = currentState.activeStatusEffects
        )

        updateStatusEffects(updatedEffects)
        GameLogger.logSystem("G113.1: Applied $effectId effect (${updatedEffects.size} active effects)")
    }

    fun updatePlayerResources(newResources: ResourcesZ4) {
        _gameState.update { it.copy(resources = newResources) }
        saveGameStateToDatabase()
        GameLogger.logSystem("Player resources updated.")
    }

    fun addExperience(amount: Int) {
        if (amount == 0) return
        _gameState.update { currentState ->
            var newPlayerState = currentState.playerState.copy(
                experience = currentState.playerState.experience + amount
            )
            GameLogger.logSystem("Gained $amount EXP. Total EXP: ${newPlayerState.experience}")

            var newState = currentState.copy(playerState = newPlayerState)
            while (newPlayerState.experience >= newPlayerState.experienceToNextLevel) {
                newState = levelUp(newState)
                newPlayerState = newState.playerState
            }
            newState
        }
        saveGameStateToDatabase()
    }

    private fun levelUp(currentState: GameStateZ7): GameStateZ7 {
        val oldState = currentState.playerState
        val newLevel = oldState.level + 1
        val newExp = oldState.experience - oldState.experienceToNextLevel
        val newExpToNext = (oldState.experienceToNextLevel * 1.5).roundToInt()
        val newStatPoints = oldState.statPoints + 5

        val newPlayerState = oldState.copy(
            level = newLevel,
            experience = newExp,
            experienceToNextLevel = newExpToNext,
            statPoints = newStatPoints,
            currentHealth = oldState.maxHealth,
            currentMana = oldState.maxMana
        )

        GameLogger.logSystem("LEVEL UP! You are now level $newLevel.")
        // OVERLAY sistemi zaten seviye atlamayı gösteriyor, journal'a spam yapmaya gerek yok

        return currentState.copy(playerState = newPlayerState)
    }

    fun applyStatAllocations(allocations: Map<com.example.isekaikuroshin.engine.StatType, Int>) {
        _gameState.update { currentState ->
            val playerState = currentState.playerState
            val totalPointsToSpend = allocations.values.sum()

            if (playerState.statPoints >= totalPointsToSpend) {
                var newPlayerState = playerState.copy(statPoints = playerState.statPoints - totalPointsToSpend)

                allocations.forEach { (statType, amount) ->
                    newPlayerState = when (statType) {
                        com.example.isekaikuroshin.engine.StatType.STR -> newPlayerState.copy(strength = newPlayerState.strength + amount)
                        com.example.isekaikuroshin.engine.StatType.AGI -> newPlayerState.copy(agility = newPlayerState.agility + amount)
                        com.example.isekaikuroshin.engine.StatType.INT -> newPlayerState.copy(intelligence = newPlayerState.intelligence + amount)
                        com.example.isekaikuroshin.engine.StatType.VIT -> newPlayerState.copy(vitality = newPlayerState.vitality + amount)
                        com.example.isekaikuroshin.engine.StatType.SPIRIT -> newPlayerState.copy(spirit = newPlayerState.spirit + amount)
                        com.example.isekaikuroshin.engine.StatType.LUCK -> newPlayerState.copy(luck = newPlayerState.luck + amount)
                        else -> {
                            GameLogger.logSystem("⚠️ Unknown stat type for allocation: $statType")
                            newPlayerState
                        }
                    }
                }
                
                val finalPlayerState = recalculateDerivedStats(newPlayerState)
                GameLogger.logSystem("Applied stat allocations: $totalPointsToSpend points spent.")
                currentState.copy(playerState = finalPlayerState)
            } else {
                GameLogger.logSystem("Not enough stat points to apply allocations.")
                currentState
            }
        }
        saveGameStateToDatabase()
    }

    private fun recalculateDerivedStats(playerState: PlayerState): PlayerState {
        val newMaxHealth = (playerState.vitality * 10).roundToInt()
        val newMaxMana = (playerState.intelligence * 5).roundToInt()
        val newPhysicalAttack = (playerState.strength * 2).roundToInt()
        val newMagicPower = (playerState.intelligence * 1.5).roundToInt()
        val newDefense = (playerState.vitality * 1.2).roundToInt()

        return playerState.copy(
            maxHealth = newMaxHealth,
            maxMana = newMaxMana,
            physicalAttack = newPhysicalAttack,
            magicPower = newMagicPower,
            defense = newDefense,
            currentHealth = playerState.currentHealth.coerceAtMost(newMaxHealth),
            currentMana = playerState.currentMana.coerceAtMost(newMaxMana)
        )
    }

    fun updateMoralityScore(scoreChange: Float) {
        _gameState.update { currentState ->
            val currentStats = currentState.playerState
            val newMoralityScore = (currentStats.moralityScore + scoreChange).coerceIn(-1.0f, 1.0f)
            val newStats = currentStats.copy(moralityScore = newMoralityScore)
            GameLogger.logSystem("Morality score updated by $scoreChange to $newMoralityScore")
            currentState.copy(playerState = newStats)
        }
        saveGameStateToDatabase()
    }

    fun updateGold(amount: Int) {
        if (amount == 0) return
        _gameState.update { currentState ->
            val newGold = currentState.playerState.gold + amount
            val newPlayerState = currentState.playerState.copy(gold = newGold)
            val logMessage = if (amount > 0) "Gained $amount gold." else "Lost ${-amount} gold."
            GameLogger.logSystem("$logMessage Total gold: $newGold")
            currentState.copy(playerState = newPlayerState)
        }
        saveGameStateToDatabase()
    }

    fun decrementStatPoints(pointsToSpend: Int) {
        if (pointsToSpend <= 0) return

        var stateChanged = false
        _gameState.update { currentState ->
            val playerState = currentState.playerState
            if (playerState.statPoints >= pointsToSpend) {
                val newPlayerState = playerState.copy(
                    statPoints = playerState.statPoints - pointsToSpend
                )
                GameLogger.logSystem("Spent $pointsToSpend stat point(s). Remaining: ${newPlayerState.statPoints}")
                stateChanged = true
                currentState.copy(playerState = newPlayerState)
            } else {
                GameLogger.logSystem("Not enough stat points to spend. Needed: $pointsToSpend, Available: ${playerState.statPoints}")
                currentState 
            }
        }

        if (stateChanged) {
            saveGameStateToDatabase()
        }
    }

    fun modifyHealth(amount: Int) {
        _gameState.update { currentState ->
            val player = currentState.playerState
            val newHealth = (player.currentHealth + amount).coerceIn(0, player.maxHealth)
            val newPlayerState = player.copy(currentHealth = newHealth)

            // G104: Critical logging for health changes
            val changeText = if (amount > 0) "Healed $amount HP" else "Took ${-amount} damage"
            GameLogger.logSystem("$changeText. HP: $newHealth/${player.maxHealth}")

            if (newHealth == 0) {
                deathManager.checkForDeath(newPlayerState)
            }
            currentState.copy(playerState = newPlayerState)
        }
        saveGameStateToDatabase()
    }

    fun modifyMana(amount: Int) {
        _gameState.update { currentState ->
            val player = currentState.playerState
            val newMana = (player.currentMana + amount).coerceIn(0, player.maxMana)
            val newPlayerState = player.copy(currentMana = newMana)

            // G104: Critical logging for mana changes
            val changeText = if (amount > 0) "Restored $amount MP" else "Spent ${-amount} MP"
            GameLogger.logSystem("$changeText. MP: $newMana/${player.maxMana}")

            currentState.copy(playerState = newPlayerState)
        }
        saveGameStateToDatabase()
    }

    fun getTrainingCost(statType: com.example.isekaikuroshin.engine.StatType): Int {
        val playerState = _gameState.value.playerState
        val baseCost = 50
        val currentStatValue: Float = when(statType) {
            com.example.isekaikuroshin.engine.StatType.STR -> playerState.strength
            com.example.isekaikuroshin.engine.StatType.AGI -> playerState.agility
            com.example.isekaikuroshin.engine.StatType.INT -> playerState.intelligence
            com.example.isekaikuroshin.engine.StatType.VIT -> playerState.vitality
            else -> throw IllegalArgumentException("Stat ${statType.name} cannot be trained directly.")
        }
        return (baseCost + currentStatValue * 2).toInt()
    }

    fun trainStat(statType: com.example.isekaikuroshin.engine.StatType) {
        val cost = getTrainingCost(statType) 
        if (_gameState.value.playerState.gold >= cost) {
            updateGold(-cost)
            _gameState.update { currentState ->
                val newPlayerState = when (statType) {
                    com.example.isekaikuroshin.engine.StatType.STR -> currentState.playerState.copy(strength = currentState.playerState.strength + 1)
                    com.example.isekaikuroshin.engine.StatType.AGI -> currentState.playerState.copy(agility = currentState.playerState.agility + 1)
                    com.example.isekaikuroshin.engine.StatType.INT -> currentState.playerState.copy(intelligence = currentState.playerState.intelligence + 1)
                    com.example.isekaikuroshin.engine.StatType.VIT -> currentState.playerState.copy(vitality = currentState.playerState.vitality + 1)
                    else -> currentState.playerState 
                }
                val finalPlayerState = recalculateDerivedStats(newPlayerState)
                // Training sonucu son sayfaya değil, AI yanıtına dahil edilmeli
                currentState.copy(playerState = finalPlayerState)
            }
            incrementThreatCounter(2) // Güvenli eylem: antrenman
            advanceTime()
        } else {
            // Yeterli altın yok mesajı overlay ile gösterilmeli
            GameLogger.logSystem("[TRAINING] Insufficient gold")
        }
    }
    
    private fun calculateCurrentWeight(): Float {
        val gameStateValue = _gameState.value
        val inventoryWeight = gameStateValue.inventory.sumOf { it.weight.toDouble() }.toFloat()
        val equippedWeight = gameStateValue.equippedItems.values.sumOf { it.weight.toDouble() }.toFloat()
        return inventoryWeight + equippedWeight
    }

    private fun calculateMaxCarryCapacity(): Float {
        return _gameState.value.playerState.strength * CARRY_CAPACITY_MULTIPLIER
    }

    fun getWeightCapacityRatio(): Float {
        val currentWeight = calculateCurrentWeight()
        val maxCapacity = calculateMaxCarryCapacity()
        return if (maxCapacity > 0f) currentWeight / maxCapacity else 0f
    }

    fun getWeightStatusDescription(): String {
        val ratio = getWeightCapacityRatio()
        return when {
            ratio <= 0.5f -> "Hafif"
            ratio <= 0.75f -> "Normal"
            ratio <= 1.0f -> "Ağır"
            else -> "Aşırı Yüklü"
        }
    }

    fun getCurrentWeightString(): String {
        val currentWeight = calculateCurrentWeight()
        val maxCapacity = calculateMaxCarryCapacity()
        return String.format("%.1f/%.1f kg", currentWeight, maxCapacity)
    }

    fun advanceTime(): Boolean {
        val current = _gameState.value
        GameLogger.logSystem("[TIME-SYSTEM] advanceTime() called - Current: ${current.currentTimeOfDay}")

        val nextTimeOfDay = when (current.currentTimeOfDay) {
            TimeOfDay.MORNING -> {
                GameLogger.logSystem("[TIME-SYSTEM] Morning → Noon")
                TimeOfDay.NOON
            }
            TimeOfDay.NOON -> {
                GameLogger.logSystem("[TIME-SYSTEM] Noon → Afternoon")
                TimeOfDay.AFTERNOON
            }
            TimeOfDay.AFTERNOON -> {
                GameLogger.logSystem("[TIME-SYSTEM] Afternoon → Evening")
                TimeOfDay.EVENING
            }
            TimeOfDay.EVENING -> {
                GameLogger.logSystem("[TIME-SYSTEM] Evening → Night")
                TimeOfDay.NIGHT
            }
            TimeOfDay.NIGHT -> {
                // YENİ GÜN BAŞLADI - YENİ SAYFA OLUŞTUR
                GameLogger.logSystem("[TIME-SYSTEM] ⭐ Night → Morning: NEW DAY STARTS!")
                GameLogger.logSystem("[TIME-SYSTEM] Current pages before new day: ${current.storyPages.size}")
                GameLogger.logSystem("[TIME-SYSTEM] Creating new story page for Day ${current.currentDay + 1}")

                // TEST-LOG: Gün bitişi detayları
                GameLogger.logSystem("[TEST-DAY-END] ========================================")
                GameLogger.logSystem("[TEST-DAY-END] DAY ${current.currentDay} COMPLETED")
                GameLogger.logSystem("[TEST-DAY-END] Current time: Night → Morning")
                GameLogger.logSystem("[TEST-DAY-END] Total story pages before transition: ${current.storyPages.size}")
                GameLogger.logSystem("[TEST-DAY-END] ========================================")

                val newDayHeader = "\n\n=== Day ${current.currentDay + 1} - Morning ===\n\n"
                _gameState.update {
                    val newPages = it.storyPages + newDayHeader
                    GameLogger.logSystem("[TIME-SYSTEM] New pages list size: ${newPages.size}")
                    it.copy(
                        currentDay = it.currentDay + 1,
                        currentTimeOfDay = TimeOfDay.MORNING,
                        storyPages = newPages,
                        currentPage = newPages.size
                    )
                }
                saveGameStateToDatabase()

                // TEST-LOG: Yeni gün başlangıcı detayları
                GameLogger.logSystem("[TEST-DAY-START] ========================================")
                GameLogger.logSystem("[TEST-DAY-START] DAY ${_gameState.value.currentDay} STARTED")
                GameLogger.logSystem("[TEST-DAY-START] New time: Morning")
                GameLogger.logSystem("[TEST-DAY-START] Total story pages after transition: ${_gameState.value.storyPages.size}")
                GameLogger.logSystem("[TEST-DAY-START] ========================================")

                // WorldUpdateEngine'i tetikle (Nemesis Sistemi - TODO-NEM-02)
                // ÖNEMLİ: ESKİ günün numarasını geç (synthesis için)
                val previousDay = _gameState.value.currentDay - 1
                worldUpdateEngine.get().onDayPassed(previousDay)

                GameLogger.logSystem("[TIME-SYSTEM] ✅ New page created. Total pages: ${_gameState.value.storyPages.size}")
                return true
            }
        }

        // TODO-G112.2: TimeSystem Integration - Her saat geçişinde stamina/fatigue/hunger artışı
        staminaSystem.performAction(staminaCost = 5, fatigueGain = 2) // 5 stamina kaybı, 2 fatigue artışı
        staminaSystem.increaseHunger(hungerIncrease = 3) // 3 hunger artışı
        GameLogger.logSystem("[TIME-SYSTEM] StaminaSystem: -5 stamina, +2 fatigue, +3 hunger")

        // TODO-G112.4: Starvation Check - Açlık 80+ ise ekstra stamina kaybı
        val currentHunger = _gameState.value.playerState.hunger
        if (currentHunger >= 80) {
            val starvationPenalty = (currentHunger - 80) / 5 // 80-100 arası 0-4 penalty
            staminaSystem.performAction(staminaCost = starvationPenalty.coerceAtLeast(1), fatigueGain = 1)
            GameLogger.logSystem("[TIME-SYSTEM] ⚠️ STARVATION! Hunger=$currentHunger, Extra stamina loss: $starvationPenalty")
        }

        _gameState.update { it.copy(currentTimeOfDay = nextTimeOfDay, lastActionTime = System.currentTimeMillis()) }
        GameLogger.logSystem("[TIME-SYSTEM] Time advanced to: $nextTimeOfDay")
        saveGameStateToDatabase()
        return false
    }

    fun addStoryPage(content: String) {
        _gameState.update {
            val updatedPages = it.storyPages + content
            it.copy(storyPages = updatedPages, currentPage = updatedPages.size)
        }
        saveGameStateToDatabase()
    }

    fun updateLastStoryPage(newContent: String) {
        GameLogger.logSystem("[UPDATE-PAGE] updateLastStoryPage called")
        GameLogger.logSystem("[UPDATE-PAGE] Current pages count: ${_gameState.value.storyPages.size}")
        GameLogger.logSystem("[UPDATE-PAGE] New content length: ${newContent.length}")
        GameLogger.logSystem("[UPDATE-PAGE] New content preview: ${newContent.take(100)}...")

        _gameState.update {
            if (it.storyPages.isNotEmpty()) {
                val updatedPages = it.storyPages.toMutableList()
                GameLogger.logSystem("[UPDATE-PAGE] Updating page index: ${updatedPages.lastIndex + 1}")
                GameLogger.logSystem("[UPDATE-PAGE] Old content preview: ${updatedPages[updatedPages.lastIndex].take(100)}...")
                updatedPages[updatedPages.lastIndex] = newContent
                it.copy(storyPages = updatedPages)
            } else {
                GameLogger.logSystem("[UPDATE-PAGE] ⚠️ No pages exist! Cannot update.")
                it
            }
        }
        GameLogger.logSystem("[UPDATE-PAGE] After update - Total pages: ${_gameState.value.storyPages.size}")
        saveGameStateToDatabase()
    }

    fun updatePlayerProfile(newProfile: PlayerProfile) {
        _gameState.update {
            val newPlayerState = it.playerState.copy(playerProfile = newProfile)
            it.copy(playerState = newPlayerState)
        }
        saveGameStateToDatabase()
        checkForClassQuestTriggers()
    }

    /**
     * TODO-G106.2: ProfileUpdaterEngine Integration
     * Track player actions and update archetype scores
     */
    fun trackPlayerAction(actionType: com.example.isekaikuroshin.engine.ActionType) {
        val currentProfile = _gameState.value.playerState.playerProfile
        val updatedProfile = com.example.isekaikuroshin.engine.ProfileUpdaterEngine.updateProfile(actionType, currentProfile)

        if (updatedProfile != currentProfile) {
            updatePlayerProfile(updatedProfile)
            GameLogger.logSystem("[PROFILE] Action tracked: $actionType → Dominant: ${updatedProfile.dominantArchetype}")
        }
    }

    /**
     * TODO-G106.3: Keyword-based Action Detection
     * Parse player input and detect archetype actions
     */
    fun detectAndTrackAction(input: String) {
        val lowercased = input.lowercase()

        val actionType = when {
            // Warrior actions
            lowercased.contains("saldır") || lowercased.contains("attack") ||
            lowercased.contains("vur") || lowercased.contains("dövüş") ->
                com.example.isekaikuroshin.engine.ActionType.ATTACK

            lowercased.contains("savun") || lowercased.contains("defend") ||
            lowercased.contains("kal") || lowercased.contains("shield") ->
                com.example.isekaikuroshin.engine.ActionType.DEFEND

            // Explorer actions
            lowercased.contains("gizlen") || lowercased.contains("hide") ||
            lowercased.contains("stealth") || lowercased.contains("saklan") ->
                com.example.isekaikuroshin.engine.ActionType.HIDE

            lowercased.contains("iz sür") || lowercased.contains("track") ||
            lowercased.contains("takip et") || lowercased.contains("izle") ->
                com.example.isekaikuroshin.engine.ActionType.TRACK

            // Mystic actions
            lowercased.contains("büyü") || lowercased.contains("spell") ||
            lowercased.contains("magic") || lowercased.contains("sihir") ->
                com.example.isekaikuroshin.engine.ActionType.CAST_SPELL

            lowercased.contains("ritüel") || lowercased.contains("ritual") ||
            lowercased.contains("tören") ->
                com.example.isekaikuroshin.engine.ActionType.RITUAL

            // Craftsman actions
            lowercased.contains("yap") || lowercased.contains("craft") ||
            lowercased.contains("üret") || lowercased.contains("imal") ->
                com.example.isekaikuroshin.engine.ActionType.CRAFT

            lowercased.contains("onar") || lowercased.contains("repair") ||
            lowercased.contains("tamir") || lowercased.contains("düzelt") ->
                com.example.isekaikuroshin.engine.ActionType.REPAIR

            else -> null
        }

        actionType?.let { trackPlayerAction(it) }
    }

    private fun checkForClassQuestTriggers() {
        val currentState = _gameState.value
        val playerProfile = currentState.playerState.playerProfile
        val archetypeScores = playerProfile.archetypeScores
        val triggeredQuests = currentState.triggeredClassQuests

        archetypeScores.forEach { (archetype, score) ->
            if (score >= 100 && !triggeredQuests.contains(archetype)) {
                val classQuest = ContentRegistry.ClassQuests.getClassQuest(archetype)
                if (classQuest != null) {
                    _gameState.update { state ->
                        state.copy(triggeredClassQuests = state.triggeredClassQuests + archetype)
                    }
                    // Quest overlay sistemi zaten gösteriyor
                    GameLogger.logSystem("[QUEST] ${classQuest.quest.title} tetiklendi! Arketip: $archetype")
                } 
            }
        }
    }

    fun updateUmbrosContract(contract: UmbrosContract?) {
        _gameState.update { it.copy(umbrosContract = contract) }
        saveGameStateToDatabase()
    }

    fun incrementThreatCounter(amount: Int) {
        _gameState.update { currentState ->
            val newThreatCounter = currentState.threatCounter + amount
            GameLogger.logSystem("Tehdit sayacı $amount arttı. Yeni değer: $newThreatCounter")
            currentState.copy(threatCounter = newThreatCounter)
        }
        saveGameStateToDatabase()
    }

    fun resetThreatCounter() {
        _gameState.update { currentState ->
            GameLogger.logSystem("Tehdit sayacı sıfırlandı. Önceki değer: ${currentState.threatCounter}")
            currentState.copy(threatCounter = 0)
        }
        saveGameStateToDatabase()
    }

    // Gelecekte eklenecek güvenli eylemler için örnekler:
    fun craftItem() {
        // Placeholder: Eşya üretimi mantığı burada olacak
        GameLogger.logSystem("[CRAFT] Item crafted")
        incrementThreatCounter(3) // Güvenli eylem: eşya üretimi
        advanceTime()
    }

    fun restInTavern() {
        // Placeholder: Handte dinlenme mantığı burada olacak
        GameLogger.logSystem("[REST] Rested in tavern")
        incrementThreatCounter(5) // Güvenli eylem: dinlenme (daha yüksek değer)
        advanceTime()
    }

    // ===== ENVANTER VE EKİPMAN EYLEMLERİ =====

    fun addItemToInventory(itemId: String, quantity: Int) {
        _gameState.update { currentState ->
            // Basit prototip: EquippedItemZ6 nesnesi oluştur
            val newItem = EquippedItemZ6(
                id = itemId,
                name = "Item_$itemId",
                description = "AI tarafından eklenen eşya",
                weight = 1.0f,
                rarity = "COMMON",
                statBonuses = emptyMap()
            )

            // Mevcut envanteri kopyala ve yeni eşyaları ekle
            val updatedInventory = currentState.inventory.toMutableList()
            repeat(quantity) {
                updatedInventory.add(newItem)
            }

            GameLogger.logSystem("Envantere eklendi: $itemId x$quantity")
            currentState.copy(inventory = updatedInventory)
        }
        saveGameStateToDatabase()
    }

    fun removeItemFromInventory(itemId: String, quantity: Int) {
        _gameState.update { currentState ->
            val updatedInventory = currentState.inventory.toMutableList()
            var removedCount = 0

            // Belirtilen ID'ye sahip eşyaları kaldır
            updatedInventory.removeAll { item ->
                if (item.id == itemId && removedCount < quantity) {
                    removedCount++
                    true
                } else {
                    false
                }
            }

            GameLogger.logSystem("Envanterden çıkarıldı: $itemId x$removedCount")
            currentState.copy(inventory = updatedInventory)
        }
        saveGameStateToDatabase()
    }

    fun equipItem(itemId: String, slot: String) {
        _gameState.update { currentState ->
            // Envanterde eşyayı bul
            val itemToEquip = currentState.inventory.find { it.id == itemId }

            if (itemToEquip != null) {
                // Eşyayı kuşan
                val updatedEquippedItems = currentState.equippedItems.toMutableMap()
                updatedEquippedItems[slot] = itemToEquip

                // Eşyayı envanterden çıkar
                val updatedInventory = currentState.inventory.toMutableList()
                updatedInventory.removeAll { it.id == itemId }

                GameLogger.logSystem("Eşya kuşanıldı: $itemId -> $slot")
                currentState.copy(
                    equippedItems = updatedEquippedItems,
                    inventory = updatedInventory
                )
            } else {
                GameLogger.logSystem("Kuşanacak eşya bulunamadı: $itemId")
                currentState
            }
        }
        saveGameStateToDatabase()
    }

    // ===== GÖREV VE HİKAYE EYLEMLERİ =====

    fun addQuest(quest: RegistryQuest) {
        _gameState.update { currentState ->
            // Görevin zaten aktif olup olmadığını kontrol et
            val isAlreadyActive = currentState.activeQuests.any { it.id == quest.id }

            if (!isAlreadyActive) {
                val updatedActiveQuests = currentState.activeQuests + quest
                GameLogger.logSystem("[QUEST] Yeni görev eklendi: ${quest.title}")
                // Quest overlay ile gösterilmeli
                currentState.copy(activeQuests = updatedActiveQuests)
            } else {
                GameLogger.logSystem("[QUEST] Görev zaten aktif: ${quest.title}")
                currentState
            }
        }
        saveGameStateToDatabase()
    }

    fun completeQuest(questId: String) {
        _gameState.update { currentState ->
            // Aktif görevler arasında ara
            val questToComplete = currentState.activeQuests.find { it.id == questId }

            if (questToComplete != null) {
                // Görevi aktif listesinden çıkar ve tamamlanan listesine ekle
                val updatedActiveQuests = currentState.activeQuests.filter { it.id != questId }
                val updatedCompletedQuests = currentState.completedQuests + questToComplete

                GameLogger.logSystem("[QUEST] Görev tamamlandı: ${questToComplete.title}")
                // Quest completion overlay ile gösterilmeli

                currentState.copy(
                    activeQuests = updatedActiveQuests,
                    completedQuests = updatedCompletedQuests
                )
            } else {
                GameLogger.logSystem("Tamamlanacak görev bulunamadı: $questId")
                currentState
            }
        }
        saveGameStateToDatabase()
    }

    fun failQuest(questId: String) {
        _gameState.update { currentState ->
            // Aktif görevler arasında ara
            val questToFail = currentState.activeQuests.find { it.id == questId }

            if (questToFail != null) {
                // Görevi aktif listesinden çıkar (başarısız görevler için ayrı liste yok, sadece kaldırılır)
                val updatedActiveQuests = currentState.activeQuests.filter { it.id != questId }

                GameLogger.logSystem("[QUEST] Görev başarısız oldu: ${questToFail.title}")
                // Quest fail overlay ile gösterilmeli

                currentState.copy(activeQuests = updatedActiveQuests)
            } else {
                GameLogger.logSystem("Başarısız yapılacak görev bulunamadı: $questId")
                currentState
            }
        }
        saveGameStateToDatabase()
    }

    // ===== DÜNYA VE ETKİLEŞİM EYLEMLERİ =====

    fun updateNpcRelationship(npcId: String, changeAmount: Int) {
        _gameState.update { currentState ->
            val currentRelationships = currentState.npcRelationships.toMutableMap()

            // Mevcut ilişkiyi al veya yeni oluştur
            val currentRelationship = currentRelationships[npcId] ?: NPCRelationship(
                npcId = npcId,
                relationshipLevel = RelationshipLevel.NEUTRAL,
                relationshipPoints = 0,
                firstMeetDate = System.currentTimeMillis(),
                lastInteractionDate = System.currentTimeMillis(),
                interactionHistory = emptyList()
            )

            // İlişki puanlarını güncelle (-100 ile 100 arası sınırla)
            val newRelationshipPoints = (currentRelationship.relationshipPoints + changeAmount).coerceIn(-100, 100)
            val newRelationshipLevel = RelationshipLevel.fromPoints(newRelationshipPoints)
            val updatedRelationship = currentRelationship.copy(
                relationshipLevel = newRelationshipLevel,
                relationshipPoints = newRelationshipPoints,
                lastInteractionDate = System.currentTimeMillis()
            )

            currentRelationships[npcId] = updatedRelationship

            val changeText = if (changeAmount > 0) "arttı" else "azaldı"
            GameLogger.logSystem("NPC ilişkisi güncellendi: $npcId -> $newRelationshipPoints ($changeText)")

            currentState.copy(npcRelationships = currentRelationships)
        }
        saveGameStateToDatabase()
    }

    fun discoverLocation(locationId: String) {
        _gameState.update { currentState ->
            // Lokasyonun zaten bilinip bilinmediğini kontrol et
            val isAlreadyKnown = currentState.knownLocations.any { it.id == locationId }

            if (!isAlreadyKnown) {
                // Basit lokasyon oluştur (gelecekte ContentRegistry'den alınabilir)
                val newLocation = Location(
                    id = locationId,
                    name = "Location_$locationId",
                    description = "AI tarafından keşfedilen lokasyon",
                    type = LocationType.WILDERNESS,
                    dangerLevel = 1,
                    requiredLevel = 1,
                    knownResources = emptyList(),
                    associatedQuests = emptyList(),
                    isDiscovered = true
                )

                val updatedKnownLocations = currentState.knownLocations + newLocation
                GameLogger.logSystem("[LOCATION] Yeni lokasyon keşfedildi: $locationId")

                // ÖNEMLİ: Yeni sayfa değil, son sayfaya ekleme yapıyoruz (sonsuz döngü önleme)
                // addStoryPage() yerine updateLastStoryPage() kullanarak mevcut sayfaya ekliyoruz
                val currentPageContent = currentState.storyPages.lastOrNull() ?: ""
                val updatedContent = "$currentPageContent\n\n🗺️ YENİ LOKASYON KEŞFEDİLDİ: ${newLocation.name}\n${newLocation.description}"

                // Sayfa içeriğini güncelle
                val updatedPages = currentState.storyPages.toMutableList()
                if (updatedPages.isNotEmpty()) {
                    updatedPages[updatedPages.lastIndex] = updatedContent
                }

                currentState.copy(
                    knownLocations = updatedKnownLocations,
                    storyPages = updatedPages
                )
            } else {
                GameLogger.logSystem("[LOCATION] Lokasyon zaten biliniyor: $locationId")
                currentState
            }
        }
        saveGameStateToDatabase()
    }

    fun changeLocation(newLocationId: String, addPageEntry: Boolean = true) {
        _gameState.update { currentState ->
            // Lokasyonun bilinip bilinmediğini kontrol et
            val isLocationKnown = currentState.knownLocations.any { it.id == newLocationId }

            if (isLocationKnown) {
                val oldLocationId = currentState.currentLocationId
                GameLogger.logSystem("Lokasyon değişti: $oldLocationId -> $newLocationId")

                // LOOP FIX: executeGMActions içinden çağrılırsa sayfa ekleme (overlay zaten gösterilecek)
                if (addPageEntry) {
                    addStoryPage("📍 Yeni konuma gidiyorsun: $newLocationId")
                }

                currentState.copy(
                    currentLocationId = newLocationId,
                    lastActionTime = System.currentTimeMillis()
                )
            } else {
                GameLogger.logSystem("Bilinmeyen lokasyona gidilemez: $newLocationId")
                if (addPageEntry) {
                    addStoryPage("❌ Bu lokasyonu henüz keşfetmedin: $newLocationId")
                }
                currentState
            }
        }
        saveGameStateToDatabase()
    }

    // ===== SAVAŞ VE DÜŞMAN EYLEMLERİ =====

    /**
     * G93 FIX: Deprecated - Use CombatController.startCombat() instead
     * Kept for backward compatibility but should not be used
     */
    @Deprecated("Use CombatController.startCombat() instead - nested update issue")
    fun startCombat(enemyIds: List<String>) {
        // Old implementation - DO NOT USE
        // This had nested update problem: addStoryPage() called inside _gameState.update{}
        GameLogger.logSystem("⚠️ startCombat() is deprecated! Use CombatController.startCombat()")
    }

    /**
     * G93 FIX: Update combat state (NO nested update!)
     * Separate function to update inCombat + enemies
     */
    fun updateCombatState(inCombat: Boolean, enemies: List<Enemy>) {
        _gameState.update { currentState ->
            currentState.copy(
                inCombat = inCombat,
                currentEnemies = enemies
            )
        }
        saveGameStateToDatabase()
    }

    /**
     * G93 FIX: Deprecated - Use CombatController.endCombat() instead
     */
    @Deprecated("Use updateCombatState(false, emptyList()) instead")
    fun endCombat() {
        GameLogger.logSystem("⚠️ endCombat() is deprecated! Use updateCombatState()")
    }

    /**
     * G93: Helper - Get player stats for combat calculations
     */
    fun getPlayerStats(): PlayerStats {
        val state = _gameState.value
        return PlayerStats(
            hp = state.playerState.currentHealth,
            maxHp = state.playerState.maxHealth,
            mp = state.playerState.currentMana,
            maxMp = state.playerState.maxMana,
            attack = state.playerState.physicalAttack,
            defense = state.playerState.defense,
            speed = 10 // Placeholder (could use agility later)
        )
    }

    /**
     * G93: Helper - Update player HP
     * NOTE: Uses existing PlayerState structure (currentHealth/maxHealth)
     */
    fun updatePlayerHp(newHp: Int) {
        _gameState.update { currentState ->
            val updatedPlayerState = currentState.playerState.copy(
                currentHealth = newHp.coerceIn(0, currentState.playerState.maxHealth)
            )
            currentState.copy(playerState = updatedPlayerState)
        }
        saveGameStateToDatabase()
    }

    /**
     * G97: Consume stamina and increase fatigue during combat
     * @param staminaCost Amount of stamina to consume
     * @param fatigueGain Amount of fatigue to gain
     */
    fun consumeCombatStamina(staminaCost: Int, fatigueGain: Int) {
        _gameState.update { currentState ->
            val playerState = currentState.playerState
            val newStamina = maxOf(0, playerState.stamina - staminaCost)
            val newFatigue = minOf(100, playerState.fatigue + fatigueGain)

            val updatedPlayerState = playerState.copy(
                stamina = newStamina,
                fatigue = newFatigue
            )
            currentState.copy(playerState = updatedPlayerState)
        }
        saveGameStateToDatabase()
        GameLogger.logSystem("[G97] Combat stamina consumed: -$staminaCost stamina, +$fatigueGain fatigue")
    }

    /**
     * G97: Decrease hunger (called during combat turns)
     * @param amount Amount of hunger to decrease
     */
    fun decreaseHunger(amount: Int) {
        _gameState.update { currentState ->
            val playerState = currentState.playerState
            val newHunger = maxOf(0, playerState.hunger - amount)

            val updatedPlayerState = playerState.copy(hunger = newHunger)
            currentState.copy(playerState = updatedPlayerState)
        }
        saveGameStateToDatabase()
        GameLogger.logSystem("[G97] Hunger decreased: -$amount (new: ${_gameState.value.playerState.hunger})")
    }

    /**
     * G102: Consume stamina (passive decrease per action)
     * @param staminaCost Amount of stamina to consume
     */
    fun consumeStamina(staminaCost: Int) {
        _gameState.update { currentState ->
            val playerState = currentState.playerState
            val newStamina = maxOf(0, playerState.stamina - staminaCost)

            val updatedPlayerState = playerState.copy(stamina = newStamina)
            currentState.copy(playerState = updatedPlayerState)
        }
        saveGameStateToDatabase()
        GameLogger.logSystem("[G102] Stamina consumed: -$staminaCost (new: ${_gameState.value.playerState.stamina})")
    }

    /**
     * G102: Increase fatigue (passive increase per action)
     * @param amount Amount of fatigue to increase
     */
    fun increaseFatigue(amount: Int) {
        _gameState.update { currentState ->
            val playerState = currentState.playerState
            val newFatigue = minOf(100, playerState.fatigue + amount)

            val updatedPlayerState = playerState.copy(fatigue = newFatigue)
            currentState.copy(playerState = updatedPlayerState)
        }
        saveGameStateToDatabase()
        GameLogger.logSystem("[G102] Fatigue increased: +$amount (new: ${_gameState.value.playerState.fatigue})")
    }

    // NOTE: addExperience() already exists at line 173 - using that one
    // NOTE: addGold() already exists at line 347 - using that one

    // === EXISTING FUNCTIONS CONTINUE (don't close class yet!) ===

    fun updateEnemyHealth(enemyId: String, damage: Int) {
        _gameState.update { currentState ->
            if (currentState.inCombat) {
                val updatedEnemies = currentState.currentEnemies.map { enemy ->
                    if (enemy.name == enemyId) {
                        val newHealth = (enemy.hp - damage).coerceAtLeast(0)
                        GameLogger.logSystem("Düşman hasar aldı: $enemyId -> $damage damage (Kalan can: $newHealth)")

                        if (newHealth == 0) {
                            addStoryPage("💥 ${enemy.name} yenildi!")
                            // Düşman öldüğünde XP ve altın ver (sabit değerler)
                            addExperience(50)
                            updateGold(25)
                        }

                        enemy.copy(hp = newHealth)
                    } else {
                        enemy
                    }
                }

                // Ölü düşmanları kaldır
                val aliveEnemies = updatedEnemies.filter { it.hp > 0 }

                // Tüm düşmanlar öldüyse savaşı bitir
                val newCombatState = if (aliveEnemies.isEmpty() && currentState.currentEnemies.isNotEmpty()) {
                    // TODO-G93: This should use CombatController instead
                    // addStoryPage("🎉 TÜM DÜŞMANLAR YENİLDİ!\n\nZaferi kazandın!")
                    false
                } else {
                    currentState.inCombat
                }

                currentState.copy(
                    currentEnemies = aliveEnemies,
                    inCombat = newCombatState
                )
            } else {
                GameLogger.logSystem("Savaş durumunda değiliz, düşman hasarı uygulanamaz.")
                currentState
            }
        }
        saveGameStateToDatabase()
    }

    fun loadTestData() {
        _gameState.value = createTestData()
        saveGameStateToDatabase()
        GameLogger.logSystem("Test data loaded.")
    }

    // REMOVED: Duplicate resetGame() - Using G63.1 version at line 2569

    // Test fonksiyonu - DirectorEngine Yaratıcı Çekirdek Testi
    fun testDirectorEngine(actionExecutor: com.example.isekaikuroshin.engine.ActionExecutorEngine? = null) {
        scope.launch {
            GameLogger.logSystem("=== DIRECTOR ENGINE - YARATICI ÇEKİRDEK TESTİ START ===")

            // Orijinal player state'i sakla
            val originalPlayerState = _gameState.value.playerState
            val originalThreatCounter = _gameState.value.threatCounter

            // TEST SENARYOSU 1: DÜŞÜK TEHDİT SAYACI - YARATICI ÇEKIRDEK AKTİF
            GameLogger.logSystem("--- TEST SENARYOSU 1: YARATICI ÇEKIRDEK AKTİF (Düşük Tehdit) ---")
            _gameState.update { currentState ->
                currentState.copy(
                    threatCounter = 10, // Düşük değer - yaratıcı çekirdek devreye girecek
                    playerState = currentState.playerState.copy(moralityScore = 0.0f) // Neutral morality
                )
            }

            // Mevcut tehdit durumunu logla
            val currentState1 = _gameState.value
            val pacingEngine = com.example.isekaikuroshin.engine.PacingEngine()
            val threshold1 = pacingEngine.calculateThreatThreshold(currentState1.playerState)
            GameLogger.logSystem("💠 TEHDİT SAYACI: ${currentState1.threatCounter}")
            GameLogger.logSystem("💠 TEHDİT EŞİĞİ: $threshold1")
            GameLogger.logSystem("💠 DURUM: ${if (currentState1.threatCounter >= threshold1) "TEHDİT EŞİĞİ AŞILDI" else "YARATICI ÇEKİRDEK DEVREYİ"}")

            val aiResponse1 = directorEngine.generateDynamicEvent(this@GameStateManager, actionExecutor)

            // TEST SENARYOSU 2: YÜKSEK TEHDİT SAYACI - TEHDİT MODU
            GameLogger.logSystem("--- TEST SENARYOSU 2: TEHDİT MODU AKTİF (Yüksek Tehdit) ---")
            _gameState.update { currentState ->
                currentState.copy(
                    threatCounter = 200, // Yüksek değer - tehdit modu devreye girecek
                    playerState = currentState.playerState.copy(moralityScore = 0.0f) // Neutral morality
                )
            }

            // Mevcut tehdit durumunu logla
            val currentState2 = _gameState.value
            val threshold2 = pacingEngine.calculateThreatThreshold(currentState2.playerState)
            GameLogger.logSystem("💠 TEHDİT SAYACI: ${currentState2.threatCounter}")
            GameLogger.logSystem("💠 TEHDİT EŞİĞİ: $threshold2")
            GameLogger.logSystem("💠 DURUM: ${if (currentState2.threatCounter >= threshold2) "TEHDİT EŞİĞİ AŞILDI" else "YARATICI ÇEKİRDEK DEVREDEYİ"}")

            val aiResponse2 = directorEngine.generateDynamicEvent(this@GameStateManager, actionExecutor)

            // TEST SENARYOSU 3: YARATICI ÇEKIRDEK + YÜKSEK HİKAYE BAĞLILIĞI
            GameLogger.logSystem("--- TEST SENARYOSU 3: YÜKSEK HİKAYE BAĞLILIĞI TESTİ ---")

            // PersistentDataManager'da storyAdherence'ı yüksek ayarla (test için)
            val originalStoryAdherence = com.example.isekaikuroshin.data.PersistentDataManager.gameData.value.settingsData.storySettings.storyAdherence
            com.example.isekaikuroshin.data.PersistentDataManager.updateSettingsData { settingsData ->
                settingsData.copy(
                    storySettings = settingsData.storySettings.copy(storyAdherence = 0.9f) // %90 hikaye odaklı
                )
            }

            _gameState.update { currentState ->
                currentState.copy(
                    threatCounter = 20, // Düşük değer - yaratıcı çekirdek devreye girecek
                    playerState = currentState.playerState.copy(moralityScore = 0.0f)
                )
            }

            GameLogger.logSystem("🎨 Test için Hikaye Bağlılık Oranı %90'a ayarlandı")
            val aiResponse3 = directorEngine.generateDynamicEvent(this@GameStateManager, actionExecutor)

            // TEST SENARYOSU 4: YARATICI ÇEKIRDEK + DÜŞÜK HİKAYE BAĞLILIĞI (Arketip Odaklı)
            GameLogger.logSystem("--- TEST SENARYOSU 4: DÜŞÜK HİKAYE BAĞLILIĞI - ARKETİP ODAKLI ---")

            com.example.isekaikuroshin.data.PersistentDataManager.updateSettingsData { settingsData ->
                settingsData.copy(
                    storySettings = settingsData.storySettings.copy(storyAdherence = 0.1f) // %10 hikaye odaklı - çoğunlukla arketip
                )
            }

            GameLogger.logSystem("🏛️ Test için Hikaye Bağlılık Oranı %10'a ayarlandı (Arketip odaklı)")
            val aiResponse4 = directorEngine.generateDynamicEvent(this@GameStateManager, actionExecutor)

            // Orijinal ayarları geri yükle
            com.example.isekaikuroshin.data.PersistentDataManager.updateSettingsData { settingsData ->
                settingsData.copy(
                    storySettings = settingsData.storySettings.copy(storyAdherence = originalStoryAdherence)
                )
            }

            _gameState.update { currentState ->
                currentState.copy(
                    playerState = originalPlayerState,
                    threatCounter = originalThreatCounter
                )
            }

            GameLogger.logSystem("=== TEST SONUÇLARI ===")
            GameLogger.logSystem("Senaryo 1 (Yaratıcı Çekirdek): ${if (aiResponse1 != null) "YARATICI OLAY OLDU" else "OLAY OLMADI"}")
            GameLogger.logSystem("Senaryo 2 (Tehdit Modu): ${if (aiResponse2 != null) "TEHDİT OLAYI OLDU" else "OLAY OLMADI"}")
            GameLogger.logSystem("Senaryo 3 (Yüksek Hikaye Bağlılığı): ${if (aiResponse3 != null) "HİKAYE ODAKLI OLAY" else "OLAY OLMADI"}")
            GameLogger.logSystem("Senaryo 4 (Düşük Hikaye Bağlılığı): ${if (aiResponse4 != null) "ARKETİP ODAKLI OLAY" else "OLAY OLMADI"}")

            GameLogger.logSystem("=== DIRECTOR ENGINE - YARATICI ÇEKİRDEK TESTİ END ===")
        }
    }

    // ========================================
    // FAZ 3: HİBRİT ROZET/UNVAN/BAŞARIM SİSTEMİ FONKSİYONLARI
    // ========================================

    /**
     * AI'ın kullanabileceği rozet verme fonksiyonu
     * ContentRegistry veya BadgeEngine'den badgeId ile bir Badge bulur ve oyuncunun activeBadges listesine ekler
     */
    fun grantBadge(badgeId: String) {
        val badge = com.example.isekaikuroshin.engine.BadgeEngine.getDefaultBadges().find { it.id == badgeId }

        if (badge != null) {
            _gameState.update { currentState ->
                val updatedBadges = currentState.activeBadges.toMutableList()

                // Aynı badge'i tekrar eklemeyi önle
                if (updatedBadges.none { it.id == badgeId }) {
                    updatedBadges.add(badge)
                    GameLogger.logSystem("Badge granted: ${badge.name} (${badge.type})")

                    currentState.copy(activeBadges = updatedBadges)
                } else {
                    GameLogger.logSystem("Badge already owned: ${badge.name}")
                    currentState
                }
            }
            saveGameStateToDatabase()
        } else {
            GameLogger.logError("GameStateManager", "Badge not found with ID: $badgeId")
        }
    }

    /**
     * Umbros gibi sistemler için rozet kaldırma fonksiyonu
     * Bir Badge'i oyuncunun listesinden kaldırır
     */
    fun revokeBadge(badgeId: String) {
        _gameState.update { currentState ->
            val updatedBadges = currentState.activeBadges.filter { it.id != badgeId }

            if (updatedBadges.size < currentState.activeBadges.size) {
                GameLogger.logSystem("Badge revoked: $badgeId")
                currentState.copy(activeBadges = updatedBadges)
            } else {
                GameLogger.logSystem("Badge not found to revoke: $badgeId")
                currentState
            }
        }
        saveGameStateToDatabase()
    }

    /**
     * Oyuncunun sahip olduğu unvanları aktifleştirme fonksiyonu
     * Sadece TITLE tipindeki badge'leri aktif unvan olarak ayarlar
     */
    fun equipTitle(titleId: String) {
        val ownedTitles = _gameState.value.activeBadges.filter { it.type == BadgeType.TITLE }
        val targetTitle = ownedTitles.find { it.id == titleId }

        if (targetTitle != null) {
            _gameState.update { currentState ->
                val updatedPlayerState = currentState.playerState.copy(equippedTitleId = titleId)
                GameLogger.logSystem("Title equipped: ${targetTitle.name}")
                currentState.copy(playerState = updatedPlayerState)
            }
            saveGameStateToDatabase()
        } else {
            GameLogger.logError("GameStateManager", "Title not owned or not found: $titleId")
        }
    }

    /**
     * Aktif unvanı kaldırma fonksiyonu
     */
    fun unequipTitle() {
        _gameState.update { currentState ->
            val updatedPlayerState = currentState.playerState.copy(equippedTitleId = null)
            GameLogger.logSystem("Title unequipped")
            currentState.copy(playerState = updatedPlayerState)
        }
        saveGameStateToDatabase()
    }

    // ===== PALADİN'İN İKİLEMİ: KUTSALLIK/KÖTÜLÜK SİSTEMİ =====

    /**
     * Kutsallık puanları ekler. Kutsallık harcandıkça maksimum artır.
     */
    fun addHoliness(amount: Int) {
        if (amount <= 0) return
        _gameState.update { currentState ->
            val playerState = currentState.playerState
            val newHolinessPoints = (playerState.holinessPoints + amount).coerceAtMost(playerState.maxHolinessPoints)
            val updatedPlayerState = playerState.copy(holinessPoints = newHolinessPoints)
            GameLogger.logSystem("Gained $amount holiness points. Total: $newHolinessPoints/${playerState.maxHolinessPoints}")
            currentState.copy(playerState = updatedPlayerState)
        }
        saveGameStateToDatabase()
    }

    /**
     * Kutsallık puanları harcar ve maksimum kutsallığı artırır.
     */
    fun spendHoliness(amount: Int): Boolean {
        if (amount <= 0) return false

        val currentState = _gameState.value
        val playerState = currentState.playerState

        if (playerState.holinessPoints >= amount) {
            _gameState.update { state ->
                val newHolinessPoints = playerState.holinessPoints - amount
                val newMaxHoliness = playerState.maxHolinessPoints + (amount / 10) // Her 10 harcanan puan 1 max artırır
                val updatedPlayerState = playerState.copy(
                    holinessPoints = newHolinessPoints,
                    maxHolinessPoints = newMaxHoliness
                )
                GameLogger.logSystem("Spent $amount holiness points. Remaining: $newHolinessPoints. Max increased to: $newMaxHoliness")
                state.copy(playerState = updatedPlayerState)
            }
            saveGameStateToDatabase()
            return true
        } else {
            GameLogger.logSystem("Not enough holiness points to spend. Needed: $amount, Available: ${playerState.holinessPoints}")
            return false
        }
    }

    /**
     * Maksimum kutsallık puanlarını artırır.
     */
    fun increaseMaxHoliness(amount: Int) {
        if (amount <= 0) return
        _gameState.update { currentState ->
            val playerState = currentState.playerState
            val newMaxHoliness = playerState.maxHolinessPoints + amount
            val updatedPlayerState = playerState.copy(maxHolinessPoints = newMaxHoliness)
            GameLogger.logSystem("Max holiness increased by $amount. New max: $newMaxHoliness")
            currentState.copy(playerState = updatedPlayerState)
        }
        saveGameStateToDatabase()
    }

    /**
     * Kötülük puanları ekler. Sadece aktif eylemlerle kazanılır.
     */
    fun addUnholy(amount: Int) {
        if (amount <= 0) return
        _gameState.update { currentState ->
            val playerState = currentState.playerState
            val newUnholyPoints = (playerState.unholyPoints + amount).coerceAtMost(playerState.maxUnholyPoints)
            val updatedPlayerState = playerState.copy(unholyPoints = newUnholyPoints)
            GameLogger.logSystem("Gained $amount unholy points through dark action. Total: $newUnholyPoints/${playerState.maxUnholyPoints}")
            currentState.copy(playerState = updatedPlayerState)
        }
        saveGameStateToDatabase()
    }

    /**
     * Kötülük puanları harcar.
     */
    fun spendUnholy(amount: Int): Boolean {
        if (amount <= 0) return false

        val currentState = _gameState.value
        val playerState = currentState.playerState

        if (playerState.unholyPoints >= amount) {
            _gameState.update { state ->
                val newUnholyPoints = playerState.unholyPoints - amount
                val updatedPlayerState = playerState.copy(unholyPoints = newUnholyPoints)
                GameLogger.logSystem("Spent $amount unholy points. Remaining: $newUnholyPoints")
                state.copy(playerState = updatedPlayerState)
            }
            saveGameStateToDatabase()
            return true
        } else {
            GameLogger.logSystem("Not enough unholy points to spend. Needed: $amount, Available: ${playerState.unholyPoints}")
            return false
        }
    }

    /**
     * Umbros paktının mevcut olup olmadığını kontrol eder.
     */
    fun isPactAvailable(): Boolean {
        val currentState = _gameState.value
        val playerState = currentState.playerState

        // Pakt için gereksinimler: yeterli kötülük puanı, aktif sözleşme yok, ölüm durumu
        val hasEnoughUnholy = playerState.unholyPoints >= 50
        val hasNoActivePact = currentState.umbrosContract == null
        val isDead = playerState.currentHealth <= 0

        return hasEnoughUnholy && hasNoActivePact && isDead
    }

    /**
     * Oyuncunun ahlaki durumunu değerlendirir (Umbros için).
     */
    fun getMoralBalance(): String {
        val playerState = _gameState.value.playerState
        val holinessRatio = if (playerState.maxHolinessPoints > 0)
            playerState.holinessPoints.toFloat() / playerState.maxHolinessPoints else 0f
        val unholyRatio = if (playerState.maxUnholyPoints > 0)
            playerState.unholyPoints.toFloat() / playerState.maxUnholyPoints else 0f

        return when {
            holinessRatio > 0.7f && unholyRatio < 0.3f -> "Pure Saint"
            holinessRatio > 0.5f && unholyRatio < 0.5f -> "Holy Warrior"
            unholyRatio > 0.7f && holinessRatio < 0.3f -> "Dark Soul"
            unholyRatio > 0.5f && holinessRatio < 0.5f -> "Corrupted"
            else -> "Balanced Soul"
        }
    }

    /**
     * Oyuncunun sahip olduğu aktif unvanı getiren fonksiyon (UI için)
     */
    fun getEquippedTitle(): Badge? {
        val titleId = _gameState.value.playerState.equippedTitleId ?: return null
        return _gameState.value.activeBadges.find { it.id == titleId && it.type == BadgeType.TITLE }
    }

    /**
     * TODO-AI-06: GM Eylem Yürütücüsü - GMResponse'dan gelen komutları işler ve görsel geri bildirim sağlar
     * @param response GameMaster'dan dönen GMResponse nesnesi
     * @param onShowOverlay SystemOverlay gösterme callback'i
     */
    suspend fun executeGMActions(
        response: GMResponse,
        onShowOverlay: (com.example.isekaikuroshin.ui.components.OverlayData) -> Unit
    ) {
        GameLogger.logSystem("=== GM ACTION EXECUTOR BAŞLADI ===")

        // 1. Journal Entry - JournalViewModel zaten mevcut sayfaya ekliyor, burada tekrar eklemeye gerek yok!
        // addStoryPage() yerine sadece log tutuyoruz
        if (response.journalEntry.isNotBlank()) {
            GameLogger.logSystem("GM_ACTION: Journal entry eklendi (JournalViewModel tarafından)")
        }

        // 2. Items Gained - Eşya kazanımı ve görsel geri bildirim
        response.itemsGained.forEach { itemGained ->
            // TODO-FIX-04: itemId null olabilir, kontrol ekle
            val itemId = itemGained.itemId ?: return@forEach

            // Mekanik Adım: Eşyayı envantere ekle
            addItemToInventory(itemId, 1)

            // Görsel Adım: ContentRegistry'den item bilgilerini al
            val registryItem = ContentRegistry.Items.getItem(itemId)

            // Rarity belirleme: AI rarity > ContentRegistry rarity > DEFAULT
            val finalRarity = itemGained.rarity?.let { rarityString ->
                try {
                    ItemRarity.valueOf(rarityString)
                } catch (e: IllegalArgumentException) {
                    GameLogger.logSystem("GM_ACTION: Geçersiz rarity '$rarityString', varsayılana dönülüyor")
                    null
                }
            } ?: registryItem?.rarity ?: ItemRarity.COMMON

            if (registryItem != null) {
                val overlayData = com.example.isekaikuroshin.ui.components.OverlayData.ItemAcquired(
                    title = com.example.isekaikuroshin.data.LanguageManager.getText("new_item_acquired"), // G135.1: Localized
                    itemName = registryItem.name,
                    itemDescription = registryItem.description,
                    itemIcon = android.R.drawable.ic_menu_add,
                    rarity = finalRarity.toCardRarity(),
                    iconEmoji = itemGained.iconEmoji  // G135.3: Pass AI emoji
                )
                onShowOverlay(overlayData)
                GameLogger.logSystem("GM_ACTION: Item kazanıldı (${finalRarity.name}) - ${itemGained.iconEmoji ?: "no emoji"} ${itemGained.itemId}")
            } else {
                // Fallback: Registry'de yoksa AI-generated data kullan
                val overlayData = com.example.isekaikuroshin.ui.components.OverlayData.ItemAcquired(
                    title = com.example.isekaikuroshin.data.LanguageManager.getText("new_item_acquired"), // G135.1: Consistent title
                    itemName = itemGained.name ?: com.example.isekaikuroshin.data.LanguageManager.getText("unknown_item"), // Use AI name
                    itemDescription = itemGained.description ?: com.example.isekaikuroshin.data.LanguageManager.getText("mysterious_item_desc"), // Use AI description
                    itemIcon = android.R.drawable.ic_menu_add,
                    rarity = finalRarity.toCardRarity(),
                    iconEmoji = itemGained.iconEmoji  // G135.3: Pass AI emoji
                )
                onShowOverlay(overlayData)
                GameLogger.logSystem("GM_ACTION: AI-generated item acquired (${finalRarity.name}) - ${itemGained.iconEmoji ?: "no emoji"} ${itemGained.name}")
            }
        }

        // 3. Quests Updated - Görev güncellemeleri ve görsel geri bildirim
        response.questsUpdated.forEach { questId ->
            // Mekanik Adım: ContentRegistry'den quest bilgilerini al
            val registryQuest = ContentRegistry.Quests.getQuest(questId)
            if (registryQuest != null) {
                // Önce quest'in zaten aktif olup olmadığını kontrol et
                val isAlreadyActive = _gameState.value.activeQuests.any { it.id == questId }
                val isCompleted = _gameState.value.completedQuests.any { it.id == questId }

                if (isCompleted) {
                    // Quest zaten tamamlanmış, bir şey yapma
                    GameLogger.logSystem("GM_ACTION: Quest zaten tamamlanmış - $questId")
                } else if (isAlreadyActive) {
                    // Quest aktif, şimdi tamamla
                    completeQuest(questId)

                    // Görsel Adım: Quest tamamlanma overlay göster
                    val overlayData = com.example.isekaikuroshin.ui.components.OverlayData.Quest(
                        title = "Görev Tamamlandı!",
                        questName = registryQuest.title,
                        goals = listOf(registryQuest.description)
                    )
                    onShowOverlay(overlayData)
                    GameLogger.logSystem("GM_ACTION: Quest tamamlandı ve overlay gösterildi - $questId")
                } else {
                    // Quest yeni, aktif quests'e ekle
                    addQuest(registryQuest)

                    // Görsel Adım: Yeni quest overlay göster
                    val overlayData = com.example.isekaikuroshin.ui.components.OverlayData.Quest(
                        title = "Yeni Görev!",
                        questName = registryQuest.title,
                        goals = listOf(registryQuest.description)
                    )
                    onShowOverlay(overlayData)
                    GameLogger.logSystem("GM_ACTION: Yeni quest eklendi ve overlay gösterildi - $questId")
                }
            } else {
                // Fallback: Registry'de yoksa basit görev oluştur
                val fallbackQuest = RegistryQuest(
                    id = questId,
                    title = "Gizemli Görev",
                    description = "GameMaster'dan gelen gizemli bir görev: $questId",
                    giverNpcId = "game_master"
                )
                addQuest(fallbackQuest)

                val overlayData = com.example.isekaikuroshin.ui.components.OverlayData.Quest(
                    title = "Görev Tamamlandı!",
                    questName = fallbackQuest.title,
                    goals = listOf(fallbackQuest.description)
                )
                onShowOverlay(overlayData)
                GameLogger.logSystem("GM_ACTION: Dinamik quest oluşturuldu ve tamamlandı - $questId")
            }
        }

        // 4. Stats Changed - Karakteristik değişiklikleri ve görsel geri bildirim
        response.statsChanged.forEach { (statName, changeAmount) ->
            // Mekanik Adım: Stat'ı güncelle
            when (statName.lowercase()) {
                "strength", "str" -> {
                    _gameState.update { currentState ->
                        val newStrength = currentState.playerState.strength + changeAmount
                        val updatedPlayerState = currentState.playerState.copy(strength = newStrength)
                        val finalPlayerState = recalculateDerivedStats(updatedPlayerState)
                        currentState.copy(playerState = finalPlayerState)
                    }
                }
                "agility", "agi" -> {
                    _gameState.update { currentState ->
                        val newAgility = currentState.playerState.agility + changeAmount
                        val updatedPlayerState = currentState.playerState.copy(agility = newAgility)
                        val finalPlayerState = recalculateDerivedStats(updatedPlayerState)
                        currentState.copy(playerState = finalPlayerState)
                    }
                }
                "intelligence", "int" -> {
                    _gameState.update { currentState ->
                        val newIntelligence = currentState.playerState.intelligence + changeAmount
                        val updatedPlayerState = currentState.playerState.copy(intelligence = newIntelligence)
                        val finalPlayerState = recalculateDerivedStats(updatedPlayerState)
                        currentState.copy(playerState = finalPlayerState)
                    }
                }
                "vitality", "vit" -> {
                    _gameState.update { currentState ->
                        val newVitality = currentState.playerState.vitality + changeAmount
                        val updatedPlayerState = currentState.playerState.copy(vitality = newVitality)
                        val finalPlayerState = recalculateDerivedStats(updatedPlayerState)
                        currentState.copy(playerState = finalPlayerState)
                    }
                }
                "health", "hp" -> {
                    modifyHealth(changeAmount)
                }
                "mana", "mp" -> {
                    modifyMana(changeAmount)
                }
                "gold" -> {
                    updateGold(changeAmount)
                }
                "experience", "exp" -> {
                    addExperience(changeAmount)
                }
                else -> {
                    GameLogger.logSystem("GM_ACTION: Bilinmeyen stat - $statName")
                }
            }

            // Görsel Adım: Stat değişikliği overlay'i göster
            val changeText = if (changeAmount >= 0) "+$changeAmount" else "$changeAmount"
            val overlayData = com.example.isekaikuroshin.ui.components.OverlayData.Alert(
                title = "Karakteristik Değişti",
                message = "$statName: $changeText"
            )
            onShowOverlay(overlayData)
            GameLogger.logSystem("GM_ACTION: Stat değişikliği ve overlay gösterildi - $statName: $changeAmount")
        }

        // ============ ÇEVRE KONTROLLERI (FAZ 1) ============

        // 5. Weather Change - Hava durumu değişiklikleri
        response.weatherChange?.let { weatherString ->
            try {
                val newWeather = Weather.valueOf(weatherString)
                _gameState.update { currentState ->
                    currentState.copy(currentWeather = newWeather)
                }
                GameLogger.logSystem("GM_ACTION: Hava durumu değiştirildi: $weatherString")
            } catch (e: IllegalArgumentException) {
                GameLogger.logSystem("GM_ACTION: Geçersiz hava durumu: $weatherString - atlandı")
            }
        }

        // 6. Time Shift - Zaman dilimi değişiklikleri
        response.timeShift?.let { timeString ->
            try {
                val newTimeOfDay = TimeOfDay.valueOf(timeString)
                _gameState.update { currentState ->
                    currentState.copy(currentTimeOfDay = newTimeOfDay)
                }
                GameLogger.logSystem("GM_ACTION: Zaman dilimi değiştirildi: $timeString")
            } catch (e: IllegalArgumentException) {
                GameLogger.logSystem("GM_ACTION: Geçersiz zaman dilimi: $timeString - atlandı")
            }
        }

        // ============ LOKASYON KONTROLLERI (FAZ 2) ============
        // 7. Locations Unlocked - Yeni lokasyonların kilidini açma
        response.locationsUnlocked.forEach { locationId ->
            discoverLocation(locationId)
            // Görsel Adım: Lokasyon keşif overlay'i göster
            val overlayData = com.example.isekaikuroshin.ui.components.OverlayData.Alert(
                title = "Yeni Lokasyon Keşfedildi!",
                message = "🗺️ $locationId artık haritadan erişilebilir!"
            )
            onShowOverlay(overlayData)
            GameLogger.logSystem("GM_ACTION: Yeni lokasyon kilidi açıldı ve overlay gösterildi - $locationId")
        }

        // 8. Location Change - Oyuncuyu otomatik seyahat ettirme
        response.locationChange?.let { newLocationId ->
            // Önce lokasyonun kilidini aç (gerekirse)
            if (!_gameState.value.knownLocations.any { it.id == newLocationId }) {
                discoverLocation(newLocationId)
            }
            // Sonra oyuncuyu o lokasyona taşı (LOOP FIX: addPageEntry = false, overlay zaten gösterilecek)
            changeLocation(newLocationId, addPageEntry = false)
            // Görsel Adım: Seyahat overlay'i göster
            val overlayData = com.example.isekaikuroshin.ui.components.OverlayData.Alert(
                title = "Otomatik Seyahat",
                message = "📍 Hikaye gereği $newLocationId konumuna götürüldün!"
            )
            onShowOverlay(overlayData)
            GameLogger.logSystem("GM_ACTION: Otomatik seyahat gerçekleştirildi ve overlay gösterildi - $newLocationId")
        }

        // 9. NPC State Change - NPC durumu değişiklikleri (TODO-NEM-03)
        response.npcStateChange?.let { npcChange ->
            // TODO-FIX-04: npcId null olabilir, kontrol ekle
            val npcId = npcChange.npcId ?: return@let

            try {
                val existingNPC = dynamicNPCDao.getNpcStateById(npcId)
                if (existingNPC != null) {
                    GameLogger.logSystem("=== GM_ACTION: NPC STATE CHANGE START ===")
                    GameLogger.logSystem("Target NPC: ${npcChange.npcId}")

                    var updatedNPC: DynamicNPCEntity = existingNPC

                    // Level değişikliği
                    npcChange.levelChange?.let { levelChange ->
                        val newLevel = (updatedNPC.level + levelChange).coerceAtLeast(1)
                        updatedNPC = updatedNPC.copy(level = newLevel)
                        GameLogger.logSystem("NPC level updated: ${updatedNPC.level} (${if (levelChange > 0) "+" else ""}$levelChange)")
                    }

                    // Stat değişiklikleri
                    if (npcChange.statChanges.isNotEmpty()) {
                        var newStrength = updatedNPC.currentStrength
                        var newAgility = updatedNPC.currentAgility
                        var newIntelligence = updatedNPC.currentIntelligence
                        var newVitality = updatedNPC.currentVitality

                        npcChange.statChanges.forEach { (statName, change) ->
                            when (statName.lowercase()) {
                                "strength", "str" -> newStrength = (newStrength + change).coerceAtLeast(1)
                                "agility", "agi" -> newAgility = (newAgility + change).coerceAtLeast(1)
                                "intelligence", "int" -> newIntelligence = (newIntelligence + change).coerceAtLeast(1)
                                "vitality", "vit" -> newVitality = (newVitality + change).coerceAtLeast(1)
                            }
                            GameLogger.logSystem("NPC stat '$statName' changed by $change")
                        }

                        updatedNPC = updatedNPC.copy(
                            currentStrength = newStrength,
                            currentAgility = newAgility,
                            currentIntelligence = newIntelligence,
                            currentVitality = newVitality
                        )
                    }

                    // Loyalty değişikliği
                    npcChange.loyaltyChange?.let { loyaltyChange ->
                        val newLoyalty = (updatedNPC.loyaltyToPlayer + loyaltyChange).coerceIn(-100, 100)
                        updatedNPC = updatedNPC.copy(loyaltyToPlayer = newLoyalty)
                        GameLogger.logSystem("NPC loyalty updated: $newLoyalty (${if (loyaltyChange > 0) "+" else ""}$loyaltyChange)")
                    }

                    // Yeni hafıza ekleme
                    npcChange.memoryAdded?.let { newMemory ->
                        // TODO-FIX-04: NPCMemoryEvent nullable alanları kontrol et
                        if (newMemory.eventType != null && newMemory.eventDescription != null) {
                            val updatedHistory = updatedNPC.historyWithPlayer + newMemory
                            updatedNPC = updatedNPC.copy(historyWithPlayer = updatedHistory)
                            GameLogger.logSystem("NPC memory added: ${newMemory.eventType} - ${newMemory.eventDescription}")
                        } else {
                            GameLogger.logSystem("NPC memory skipped - incomplete data (eventType or description missing)")
                        }
                    }

                    // Mood değişikliği
                    npcChange.moodOverride?.let { newMoodString ->
                        try {
                            val newMood = NPCMood.valueOf(newMoodString)
                            updatedNPC = updatedNPC.copy(currentMood = newMood.name)
                            GameLogger.logSystem("NPC mood updated: ${newMood.name}")
                        } catch (e: IllegalArgumentException) {
                            GameLogger.logSystem("Invalid mood '$newMoodString', ignoring")
                        }
                    }

                    // Kişilik özelliği ekleme
                    npcChange.personalityTraitAdded?.let { newTrait ->
                        if (!updatedNPC.personalityTraits.contains(newTrait)) {
                            val updatedTraits = updatedNPC.personalityTraits + newTrait
                            updatedNPC = updatedNPC.copy(personalityTraits = updatedTraits)
                            GameLogger.logSystem("NPC personality trait added: $newTrait")
                        }
                    }

                    // Lokasyon değişikliği
                    npcChange.locationChange?.let { newLocation ->
                        updatedNPC = updatedNPC.copy(currentLocationId = newLocation)
                        GameLogger.logSystem("NPC location updated: $newLocation")
                    }

                    // ============ ADVANCED FEATURES (TODO-NEM-04) ============
                    // Skills kazanımı
                    if (npcChange.skillsLearned.isNotEmpty()) {
                        val currentSkills = updatedNPC.skills.toMutableList()
                        val newSkills = npcChange.skillsLearned.filter { !currentSkills.contains(it) }
                        currentSkills.addAll(newSkills)
                        updatedNPC = updatedNPC.copy(skills = currentSkills)

                        newSkills.forEach { skill ->
                            GameLogger.logSystem("NPC skill learned: $skill")
                        }
                    }

                    // Titles kazanımı
                    if (npcChange.titlesGained.isNotEmpty()) {
                        val currentTitles = updatedNPC.titles.toMutableList()
                        val newTitles = npcChange.titlesGained.filter { !currentTitles.contains(it) }
                        currentTitles.addAll(newTitles)
                        updatedNPC = updatedNPC.copy(titles = currentTitles)

                        newTitles.forEach { title ->
                            GameLogger.logSystem("NPC title gained: $title")
                        }
                    }

                    // Badges kazanımı
                    if (npcChange.badgesEarned.isNotEmpty()) {
                        val currentBadges = updatedNPC.badges.toMutableList()
                        val newBadges = npcChange.badgesEarned.filter { !currentBadges.contains(it) }
                        currentBadges.addAll(newBadges)
                        updatedNPC = updatedNPC.copy(badges = currentBadges)

                        newBadges.forEach { badge ->
                            GameLogger.logSystem("NPC badge earned: $badge")
                        }
                    }

                    // Güncellenmiş timestamp
                    updatedNPC = updatedNPC.copy(lastUpdateTime = System.currentTimeMillis())

                    // Veritabanına kaydet
                    dynamicNPCDao.updateNpcState(updatedNPC)

                    // Görsel geri bildirim
                    val overlayData = com.example.isekaikuroshin.ui.components.OverlayData.Alert(
                        title = "NPC Durumu Değişti",
                        message = "🎭 ${updatedNPC.npcId} karakterinin durumu GameMaster tarafından güncellendi!"
                    )
                    onShowOverlay(overlayData)

                    GameLogger.logSystem("=== GM_ACTION: NPC STATE CHANGE COMPLETED ===")
                } else {
                    GameLogger.logSystem("GM_ACTION: NPC '${npcChange.npcId}' not found in database")
                }
            } catch (e: Exception) {
                GameLogger.logError("GameStateManager", "Failed to process NPC state change", e)
            }
        }

        // 10. Nemesis Evolution - NPC'yi Nemesis'e çevirme (TODO-NEM-03)
        response.nemesisEvolution?.let { nemesisChange ->
            // TODO-FIX-04: Tüm nullable alanları kontrol et
            val npcId = nemesisChange.npcId ?: return@let
            val newNemesisLevel = nemesisChange.newNemesisLevel ?: return@let
            val vendettaReason = nemesisChange.vendettaReason ?: return@let
            val evolutionDescription = nemesisChange.evolutionDescription ?: ""

            try {
                val existingNPC = dynamicNPCDao.getNpcStateById(npcId)
                if (existingNPC != null) {
                    GameLogger.logSystem("=== GM_ACTION: NEMESIS EVOLUTION START ===")
                    GameLogger.logSystem("Target NPC: $npcId")
                    GameLogger.logSystem("New Nemesis Level: $newNemesisLevel")
                    GameLogger.logSystem("Vendetta Reason: $vendettaReason")

                    // Nemesis seviyesi güncelleme
                    val finalNemesisLevel = newNemesisLevel.coerceIn(0, 5)

                    // Vendetta sebeplerini güncelleme
                    val updatedVendettaReasons = if (!existingNPC.vendettaReasons.contains(vendettaReason)) {
                        existingNPC.vendettaReasons + vendettaReason
                    } else {
                        existingNPC.vendettaReasons
                    }

                    // Adaptasyon özelliklerini ekleme
                    val updatedAdaptationTraits = (existingNPC.adaptationTraits + nemesisChange.adaptationTraits).distinct()

                    // Nemesis evolution memory ekleme
                    val nemesisMemory = NPCMemoryEvent(
                        timestamp = System.currentTimeMillis(),
                        eventType = "NEMESIS_EVOLUTION",
                        eventDescription = evolutionDescription,
                        emotionalImpact = "MAJOR_NEGATIVE"
                    )
                    val updatedHistory = existingNPC.historyWithPlayer + nemesisMemory

                    // Güncellenmiş NPC
                    val updatedNPC = existingNPC.copy(
                        nemesisLevel = finalNemesisLevel,
                        vendettaReasons = updatedVendettaReasons,
                        adaptationTraits = updatedAdaptationTraits,
                        historyWithPlayer = updatedHistory,
                        loyaltyToPlayer = (existingNPC.loyaltyToPlayer - 30).coerceAtLeast(-100), // Nemesis olunca loyalty düşer
                        currentMood = NPCMood.HOSTILE.name,
                        lastUpdateTime = System.currentTimeMillis()
                    )

                    // Veritabanına kaydet
                    dynamicNPCDao.updateNpcState(updatedNPC)

                    // Özel Nemesis overlay
                    val nemesisTitle = when (newNemesisLevel) {
                        1 -> "⚡ Yeni Rakip!"
                        2 -> "💀 Aktif Düşman!"
                        3 -> "🔥 Ciddi Nemesis!"
                        4 -> "💥 Tehlikeli Nemesis!"
                        5 -> "👹 Arch-Nemesis!"
                        else -> "❗ Nemesis Değişimi"
                    }

                    val overlayData = com.example.isekaikuroshin.ui.components.OverlayData.Alert(
                        title = nemesisTitle,
                        message = "${updatedNPC.npcId} artık seviye $newNemesisLevel Nemesis! \n\nSebep: ${nemesisChange.vendettaReason}"
                    )
                    onShowOverlay(overlayData)

                    GameLogger.logSystem("=== GM_ACTION: NEMESIS EVOLUTION COMPLETED ===")
                    GameLogger.logSystem("${updatedNPC.npcId} is now level $newNemesisLevel Nemesis with ${updatedVendettaReasons.size} vendetta reasons")
                } else {
                    GameLogger.logSystem("GM_ACTION: NPC '${nemesisChange.npcId}' not found for nemesis evolution")
                }
            } catch (e: Exception) {
                GameLogger.logError("GameStateManager", "Failed to process nemesis evolution", e)
            }
        }

        // ============ MAKRO OLAYLAR (FAZ 4 - TODO-NEM-09) ============
        // 11. Macro Event - AI güdümlü makro politik olaylar
        response.macroEvent?.let { macroEvent ->
            try {
                GameLogger.logSystem("=== GM_ACTION: MACRO EVENT START ===")
                GameLogger.logSystem("Event Type: ${macroEvent.eventType}")
                GameLogger.logSystem("Affected Factions: ${macroEvent.factionIds}")
                GameLogger.logSystem("Reason: ${macroEvent.reason}")

                when (macroEvent.eventType) {
                    "WAR_DECLARATION" -> {
                        if (macroEvent.factionIds.size >= 2) {
                            val factionA = macroEvent.factionIds[0]
                            val factionB = macroEvent.factionIds[1]

                            // DiplomacyEngine ile savaş başlat
                            com.example.isekaikuroshin.engine.DiplomacyEngine.declareFactionWar(factionA, factionB)

                            // Görsel geri bildirim
                            val overlayData = com.example.isekaikuroshin.ui.components.OverlayData.Alert(
                                title = "🔥 SAVAŞ İLAN EDİLDİ!",
                                message = "💀 $factionA ve $factionB arasında savaş başladı!\n\nSebep: ${macroEvent.reason}"
                            )
                            onShowOverlay(overlayData)

                            // Hikayeye ekle
                            addStoryPage("🔥 DÜNYA SALLANIYOR!\n\n$factionA ve $factionB arasında büyük bir savaş başladı! Dünyada politik dengelerin değiştiğini hissediyorsun.\n\nSavaşın sebebi: ${macroEvent.reason}")

                            GameLogger.logSystem("MACRO_EVENT: Savaş başlatıldı: $factionA vs $factionB")
                        }
                    }
                    "PEACE_TREATY" -> {
                        if (macroEvent.factionIds.size >= 2) {
                            val factionA = macroEvent.factionIds[0]
                            val factionB = macroEvent.factionIds[1]

                            // DiplomacyEngine ile barış yap
                            com.example.isekaikuroshin.engine.DiplomacyEngine.makeFactionPeace(factionA, factionB)

                            // Görsel geri bildirim
                            val overlayData = com.example.isekaikuroshin.ui.components.OverlayData.Alert(
                                title = "🕊️ BARIŞ ANLAŞMASI!",
                                message = "✌️ $factionA ve $factionB barış anlaşması imzaladı!\n\nSebep: ${macroEvent.reason}"
                            )
                            onShowOverlay(overlayData)

                            // Hikayeye ekle
                            addStoryPage("🕊️ BARIŞ ZAMANINDA!\n\n$factionA ve $factionB arasındaki düşmanlık sona erdi. Dünyada barışın hâkim olduğunu hissediyorsun.\n\nBarışın sebebi: ${macroEvent.reason}")

                            GameLogger.logSystem("MACRO_EVENT: Barış yapıldı: $factionA <-> $factionB")
                        }
                    }
                    else -> {
                        GameLogger.logSystem("MACRO_EVENT: Bilinmeyen event type: ${macroEvent.eventType}")
                    }
                }

                GameLogger.logSystem("=== GM_ACTION: MACRO EVENT COMPLETED ===")
            } catch (e: Exception) {
                GameLogger.logError("GameStateManager", "Failed to process macro event", e)
            }
        }

        // Değişiklikleri kaydet
        saveGameStateToDatabase()
        GameLogger.logSystem("=== GM ACTION EXECUTOR TAMAMLANDI ===")
    }

    // ============= STAT ALLOCATION SYSTEM (HYBRID: Level-up + GM Bonus) =============

    /**
     * Kullanıcının stat allocation UI'dan belirli bir stat'a puan ataması
     * @param statName: "strength", "agility", "intelligence", "vitality", "spirit", "luck"
     * @param amount: Atanacak puan miktarı
     * @return Boolean: Başarılı olduysa true
     */
    fun allocateStatPoint(statName: String, amount: Int = 1): Boolean {
        val currentPlayer = _gameState.value.playerState

        if (currentPlayer.statPoints < amount) {
            GameLogger.logSystem("StatAllocation: Not enough unspent points: ${currentPlayer.statPoints}")
            return false
        }

        _gameState.update { currentState ->
            val newPlayer = when (statName.lowercase()) {
                "str", "strength" -> currentPlayer.copy(
                    strength = currentPlayer.strength + amount,
                    statPoints = currentPlayer.statPoints - amount
                )
                "agi", "agility" -> currentPlayer.copy(
                    agility = currentPlayer.agility + amount,
                    statPoints = currentPlayer.statPoints - amount
                )
                "int", "intelligence" -> currentPlayer.copy(
                    intelligence = currentPlayer.intelligence + amount,
                    statPoints = currentPlayer.statPoints - amount
                )
                "vit", "vitality" -> currentPlayer.copy(
                    vitality = currentPlayer.vitality + amount,
                    statPoints = currentPlayer.statPoints - amount
                )
                "spirit" -> currentPlayer.copy(
                    spirit = currentPlayer.spirit + amount,
                    statPoints = currentPlayer.statPoints - amount
                )
                "luck" -> currentPlayer.copy(
                    luck = currentPlayer.luck + amount,
                    statPoints = currentPlayer.statPoints - amount
                )
                else -> {
                    GameLogger.logSystem("StatAllocation: Unknown stat: $statName")
                    return false
                }
            }

            GameLogger.logSystem("StatAllocation: Allocated $amount point(s) to $statName")
            currentState.copy(playerState = newPlayer)
        }

        saveGameStateToDatabase()
        return true
    }

    /**
     * GM'in hikaye-tabanlı bonus stat vermesi (örn: "Ağır kılıcı kaldırmayı başardın! +3 Güç")
     * Bu fonksiyon GameMasterEngine tarafından çağrılabilir
     */
    fun grantBonusStats(
        strength: Float = 0f,
        agility: Float = 0f,
        intelligence: Float = 0f,
        vitality: Float = 0f,
        spirit: Int = 0,
        luck: Int = 0
    ) {
        _gameState.update { currentState ->
            val updatedPlayer = currentState.playerState.copy(
                strength = currentState.playerState.strength + strength,
                agility = currentState.playerState.agility + agility,
                intelligence = currentState.playerState.intelligence + intelligence,
                vitality = currentState.playerState.vitality + vitality,
                spirit = currentState.playerState.spirit + spirit,
                luck = currentState.playerState.luck + luck
            )

            GameLogger.logSystem("StatAllocation: GM granted bonus stats: STR+$strength AGI+$agility INT+$intelligence VIT+$vitality SPI+$spirit LCK+$luck")
            currentState.copy(playerState = updatedPlayer)
        }
        saveGameStateToDatabase()
    }

    /**
     * STRATEJI #3: Memory synthesis loading state (DUMMY - gerçek implementasyon com.example.isekaikuroshin.game.GameStateManager'da)
     * Bu metod JournalViewModel uyumluluğu için buraya eklendi
     */
    fun setMemorySynthesisState(@Suppress("UNUSED_PARAMETER") isActive: Boolean) {
        // Bu GameStateManager (data package) memory synthesis state'i tutmuyor
        // Sadece uyumluluk için boş metod
        GameLogger.logSystem("⚠️ setMemorySynthesisState called on data.GameStateManager (no-op)")
    }

    val isSynthesizingMemory = MutableStateFlow(false).asStateFlow()

    // ========================================
    // KADIM MÜHÜR TEKNİKLERİ - SEAL MANAGEMENT
    // ========================================

    /**
     * Unlock a new seal for the player
     */
    fun unlockSeal(seal: Seal) {
        _gameState.update { currentState ->
            // Check if already unlocked
            if (currentState.unlockedSeals.any { it.id == seal.id }) {
                GameLogger.logSealPractice("Seal already unlocked: ${seal.nameKey}")
                return@update currentState
            }

            GameLogger.logSealPractice("✅ Unlocking new seal: ${seal.nameKey} (Difficulty: ${seal.difficulty}, MinLevel: ${seal.minPlayerLevel})")
            currentState.copy(
                unlockedSeals = currentState.unlockedSeals + seal
            )
        }
        saveGameStateToDatabase()
    }

    /**
     * Update seal mastery level
     *
     * @param sealId Seal ID
     * @param masteryIncrease Amount to increase (can be negative)
     */
    fun updateSealMastery(sealId: String, masteryIncrease: Int) {
        _gameState.update { currentState ->
            val updatedSeals = currentState.unlockedSeals.map { seal ->
                if (seal.id == sealId) {
                    val oldMastery = seal.masteryLevel
                    val newMastery = (seal.masteryLevel + masteryIncrease).coerceIn(0, 100)
                    GameLogger.logSealPractice("Mastery update: ${seal.nameKey} -> $newMastery% (from $oldMastery%, +$masteryIncrease)")

                    // Check if mastered (100%)
                    if (newMastery >= 100 && seal.masteryLevel < 100) {
                        GameLogger.logSealPractice("🎉 SEAL MASTERED: ${seal.nameKey} - Related skills will be unlocked")
                    }

                    seal.copy(masteryLevel = newMastery)
                } else {
                    seal
                }
            }

            currentState.copy(unlockedSeals = updatedSeals)
        }
        saveGameStateToDatabase()
    }

    /**
     * Get seal by ID
     */
    fun getSealById(sealId: String): Seal? {
        return _gameState.value.unlockedSeals.find { it.id == sealId }
    }

    /**
     * Update entire seal object (for calibration changes)
     */
    fun updateSeal(updatedSeal: Seal) {
        _gameState.update { currentState ->
            val updatedSeals = currentState.unlockedSeals.map { seal ->
                if (seal.id == updatedSeal.id) {
                    updatedSeal
                } else {
                    seal
                }
            }
            currentState.copy(unlockedSeals = updatedSeals)
        }
        saveGameStateToDatabase()
        GameLogger.logSealPractice("Updated seal: ${updatedSeal.nameKey}")
    }

    /**
     * Get all unlocked seals
     */
    fun getUnlockedSeals(): List<Seal> {
        return _gameState.value.unlockedSeals
    }

    /**
     * Get mastered seals (masteryLevel >= 100)
     */
    fun getMasteredSeals(): List<Seal> {
        return _gameState.value.unlockedSeals.filter { it.masteryLevel >= 100 }
    }

    /**
     * Update seal practice metrics
     */
    fun updateSealPracticeMetrics(sealId: String, metrics: PracticeMetrics) {
        _gameState.update { currentState ->
            val updatedSeals = currentState.unlockedSeals.map { seal ->
                if (seal.id == sealId) {
                    seal.copy(practiceMetrics = metrics)
                } else {
                    seal
                }
            }

            currentState.copy(unlockedSeals = updatedSeals)
        }
        saveGameStateToDatabase()
    }

    /**
     * Record seal practice attempt
     *
     * Updates practice metrics based on gesture result
     */
    fun recordSealPracticeAttempt(
        sealId: String,
        wasSuccessful: Boolean,
        wasPerfect: Boolean,
        accuracy: Float
    ) {
        _gameState.update { currentState ->
            val updatedSeals = currentState.unlockedSeals.map { seal ->
                if (seal.id == sealId) {
                    val metrics = seal.practiceMetrics
                    val updatedMetrics = metrics.copy(
                        totalAttempts = metrics.totalAttempts + 1,
                        successfulExecutions = if (wasSuccessful) metrics.successfulExecutions + 1 else metrics.successfulExecutions,
                        perfectExecutions = if (wasPerfect) metrics.perfectExecutions + 1 else metrics.perfectExecutions,
                        failedAttempts = if (!wasSuccessful) metrics.failedAttempts + 1 else metrics.failedAttempts,
                        lastPracticeTimestamp = System.currentTimeMillis(),
                        bestAccuracy = maxOf(metrics.bestAccuracy, accuracy),
                        avgAccuracy = if (metrics.totalAttempts > 0) {
                            (metrics.avgAccuracy * metrics.totalAttempts + accuracy) / (metrics.totalAttempts + 1)
                        } else {
                            accuracy
                        }
                    )

                    val result = when {
                        wasPerfect -> "✨ PERFECT"
                        wasSuccessful -> "✓ SUCCESS"
                        else -> "✗ FAILED"
                    }
                    GameLogger.logSealPractice("Practice attempt: ${seal.nameKey} - $result (Accuracy: ${(accuracy*100).toInt()}%, Total: ${updatedMetrics.totalAttempts}, Success Rate: ${if(updatedMetrics.totalAttempts > 0) (updatedMetrics.successfulExecutions*100/updatedMetrics.totalAttempts) else 0}%)")
                    seal.copy(practiceMetrics = updatedMetrics)
                } else {
                    seal
                }
            }

            currentState.copy(unlockedSeals = updatedSeals)
        }
        saveGameStateToDatabase()
    }

    // ========================================
    // SKILL MANAGEMENT (Kadim Mühür ile Skill Linking için)
    // ========================================

    /**
     * Skill ID'sine göre skill döndür
     */
    fun getSkillById(skillId: String): Skill? {
        return _gameState.value.activeSkills.firstOrNull { it.id == skillId }
    }

    /**
     * Skill'i unlock et (yeni skill ekle)
     */
    fun unlockSkill(skillId: String) {
        _gameState.update { currentState ->
            // Eğer skill zaten unlocked ise, bir şey yapma
            if (currentState.activeSkills.any { it.id == skillId }) {
                GameLogger.logSystem("[SKILL] Skill already unlocked: $skillId")
                return@update currentState
            }

            // Yeni skill oluştur (TODO: Skill library'den çekebiliriz)
            val newSkill = createDefaultSkill(skillId)
            if (newSkill == null) {
                GameLogger.logSystem("[SKILL] Unknown skill ID: $skillId")
                return@update currentState
            }

            GameLogger.logSystem("[SKILL] Unlocked: ${newSkill.name}")
            currentState.copy(
                activeSkills = currentState.activeSkills + newSkill
            )
        }
        saveGameStateToDatabase()
    }

    /**
     * Default skill oluştur (basit implementasyon)
     * TODO: Gerçek skill library entegrasyonu
     */
    private fun createDefaultSkill(skillId: String): Skill? {
        return when (skillId) {
            "skill_fireball_basic" -> Skill(
                id = "skill_fireball_basic",
                name = "Ateş Topu",
                description = "Temel ateş elementi saldırısı",
                elementType = ElementType.FIRE,
                tier = "Temel",
                manaCost = 10,
                baseCooldown = 3,
                diceModifier = 2,
                linkedSealId = "seal_fire_basic"
            )
            "skill_water_shield" -> Skill(
                id = "skill_water_shield",
                name = "Su Kalkanı",
                description = "Su elementi ile savunma kalkanı oluşturur",
                elementType = ElementType.WATER,
                tier = "Temel",
                manaCost = 15,
                baseCooldown = 5,
                diceModifier = 1,
                linkedSealId = "seal_water_basic"
            )
            "skill_stone_armor" -> Skill(
                id = "skill_stone_armor",
                name = "Taş Zırh",
                description = "Toprak elementi ile geçici zırh oluşturur",
                elementType = ElementType.EARTH,
                tier = "Temel",
                manaCost = 20,
                baseCooldown = 10,
                diceModifier = 0,
                linkedSealId = "seal_earth_basic"
            )
            "skill_wind_slash" -> Skill(
                id = "skill_wind_slash",
                name = "Rüzgar Kılıcı",
                description = "Keskin rüzgar elementi saldırısı",
                elementType = ElementType.AIR,
                tier = "Temel",
                manaCost = 12,
                baseCooldown = 4,
                diceModifier = 3,
                linkedSealId = "seal_wind_basic"
            )
            "skill_lightning_bolt" -> Skill(
                id = "skill_lightning_bolt",
                name = "Şimşek Yıldırımı",
                description = "Güçlü şimşek elementi saldırısı",
                elementType = ElementType.LIGHTNING,
                tier = "Temel",
                manaCost = 25,
                baseCooldown = 8,
                diceModifier = 4,
                linkedSealId = "seal_lightning_basic"
            )
            else -> null
        }
    }

    /**
     * Tüm unlocked skills'i döndür
     */
    fun getUnlockedSkills(): List<Skill> {
        return _gameState.value.activeSkills
    }

    /**
     * Mühür ile ilişkili skills'i döndür
     */
    fun getSkillsLinkedToSeal(sealId: String): List<Skill> {
        return _gameState.value.activeSkills.filter { it.linkedSealId == sealId }
    }

    // ========================================
    // SEAL INITIALIZATION (İlk başlatma için seed data)
    // ========================================

    /**
     * Default seals'ları oyuna yükle (ilk kez çalıştırıldığında)
     * Player seviyesine göre uygun seals unlock edilir
     */
    fun initializeDefaultSeals() {
        val currentSeals = _gameState.value.unlockedSeals

        // Eğer zaten seal varsa, tekrar yükleme
        if (currentSeals.isNotEmpty()) {
            GameLogger.logSealPractice("Seals already initialized (${currentSeals.size} seals)")
            return
        }

        val playerLevel = _gameState.value.playerState.level
        val defaultSeals = DefaultSeals.getAllDefaultSeals()

        // Player seviyesine uygun seals'ları unlock et
        val sealsToUnlock = defaultSeals.filter { it.minPlayerLevel <= playerLevel }

        GameLogger.logSealPractice("Initializing ${sealsToUnlock.size} default seals for player level $playerLevel")

        _gameState.update { currentState ->
            currentState.copy(unlockedSeals = sealsToUnlock)
        }

        // Her seal'ı detaylı logla
        sealsToUnlock.forEach { seal ->
            GameLogger.logSealPractice("  ✓ Unlocked Seal: ${seal.nameKey} (ID: ${seal.id}, MinLevel: ${seal.minPlayerLevel})")
        }

        saveGameStateToDatabase()
    }

    /**
     * Belirli bir seal'ı seed data olarak yükle (test/debug için)
     */
    fun seedSeal(sealId: String) {
        val seal = DefaultSeals.getSealById(sealId)
        if (seal != null) {
            unlockSeal(seal)
            GameLogger.logSystem("[SEAL] Seeded seal: ${seal.nameKey}")
        }
    }

    // ============================
    // GÖREV N - FAZ 4: LEARNED SPELL YÖNETİMİ
    // ============================

    /**
     * Yeni bir LearnedSpell ekle
     */
    fun addLearnedSpell(spell: com.example.isekaikuroshin.data.combat.LearnedSpell) {
        scope.launch {
            // PersistentDataManager'a kaydet
            val currentData = PersistentDataManager.gameData.value
            val exists = currentData.playerData.learnedSpells.any { it.recipeId == spell.recipeId }

            if (exists) {
                GameLogger.logSystem("[SPELL] Büyü zaten öğrenilmiş: ${spell.customName}")
                return@launch
            }

            PersistentDataManager.updateGameData { data ->
                val updatedPlayerData = data.playerData.copy(
                    learnedSpells = data.playerData.learnedSpells + spell
                )
                data.copy(playerData = updatedPlayerData)
            }

            GameLogger.logSystem("[SPELL] Yeni büyü öğrenildi: ${spell.customName} (ID: ${spell.id})")
        }
    }

    /**
     * LearnedSpell'i güncelle (skill level artırma, usage count, etc.)
     */
    fun updateLearnedSpell(spellId: String, updater: (com.example.isekaikuroshin.data.combat.LearnedSpell) -> com.example.isekaikuroshin.data.combat.LearnedSpell) {
        scope.launch {
            PersistentDataManager.updateGameData { data ->
                val updatedLearnedSpells = data.playerData.learnedSpells.map { spell ->
                    if (spell.id == spellId) {
                        updater(spell)
                    } else {
                        spell
                    }
                }

                val updatedPlayerData = data.playerData.copy(
                    learnedSpells = updatedLearnedSpells
                )
                data.copy(playerData = updatedPlayerData)
            }
        }
    }

    /**
     * Büyü kullanım sayısını artır (savaşta kullanıldığında)
     */
    fun incrementSpellCombatUsage(spellId: String) {
        updateLearnedSpell(spellId) { spell ->
            spell.copy(combatUsageCount = spell.combatUsageCount + 1)
        }
        GameLogger.logSystem("[SPELL] Combat usage incremented for spell: $spellId")
    }

    /**
     * FAZ 7: Büyü pratik sayısını artır ve XP kazan (Spell Studio'da pratik yapınca)
     * Her pratik +10 XP, 100 XP = +1 Skill Tree Level
     */
    fun incrementSpellPracticeCount(spellId: String): Boolean {
        var didLevelUp = false
        updateLearnedSpell(spellId) { spell ->
            val oldLevel = spell.skillTreeLevel
            val updatedSpell = spell.addPracticeWithXP(xpGained = 10)
            didLevelUp = updatedSpell.skillTreeLevel > oldLevel

            if (didLevelUp) {
                GameLogger.logSystem("[SPELL] ⭐ LEVEL UP! $spellId: Lv${oldLevel} → Lv${updatedSpell.skillTreeLevel}")
            } else {
                GameLogger.logSystem("[SPELL] Practice +10 XP for $spellId (${updatedSpell.currentXP}/100 XP)")
            }
            updatedSpell
        }
        return didLevelUp
    }

    /**
     * FAZ 9.2: Düşman HP Güncelleme
     * @return Düşman öldü mü? (true/false)
     */
    fun updateEnemyHp(enemyName: String, damage: Int): Boolean {
        var enemyDefeated = false
        _gameState.update { currentState ->
            val updatedEnemies = currentState.currentEnemies.map { enemy ->
                if (enemy.name == enemyName) {
                    val newHp = (enemy.hp - damage).coerceAtLeast(0)
                    enemyDefeated = newHp <= 0

                    GameLogger.logSystem("[COMBAT] $enemyName took $damage damage! HP: ${enemy.hp} → $newHp")

                    if (enemyDefeated) {
                        GameLogger.logSystem("[COMBAT] 💀 $enemyName has been defeated!")
                    }

                    enemy.copy(hp = newHp)
                } else {
                    enemy
                }
            }

            currentState.copy(currentEnemies = updatedEnemies)
        }
        saveGameStateToDatabase()
        return enemyDefeated
    }

    // REMOVED: Duplicate consumeCombatStamina() - Using G97 version at line 1057
    // Old StaminaSystem wrapper removed to avoid conflict

    /**
     * TODO-G112.5: Fatigue Penalties - Yorgunluk durumu için accessor
     */
    fun getFatigueStatus(): String = staminaSystem.getFatigueStatus()
    fun getFatigueMultiplier(): Float = staminaSystem.getFatigueMultiplier()

    /**
     * TODO-G112.6: Rest Mechanics - Dinlenme/uyku sistemi
     * Oyuncu dinlendiğinde stamina/fatigue restore eder
     */
    fun performRest(hours: Int = 1) {
        val staminaRecovery = hours * 20 // Saat başına 20 stamina
        val fatigueReduction = hours * 15 // Saat başına 15 fatigue azalması
        staminaSystem.rest(staminaRecovery, fatigueReduction)
        GameLogger.logSystem("[REST] ${hours}h rest: +$staminaRecovery stamina, -$fatigueReduction fatigue")
    }

    /**
     * FAZ 9.2: Ölen Düşmanı Listeden Çıkar
     */
    fun removeDefeatedEnemies() {
        _gameState.update { currentState ->
            val aliveEnemies = currentState.currentEnemies.filter { it.hp > 0 }

            if (aliveEnemies.isEmpty()) {
                GameLogger.logSystem("[COMBAT] ✅ All enemies defeated! Combat ends.")
            }

            currentState.copy(currentEnemies = aliveEnemies)
        }
        saveGameStateToDatabase()
    }

    /**
     * FAZ 9.2: Savaş Durumunu Güncelle
     */
    fun updateCombatStatus(inCombat: Boolean) {
        _gameState.update { it.copy(inCombat = inCombat) }
        saveGameStateToDatabase()
        GameLogger.logSystem("[COMBAT] Combat status: $inCombat")
    }

    /**
     * TODO-G63.1: Reset game to initial state (full reset - like new game)
     * Called by DeathManager when player dies and chooses full reset
     */
    fun resetGame() {
        GameLogger.logSystem("[G63.1] resetGame() - Full game reset")
        _gameState.value = GameStateZ7()
        saveGameStateToDatabase()
    }

    /**
     * TODO-G63.1: Handle player death with penalties (respawn system)
     *
     * Death penalties:
     * - XP loss: 10% of current XP
     * - Gold loss: 20% of current gold
     * - Respawn at safe location (FOREST_CAMP)
     * - Death counter increment
     *
     * @param deathCause Localization key for death cause (e.g., "death_cause_combat")
     * @return Pair of (xpLoss, goldLoss) for G62 combat history recording
     */
    fun handleDeath(deathCause: String): Pair<Int, Int> {
        val currentState = _gameState.value

        GameLogger.logSystem("[G63.1] handleDeath() - Cause: $deathCause")
        GameLogger.logSystem("[G63.1] Before death - XP: ${currentState.playerState.experience}, Gold: ${currentState.playerState.gold}, Deaths: ${currentState.totalDeaths}")

        // Calculate penalties
        val xpLoss = (currentState.playerState.experience * 0.10).toInt()
        val goldLoss = (currentState.playerState.gold * 0.20).toInt()

        val newPlayerState = currentState.playerState.copy(
            experience = maxOf(0, currentState.playerState.experience - xpLoss),
            gold = maxOf(0, currentState.playerState.gold - goldLoss),
            currentHealth = currentState.playerState.maxHealth, // Full heal on respawn
            currentMana = currentState.playerState.maxMana // Full mana on respawn
        )

        // Update state with penalties
        _gameState.value = currentState.copy(
            playerState = newPlayerState,
            totalDeaths = currentState.totalDeaths + 1,
            lastDeathCause = deathCause, // KURAL 9: Localization key
            lastDeathTimestamp = System.currentTimeMillis(),
            currentLocationId = WorldLocations.FOREST_CAMP.id, // Respawn at safe zone
            inCombat = false, // Exit combat
            currentEnemies = emptyList()
        )

        GameLogger.logSystem("[G63.1] Death penalties applied - XP lost: $xpLoss, Gold lost: $goldLoss")
        GameLogger.logSystem("[G63.1] After death - XP: ${newPlayerState.experience}, Gold: ${newPlayerState.gold}, Total Deaths: ${currentState.totalDeaths + 1}")
        GameLogger.logSystem("[G63.1] Respawned at: ${WorldLocations.FOREST_CAMP.id}")

        saveGameStateToDatabase()

        // G62: Return penalties for combat history recording
        return Pair(xpLoss, goldLoss)
    }

    /**
     * TODO-G62: Record combat victory in history
     * @param enemiesKilled Number of enemies killed
     * @param xpGained XP reward
     * @param goldGained Gold reward
     */
    fun recordCombatVictory(enemiesKilled: Int, xpGained: Int, goldGained: Int) {
        val currentState = _gameState.value

        val entry = CombatHistoryEntry(
            timestamp = System.currentTimeMillis(),
            result = "combat_result_victory", // KURAL 9: Localization key
            enemiesKilled = enemiesKilled,
            xpGained = xpGained,
            goldGained = goldGained,
            xpLost = 0,
            goldLost = 0,
            playerLevel = currentState.playerState.level,
            locationId = currentState.currentLocationId
        )

        _gameState.value = currentState.copy(
            combatHistory = currentState.combatHistory + entry,
            totalCombatsWon = currentState.totalCombatsWon + 1,
            totalEnemiesKilled = currentState.totalEnemiesKilled + enemiesKilled
        )

        GameLogger.logSystem("[G62] Combat victory recorded - Enemies: $enemiesKilled, XP: +$xpGained, Gold: +$goldGained")
        GameLogger.logSystem("[G62] Total stats - Wins: ${currentState.totalCombatsWon + 1}, Kills: ${currentState.totalEnemiesKilled + enemiesKilled}")

        saveGameStateToDatabase()
    }

    /**
     * TODO-G62: Record combat defeat in history
     * @param xpLost XP penalty (from handleDeath)
     * @param goldLost Gold penalty (from handleDeath)
     */
    fun recordCombatDefeat(xpLost: Int, goldLost: Int) {
        val currentState = _gameState.value

        val entry = CombatHistoryEntry(
            timestamp = System.currentTimeMillis(),
            result = "combat_result_defeat", // KURAL 9: Localization key
            enemiesKilled = 0,
            xpGained = 0,
            goldGained = 0,
            xpLost = xpLost,
            goldLost = goldLost,
            playerLevel = currentState.playerState.level,
            locationId = currentState.currentLocationId
        )

        _gameState.value = currentState.copy(
            combatHistory = currentState.combatHistory + entry,
            totalCombatsLost = currentState.totalCombatsLost + 1
        )

        GameLogger.logSystem("[G62] Combat defeat recorded - XP: -$xpLost, Gold: -$goldLost")
        GameLogger.logSystem("[G62] Total stats - Losses: ${currentState.totalCombatsLost + 1}")

        saveGameStateToDatabase()
    }
}

/**
 * G93: Data class for player stats (combat calculations)
 */
data class PlayerStats(
    val hp: Int,
    val maxHp: Int,
    val mp: Int,
    val maxMp: Int,
    val attack: Int,
    val defense: Int,
    val speed: Int
)

@Serializable
// FIX-TASK-2.2: displayName değerleri İngilizce olmalı - UI katmanı çevirecek
enum class TimeOfDay(
    val displayName: String,
    val statEffects: Map<com.example.isekaikuroshin.engine.StatType, Float>
) {
    MORNING(
        displayName = "Morning",
        statEffects = mapOf(com.example.isekaikuroshin.engine.StatType.PER_PERCENT to 0.05f)
    ),
    NOON(
        displayName = "Noon",
        statEffects = emptyMap()
    ),
    AFTERNOON(
        displayName = "Afternoon",
        statEffects = emptyMap()
    ),
    EVENING(
        displayName = "Evening",
        statEffects = mapOf(com.example.isekaikuroshin.engine.StatType.PER_PERCENT to -0.05f)
    ),
    NIGHT(
        displayName = "Night",
        statEffects = mapOf(
            com.example.isekaikuroshin.engine.StatType.AGI_PERCENT to 0.05f,
            com.example.isekaikuroshin.engine.StatType.PER_PERCENT to -0.10f
        )
    );
}