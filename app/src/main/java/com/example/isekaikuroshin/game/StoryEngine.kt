package com.example.isekaikuroshin.game

import android.content.Context
import com.example.isekaikuroshin.ai.AiNarrator
import com.example.isekaikuroshin.data.GameStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * StoryEngine class to orchestrate game logic related to story progression.
 * Interacts with AiNarrator and GameStateManager.
 */
class StoryEngine(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val gameStateManager: GameStateManager
) {

    private val aiNarrator = AiNarrator(context)

    init {
        // Initialize AI model when StoryEngine is created
        aiNarrator.initializeModel()
    }

    /**
     * Processes user input, generates AI response, and updates game state.
     * @param userInput The primary input from the user.
     */
    fun processUserInput(userInput: String) {
        coroutineScope.launch {
            // Get last few story pages for context
            val storyContext = gameStateManager.gameState.value.storyPages.takeLast(5).joinToString("\n")

            // Construct prompt for AI
            val prompt = """
            Sen bir RPG oyununda 3. şahıs anlatıcısısın. Oyuncunun eylemlerini ve hikayenin gidişatını anlat.
            Mevcut Hikaye Bağlamı:
            $storyContext

            Oyuncunun Eylemi: $userInput

            Hikayeyi devam ettir:
            """.trimIndent()

            val aiResponse = aiNarrator.generateStory(prompt)

            // Update game state with AI response
            gameStateManager.addStoryPage(aiResponse)

            // Advance time
            gameStateManager.advanceTime()
        }
    }

    /**
     * Releases resources when no longer needed.
     */
    fun release() {
        aiNarrator.closeModel()
    }
}