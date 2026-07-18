package com.example.isekaikuroshin.engine

import com.example.isekaikuroshin.ai.GlobalAIManager
import com.example.isekaikuroshin.data.GameStateManager
import com.example.isekaikuroshin.data.database.DynamicNPCDao
import com.example.isekaikuroshin.data.database.DynamicNPCEntity
import com.example.isekaikuroshin.data.database.FactionDao
import com.example.isekaikuroshin.data.database.SettlementDao
import com.example.isekaikuroshin.data.database.toDynamicNPCState
import com.example.isekaikuroshin.data.database.toEntity
import com.example.isekaikuroshin.data.database.toFactionState
import com.example.isekaikuroshin.data.database.toSettlementState
import com.example.isekaikuroshin.data.npc.DynamicNPCState
import com.example.isekaikuroshin.data.npc.NPCMood
import com.example.isekaikuroshin.data.world.SettlementState
import com.example.isekaikuroshin.data.world.EconomyLevel
import com.example.isekaikuroshin.data.LocationType
import com.example.isekaikuroshin.utils.GameLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * World Update Engine - Nemesis Sistemi'nin kalbinde yer alan NPC evolüsyon motoru
 *
 * Bu motor, oyun dünyasında zaman ilerledikçe tüm NPC'lerin dinamik olarak gelişmesini,
 * seviye atlamasını, stat artışını ve Nemesis transformasyonlarını yönetir.
 *
 * TODO-NEM-02 kapsamında oluşturulan bu prototip, zamanla daha karmaşık evrim
 * algoritmaları ve AI-driven karakterizasyon sistemleri içerecek şekilde genişletilecektir.
 */
@Singleton
class WorldUpdateEngine @Inject constructor(
    private val gameStateManager: GameStateManager,
    private val dynamicNpcDao: DynamicNPCDao,
    private val settlementDao: SettlementDao,
    private val factionDao: FactionDao,
    private val macroEventEngine: MacroEventEngine
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val worldUpdateMutex = Mutex() // Eşzamanlı dünya güncellemelerini önlemek için

    companion object {
        private const val TAG = "WorldUpdateEngine"
        private const val WORLD_TICK_INTERVAL = 3 // Her 3 günde bir World Tick
        private const val EVOLUTION_POINTS_THRESHOLD = 10 // Level atlamak için gereken puan
        private const val MIN_STAT_INCREASE = 1 // Minimum stat artışı
        private const val MAX_STAT_INCREASE = 3 // Maksimum stat artışı
    }

    /**
     * Gün geçişi olayında çağrılır - GameStateManager'dan gelecek tetikleyici
     * Her günün sonunda NPC dünyasının evrimini kontrol eder
     *
     * @param completedDay Tamamlanan günün numarası (memory synthesis için)
     */
    fun onDayPassed(completedDay: Int = gameStateManager.gameState.value.currentDay) {
        GameLogger.logSystem("$TAG: Gün $completedDay tamamlandı - Dünya durumu kontrol ediliyor...")

        // === STRATEJI #3: END-OF-DAY MEMORY SYNTHESIS ===
        // TODO-G132: FIX RACE CONDITION - JournalViewModel'den son mesajların gelmesini bekle
        // GÜN GEÇİŞİNİ BLOKE ET - Özet tamamlanana kadar bekle!
        // NOT: UI loading state artık MemoryManager içinde yönetiliyor
        scope.launch {
            try {
                GameLogger.logSystem("[MEMORY] 🌙 End-of-Day $completedDay - Starting memory synthesis...")
                GameLogger.logSystem("[MEMORY] ⏸️ Waiting 200ms for final dialogues to be added...")

                // G132 FIX: JournalViewModel'in son mesajları eklemesi için kısa bekleme
                kotlinx.coroutines.delay(200)

                GameLogger.logSystem("[MEMORY] ⏸️ Day transition BLOCKED until synthesis completes")

                // Özet yapılıyor - Bu tamamlanana kadar gün geçişi yapılmaz
                // MemoryManager içinde UI loading state yönetimi yapılıyor
                GlobalAIManager.synthesizeEndOfDayMemory(completedDay)

                GameLogger.logSystem("[MEMORY] ✅ Memory synthesis completed for Day $completedDay")
                GameLogger.logSystem("[MEMORY] ▶️ Day transition now allowed")

            } catch (e: Exception) {
                GameLogger.logError(TAG, "Memory synthesis failed for Day $completedDay", e)
            }
        }

        // Her WORLD_TICK_INTERVAL günde bir büyük dünya güncellemesi
        if (completedDay % WORLD_TICK_INTERVAL == 0) {
            GameLogger.logSystem("$TAG: === WORLD TICK EVENT === (Gün: $completedDay)")

            // TEST-LOG: 3 günlük sistem tracking
            GameLogger.logSystem("[TEST-3DAY] ========================================")
            GameLogger.logSystem("[TEST-3DAY] WORLD TICK TRIGGERED ON DAY $completedDay")
            GameLogger.logSystem("[TEST-3DAY] This is a major world update cycle")
            GameLogger.logSystem("[TEST-3DAY] All NPCs will evolve, settlements update")
            GameLogger.logSystem("[TEST-3DAY] ========================================")

            // FIX #47: 3-DAY MASTER SUMMARY (FORCED GOOGLE API)
            scope.launch {
                try {
                    GameLogger.logSystem("[MEMORY] 🌟 3-Day Master Summary - Starting synthesis for Day $completedDay...")
                    GameLogger.logSystem("[MEMORY] 🔒 FORCED GOOGLE API - Ignoring user Settings")

                    GlobalAIManager.synthesizeThreeDayMasterSummary(completedDay)

                    GameLogger.logSystem("[MEMORY] ✅ 3-Day Master Summary completed for Day $completedDay")
                } catch (e: Exception) {
                    GameLogger.logError(TAG, "3-Day Master Summary failed for Day $completedDay", e)
                }

                // Continue with world tick after summary
                triggerWorldTickEvent()
            }
        }

        // 7 günlük makro event kontrolü
        if (completedDay % 7 == 0) {
            GameLogger.logSystem("[TEST-7DAY] ========================================")
            GameLogger.logSystem("[TEST-7DAY] WEEKLY MACRO EVENT TRIGGERED ON DAY $completedDay")
            GameLogger.logSystem("[TEST-7DAY] This is a major narrative event cycle")
            GameLogger.logSystem("[TEST-7DAY] ========================================")
        }

        // Her gün basit evolüsyon puanı artışı
        scope.launch {
            dailyEvolutionPointsIncrease()
        }
    }

    /**
     * Büyük dünya güncellemesi - NPC'lerin kapsamlı evrimini tetikler
     */
    private suspend fun triggerWorldTickEvent() {
        worldUpdateMutex.withLock {
            try {
                GameLogger.logSystem("$TAG: WORLD TICK başlatılıyor - Tüm NPC'ler evolve ediliyor...")

                // Tüm dinamik NPC'leri veritabanından çek
                val allNpcs = dynamicNpcDao.getAllNpcStates()
                GameLogger.logSystem("$TAG: Toplam ${allNpcs.size} dinamik NPC bulundu")

                var evolvedCount = 0
                var levelUpCount = 0

                // Her NPC için evolüsyon işlemi
                allNpcs.forEach { npcEntity ->
                    val originalState = npcEntity.toDynamicNPCState()
                    val evolvedState = evolveNPC(originalState)

                    // Değişiklik varsa veritabanını güncelle
                    if (evolvedState != originalState) {
                        dynamicNpcDao.updateNpcState(evolvedState.toEntity())
                        evolvedCount++

                        // Level atlama kontrolü
                        if (evolvedState.level > originalState.level) {
                            levelUpCount++
                            GameLogger.logSystem("$TAG: " +
                                "🎖️ NPC ${evolvedState.npcId} seviye atladı! " +
                                "${originalState.level} → ${evolvedState.level}")
                        }
                    }
                }

                // TODO-NEM-05: Settlement'ları da güncelle
                updateSettlementEconomics()

                // TODO-NEM-07: Fraksiyon diplomasilerini güncelle
                updateFactionDiplomacy()

                // TODO-NEM-08: Oyuncu etkilerini uygula (sistemik etkileşimler)
                applyPlayerSystemicEffects()

                // TODO-NEM-09: Her 7 günde bir makro olayları kontrol et
                val currentDay = gameStateManager.gameState.value.currentDay
                if (currentDay % 7 == 0) {
                    GameLogger.logSystem("$TAG: Makro olaylar kontrol ediliyor (Gün: $currentDay)")

                    // TEST-LOG: 7 günlük özel olay sistemi
                    GameLogger.logSystem("[TEST-7DAY] ========================================")
                    GameLogger.logSystem("[TEST-7DAY] WEEKLY MACRO EVENT CHECK ON DAY $currentDay")
                    GameLogger.logSystem("[TEST-7DAY] This is a seasonal/weekly event trigger")
                    GameLogger.logSystem("[TEST-7DAY] MacroEventEngine will consider major events")
                    GameLogger.logSystem("[TEST-7DAY] ========================================")

                    macroEventEngine.considerTriggeringMacroEvent()
                }

                GameLogger.logSystem("$TAG: " +
                    "WORLD TICK tamamlandı: $evolvedCount NPC gelişti, $levelUpCount seviye atladı")

            } catch (e: Exception) {
                GameLogger.logSystem("$TAG: WORLD TICK hatası: ${e.message}")
            }
        }
    }

    /**
     * Günlük evolüsyon puanı artışı - Daha az maliyetli rutin güncelleme
     */
    private suspend fun dailyEvolutionPointsIncrease() {
        try {
            // Sadece canlı NPC'leri güncelle
            val aliveNpcs = dynamicNpcDao.getAllAliveNpcs()

            aliveNpcs.forEach { npcEntity ->
                val currentPoints = npcEntity.evolutionPoints
                val newPoints = currentPoints + 1 // Her gün 1 puan

                // Evolution points güncellemesi
                dynamicNpcDao.updateNpcState(
                    npcEntity.copy(
                        evolutionPoints = newPoints,
                        lastUpdateTime = System.currentTimeMillis()
                    )
                )
            }

            GameLogger.logSystem("$TAG: ${aliveNpcs.size} NPC için günlük evolüsyon puanı artırıldı")

        } catch (e: Exception) {
            GameLogger.logSystem("$TAG: Günlük evolüsyon güncellemesi hatası: ${e.message}")
        }
    }

    /**
     * Ana NPC evolüsyon algoritması - SYS-18 raporundaki basit prototip versiyonu
     *
     * Bu fonksiyon bir NPC'nin mevcut durumunu alır ve evolüsyon kurallarını uygulayarak
     * güncellenmiş durumunu döndürür.
     */
    private fun evolveNPC(npcState: DynamicNPCState): DynamicNPCState {
        var updatedState = npcState

        // 1. Evolution Points kontrolü ve level atlama
        if (updatedState.evolutionPoints >= EVOLUTION_POINTS_THRESHOLD) {
            updatedState = performLevelUp(updatedState)
        }

        // 2. Mood evolüsyonu (zaman içinde loyalty'e göre mood ayarlama)
        updatedState = updateMoodBasedOnLoyalty(updatedState)

        // 3. Zamansal stat bozulması (çok az, gerçekçilik için)
        updatedState = applyTimeBasedStatDecay(updatedState)

        // 4. Nemesis potansiyeli kontrollü
        updatedState = checkNemesisPromotion(updatedState)

        return updatedState.copy(lastUpdateTime = System.currentTimeMillis())
    }

    /**
     * Fraksiyon diplomasilerini güncelle - TODO-NEM-07 kapsamında
     * Her World Tick'te fraksiyonlar arası diplomatik durumları simüle eder
     */
    private suspend fun updateFactionDiplomacy() {
        try {
            GameLogger.logSystem("$TAG: Fraksiyon diplomasileri güncelleniyor...")

            // Tüm fraksiyonları veritabanından çek
            val allFactionEntities = factionDao.getAllFactions()
            GameLogger.logSystem("$TAG: Toplam ${allFactionEntities.size} fraksiyon bulundu")

            if (allFactionEntities.size < 2) {
                GameLogger.logSystem("$TAG: Diplomatik simülasyon için yeterli fraksiyon yok (minimum 2 gerekli)")
                return
            }

            // Entity'leri FactionState'e çevir
            val currentFactions = allFactionEntities.map { it.toFactionState() }

            // DiplomacyEngine ile diplomatik simülasyon yap
            val updatedFactions = DiplomacyEngine.simulateDiplomaticEvents(currentFactions)

            // Değişiklikleri veritabanına kaydet
            var diplomaticUpdates = 0
            updatedFactions.forEachIndexed { index, updatedFaction ->
                val originalFaction = currentFactions[index]
                if (updatedFaction != originalFaction) {
                    factionDao.updateFaction(updatedFaction.toEntity())
                    diplomaticUpdates++

                    GameLogger.logSystem("$TAG: " +
                        "🏛️ ${updatedFaction.name} diplomasi durumu güncellendi")
                }
            }

            GameLogger.logSystem("$TAG: Fraksiyon diplomasi güncellemesi tamamlandı: $diplomaticUpdates fraksiyon güncellendi")

        } catch (e: Exception) {
            GameLogger.logSystem("$TAG: Fraksiyon diplomasi güncelleme hatası: ${e.message}")
        }
    }

    /**
     * NPC seviye atlama işlemi
     * TODO-NEM-04: Skills ve titles kazanma yeteneği eklendi
     */
    private fun performLevelUp(npcState: DynamicNPCState): DynamicNPCState {
        val newLevel = npcState.level + 1

        // Rastgele stat artışları
        val strengthIncrease = Random.nextInt(MIN_STAT_INCREASE, MAX_STAT_INCREASE + 1)
        val agilityIncrease = Random.nextInt(MIN_STAT_INCREASE, MAX_STAT_INCREASE + 1)
        val intelligenceIncrease = Random.nextInt(MIN_STAT_INCREASE, MAX_STAT_INCREASE + 1)
        val vitalityIncrease = Random.nextInt(MIN_STAT_INCREASE, MAX_STAT_INCREASE + 1)

        // Yeni skills ve titles kazanma şansı (TODO-NEM-04)
        val updatedSkills = grantRandomSkillOnLevelUp(npcState)
        val updatedTitles = grantRandomTitleOnLevelUp(npcState, newLevel)

        GameLogger.logSystem("$TAG: ${npcState.npcId} seviye atladı! ${npcState.level} -> $newLevel")
        if (updatedSkills.size > npcState.skills.size) {
            val newSkill = updatedSkills.subtract(npcState.skills.toSet()).first()
            GameLogger.logSystem("$TAG: ${npcState.npcId} yeni skill kazandı: $newSkill")
        }
        if (updatedTitles.size > npcState.titles.size) {
            val newTitle = updatedTitles.subtract(npcState.titles.toSet()).first()
            GameLogger.logSystem("$TAG: ${npcState.npcId} yeni title kazandı: $newTitle")
        }

        return npcState.copy(
            level = newLevel,
            currentStrength = npcState.currentStrength + strengthIncrease,
            currentAgility = npcState.currentAgility + agilityIncrease,
            currentIntelligence = npcState.currentIntelligence + intelligenceIncrease,
            currentVitality = npcState.currentVitality + vitalityIncrease,
            evolutionPoints = 0, // Puanları sıfırla
            skills = updatedSkills,
            titles = updatedTitles
        )
    }

    /**
     * Loyalty durumuna göre mood güncelleme
     */
    private fun updateMoodBasedOnLoyalty(npcState: DynamicNPCState): DynamicNPCState {
        val targetMood = NPCMood.fromLoyalty(npcState.loyaltyToPlayer)

        // Eğer mood farklıysa güncelle
        return if (npcState.currentMood != targetMood) {
            npcState.copy(currentMood = targetMood)
        } else {
            npcState
        }
    }

    /**
     * Zamansal stat azalması (çok minimal, gerçekçilik için)
     */
    private fun applyTimeBasedStatDecay(npcState: DynamicNPCState): DynamicNPCState {
        // Çok nadir durumlarda minimal stat azalması (aging effect)
        return if (Random.nextFloat() < 0.05f) { // %5 şans
            npcState.copy(
                currentStrength = (npcState.currentStrength - 1).coerceAtLeast(1),
                currentAgility = (npcState.currentAgility - 1).coerceAtLeast(1)
            )
        } else {
            npcState
        }
    }

    /**
     * Nemesis durumuna yükselme kontrolü
     */
    private fun checkNemesisPromotion(npcState: DynamicNPCState): DynamicNPCState {
        // Nemesis promosyon koşulları:
        // 1. Çok düşük loyalty (-60 ve altı)
        // 2. Yeterli level (3+)
        // 3. Henüz Nemesis değil

        val shouldPromote = npcState.loyaltyToPlayer <= -60 &&
                           npcState.level >= 3 &&
                           npcState.nemesisLevel == 0 &&
                           npcState.timesDefeated >= 1

        return if (shouldPromote) {
            GameLogger.logSystem("$TAG: 🔥 ${npcState.npcId} NEMESIS'e yükseltildi!")
            npcState.copy(
                nemesisLevel = 1,
                vendettaReasons = npcState.vendettaReasons + "defeated_multiple_times",
                personalityTraits = npcState.personalityTraits + "vengeful"
            )
        } else {
            npcState
        }
    }

    /**
     * Seviye atlama sırasında rastgele skill verme (TODO-NEM-04)
     */
    private fun grantRandomSkillOnLevelUp(npcState: DynamicNPCState): List<String> {
        val availableSkills = listOf(
            "heavy_armor", "light_armor", "shield_mastery",
            "sword_mastery", "axe_mastery", "bow_mastery", "magic_staff",
            "fire_magic", "ice_magic", "lightning_magic", "healing_magic",
            "stealth", "lockpicking", "intimidation", "persuasion",
            "survival", "tracking", "alchemy", "enchanting",
            "dodge_mastery", "berserker_rage", "tactical_mind"
        )

        val currentSkills = npcState.skills.toMutableList()

        // %40 şans ile yeni skill kazanma
        if (Random.nextFloat() < 0.4f && currentSkills.size < 8) {
            val unownedSkills = availableSkills.filter { !currentSkills.contains(it) }
            if (unownedSkills.isNotEmpty()) {
                val newSkill = unownedSkills.random()
                currentSkills.add(newSkill)
            }
        }

        return currentSkills
    }

    /**
     * Seviye atlama sırasında rastgele title verme (TODO-NEM-04)
     */
    private fun grantRandomTitleOnLevelUp(npcState: DynamicNPCState, newLevel: Int): List<String> {
        val levelBasedTitles = when (newLevel) {
            3 -> listOf("The Apprentice", "Novice Fighter", "Young Blood")
            5 -> listOf("Veteran Warrior", "The Experienced", "Battle-Tested")
            7 -> listOf("Elite Fighter", "The Skilled", "Master of Arms")
            10 -> listOf("The Legendary", "Grand Master", "Hero of the Realm")
            else -> emptyList()
        }

        val personalityTitles = when {
            npcState.personalityTraits.contains("aggressive") -> listOf("The Fierce", "Bloodthirsty", "The Brutal")
            npcState.personalityTraits.contains("cunning") -> listOf("The Cunning", "Shadowmaster", "The Deceiver")
            npcState.personalityTraits.contains("loyal") -> listOf("The Faithful", "True Friend", "Loyal Companion")
            npcState.personalityTraits.contains("vengeful") -> listOf("The Avenger", "Nemesis", "Vengeance Seeker")
            else -> listOf("The Wanderer", "Unknown Fighter", "The Mysterious")
        }

        val allPossibleTitles = levelBasedTitles + personalityTitles
        val currentTitles = npcState.titles.toMutableList()

        // %30 şans ile yeni title kazanma
        if (Random.nextFloat() < 0.3f && currentTitles.size < 5) {
            val unownedTitles = allPossibleTitles.filter { !currentTitles.contains(it) }
            if (unownedTitles.isNotEmpty()) {
                val newTitle = unownedTitles.random()
                currentTitles.add(newTitle)
            }
        }

        return currentTitles
    }

    /**
     * Manuel World Tick tetikleme (test amaçlı)
     */
    fun forceWorldTick() {
        GameLogger.logSystem("$TAG: Manuel WORLD TICK tetikleniyor...")
        scope.launch {
            triggerWorldTickEvent()
        }
    }

    /**
     * Settlement ekonomilerini güncelle - TODO-NEM-05 kapsamında
     * Her World Tick'te settlement'ların ekonomik durumlarını değiştirir
     */
    private suspend fun updateSettlementEconomics() {
        try {
            GameLogger.logSystem("$TAG: Settlement ekonomileri güncelleniyor...")

            val allSettlements = settlementDao.getAllSettlements()
            GameLogger.logSystem("$TAG: Toplam ${allSettlements.size} settlement bulundu")

            var updatedCount = 0

            allSettlements.forEach { settlementEntity ->
                val originalState = settlementEntity.toSettlementState()
                val updatedState = evolveSettlement(originalState)

                if (updatedState != originalState) {
                    settlementDao.updateSettlement(updatedState.toEntity())
                    updatedCount++

                    GameLogger.logSystem("$TAG: " +
                        "🏘️ ${updatedState.name} ekonomisi değişti: " +
                        "${originalState.economyLevel} → ${updatedState.economyLevel}")
                }
            }

            GameLogger.logSystem("$TAG: Settlement güncellemesi tamamlandı: $updatedCount settlement değişti")

        } catch (e: Exception) {
            GameLogger.logSystem("$TAG: Settlement güncelleme hatası: ${e.message}")
        }
    }

    /**
     * Tek bir settlement'ın evrimini simüle eder
     */
    private fun evolveSettlement(settlement: SettlementState): SettlementState {
        val currentDay = gameStateManager.gameState.value.currentDay

        // Basit ekonomik simülasyon: %20 şansla ekonomi değişir
        val economyChange = when {
            Random.nextFloat() < 0.1f -> -1 // %10 şans düşüş
            Random.nextFloat() < 0.1f -> 1  // %10 şans artış
            else -> 0 // %80 değişiklik yok
        }

        // Yeni ekonomi seviyesini hesapla
        val newEconomyLevel = when (settlement.economyLevel.ordinal + economyChange) {
            0 -> EconomyLevel.DESTITUTE
            1 -> EconomyLevel.POOR
            2 -> EconomyLevel.MODEST
            3 -> EconomyLevel.PROSPEROUS
            4 -> EconomyLevel.WEALTHY
            else -> settlement.economyLevel // Sınırların dışında kaldıysa değişiklik yok
        }

        // Mutluluk ekonomiye bağlı olarak değişir
        val happinessChange = when {
            newEconomyLevel.ordinal > settlement.economyLevel.ordinal -> Random.nextInt(5, 15) // Ekonomi iyileştiyse mutluluk artar
            newEconomyLevel.ordinal < settlement.economyLevel.ordinal -> Random.nextInt(-15, -5) // Ekonomi kötüleştiyse mutluluk azalır
            else -> Random.nextInt(-3, 4) // Rastgele küçük değişim
        }

        val newHappiness = (settlement.happiness + happinessChange).coerceIn(0, 100)

        return settlement.copy(
            economyLevel = newEconomyLevel,
            happiness = newHappiness,
            lastUpdateDay = currentDay
        )
    }

    /**
     * Oyuncu etkilerini uygula - TODO-NEM-08 kapsamında sistemik etkileşimler
     * Bu fonksiyon, oyuncunun sahip olduğu unvanları ve fraksiyonlarla olan itibarını,
     * dünya durumu değişkenlerine etkisini hesaplar ve uygular.
     */
    private suspend fun applyPlayerSystemicEffects() {
        try {
            GameLogger.logSystem("$TAG: Oyuncu sistemik etkileri uygulanıyor...")

            // Mevcut oyun durumunu al
            val gameState = gameStateManager.gameState.value

            // Oyuncunun unvanlarını al (TITLE tipindeki badge'ler)
            val playerTitles = gameState.activeBadges.filter { it.type == com.example.isekaikuroshin.data.BadgeType.TITLE }

            // Tüm settlement'ları veritabanından çek
            val allSettlements = settlementDao.getAllSettlements()
            val settlements = allSettlements.map { it.toSettlementState() }

            // Tüm fraksiyonları veritabanından çek
            val allFactionEntities = factionDao.getAllFactions()
            val factions = allFactionEntities.map { it.toFactionState() }

            GameLogger.logSystem("$TAG: ${playerTitles.size} unvan, ${settlements.size} settlement, ${factions.size} fraksiyon işlenecek")

            // 1. Unvan etkilerini uygula
            var updatedSettlements = SystemicEngine.applyPlayerTitleEffects(
                settlements = settlements,
                playerState = gameState.playerState,
                playerTitles = playerTitles
            )

            // 2. İtibar etkilerini uygula
            updatedSettlements = SystemicEngine.applyPlayerReputationEffects(
                settlements = updatedSettlements,
                playerState = gameState.playerState,
                factions = factions
            )

            // 3. Değişiklikleri veritabanına kaydet
            var updatedCount = 0
            updatedSettlements.forEachIndexed { index, updatedSettlement ->
                val originalSettlement = settlements[index]
                if (updatedSettlement != originalSettlement) {
                    settlementDao.updateSettlement(updatedSettlement.toEntity())
                    updatedCount++

                    GameLogger.logSystem("$TAG: " +
                        "🌟 ${updatedSettlement.name} oyuncu etkisiyle güncellendi: " +
                        "Mutluluk: ${originalSettlement.happiness} → ${updatedSettlement.happiness}, " +
                        "Ekonomi: ${originalSettlement.economyLevel} → ${updatedSettlement.economyLevel}")
                }
            }

            GameLogger.logSystem("$TAG: Oyuncu sistemik etkileri tamamlandı: $updatedCount settlement güncellendi")

        } catch (e: Exception) {
            GameLogger.logSystem("$TAG: Oyuncu sistemik etkileri hatası: ${e.message}")
        }
    }

    /**
     * Motor durumu raporu
     */
    suspend fun getEngineStatusReport(): String {
        val totalNpcs = dynamicNpcDao.getAllNpcStates().size
        val aliveNpcs = dynamicNpcDao.getAllAliveNpcs().size
        val nemesisNpcs = dynamicNpcDao.getNpcsByNemesisLevel(1).size
        val currentDay = gameStateManager.gameState.value.currentDay
        val nextWorldTick = currentDay + (WORLD_TICK_INTERVAL - (currentDay % WORLD_TICK_INTERVAL))

        return """
            🌍 WORLD UPDATE ENGINE RAPORU 🌍

            📊 NPC İstatistikleri:
            • Toplam NPC: $totalNpcs
            • Canlı NPC: $aliveNpcs
            • Nemesis NPC: $nemesisNpcs

            ⏰ Zaman Bilgileri:
            • Mevcut Gün: $currentDay
            • Sonraki World Tick: Gün $nextWorldTick
            • Tick Aralığı: Her $WORLD_TICK_INTERVAL gün

            ⚙️ Motor Durumu: AKTİF
        """.trimIndent()
    }
}