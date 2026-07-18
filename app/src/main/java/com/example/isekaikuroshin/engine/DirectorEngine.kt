package com.example.isekaikuroshin.engine

import com.example.isekaikuroshin.ai.AIResponse
import com.example.isekaikuroshin.data.GameStateManager
import com.example.isekaikuroshin.data.PlayerState
import com.example.isekaikuroshin.data.PersistentDataManager
import com.example.isekaikuroshin.utils.GameLogger
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class DirectorEngine @Inject constructor(
    private val observerEngine: ObserverEngine,
    private val promptEngine: PromptEngine,
    private val aiClient: AIClient,
    private val pacingEngine: PacingEngine,
    private val storyAnalysisEngine: StoryAnalysisEngine
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun generateDynamicEvent(gameStateManager: GameStateManager, actionExecutor: ActionExecutorEngine? = null): AIResponse? {
        return try {
            GameLogger.logSystem("=== DIRECTOR ENGINE: Starting Dynamic Event Generation ===")

            // A. Gözlemle: ObserverEngine ile dünya durumu özeti ve gameState'i al
            val currentState = gameStateManager.gameState.value
            val worldStateSummary = observerEngine.createWorldStateSummary(currentState)
            GameLogger.logSystem("World state summary obtained")

            // B. Eşiği Hesapla: PacingEngine ile threatThreshold değerini hesapla
            val threatThreshold = pacingEngine.calculateThreatThreshold(
                playerState = currentState.playerState,
                storyMoodModifier = 1.0f // Bu prototip için sabit
            )
            GameLogger.logSystem("💠 TEHDİT SAYACI: ${currentState.threatCounter}")
            GameLogger.logSystem("💠 TEHDİT EŞİĞİ: $threatThreshold")

            // C. Karar Ver: threatCounter >= threatThreshold kontrolü
            if (currentState.threatCounter >= threatThreshold) {
                GameLogger.logSystem("🚨 TEHDİT EŞİĞİ AŞILDI! Tehdit olayı yaratılıyor...")

                // D. Tehdit Yarat: Gizli Zar At -> Başarılıysa Seçenek Sun / Başarısızsa Doğrudan Aksiyon Uygula
                val perceptionCheck = performPerceptionCheck(gameStateManager)
                GameLogger.logSystem("🎲 ZAR SONUCU: ${if (perceptionCheck.isSuccess) "SUCCESS" else "FAILURE"}")
                GameLogger.logSystem("🎲 Detaylar: Base=${perceptionCheck.baseRoll}, Modified=${perceptionCheck.modifiedRoll}, Success Level=${perceptionCheck.successLevel}")

                val eventInstruction = if (perceptionCheck.isSuccess) {
                    // Başarılı senaryo - Oyuncu fark etti, seçenekler sun
                    """
                    Oyuncu, yüksek algısı sayesinde ilerideki tehlikeyi (örn: bir pusu, bir tuzak, yaklaşan bir tehdit) fark etti.
                    Bu durumu, oyuncuya ipuçları vererek betimle ve ona en az 3 farklı eylem seçeneği sun:
                    - [Gizlice Yaklaş]
                    - [Ses Çıkararak Dikkat Dağıt]
                    - [Geri Çekil]
                    - [Diğer yaratıcı seçenekler]

                    Oyuncuya seçim hakkı tanı ve "Ne yapmak istiyorsun?" gibi bir soru ile bitir.
                    """.trimIndent()
                } else {
                    // Başarısız senaryo - Oyuncu yakalandı, doğrudan aksiyon
                    """
                    Oyuncu, dikkatsizliği sonucu bir pusuya/tuzağa/ani duruma hazırlıksız yakalandı.
                    Bu ani ve şaşırtıcı olayı, doğrudan bir aksiyonla başlatarak betimle.
                    Oyuncunun derhal tepki vermesi gereken dramatik bir durum yarat.
                    """.trimIndent()
                }

                GameLogger.logSystem("📝 TALİMAT METNİ: ${eventInstruction.take(100)}...")

                // Nihai Prompt'u Oluştur
                val enhancedWorldSummary = "$worldStateSummary\n\n# ÖZEL TALİMAT\n$eventInstruction"
                val finalPrompt = promptEngine.createPrompt(enhancedWorldSummary)
                GameLogger.logSystem("Final prompt created with ${if (perceptionCheck.isSuccess) "SUCCESS" else "FAILURE"} scenario")

                // AI'ı Çağır ve Yanıtı Ayrıştır
                val aiResult = aiClient.generateStory(finalPrompt)

                if (aiResult.isSuccess) {
                    val rawResponse = aiResult.getOrNull()
                    if (rawResponse != null) {
                        // JSON yanıtını parse et
                        val aiResponse = parseAIResponse(rawResponse.storyText)
                        if (aiResponse != null) {
                            GameLogger.logSystem("AI response successfully parsed")
                            GameLogger.logSystem("Story text: ${aiResponse.storyText.take(100)}...")
                            GameLogger.logSystem("Actions count: ${aiResponse.actions.size}")

                            // YENİ EKLENECEK KISIM: AI eylemlerini uygula
                            if (aiResponse.actions.isNotEmpty() && actionExecutor != null) {
                                GameLogger.logSystem("Executing ${aiResponse.actions.size} action(s) from AI...")
                                actionExecutor.executeActions(aiResponse.actions)
                            }

                            // Tehdit sayacını sıfırla
                            gameStateManager.resetThreatCounter()

                            aiResponse
                        } else {
                            GameLogger.logError("DirectorEngine", "Failed to parse AI response as JSON")
                            null
                        }
                    } else {
                        GameLogger.logError("DirectorEngine", "AI response was null")
                        null
                    }
                } else {
                    GameLogger.logError("DirectorEngine", "AI call failed", aiResult.exceptionOrNull() as? Exception)
                    null
                }
            } else {
                // E. Yaratıcı Çekirdek: Eşik aşılmadıysa yaratıcı olaylar deneyelim
                GameLogger.logSystem("💤 Tehdit Eşiği aşılmadı. Yaratıcı Çekirdek devreye giriyor...")

                // A. Oyuncu Ayarını Oku: PersistentDataManager'dan storyAdherence değerini al
                val storyAdherence = PersistentDataManager.gameData.value.settingsData.storySettings.storyAdherence
                GameLogger.logSystem("🎨 Hikaye Bağlılık Oranı: ${(storyAdherence * 100).toInt()}%")

                // TODO-G111.3: B. Ana Zarı At - DiceSystem ile (Random.nextInt yerine)
                val masterRoll = DiceSystem.rollDice(DiceSystem.DiceType.D100, 1, 0).total
                GameLogger.logSystem("🎲 Ana Zar: $masterRoll (DiceSystem)")

                // C. Olay Kategorisini Belirle
                val eventCategory = if (masterRoll <= storyAdherence * 100) {
                    "HİKAYE_ODAKLI"
                } else {
                    "ARKETİP_ODAKLI"
                }
                GameLogger.logSystem("🎯 Seçilen Kategori: $eventCategory")

                // D. Olay Talimatı Oluştur
                val eventInstruction = when (eventCategory) {
                    "HİKAYE_ODAKLI" -> {
                        // StoryAnalysisEngine'i çağır ve temaları al
                        val analysisResult = storyAnalysisEngine.analyzeTextChunk() // lastStoryPage argument removed
                        GameLogger.logSystem("📚 Analiz Temaları: ${analysisResult.themes}")
                        GameLogger.logSystem("🌟 Atmosfer: ${analysisResult.atmosphere}")

                        """
                        Aşağıdaki temalardan birini kullanarak bir 'Fırsat' veya 'Atmosfer' olayı yarat:
                        ${analysisResult.themes.joinToString(", ")}.

                        Olay, ${analysisResult.atmosphere} havasında olmalı.
                        Bu, oyuncuya yeni keşif fırsatları sunan veya atmosferi zenginleştiren yumuşak bir olay olmalıdır.
                        Tehdit değil, merak uyandıran bir durum yarat.
                        """.trimIndent()
                    }
                    "ARKETİP_ODAKLI" -> {
                        // Oyuncunun en baskın arketipini bul
                        val playerProfile = currentState.playerState.playerProfile
                        val dominantArchetype = playerProfile.archetypeScores.maxByOrNull { it.value }?.key ?: "Kaşif"
                        GameLogger.logSystem("🏛️ Baskın Arketip: $dominantArchetype")

                        """
                        Oyuncunun baskın arketipi '$dominantArchetype'.
                        Ona, bu arketipte bulunan kişiliğini tetikleyecek bir 'Fırsat' olayı yarat.

                        Örneğin:
                        - Kaşif için: eski bir harita, gizemli bir harabe, ilginç bir NPC
                        - Savaşçı için: antrenman fırsatı, silah ustası, güç gösterisi
                        - Diplomat için: sosyal olay, ittifak fırsatı, müzakere durumu

                        Bu, tehlike değil, oyuncunun karakterine uygun gelişim fırsatı sunmalıdır.
                        """.trimIndent()
                    }
                    else -> {
                        // Fallback
                        """
                        Oyuncunun çevresinde, sakin ama ilginç bir durum yarat.
                        Bu, tehdit değil, merak uyandıran küçük bir olay olmalıdır.
                        """.trimIndent()
                    }
                }

                GameLogger.logSystem("📝 YARATICI TALİMAT: ${eventInstruction.take(100)}...")

                // E. AI'ı Çağır
                val enhancedWorldSummary = "$worldStateSummary\n\n# YARATICI ÇEKIRDEK TALİMATI\n$eventInstruction"
                val finalPrompt = promptEngine.createPrompt(enhancedWorldSummary)
                GameLogger.logSystem("Final prompt created with $eventCategory scenario")

                val aiResult = aiClient.generateStory(finalPrompt)

                if (aiResult.isSuccess) {
                    val rawResponse = aiResult.getOrNull()
                    if (rawResponse != null) {
                        val aiResponse = parseAIResponse(rawResponse.storyText)
                        if (aiResponse != null) {
                            GameLogger.logSystem("🎨 Yaratıcı olay başarıyla oluşturuldu")
                            GameLogger.logSystem("Story text: ${aiResponse.storyText.take(100)}...")
                            GameLogger.logSystem("Actions count: ${aiResponse.actions.size}")

                            // YENİ EKLENECEK KISIM: AI eylemlerini uygula
                            if (aiResponse.actions.isNotEmpty() && actionExecutor != null) {
                                GameLogger.logSystem("Executing ${aiResponse.actions.size} action(s) from AI...")
                                actionExecutor.executeActions(aiResponse.actions)
                            }

                            aiResponse
                        } else {
                            GameLogger.logError("DirectorEngine", "Failed to parse creative AI response as JSON")
                            null
                        }
                    } else {
                        GameLogger.logError("DirectorEngine", "Creative AI response was null")
                        null
                    }
                } else {
                    GameLogger.logError("DirectorEngine", "Creative AI call failed", aiResult.exceptionOrNull() as? Exception)
                    null
                }
            }

        } catch (e: Exception) {
            GameLogger.logError("DirectorEngine", "Exception in generateDynamicEvent", e)
            null
        } finally {
            GameLogger.logSystem("=== DIRECTOR ENGINE: Dynamic Event Generation Complete ===")
        }
    }

    private fun performPerceptionCheck(gameStateManager: GameStateManager): DiceResult {
        // Gizli algı kontrolü - sabit zorluk 10
        val currentState = gameStateManager.gameState.value
        val context = ContextAdapterL1.createDiceContext(
            gameState = currentState,
            actionType = DiceActionType.EXPLORATION_PERCEPTION,
            difficulty = 10
        )

        return DiceEngine.calculateComplexRoll(context)
    }

    private fun parseAIResponse(rawResponse: String): AIResponse? {
        return try {
            // JSON bloğunu ayıkla (eğer ``` ile sarılmışsa)
            val cleanedResponse = cleanJsonResponse(rawResponse)
            json.decodeFromString(AIResponse.serializer(), cleanedResponse)
        } catch (e: Exception) {
            GameLogger.logError("DirectorEngine", "JSON parse error: ${e.message}")
            // Fallback: Ham metni story olarak kullan
            AIResponse(
                storyText = rawResponse,
                actions = emptyList()
            )
        }
    }

    private fun cleanJsonResponse(rawResponse: String): String {
        // ```json ve ``` etiketlerini kaldır
        return rawResponse
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }

    /**
     * Umbros'un Fısıltıları - Ahlaki karar algılandığında bildirim sistemi
     */
    fun triggerUmbrosWhisper(gameStateManager: GameStateManager, moralChoice: String) {
        val currentState = gameStateManager.gameState.value
        val playerState = currentState.playerState

        // Umbros paktı aktif mi kontrol et
        val hasUmbrosContract = currentState.umbrosContract != null

        // Ahlaki durumu kontrol et - Yüksek kötülük veya aktif pakt varsa bildirim göster
        val shouldShowWhisper = hasUmbrosContract ||
                               playerState.unholyPoints > 30 ||
                               playerState.moralityScore < -0.3f

        if (shouldShowWhisper) {
            val whisperMessage = generateUmbrosWhisperMessage(playerState, moralChoice, hasUmbrosContract)

            // SystemOverlay ile UMBROS_CURSE rengiyle bildirim göster
            // Bu kısım UI tarafından implement edilecek
            GameLogger.logSystem("👁️ UMBROS FISIILTISI: $whisperMessage")

            // Opsiyonel: Kötülük puanı ekle
            if (moralChoice.contains("dark") || moralChoice.contains("evil")) {
                gameStateManager.addUnholy(5)
            }
        }
    }

    /**
     * Umbros fısıltı mesajı oluşturur
     */
    private fun generateUmbrosWhisperMessage(playerState: PlayerState, moralChoice: String, hasContract: Boolean): String {
        return if (hasContract) {
            when {
                moralChoice.contains("kill") -> "Yees... Karanlık güç akar damarlarında. Bu sadece başlangıç..."
                moralChoice.contains("steal") -> "Mükemmel seçim. Başkalarının malı senin olmalı, değil mi?"
                moralChoice.contains("lie") -> "Gerçek overrated. Yalan daha... kullanışlı."
                moralChoice.contains("betray") -> "İhanet... Ah, benim en sevdiğim lezzet. Devam et."
                else -> "İlginç seçim... Ruhun yavaş yavaş benimle uyum sağlıyor."
            }
        } else {
            when {
                playerState.unholyPoints > 70 -> "Bu karanlık dürtülerin... benimle bir anlaşma yapmak istemez misin?"
                playerState.unholyPoints > 50 -> "Ruhundaki karanlığı hissediyorum. Çok yakışıyor sana..."
                else -> "Bu seçimin ağır sonuçları olabilir... Yardıma ihtiyacın olursa bil ki ben buradayım."
            }
        }
    }
}

// DiceResult extension property
val DiceResult.isSuccess: Boolean
    get() = this.successLevel in listOf(
        SuccessLevel.SUCCESS,
        SuccessLevel.GREAT_SUCCESS,
        SuccessLevel.CRITICAL_SUCCESS,
        SuccessLevel.LEGENDARY_SUCCESS,
        SuccessLevel.PARTIAL_SUCCESS
    )