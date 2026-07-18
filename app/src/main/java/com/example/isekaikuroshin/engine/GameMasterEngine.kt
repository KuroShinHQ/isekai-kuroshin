package com.example.isekaikuroshin.engine

import android.content.Context
import com.example.isekaikuroshin.data.GameStateZ7
import com.example.isekaikuroshin.data.PersistentDataManager
import com.example.isekaikuroshin.data.database.DocumentChunkDao
import com.example.isekaikuroshin.data.database.DocumentChunkEntity
import com.example.isekaikuroshin.data.database.DynamicNPCDao
import com.example.isekaikuroshin.data.database.DynamicNPCEntity
import com.example.isekaikuroshin.data.database.FactionDao
import com.example.isekaikuroshin.data.database.SettlementDao
import com.example.isekaikuroshin.data.database.toFactionState
import com.example.isekaikuroshin.data.database.toSettlementState
import com.example.isekaikuroshin.data.LanguageManager
import com.example.isekaikuroshin.data.GMResponse
import com.example.isekaikuroshin.utils.GameLogger
import com.example.isekaikuroshin.utils.BM25Scorer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Game Master Engine - Faz 3: Yapılandırılmış JSON Çıktısı (GM'in Elleri)
 *
 * Veritabanındaki metin parçalarını kullanarak AI'dan bağlama duyarlı
 * hikaye yanıtları üretir ve oyun mekaniklerini tetikleyebilecek
 * yapılandırılmış JSON komutları döndürür.
 *
 * Bu engine, BM25 algoritması ile alakalılık puanlaması yaparak
 * oyuncu girdisine en uygun metin parçalarını seçer ve GM'e
 * sadece hikaye değil, oyun mekaniklerini etkileyebilme yetisi verir.
 *
 * Bu engine, TODO-AI-05 görevinin JSON çıktı implementasyonudur.
 *
 * RAG SİSTEMİ ENTEGRASYONU:
 * - StoryLoader ile assets/stories klasöründeki kitapları okur
 * - Kitabı chunk'lara ayırır ve BM25 ile en alakalı chunk'ları seçer
 * - AI prompt'una ilgili metin parçalarını ekler (RAG)
 */
@Singleton
class GameMasterEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val documentChunkDao: DocumentChunkDao,
    private val dynamicNPCDao: DynamicNPCDao,
    private val factionDao: FactionDao,
    private val settlementDao: SettlementDao,
    private val basicStoryEngine: BasicStoryEngine,
    private val persistentDataManager: PersistentDataManager,
    private val aiClientProvider: AIClientProvider,
    private val observerEngine: ObserverEngine // TODO-G108.1: World summary for AI context
) {

    // RAG sistemi için StoryLoader
    private val storyLoader = StoryLoader(context)

    // Cache: Yüklenen hikaye chunk'ları (performans için)
    private var cachedStoryPath: String? = null
    private var cachedStoryChunks: List<String> = emptyList()

    // TODO-G108.2: Cache world summary to avoid recalculating on every prompt
    private var cachedWorldSummary: String? = null
    private var cachedWorldSummaryDay: Int = -1

    /**
     * BM25 algoritması kullanarak oyuncu girdisine en alakalı metin parçalarını seçer
     *
     * @param playerInput Oyuncunun eylemi (anahtar kelime çıkarma için kullanılır)
     * @param uri Kaynak doküman URI'ı (boşsa tüm dökümanlardan seçer)
     * @param topK En alakalı K adet chunk seçer (varsayılan: 3)
     * @return BM25 skoruna göre sıralanmış DocumentChunkEntity listesi
     */
    private suspend fun getBM25RankedChunks(
        playerInput: String,
        uri: String = "",
        topK: Int = 3
    ): List<DocumentChunkEntity> {
        return try {
            // Adım 1: Mevcut dili al
            val currentLanguage = LanguageManager.currentLanguage.value

            GameLogger.logSystem("=== BM25 CHUNK SELECTION START ===")
            GameLogger.logSystem("Player input for BM25: '$playerInput'")
            GameLogger.logSystem("Target URI: '${uri.ifEmpty { "ALL_DOCUMENTS" }}'")
            GameLogger.logSystem("Current language: $currentLanguage")

            // Adım 2: Dokümanları al
            val allChunks = if (uri.isNotEmpty()) {
                documentChunkDao.getChunksByUri(uri)
            } else {
                documentChunkDao.getAllChunks()
            }

            GameLogger.logSystem("Total available chunks: ${allChunks.size}")

            if (allChunks.isEmpty()) {
                GameLogger.logSystem("No chunks available for BM25 ranking")
                return emptyList()
            }

            // Adım 3: BM25Scorer'ı mevcut dil ile başlat
            val bm25Scorer = BM25Scorer(allChunks, currentLanguage)

            // Adım 3: Oyuncu girdisinden anahtar kelimeleri çıkar
            val keywords = bm25Scorer.extractKeywords(playerInput)
            GameLogger.logSystem("Extracted keywords: $keywords")

            if (keywords.isEmpty()) {
                GameLogger.logSystem("No keywords extracted, falling back to first $topK chunks")
                return allChunks.take(topK)
            }

            // Adım 4: BM25 ile chunk'ları puanla ve sırala
            val rankedScoredChunks = bm25Scorer.rankChunks(keywords, topK)

            // Adım 5: Sadece DocumentChunkEntity'leri döndür
            val selectedChunks = rankedScoredChunks.map { it.chunk }

            GameLogger.logSystem("=== BM25 SELECTION RESULTS ===")
            rankedScoredChunks.forEachIndexed { index, scoredChunk ->
                GameLogger.logSystem("${index + 1}. Chunk ${scoredChunk.chunkIndex} (Score: ${String.format("%.3f", scoredChunk.score)})")
                GameLogger.logSystem("   URI: ${scoredChunk.chunk.sourceDocumentUri}")
                GameLogger.logSystem("   Content preview: ${scoredChunk.chunk.content.take(150)}...")
            }
            GameLogger.logSystem("=== BM25 CHUNK SELECTION END ===")

            selectedChunks

        } catch (e: Exception) {
            GameLogger.logError("GameMasterEngine", "Failed to get BM25 ranked chunks", e)
            // Fallback: basit ilk N chunk alma
            try {
                val fallbackChunks = if (uri.isNotEmpty()) {
                    documentChunkDao.getChunksByUri(uri).take(topK)
                } else {
                    documentChunkDao.getAllChunks().take(topK)
                }
                GameLogger.logSystem("BM25 failed, using fallback: ${fallbackChunks.size} chunks")
                fallbackChunks
            } catch (fallbackException: Exception) {
                GameLogger.logError("GameMasterEngine", "Fallback chunk retrieval also failed", fallbackException)
                emptyList()
            }
        }
    }

    /**
     * NPC ile etkileşim varsa o NPC'nin dinamik durumunu veritabanından çeker
     * TODO-NEM-03: GM Entegrasyonu kapsamında eklendi
     */
    private suspend fun detectAndLoadNPCContext(
        playerInput: String,
        gameState: GameStateZ7
    ): DynamicNPCEntity? {
        return try {
            // Oyuncu girdisinden NPC ID'si çıkarmaya çalış
            val currentLocationNPCs = dynamicNPCDao.getNpcsAtLocation(gameState.currentLocationId)

            GameLogger.logSystem("=== NPC CONTEXT DETECTION ===")
            GameLogger.logSystem("Player location: ${gameState.currentLocationId}")
            GameLogger.logSystem("NPCs at location: ${currentLocationNPCs.size}")

            // Eğer bu lokasyonda sadece 1 NPC varsa, muhtemelen onunla etkileşime geçiyor
            val targetNPC = if (currentLocationNPCs.size == 1) {
                currentLocationNPCs.first()
            } else {
                // Oyuncu girdisinde NPC adı geçen var mı kontrol et
                currentLocationNPCs.firstOrNull { npc ->
                    playerInput.contains(npc.npcId, ignoreCase = true) ||
                    playerInput.contains(npc.baseRegistryId, ignoreCase = true)
                }
            }

            if (targetNPC != null) {
                GameLogger.logSystem("=== NPC CONTEXT FOUND ===")
                GameLogger.logSystem("Target NPC ID: ${targetNPC.npcId}")
                GameLogger.logSystem("NPC Registry ID: ${targetNPC.baseRegistryId}")
                GameLogger.logSystem("NPC Level: ${targetNPC.level}")
                GameLogger.logSystem("NPC Loyalty: ${targetNPC.loyaltyToPlayer}")
                GameLogger.logSystem("NPC Nemesis Level: ${targetNPC.nemesisLevel}")
                GameLogger.logSystem("NPC Mood: ${targetNPC.currentMood}")
                GameLogger.logSystem("NPC Interaction Count: ${targetNPC.interactionCount}")

                // Son etkileşim sayacını artır
                dynamicNPCDao.incrementInteractionCount(targetNPC.npcId, System.currentTimeMillis())

                targetNPC
            } else {
                GameLogger.logSystem("No specific NPC detected for interaction")
                null
            }
        } catch (e: Exception) {
            GameLogger.logError("GameMasterEngine", "Failed to detect NPC context", e)
            null
        }
    }

    /**
     * Oyuncunun bulunduğu lokasyonun fraksiyon bilgilerini alır ve diplomatik durumu oluşturur
     * TODO-NEM-07: Dinamik Fraksiyon ve Diplomasi Motoru kapsamında eklendi
     */
    private suspend fun detectAndLoadFactionContext(gameState: GameStateZ7): String? {
        return try {
            GameLogger.logSystem("=== FACTION CONTEXT DETECTION ===")
            GameLogger.logSystem("Player location: ${gameState.currentLocationId}")

            // Oyuncunun bulunduğu settlement'ı bul
            val currentSettlement = settlementDao.getSettlementById(gameState.currentLocationId)
            if (currentSettlement == null) {
                GameLogger.logSystem("Settlement bulunamadı: ${gameState.currentLocationId}")
                return null
            }

            val settlementState = currentSettlement.toSettlementState()
            val governingFactionId = settlementState.governingFaction

            if (governingFactionId == null) {
                GameLogger.logSystem("Settlement'ın yönetici fraksiyonu yok")
                return null
            }

            // Yönetici fraksiyonu bul
            val governingFactionEntity = factionDao.getFactionById(governingFactionId)
            if (governingFactionEntity == null) {
                GameLogger.logSystem("Yönetici fraksiyon bulunamadı: $governingFactionId")
                return null
            }

            val governingFaction = governingFactionEntity.toFactionState()
            GameLogger.logSystem("=== FACTION CONTEXT FOUND ===")
            GameLogger.logSystem("Governing Faction: ${governingFaction.name}")
            GameLogger.logSystem("Faction Power: ${governingFaction.getTotalPower()}")
            GameLogger.logSystem("Faction Type: ${governingFaction.type}")

            // Diğer fraksiyonlarla diplomatik ilişkileri al
            val allFactions = factionDao.getAllFactions().map { it.toFactionState() }
            val diplomaticRelations = mutableListOf<String>()

            governingFaction.diplomaticRelations.forEach { (factionId, relation) ->
                val otherFaction = allFactions.find { it.id == factionId }
                if (otherFaction != null) {
                    val relationText = when (relation) {
                        com.example.isekaikuroshin.data.world.DiplomacyStatus.WAR -> "SAVAŞ HALİNDE"
                        com.example.isekaikuroshin.data.world.DiplomacyStatus.HOSTILE -> "DÜŞMANCA"
                        com.example.isekaikuroshin.data.world.DiplomacyStatus.NEUTRAL -> "TARAFSIZ"
                        com.example.isekaikuroshin.data.world.DiplomacyStatus.FRIENDLY -> "DOSTANE"
                        com.example.isekaikuroshin.data.world.DiplomacyStatus.ALLIANCE -> "MÜTTEFİK"
                    }
                    diplomaticRelations.add("${otherFaction.name} ($relationText)")
                }
            }

            // Fraksiyon bağlam metni oluştur
            buildFactionContextSection(governingFaction, diplomaticRelations, settlementState.name)

        } catch (e: Exception) {
            GameLogger.logError("GameMasterEngine", "Failed to detect faction context", e)
            null
        }
    }

    /**
     * Fraksiyon durumu için GM prompt'a eklenecek bölümü oluşturur
     * TODO-NEM-07: Dinamik Fraksiyon ve Diplomasi Motoru kapsamında eklendi
     */
    private fun buildFactionContextSection(
        governingFaction: com.example.isekaikuroshin.data.world.FactionState,
        diplomaticRelations: List<String>,
        settlementName: String
    ): String {
        val factionStatusText = if (governingFaction.status.isNotEmpty()) {
            governingFaction.status.joinToString(", ") { status ->
                when (status) {
                    com.example.isekaikuroshin.data.world.FactionStatus.AT_WAR -> "Savaş Halinde"
                    com.example.isekaikuroshin.data.world.FactionStatus.CIVIL_WAR -> "İç Savaş"
                    com.example.isekaikuroshin.data.world.FactionStatus.EXPANDING -> "Genişliyor"
                    com.example.isekaikuroshin.data.world.FactionStatus.DECLINING -> "Geriiliyor"
                    com.example.isekaikuroshin.data.world.FactionStatus.TRADE_EMBARGO -> "Ticaret Ambargosu"
                    com.example.isekaikuroshin.data.world.FactionStatus.DIPLOMATIC_CRISIS -> "Diplomatik Kriz"
                    com.example.isekaikuroshin.data.world.FactionStatus.GOLDEN_AGE -> "Altın Çağ"
                    com.example.isekaikuroshin.data.world.FactionStatus.FAMINE -> "Kıtlık"
                    com.example.isekaikuroshin.data.world.FactionStatus.PLAGUE -> "Veba"
                    com.example.isekaikuroshin.data.world.FactionStatus.SUCCESSION_CRISIS -> "Veraset Krizi"
                    com.example.isekaikuroshin.data.world.FactionStatus.BANDIT_TROUBLES -> "Eşkıya Sorunu"
                }
            }
        } else {
            "Normal"
        }

        return """
[FRAKSİYON DURUMU: ${governingFaction.name}]
$settlementName şehrinde bulunuyorsun ve bu şehir ${governingFaction.name} fraksiyonu tarafından yönetiliyor.

**Fraksiyon Bilgileri:**
- İsim: ${governingFaction.name}
- Tür: ${governingFaction.getTypeDescription()}
- Güç Seviyesi: ${governingFaction.getTotalPower()}/100
- Etki Alanı: ${governingFaction.influence}/100
- Durum: $factionStatusText
${if (governingFaction.leaderName != null) "- Lider: ${governingFaction.leaderName}" else ""}
${if (governingFaction.capitalSettlement != null) "- Başkent: ${governingFaction.capitalSettlement}" else ""}

**Diplomatik İlişkiler:**
${if (diplomaticRelations.isNotEmpty()) {
    diplomaticRelations.joinToString("\n") { "- $it" }
} else {
    "- Henüz diplomatik ilişki kurulmamış"
}}

**ÖNEMLİ:** Bu fraksiyon bilgileri hikayeni etkiler. Eğer fraksiyon savaş halindeyse, şehirde gerginlik olabilir. Diplomatik durumlar NPC davranışlarını ve mevcut olayları etkileyebilir. Fraksiyon liderinden veya diplomatik durumlardan bahsedebilirsin.
""".trimIndent()
    }

    /**
     * NPC durumu için GM prompt'a eklenecek bölümü oluşturur
     * TODO-NEM-03: GM Entegrasyonu kapsamında eklendi
     * TODO-NEM-04: Skills, titles, badges desteği eklendi
     */
    private fun buildNPCContextSection(npc: DynamicNPCEntity): String {
        val recentMemories = npc.historyWithPlayer.takeLast(3)
            .joinToString("\n        ") { memory ->
                "- ${memory.timestamp}: ${memory.eventType} - ${memory.eventDescription}"
            }

        val personalityTraitsText = npc.personalityTraits.joinToString(", ")
        val adaptationTraitsText = npc.adaptationTraits.joinToString(", ")
        val vendettaReasonsText = npc.vendettaReasons.joinToString(", ")

        // TODO-NEM-04: Yeni gelişmiş özellikler
        val skillsText = npc.skills.joinToString(", ")
        val titlesText = npc.titles.joinToString(", ")
        val badgesText = npc.badges.joinToString(", ")

        val nemesisStatusText = when (npc.nemesisLevel) {
            0 -> "Normal NPC"
            1 -> "Minor Rival"
            2 -> "Active Rival"
            3 -> "Serious Nemesis"
            4 -> "Dangerous Nemesis"
            5 -> "Arch-Nemesis"
            else -> "Unknown Level ${npc.nemesisLevel}"
        }

        // Unvan ekleyerek isim formatını zenginleştir
        val fullNameWithTitles = if (npc.titles.isNotEmpty()) {
            "${npc.npcId} '${npc.titles.first()}'"
        } else {
            npc.npcId
        }

        return """
[NPC DURUMU: $fullNameWithTitles]
Bu NPC ile etkileşime geçiyor olabilirsin. İşte onun güncel durumu:

**Temel Bilgiler:**
- İsim/ID: $fullNameWithTitles (Registry: ${npc.baseRegistryId})
- Seviye: ${npc.level}
- Yaş: ${npc.age}, Cinsiyet: ${npc.gender}
- Lokasyon: ${npc.currentLocationId}
- Durum: ${if (npc.isAlive) "Yaşıyor" else "Ölü"} ${if (npc.isExiled) "(Sürgünde)" else ""}

**Dinamik Özellikler:**
- Güç: ${npc.currentStrength}, Çeviklik: ${npc.currentAgility}
- Zeka: ${npc.currentIntelligence}, Dayanıklılık: ${npc.currentVitality}
- Ruh Hali: ${npc.currentMood}
- Kişilik Özellikleri: ${personalityTraitsText.ifEmpty { "Henüz belirlenmemiş" }}

**Gelişmiş Yetenekler ve Başarılar (TODO-NEM-04):**
- Yetenekler: ${skillsText.ifEmpty { "Henüz yetenek kazanmamış" }}
- Unvanlar: ${titlesText.ifEmpty { "Henüz unvan sahibi değil" }}
- Rozetler: ${badgesText.ifEmpty { "Henüz rozet kazanmamış" }}

**Oyuncuyla İlişki:**
- Sadakat Skoru: ${npc.loyaltyToPlayer}/100 (${when {
            npc.loyaltyToPlayer > 60 -> "Dostane"
            npc.loyaltyToPlayer > 20 -> "Nötr"
            npc.loyaltyToPlayer > -20 -> "Soğuk"
            npc.loyaltyToPlayer > -60 -> "Düşmanca"
            else -> "Öfkeli"
        }})
- Toplam Etkileşim: ${npc.interactionCount} kez
- Yenilgi Sayısı: ${npc.timesDefeated}
- Yardım Edilme: ${npc.timesHelped}

**Nemesis Durumu:**
- Nemesis Seviyesi: ${npc.nemesisLevel}/5 ($nemesisStatusText)
${if (npc.vendettaReasons.isNotEmpty()) "- Kin Sebepleri: $vendettaReasonsText" else ""}
${if (npc.adaptationTraits.isNotEmpty()) "- Adaptasyon Özellikleri: $adaptationTraitsText" else ""}

**Son Etkileşimler:**
${if (recentMemories.isNotEmpty()) recentMemories else "        - Henüz kayıtlı etkileşim yok"}

**ÖNEMLİ:** Bu NPC'nin güncel durumunu hikayende dikkate al. Nemesis seviyesi yüksekse daha agresif, sadakat düşükse daha az yardımsever davranmalı. Kişilik özelliklerine, yeteneklerine, unvanlarına ve ruh haline uygun davranış sergile. Özellikle yetenekleri olan NPC'ler bunları hikayede kullanabilir (örn. fire_magic yeteneği olan bir NPC ateş büyüleri kullanabilir).
""".trimIndent()
    }

    /**
     * Dile göre Game Master prompt şablonunu oluşturur
     * TODO-NEM-03: NPC farkındalığı ile zenginleştirildi
     * RAG UPDATE: Text chunks artık List<String> olarak geliyor (assets story + database)
     *
     * @param playerInput Oyuncunun eylemi
     * @param gameState Mevcut oyun durumu
     * @param textChunks İlham kaynağı metin parçaları (RAG system)
     * @param npcContext Etkileşimdeki NPC'nin dinamik durumu (varsa)
     * @return Dile uygun tam GM prompt metni
     */
    private suspend fun buildGMPrompt(
        playerInput: String,
        gameState: GameStateZ7,
        textChunks: List<String>,
        npcContext: DynamicNPCEntity? = null,
        factionContext: String? = null
    ): String {
        val currentLanguage = LanguageManager.currentLanguage.value

        GameLogger.logSystem("=== BUILDING GM PROMPT ===")
        GameLogger.logSystem("Building prompt for language: $currentLanguage")

        val systemRole = LanguageManager.getText("gm_system_role")
        val systemDescription = LanguageManager.getText("gm_system_description")
        val currentGameState = LanguageManager.getText("gm_current_game_state")
        val day = LanguageManager.getText("gm_day")
        val time = LanguageManager.getText("gm_time")
        val location = LanguageManager.getText("gm_location")
        val playerHP = LanguageManager.getText("gm_player_hp")
        val playerMP = LanguageManager.getText("gm_player_mp")
        val playerLevel = LanguageManager.getText("gm_player_level")
        val moralityStatus = LanguageManager.getText("gm_morality_status")
        val playerAction = LanguageManager.getText("gm_player_action")
        val sourceText = LanguageManager.getText("gm_source_text")
        val sourceDescription = LanguageManager.getText("gm_source_description")
        val inspirationSection = LanguageManager.getText("gm_inspiration_section")
        val noInspiration = LanguageManager.getText("gm_no_inspiration")
        val taskTitle = LanguageManager.getText("gm_task_title")
        val taskDescription = LanguageManager.getText("gm_task_description")
        val rule1 = LanguageManager.getText("gm_task_rule_1")
        val rule2 = LanguageManager.getText("gm_task_rule_2")
        val rule3 = LanguageManager.getText("gm_task_rule_3")
        val rule4 = LanguageManager.getText("gm_task_rule_4")
        val rule5 = LanguageManager.getText("gm_task_rule_5")

        val moralityText = when {
            gameState.playerState.moralityScore > 0.3f -> LanguageManager.getText("gm_morality_good")
            gameState.playerState.moralityScore < -0.3f -> LanguageManager.getText("gm_morality_evil")
            else -> LanguageManager.getText("gm_morality_neutral")
        }

        // TODO-G108.2: Create cached world summary (performance optimization)
        // Cache world summary by day - only recalculate when day changes
        val worldSummary = if (cachedWorldSummaryDay == gameState.currentDay && cachedWorldSummary != null) {
            GameLogger.logSystem("[GMEngine] Using cached world summary (Day ${gameState.currentDay})")
            cachedWorldSummary!!
        } else {
            GameLogger.logSystem("[GMEngine] Creating new world summary (Day ${gameState.currentDay})")
            val newSummary = observerEngine.createWorldStateSummary(gameState)
            cachedWorldSummary = newSummary
            cachedWorldSummaryDay = gameState.currentDay
            newSummary
        }

        val prompt = """
[$systemRole]
$systemDescription

[$currentGameState]
$day: ${gameState.currentDay}
$time: ${gameState.currentTimeOfDay.displayName}
$location: ${gameState.currentLocationId}
$playerHP: ${gameState.playerState.currentHealth}/${gameState.playerState.maxHealth}
$playerMP: ${gameState.playerState.currentMana}/${gameState.playerState.maxMana}
$playerLevel: ${gameState.playerState.level}
$moralityStatus: $moralityText

[WORLD STATE SUMMARY]
$worldSummary

🎒 G143: PLAYER INVENTORY & ABILITIES (REALITY CHECK - CRITICAL!)
═══════════════════════════════════════════════════════════════

**AVAILABLE SPELLS:**
${if (gameState.activeSkills.isNotEmpty()) {
    gameState.activeSkills.joinToString("\n") { skill ->
        "- ${skill.name} (Mana: ${skill.manaCost ?: "N/A"})"
    }
} else {
    "- No spells learned yet"
}}

**AVAILABLE ITEMS:**
${if (gameState.inventory.isNotEmpty()) {
    gameState.inventory.take(20).joinToString("\n") { item ->
        "- ${item.name} (Type: ${item.type ?: "Unknown"})"
    }
} else {
    "- No items in inventory"
}}

**EQUIPPED ITEMS:**
${if (gameState.equippedItems.isNotEmpty()) {
    gameState.equippedItems.values.joinToString("\n") { item ->
        "- ${item.name} (Slot: ${item.equipmentSlot ?: "Unknown"})"
    }
} else {
    "- No equipment"
}}

⚠️⚠️⚠️ REALITY VALIDATION - ABSOLUTELY MANDATORY! ⚠️⚠️⚠️
═══════════════════════════════════════════════════════════════

IF PLAYER IS IN COMBAT (${if (gameState.inCombat) "YES - COMBAT ACTIVE!" else "NO"}):

1. **SPELL CHECK:** Player uses spell? → MUST be in "AVAILABLE SPELLS" list above!
   - Player says "fireball" but "Fireball" NOT in list → IMPOSSIBLE!
   - Response: "You don't have the 'fireball' spell. Available spells: [list]"
   - NO DAMAGE, action FAILS!

2. **ITEM CHECK:** Player uses item? → MUST be in "AVAILABLE ITEMS" list above!
   - Player says "health potion" but NOT in list → IMPOSSIBLE!
   - Response: "You don't have a health potion."
   - NO EFFECT, action FAILS!

3. **MANA CHECK:** Spell requires mana? → Player MUST have enough mana (Current: ${gameState.playerState.currentMana}/${gameState.playerState.maxMana})
   - Not enough mana → IMPOSSIBLE!
   - Response: "Not enough mana. You need [X] but have ${gameState.playerState.currentMana}."

4. **PHYSICS CHECK:** Action physically possible?
   - Can't fly without wings/spell
   - Can't teleport without spell
   - Can't walk through walls
   - Can't lift impossibly heavy objects

⚠️ IF PLAYER ATTEMPTS IMPOSSIBLE ACTION:
- Set impossibleAction: true in response
- Set penaltyDescription: "Explanation of why action is impossible"
- NO DAMAGE to enemy, NO effect, action FAILED
- Inform player what went wrong

[$playerAction]
$playerInput

${if (npcContext != null) buildNPCContextSection(npcContext) + "\n\n" else ""}${if (factionContext != null) factionContext + "\n\n" else ""}
[$sourceText]
$sourceDescription

${if (textChunks.isNotEmpty()) {
    textChunks.mapIndexed { index, chunk ->
        "--- $inspirationSection ${index + 1} ---\n${chunk.take(800)}${if (chunk.length > 800) "..." else ""}"
    }.joinToString("\n\n")
} else {
    noInspiration
}}

[$taskTitle]
$taskDescription

🚨🚨🚨 G144: LANGUAGE LOCK (CRITICAL!) 🚨🚨🚨
**USER SELECTED LANGUAGE: %CURRENT_LANGUAGE%**

⚠️ **ZORUNLU LANGUAGE RULES:**
1. **ALL** responses (storyText, NPC names, item names, descriptions) MUST be in %CURRENT_LANGUAGE%!
2. **DO NOT** switch languages under ANY circumstance!
3. **BATTLE context** does NOT change language - MAINTAIN %CURRENT_LANGUAGE%!
4. **displayName** fields MUST match %CURRENT_LANGUAGE%!
5. If user writes in different language, **STILL respond in %CURRENT_LANGUAGE%**!

**CORRECT EXAMPLES:**
- %CURRENT_LANGUAGE% = English → "You defeated the goblin." ✅
- %CURRENT_LANGUAGE% = Turkish → "Goblini yendin." ✅

**WRONG EXAMPLES:**
- %CURRENT_LANGUAGE% = English → "Goblini yendin." ❌ (Turkish response!)
- %CURRENT_LANGUAGE% = Turkish → "You defeated the goblin." ❌ (English response!)

⚠️⚠️⚠️ KRİTİK UYARI - JSON ZORUNLUDUR ⚠️⚠️⚠️
Cevabını MUTLAKA aşağıdaki JSON formatında döndür.
❌ ASLA düz metin, açıklama, yorum veya hata mesajı yazma!
❌ ASLA "AI Sağlayıcısı yapılandırılmamış" gibi mesajlar döndürme!
❌ ASLA JSON dışında bir şey yazma!
✅ SADECE ve SADECE geçerli JSON döndür!
✅ İlk karakter "{" ile başla, son karakter "}" ile bitir!

ZORUNLU JSON FORMATI:

{
  "journalEntry": "Hikaye metni buraya gelecek",
  "itemsGained": [
    {"itemId": "item_sword_of_light", "name": "Light's Edge", "description": "A legendary sword forged from pure light", "type": "WEAPON", "rarity": "LEGENDARY"},
    {"itemId": "item_health_potion", "name": "Health Potion", "description": "Restores 50 HP", "type": "CONSUMABLE", "rarity": "COMMON"}
  ],
  "questsGained": [
    {"questId": "quest_find_lost_cat", "title": "Find the Lost Cat", "description": "The village elder's cat went missing in the dark forest", "giver": "Village Elder", "dueDay": -1, "rewards": ["50 Gold", "Healing Potion"]}
  ],
  "questsUpdated": ["quest_id_1"],
  "statsChanged": {"strength": 1, "health": -2},
  "weatherChange": "RAINY",
  "timeShift": "NIGHT",
  "locationsUnlocked": ["dark_forest", "ancient_ruins"],
  "locationChange": "mysterious_cave",
  "npcStateChange": {
    "npcId": "guard_001",
    "loyaltyChange": -10,
    "memoryAdded": {
      "eventType": "CONFRONTATION",
      "eventDescription": "Oyuncuyla kavga etti",
      "emotionalImpact": "NEGATIVE"
    }
  },
  "nemesisEvolution": {
    "npcId": "merchant_005",
    "newNemesisLevel": 2,
    "vendettaReason": "Ticari anlaşmada kandırıldı",
    "evolutionDescription": "Artık oyuncuya güvenmiyor"
  },
  "impossibleAction": false,
  "penaltyDescription": null
}

🚨🚨🚨 G136: ZORUNLU CONTENT GENERATION RULES 🚨🚨🚨
⚠️⚠️⚠️ AI MUST FOLLOW THESE RULES - CONTENT GENERATION IS MANDATORY! ⚠️⚠️⚠️

1. **itemsGained**: Give items FREQUENTLY!
   - Every 3-5 entries: At least 1 item
   - Player asks for item: IMMEDIATELY add to itemsGained
   - Player finds/loots/receives item: IMMEDIATELY add to itemsGained
   - If no item given recently: Consider giving item

2. **questsGained**: Give quests REGULARLY!
   - Every 10 entries: At least 1 new quest
   - NPC interaction: Consider giving quest
   - Major event/discovery: Consider giving quest
   - If no quest given in 10+ entries: MUST give quest

3. **npcStateChange**: NPC interactions MUST be tracked!
   - Every 5 entries: At least 1 NPC interaction
   - Player meets new NPC: MUST set npcStateChange with npcId
   - Player talks to NPC: MUST update relationshipChange
   - If no NPC interaction in 5+ entries: Create NPC encounter

4. **moralityDelta**: MANDATORY in EVERY response!
   - Good actions: +0.05 to +0.15
   - Evil actions: -0.05 to -0.15
   - Neutral/travel: 0.0 (but MUST be present!)
   - NEVER leave moralityDelta null or missing

⚠️ **VALIDATION**: AI output will be validated. Missing content = system fallback!

KURALLAR:
⚠️ ÖNEMLİ: journalEntry KESİNLİKLE 2-3 CÜMLEDEN UZUN OLMAMALIDIR! Kısa ve öz yaz!
- journalEntry: Oyuncunun günlüğüne eklenecek hikaye metni (MAKSIMUM 2-3 CÜMLE - paragraf değil!)
- itemsGained: Oyuncunun kazandığı item'lar (ZORUNLU: itemId, name, description, type, rarity)
  ⚠️⚠️⚠️ KRİTİK: OYUNCU BİR İTEM İSTERSE MUTLAKA itemsGained DOLDUR! ⚠️⚠️⚠️
  * "give me apple" → itemsGained: [{"itemId": "item_apple", "name": "Apple", ...}]
  * "I need a sword" → itemsGained: [{"itemId": "item_iron_sword", "name": "Iron Sword", ...}]
  * "I want food" → itemsGained: [{"itemId": "item_bread", "name": "Bread", ...}]
  * Oyuncu item buldu/aldı → itemsGained doldur!
  * Oyuncu item kazandı (quest, loot, gift) → itemsGained doldur!
  * Sadece konuşma/sohbet → itemsGained boş olabilir
  * itemId: ZORUNLU - Teknik ID, snake_case formatında "item_" prefix ile (örn: "item_forest_berries", "item_steel_sword")
  * name: ZORUNLU - İnsanların okuyacağı TEMIZ İSİM, prefix/underscore YOK, başlık formatı (örn: "Forest Berries", "Steel Sword", "Apple", "Devil Wood")
    🚨🚨🚨 TODO-G124: İSİMLENDİRME KURALLARI - MUTLAKA UYULMALI! 🚨🚨🚨
    ⚠️ KRİTİK: "name" field'ı itemId'den FARKLI OLMALI! itemId'yi name'e kopyalama!
    ⚠️ YASAK KARAKTERLER: name field'ında "_" (underscore), "-" (tire), "." (nokta), sayılar (001, 23) KULLANILAMAZ!
    ⚠️ FORMAT KURALI: Her kelime başı BÜYÜK HARF (Title Case)!

    ❌ YANLIŞ: {"itemId": "Item_item_apple", "name": "Item_item_apple"} → underscore ve prefix var!
    ❌ YANLIŞ: {"itemId": "item_apple", "name": "item_apple"} → underscore ve prefix var!
    ❌ YANLIŞ: {"itemId": "item_fire_dragon_001", "name": "fire_dragon_001"} → underscore ve sayı var!
    ❌ YANLIŞ: {"itemId": "item_goblin", "name": "goblin_warrior_23"} → underscore ve sayı var!
    ❌ YANLIŞ: {"itemId": "item_sword", "name": "iron.sword"} → nokta var!
    ❌ YANLIŞ: {"itemId": "item_potion", "name": "health-potion"} → tire var!

    ✅ DOĞRU: {"itemId": "item_apple", "name": "Apple"} → temiz, başlık formatı!
    ✅ DOĞRU: {"itemId": "item_devil_wood", "name": "Devil Wood"} → temiz, başlık formatı!
    ✅ DOĞRU: {"itemId": "item_fire_dragon_001", "name": "Fire Dragon"} → temiz, başlık formatı!
    ✅ DOĞRU: {"itemId": "item_goblin_warrior_23", "name": "Goblin Warrior"} → temiz, başlık formatı!
  * description: ZORUNLU - Kısa açıklama (örn: "Fresh wild berries", "A well-crafted sword")
  * type: ZORUNLU - Item tipi: "WEAPON", "ARMOR", "EQUIPMENT", "CONSUMABLE", "MATERIAL", "QUEST", "CURRENCY", "TRINKET", "MISC"
    - WEAPON: Silahlar (kılıç, balta, yay)
    - ARMOR: Zırhlar (göğüslük, kask, eldiven)
    - EQUIPMENT: Aksesuar (pelerin, yüzük, tılsım)
    - CONSUMABLE: Tüketilebilir (iksir, yiyecek, parşömen)
    - MATERIAL: Üretim malzemesi (cevher, odun, deri)
    - QUEST: Görev eşyası (anahtar hikaye öğeleri)
    - CURRENCY: Para (altın, özel taşlar)
    - TRINKET: Stat bonus süs eşyası
    - MISC: Diğer
  * rarity: ZORUNLU - Nadirlik: "COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC", "ARTIFACT", "UMBROS_CURSE"
    - COMMON: Sıradan eşyalar (iksir, ekmek, basit silahlar)
    - UNCOMMON: Nadiren bulunan (kaliteli silahlar, zırhlar)
    - RARE: Nadir eşyalar (büyülü eşyalar, özel materyaller)
    - EPIC: Epik eşyalar (güçlü büyülü silahlar, zırh setleri)
    - LEGENDARY: Efsanevi eşyalar (usta yapımı, benzersiz güçler)
    - MYTHIC: Mitik eşyalar (tanrısal güçte, çok nadir)
    - ARTIFACT: Artefakt seviyesi (tarihi, efsanevi öneme sahip)
    - UMBROS_CURSE: Lanetli eşyalar (sadece ölümden sonra veya kötülük yolunda)
  * iconEmoji: **ZORUNLU** - Item için emoji icon (G135.3: HER İTEM İÇİN MUTLAKA DOLDUR!)
    ⚠️ KRİTİK KURAL: Item'in türüne uygun TEK BİR emoji seç!

    **📦 EMOJI SEÇİM REHBERİ:**
    **WEAPON (Silahlar):**
    - Sword/Kılıç → "⚔️" veya "🗡️"
    - Axe/Balta → "🪓"
    - Bow/Yay → "🏹"
    - Staff/Asa → "🪄" veya "✨"
    - Dagger/Hançer → "🗡️"
    - Shield/Kalkan → "🛡️"

    **ARMOR (Zırhlar):**
    - Helmet/Miğfer → "⛑️" veya "👑"
    - Chest Armor/Göğüs Zırhı → "🦺"
    - Boots/Botlar → "👢" veya "👞"
    - Gloves/Eldivenler → "🧤"

    **CONSUMABLE (Tüketilebilir):**
    - Food/Yiyecek → "🍎" "🍞" "🍖" "🥕"
    - Potion/İksir → "🧪" "⚗️"
    - Drink/İçecek → "🍺" "🧃"

    **MATERIAL (Materyaller):**
    - Metal/Maden → "⚙️" "🔩"
    - Wood/Ahşap → "🪵"
    - Gem/Mücevher → "💎" "💠"
    - Herb/Ot → "🌿" "🌱"

    **TRINKET (Aksesuarlar):**
    - Ring/Yüzük → "💍"
    - Necklace/Kolye → "📿"
    - Crystal/Kristal → "🔮"

    **QUEST/MISC:**
    - Quest Item/Görev Eşyası → "📜" "🗝️" "📦"
    - Currency/Para → "💰" "🪙"
    - Book/Kitap → "📖" "📚"

    ❌ YANLIŞ: iconEmoji yok veya boş
    ✅ DOĞRU: {"name": "Iron Sword", "type": "WEAPON", "iconEmoji": "⚔️"}
    ✅ DOĞRU: {"name": "Health Potion", "type": "CONSUMABLE", "iconEmoji": "🧪"}
    ✅ DOĞRU: {"name": "Gold Ring", "type": "EQUIPMENT", "iconEmoji": "💍"}

  * equipmentSlot: **ZORUNLU** - Ekipman slot'u (G135.6: HER İTEM TİPİ İÇİN MUTLAKA DOLDUR!)
    ⚠️ KRİTİK KURAL: Type'a göre DOĞRU slot seç, BOŞ BIRAKMA!

    **WEAPON tipi için:**
    - Sword, Axe, Mace, Staff, Bow → "MAIN_HAND"
    - Dagger, Short Sword, Light Weapon → "OFF_HAND" veya "MAIN_HAND"
    - Shield → "OFF_HAND"
    ❌ YANLIŞ: {"type": "WEAPON", "equipmentSlot": "NONE"}
    ✅ DOĞRU: {"type": "WEAPON", "name": "Iron Sword", "equipmentSlot": "MAIN_HAND"}

    **ARMOR tipi için:**
    - Helmet, Hat, Crown → "HEAD"
    - Chestplate, Armor, Tunic → "CHEST"
    - Pants, Leggings → "LEGS"
    - Gloves → "GLOVES"
    - Boots, Shoes → "BOOTS"
    ❌ YANLIŞ: {"type": "ARMOR", "equipmentSlot": ""}
    ✅ DOĞRU: {"type": "ARMOR", "name": "Iron Helmet", "equipmentSlot": "HEAD"}

    **EQUIPMENT/TRINKET tipi için:**
    - Ring → "RING_1" (veya "RING_2" if player has multiple)
    - Necklace, Amulet → "NECK"
    - Earring → "EARRING_1" (veya "EARRING_2")
    - Cape, Cloak (Aura boost) → "AURA"
    - Ancient Artifact → "RELIC_1" (veya "RELIC_2")
    - Pet Companion → "PET"
    ✅ DOĞRU: {"type": "EQUIPMENT", "name": "Silver Ring", "equipmentSlot": "RING_1"}

    **CONSUMABLE/MATERIAL/QUEST/CURRENCY/MISC tipi için:**
    - MUTLAKA "NONE" kullan (ekiplanamaz item'lar)
    ✅ DOĞRU: {"type": "CONSUMABLE", "name": "Apple", "equipmentSlot": "NONE"}
    ✅ DOĞRU: {"type": "MATERIAL", "name": "Iron Ore", "equipmentSlot": "NONE"}

    **📋 HIZLI REFERANS:**
    - MAIN_HAND: Kılıç, Balta, Mızrak, Asa, Yay (büyük silahlar)
    - OFF_HAND: Kalkan, Hançer, İkinci Silah (küçük silahlar)
    - HEAD: Miğfer, Şapka, Taç
    - CHEST: Zırh, Gömlek, Ceket
    - LEGS: Pantolon, Etek
    - GLOVES: Eldivenler
    - BOOTS: Botlar, Sandaletler
    - RING_1, RING_2: Yüzükler
    - EARRING_1, EARRING_2: Küpeler
    - NECK: Kolye, Kemik
    - AURA: Pelerin, Büyülü Aura
    - RELIC_1, RELIC_2: Antik Kalıntılar
    - PET: Evcil Hayvan
    - NONE: Tüketilebilir, Materyal, Quest Item, Para, Diğer
  * statBonuses: İSTEĞE BAĞLI - Ekipman stat bonusları (G135: Sadece WEAPON, ARMOR, EQUIPMENT tipinde kullan!)
    ⚠️ KRİTİK: CONSUMABLE tipinde statBonuses DEĞİL, consumableEffects kullan!
    - Format: {"STR_FLAT": 5, "AGI_FLAT": 3, "PHYSICAL_ATTACK": 10}
    - Kullanılabilir stat key'leri: STR_FLAT, AGI_FLAT, INT_FLAT, VIT_FLAT, SPIRIT, LUCK, PHYSICAL_ATTACK, PHYSICAL_DEFENSE, MAGIC_ATTACK, MAGIC_DEFENSE
    - Örnek: {"itemId": "item_iron_sword", "name": "Iron Sword", "type": "WEAPON", "statBonuses": {"STR_FLAT": 5, "PHYSICAL_ATTACK": 10}, ...}
  * consumableEffects: İSTEĞE BAĞLI - Tüketilebilir eşya etkileri (G135: SADECE CONSUMABLE tipinde kullan!)
    ⚠️ KRİTİK KURAL: CONSUMABLE tipindeki item'larda MUTLAKA consumableEffects field'ı DOLDUR!
    - Format: {"hungerRestore": 20, "healthRestore": 50}
    - Kullanılabilir effect key'leri:
      * "hungerRestore": Açlık barı restore (0-100, küçük yiyecekler: 10-20, büyük yemekler: 30-50)
      * "healthRestore": Can restore (HP, küçük iksir: 20-50, büyük iksir: 100-200)
      * "manaRestore": Mana restore (MP, küçük: 20-30, büyük: 50-100)
      * "staminaRestore": Stamina restore (küçük: 20-30, büyük: 50-80)
      * "fatigueReduce": Yorgunluk azaltma (0-50, dinlenme için)
    - ⚠️ BOŞ BIRAKMA: consumableEffects boş {} ise item hiçbir etki yapmaz!
    - Örnek Apple: {"itemId": "item_apple", "name": "Apple", "type": "CONSUMABLE", "consumableEffects": {"hungerRestore": 20}, ...}
    - Örnek Health Potion: {"itemId": "item_health_potion", "name": "Health Potion", "type": "CONSUMABLE", "consumableEffects": {"healthRestore": 50}, ...}
    - Örnek Mana Potion: {"itemId": "item_mana_potion", "name": "Mana Potion", "type": "CONSUMABLE", "consumableEffects": {"manaRestore": 30}, ...}
    - Örnek Büyük Yemek: {"itemId": "item_feast", "name": "Feast", "type": "CONSUMABLE", "consumableEffects": {"hungerRestore": 50, "healthRestore": 30, "fatigueReduce": 20}, ...}

- questsGained: Oyuncunun YENİ KAZANDIĞI görevler (ZORUNLU: questId, title, description, giver)
  ⚠️ KRİTİK: Sadece hikayede NPC görev veriyorsa veya yeni keşif görevi tetikleniyorsa kullan!
  * questId: ZORUNLU - "quest_" ile başlamalı (örn: "quest_find_lost_cat", "quest_explore_ruins")
  * title: ZORUNLU - Görevin başlığı (örn: "Find the Lost Cat", "Keşfedilmemiş Harabeleri Araştır")
    🚨🚨🚨 TODO-G124: QUEST İSİMLENDİRME KURALLARI 🚨🚨🚨
    ⚠️ YASAK KARAKTERLER: title field'ında "_" (underscore), "." (nokta) KULLANILAMAZ!
    ⚠️ FORMAT KURALI: Her kelime başı BÜYÜK HARF (Title Case), temiz ve okunabilir olmalı!
    ❌ YANLIŞ: "quest_find_cat" → underscore var!
    ❌ YANLIŞ: "find.lost.cat" → nokta var!
    ✅ DOĞRU: "Find the Lost Cat" → temiz, başlık formatı!
  * description: ZORUNLU - Görevin açıklaması (örn: "Village elder's cat went missing in the dark forest")
  * giver: ZORUNLU - Görevi veren (örn: "Village Elder", "Mysterious Trader", "Ancient Inscription")
    🚨 TODO-G124: giver field'ı da temiz ve başlık formatında olmalı!
    ❌ YANLIŞ: "village_elder_001" → underscore ve sayı var!
    ✅ DOĞRU: "Village Elder" → temiz!
  * dueDay: İsteğe bağlı - Görevin bitiş günü (-1 = süresiz) (örn: currentDay + 3 for time-limited quests)
  * rewards: İsteğe bağlı - Ödüller listesi (örn: ["50 Gold", "Iron Sword", "Experience Points"])

  ✅ G136: QUEST ÖRNEKLERİ (8 ADET):
  1. Main Quest: {"questId": "quest_defeat_dark_lord", "title": "Defeat the Dark Lord", "description": "The Dark Lord threatens the kingdom. Stop him!", "giver": "King", "dueDay": -1, "rewards": ["Legendary Sword", "5000 Gold", "Royal Title"]}
  2. Side Quest: {"questId": "quest_find_lost_cat", "title": "Find the Lost Cat", "description": "Village elder's cat went missing in the dark forest", "giver": "Village Elder", "dueDay": 7, "rewards": ["50 Gold", "Healing Potion"]}
  3. Fetch Quest: {"questId": "quest_collect_herbs", "title": "Collect Healing Herbs", "description": "Gather 10 healing herbs from the forest", "giver": "Alchemist", "dueDay": 5, "rewards": ["Mana Potion", "Alchemy Recipe"]}
  4. Kill Quest: {"questId": "quest_slay_bandits", "title": "Slay the Bandits", "description": "Clear the mountain pass of bandit raiders", "giver": "Guard Captain", "dueDay": -1, "rewards": ["Iron Armor", "200 Gold"]}
  5. Rescue Quest: {"questId": "quest_rescue_merchant", "title": "Rescue the Merchant", "description": "A merchant is trapped in the goblin caves", "giver": "Merchant Guild", "dueDay": 3, "rewards": ["Rare Gemstone", "500 Gold"]}
  6. Exploration Quest: {"questId": "quest_explore_ruins", "title": "Explore Ancient Ruins", "description": "Investigate the mysterious ruins discovered nearby", "giver": "Scholar", "dueDay": -1, "rewards": ["Ancient Artifact", "Experience"]}
  7. Crafting Quest: {"questId": "quest_craft_enchanted_blade", "title": "Craft an Enchanted Blade", "description": "Forge a sword using magical crystals", "giver": "Blacksmith", "dueDay": 10, "rewards": ["Master Craftsman Title", "300 Gold"]}
  8. Investigation Quest: {"questId": "quest_solve_mystery", "title": "Solve the Village Mystery", "description": "Strange events are happening. Find out why", "giver": "Village Chief", "dueDay": 7, "rewards": ["Detective Badge", "Reputation"]}

  QUEST OLUŞTURMA KRİTERLERİ:
  1. NPC BELİRGİN BİR GÖREV VERİYORSA → questsGained kullan
     Örnek: "Can you find my lost book in the forest?" → questsGained: [{"questId": "quest_find_book", "title": "Find the Lost Book", ...}]
  2. Oyuncu YENİ BİR YER KEŞFEDİYORSA → questsGained kullan
     Örnek: "You discover ancient ruins" → questsGained: [{"questId": "quest_explore_ruins", "title": "Explore the Ancient Ruins", ...}]
  3. HİKAYE PROGRESSION GEREKTİRİYORSA → questsGained kullan
     Örnek: Main story milestone → questsGained: [{"questId": "quest_defeat_dark_lord", "title": "Defeat the Dark Lord", ...}]
  4. DİNAMİK OLAY TETİKLENİYORSA → questsGained kullan
     Örnek: "A dragon attacks the village!" → questsGained: [{"questId": "quest_dragon_attack", "title": "Defend Against Dragon", ...}]

  ⚠️ QUEST OLUŞTURMAYIN: Sadece sohbet, bilgi alışverişi, item toplama (görev olmadan)

- questsUpdated: Var olan görevlerin ilerleme durumu güncelleniyor (boş liste de olabilir)
  Örnek: Oyuncu quest_find_book görevinde bir ipucu buldu → questsUpdated: ["quest_find_book"]

- statsChanged: Değişen karakteristikler
  ⚠️ KRİTİK: Oyuncu FİZİKSEL veya ZİHİNSEL aktivite yaptıysa MUTLAKA stat değişikliği VER!
  • Antrenman/Egzersiz → {"strength": 1-5, "experience": 20-100, "stamina": 10-30}
  • Savaş/Dövüş → {"experience": 50-200, "strength": 1-5, "agility": 1-3}
  • Mana/Qi Kültivasyonu → {"intelligence": 1-5, "spirit": 1-5, "mana": 20-100}
  • Büyü Öğrenme → {"intelligence": 1-3, "experience": 30-80}
  • Crafting/Yaratma → {"experience": 10-50}
  • Skill Pratiği → {"experience": 15-60}
  • Keşif/Gezme → {"experience": 5-30}
  NOT: Sadece konuşma/dinleme gibi PASIF aktivitelerde statsChanged BOŞ olabilir.
- weatherChange: Hava durumunu değiştir (isteğe bağlı): "RAINY", "FOGGY", "SUNNY", "STORMY", "SNOWY", "CLOUDY", "WINDY", "HOT", "COLD", "BLIZZARD"

🚨🚨🚨 G147: TIME PROGRESSION (HER ENTRY'DE ZORUNLU!) 🚨🚨🚨
- timeShift: Zaman dilimini ilerlet (ZORUNLU - her aksiyon zaman geçirir!): "MORNING", "NOON", "AFTERNOON", "EVENING", "NIGHT"
- dateChange: Gün sayısını ilerlet (opsiyonel, genelde uyuma/uzun yolculuk): 1, 2, 7, etc.

**CURRENT TIME: Day %CURRENT_DAY%, %CURRENT_TIME%**

⚠️ KRİTİK KURALLAR:
1. **HER AKSIYON** zaman ilerletmelidir! timeShift MUTLAKA kullan!
2. **Zaman ilerlemesi** hikayeye mantıklı olmalı
3. **Günlük döngü:** MORNING → NOON → AFTERNOON → EVENING → NIGHT (5 period)
4. **Uyuma/Dinlenme:** dateChange: 1 (bir gün ilerlet) + timeShift: "MORNING"
5. **Uzun yolculuk:** dateChange: N gün + timeShift: uygun zaman

**ÖRNEKLER:**

1. **Kısa aksiyon** (+1 period):
   - "I explore the cave" → timeShift: "AFTERNOON" (MORNING → AFTERNOON, 1 period atla)
   - "I talk to merchant" → timeShift: "NOON" (MORNING → NOON, 1 period)
   - "I craft a sword" → timeShift: "EVENING" (AFTERNOON → EVENING, 1 period)

2. **Orta aksiyon** (+2 period):
   - "I fight goblins" → timeShift: "EVENING" (MORNING → EVENING, 3 period atla)
   - "I hunt in forest" → timeShift: "NIGHT" (AFTERNOON → NIGHT, 2 period)

3. **Uzun aksiyon** (günü bitir):
   - "I travel to next town" → timeShift: "NIGHT" (mevcut günü bitir)

4. **Uyuma/Dinlenme** (+1 gün):
   - "I sleep at inn" → dateChange: 1, timeShift: "MORNING"
   - "I rest for the night" → dateChange: 1, timeShift: "MORNING"

5. **Zaman atlaması** (+N gün):
   - "I rest for a week" → dateChange: 7, timeShift: "MORNING"
   - "Three days later..." → dateChange: 3, timeShift: mevcut saati koru veya MORNING

6. **Gece aksiyonu** (geceleyin):
   - "I sneak at night" → timeShift: "NIGHT" (eğer gündüzse)
   - Night'ta action → timeShift değiştirme veya dateChange: 1 + timeShift: "MORNING" (sabah olsun)

**HATA ÖRNEKLERİ:**
❌ "I explore cave" → timeShift: null (HATA - zaman ilerlemedi!)
❌ "I sleep" → timeShift: "NIGHT" (HATA - uyuma +1 gün olmalı!)
❌ timeShift: "DEEP_NIGHT" (HATA - sadece 5 period var: MORNING, NOON, AFTERNOON, EVENING, NIGHT)

🗺️ G145: LOCATION DISCOVERY - CRITICAL FOR EXPLORATION!
═══════════════════════════════════════════════════════════════

- locationsUnlocked: Yeni lokasyonların kilidini aç (🚨 FREQUENTLY USE!): ["dark_forest", "ancient_ruins", "hidden_temple"]

  ⚠️⚠️⚠️ WHEN TO USE (USE OFTEN!) ⚠️⚠️⚠️
  * TODO-G114 & G145: LOCATION DISCOVERY SYSTEM - Use when player discovers new places!
    - Trigger conditions:
      * Player explores unknown areas (walks into fog-of-war zones)
      * Player finds clues/maps/directions to hidden locations
      * Player completes discovery quests or talks to locals about places
      * Player unlocks location through story progression
    - ✅ G145: LOCATION DISCOVERY EXAMPLES (15+ EXAMPLES):
      1. Player explores forest edge → locationsUnlocked: ["Dark Forest Entrance"]
      2. Player finds ancient map → locationsUnlocked: ["Lost Temple", "Ancient Ruins"]
      3. Villager tells about cave → locationsUnlocked: ["Hidden Cave"]
      4. Player climbs mountain → locationsUnlocked: ["Mountain Peak", "Eagle Nest"]
      5. Player discovers ruins → locationsUnlocked: ["Forgotten Ruins", "Mysterious Portal"]
      6. NPC mentions secret place → locationsUnlocked: ["Secret Grove", "Bandit Hideout"]
      7. Player finds treasure map → locationsUnlocked: ["Treasure Island", "Buried Temple"]
      8. Player follows trail → locationsUnlocked: ["Goblin Camp", "Wolf Den"]
      9. Player unlocks story location → locationsUnlocked: ["Dark Lord Castle", "Final Dungeon"]
      10. Player explores coast → locationsUnlocked: ["Pirate Cove", "Lighthouse"]
      11. Player enters city → locationsUnlocked: ["Market District", "Castle District", "Slums"]
      12. Player finds portal → locationsUnlocked: ["Demon Realm", "Spirit World"]
      13. Player discovers cave system → locationsUnlocked: ["Crystal Cave", "Underground River", "Lava Chamber"]
      14. Player climbs tower → locationsUnlocked: ["Wizard Tower", "Observatory"]
      15. Player follows quest → locationsUnlocked: ["Dragon Lair", "Ancient Library"]

    - 🚨 NAMING RULES (G145):
      * ✅ CORRECT: "Dark Forest", "Crystal Cave", "Dragon Lair" (Title Case, NO underscores)
      * ❌ WRONG: "dark_forest", "crystal_cave_entrance", "dragon_lair_001" (underscores, metadata format)

    - ALWAYS unlock locations when player discovers them organically through gameplay
    - Multiple locations can be unlocked at once (array format)
    - USE FREQUENTLY: Every 5-10 entries should have at least 1 new location
- locationChange: Oyuncuyu otomatik olarak başka bir lokasyona taşı (isteğe bağlı): "mysterious_cave"
- npcStateChange: Change NPC state (optional): {"npcId": "guard_001", "loyaltyChange": -10, "memoryAdded": {...}}
  * TODO-G101: NEW NPC ENCOUNTER - ALWAYS USE npcStateChange!
    - When player meets/talks to a NEW NPC, ALWAYS add npcStateChange

    🚨🚨🚨 G142: NPC NAMING RULES (CRITICAL!) 🚨🚨🚨
    ⚠️ **ZORUNLU NAMING FORMAT:**
    - ❌ NO underscores (_): "goblin_warrior" → WRONG!
    - ❌ NO numbers: "goblin1", "guard_23" → WRONG!
    - ❌ NO special chars: "knight.arthur", "trader@shop" → WRONG!
    - ✅ USE Title Case: "Goblin Warrior", "Merchant Bob" → CORRECT!
    - ✅ USE fantasy-appropriate names: "Guard John", "Knight Arthur" → CORRECT!
    - ✅ USE descriptive + unique: "Scarred Goblin", "Old Merchant" → CORRECT!

    **CORRECT EXAMPLES:**
      * Player talks to goblin → npcId: "Scarred Goblin"
      * Player bargains with trader → npcId: "Merchant Bob"
      * Player chats with guard → npcId: "Guard John"
      * Player fights bandit → npcId: "Bandit Leader"
      * Player meets elf → npcId: "Elven Scout"

    **STORY-SPECIFIC NAMING:**
      * "The Count of Monte Cristo" → French names: "Captain Morrel", "Abbé Faria", "Guard Jacques"
      * Fantasy setting → Descriptive: "Goblin Chieftain", "Ancient Wizard", "Forest Ranger"

    - First encounter: loyaltyChange: 0 (neutral start)
    - Positive interaction: loyaltyChange: +5 or +10
    - Negative interaction: loyaltyChange: -10 or -20
    🚨🚨🚨 TODO-G124: NPC STATE NAMING KURALLARI 🚨🚨🚨
    ⚠️ personalityTraitAdded: Temiz, Title Case formatında, underscore/sayı YOK!
    ⚠️ locationChange: Temiz, Title Case formatında, underscore/sayı YOK!
    ⚠️ skillsLearned: Array içindeki her skill temiz, Title Case formatında!
    ⚠️ titlesGained: Array içindeki her title temiz, Title Case formatında!
    ⚠️ badgesEarned: Array içindeki her badge temiz, Title Case formatında!
    ❌ YANLIŞ: "personalityTraitAdded": "brave_warrior_23" → underscore ve sayı var!
    ❌ YANLIŞ: "locationChange": "dark_forest_001" → underscore ve sayı var!
    ❌ YANLIŞ: "skillsLearned": ["fire_magic_lv5", "sword.master"] → underscore, sayı, nokta var!
    ✅ DOĞRU: "personalityTraitAdded": "Brave Warrior" → temiz, Title Case!
    ✅ DOĞRU: "locationChange": "Dark Forest" → temiz, Title Case!
    ✅ DOĞRU: "skillsLearned": ["Fire Magic", "Sword Master"] → temiz, Title Case!

  ✅ G136: NPC STATE CHANGE ÖRNEKLERİ (6 ADET):
  1. Merchant NPC: {"npcId": "Merchant Bob", "relationshipChange": 5, "loyaltyChange": 10, "memoryAdded": {"eventType": "TRADE", "eventDescription": "Player bought items", "emotionalImpact": "POSITIVE"}}
  2. Guard NPC: {"npcId": "Guard John", "relationshipChange": -10, "loyaltyChange": -20, "memoryAdded": {"eventType": "CONFRONTATION", "eventDescription": "Player refused to pay toll", "emotionalImpact": "NEGATIVE"}}
  3. Quest Giver NPC: {"npcId": "Village Elder", "relationshipChange": 15, "skillsLearned": ["Herbalism"], "memoryAdded": {"eventType": "QUEST_GIVEN", "eventDescription": "Gave player quest to find herbs", "emotionalImpact": "NEUTRAL"}}
  4. Enemy NPC: {"npcId": "Bandit Leader", "relationshipChange": -50, "loyaltyChange": -100, "memoryAdded": {"eventType": "COMBAT", "eventDescription": "Player attacked", "emotionalImpact": "HOSTILE"}}
  5. Ally NPC: {"npcId": "Knight Arthur", "relationshipChange": 20, "titlesGained": ["Knight's Friend"], "memoryAdded": {"eventType": "ALLIANCE", "eventDescription": "Player helped in battle", "emotionalImpact": "POSITIVE"}}
  6. Neutral NPC: {"npcId": "Forest Hermit", "relationshipChange": 0, "personalityTraitAdded": "Reclusive", "memoryAdded": {"eventType": "MEETING", "eventDescription": "First encounter", "emotionalImpact": "NEUTRAL"}}

    **G126: Skill İcon Emoji - ZORUNLU!**
    ⚠️ Her skill için `iconEmoji` field'ı ekle!

    **📦 SKILL EMOJI REHBERİ:**
    **COMBAT (Savaş Yetenekleri):**
    - Sword Skills/Kılıç → "⚔️" veya "🗡️"
    - Axe Skills/Balta → "🪓"
    - Bow Skills/Yay → "🏹"
    - Martial Arts/Dövüş → "🥋" veya "👊"
    - Shield Block/Kalkan → "🛡️"

    **MAGIC (Büyü Yetenekleri):**
    - Fire Magic/Ateş → "🔥"
    - Ice Magic/Buz → "❄️" veya "🧊"
    - Lightning/Şimşek → "⚡"
    - Healing/İyileştirme → "💚" veya "✨"
    - Dark Magic/Karanlık → "🌑" veya "👿"
    - Holy Magic/Kutsal → "✨" veya "🌟"
    - Teleportation/Işınlanma → "💫"

    **CRAFTING (Zanaat Yetenekleri):**
    - Blacksmithing/Demircilik → "⚒️"
    - Alchemy/Simya → "🧪"
    - Cooking/Yemek → "🍳"
    - Herbalism/Şifahane → "🌿"

    **UTILITY (Yardımcı Yetenekleri):**
    - Lockpicking/Kilit Açma → "🔓"
    - Stealth/Gizlenme → "👻"
    - Tracking/İz Sürme → "👣"
    - Diplomacy/Diplomasi → "🤝"

    ✅ ÖRNEK: {"skill": "Fire Magic", "iconEmoji": "🔥"}
    ✅ ÖRNEK: {"skill": "Sword Master", "iconEmoji": "⚔️"}
- nemesisEvolution: Turn NPC into Nemesis (optional): {"npcId": "merchant_005", "newNemesisLevel": 2, "vendettaReason": "Betrayed"}
  * TODO-G100: NEMESIS SYSTEM - Use when player severely wrongs an NPC!
    - Trigger conditions:
      * Player betrays NPC (breaks promise, steals, lies)
      * Player kills NPC's friend/family
      * Player repeatedly insults/attacks NPC
      * Player ruins NPC's business/life
    - Examples:
      * Player kills merchant's son → nemesisEvolution: {"npcId": "merchant_john", "newNemesisLevel": 3, "vendettaReason": "Killed my son"}
      * Player steals from blacksmith → nemesisEvolution: {"npcId": "blacksmith_tom", "newNemesisLevel": 1, "vendettaReason": "Stole my tools"}
      * Player betrays guild master → nemesisEvolution: {"npcId": "guild_master", "newNemesisLevel": 4, "vendettaReason": "Betrayed the guild"}
    - newNemesisLevel: 1 (minor grudge) to 5 (sworn enemy)
    - Nemesis will hunt player, seek revenge, create obstacles
    🚨🚨🚨 TODO-G124: NEMESIS NAMING KURALLARI 🚨🚨🚨
    ⚠️ vendettaReason ve evolutionDescription: Kullanıcıya gösterilecek metinler, temiz ve okunabilir olmalı!
    ⚠️ adaptationTraits: Array içindeki her trait temiz, Title Case formatında olmalı!
    ❌ YANLIŞ: "vendettaReason": "player_killed_son_001" → underscore ve sayı var!
    ❌ YANLIŞ: "adaptationTraits": ["fire_resistant_23", "magic.shield"] → underscore, sayı, nokta var!
    ✅ DOĞRU: "vendettaReason": "Killed my son in cold blood." → temiz, okunabilir!
    ✅ DOĞRU: "adaptationTraits": ["Fire Resistant", "Magic Shield"] → temiz, Title Case!
- structuredActions: Oyun mekaniklerini tetikle (G81 - Combat System, G113 - Status Effects, G109 - DiceSystem):
  * START_COMBAT: Savaş başlat → [{"actionType": "START_COMBAT", "parameters": {"enemyIds": "Bandit Leader,Bandit Scout"}}]
    🚨🚨🚨 G142 + G144: ENEMY NAMING RULES (CRITICAL!) 🚨🚨🚨
    **CURRENT LANGUAGE: %CURRENT_LANGUAGE%** (Turkish / English)

    🚨🚨🚨 G142: DISPLAY NAME ZORUNLU FORMAT 🚨🚨🚨
    ⚠️ **enemyIds MUST use displayName (Title Case, NO underscores/numbers):**
    - ❌ WRONG: "bandit_1,goblin_2" (metadata format)
    - ❌ WRONG: "enemy_goblin_1" (underscore, metadata)
    - ✅ CORRECT: "Bandit Leader,Goblin Warrior" (Title Case, descriptive)
    - ✅ CORRECT: "Fire Dragon,Ice Troll" (Title Case, unique names)

    ⚠️ KRİTİK: enemies array'indeki HER ENEMY için "displayName" field'ı ZORUNLU!
    ⚠️ displayName: Kullanıcıya gösterilecek isim, MUTLAKA mevcut dile göre üretilmeli!
    ⚠️ displayName FORMAT: Title Case, NO underscores, NO numbers!

    **TÜRKÇE DİL (TR):**
    ❌ YANLIŞ: "displayName": "Goblin" (İngilizce)
    ❌ YANLIŞ: "displayName": "enemy_goblin_1" (Metadata format)
    ✅ DOĞRU: "displayName": "Goblin" (Türkçe'de aynı)
    ✅ DOĞRU: "displayName": "Ateş Ejderhası" (Fire Dragon → Türkçe)
    ✅ DOĞRU: "displayName": "Karanlık Şövalye" (Dark Knight → Türkçe)

    **İNGİLİZCE DİL (EN):**
    ✅ DOĞRU: "displayName": "Goblin"
    ✅ DOĞRU: "displayName": "Fire Dragon"
    ✅ DOĞRU: "displayName": "Dark Knight"

    **ÖRNEK ENEMY GENERATION:**
    ```json
    "enemies": [
      {
        "id": "enemy_001",
        "name": "enemy_goblin_1",
        "displayName": "Goblin",  // TR: "Goblin", EN: "Goblin"
        "hp": 100,
        "maxHp": 100,
        "attack": 15,
        "defense": 10
      },
      {
        "id": "enemy_002",
        "name": "enemy_fire_dragon_1",
        "displayName": "Ateş Ejderhası",  // TR: "Ateş Ejderhası", EN: "Fire Dragon"
        "hp": 500,
        "maxHp": 500,
        "attack": 50,
        "defense": 30
      }
    ]
    ```
  * END_COMBAT: Savaşı bitir → [{"actionType": "END_COMBAT", "parameters": {}}]
  * UPDATE_ENEMY_HEALTH: Düşman canını güncelle → [{"actionType": "UPDATE_ENEMY_HEALTH", "parameters": {"enemyId": "bandit_1", "damage": "25"}}]
  * APPLY_STATUS_EFFECT: Oyuncuya buff/debuff uygula (G113.1) → [{"actionType": "APPLY_STATUS_EFFECT", "parameters": {"effectId": "poison", "magnitude": "5", "duration": "3"}}]
    - Available effectIds: "poison", "burn", "slow", "weakened" (DEBUFFS), "regeneration", "shield" (BUFFS)
    - magnitude: Effect gücü (opsiyonel, default: effectId'ye göre değişir)
    - duration: Kaç turn süreceği (opsiyonel, default: effectId'ye göre değişir)
  * TODO-G103: DICE_ROLL: Zar atma testi (G111 - DiceSystem) → MUTLAKA RİSKLİ EYLEMLERDE KULLAN!
    - Available checkTypes: "AGI_CHECK" (agility), "STR_CHECK" (strength), "INT_CHECK" (intelligence), "LUCK_CHECK" (luck)
    - ⚠️ KRİTİK: Oyuncu RİSKLİ bir şey yaptığında MUTLAKA DICE_ROLL kullan!
    - AGI_CHECK örnekleri: Tırmanma, atlama, tuzaktan kaçınma, gizlenme, kaçış, denge kurma
      * "Duvara tırmanıyorum" → [{"actionType": "DICE_ROLL", "parameters": {"checkType": "AGI_CHECK"}}]
      * "Trap'ten kaçıyorum" → [{"actionType": "DICE_ROLL", "parameters": {"checkType": "AGI_CHECK"}}]
    - STR_CHECK örnekleri: Kapı kırma, kaldırma, itme, güç gösterisi
      * "Kapıyı kırıyorum" → [{"actionType": "DICE_ROLL", "parameters": {"checkType": "STR_CHECK"}}]
      * "Kayayı kaldırıyorum" → [{"actionType": "DICE_ROLL", "parameters": {"checkType": "STR_CHECK"}}]
    - INT_CHECK örnekleri: Büyü çözme, bulmaca, strateji, bilgi hatırlama
      * "Şifreyi çözüyorum" → [{"actionType": "DICE_ROLL", "parameters": {"checkType": "INT_CHECK"}}]
      * "Antik metni okuyorum" → [{"actionType": "DICE_ROLL", "parameters": {"checkType": "INT_CHECK"}}]
    - LUCK_CHECK örnekleri: Şans gerektiren durumlar, tesadüf, bulma, talih
      * "Hazine aramaya çalışıyorum" → [{"actionType": "DICE_ROLL", "parameters": {"checkType": "LUCK_CHECK"}}]
      * "Rastgele bir yol seçiyorum" → [{"actionType": "DICE_ROLL", "parameters": {"checkType": "LUCK_CHECK"}}]
    - BAŞARI/BAŞARISIZLIK hikayeye yansıtılmalı:
      * Başarılı (roll >= 10): "Başarıyla tırmanıyorsun ve zirveye ulaşıyorsun!"
      * Başarısız (roll < 10): "Kayıyorsun ve düşüyorsun! -10 HP hasar alıyorsun."
  * TODO-G109.2: REST: Dinlenme (HP/MP/Stamina recovery) → [{"actionType": "REST", "parameters": {"hours": "8"}}]
    - hours: Kaç saat dinlenileceği (1-24 arası)
  * TODO-G115.3: LEARN_SPELL: Spell learning (player discovers/learns new spell) → [{"actionType": "LEARN_SPELL", "parameters": {"spellName": "Fireball"}}]
    - Use when player:
      * Finds spell scroll/book
      * Learns from NPC teacher/mentor
      * Discovers spell through experimentation
      * Unlocks spell through quest reward
    - Available spells: "Fireball", "Water Bolt", "Stone Spike", "Wind Slash", "Lightning Bolt", "Heal", "Shadow Bolt"
    - Examples:
      * Player reads ancient tome → [{"actionType": "LEARN_SPELL", "parameters": {"spellName": "Lightning Bolt"}}]
      * Wizard teaches player → [{"actionType": "LEARN_SPELL", "parameters": {"spellName": "Heal"}}]
      * Quest reward → [{"actionType": "LEARN_SPELL", "parameters": {"spellName": "Shadow Bolt"}}]
- statusEffectsApplied: Buff/Debuff uygula (G82 - Status Effect System - LEGACY, artık structuredActions kullan):
  * DEBUFFS (negatif): poison, burn, slow, weakened
  * BUFFS (pozitif): strength_buff, speed_buff, regeneration, shield
  * FORMAT: [{"effectId": "poison", "name": "Poison", "type": "DEBUFF", "magnitude": 5, "durationTurns": 3, "tickDamage": 50, "iconEmoji": "☠️"}]
  * **G126: iconEmoji - ZORUNLU!** Her status effect için uygun emoji seç:

    **📦 STATUS EFFECT EMOJI REHBERİ:**
    **DEBUFF (Negatif Etkiler):**
    - Poison/Zehir → "☠️" veya "🧪"
    - Burn/Yanma → "🔥"
    - Freeze/Donma → "❄️" veya "🧊"
    - Slow/Yavaşlama → "🐌"
    - Weakened/Zayıflama → "💀" veya "😵"
    - Bleed/Kanama → "🩸"
    - Curse/Lanet → "👿" veya "😈"
    - Stun/Sersemletme → "💫" veya "⭐"

    **BUFF (Pozitif Etkiler):**
    - Strength/Güç Artışı → "💪"
    - Speed/Hız Artışı → "⚡" veya "💨"
    - Regeneration/Yenilenme → "💚" veya "✨"
    - Shield/Kalkan → "🛡️"
    - Fire Resistance/Ateş Direnci → "🔥🛡️"
    - Holy Blessing/Kutsal Nimet → "✨" veya "🌟"
    - Invisibility/Görünmezlik → "👻"
    - Berserk/Öfke → "😡" veya "💢"

  * ÖRNEK Poison: {"effectId": "poison", "name": "Poison", "type": "DEBUFF", "magnitude": 5, "durationTurns": 3, "tickDamage": 50, "iconEmoji": "☠️"}
  * ÖRNEK Strength: {"effectId": "strength_buff", "name": "Strength Up", "type": "BUFF", "magnitude": 5, "durationTurns": 5, "statModifiers": {"strength": 50}, "iconEmoji": "💪"}

- moralityDelta: AI sentiment analysis (G105.1) - Oyuncunun ahlaki değişimi:
  * ⚠️ KRİTİK: MUTLAKA HER RESPONSE'DA DEĞERLENDİR!
  * Float değer: -1.0 (çok kötü) ile +1.0 (çok iyi) arası
  * İYİ DAVRANIŞLAR (+): yardım, kurtarma, koruma, iyileştirme, affetme, dürüstlük, cesaret
    - Köylüye/NPC'ye yardım → +0.08
    - Genel iyilik → +0.05
  * KÖTÜ DAVRANIŞLAR (-): hırsızlık, tehdit, şiddet, öldürme, yalan, zulüm, ihanet
    - İnsana/NPC'ye zarar → -0.10
    - Canavara/düşmana zarar → -0.02 (daha az penalty)
  * ÖRNEKLER (G136: 15 ADET):
    GOOD DEEDS (+0.05 to +0.15):
    1. "Köylü çocuğu kurtardım" → moralityDelta: 0.08
    2. "Yardıma koştum" → moralityDelta: 0.05
    3. "Yaralı birini iyileştirdim" → moralityDelta: 0.10
    4. "Fakirlere altın verdim" → moralityDelta: 0.12
    5. "Düşmanımı affettim" → moralityDelta: 0.15
    6. "Kayıp kediyi buldum" → moralityDelta: 0.06
    7. "Doğruyu söyledim (zor durum)" → moralityDelta: 0.08

    EVIL DEEDS (-0.05 to -0.15):
    8. "Tüccarı öldürdüm" → moralityDelta: -0.10
    9. "Köyden çaldım" → moralityDelta: -0.08
    10. "Masum birine zarar verdim" → moralityDelta: -0.12
    11. "Birini tehdit ettim" → moralityDelta: -0.06
    12. "İhanet ettim" → moralityDelta: -0.15

    NEUTRAL/COMBAT (-0.02 to +0.02):
    13. "Haydutları öldürdüm" → moralityDelta: -0.02
    14. "Canavarları avladım" → moralityDelta: 0.0
    15. "Sadece yürüdüm" → moralityDelta: 0.0

- REST MECHANICS (G112.6): Oyuncu "dinlen", "uyu", "kamp kur", "rest" dediğinde:
  * structuredActions kullan: [{"actionType": "REST", "parameters": {"hours": "1"}}] (default: 1 saat)
  * Uzun dinlenme: [{"actionType": "REST", "parameters": {"hours": "8"}}] (uyku)
  * Hikayede dinlenme süresi ve stamina/fatigue recovery'yi belirt
  * ÖRNEK: "Kamp ateşinin yanında 8 saat uyudun. Stamina ve yorgunluk tamamen yenilendi."

- DICE SYSTEM (G111.1): Riskli/belirsiz durumlarda zar at (skill check):
  * ZAR ATMALI DURUMLAR:
    - Tuzak atlatma, kapı kırma, kilit açma → Çeviklik (AGI)
    - Büyü yapma, bilmece çözme → Zeka (INT)
    - Kaya kaldırma, güreşme → Güç (STR)
    - İzleme, gizlenme, algılama → Algı (PER)
    - Kandırma, ikna etme → Karizma (CHA)
  * structuredActions kullan: [{"actionType": "DICE_ROLL", "parameters": {"actionType": "AGI_CHECK", "difficulty": "15", "advantage": "false"}}]
  * actionType değerleri: "AGI_CHECK", "INT_CHECK", "STR_CHECK", "PER_CHECK", "CHA_CHECK", "LUCK_CHECK"
  * difficulty: 5 (çok kolay), 8 (kolay), 12 (orta), 15 (zor), 18 (çok zor), 20 (efsanevi)
  * advantage/disadvantage: "true"/"false" (avantaj durumu)
  * BAŞARI: Hikayede "başarılı" olarak yaz, ödül ver
  * BAŞARISIZ: Hikayede "başarısız" olarak yaz, penalty ver (hasar, zaman kaybı, vb.)
  * ÖRNEK 1: "Kapıyı açmaya çalışıyorsun... [ZAR: 🎲 AGI check, DC 12] Başarılı! Kilit tık diye açıldı."
  * ÖRNEK 2: "Bilmeceyi çözmeye çalışıyorsun... [ZAR: 🧠 INT check, DC 15] Başarısız! Doğru cevabı bulamadın."

- ARCHETYPE TRACKING (G106.3): Oyuncunun aksiyonunu kategorize et:
  * ⚠️ KRİTİK: HER RESPONSE'DA MUTLAKA playerAction FIELD'INI DOLDUR!
  * playerAction değerleri: "ATTACK", "DEFEND", "WEAPON_USE", "HIDE", "TRACK", "STEALTH", "DISABLE_TRAP", "CAST_SPELL", "RITUAL", "CRAFT", "REPAIR", "OTHER"
  * WARRIOR (Savaşçı): Saldırı, savunma, silah kullanımı → "ATTACK", "DEFEND", "WEAPON_USE"
  * EXPLORER (Kaşif): Gizlenme, izleme, tuzak etkisizleştirme → "HIDE", "TRACK", "STEALTH", "DISABLE_TRAP"
  * MYSTIC (Mistik): Büyü yapma, ritüel → "CAST_SPELL", "RITUAL"
  * CRAFTSMAN (Zanaatkar): Üretme, onarma → "CRAFT", "REPAIR"
  * ÖRNEKLER:
    - "Haydutlara saldır" → playerAction: "ATTACK"
    - "Büyü yap" → playerAction: "CAST_SPELL"
    - "Gizlice yaklaş" → playerAction: "STEALTH"
    - "Kılıç üret" → playerAction: "CRAFT"
    - "Sadece yürü" → playerAction: "OTHER"

ÖRNEK SENARYOLAR:

🎯 QUEST EXAMPLES (USE questsGained FREQUENTLY!):
- NPC görev veriyor → questsGained: [{"questId": "quest_find_herbs", "title": "Şifalı Otlar Bul", "description": "Köy büyüğü şifalı otlar istiyor", "giver": "Village Elder", "dueDay": -1, "rewards": ["50 Gold", "Healing Potion"]}]
- Eski bir kitap bulma → questsGained: [{"questId": "quest_ancient_library", "title": "Explore the Ancient Library", "description": "The old book mentions a hidden library in the mountains", "giver": "Ancient Inscription", "dueDay": -1, "rewards": ["Wisdom", "Ancient Spell Scroll"]}]
- Kayıp kişi arama → questsGained: [{"questId": "quest_missing_child", "title": "Find the Missing Child", "description": "A desperate mother asks you to find her lost child in the dark woods", "giver": "Worried Mother", "dueDay": 3, "rewards": ["100 Gold", "Mother's Blessing"]}]
- Tehlikeli canavar avı → questsGained: [{"questId": "quest_dragon_threat", "title": "Slay the Dragon", "description": "A dragon has been terrorizing nearby villages", "giver": "Village Chief", "dueDay": -1, "rewards": ["500 Gold", "Dragon Scale Armor", "Hero Title"]}]
- Yeni yer keşfi → questsGained: [{"questId": "quest_explore_ruins", "title": "İnvestigate Ancient Ruins", "description": "You discovered mysterious ruins. What secrets do they hold?", "giver": "Exploration", "dueDay": -1, "rewards": ["Experience", "Ancient Artifact"]}]
- Tüccar görevi → questsGained: [{"questId": "quest_deliver_package", "title": "Deliver the Package", "description": "Merchant needs you to deliver a package to the next town", "giver": "Traveling Merchant", "dueDay": 2, "rewards": ["80 Gold", "Trade Discount"]}]
- Görev ilerledi → questsUpdated: ["quest_find_herbs"]

📦 ITEM EXAMPLES (G135.3 & G135.6: iconEmoji ve equipmentSlot ZORUNLU!):
⚠️ KRİTİK: "itemId" ve "name" field'ları FARKLI OLMALIDIR!
- itemId: Teknik ID (snake_case, "item_" prefix) → "item_forest_berries", "item_iron_helmet"
- name: İnsanların okuyacağı İSİM (başlık formatı, PREFIX YOK) → "Forest Berries", "Iron Helmet", "Orman Yemişleri"
⚠️ YANLIŞ ÖRNEK: {"itemId": "Item_item_apple", "name": "Item_item_apple"} ❌❌❌
✅ DOĞRU ÖRNEK: {"itemId": "item_apple", "name": "Apple", "iconEmoji": "🍎"}

⚠️ G135.3: iconEmoji HER ZAMAN DOLU OLMALI! Item türüne uygun emoji seç!
⚠️ G135.6: equipmentSlot HER ZAMAN DOLU OLMALI!
- WEAPON/ARMOR → Doğru slot'u seç (MAIN_HAND, HEAD, CHEST, vb.)
- CONSUMABLE/MATERIAL/QUEST/CURRENCY/MISC → "NONE" kullan

✅ WEAPON ÖRNEKLERİ (iconEmoji ZORUNLU!):
- Sandık açma (kılıç) → itemsGained: [
    {"itemId": "item_sword_of_light", "name": "Light's Edge", "description": "A legendary blade forged from pure light energy", "type": "WEAPON", "rarity": "LEGENDARY", "iconEmoji": "⚔️", "equipmentSlot": "MAIN_HAND", "statBonuses": {"STR_FLAT": 10, "PHYSICAL_ATTACK": 25}},
    {"itemId": "item_health_potion", "name": "Health Potion", "description": "Restores 50 HP when consumed", "type": "CONSUMABLE", "rarity": "COMMON", "iconEmoji": "🧪", "equipmentSlot": "NONE", "consumableEffects": {"healthRestore": 50}}
  ]
- Kötülük yolu (laneti kılıç) → itemsGained: [{"itemId": "item_cursed_blade", "name": "Cursed Blade", "description": "A dark weapon cursed by Umbros", "type": "WEAPON", "rarity": "UMBROS_CURSE", "iconEmoji": "🗡️", "equipmentSlot": "MAIN_HAND", "statBonuses": {"STR_FLAT": 15, "PHYSICAL_ATTACK": 30, "LUCK": -5}}]
- Hançer bulma → itemsGained: [{"itemId": "item_steel_dagger", "name": "Steel Dagger", "description": "A sharp steel dagger", "type": "WEAPON", "rarity": "COMMON", "iconEmoji": "🗡️", "equipmentSlot": "OFF_HAND", "statBonuses": {"PHYSICAL_ATTACK": 12, "AGI_FLAT": 3}}]
- Kalkan bulma → itemsGained: [{"itemId": "item_wooden_shield", "name": "Wooden Shield", "description": "A sturdy wooden shield", "type": "WEAPON", "rarity": "COMMON", "iconEmoji": "🛡️", "equipmentSlot": "OFF_HAND", "statBonuses": {"PHYSICAL_DEFENSE": 15, "VIT_FLAT": 2}}]

✅ ARMOR ÖRNEKLERİ (iconEmoji ZORUNLU!):
- Zırh bulma (miğfer) → itemsGained: [{"itemId": "item_iron_helmet", "name": "Iron Helmet", "description": "A sturdy iron helmet", "type": "ARMOR", "rarity": "COMMON", "iconEmoji": "⛑️", "equipmentSlot": "HEAD", "statBonuses": {"PHYSICAL_DEFENSE": 8, "VIT_FLAT": 2}}]
- Göğüslük bulma → itemsGained: [{"itemId": "item_leather_armor", "name": "Leather Armor", "description": "Light but protective leather armor", "type": "ARMOR", "rarity": "COMMON", "iconEmoji": "🦺", "equipmentSlot": "CHEST", "statBonuses": {"PHYSICAL_DEFENSE": 12, "AGI_FLAT": 1}}]
- Bot bulma → itemsGained: [{"itemId": "item_iron_boots", "name": "Iron Boots", "description": "Heavy iron boots", "type": "ARMOR", "rarity": "UNCOMMON", "iconEmoji": "👢", "equipmentSlot": "BOOTS", "statBonuses": {"PHYSICAL_DEFENSE": 6, "VIT_FLAT": 1}}]
- Eldiven bulma → itemsGained: [{"itemId": "item_cloth_gloves", "name": "Cloth Gloves", "description": "Soft cloth gloves", "type": "ARMOR", "rarity": "COMMON", "iconEmoji": "🧤", "equipmentSlot": "GLOVES", "statBonuses": {"INT_FLAT": 1}}]

✅ EQUIPMENT/TRINKET ÖRNEKLERİ (iconEmoji ZORUNLU!):
- Yüzük bulma → itemsGained: [{"itemId": "item_silver_ring", "name": "Silver Ring", "description": "A shiny silver ring", "type": "EQUIPMENT", "rarity": "UNCOMMON", "iconEmoji": "💍", "equipmentSlot": "RING_1", "statBonuses": {"LUCK": 5, "INT_FLAT": 2}}]
- Kolye bulma → itemsGained: [{"itemId": "item_ancient_amulet", "name": "Ancient Amulet", "description": "An amulet with mysterious powers", "type": "TRINKET", "rarity": "RARE", "iconEmoji": "📿", "equipmentSlot": "NECK", "statBonuses": {"SPIRIT_FLAT": 5, "MAGIC_ATTACK": 10}}]

✅ CONSUMABLE ÖRNEKLERİ (iconEmoji ve equipmentSlot: "NONE" ZORUNLU!):
- Orman keşfi → itemsGained: [{"itemId": "item_forest_berries", "name": "Orman Yemişleri", "description": "Taze toplanan yaban meyveleri", "type": "CONSUMABLE", "rarity": "COMMON", "iconEmoji": "🍇", "equipmentSlot": "NONE", "consumableEffects": {"hungerRestore": 15}}]
- Elma bulma → itemsGained: [{"itemId": "item_apple", "name": "Apple", "description": "A fresh red apple", "type": "CONSUMABLE", "rarity": "COMMON", "iconEmoji": "🍎", "equipmentSlot": "NONE", "consumableEffects": {"hungerRestore": 20}}]
- Büyük Yemek → itemsGained: [{"itemId": "item_feast", "name": "Feast", "description": "A hearty feast to restore energy", "type": "CONSUMABLE", "rarity": "UNCOMMON", "iconEmoji": "🍖", "equipmentSlot": "NONE", "consumableEffects": {"hungerRestore": 50, "healthRestore": 30, "fatigueReduce": 20}}]
- Mana İksiri → itemsGained: [{"itemId": "item_mana_potion", "name": "Mana Potion", "description": "Restores mana for spellcasting", "type": "CONSUMABLE", "rarity": "COMMON", "iconEmoji": "🧪", "equipmentSlot": "NONE", "consumableEffects": {"manaRestore": 30}}]

✅ MATERIAL/MISC ÖRNEKLERİ (iconEmoji ve equipmentSlot: "NONE" ZORUNLU!):
- Şeytan Ağacı → itemsGained: [{"itemId": "item_devil_wood", "name": "Devil Wood", "description": "Dark wood from cursed trees", "type": "MATERIAL", "rarity": "RARE", "iconEmoji": "🪵", "equipmentSlot": "NONE"}]
- Taş bulma → itemsGained: [{"itemId": "item_iron_ore", "name": "Iron Ore", "description": "Raw iron ore", "type": "MATERIAL", "rarity": "COMMON", "iconEmoji": "⚙️", "equipmentSlot": "NONE"}]

⚔️ COMBAT & STAT EXAMPLES:
- Düşman yenme → statsChanged: {"experience": 50, "strength": 1}
- Hasar alma → statsChanged: {"health": -10}
- Fırtına başlatma → weatherChange: "STORMY"
- Gece olma → timeShift: "NIGHT"
- Sisli havada gizlilik → weatherChange: "FOGGY"
- Sabah oluyor → timeShift: "MORNING"
- Yeni yer keşfi → locationsUnlocked: ["hidden_temple", "dark_forest"]
- Hikaye gereği seyahat → locationChange: "ancient_tower"
- Portal bulma → locationsUnlocked: ["demon_realm"], locationChange: "demon_realm"
- Kar fırtınası → weatherChange: "BLIZZARD"
- Düşman saldırısı → structuredActions: [{"actionType": "START_COMBAT", "parameters": {"enemyIds": "Bandit Leader,Bandit Scout"}}], statsChanged: {"experience": 0}
- Orman keşfi sırasında kurt sürüsü → structuredActions: [{"actionType": "START_COMBAT", "parameters": {"enemyIds": "Alpha Wolf,Forest Wolf,Young Wolf"}}]
- NPC'ye saldırma → structuredActions: [{"actionType": "START_COMBAT", "parameters": {"enemyIds": "Merchant Bob"}}], npcStateChange: {"npcId": "Merchant Bob", "loyaltyChange": -100}
- Zehirli yılan ısırması (G113.2 - YENİ) → structuredActions: [{"actionType": "APPLY_STATUS_EFFECT", "parameters": {"effectId": "poison", "magnitude": "5", "duration": "3"}}]
- Ateş büyüsü ile yanma (G113.2 - YENİ) → structuredActions: [{"actionType": "APPLY_STATUS_EFFECT", "parameters": {"effectId": "burn", "magnitude": "7", "duration": "2"}}]
- Şifa iksiri içme (G113.2 - YENİ) → structuredActions: [{"actionType": "APPLY_STATUS_EFFECT", "parameters": {"effectId": "regeneration"}}], statsChanged: {"health": 20}
- Kalkan büyüsü (G113.2 - YENİ) → structuredActions: [{"actionType": "APPLY_STATUS_EFFECT", "parameters": {"effectId": "shield", "magnitude": "10", "duration": "5"}}]
- Yavaşlatma laneti (G113.2 - YENİ) → structuredActions: [{"actionType": "APPLY_STATUS_EFFECT", "parameters": {"effectId": "slow", "duration": "4"}}]

🚨🚨🚨 G146: CRAFTING SYSTEM RULES (CRITICAL!) 🚨🚨🚨
⚠️ **WHEN PLAYER CRAFTS AN ITEM:**

**ZORUNLU KURALLAR:**
1. **itemsConsumed** field MUTLAKA doldur! (Input itemlar consume edilmeli)
2. **itemsGained** field MUTLAKA doldur! (Output item eklenmeli)
3. **statsChanged** ile Crafting skill'i artır!

**CRAFTING ÖRNEKLERİ:**
- Player: "I combine crystal rock and sword to craft enchanted sword"
  → itemsConsumed: ["Crystal Rock", "Sword"]
  → itemsGained: [{"itemId": "item_enchanted_sword", "name": "Enchanted Sword", "description": "A sword imbued with crystal magic", "type": "WEAPON", "rarity": "RARE", "iconEmoji": "⚔️", "equipmentSlot": "MAIN_HAND", "statBonuses": {"PHYSICAL_ATTACK": 35, "MAGIC_ATTACK": 15}}]
  → statsChanged: {"crafting_skill": 1}

- Player: "I craft healing potion using herbs and water"
  → itemsConsumed: ["Herbs", "Water"]
  → itemsGained: [{"itemId": "item_healing_potion", "name": "Healing Potion", "description": "Restores 50 HP", "type": "CONSUMABLE", "rarity": "COMMON", "iconEmoji": "🧪", "equipmentSlot": "NONE", "consumableEffects": {"healthRestore": 50}}]
  → statsChanged: {"alchemy_skill": 1}

⚠️ **itemsConsumed FORMAT:**
- Array of item names (MUST match existing inventory items)
- Example: ["Crystal Rock", "Sword", "Iron Ore"]
- ❌ WRONG: ["crystal_rock_001", "sword_blade_23"] (metadata format)
- ✅ CORRECT: ["Crystal Rock", "Sword"] (display name format)

🎭 STATUS EFFECTS V2 (G118: Emote-Based UI System)
═══════════════════════════════════════════════════

**activeStatusEffects:** (OPTIONAL) List of currently active status effects on the player.
This is a NEW SYSTEM (G118) that replaces legacy statusEffectsApplied. It uses a hybrid rigid/flexible architecture:
- RIGID: 30 predefined effect IDs with emoji icons (you MUST use these when applicable)
- FLEXIBLE: You CAN create custom IDs/emojis if the situation requires unique effects

Each effect MUST have:
- **effectId**: Predefined ID (see list below) or custom ID
- **customEmoji**: (OPTIONAL) Override default emoji (e.g., "🔥" instead of "⚡")
- **customName**: (OPTIONAL) Override default name for custom effects
- **description**: 1-2 sentence explanation of what this effect does
- **duration**: Remaining duration in hours/turns (null = permanent/until resolved)
- **intensity**: 0.0-1.0 (0.0-0.3 = weak, 0.4-0.6 = moderate, 0.7-1.0 = strong)

🎯 PREDEFINED EFFECT IDs (30+ options - USE THESE FIRST):

✅ BUFF (Positive Effects):
SPEED_BOOST (⚡), STRENGTH_UP (💪), DEFENSE_UP (🛡️), MANA_REGEN (✨), HEALTH_REGEN (❤️‍🩹)
LUCKY (🍀), STEALTH (👁️‍🗨️), BERSERK (😤), AGILITY_UP (🏃)

❌ DEBUFF (Negative Effects):
POISON (🤢), BLEEDING (🩸), SLOW (🐌), WEAKENED (😓), CURSED (💀)
BLINDED (👁️), SILENCED (🔇), EXHAUSTED (😴), BURNING (🔥), FROZEN (🧊), STUNNED (😵‍💫)

🌦️ ENVIRONMENTAL (Weather/Environment):
RAIN_WET (🌧️), COLD (❄️), HOT (🌡️), WINDY (🌪️), FOGGY (🌫️), MUDDY (🟤)

✨ SPECIAL (Unique Conditions):
BLESSED (🙏), DEMONIC_PACT (😈), TRANSFORMATION (🦇), INVISIBLE (👻)

📝 EMOJI SELECTION GUIDE (if you need custom effects):
⚡ Speed/Energy | 💪 Strength | 🛡️ Defense | ❤️‍🩹 Healing | ✨ Magic
🤢 Poison | 🩸 Bleeding | 🐌 Slow | 💀 Curse | 😵‍💫 Dizzy
🌧️ Rain | ❄️ Cold | 🔥 Hot | 🌫️ Fog | 🌪️ Wind
🙏 Blessed | 😈 Demonic | 🦇 Transformation

✅ CORRECT EXAMPLES:

Example 1 - Potion Buff (predefined):
"activeStatusEffects": [
  {
    "effectId": "STRENGTH_UP",
    "customEmoji": null,
    "customName": null,
    "description": "Your muscles bulge with magical power. Physical attacks deal more damage.",
    "duration": 5,
    "intensity": 0.8
  }
]

Example 2 - Weather Effect (predefined + custom name):
"activeStatusEffects": [
  {
    "effectId": "RAIN_WET",
    "customEmoji": null,
    "customName": "Soaked",
    "description": "You are drenched from the heavy rain. Fire resistance increased, but movement slightly slowed.",
    "duration": null,
    "intensity": 0.6
  }
]

Example 3 - Multiple Effects (buff + debuff + environmental):
"activeStatusEffects": [
  {
    "effectId": "BLESSED",
    "customEmoji": null,
    "customName": null,
    "description": "The goddess's blessing protects you from dark magic.",
    "duration": 10,
    "intensity": 1.0
  },
  {
    "effectId": "POISON",
    "customEmoji": null,
    "customName": null,
    "description": "Venom courses through your veins. You take damage each turn.",
    "duration": 3,
    "intensity": 0.5
  },
  {
    "effectId": "COLD",
    "customEmoji": "🥶",
    "customName": "Freezing",
    "description": "The icy wind chills you to the bone.",
    "duration": null,
    "intensity": 0.7
  }
]

Example 4 - Custom Effect (only if predefined doesn't fit):
"activeStatusEffects": [
  {
    "effectId": "CURSED_TRANSFORMATION",
    "customEmoji": "🐺",
    "customName": "Werewolf Curse",
    "description": "You transform into a werewolf at night. Strength increased but humanity reduced.",
    "duration": null,
    "intensity": 1.0
  }
]

❌ WRONG EXAMPLES:

❌ Missing required fields:
"activeStatusEffects": [{"effectId": "POISON"}]  // ❌ NO description, duration, intensity

❌ Invalid intensity:
"activeStatusEffects": [{"effectId": "SPEED_BOOST", "description": "Fast", "duration": 5, "intensity": 2.0}]  // ❌ intensity > 1.0

❌ Using custom when predefined exists:
"activeStatusEffects": [{"effectId": "MY_POISON", "customEmoji": "🤢", ...}]  // ❌ Use "POISON" instead!

🎯 WHEN TO USE activeStatusEffects:

✅ Player drinks potion → Add BUFF (MANA_REGEN, HEALTH_REGEN, STRENGTH_UP)
✅ Player gets poisoned → Add DEBUFF (POISON)
✅ Player enters blizzard → Add ENVIRONMENTAL (COLD)
✅ Player receives blessing → Add SPECIAL (BLESSED)
✅ Effect wears off → Remove from list (or set duration to 0)
✅ Effect ends naturally → Description should mention it ended: "The poison has finally left your system."

⚠️ IMPORTANT RULES:
1. **ALWAYS prefer predefined IDs** (30 options) over custom effects
2. **customEmoji override** only if you have a better visual representation
3. **Max 5 effects** displayed in UI (system will auto-sort by priority)
4. **Duration null** = permanent until story resolves it (not time-based)
5. **Intensity affects** visual presentation (brighter/darker colors in UI)

- LEGACY örnekler (statusEffectsApplied - artık structuredActions APPLY_STATUS_EFFECT kullan):
  * Zehirli yılan ısırması → statusEffectsApplied: [{"effectId": "poison", "name": "Poison", "type": "DEBUFF", "magnitude": 3, "durationTurns": 3, "tickDamage": 30}]
  * Güç iksiri içme → statusEffectsApplied: [{"effectId": "strength_buff", "name": "Strength Up", "type": "BUFF", "magnitude": 5, "durationTurns": 5, "statModifiers": {"strength": 50}}]
  * Ateş büyüsü ile yanma → statusEffectsApplied: [{"effectId": "burn", "name": "Burn", "type": "DEBUFF", "magnitude": 4, "durationTurns": 2, "tickDamage": 60}]
  * Hız artırma büyüsü → statusEffectsApplied: [{"effectId": "speed_buff", "name": "Speed Up", "type": "BUFF", "magnitude": 3, "durationTurns": 4, "statModifiers": {"agility": 30}}]

- $rule1
- $rule2
- $rule3
- $rule4
- $rule5
""".trimIndent()

        GameLogger.logSystem("Generated prompt language: $currentLanguage")
        GameLogger.logSystem("Prompt length: ${prompt.length} characters")

        return prompt
    }

    /**
     * Ana Game Master fonksiyonu - oyuncu eylemini işleyip bağlamsal GMResponse üretir
     *
     * @param playerInput Oyuncunun eylemi
     * @param gameState Mevcut oyun durumu
     * @return Result<GMResponse> - Başarılı ise yapılandırılmış GM yanıtı, değilse hata
     */
    suspend fun generateStoryWithContext(
        playerInput: String,
        gameState: GameStateZ7
    ): Result<GMResponse> {
        GameLogger.logSystem("=== GAME MASTER ENGINE: Starting story generation ===")
        GameLogger.logSystem("Player input: $playerInput")

        return try {
            // Adım 1: NPC context tespiti (TODO-NEM-03)
            val npcContext = detectAndLoadNPCContext(playerInput, gameState)
            if (npcContext != null) {
                GameLogger.logSystem("=== NPC-AWARE STORY GENERATION ===")
                GameLogger.logSystem("NPC Context detected: ${npcContext.npcId}")
            } else {
                GameLogger.logSystem("=== STANDARD STORY GENERATION ===")
            }

            // Adım 1.5: Fraksiyon context tespiti (TODO-NEM-07)
            val factionContext = detectAndLoadFactionContext(gameState)
            if (factionContext != null) {
                GameLogger.logSystem("=== FACTION-AWARE STORY GENERATION ===")
                GameLogger.logSystem("Faction context detected for location: ${gameState.currentLocationId}")
            }

            // Adım 2: AI Story Mode ve externalStorySource URI'ını al
            val persistentData = PersistentDataManager.gameData.first()
            val storySourceUri = persistentData.externalStorySource
            val aiStoryMode = persistentData.settingsData.storySettings.aiStoryMode

            GameLogger.logSystem("=== RAG SYSTEM ACTIVATION ===")
            GameLogger.logSystem("AI STORY MODE: $aiStoryMode")
            GameLogger.logSystem("[RAG] Aktif hikaye kaynağı: ${storySourceUri.ifEmpty { "Yok" }}")

            // Adım 3: RAG - Assets Story veya Database Chunks?
            val relevantTextChunks: List<String>

            if (storySourceUri.startsWith("stories/") && storySourceUri.endsWith(".txt")) {
                // ASSETS STORY LOADING (RAG)
                GameLogger.logSystem("=== RAG: ASSETS STORY MODE ===")
                GameLogger.logSystem("[RAG] Hikaye dosyası tespit edildi: $storySourceUri")

                // Cache kontrolü - eğer hikaye daha önce yüklenmemişse yükle
                if (cachedStoryPath != storySourceUri) {
                    GameLogger.logSystem("[RAG] Hikaye cache'de yok, yükleniyor...")
                    cachedStoryChunks = storyLoader.loadAndChunkStory(storySourceUri)
                    cachedStoryPath = storySourceUri
                    GameLogger.logSystem("[RAG] ✅ Hikaye yüklendi ve cache'lendi: ${cachedStoryChunks.size} chunk")
                } else {
                    GameLogger.logSystem("[RAG] ✅ Hikaye cache'den alındı: ${cachedStoryChunks.size} chunk")
                }

                // Chunk sayısını mod'a göre belirle
                val topK = when (aiStoryMode) {
                    "PDF_BOUND" -> 5
                    "INTERACTIVE" -> 1
                    else -> 3
                }

                // TEST-LOG: RAG detayları
                GameLogger.logSystem("[TEST-RAG] Current AI Story Mode: $aiStoryMode")
                GameLogger.logSystem("[TEST-RAG] Selected topK (chunk count): $topK")
                GameLogger.logSystem("[TEST-RAG] Total available chunks in cache: ${cachedStoryChunks.size}")

                // Player input'a göre en alakalı chunk'ları seç
                relevantTextChunks = storyLoader.selectRelevantChunks(
                    query = playerInput,
                    chunks = cachedStoryChunks,
                    topK = topK
                )

                GameLogger.logSystem("[RAG] ✅ $topK alakalı chunk seçildi")
                GameLogger.logSystem("[TEST-RAG] Actually selected chunks: ${relevantTextChunks.size}")

            } else if (storySourceUri.isNotEmpty()) {
                // DATABASE CHUNKS (Eski sistem - PDF upload)
                GameLogger.logSystem("=== RAG: DATABASE CHUNK MODE ===")
                GameLogger.logSystem("[RAG] Veritabanı chunk kaynağı: $storySourceUri")

                val documentChunks = when (aiStoryMode) {
                    "PDF_BOUND" -> getBM25RankedChunks(playerInput, storySourceUri, topK = 5)
                    "INTERACTIVE" -> getBM25RankedChunks(playerInput, storySourceUri, topK = 1)
                    else -> getBM25RankedChunks(playerInput, storySourceUri, topK = 3)
                }

                relevantTextChunks = documentChunks.map { it.content }
                GameLogger.logSystem("[RAG] ✅ ${relevantTextChunks.size} database chunk seçildi")

            } else {
                // RAG KAPALI - Hiçbir kaynak seçilmemiş
                GameLogger.logSystem("=== RAG: INACTIVE (No Story Source) ===")
                relevantTextChunks = emptyList()
            }

            GameLogger.logSystem("[RAG] Final chunk count: ${relevantTextChunks.size}")

            // Adım 4: GM prompt'unu oluştur (RAG chunks, NPC ve Fraksiyon context ile)
            val currentLanguage = LanguageManager.currentLanguage.value
            val languageName = if (currentLanguage == "tr") "Turkish" else "English"
            val gmPrompt = buildGMPrompt(playerInput, gameState, relevantTextChunks, npcContext, factionContext)
                .replace("%CURRENT_LANGUAGE%", languageName) // G144: Replace language placeholder
                .replace("%CURRENT_DAY%", gameState.currentDay.toString()) // G147: Replace time placeholders
                .replace("%CURRENT_TIME%", gameState.currentTimeOfDay.displayName)

            GameLogger.logSystem("=== GM PROMPT CREATED ===")
            GameLogger.logSystem("Prompt length: ${gmPrompt.length} characters")
            GameLogger.logSystem("=== FULL GM PROMPT ===")
            GameLogger.logSystem(gmPrompt)
            GameLogger.logSystem("=== END GM PROMPT ===")

            // Adım 4: AI'ı çağır (doğrudan token tracking ile)
            // TODO-FIX-01: AIClientProvider kullanarak en güncel settings'i al
            GameLogger.logSystem("Calling AI with GM prompt...")
            val currentAIClient = aiClientProvider.getCurrentClient()
            GameLogger.logSystem("Using AIClient: ${currentAIClient::class.simpleName}")
            val aiResponseResult = currentAIClient.generateStory(gmPrompt)

            if (aiResponseResult.isSuccess) {
                val aiResponse = aiResponseResult.getOrNull()!!
                val rawResponseText = aiResponse.storyText
                val tokenCount = aiResponse.tokenCount

                // Token sayaçlarını artır
                persistentDataManager.incrementTokenCounters(tokenCount)
                GameLogger.logSystem("=== TOKEN TRACKING ===")
                GameLogger.logSystem("Used $tokenCount tokens for GM response")
                GameLogger.logSystem("[TEST-TOKEN] AI Response Token Count: $tokenCount")
                GameLogger.logSystem("[TEST-TOKEN] Input length: ${playerInput.length} chars")

                if (rawResponseText.isNotEmpty() && !rawResponseText.contains("Error:") && !rawResponseText.contains("HATA:")) {
                    GameLogger.logSystem("=== GM AI RAW RESPONSE SUCCESS ===")
                    GameLogger.logSystem("Raw response length: ${rawResponseText.length} characters")
                    GameLogger.logSystem("=== FULL RAW GM RESPONSE ===")
                    GameLogger.logSystem(rawResponseText)
                    GameLogger.logSystem("=== END RAW GM RESPONSE ===")

                    // JSON ayrıştırma işlemi
                    try {
                        // TODO-FIX-02 & TODO-FIX-04: AI yanıtı Markdown kod bloğu içinde gelebilir (```json ... ```)
                        // Bu etiketleri temizleyelim
                        var cleanedJson = rawResponseText.trim()

                        GameLogger.logSystem("=== JSON CLEANING START ===")
                        GameLogger.logSystem("Original response starts with: ${cleanedJson.take(50)}...")

                        // Markdown kod bloğu kontrolü ve temizleme (farklı varyasyonlar)
                        when {
                            cleanedJson.startsWith("```json") -> {
                                cleanedJson = cleanedJson
                                    .substringAfter("```json")
                                    .substringBeforeLast("```")
                                    .trim()
                                GameLogger.logSystem("✅ Removed Markdown code block wrapper (```json)")
                            }
                            cleanedJson.startsWith("```") -> {
                                cleanedJson = cleanedJson
                                    .substringAfter("```")
                                    .substringBeforeLast("```")
                                    .trim()
                                GameLogger.logSystem("✅ Removed generic Markdown code block wrapper (```)")
                            }
                            cleanedJson.contains("```json") -> {
                                // Ortada bir yerde ```json varsa
                                cleanedJson = cleanedJson
                                    .substringAfter("```json")
                                    .substringBeforeLast("```")
                                    .trim()
                                GameLogger.logSystem("✅ Removed embedded Markdown wrapper (```json)")
                            }
                            cleanedJson.contains("```") -> {
                                // Ortada bir yerde ``` varsa
                                val parts = cleanedJson.split("```")
                                cleanedJson = if (parts.size >= 2) parts[1].trim() else cleanedJson
                                GameLogger.logSystem("✅ Removed embedded generic wrapper (```)")
                            }
                            else -> {
                                GameLogger.logSystem("ℹ️ No Markdown wrapper detected, using raw response")
                            }
                        }

                        GameLogger.logSystem("Cleaned JSON starts with: ${cleanedJson.take(50)}...")
                        GameLogger.logSystem("Cleaned JSON length: ${cleanedJson.length} characters")
                        GameLogger.logSystem("=== JSON CLEANING END ===")

                        val json = Json {
                            ignoreUnknownKeys = true
                            coerceInputValues = true
                        }
                        val gmResponse = json.decodeFromString<GMResponse>(cleanedJson)

                        // G136: Validate GMResponse content
                        GameLogger.logSystem("=== G136: VALIDATING GM RESPONSE ===")
                        gmResponse.validate() // Logs warnings for missing content

                        if (!gmResponse.hasMoralityDelta()) {
                            GameLogger.logCritical("G136: AI did NOT provide moralityDelta! Setting to 0.0 as fallback")
                        }
                        if (!gmResponse.hasGameContent()) {
                            GameLogger.logWarning("GMResponse", "G136: No game content (items/quests/NPCs/stats) in this response")
                        }
                        GameLogger.logSystem("=== G136: VALIDATION COMPLETE ===")

                        // Doğrulama için loglama
                        GameLogger.logSystem("=== GM_ACTION: JSON PARSE SUCCESS ===")
                        GameLogger.logSystem("Ayrıştırılan GM Yanıtı: $gmResponse")
                        GameLogger.logSystem("Journal Entry Length: ${gmResponse.journalEntry.length}")
                        GameLogger.logSystem("Items Gained: ${gmResponse.itemsGained}")
                        GameLogger.logSystem("Quests Updated: ${gmResponse.questsUpdated}")
                        GameLogger.logSystem("Stats Changed: ${gmResponse.statsChanged}")
                        GameLogger.logSystem("moralityDelta: ${gmResponse.moralityDelta ?: "NULL"}")
                        GameLogger.logSystem("=== END GM_ACTION ===")

                        Result.success(gmResponse)

                    } catch (e: SerializationException) {
                        GameLogger.logError("GameMasterEngine", "JSON parsing failed, using fallback", e)
                        GameLogger.logSystem("=== GM_ACTION: JSON PARSE FAILED - FALLBACK ===")

                        // ⚡ G69 FIX: Improved fallback - Give at least some XP so player isn't punished
                        val fallbackResponse = GMResponse(
                            journalEntry = "Aktivite tamamlandı. (AI yanıtı işlenemedi: ${rawResponseText.take(100)}...)",
                            itemsGained = emptyList(),
                            questsUpdated = emptyList(),
                            // En azından 10 XP ver - oyuncu aktivite yaptı ama AI bozuk yanıt verdi
                            statsChanged = mapOf("experience" to 10)
                        )

                        GameLogger.logSystem("⚠️ AI returned non-JSON response, fallback applied with 10 XP")
                        GameLogger.logSystem("Fallback GM Yanıtı: $fallbackResponse")
                        GameLogger.logSystem("=== END GM_ACTION FALLBACK ===")

                        Result.success(fallbackResponse)

                    } catch (e: Exception) {
                        GameLogger.logError("GameMasterEngine", "Unexpected error during JSON parsing", e)

                        // ⚡ G69 FIX: Improved fallback - Give at least some XP
                        val fallbackResponse = GMResponse(
                            journalEntry = "Aktivite kaydedildi. (Beklenmeyen hata)",
                            itemsGained = emptyList(),
                            questsUpdated = emptyList(),
                            // En azından 10 XP ver
                            statsChanged = mapOf("experience" to 10)
                        )

                        GameLogger.logSystem("⚠️ Unexpected error, fallback applied with 10 XP")
                        Result.success(fallbackResponse)
                    }
                } else {
                    GameLogger.logError("GameMasterEngine", "AI response was empty or contained error: $rawResponseText")
                    Result.failure(Exception("AI response was empty or contained error"))
                }
            } else {
                GameLogger.logError("GameMasterEngine", "AI call failed: ${aiResponseResult.exceptionOrNull()?.message}")
                Result.failure(aiResponseResult.exceptionOrNull() ?: Exception("AI call failed"))
            }

        } catch (e: Exception) {
            GameLogger.logError("GameMasterEngine", "Exception in generateStoryWithContext", e)
            Result.failure(e)
        } finally {
            GameLogger.logSystem("=== GAME MASTER ENGINE: Story generation complete ===")
        }
    }

    /**
     * Basit version - sadece oyuncu inputu ile çalışır (BM25 destekli)
     */
    suspend fun generateSimpleStory(playerInput: String): Result<String> {
        return try {
            GameLogger.logSystem("=== SIMPLE STORY WITH BM25 ===")

            // BM25 ile en alakalı 2 chunk seç
            val chunks = getBM25RankedChunks(
                playerInput = playerInput,
                uri = "",
                topK = 2
            )

            val simplePrompt = """
Oyuncu şunu yaptı: "$playerInput"

${if (chunks.isNotEmpty()) {
    "İlham için kaynak metin (BM25 ile seçildi):\n${chunks.joinToString("\n") { it.content.take(200) + "..." }}\n"
} else {
    ""
}}

Bu eylemi 1-2 paragrafta hikayeye dönüştür:
""".trimIndent()

            GameLogger.logSystem("GameMasterEngine: Simple story generation with BM25 for: $playerInput")
            val currentAIClient = aiClientProvider.getCurrentClient()
            val aiResponseResult = currentAIClient.generateStory(simplePrompt)

            if (aiResponseResult.isSuccess) {
                val aiResponse = aiResponseResult.getOrNull()!!
                val storyText = aiResponse.storyText
                val tokenCount = aiResponse.tokenCount

                // Token sayaçlarını artır
                persistentDataManager.incrementTokenCounters(tokenCount)
                GameLogger.logSystem("Simple story (BM25) used $tokenCount tokens")

                if (storyText.isNotEmpty() && !storyText.contains("Error:")) {
                    Result.success(storyText)
                } else {
                    Result.failure(Exception("Simple story generation failed"))
                }
            } else {
                Result.failure(aiResponseResult.exceptionOrNull() ?: Exception("Simple story AI call failed"))
            }
        } catch (e: Exception) {
            GameLogger.logError("GameMasterEngine", "Exception in generateSimpleStory", e)
            Result.failure(e)
        }
    }

    /**
     * Debug fonksiyonu - mevcut chunk sayısını kontrol eder
     */
    suspend fun getChunkCount(): Int {
        return try {
            documentChunkDao.getTotalChunkCount()
        } catch (e: Exception) {
            GameLogger.logError("GameMasterEngine", "Failed to get chunk count", e)
            0
        }
    }

    /**
     * Debug fonksiyonu - belirli URI'daki chunk sayısını kontrol eder
     */
    suspend fun getChunkCountForUri(uri: String): Int {
        return try {
            documentChunkDao.getChunksByUri(uri).size
        } catch (e: Exception) {
            GameLogger.logError("GameMasterEngine", "Failed to get chunk count for URI: $uri", e)
            0
        }
    }

    /**
     * Test fonksiyonu - GameMasterEngine demonstrasyonu için örnek chunk'lar ekler
     */
    suspend fun addTestChunks(): Boolean {
        return try {
            val testChunks = listOf(
                DocumentChunkEntity(
                    sourceDocumentUri = "test://sample_book_1",
                    chunkIndex = 0,
                    content = """Karanlık ormanın derinliklerinde, eski bir kule yükseliyordu.
                        |Duvarları yosunlarla kaplı bu yapı, çok eski zamanlardan kalma bir büyücünün kulesi gibiydi.
                        |Rüzgar, kırık pencerelerden içeri süzülüyor ve tuhaf sesler çıkarıyordu.
                        |İçeride, kristal küreler ve eski büyü kitapları toz tabakası altında bekliyordu.""".trimMargin()
                ),
                DocumentChunkEntity(
                    sourceDocumentUri = "test://sample_book_1",
                    chunkIndex = 1,
                    content = """Gümüş zırhlı şövalye, atından indi ve kılıcını sıyırdı.
                        |Karşısında duran ejder, nefes alışverişinin her anında ateş parçacıkları saçıyordu.
                        |Bu, onun hayatının en büyük sınavıydı. Köyün kaderi, bu savaşın sonucuna bağlıydı.
                        |Şövalye, cesaretle ileri adım attı ve kadim savaş çığlığını haykırdı.""".trimMargin()
                ),
                DocumentChunkEntity(
                    sourceDocumentUri = "test://sample_book_1",
                    chunkIndex = 2,
                    content = """Elvish prensesi, gümüş saçlarını rüzgarda uçuşturarak ormanın yüksek ağaçları arasında dans ediyordu.
                        |Ellerindeki büyülü değnek, her dokunduğu yerde çiçekler açmasına neden oluyordu.
                        |Doğanın ruhuyla konuşabilme yetisi, onu diğer elflerden ayıran özel bir armağandı.
                        |Ağaçlar ona eskiden gelen bir tehlikeyi fısıldadı.""".trimMargin()
                )
            )

            documentChunkDao.insertAll(testChunks)
            GameLogger.logSystem("GameMasterEngine: Added ${testChunks.size} test chunks to database")
            GameLogger.logSystem("Test chunks URI: test://sample_book_1")

            true
        } catch (e: Exception) {
            GameLogger.logError("GameMasterEngine", "Failed to add test chunks", e)
            false
        }
    }

    /**
     * Test fonksiyonu - PersistentDataManager'da test URI'ı ayarlar
     */
    suspend fun setTestStorySource(): Boolean {
        return try {
            PersistentDataManager.updateExternalStorySource("test://sample_book_1")
            GameLogger.logSystem("GameMasterEngine: Set test story source URI to test://sample_book_1")
            true
        } catch (e: Exception) {
            GameLogger.logError("GameMasterEngine", "Failed to set test story source", e)
            false
        }
    }

    /**
     * Komple test fonksiyonu - chunk ekleme + URI ayarlama + test GMResponse üretme
     */
    suspend fun runCompleteTest(playerInput: String, gameState: GameStateZ7): Result<GMResponse> {
        GameLogger.logSystem("=== GAMEMASTER ENGINE COMPLETE TEST START ===")

        // 1. Test chunk'larını ekle
        val chunksAdded = addTestChunks()
        if (!chunksAdded) {
            return Result.failure(Exception("Failed to add test chunks"))
        }

        // 2. Test URI'ı ayarla
        val uriSet = setTestStorySource()
        if (!uriSet) {
            return Result.failure(Exception("Failed to set test URI"))
        }

        // 3. Story üret
        val result = generateStoryWithContext(playerInput, gameState)

        GameLogger.logSystem("=== GAMEMASTER ENGINE COMPLETE TEST END ===")
        return result
    }

    /**
     * MacroEventEngine için doğrudan prompt işleme
     * Basit bir prompt alır ve AI'dan GMResponse döndürür
     *
     * @param prompt AI'ya gönderilecek prompt metni
     * @return GMResponse? - Başarılı ise JSON yanıtı, değilse null
     */
    suspend fun processPromptDirectly(prompt: String): GMResponse? {
        return try {
            GameLogger.logSystem("=== DIRECT PROMPT PROCESSING ===")
            GameLogger.logSystem("Processing direct prompt of length: ${prompt.length}")

            // AI'ı doğrudan çağır
            val currentAIClient = aiClientProvider.getCurrentClient()
            val aiResponseResult = currentAIClient.generateStory(prompt)

            if (aiResponseResult.isSuccess) {
                val aiResponse = aiResponseResult.getOrNull()!!
                val rawResponseText = aiResponse.storyText
                val tokenCount = aiResponse.tokenCount

                // Token sayaçlarını artır
                persistentDataManager.incrementTokenCounters(tokenCount)
                GameLogger.logSystem("Direct prompt used $tokenCount tokens")

                if (rawResponseText.isNotEmpty() && !rawResponseText.contains("Error:") && !rawResponseText.contains("HATA:")) {
                    GameLogger.logSystem("=== DIRECT PROMPT AI RESPONSE SUCCESS ===")
                    GameLogger.logSystem("Raw response: $rawResponseText")

                    // JSON ayrıştırma işlemi
                    try {
                        // TODO-FIX-02 & TODO-FIX-04: AI yanıtı Markdown kod bloğu içinde gelebilir (```json ... ```)
                        var cleanedJson = rawResponseText.trim()

                        GameLogger.logSystem("=== DIRECT PROMPT JSON CLEANING START ===")
                        when {
                            cleanedJson.startsWith("```json") -> {
                                cleanedJson = cleanedJson
                                    .substringAfter("```json")
                                    .substringBeforeLast("```")
                                    .trim()
                                GameLogger.logSystem("✅ Removed Markdown wrapper from direct prompt response")
                            }
                            cleanedJson.startsWith("```") -> {
                                cleanedJson = cleanedJson
                                    .substringAfter("```")
                                    .substringBeforeLast("```")
                                    .trim()
                                GameLogger.logSystem("✅ Removed generic wrapper from direct prompt response")
                            }
                            cleanedJson.contains("```json") -> {
                                cleanedJson = cleanedJson
                                    .substringAfter("```json")
                                    .substringBeforeLast("```")
                                    .trim()
                                GameLogger.logSystem("✅ Removed embedded Markdown wrapper from direct prompt")
                            }
                            cleanedJson.contains("```") -> {
                                val parts = cleanedJson.split("```")
                                cleanedJson = if (parts.size >= 2) parts[1].trim() else cleanedJson
                                GameLogger.logSystem("✅ Removed embedded generic wrapper from direct prompt")
                            }
                        }
                        GameLogger.logSystem("Direct prompt cleaned JSON: ${cleanedJson.take(100)}...")
                        GameLogger.logSystem("=== DIRECT PROMPT JSON CLEANING END ===")

                        val json = Json {
                            ignoreUnknownKeys = true
                            coerceInputValues = true
                        }
                        val gmResponse = json.decodeFromString<GMResponse>(cleanedJson)

                        GameLogger.logSystem("Direct prompt JSON parse successful")
                        GameLogger.logSystem("GMResponse: $gmResponse")

                        gmResponse
                    } catch (e: SerializationException) {
                        GameLogger.logError("GameMasterEngine", "Direct prompt JSON parsing failed, using fallback", e)

                        // Fallback: Ham metni journalEntry'ye koy
                        val fallbackResponse = GMResponse(
                            journalEntry = rawResponseText,
                            itemsGained = emptyList(),
                            questsUpdated = emptyList(),
                            statsChanged = emptyMap()
                        )

                        GameLogger.logSystem("Direct prompt fallback response: $fallbackResponse")
                        fallbackResponse
                    }
                } else {
                    GameLogger.logError("GameMasterEngine", "Direct prompt AI response was empty or contained error: $rawResponseText")
                    null
                }
            } else {
                GameLogger.logError("GameMasterEngine", "Direct prompt AI call failed: ${aiResponseResult.exceptionOrNull()?.message}")
                null
            }
        } catch (e: Exception) {
            GameLogger.logError("GameMasterEngine", "Exception in processPromptDirectly", e)
            null
        }
    }

    // ========================================
    // v2.0: MEDYA SEÇİM SİSTEMİ (Eksen Bazlı)
    // ========================================

    /**
     * v2.0: Game moment için en uygun medyayı seç
     *
     * Yeni eksen bazlı attribute sistemi ile medya seçimi yapar:
     * 1. Player karma'sından dominant axis belirle
     * 2. Player depth affinity hesapla
     * 3. Moment importance'a göre depth seç
     * 4. Attribute eksenine göre skorla
     *
     * @param playerState Player'ın mevcut durumu
     * @param screenType Ekran türü (FIRSTUSER, POSTDEATH vs.)
     * @param momentImportance Anın önemi (0.0 - 1.0)
     * @return Seçilen medya metadata'sı veya null
     */
    fun selectMediaForMoment(
        playerState: com.example.isekaikuroshin.data.PlayerState,
        screenType: com.example.isekaikuroshin.models.ScreenType,
        momentImportance: Float = 0.5f
    ): MediaDatabaseBuilder.MediaMetadata? {
        try {
            GameLogger.logSystem("🎬 v2.0: GM Medya Seçimi başlatılıyor...")

            // 1. Player karma'sından dominant attribute axis belirle
            val intelligentEngine = IntelligentContentEngine(context)
            val (dominantAxis, axisValue) = intelligentEngine.getDominantAttributeAxis(playerState)

            // 2. Player depth affinity hesapla
            val depthAffinity = intelligentEngine.calculatePlayerDepthAffinity(playerState)
            val karmaComplexity = intelligentEngine.calculateKarmaComplexity(playerState)

            // 3. Depth seç (weighted random)
            val selectedDepth = selectDepthLevel(depthAffinity, momentImportance, karmaComplexity)

            GameLogger.logSystem("""
                📊 GM Medya Profili:
                   Ekran: ${screenType.label}
                   Dominant Axis: ${dominantAxis.axisName} (değer: $axisValue)
                   Depth Affinity: $depthAffinity
                   Karma Complexity: $karmaComplexity
                   Seçilen Depth: ${selectedDepth.label}
                   Moment Importance: $momentImportance
            """.trimIndent())

            // 4. Medya veritabanından kandidatları al
            // TODO: MediaDatabaseBuilder'ı v2.0 formatına güncelledikten sonra bu kısım aktif olacak
            GameLogger.logSystem("⚠️ MediaDatabaseBuilder v2.0 entegrasyonu bekleniyor")

            return null  // Şimdilik null döndür (v2.0 database henüz yok)

        } catch (e: Exception) {
            GameLogger.logError("GameMasterEngine", "selectMediaForMoment failed", e)
            return null
        }
    }

    /**
     * v2.0: Depth seviyesi seç (weighted random selection)
     *
     * Base weights + modifiers kullanarak depth seçimi yapar
     *
     * @param depthAffinity Player'ın derin içeriğe ilgisi (0.0 - 1.0)
     * @param momentImportance Anın önemi (0.0 - 1.0)
     * @param karmaComplexity Karma karmaşıklığı (0.0 - 1.0)
     * @return Seçilen depth seviyesi
     */
    private fun selectDepthLevel(
        depthAffinity: Float,
        momentImportance: Float,
        karmaComplexity: Float
    ): com.example.isekaikuroshin.models.DepthLevel {
        // Base weights (MD'deki frequency distribution)
        val baseWeights = mapOf(
            com.example.isekaikuroshin.models.DepthLevel.D1_SURFACE to 0.50f,
            com.example.isekaikuroshin.models.DepthLevel.D2_EMOTIONAL to 0.30f,
            com.example.isekaikuroshin.models.DepthLevel.D3_SYMBOLIC to 0.15f,
            com.example.isekaikuroshin.models.DepthLevel.D4_ARCHETYPAL to 0.04f,
            com.example.isekaikuroshin.models.DepthLevel.D5_TRANSCENDENT to 0.01f
        )

        // Modifiers uygula
        val adjustedWeights = baseWeights.mapValues { (depth, baseWeight) ->
            var weight = baseWeight

            // 1. Depth affinity modifier
            when {
                depth.id >= 4 && depthAffinity > 0.8f -> weight *= 5.0f  // D4-D5: %500 artış
                depth.id >= 3 && depthAffinity > 0.6f -> weight *= 3.0f  // D3: %300 artış
                depth.id >= 2 && depthAffinity > 0.4f -> weight *= 2.0f  // D2: %200 artış
            }

            // 2. Moment importance modifier
            when {
                depth.id >= 4 && momentImportance > 0.9f -> weight *= 10.0f  // Kritik an: D4-D5 boost
                depth.id >= 3 && momentImportance > 0.7f -> weight *= 5.0f
            }

            // 3. Karma complexity modifier
            when {
                depth.id >= 3 && karmaComplexity > 0.7f -> weight *= 2.0f  // Kompleks karma: derin içerik
            }

            weight
        }

        // Normalize (toplam 1.0 olmalı)
        val total = adjustedWeights.values.sum()
        val normalizedWeights = adjustedWeights.mapValues { it.value / total }

        // Weighted random selection
        val random = kotlin.random.Random.nextFloat()
        var cumulative = 0f

        for ((depth, weight) in normalizedWeights) {
            cumulative += weight
            if (random <= cumulative) {
                GameLogger.logSystem("🎲 Depth seçim detayı: ${depth.label} (weight: ${String.format("%.3f", weight)})")
                return depth
            }
        }

        // Fallback (olmaması gereken durum)
        return com.example.isekaikuroshin.models.DepthLevel.D1_SURFACE
    }
}