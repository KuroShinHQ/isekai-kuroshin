package com.example.isekaikuroshin.utils

import android.content.Context
import com.example.isekaikuroshin.data.TimeOfDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Story Context Manager - Hikaye verilerini JSON olarak kaydeder
 * Her gün sonunda (5 vakit tamamlandığında) context'i kaydeder
 */
@Serializable
data class StoryContext(
    val day: Int,
    val pages: List<String>,
    val playerActions: List<String>,
    val discoveredItems: List<String>,
    val statChanges: Map<String, Int>,
    val timeProgression: List<String>, // Hangi vakitlerde hangi eylemler yapıldı
    val lastSaveTime: Long = System.currentTimeMillis()
)

object StoryContextManager {

    private var context: Context? = null
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun initialize(appContext: Context) {
        context = appContext.applicationContext
        GameLogger.logSaveSystem("StoryContextManager initialized")
    }

    // Güncel story context'i al
    fun getCurrentContext(
        day: Int,
        pages: List<String>,
        actions: List<String>,
        items: List<String>,
        stats: Map<String, Int>,
        timeProgression: List<String>
    ): StoryContext {
        return StoryContext(
            day = day,
            pages = pages,
            playerActions = actions,
            discoveredItems = items,
            statChanges = stats,
            timeProgression = timeProgression
        )
    }

    // Story context'i kaydet
    suspend fun saveStoryContext(storyContext: StoryContext): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val contextFile = getStoryContextFile(storyContext.day)
            val jsonString = json.encodeToString(StoryContext.serializer(), storyContext)

            contextFile.writeText(jsonString)

            GameLogger.logSaveSystem("Story context saved for Day ${storyContext.day}")
            GameLogger.logSaveSystem("Pages: ${storyContext.pages.size}, Actions: ${storyContext.playerActions.size}")
            true
        } catch (e: Exception) {
            GameLogger.logError("SAVE_SYSTEM", "Failed to save story context for Day ${storyContext.day}", e)
            false
        }
    }

    // Story context'i yükle
    suspend fun loadStoryContext(day: Int): StoryContext? = withContext(Dispatchers.IO) {
        return@withContext try {
            val contextFile = getStoryContextFile(day)
            if (!contextFile.exists()) {
                GameLogger.logSaveSystem("No story context found for Day $day")
                return@withContext null
            }

            val jsonString = contextFile.readText()
            val storyContext = json.decodeFromString(StoryContext.serializer(), jsonString)

            GameLogger.logSaveSystem("Story context loaded for Day $day")
            GameLogger.logSaveSystem("Loaded: ${storyContext.pages.size} pages, ${storyContext.playerActions.size} actions")
            storyContext
        } catch (e: Exception) {
            GameLogger.logError("SAVE_SYSTEM", "Failed to load story context for Day $day", e)
            null
        }
    }

    // Son 3 günün context'ini al (AI için)
    suspend fun getRecentContext(currentDay: Int): List<StoryContext> = withContext(Dispatchers.IO) {
        val recentContexts = mutableListOf<StoryContext>()

        // Son 3 günü kontrol et (bugün dahil değil, sadece geçmiş)
        for (day in maxOf(1, currentDay - 3) until currentDay) {
            loadStoryContext(day)?.let { context ->
                recentContexts.add(context)
            }
        }

        GameLogger.logSaveSystem("Loaded ${recentContexts.size} recent contexts for AI prompt")
        return@withContext recentContexts
    }

    // Tüm kaydedilmiş günleri listele
    suspend fun getSavedDays(): List<Int> = withContext(Dispatchers.IO) {
        return@withContext try {
            val storyDir = getStoryDirectory()
            if (!storyDir.exists()) return@withContext emptyList()

            val savedDays = storyDir.listFiles()
                ?.filter { it.name.startsWith("story_day_") && it.name.endsWith(".json") }
                ?.mapNotNull { file ->
                    val dayString = file.name.removePrefix("story_day_").removeSuffix(".json")
                    dayString.toIntOrNull()
                }
                ?.sorted()
                ?: emptyList()

            GameLogger.logSaveSystem("Found ${savedDays.size} saved story days: $savedDays")
            savedDays
        } catch (e: Exception) {
            GameLogger.logError("SAVE_SYSTEM", "Failed to get saved days", e)
            emptyList()
        }
    }

    // Helper functions
    private fun getStoryDirectory(): File {
        val appContext = context ?: throw IllegalStateException("StoryContextManager not initialized")
        val storyDir = File(appContext.filesDir, "story_contexts")
        if (!storyDir.exists()) {
            storyDir.mkdirs()
            GameLogger.logSaveSystem("Created story contexts directory")
        }
        return storyDir
    }

    private fun getStoryContextFile(day: Int): File {
        return File(getStoryDirectory(), "story_day_${day}.json")
    }

    // Debug: Tüm kaydedilmiş context'leri logla
    suspend fun debugLogAllContexts() {
        val savedDays = getSavedDays()
        GameLogger.logSaveSystem("=== DEBUG: ALL SAVED CONTEXTS ===")

        for (day in savedDays) {
            val context = loadStoryContext(day)
            context?.let {
                GameLogger.logSaveSystem("Day $day: ${it.pages.size} pages, ${it.playerActions.size} actions")
                it.pages.forEachIndexed { index, page ->
                    GameLogger.logSaveSystem("  Page ${index + 1}: ${page.take(50)}...")
                }
            }
        }
        GameLogger.logSaveSystem("=== END DEBUG ===")
    }
}