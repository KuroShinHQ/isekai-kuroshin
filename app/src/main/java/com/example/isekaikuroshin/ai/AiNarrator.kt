package com.example.isekaikuroshin.ai

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AiNarrator class to handle interactions with the Gemma AI model.
 * Responsible for constructing prompts, sending requests, and receiving responses.
 */
class AiNarrator(private val context: Context) {

    private var llmInference: LlmInference? = null

    /**
     * Initializes the Gemma AI model.
     * This should be called once, ideally during application startup or when the AI is first needed.
     */
    fun initializeModel() {
        if (llmInference == null) {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath("/data/local/tmp/gemma-1.1-2b-it-cpu-int4.bin") // Adjust path as needed
                .build()
            llmInference = LlmInference.createFromOptions(context, options)
        }
    }

    /**
     * Generates a story segment based on the given prompt.
     * The AI is instructed to act as a 3rd person narrator.
     * @param prompt The input prompt for the AI.
     * @return The AI-generated story segment.
     */
    suspend fun generateStory(prompt: String): String = withContext(Dispatchers.IO) {
        llmInference?.generateResponse(prompt)?.firstOrNull()?.toString() ?: "Yapay zeka yanıt veremedi."
    }

    /**
     * Releases the AI model resources.
     */
    fun closeModel() {
        llmInference?.close()
        llmInference = null
    }
}