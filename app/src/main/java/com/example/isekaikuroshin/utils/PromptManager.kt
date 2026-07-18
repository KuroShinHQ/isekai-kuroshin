package com.example.isekaikuroshin.utils

import com.example.isekaikuroshin.data.PersistentDataManager
import com.example.isekaikuroshin.data.GameStateZ7
import kotlinx.coroutines.flow.first

/**
 * Centralized Prompt Management for Multi-language Support
 */
object PromptManager {

    suspend fun getStoryPrompt(
        userInput: String,
        gameState: GameStateZ7,
        recentHistory: String
    ): String {
        val language = PersistentDataManager.gameData.first().settingsData.gameSettings.language

        return when (language) {
            "EN" -> buildStoryPromptEN(userInput, gameState, recentHistory)
            else -> buildStoryPromptTR(userInput, gameState, recentHistory) // Default TR
        }
    }

    suspend fun getTimeSkipPrompt(currentTime: String): String {
        val language = PersistentDataManager.gameData.first().settingsData.gameSettings.language

        return when (language) {
            "EN" -> "During this $currentTime period, our hero quietly spent their time. Write a brief summary (1-2 sentences)."
            else -> "Bu $currentTime vakti boyunca kahramanımız sessizce zamanını geçirdi. Kısa bir özet yaz (1-2 cümle)."
        }
    }

    suspend fun getAICompanionBasePrompt(): AICompanionPrompts {
        val language = PersistentDataManager.gameData.first().settingsData.gameSettings.language

        return when (language) {
            "EN" -> getAICompanionPromptsEN()
            else -> getAICompanionPromptsTR() // Default TR
        }
    }

    private suspend fun buildStoryPromptTR(
        userInput: String,
        gameState: GameStateZ7,
        recentHistory: String
    ): String {
        val settings = PersistentDataManager.gameData.first().settingsData
        val externalStorySource = settings.storySettings.externalStorySource

        val basePrompt = """
            Sen bir Isekai/Fantasy RPG hikaye yazarısın. Kullanıcının eylemlerine göre sürükleyici hikaye yaratın.

            $recentHistory

            Mevcut Durum:
            Gün: ${gameState.currentDay}
            Zaman: ${gameState.currentTimeOfDay.displayName}
            Konum: ${gameState.knownLocations.find { it.id == gameState.currentLocationId }?.name ?: gameState.currentLocationId}
            Oyuncu: ${gameState.playerState.playerName}

            Kullanıcının eylemi: \"$userInput\"

            Bu eyleme göre akıcı ve doğal bir hikaye yazın. Türkçe karakterleri doğru kullanın.

            KURALLAR:
            - Sadece hikaye yazın, seçenek sunmayın
            - Öneride bulunmayın, sadece olayları anlatın
            - JSON formatı kullanmayın
            - Gerçekçi diyaloglar ve betimlemeler yapın
            - 2-3 paragraf uzunluğunda yanıt verin
            - Türkçe ı, ü, ö, ç, ş, ğ harflerini doğru kullanın
        """.trimIndent()

        val storySourcePrompt = if (externalStorySource.isNotBlank()) {
            "\n\nAyrıca, hikayeyi ve atmosferi şekillendirirken şu metni ana kaynak olarak kullan: '$externalStorySource'"
        } else {
            ""
        }

        return "$basePrompt$storySourcePrompt\n\nSadece hikayeyi devam ettirin."
    }

    private suspend fun buildStoryPromptEN(
        userInput: String,
        gameState: GameStateZ7,
        recentHistory: String
    ): String {
        val settings = PersistentDataManager.gameData.first().settingsData
        val externalStorySource = settings.storySettings.externalStorySource

        val basePrompt = """
            You are an Isekai/Fantasy RPG storyteller. Create an engaging story based on the user's actions.

            $recentHistory

            Current Situation:
            Day: ${gameState.currentDay}
            Time: ${gameState.currentTimeOfDay.displayName}
            Location: ${gameState.knownLocations.find { it.id == gameState.currentLocationId }?.name ?: gameState.currentLocationId}
            Player: ${gameState.playerState.playerName}

            User's action: \"$userInput\"

            Write a smooth and natural story based on this action.

            RULES:
            - Only write the story, don't offer choices
            - Don't make suggestions, just narrate events
            - Don't use JSON format
            - Use realistic dialogues and descriptions
            - Write 2-3 paragraphs long response
        """.trimIndent()

        val storySourcePrompt = if (externalStorySource.isNotBlank()) {
            "\n\nAlso, use the following text as the main source for shaping the story and atmosphere: '$externalStorySource'"
        } else {
            ""
        }

        return "$basePrompt$storySourcePrompt\n\nContinue the story only."
    }

    private fun getAICompanionPromptsTR(): AICompanionPrompts {
        return AICompanionPrompts(
            systemPrompt = "Sen Umbros adında eski bir şeytansın. Gizemli, hafif kibirli ama yardımcısın.",
            greetingPatterns = mapOf(
                "greeting" to "Varlığını yine hissediyorum, ölümlü.",
                "farewell" to "Yollarımız tekrar kesişene kadar...",
                "combat" to "Düşmanlarımızın üzerine yıkım saçalım mı?",
                "casual" to "Şimdi hangi önemsiz konu seni meşgul ediyor?",
                "praise" to "Takdirin... not edildi.",
                "insult" to "Küstahlığın unutulmayacak."
            ),
            moodDescriptions = mapOf(
                "very_friendly" to "Çok Samimi",
                "friendly" to "Samimi",
                "neutral" to "Nötr",
                "dismissive" to "Umursamaz",
                "hostile" to "Düşmanca"
            ),
            responseInstruction = "Umbros olarak cevap ver, karakterini tutarlı tut. Yanıtını 100 kelimeden kısa tut."
        )
    }

    private fun getAICompanionPromptsEN(): AICompanionPrompts {
        return AICompanionPrompts(
            systemPrompt = "You are Umbros, an ancient demon. Mysterious, slightly arrogant but helpful.",
            greetingPatterns = mapOf(
                "greeting" to "I sense your presence again, mortal.",
                "farewell" to "Until our paths cross once more...",
                "combat" to "Shall we unleash destruction upon our enemies?",
                "casual" to "What trivial matter concerns you now?",
                "praise" to "Your acknowledgment is... noted.",
                "insult" to "Your insolence will not be forgotten."
            ),
            moodDescriptions = mapOf(
                "very_friendly" to "Very Friendly",
                "friendly" to "Friendly",
                "neutral" to "Neutral",
                "dismissive" to "Dismissive",
                "hostile" to "Hostile"
            ),
            responseInstruction = "Respond as Umbros keeping your personality consistent. Keep response under 100 words and maintain character."
        )
    }
}

data class AICompanionPrompts(
    val systemPrompt: String,
    val greetingPatterns: Map<String, String>,
    val moodDescriptions: Map<String, String>,
    val responseInstruction: String
)
