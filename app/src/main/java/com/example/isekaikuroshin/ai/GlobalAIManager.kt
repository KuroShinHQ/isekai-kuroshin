package com.example.isekaikuroshin.ai

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import android.content.SharedPreferences
import com.example.isekaikuroshin.utils.PromptManager

// AI Companion Memory & Personality Data Classes
data class ConversationMemory(
    val userMessages: MutableList<String> = mutableListOf(),
    val aiResponses: MutableList<String> = mutableListOf(),
    val contextSummary: String = "",
    val personalityTriggers: MutableMap<String, Int> = mutableMapOf(),
    val userPreferences: MutableMap<String, String> = mutableMapOf()
)

data class AIPersonality(
    val name: String = "Umbros",
    val basePersonality: String = "Ancient demon, mysterious, slightly arrogant but helpful",
    val moodLevel: Int = 50, // 0-100 (hostile to friendly)
    val relationshipLevel: Int = 1, // 1-10 trust level
    val memoryStrength: Int = 5, // How many past conversations to remember
    val responsePatterns: Map<String, String> = mapOf(
        "greeting" to "I sense your presence again, mortal.",
        "farewell" to "Until our paths cross once more...",
        "combat" to "Shall we unleash destruction upon our enemies?",
        "casual" to "What trivial matter concerns you now?",
        "praise" to "Your acknowledgment is... noted.",
        "insult" to "Your insolence will not be forgotten."
    )
)

/**
 * Global AI Manager - Application-level singleton for AI model management
 * Handles lazy loading, background initialization, memory, and personality system
 */
object GlobalAIManager {

    private var llmInference: LlmInference? = null
    private var applicationContext: Context? = null
    private var sharedPreferences: SharedPreferences? = null

    // AI Memory & Personality System
    private var conversationMemory = ConversationMemory()
    private var aiPersonality = AIPersonality()
    private var conversationCount = 0

    // Strateji #3 Memory Manager
    private var memoryManager: com.example.isekaikuroshin.ai.MemoryManager? = null

    // AI State Management
    private val _isModelInitialized = MutableStateFlow(false)
    val isModelInitialized: StateFlow<Boolean> = _isModelInitialized.asStateFlow()

    private val _loadingProgress = MutableStateFlow(0f)
    val loadingProgress: StateFlow<Float> = _loadingProgress.asStateFlow()

    private val _loadingStatus = MutableStateFlow("AI ready to load")
    val loadingStatus: StateFlow<String> = _loadingStatus.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val aiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Initialize the Global AI Manager with application context
     * Should be called once from Application.onCreate()
     */
    fun initialize(context: Context) {
        applicationContext = context.applicationContext
        sharedPreferences = context.getSharedPreferences("ai_memory", Context.MODE_PRIVATE)
        Log.d("GlobalAIManager", "Initialized with application context")

        // Load saved memory and personality
        loadMemoryFromPreferences()

        // Check if model already exists and set initial state
        checkExistingModel()
    }

    /**
     * Start AI model loading (triggered from Dashboard red eye)
     * This is the lazy loading entry point
     * KARMA FIX: Settings'den useGeminiNano kontrolü eklendi
     */
    fun startAILoading() {
        if (_isLoading.value || _isModelInitialized.value) {
            Log.d("GlobalAIManager", "AI loading already in progress or completed")
            return
        }

        // G132 FIX: textGenerationModel kontrolü (useGeminiNano DEPRECATED)
        // Artık textGenerationModel == "GEMMA_LOCAL" kontrolü yapılıyor
        val context = applicationContext
        if (context != null) {
            try {
                val settingsData = com.example.isekaikuroshin.data.PersistentDataManager.getSettingsDataBlocking(context)

                // G132: useGeminiNano yerine textGenerationModel kontrol et
                if (settingsData.apiSettings.textGenerationModel != "GEMMA_LOCAL") {
                    Log.w("GlobalAIManager", "❌ Gemma Local NOT selected (current: ${settingsData.apiSettings.textGenerationModel}) - skipping model load")
                    _loadingStatus.value = "Gemma Local not selected (using ${settingsData.apiSettings.textGenerationModel})"
                    return
                }

                Log.d("GlobalAIManager", "✅ Gemma Local selected - proceeding with model load")
            } catch (e: Exception) {
                Log.e("GlobalAIManager", "Failed to check textGenerationModel setting: ${e.message}")
            }
        }

        Log.d("GlobalAIManager", "Starting AI model loading from Dashboard trigger")
        _isLoading.value = true
        _loadingStatus.value = "Initializing AI..."
        _loadingProgress.value = 0f

        aiScope.launch {
            try {
                val appContext = applicationContext ?: throw IllegalStateException("Context not initialized")

                // Copy model from assets to external storage with progress
                val modelFile = copyModelFromAssets(appContext)

                // Initialize MediaPipe LLM
                _loadingStatus.value = "Loading AI engine..."
                _loadingProgress.value = 0.9f

                // XNNPACK cache temizleme - crash fix
                try {
                    val cacheDir = appContext.cacheDir
                    val xnnpackCache = File(cacheDir, "gemma3-1b-it-int4.litertlm.xnnpack_cache")
                    if (xnnpackCache.exists()) {
                        xnnpackCache.delete()
                        Log.d("GlobalAIManager", "XNNPACK cache temizlendi: ${xnnpackCache.absolutePath}")
                    }
                } catch (e: Exception) {
                    Log.w("GlobalAIManager", "XNNPACK cache temizlenemedi (devam ediliyor): ${e.message}")
                }

                val options = LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .build()

                Log.d("GlobalAIManager", "Creating LLM inference from file: ${modelFile.absolutePath}")

                // Model yükleme - XNNPACK hatalarını yakala
                try {
                    llmInference = LlmInference.createFromOptions(context, options)
                    Log.d("GlobalAIManager", "AI model initialized successfully!")
                } catch (e: Exception) {
                    Log.e("GlobalAIManager", "LLM creation failed, retrying with cleared cache...", e)

                    // Cache tamamen temizle ve tekrar dene
                    try {
                        appContext.cacheDir.listFiles()?.forEach { file ->
                            if (file.name.contains("xnnpack") || file.name.contains(".cache")) {
                                file.delete()
                                Log.d("GlobalAIManager", "Deleted cache: ${file.name}")
                            }
                        }
                    } catch (ex: Exception) {
                        Log.w("GlobalAIManager", "Cache cleanup failed: ${ex.message}")
                    }

                    // Son deneme
                    llmInference = LlmInference.createFromOptions(context, options)
                    Log.d("GlobalAIManager", "AI model initialized on retry!")
                }

                _isModelInitialized.value = true
                _loadingProgress.value = 1.0f
                _loadingStatus.value = "AI ready!"
                _isLoading.value = false

            } catch (e: Exception) {
                Log.e("GlobalAIManager", "AI model initialization COMPLETELY failed: ${e.message}", e)
                _isModelInitialized.value = false
                _loadingStatus.value = "AI loading failed - ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Generate AI response with memory and personality (used by AI Dialog Screen)
     */
    suspend fun generateResponse(userMessage: String): String = withContext(Dispatchers.Default) {
        if (!isModelInitialized.value) {
            return@withContext "Error: AI model not ready. Trigger loading from Dashboard first."
        }

        llmInference?.let { llm ->
            try {
                // Add to conversation memory
                conversationMemory.userMessages.add(userMessage)
                conversationCount++

                // Add to Strateji #3 flash memory
                addDialogueToMemory("Player", userMessage)

                // Create enhanced prompt with memory and personality
                val enhancedPrompt = buildPersonalityPrompt(userMessage)

                // Ensure UTF-8 encoding for Turkish characters
                val normalizedPrompt = enhancedPrompt.toByteArray(Charsets.UTF_8).toString(Charsets.UTF_8)
                Log.d("GlobalAIManager", "Enhanced prompt: $normalizedPrompt")
                val result = llm.generateResponse(normalizedPrompt)

                // Save AI response to memory
                conversationMemory.aiResponses.add(result)
                addDialogueToMemory("AI", result)
                updatePersonalityBasedOnResponse(userMessage) // aiResponse removed here
                saveMemoryToPreferences()

                result
            } catch (e: Exception) {
                Log.e("GlobalAIManager", "Response generation failed: ${e.message}", e)
                "Error: Could not generate AI response."
            }
        } ?: "Error: AI model not available."
    }

    /**
     * Check if model file already exists in external storage
     */
    private fun checkExistingModel() {
        val context = applicationContext ?: return
        val fileName = "gemma3-1b-it-int4.litertlm"
        val externalDir = context.getExternalFilesDir(null)
        val modelFile = File(externalDir, fileName)

        if (modelFile.exists()) {
            Log.d("GlobalAIManager", "Model file already exists - ready for quick loading")
            _loadingStatus.value = "AI ready (cached)"
        }
    }

    /**
     * Copy model from assets to external storage with progress tracking
     */
    private suspend fun copyModelFromAssets(context: Context): File = withContext(Dispatchers.IO) {
        val fileName = "gemma3-1b-it-int4.litertlm"
        val externalDir = context.getExternalFilesDir(null)
        val modelFile = File(externalDir, fileName)

        if (modelFile.exists()) {
            Log.d("GlobalAIManager", "Model already exists in external storage")
            return@withContext modelFile
        }

        Log.d("GlobalAIManager", "Copying model from assets...")
        _loadingStatus.value = "Copying AI model..."

        context.assets.open(fileName).use { inputStream ->
            modelFile.outputStream().use { outputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytes = 0L
                val totalSize = 700000000L // ~700MB approximate size for gemma3-1b

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytes += bytesRead
                    val progress = (totalBytes.toFloat() / totalSize.toFloat()).coerceIn(0f, 0.8f)
                    _loadingProgress.value = progress

                    if (totalBytes % (50 * 1024 * 1024) == 0L) { // Update every 50MB
                        val progressPercent = (progress * 100).toInt()
                        _loadingStatus.value = "Copying AI model... $progressPercent%"
                        Log.d("GlobalAIManager", "Copied ${totalBytes / (1024 * 1024)}MB")
                    }
                }
            }
        }

        Log.d("GlobalAIManager", "Model copied to: ${modelFile.absolutePath}")
        modelFile
    }

    /**
     * Build enhanced prompt with personality and memory context
     */
    private suspend fun buildPersonalityPrompt(userMessage: String): String {
        val recentMemory = getRecentConversationContext()
        val personalityContext = buildPersonalityContext()
        val relationshipContext = buildRelationshipContext()

        // Get Strateji #3 long-term memory context
        val longTermContext = getMemoryContext(3)

        // Get language-specific prompts from PromptManager
        val aiPrompts = PromptManager.getAICompanionBasePrompt()

        val moodKey = when {
            aiPersonality.moodLevel >= 80 -> "very_friendly"
            aiPersonality.moodLevel >= 60 -> "friendly"
            aiPersonality.moodLevel >= 40 -> "neutral"
            aiPersonality.moodLevel >= 20 -> "dismissive"
            else -> "hostile"
        }

        return """
${aiPrompts.systemPrompt}

${personalityContext}
${relationshipContext}

${if (longTermContext.isNotEmpty()) "$longTermContext\n" else ""}
Recent conversation context:
${recentMemory}

Current relationship level: ${aiPersonality.relationshipLevel}/10
Current mood: ${aiPrompts.moodDescriptions[moodKey] ?: "Neutral"}

User says: \"$userMessage\"

${aiPrompts.responseInstruction}
        """.trimIndent()
    }

    /**
     * Build personality context based on current stats
     */
    private fun buildPersonalityContext(): String {
        return when {
            aiPersonality.relationshipLevel >= 8 -> "You've grown to respect this mortal's determination."
            aiPersonality.relationshipLevel >= 5 -> "You're beginning to find this human somewhat interesting."
            aiPersonality.relationshipLevel >= 3 -> "This mortal has proven they're not completely worthless."
            else -> "You barely tolerate this insignificant mortal's presence."
        }
    }

    /**
     * Build relationship context based on conversation history
     */
    private fun buildRelationshipContext(): String {
        val totalConversations = conversationMemory.userMessages.size
        return when {
            totalConversations >= 20 -> "You've had many conversations with this human. They've earned some respect."
            totalConversations >= 10 -> "You've spoken with this mortal several times. They're persistent."
            totalConversations >= 5 -> "This human has returned to speak with you multiple times."
            else -> "This is still a new acquaintance to you."
        }
    }

    /**
     * Get recent conversation context for memory
     */
    private fun getRecentConversationContext(): String {
        val memoryLimit = aiPersonality.memoryStrength
        val recentMessages = conversationMemory.userMessages.takeLast(memoryLimit)
        val recentResponses = conversationMemory.aiResponses.takeLast(memoryLimit)

        if (recentMessages.isEmpty()) return "This is your first conversation."

        val context = StringBuilder()
        for (i in recentMessages.indices) {
            if (i < recentResponses.size) {
                context.append("User: ${recentMessages[i]}\n")
                context.append("You: ${recentResponses[i]}\n")
            }
        }
        return context.toString()
    }

    /**
     * Update personality based on user message and AI response
     */
    private fun updatePersonalityBasedOnResponse(userMessage: String) { // aiResponse parameter removed
        // Analyze user message for personality triggers
        val message = userMessage.lowercase()

        when {
            // Positive interactions
            message.contains("thank") || message.contains("please") -> {
                aiPersonality = aiPersonality.copy(
                    moodLevel = (aiPersonality.moodLevel + 2).coerceAtMost(100),
                    relationshipLevel = if (aiPersonality.moodLevel >= 70) {
                        (aiPersonality.relationshipLevel + 1).coerceAtMost(10)
                    } else aiPersonality.relationshipLevel
                )
            }

            // Combat/adventure related
            message.contains("fight") || message.contains("battle") || message.contains("combat") -> {
                conversationMemory.personalityTriggers["combat"] =
                    conversationMemory.personalityTriggers.getOrDefault("combat", 0) + 1
                aiPersonality = aiPersonality.copy(moodLevel = (aiPersonality.moodLevel + 3).coerceAtMost(100))
            }

            // Negative interactions
            message.contains("stupid") || message.contains("useless") -> {
                aiPersonality = aiPersonality.copy(
                    moodLevel = (aiPersonality.moodLevel - 5).coerceAtLeast(0)
                )
            }

            // Questions about AI itself
            message.contains("how are you") || message.contains("what are you") -> {
                conversationMemory.personalityTriggers["personal"] =
                    conversationMemory.personalityTriggers.getOrDefault("personal", 0) + 1
            }
        }

        // Gradual relationship building over time
        if (conversationCount % 5 == 0 && aiPersonality.moodLevel >= 60) {
            aiPersonality = aiPersonality.copy(
                relationshipLevel = (aiPersonality.relationshipLevel + 1).coerceAtMost(10)
            )
        }
    }

    /**
     * Save memory to SharedPreferences
     */
    private fun saveMemoryToPreferences() {
        sharedPreferences?.edit()?.apply {
            // Save conversation count and personality stats
            putInt("conversation_count", conversationCount)
            putInt("mood_level", aiPersonality.moodLevel)
            putInt("relationship_level", aiPersonality.relationshipLevel)

            // Save recent messages (last 10)
            val recentMessages = conversationMemory.userMessages.takeLast(10)
            val recentResponses = conversationMemory.aiResponses.takeLast(10)

            putString("recent_user_messages", recentMessages.joinToString("|||"))
            putString("recent_ai_responses", recentResponses.joinToString("|||"))

            // Save personality triggers
            conversationMemory.personalityTriggers.forEach { (key, value) ->
                putInt("trigger_$key", value)
            }

            apply()
        }

        Log.d("GlobalAIManager", "Memory saved - Conversations: $conversationCount, Mood: ${aiPersonality.moodLevel}, Relationship: ${aiPersonality.relationshipLevel}")
    }

    /**
     * Load memory from SharedPreferences
     */
    private fun loadMemoryFromPreferences() {
        sharedPreferences?.let { prefs ->
            conversationCount = prefs.getInt("conversation_count", 0)

            aiPersonality = aiPersonality.copy(
                moodLevel = prefs.getInt("mood_level", 50),
                relationshipLevel = prefs.getInt("relationship_level", 1)
            )

            // Load recent messages
            val userMessages = prefs.getString("recent_user_messages", "")?.split("|||")?.filter { it.isNotEmpty() } ?: emptyList()
            val aiResponses = prefs.getString("recent_ai_responses", "")?.split("|||")?.filter { it.isNotEmpty() } ?: emptyList()

            conversationMemory.userMessages.clear()
            conversationMemory.userMessages.addAll(userMessages)
            conversationMemory.aiResponses.clear()
            conversationMemory.aiResponses.addAll(aiResponses)

            // Load personality triggers
            conversationMemory.personalityTriggers["combat"] = prefs.getInt("trigger_combat", 0)
            conversationMemory.personalityTriggers["personal"] = prefs.getInt("trigger_personal", 0)

            Log.d("GlobalAIManager", "Memory loaded - Conversations: $conversationCount, Mood: ${aiPersonality.moodLevel}, Relationship: ${aiPersonality.relationshipLevel}")
        }
    }

    /**
     * Get current AI personality info (for debugging/display)
     */
    fun getPersonalityInfo(): Map<String, Any> {
        return mapOf(
            "name" to aiPersonality.name,
            "mood" to aiPersonality.moodLevel,
            "relationship" to aiPersonality.relationshipLevel,
            "conversations" to conversationCount,
            "memory_size" to conversationMemory.userMessages.size
        )
    }

    /**
     * Initialize MemoryManager with database access
     * STRATEJI #3: Dile duyarlı hibrit AI mimarisi
     * - EN: Lokal model (offline)
     * - TR: Google AI (online)
     */
    suspend fun initializeMemoryManager(
        context: android.content.Context,
        database: com.example.isekaikuroshin.data.database.AppDatabase,
        aiClientProvider: com.example.isekaikuroshin.engine.AIClientProvider,
        gameStateManager: com.example.isekaikuroshin.game.GameStateManager
    ) {
        memoryManager = com.example.isekaikuroshin.ai.MemoryManager(
            context = context,
            database = database,
            aiClientProvider = aiClientProvider,
            gameStateManager = gameStateManager
        )
        memoryManager?.initialize()
        com.example.isekaikuroshin.utils.GameLogger.logAISystem("MemoryManager initialized with hybrid AI architecture")
    }

    /**
     * Add dialogue to flash memory (called whenever user/AI interact)
     */
    fun addDialogueToMemory(speaker: String, message: String) {
        memoryManager?.addDialogue(speaker, message)
    }

    /**
     * Synthesize end-of-day memory (called at day end)
     */
    suspend fun synthesizeEndOfDayMemory(currentDay: Int) {
        memoryManager?.synthesizeMemory(currentDay)
    }

    /**
     * Get memory context for AI prompts
     */
    suspend fun getMemoryContext(lastNDays: Int = 3): String {
        return memoryManager?.getMemoryContext(lastNDays) ?: ""
    }

    /**
     * FIX #47: Synthesize 3-day master summary (FORCED GOOGLE API)
     */
    suspend fun synthesizeThreeDayMasterSummary(currentDay: Int) {
        memoryManager?.synthesizeThreeDayMasterSummary(currentDay)
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        llmInference?.close()
        llmInference = null
        memoryManager?.cleanup()
        memoryManager = null
        _isModelInitialized.value = false
        _isLoading.value = false
        Log.d("GlobalAIManager", "AI resources cleaned up")
    }
}
