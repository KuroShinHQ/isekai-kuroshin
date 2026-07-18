package com.example.isekaikuroshin.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.isekaikuroshin.utils.GameLogger
import com.example.isekaikuroshin.data.database.AppDatabase
import com.example.isekaikuroshin.data.database.LongTermMemoryEntity
import com.example.isekaikuroshin.engine.AIClientProvider
import com.example.isekaikuroshin.game.GameStateManager
import com.example.isekaikuroshin.data.LanguageManager
import com.example.isekaikuroshin.data.PersistentDataManager

/**
 * MemoryManager - Strateji #3 Hiyerarşik Hafıza Mimarisi + Dile Duyarlı Hibrit AI
 * Flash Memory (Son 15-20 diyalog) + Long-Term Memory (Özetler)
 *
 * HİBRİT MİMARİ:
 * - İngilizce modda: Lokal model (offline, ücretsiz)
 * - Türkçe modda: Google AI (online, daha kaliteli Türkçe)
 */
class MemoryManager(
    private val context: Context,
    private val database: AppDatabase,
    private val aiClientProvider: AIClientProvider,
    private val gameStateManager: GameStateManager
) {
    companion object {
        private const val TAG = "MemoryManager"
        private const val FLASH_MEMORY_SIZE = 20
    }

    // Flash Memory: Son 15-20 diyaloğun ham metni
    private val flashMemory = mutableListOf<String>()

    /**
     * Initialize the memory manager (no model loading at init)
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        GameLogger.logAISystem("MemoryManager initialized (on-demand local loading strategy)")
    }

    /**
     * Add new dialogue to flash memory
     */
    fun addDialogue(speaker: String, message: String) {
        val dialogue = "$speaker: $message"
        flashMemory.add(dialogue)

        // Keep only last FLASH_MEMORY_SIZE dialogues
        if (flashMemory.size > FLASH_MEMORY_SIZE) {
            flashMemory.removeAt(0)
        }

        GameLogger.logAISystem("Flash Memory updated - Size: ${flashMemory.size}, Latest: $dialogue")
    }

    /**
     * Synthesize flash memory into long-term summary (called at end of day)
     * HİBRİT MİMARİ: Dile göre farklı AI motoru kullanır
     * - EN: Lokal model (offline)
     * - TR: Google AI (online, daha kaliteli Türkçe)
     */
    suspend fun synthesizeMemory(currentDay: Int) = withContext(Dispatchers.IO) {
        try {
            GameLogger.logAISystem("🔵🔵🔵 [MEMORY-SYNTHESIS] ═══ BAŞLADI - DAY $currentDay ═══")

            if (flashMemory.isEmpty()) {
                GameLogger.logAISystem("⚠️ [MEMORY-SYNTHESIS] Flash memory BOŞ - Day $currentDay için synthesis yapılamaz")
                return@withContext
            }

            GameLogger.logAISystem("════════════════════════════════════════════════════")
            GameLogger.logAISystem("🌙 END-OF-DAY MEMORY SYNTHESIS - DAY $currentDay")
            GameLogger.logAISystem("════════════════════════════════════════════════════")
            GameLogger.logAISystem("📊 Flash Memory Statistics:")
            GameLogger.logAISystem("   - Total dialogues: ${flashMemory.size}")
            GameLogger.logAISystem("   - Total characters: ${flashMemory.sumOf { it.length }}")

            val startTime = System.currentTimeMillis()
            GameLogger.logAISystem("⏰ [MEMORY-SYNTHESIS] Start time: $startTime")

            // === FIX #47: ADIM 1: Settings'teki kullanıcı tercihini kontrol et ===
            val settingsData = PersistentDataManager.getSettingsDataBlocking(context)
            val userModelChoice = settingsData.apiSettings.textGenerationModel
            GameLogger.logAISystem("⚙️ [USER-CHOICE] Text Generation Model: $userModelChoice")

            // Combine all flash memory into single text
            val flashMemoryContent = flashMemory.joinToString("\n")
            GameLogger.logAISystem("🔄 [MEMORY-SYNTHESIS] Combined ${flashMemory.size} dialogues (${flashMemoryContent.length} chars)")
            GameLogger.logAISystem("📝 Preview: ${flashMemoryContent.take(300)}...")

            var finalSummary: String? = null

            // === FIX #47: ADIM 2: Kullanıcı tercihine göre AI motorunu kullan ===
            if (userModelChoice == "GEMMA_LOCAL") {
                // --- LOKAL MODEL MODU ---
                GameLogger.logAISystem("🤖 [USER-CHOICE] Gemma 3-bit LOCAL MODEL selected")
                gameStateManager.setMemorySynthesisState(true, "Loading local engine...")

                val localClient = try {
                    aiClientProvider.getLocalClient()
                } catch (e: Exception) {
                    GameLogger.logError(TAG, "❌ Failed to load local model!", e)
                    null
                }

                if (localClient != null) {
                    GameLogger.logAISystem("✅ [LOCAL-AI] Local model loaded successfully")
                    gameStateManager.setMemorySynthesisState(true, "Creating summary...")

                    // G132 CRASH FIX: Gemma maxTokens=512, çok uzun prompt crash ediyor!
                    // ~1 token = 4 karakter, 512 token = ~2000 karakter
                    // Güvenlik için 800 karakter (200 token input) limiti
                    val maxChars = 800
                    val truncatedContent = if (flashMemoryContent.length > maxChars) {
                        GameLogger.logAISystem("⚠️ [LOCAL-AI] Flash memory too long (${flashMemoryContent.length} chars), truncating to $maxChars chars")
                        flashMemoryContent.takeLast(maxChars) + "\n...(earlier events truncated)"
                    } else {
                        flashMemoryContent
                    }

                    val prompt = """
You are a memory synthesis module. Summarize the following recent events and dialogues into a single, coherent paragraph. Focus on key decisions, quest progress, and changes in relationships.

Recent Events:
$truncatedContent

Summary:
                    """.trimIndent()

                    val result = try {
                        localClient.generateStory(prompt)
                    } catch (e: Exception) {
                        GameLogger.logError(TAG, "❌ Local model generation failed!", e)
                        null
                    }

                    if (result?.isSuccess == true) {
                        finalSummary = result.getOrNull()?.storyText
                        GameLogger.logAISystem("✅ [LOCAL-AI] Summary generated (${finalSummary?.length ?: 0} chars)")
                    } else {
                        GameLogger.logError(TAG, "❌ Local model returned empty or failed", null)
                    }
                } else {
                    GameLogger.logError(TAG, "❌ Local model could not be loaded for English summary", null)
                }

            } else {
                // --- GOOGLE GEMINI API MODU ---
                GameLogger.logAISystem("🌐 [USER-CHOICE] Google Gemini API selected")
                gameStateManager.setMemorySynthesisState(true, "Sunucuya bağlanılıyor...")

                val googleClient = try {
                    aiClientProvider.getCurrentClient() // Settings'e göre client al
                } catch (e: Exception) {
                    GameLogger.logError(TAG, "❌ Failed to get AI client!", e)
                    null
                }

                if (googleClient != null) {
                    GameLogger.logAISystem("✅ [GOOGLE-AI] AI client obtained")

                    // FIX #47: Kullanıcı diline göre prompt seç
                    val currentLanguage = LanguageManager.currentLanguage.value
                    val loadingMessage = if (currentLanguage == "TR") "Anılar özetleniyor..." else "Synthesizing memories..."
                    gameStateManager.setMemorySynthesisState(true, loadingMessage)

                    val prompt = if (currentLanguage == "TR") {
                        """
Sen bir hafıza sentezi modülüsün. Aşağıdaki son olayları ve diyalogları, tek ve tutarlı bir paragrafta özetle. Önemli kararlara, görev ilerlemesine ve ilişkilerdeki değişikliklere odaklan.

Son Olaylar:
$flashMemoryContent

Özet:
                        """.trimIndent()
                    } else {
                        """
You are a memory synthesis module. Summarize the following recent events and dialogues into a single, coherent paragraph. Focus on key decisions, quest progress, and changes in relationships.

Recent Events:
$flashMemoryContent

Summary:
                        """.trimIndent()
                    }

                    val result = try {
                        googleClient.generateStory(prompt)
                    } catch (e: Exception) {
                        GameLogger.logError(TAG, "❌ AI generation failed!", e)
                        null
                    }

                    if (result?.isSuccess == true) {
                        finalSummary = result.getOrNull()?.storyText
                        GameLogger.logAISystem("✅ [GOOGLE-AI] Summary generated (${finalSummary?.length ?: 0} chars)")
                    } else {
                        GameLogger.logError(TAG, "❌ AI returned empty or failed", null)
                    }
                } else {
                    GameLogger.logError(TAG, "❌ AI client not available for Turkish summary", null)
                }
            }

            // === ADIM 3: Özet başarılıysa veritabanına kaydet ===
            // G132 FIX: Graceful degradation - AI başarısız olsa bile basit özet kaydet
            val summaryToSave = if (finalSummary != null && finalSummary.isNotBlank()) {
                val endTime = System.currentTimeMillis()
                val latency = endTime - startTime

                GameLogger.logAISystem("⏱️ Performance: ${latency}ms, Summary: ${finalSummary.length} chars")
                GameLogger.logAISystem("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                GameLogger.logAISystem("📄 FINAL SUMMARY FOR DAY $currentDay:")
                GameLogger.logAISystem(finalSummary)
                GameLogger.logAISystem("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                finalSummary
            } else {
                // FALLBACK: AI özet üretemedi, flash memory'yi ham olarak kaydet
                GameLogger.logError(TAG, "❌ No AI summary generated - using fallback (raw flash memory)", null)
                val currentLanguage = com.example.isekaikuroshin.data.LanguageManager.currentLanguage.value
                val fallbackPrefix = if (currentLanguage == "TR") {
                    "[Otomatik Özet] Gün $currentDay - ${flashMemory.size} olay kaydedildi."
                } else {
                    "[Auto Summary] Day $currentDay - ${flashMemory.size} events recorded."
                }

                // İlk 500 karakter flash memory'den al (çok uzun olmasın)
                val flashPreview = flashMemoryContent.take(500)
                "$fallbackPrefix\n\n$flashPreview${if (flashMemoryContent.length > 500) "..." else ""}"
            }

            val memoryEntity = LongTermMemoryEntity(
                day = currentDay,
                summaryText = summaryToSave
            )

            try {
                database.longTermMemoryDao().insertMemory(memoryEntity)
                GameLogger.logAISystem("✅ [DATABASE] Summary saved successfully (${summaryToSave.length} chars)")
            } catch (e: Exception) {
                GameLogger.logError(TAG, "❌ Database insert failed!", e)
            }

            // === ADIM 4: Temizlik ===
            flashMemory.clear()
            GameLogger.logAISystem("🧹 Flash memory cleared")
            GameLogger.logAISystem("✅✅✅ MEMORY SYNTHESIS COMPLETE - Day $currentDay")
            GameLogger.logAISystem("════════════════════════════════════════════════════")

            gameStateManager.setMemorySynthesisState(false)
            GameLogger.logAISystem("🔵🔵🔵 [MEMORY-SYNTHESIS] ═══ TAMAMLANDI - DAY $currentDay ═══")

        } catch (t: Throwable) {
            Log.e(TAG, "❌❌❌ FATAL ERROR - Failed to synthesize memory for day $currentDay!", t)
            GameLogger.logError(TAG, "❌❌❌ FATAL ERROR in synthesizeMemory", t as? Exception ?: Exception(t))
            gameStateManager.setMemorySynthesisState(false)
        }
    }

    /**
     * Get memory context for AI prompts (last N days of summaries)
     */
    suspend fun getMemoryContext(lastNDays: Int = 3): String = withContext(Dispatchers.IO) {
        try {
            val memories = database.longTermMemoryDao().getRecentMemories(lastNDays)
            if (memories.isEmpty()) {
                return@withContext ""
            }

            val context = StringBuilder()
            context.append("Recent Memory Summaries:\n")
            memories.forEach { memory ->
                context.append("Day ${memory.day}: ${memory.summaryText}\n")
            }

            GameLogger.logAISystem("Retrieved memory context: ${memories.size} summaries")
            return@withContext context.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get memory context!", e)
            GameLogger.logError(TAG, "Failed to get memory context", e)
            return@withContext ""
        }
    }

    /**
     * FIX #47: Synthesize 3-day master summary (FORCED GOOGLE API)
     * Called every 3 days to create a consolidated summary
     * IGNORES user Settings - always uses Google API for quality
     */
    suspend fun synthesizeThreeDayMasterSummary(currentDay: Int) = withContext(Dispatchers.IO) {
        try {
            GameLogger.logAISystem("🔵🔵🔵 [3-DAY-SUMMARY] ═══ BAŞLADI - DAY $currentDay ═══")

            // Get last 3 days summaries
            val threeDaySummaries = getMemoryContext(3)
            if (threeDaySummaries.isBlank()) {
                GameLogger.logAISystem("⚠️ [3-DAY-SUMMARY] No summaries found for last 3 days")
                return@withContext
            }

            GameLogger.logAISystem("════════════════════════════════════════════════════")
            GameLogger.logAISystem("🌙 3-DAY MASTER SUMMARY - DAY $currentDay")
            GameLogger.logAISystem("════════════════════════════════════════════════════")
            GameLogger.logAISystem("📊 Retrieved ${threeDaySummaries.length} chars of summaries")

            val startTime = System.currentTimeMillis()

            // FIX #47: FORCE GOOGLE API (ignore user Settings choice)
            GameLogger.logAISystem("🌐 [FORCED-GOOGLE-API] 3-day summary MUST use Google API")
            gameStateManager.setMemorySynthesisState(true, "Creating 3-day master summary...")

            val googleClient = try {
                aiClientProvider.getCurrentClient()
            } catch (e: Exception) {
                GameLogger.logError(TAG, "❌ Failed to get Google AI client for 3-day summary!", e)
                gameStateManager.setMemorySynthesisState(false)
                return@withContext
            }

            val currentLanguage = LanguageManager.currentLanguage.value
            val loadingMessage = if (currentLanguage == "TR") {
                "3 günlük ana özet oluşturuluyor..."
            } else {
                "Creating 3-day master summary..."
            }
            gameStateManager.setMemorySynthesisState(true, loadingMessage)

            val prompt = if (currentLanguage == "TR") {
                """
Sen bir hafıza sentezi modülüsün. Aşağıdaki son 3 günün özetlerini tek bir kapsamlı ana özet haline getir. Kritik olayları, karakter gelişimlerini, görev ilerlemelerini ve ilişki değişikliklerini vurgula. Bu ana özet, 3 günlük dönemin tamamını temsil etmeli.

Son 3 Günün Özetleri:
$threeDaySummaries

3 Günlük Ana Özet:
                """.trimIndent()
            } else {
                """
You are a memory synthesis module. Consolidate the following 3 days of summaries into a single comprehensive master summary. Highlight critical events, character developments, quest progress, and relationship changes. This master summary should represent the entire 3-day period.

Last 3 Days Summaries:
$threeDaySummaries

3-Day Master Summary:
                """.trimIndent()
            }

            val result = try {
                googleClient.generateStory(prompt)
            } catch (e: Exception) {
                GameLogger.logError(TAG, "❌ Google AI generation failed for 3-day summary!", e)
                gameStateManager.setMemorySynthesisState(false)
                return@withContext
            }

            if (result?.isSuccess == true) {
                val masterSummary = result.getOrNull()?.storyText

                if (masterSummary != null && masterSummary.isNotBlank()) {
                    val endTime = System.currentTimeMillis()
                    val latency = endTime - startTime

                    GameLogger.logAISystem("⏱️ Performance: ${latency}ms, Master Summary: ${masterSummary.length} chars")
                    GameLogger.logAISystem("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    GameLogger.logAISystem("📄 3-DAY MASTER SUMMARY FOR DAY $currentDay:")
                    GameLogger.logAISystem(masterSummary)
                    GameLogger.logAISystem("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                    // Save master summary with special marker
                    val memoryEntity = LongTermMemoryEntity(
                        day = currentDay,
                        summaryText = "[3-DAY MASTER] $masterSummary"
                    )

                    try {
                        database.longTermMemoryDao().insertMemory(memoryEntity)
                        GameLogger.logAISystem("✅ [DATABASE] 3-day master summary saved successfully")
                    } catch (e: Exception) {
                        GameLogger.logError(TAG, "❌ Database insert failed for 3-day summary!", e)
                    }
                } else {
                    GameLogger.logError(TAG, "❌ Google AI returned empty master summary", null)
                }
            } else {
                GameLogger.logError(TAG, "❌ Google AI failed for 3-day summary", null)
            }

            gameStateManager.setMemorySynthesisState(false)
            GameLogger.logAISystem("✅✅✅ 3-DAY MASTER SUMMARY COMPLETE - Day $currentDay")
            GameLogger.logAISystem("════════════════════════════════════════════════════")
            GameLogger.logAISystem("🔵🔵🔵 [3-DAY-SUMMARY] ═══ TAMAMLANDI - DAY $currentDay ═══")

        } catch (t: Throwable) {
            Log.e(TAG, "❌❌❌ FATAL ERROR - Failed to create 3-day master summary for day $currentDay!", t)
            GameLogger.logError(TAG, "❌❌❌ FATAL ERROR in synthesizeThreeDayMasterSummary", t as? Exception ?: Exception(t))
            gameStateManager.setMemorySynthesisState(false)
        }
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        flashMemory.clear()
        GameLogger.logAISystem("MemoryManager cleaned up")
    }
}
