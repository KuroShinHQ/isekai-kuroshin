package com.example.isekaikuroshin.engine

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// AI yanıtını ve token kullanım bilgisini içeren data class
data class AIResponse(
    val storyText: String,
    val tokenCount: Int
)

// Tüm AI istemcilerinin uygulayacağı temel arayüz
interface AIClient {
    suspend fun generateStory(prompt: String): Result<AIResponse>
}

// FIX-JOURNAL: GlobalAIManager (Gemini/Gemma model) kullanarak gerçek local AI
class LocalAIClient(private val context: Context) : AIClient {

    init {
        // GlobalAIManager'ı context ile initialize et (eğer edilmemişse)
        com.example.isekaikuroshin.ai.GlobalAIManager.initialize(context)
    }

    override suspend fun generateStory(prompt: String): Result<AIResponse> = withContext(Dispatchers.IO) {
        try {
            // Model yüklenmemişse FALLBACK döndür (beklemeden!)
            if (!com.example.isekaikuroshin.ai.GlobalAIManager.isModelInitialized.value) {
                com.example.isekaikuroshin.utils.GameLogger.logSystem("⚠️ Local AI model not loaded, returning fallback")

                val playerActionMatch = Regex("""\[PLAYER ACTION\]\s*(.+?)\s*\[""").find(prompt)
                val playerAction = playerActionMatch?.groupValues?.get(1)?.trim() ?: "explored the area"

                val jsonResponse = """
{
  "journalEntry": "You $playerAction. (Local AI model yükleniyor, lütfen Settings'ten Google AI seçin veya birkaç saniye bekleyin)",
  "itemsGained": [],
  "questsUpdated": [],
  "statsChanged": {},
  "weatherChange": null,
  "timeShift": null,
  "locationsUnlocked": [],
  "locationChange": null,
  "npcStateChange": null,
  "nemesisEvolution": null,
  "macroEvent": null
}
                """.trimIndent()

                return@withContext Result.success(AIResponse(jsonResponse, 0))
            }

            if (com.example.isekaikuroshin.ai.GlobalAIManager.isModelInitialized.value) {
                // GlobalAIManager ile story üret
                val response = com.example.isekaikuroshin.ai.GlobalAIManager.generateResponse(prompt)

                // Token count estimation (roughly 4 characters per token)
                val estimatedTokens = (prompt.length + response.length) / 4

                com.example.isekaikuroshin.utils.GameLogger.logSystem("✅ Local AI generated response: ${response.take(100)}...")
                Result.success(AIResponse(response, estimatedTokens))
            } else {
                // Model yüklenemedi, fallback JSON döndür
                com.example.isekaikuroshin.utils.GameLogger.logSystem("⚠️ Local AI model failed to load, using fallback")

                val playerActionMatch = Regex("""\[PLAYER ACTION\]\s*(.+?)\s*\[""").find(prompt)
                val playerAction = playerActionMatch?.groupValues?.get(1)?.trim() ?: "explored the area"

                val jsonResponse = """
{
  "journalEntry": "You $playerAction. The world responds to your actions. (Local AI model is loading...)",
  "itemsGained": [],
  "questsUpdated": [],
  "statsChanged": {},
  "weatherChange": null,
  "timeShift": null,
  "locationsUnlocked": [],
  "locationChange": null,
  "npcStateChange": null,
  "nemesisEvolution": null,
  "macroEvent": null
}
                """.trimIndent()

                Result.success(AIResponse(jsonResponse, 0))
            }
        } catch (e: Exception) {
            com.example.isekaikuroshin.utils.GameLogger.logError("LocalAIClient", "Error generating story: ${e.message}", e)
            Result.failure(e)
        }
    }
}

// Geçersiz bir durumda kullanılacak boş bir istemci
class NoOpAIClient : AIClient {
     override suspend fun generateStory(prompt: String): Result<AIResponse> {
        return Result.success(AIResponse("AI Sağlayıcısı yapılandırılmamış.", 0))
    }
}