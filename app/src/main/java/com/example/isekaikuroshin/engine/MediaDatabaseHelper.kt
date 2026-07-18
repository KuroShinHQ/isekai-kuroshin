package com.example.isekaikuroshin.engine

import android.content.Context
import com.example.isekaikuroshin.engine.MediaDatabaseBuilder.MediaMetadata

/**
 * MediaDatabaseHelper - Global singleton for accessing media database
 *
 * Provides easy access to MediaDatabase methods across the entire app
 * Automatically loads database on first access
 */
object MediaDatabaseHelper {

    private var databaseBuilder: MediaDatabaseBuilder? = null
    private var cachedDatabase: MediaDatabaseBuilder.MediaDatabase? = null

    /**
     * Initialize the helper with application context
     * Call this in Application.onCreate() or MainActivity.onCreate()
     */
    fun initialize(context: Context) {
        databaseBuilder = MediaDatabaseBuilder(context.applicationContext)
    }

    /**
     * Load database from assets or build new one
     */
    private fun getDatabase(): MediaDatabaseBuilder.MediaDatabase {
        if (cachedDatabase == null) {
            cachedDatabase = databaseBuilder?.buildAndSaveDatabase()
                ?: throw IllegalStateException("MediaDatabaseHelper not initialized! Call initialize(context) first.")
        }
        return cachedDatabase!!
    }

    /**
     * JOURNEY ve özel sistem videoları - asla FIRSTUSER/RETURNINGUSER'da gösterilmemeli
     * Bu içerikler karma sistemiyle etiketlenmemiş, özel amaçlı videolardır
     */
    private val EXCLUDED_SYSTEM_VIDEOS = listOf(
        "vid_journey", "vid_journey_1", "vid_journey_2", "vid_journey_3", "vid_journey_4",
        "angeldevil", "butterfly_transformation", "eye_effect",
        "page_turn", "page_turn_forward", "page_turn_backward", "reverse_page_turning",
        "book_opening", "book_closing", "book_waiting",
        "lotus_blossom_animation", "intro_animation"
    )

    /**
     * Get media for specific screen type and depth
     *
     * FALLBACK STRATEGY: Eğer istenen screenType'da medya yoksa, yakın screenType'ları dene:
     * - FIRSTUSER → RETURNINGUSER, UMBROS
     * - UMBROS → JOURNEY_TRANSITION, DEATH_TRANSITION
     * - Her zaman nötr attribute'leri (SURVIVAL, MYSTERY, DIVINE) önceliklendir
     *
     * ⚠️ JOURNEY ve sistem videoları FİLTRELENİR - bunlar özel amaçlı içeriklerdir
     */
    fun getMediaForScreen(screenType: String, depth: String): List<MediaMetadata> {
        val db = getDatabase()
        val allMedia = db.videos + db.photos

        // İlk önce exact match dene
        var results = allMedia.filter { media ->
            media.screenType == screenType && media.depth == depth &&
            !isExcludedSystemVideo(media.fileName)
        }

        // Eğer boşsa, fallback screenTypes kullan
        if (results.isEmpty()) {
            val fallbackScreenTypes = when (screenType.uppercase()) {
                "FIRSTUSER" -> listOf("RETURNINGUSER", "UMBROS")
                "RETURNINGUSER" -> listOf("FIRSTUSER", "UMBROS")
                "UMBROS" -> listOf("JOURNEY_TRANSITION", "DEATH_TRANSITION")
                else -> listOf("FIRSTUSER") // Genel fallback
            }

            // Fallback screenTypes'ı dene
            for (fallbackType in fallbackScreenTypes) {
                results = allMedia.filter { media ->
                    media.screenType == fallbackType && media.depth == depth &&
                    !isExcludedSystemVideo(media.fileName)
                }
                if (results.isNotEmpty()) {
                    android.util.Log.d("MediaDatabaseHelper", "⚠️ $screenType için medya bulunamadı, fallback kullanıldı: $fallbackType (${ results.size} medya)")
                    break
                }
            }
        }

        // Hala boşsa, depth filtresi olmadan nötr attribute'ları dene
        if (results.isEmpty()) {
            results = allMedia.filter { media ->
                media.primaryAttribute in listOf("SURVIVAL", "MYSTERY", "DIVINE", "NONE") &&
                !isExcludedSystemVideo(media.fileName)
            }
            if (results.isNotEmpty()) {
                android.util.Log.d("MediaDatabaseHelper", "⚠️ $screenType için medya bulunamadı, nötr attribute fallback kullanıldı (${results.size} medya)")
            }
        }

        return results
    }

    /**
     * Check if a video/photo should be excluded from normal content flow
     * JOURNEY ve özel sistem videoları asla FIRSTUSER/RETURNINGUSER'da gösterilmemeli
     */
    private fun isExcludedSystemVideo(fileName: String): Boolean {
        val fileNameLower = fileName.lowercase().removeSuffix(".mp4").removeSuffix(".jpg").removeSuffix(".png")
        return EXCLUDED_SYSTEM_VIDEOS.any { excluded ->
            fileNameLower.contains(excluded.lowercase())
        }
    }

    /**
     * Get media for specific screen type, karma range, and depth
     */
    fun getMediaForKarmaRange(
        screenType: String,
        karmaRange: ClosedFloatingPointRange<Float>,
        depth: String
    ): List<MediaMetadata> {
        val db = getDatabase()
        val allMedia = db.videos + db.photos

        return allMedia.filter { media ->
            media.screenType == screenType &&
            media.depth == depth &&
            isInKarmaRange(media, karmaRange)
        }
    }

    /**
     * Get metadata for specific resource ID
     */
    fun getMetadata(resourceId: Int): MediaMetadata? {
        val db = getDatabase()
        val allMedia = db.videos + db.photos

        return allMedia.find { it.resourceId == resourceId }
    }

    /**
     * Check if media's attributes match karma range
     * Neutral attributes (-0.2..0.2) always match
     */
    private fun isInKarmaRange(
        media: MediaMetadata,
        karmaRange: ClosedFloatingPointRange<Float>
    ): Boolean {
        val primaryAttr = media.primaryAttribute

        // Special neutral attributes always match
        if (primaryAttr in listOf("DIVINE", "MYSTERY", "SURVIVAL", "NONE")) {
            return true
        }

        // Positive attributes (LIGHT, MERCY, LOYALTY, COURAGE, SACRIFICE, ORDER)
        val positiveAttrs = listOf("LIGHT", "MERCY", "LOYALTY", "COURAGE", "SACRIFICE", "ORDER")

        // Negative attributes (VIOLENCE, CRUELTY, BETRAYAL, GREED, FEAR, CHAOS, etc.)
        val negativeAttrs = listOf("VIOLENCE", "CRUELTY", "BETRAYAL", "GREED", "FEAR", "CHAOS",
                                    "CORRUPTION", "DARK", "DEATH", "PAIN", "DESPAIR")

        return when {
            primaryAttr in positiveAttrs -> karmaRange.endInclusive > 0.3f
            primaryAttr in negativeAttrs -> karmaRange.start < -0.3f
            else -> true // Unknown attributes match all
        }
    }

    /**
     * Force refresh database (useful after GM updates media files)
     * Also deletes cached JSON file to force complete rebuild
     */
    fun refreshDatabase() {
        cachedDatabase = null
        // Delete cached JSON to force rebuild from drawable resources
        databaseBuilder?.let { builder ->
            try {
                val context = builder.javaClass.getDeclaredField("context").let { field ->
                    field.isAccessible = true
                    field.get(builder) as? android.content.Context
                }
                context?.let { ctx ->
                    val cacheFile = java.io.File(ctx.filesDir, "media_database.json")
                    if (cacheFile.exists()) {
                        cacheFile.delete()
                        android.util.Log.d("MediaDatabaseHelper", "🔄 Cached media_database.json deleted - forcing rebuild")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MediaDatabaseHelper", "⚠️ Failed to delete cache file: ${e.message}")
            }
        }
    }

    /**
     * Preload database in background (performance optimization)
     * Call this from a background thread to avoid blocking UI
     */
    fun preloadDatabase() {
        getDatabase() // This will trigger lazy build if not already built
    }
}
