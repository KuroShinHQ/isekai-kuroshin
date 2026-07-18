package com.example.isekaikuroshin.engine

import android.content.Context
import com.example.isekaikuroshin.data.GameStateManager
import com.example.isekaikuroshin.data.PlayerState
import com.example.isekaikuroshin.data.Location
import com.example.isekaikuroshin.data.TimeOfDay
import com.example.isekaikuroshin.engine.Weather // DEĞİŞTİRİLDİ (data.Weather -> engine.Weather)
import com.example.isekaikuroshin.ai.GlobalAIManager
import com.example.isekaikuroshin.utils.GameLogger
import com.example.isekaikuroshin.utils.StoryContextManager
import com.example.isekaikuroshin.utils.PromptManager
import com.example.isekaikuroshin.data.PersistentDataManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Enhanced story context for location-aware AI generation
 */
data class StoryContext(
    val userAction: String,
    val playerStats: PlayerState,
    val currentLocation: Location,
    val timeOfDay: TimeOfDay,
    val weather: Weather, // Şimdi engine.Weather olmalı
    val recentEvents: List<String> = emptyList()
)

// Mock LlmInference for compilation purposes if the actual dependency is not resolved
interface MockLlmInference {
    fun generateResponse(prompt: String): String
    fun close()
}

class MockLlmInferenceImpl : MockLlmInference {
    override fun generateResponse(prompt: String): String {
        println("❌ DEBUG: Mock model çağrıldı - GERÇEK MODEL BAĞLANAMADI")
        println("❌ DEBUG: MediaPipe LLM model path: /data/local/tmp/gemma-1.1-2b-it-cpu-int4.bin")
        println("❌ DEBUG: Model dosyası mevcut değil veya MediaPipe bağımlılığı eksik")

        // GERÇEK HATA MESAJI - Mock story değil
        return "HATA: Yerel AI modeli başlatılamadı. MediaPipe bağımlılığı kontrol edilmeli."
    }
    override fun close() {
        println("❌ DEBUG: Mock LLM kapatıldı - GERÇEK MODEL YÜKLENEMEDİ")
    }
}

class BasicStoryEngine(
    context: Context,
    private val gameStateManager: GameStateManager
) {

    private var mockLlmInference: MockLlmInference? = null // Sadece hata durumunda

    // Google AI Studio API Client - Primary AI System
    // G110: Lazy initialization ile API key PersistentDataManager'dan alınıyor (hardcoded key kaldırıldı)
    private var googleAIClient: GoogleAIClient? = null

    init {
        // GlobalAIManager zaten MediaPipe'ı yönetiyor
        // Dashboard kırmızı göz ile başlatılıyor
        println("🎮 BasicStoryEngine: GlobalAIManager entegrasyonu hazır")
        GameLogger.logStorySystem("BasicStoryEngine initialized with GlobalAIManager integration")

        // StoryContextManager'ı initialize et
        StoryContextManager.initialize(context)

        // CharacterManager'ı initialize et
        com.example.isekaikuroshin.utils.CharacterManager.initialize(context)

        // Mock sadece gerçekten gerekirse oluşturulacak
        mockLlmInference = MockLlmInferenceImpl()
    }

    /**
     * G110: Get or create GoogleAIClient with API key from PersistentDataManager
     * Security fix: No hardcoded API keys, throws error if not configured
     * BUG FIX: Use geminiApiKey instead of customAPIKey (matches AIClientProvider)
     */
    private suspend fun getGoogleAIClient(): GoogleAIClient {
        if (googleAIClient == null) {
            val apiSettings = PersistentDataManager.gameData.first().settingsData.apiSettings
            val apiKey = apiSettings.geminiApiKey.ifBlank { // FIX: customAPIKey → geminiApiKey
                throw IllegalStateException("Google API key not configured! Please set API key in Settings.")
            }
            googleAIClient = GoogleAIClient(apiKey = apiKey)
            println("🔑 GoogleAIClient initialized with geminiApiKey from settings")
        }
        return googleAIClient!!
    }

    /**
     * Generates a story response based on a single user input.
     *
     * @param userInput The single input provided by the user.
     * @return The AI-generated story response.
     */
    suspend fun generateStoryResponse(userInput: String): String {
        GameLogger.logStorySystem("Starting story generation for input: $userInput")

        // Get current game state for context
        val gameState = gameStateManager.gameState.value

        // Get recent story context for AI memory
        val recentContexts = StoryContextManager.getRecentContext(gameState.currentDay)

        val prompt = PromptManager.getStoryPrompt(userInput, gameState, recentContexts.joinToString("\n") { context ->
            "Gün ${context.day}: ${context.pages.lastOrNull()?.take(100) ?: "Olay yok"}..."
        })
        GameLogger.logStorySystem("Built AI prompt with ${recentContexts.size} recent contexts")

        val storyText = withContext(Dispatchers.IO) {
            try {
                // Get API settings
                val apiSettings = PersistentDataManager.gameData.first().settingsData.apiSettings
                val selectedProvider = apiSettings.selectedProvider

                println("🤖 BasicStoryEngine: Starting AI generation...")
                println("🤖 Selected Provider: $selectedProvider")
                println("🤖 Prompt length: ${prompt.length}")

                when (selectedProvider) {
                    "GOOGLE" -> {
                        println("🌐 Using Google AI Studio API...")
                        val client = getGoogleAIClient() // G110: Lazy initialization
                        val googleResult = client.generateStory(prompt)

                        if (googleResult.isSuccess) {
                            val response = googleResult.getOrNull()
                            if (response != null && !response.storyText.isBlank()) {
                                println("✅ Google AI Success")
                                updateApiHealth("GOOGLE", true)

                                // Add dialogue to MemoryManager (Strateji #3)
                                GlobalAIManager.addDialogueToMemory("Player", userInput)
                                GlobalAIManager.addDialogueToMemory("AI", response.storyText)

                                return@withContext response.storyText
                            }
                        }

                        println("❌ Google AI failed, trying fallback...")
                        updateApiHealth("GOOGLE", false)
                        return@withContext tryFallbackProvider(prompt, "GOOGLE")
                    }

                    "LOCAL" -> {
                        println("🔌 Using Local Gemma model...")
                        if (GlobalAIManager.isModelInitialized.value) {
                            val localResult = GlobalAIManager.generateResponse(prompt)
                            if (localResult.isNotBlank() && !localResult.contains("Error:")) {
                                println("✅ Local AI Success")
                                updateApiHealth("LOCAL", true)

                                // Add dialogue to MemoryManager (Strateji #3)
                                GlobalAIManager.addDialogueToMemory("Player", userInput)
                                GlobalAIManager.addDialogueToMemory("AI", localResult)

                                return@withContext localResult
                            }
                        }

                        println("❌ Local AI failed, trying fallback...")
                        updateApiHealth("LOCAL", false)
                        return@withContext tryFallbackProvider(prompt, "LOCAL")
                    }

                    "CUSTOM" -> {
                        // TODO: Implement custom API provider
                        println("⚠️ Custom API not implemented yet, using Google...")
                        return@withContext tryGoogleAPI(prompt)
                    }

                    else -> {
                        println("⚠️ Unknown provider, using Google...")
                        return@withContext tryGoogleAPI(prompt)
                    }
                }

            } catch (e: Exception) {
                println("🎮 ERROR: Exception in BasicStoryEngine: ${e.message}")
                e.printStackTrace()
                "Yapay zeka yanıtı oluşturulurken bir hata oluştu. Hikaye manuel olarak devam ediyor..."
            }
        }

        // ⭐ ANA GÖREV KONTROLÜ: Hikaye üretildikten sonra ana quest tetiklenebilir mi kontrol et
        checkAndTriggerMainQuestIfNeeded(gameState.currentDay, storyText)

        return storyText
    }

    /**
     * Ana görev tetikleme kontrolü
     * 3 gün sonunda veya kullanıcı yerleşkeye ulaştığında ana quest tetiklenir
     */
    private fun checkAndTriggerMainQuestIfNeeded(currentDay: Int, storyText: String) {
        try {
            // Ana quest tetikleme kontrolü yap
            val newMainQuest = PersistentDataManager.checkAndTriggerMainQuest(currentDay, storyText)

            if (newMainQuest != null) {
                GameLogger.logSystem("🎯 [MAIN QUEST] Yeni ana görev tetiklendi: ${newMainQuest.title}")
                // TODO: SystemOverlay ile göster
                // showSystemOverlay(title = "🎯 YENİ ANA GÖREV!", message = newMainQuest.description)
            }
        } catch (e: Exception) {
            GameLogger.logSystem("⚠️ Ana görev kontrolünde hata: ${e.message}")
        }
    }

    /**
     * Ölüm özeti oluştur - GM'den 2-3 satırlık dramatik özet
     * @param characterName Karakter adı
     * @param deathCause Ölüm sebebi
     * @param location Ölüm yeri
     * @param activeTitles Aktif ünvanlar
     * @param recentStoryContext Son 3 günün hikaye özeti
     * @return GM'den üretilen dramatik ölüm özeti
     */
    suspend fun generateDeathSummary(
        characterName: String,
        deathCause: String,
        location: String,
        activeTitles: List<String>,
        recentStoryContext: String
    ): String {
        val titlesText = if (activeTitles.isNotEmpty()) {
            "\"${activeTitles.joinToString(", ")}\" ünvanlarıyla bilinen"
        } else {
            ""
        }

        val prompt = """
            Bir RPG oyununda karakter öldü. 2-3 cümlelik KISA ve DRAMATİK bir ölüm özeti yaz.

            Karakter: $characterName $titlesText
            Ölüm Yeri: $location
            Ölüm Sebebi: $deathCause
            Son Olaylar: $recentStoryContext

            Sadece özeti yaz, başka hiçbir şey ekleme. Dramatik ve etkileyici olsun.
        """.trimIndent()

        return withContext(Dispatchers.IO) {
            try {
                val apiSettings = PersistentDataManager.gameData.first().settingsData.apiSettings
                val selectedProvider = apiSettings.selectedProvider

                when (selectedProvider) {
                    "GOOGLE" -> {
                        val client = getGoogleAIClient() // G110: Lazy initialization
                        val result = client.generateStory(prompt)
                        if (result.isSuccess) {
                            result.getOrNull()?.storyText ?: "Bir kahraman düştü, hikayesi sona erdi."
                        } else {
                            "Karanlık güçler tarafından yenildi. Destanı burada sona erdi."
                        }
                    }
                    "LOCAL" -> {
                        if (GlobalAIManager.isModelInitialized.value) {
                            val result = GlobalAIManager.generateResponse(prompt)
                            if (result.isNotBlank() && !result.contains("Error:")) {
                                result
                            } else {
                                "Sonunun geldiği yerde, destanı burada sona erdi."
                            }
                        } else {
                            "Karanlık güçler tarafından yenildi. Hikayesi sona erdi."
                        }
                    }
                    else -> "Bir kahraman düştü, hikayesi sona erdi."
                }
            } catch (e: Exception) {
                GameLogger.logSystem("⚠️ Ölüm özeti oluştururken hata: ${e.message}")
                "Sonunda karanlık kazandı. Hikayesi burada sona erdi."
            }
        }
    }

    /**
     * Generates a story response with enhanced location and context information.
     *
     * @param storyContext The enhanced context containing location, weather, time and player info.
     * @return The AI-generated story response.
     */
    suspend fun generateStoryResponseWithContext(storyContext: StoryContext): String {
        GameLogger.logStorySystem("Starting context-aware story generation for: ${storyContext.userAction}")

        // Get current game state for additional context
        val gameState = gameStateManager.gameState.value

        // Get recent story context for AI memory
        val recentContexts = StoryContextManager.getRecentContext(gameState.currentDay)

        // Enhanced prompt with location, weather, and time information
        val enhancedPrompt = buildString {
            append("Hikaye Bağlamı:\n")
            append("Konum: ${storyContext.currentLocation.name} - ${storyContext.currentLocation.description}\n")
            append("Tehlike Seviyesi: ${storyContext.currentLocation.dangerLevel}\n")
            append("Zaman: ${storyContext.timeOfDay.displayName}\n")
            append("Hava Durumu: ${storyContext.weather.displayName}\n") // Burası artık doğru Weather'ı kullanmalı
            append("Mevcut Kaynaklar: ${storyContext.currentLocation.knownResources.joinToString(", ")}\n")
            // append("Olası Karşılaşmalar: ${storyContext.currentLocation.encounterTypes.joinToString(", ")}\n")
            append("\nOyuncu Eylemi: ${storyContext.userAction}\n")
            append("\nOyuncu İstatistikleri:\n")
            append("HP: ${storyContext.playerStats.currentHealth}, MP: ${storyContext.playerStats.currentMana}\n")
            append("Güç: ${storyContext.playerStats.strength}, Zeka: ${storyContext.playerStats.intelligence}, Çeviklik: ${storyContext.playerStats.agility}\n")
            append("\nYakın Zamandaki Olaylar:\n")
            append(recentContexts.joinToString("\n") { context ->
                "Gün ${context.day}: ${context.pages.lastOrNull()?.take(150) ?: "Olay yok"}..."
            })
            append("\nBu bağlama uygun, konumun özelliklerini, hava durumunu ve zamanı dikkate alan bir hikaye oluştur.")
        }

        GameLogger.logStorySystem("Built enhanced prompt with location context")

        return withContext(Dispatchers.IO) {
            try {
                // Get API settings
                val apiSettings = PersistentDataManager.gameData.first().settingsData.apiSettings
                val selectedProvider = apiSettings.selectedProvider

                println("🤖 BasicStoryEngine: Starting context-aware AI generation...")
                println("🌍 Location: ${storyContext.currentLocation.name}")
                println("🕐 Time: ${storyContext.timeOfDay.displayName}")
                println("🌤️ Weather: ${storyContext.weather.displayName}") // Burası artık doğru Weather'ı kullanmalı

                when (selectedProvider) {
                    "GOOGLE" -> {
                        println("🌐 Using Google AI Studio API...")
                        val client = getGoogleAIClient() // G110: Lazy initialization
                        val googleResult = client.generateStory(enhancedPrompt)

                        if (googleResult.isSuccess) {
                            val response = googleResult.getOrNull()
                            if (response != null && !response.storyText.isBlank()) {
                                println("✅ Context-aware Google AI Success")
                                updateApiHealth("GOOGLE", true)

                                // Add dialogue to MemoryManager (Strateji #3)
                                GlobalAIManager.addDialogueToMemory("Player", storyContext.userAction)
                                GlobalAIManager.addDialogueToMemory("AI", response.storyText)

                                return@withContext response.storyText
                            }
                        }

                        println("❌ Google AI failed, trying fallback...")
                        updateApiHealth("GOOGLE", false)
                        return@withContext tryFallbackProvider(enhancedPrompt, "GOOGLE")
                    }

                    "LOCAL" -> {
                        println("🔌 Using Local Gemma model...")
                        if (GlobalAIManager.isModelInitialized.value) {
                            val localResult = GlobalAIManager.generateResponse(enhancedPrompt)
                            if (localResult.isNotBlank() && !localResult.contains("Error:")) {
                                println("✅ Context-aware Local AI Success")
                                updateApiHealth("LOCAL", true)

                                // Add dialogue to MemoryManager (Strateji #3)
                                GlobalAIManager.addDialogueToMemory("Player", storyContext.userAction)
                                GlobalAIManager.addDialogueToMemory("AI", localResult)

                                return@withContext localResult
                            }
                        }

                        println("❌ Local AI failed, trying fallback...")
                        updateApiHealth("LOCAL", false)
                        return@withContext tryFallbackProvider(enhancedPrompt, "LOCAL")
                    }

                    "CUSTOM" -> {
                        // TODO: Implement custom API provider
                        println("⚠️ Custom API not implemented yet, using Google...")
                        return@withContext tryGoogleAPI(enhancedPrompt)
                    }

                    else -> {
                        println("⚠️ Unknown provider, using Google...")
                        return@withContext tryGoogleAPI(enhancedPrompt)
                    }
                }

            } catch (e: Exception) {
                println("🎮 ERROR: Exception in context-aware BasicStoryEngine: ${e.message}")
                e.printStackTrace()
                "Yapay zeka yanıtı oluşturulurken bir hata oluştu. Hikaye manuel olarak devam ediyor..."
            }
        }
    }

    private suspend fun updateApiHealth(provider: String, isHealthy: Boolean) {
        try {
            PersistentDataManager.updateSettingsData { settingsData ->
                settingsData.copy(
                    apiSettings = settingsData.apiSettings.copy(
                        googleApiHealthy = if (provider == "GOOGLE") isHealthy else settingsData.apiSettings.googleApiHealthy,
                        localModelHealthy = if (provider == "LOCAL") isHealthy else settingsData.apiSettings.localModelHealthy,
                        lastSuccessfulProvider = if (isHealthy) provider else settingsData.apiSettings.lastSuccessfulProvider
                    )
                )
            }
        } catch (e: Exception) {
            println("❌ Failed to update API health: ${e.message}")
        }
    }

    private suspend fun tryFallbackProvider(prompt: String, failedProvider: String): String {
        return when (failedProvider) {
            "GOOGLE" -> {
                // Try Local as fallback
                if (GlobalAIManager.isModelInitialized.value) {
                    val result = GlobalAIManager.generateResponse(prompt)
                    if (result.isNotBlank() && !result.contains("Error:")) {
                        updateApiHealth("LOCAL", true)
                        "🔌 [Fallback: Local] $result"
                    } else {
                        getFinalFallback()
                    }
                } else {
                    getFinalFallback()
                }
            }
            "LOCAL" -> {
                // Try Google as fallback
                tryGoogleAPI(prompt)
            }
            else -> getFinalFallback()
        }
    }

    private suspend fun tryGoogleAPI(prompt: String): String {
        val client = getGoogleAIClient() // G110: Lazy initialization
        val googleResult = client.generateStory(prompt)
        return if (googleResult.isSuccess) {
            val response = googleResult.getOrNull()
            if (response != null && !response.storyText.isBlank()) {
                updateApiHealth("GOOGLE", true)
                response.storyText
            } else {
                getFinalFallback()
            }
        } else {
            updateApiHealth("GOOGLE", false)
            getFinalFallback()
        }
    }

    private fun getFinalFallback(): String {
        return "🎮 AI sistemleri şu anda kullanılamıyor. Hikaye manuel olarak devam ediyor..."
    }

    @Suppress("unused")
    fun shutdown() {
        // GlobalAIManager kendi modelini yönetiyor, müdahale etmeye gerek yok
        mockLlmInference?.close() // Mock varsa onu da kapat
        mockLlmInference = null // DEĞİŞTİRİLDİ (mockLpmInference -> mockLlmInference)
        println("🎮 DEBUG: BasicStoryEngine kapatıldı. GlobalAIManager aktif.")
    }
}