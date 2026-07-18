package com.example.isekaikuroshin.engine

import android.content.Context
import android.util.Log
import com.example.isekaikuroshin.R
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * MediaDatabaseBuilder - Akıllı Kalp Sistemi için Medya Veritabanı Oluşturucu
 *
 * Görev: res/raw ve res/drawable klasörlerini tarayarak, yeni isimlendirme standardına göre
 * etiketlenmiş tüm medya dosyalarını bulup media_database.json oluşturur.
 *
 * Yeni İsimlendirme Standardı:
 * TÜR_EKRANTÜRÜ_NİTELİK_DUYGU_DERİNLİK_NUMARA
 *
 * Örnekler:
 * - VID_RETURNINGUSER_ORDER_JOY_D1_001.mp4
 * - VID_NEWUSER_CHAOS_SADNESS_D2_001.mp4
 * - VID_UMBROS_VIOLENCE_ANGER_D3_001.mp4
 * - PHT_JOURNEY_CONFLICT_FEAR_D2_001.png
 */
class MediaDatabaseBuilder(private val context: Context) {

    companion object {
        private const val TAG = "MediaDatabaseBuilder"
        private const val OUTPUT_FILENAME = "media_database.json"

        // ⚡ PERFORMANCE: Maksimum dosya limitleri (başlangıç yükünü azaltmak için)
        private const val MAX_VIDEOS_PER_SCREEN = 10  // Ekran başına max video
        private const val MAX_PHOTOS_PER_SCREEN = 10  // Ekran başına max fotoğraf

        // Desteklenen dosya türleri
        private val VIDEO_EXTENSIONS = setOf("mp4", "avi", "mkv", "webm")
        private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp")

        // 🆕 TOKEN OPTIMIZATION V3: ULTRA KISALTMA - Decode fonksiyonları

        /**
         * Dosya türü kodunu decode eder
         * Örnek: V → VIDEO, P → PHOTO
         */
        private fun decodeFileType(shortCode: String): String {
            return when (shortCode.uppercase()) {
                "V" -> "VIDEO"
                "P" -> "PHOTO"
                else -> shortCode
            }
        }

        /**
         * Ekran türü kodunu decode eder
         * Örnek: F1 → FIRSTUSER, U1 → UMBROS, J2 → JOURNEY_TRANSITION
         */
        private fun decodeScreenType(shortCode: String): String {
            return when (shortCode.uppercase()) {
                "F1" -> "FIRSTUSER"
                "F2" -> "RETURNINGUSER"
                "J1" -> "JOURNEY"
                "J2" -> "JOURNEY_TRANSITION"
                "P1" -> "POSTDEATH"
                "P2" -> "DEATH_TRANSITION"
                "P3" -> "DEATH_STATISTICS"
                "U1" -> "UMBROS"
                "L1" -> "LAUNCHER_ICON"
                else -> shortCode
            }
        }

        /**
         * Attribute kısa kodunu decode eder
         * Örnek: 1N15 → VIOLENCE, 1P04 → MERCY, S01 → DIVINE, 0 → NONE
         */
        private fun decodeAttributeShort(shortCode: String): String {
            if (shortCode == "0") return "NONE"
            if (shortCode.uppercase() == "NONE") return "NONE"

            // Özel attribute'ler (S01, S08, S09, S16)
            if (shortCode.startsWith("S", ignoreCase = true)) {
                val id = shortCode.substring(1).toIntOrNull() ?: return "NONE"
                return when (id) {
                    1 -> "DIVINE"
                    8 -> "MYSTERY"
                    9 -> "SURVIVAL"
                    16 -> "CORRUPTION"
                    else -> "NONE"
                }
            }

            // Eksen attribute'leri (1N15, 1P04, etc.)
            if (shortCode.length >= 4) {
                val attrId = shortCode.substring(2).toIntOrNull() ?: return "NONE"

                return when (attrId) {
                    1 -> "DIVINE"
                    2 -> "LIGHT"
                    3 -> "LOYALTY"
                    4 -> "MERCY"
                    5 -> "COURAGE"
                    6 -> "SACRIFICE"
                    7 -> "ORDER"
                    8 -> "MYSTERY"
                    9 -> "SURVIVAL"
                    10 -> "FEAR"
                    11 -> "SELFISH"
                    12 -> "CHAOS"
                    13 -> "DECEIT"
                    14 -> "DARK"
                    15 -> "VIOLENCE"
                    16 -> "CORRUPTION"
                    else -> "NONE"
                }
            }

            return "NONE"
        }

        /**
         * Emotion kısa kodunu decode eder
         * Örnek: E5 → ANGER, E1 → JOY, 0 → NONE
         */
        private fun decodeEmotionShort(shortCode: String): String {
            if (shortCode == "0") return "NONE"
            if (shortCode.uppercase() == "NONE") return "NONE"

            return when (shortCode.uppercase()) {
                "E1" -> "JOY"
                "E2" -> "CALM"
                "E3" -> "CONFUSION"
                "E4" -> "SADNESS"
                "E5" -> "ANGER"
                else -> "NONE"
            }
        }

        /**
         * Narrative Atmosphere kısa kodunu decode eder
         * Örnek: N11 → DARK_VENGEANCE, N5 → TRAGIC_REDEMPTION, 0 → NONE
         */
        private fun decodeNarrativeShort(shortCode: String): String {
            if (shortCode == "0") return "NONE"
            if (shortCode.uppercase() == "NONE") return "NONE"

            if (shortCode.startsWith("N", ignoreCase = true)) {
                val id = shortCode.substring(1).toIntOrNull() ?: return "NONE"
                return when (id) {
                    1 -> "TRIUMPHANT_JUDGMENT"
                    2 -> "EPIC_JUSTICE"
                    3 -> "ROMANTIC_TRANSFORMATION"
                    4 -> "MYSTICAL_ACCEPTANCE"
                    5 -> "TRAGIC_REDEMPTION"
                    6 -> "MELANCHOLIC_FORGIVENESS"
                    7 -> "PHILOSOPHICAL"
                    8 -> "COMEDIC"
                    9 -> "NOIR"
                    10 -> "HORROR_DENIAL"
                    11 -> "DARK_VENGEANCE"
                    else -> "NONE"
                }
            }

            return "NONE"
        }

        /**
         * Psychological Archetype kısa kodunu decode eder
         * Örnek: A11 → SHADOW, A1 → HERO, 0 → NONE
         */
        private fun decodeArchetypeShort(shortCode: String): String {
            if (shortCode == "0") return "NONE"
            if (shortCode.uppercase() == "NONE") return "NONE"

            if (shortCode.startsWith("A", ignoreCase = true)) {
                val id = shortCode.substring(1).toIntOrNull() ?: return "NONE"
                return when (id) {
                    1 -> "HERO"
                    2 -> "SAGE"
                    3 -> "CAREGIVER"
                    4 -> "LOVER"
                    5 -> "CREATOR"
                    6 -> "RULER"
                    7 -> "EXPLORER"
                    8 -> "INNOCENT"
                    9 -> "JESTER"
                    10 -> "REBEL"
                    11 -> "SHADOW"
                    12 -> "DESTROYER"
                    else -> "NONE"
                }
            }

            return "NONE"
        }

        /**
         * Depth kısa kodunu decode eder
         * Örnek: 1 → D1, 2 → D2, 5 → D5
         */
        private fun decodeDepthShort(shortCode: String): String {
            return when (shortCode) {
                "1" -> "D1"
                "2" -> "D2"
                "3" -> "D3"
                "4" -> "D4"
                "5" -> "D5"
                else -> "D1"
            }
        }
    }

    /**
     * Medya dosyası metadata yapısı
     */
    @Serializable
    data class MediaMetadata(
        val fileName: String,                    // Dosya adı (uzantısız)
        val fileType: String,                    // "VIDEO" veya "PHOTO"
        val resourceId: Int,                     // R.raw.xxx veya R.drawable.xxx
        val screenType: String,                  // FIRSTUSER, RETURNINGUSER, JOURNEY, UMBROS, vb.
        val primaryAttribute: String,            // VIOLENCE, MERCY, ORDER, CHAOS, vb.
        val secondaryAttribute: String = "NONE", // İkincil Nitelik (opsiyonel)
        val emotion: String,                     // JOY, SADNESS, ANGER, FEAR, CALM, vb.
        val narrativeAtmosphere: String = "NONE", // TRAGIC_REDEMPTION, DARK_VENGEANCE, vb.
        val psychologicalArchetype: String = "NONE", // HERO, SHADOW, SAGE, vb.
        val depth: String,                       // D1, D2, D3, D4, D5
        val sequenceNumber: String,              // 001, 002, 003, vb.
        val tags: List<String> = emptyList()     // Ek etiketler (gelecekte kullanım için)
    )

    /**
     * Veritabanı yapısı
     */
    @Serializable
    data class MediaDatabase(
        val version: String = "1.0",
        val generatedAt: Long = System.currentTimeMillis(),
        val totalVideos: Int = 0,
        val totalPhotos: Int = 0,
        val videos: List<MediaMetadata> = emptyList(),
        val photos: List<MediaMetadata> = emptyList()
    )

    /**
     * Ana build fonksiyonu - Veritabanını oluşturur ve kaydeder
     */
    fun buildAndSaveDatabase(): MediaDatabase {
        Log.d(TAG, "🎬 Medya veritabanı oluşturma başladı...")

        // 1. res/raw klasöründeki videoları tara
        val videos = scanRawResources()

        // 2. res/drawable klasöründeki fotoğrafları tara
        val photos = scanDrawableResources()

        // 3. Veritabanı nesnesini oluştur
        val database = MediaDatabase(
            version = "1.0",
            generatedAt = System.currentTimeMillis(),
            totalVideos = videos.size,
            totalPhotos = photos.size,
            videos = videos,
            photos = photos
        )

        // 4. JSON'a dönüştür ve kaydet
        saveToAssets(database)

        Log.d(TAG, "✅ Veritabanı oluşturuldu: ${videos.size} video, ${photos.size} fotoğraf")
        return database
    }

    /**
     * res/raw klasöründeki videoları tarar
     */
    private fun scanRawResources(): List<MediaMetadata> {
        val videos = mutableListOf<MediaMetadata>()
        val screenTypeCount = mutableMapOf<String, Int>()  // ⚡ Ekran türü sayacı

        // R.raw sınıfındaki tüm alanları reflection ile tara
        val rawClass = R.raw::class.java
        val fields = rawClass.declaredFields

        for (field in fields) {
            try {
                val fileName = field.name
                val resourceId = field.getInt(null)

                // Sadece video dosyalarını işle
                if (isVideoFile(fileName)) {
                    val metadata = parseFileName(fileName, "VIDEO", resourceId)
                    if (metadata != null) {
                        // ⚡ PERFORMANCE: Ekran başına limit kontrolü
                        val currentCount = screenTypeCount.getOrDefault(metadata.screenType, 0)
                        if (currentCount < MAX_VIDEOS_PER_SCREEN) {
                            videos.add(metadata)
                            screenTypeCount[metadata.screenType] = currentCount + 1
                            Log.d(TAG, "📹 Video bulundu: $fileName -> ${metadata.screenType}/${metadata.primaryAttribute}/${metadata.emotion}")
                        } else {
                            Log.d(TAG, "⏭️ Video atlandı (limit): $fileName -> ${metadata.screenType} (${currentCount}/${MAX_VIDEOS_PER_SCREEN})")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "❌ Dosya işlenirken hata: ${field.name}", e)
            }
        }

        return videos
    }

    /**
     * res/drawable klasöründeki fotoğrafları tarar
     */
    private fun scanDrawableResources(): List<MediaMetadata> {
        val photos = mutableListOf<MediaMetadata>()
        val screenTypeCount = mutableMapOf<String, Int>()  // ⚡ Ekran türü sayacı

        // R.drawable sınıfındaki tüm alanları reflection ile tara
        val drawableClass = R.drawable::class.java
        val fields = drawableClass.declaredFields

        for (field in fields) {
            try {
                val fileName = field.name
                val resourceId = field.getInt(null)

                // Sadece fotoğraf dosyalarını işle
                if (isImageFile(fileName)) {
                    val metadata = parseFileName(fileName, "PHOTO", resourceId)
                    if (metadata != null) {
                        // ⚡ PERFORMANCE: Ekran başına limit kontrolü
                        val currentCount = screenTypeCount.getOrDefault(metadata.screenType, 0)
                        if (currentCount < MAX_PHOTOS_PER_SCREEN) {
                            photos.add(metadata)
                            screenTypeCount[metadata.screenType] = currentCount + 1
                            Log.d(TAG, "📸 Fotoğraf bulundu: $fileName -> ${metadata.screenType}/${metadata.primaryAttribute}/${metadata.emotion}")
                        } else {
                            Log.d(TAG, "⏭️ Fotoğraf atlandı (limit): $fileName -> ${metadata.screenType} (${currentCount}/${MAX_PHOTOS_PER_SCREEN})")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "❌ Dosya işlenirken hata: ${field.name}", e)
            }
        }

        return photos
    }

    /**
     * Dosya adını parse eder ve metadata oluşturur
     *
     * 🆕 V3 FORMAT (10 parça): TÜR_EKRAN_PRIM_SEC_EMO_NAR_ARCH_DEPTH_SEQ
     * Örnek: v_f1_1n15_1p04_e5_n11_a11_2_001
     * Decode: V(VIDEO)_F1(FIRSTUSER)_1N15(VIOLENCE)_1P04(MERCY)_E5(ANGER)_N11(DARK_VENGEANCE)_A11(SHADOW)_2(D2)_001
     */
    private fun parseFileName(fileName: String, fileType: String, resourceId: Int): MediaMetadata? {
        try {
            // Dosya adını büyük harfe çevir ve parçalara ayır
            val parts = fileName.uppercase().split("_")

            // İlk parça "V" veya "P" (yeni) veya "VID"/"PHT" (eski) olmalı
            val typePrefix = parts[0]

            // 🆕 YENİ V3 FORMAT (9 parça): V_F1_1N15_0_E5_0_0_2_001
            // Format: [tür]_[ekran]_[prim]_[sec]_[emo]_[nar]_[arch]_[dep]_[seq]
            if (parts.size >= 9 && (typePrefix == "V" || typePrefix == "P")) {
                Log.d(TAG, "✅ V3 Format: $fileName")
                return MediaMetadata(
                    fileName = fileName,
                    fileType = decodeFileType(typePrefix),               // V → VIDEO, P → PHOTO
                    resourceId = resourceId,
                    screenType = decodeScreenType(parts[1]),             // F1 → FIRSTUSER
                    primaryAttribute = decodeAttributeShort(parts[2]),   // 1N15 → VIOLENCE
                    secondaryAttribute = decodeAttributeShort(parts[3]), // 1P04 → MERCY, 0 → NONE
                    emotion = decodeEmotionShort(parts[4]),              // E5 → ANGER, 0 → NONE
                    narrativeAtmosphere = decodeNarrativeShort(parts[5]), // N11 → DARK_VENGEANCE, 0 → NONE
                    psychologicalArchetype = decodeArchetypeShort(parts[6]), // A11 → SHADOW, 0 → NONE
                    depth = decodeDepthShort(parts[7]),                  // 2 → D2
                    sequenceNumber = parts[8],                           // 001
                    tags = buildTags(parts)
                )
            }

            // V2 FORMAT (9 parça): VID_FIRSTUSER_1N15_1P04_E5_N11_A11_D2_001
            else if (parts.size >= 9 && (typePrefix == "VID" || typePrefix == "PHT")) {
                Log.d(TAG, "⚠️ V2 Format: $fileName (geçiş önerilir)")
                return MediaMetadata(
                    fileName = fileName,
                    fileType = fileType,
                    resourceId = resourceId,
                    screenType = parts[1],                               // FIRSTUSER
                    primaryAttribute = decodeAttributeShort(parts[2]),   // 1N15 → VIOLENCE
                    secondaryAttribute = decodeAttributeShort(parts[3]), // 1P04 → MERCY
                    emotion = decodeEmotionShort(parts[4]),              // E5 → ANGER
                    narrativeAtmosphere = decodeNarrativeShort(parts[5]), // N11 → DARK_VENGEANCE
                    psychologicalArchetype = decodeArchetypeShort(parts[6]), // A11 → SHADOW
                    depth = parts[7],                                    // D2
                    sequenceNumber = parts[8],                           // 001
                    tags = buildTags(parts)
                )
            }

            // ESKİ V1 FORMAT (6 parça): VID_RETURNINGUSER_ORDER_JOY_D1_001
            else if (parts.size >= 6 && (typePrefix == "VID" || typePrefix == "PHT")) {
                Log.d(TAG, "⚠️ V1 Format (eski): $fileName")
                return MediaMetadata(
                    fileName = fileName,
                    fileType = fileType,
                    resourceId = resourceId,
                    screenType = parts[1],
                    primaryAttribute = parts[2],
                    emotion = parts[3],
                    depth = parts[4],
                    sequenceNumber = parts[5],
                    tags = buildTags(parts)
                )
            }

            // 🔄 LEGACY FORMAT (2-5 parça): vid_journey, vid_journey_1, pht_journey, etc.
            // Bu dosyalar basit placeholder medyalar - varsayılan değerlerle parse et
            else if (parts.size >= 2 && (typePrefix == "VID" || typePrefix == "PHT" || typePrefix == "V" || typePrefix == "P")) {
                Log.d(TAG, "🔄 Legacy Format (${parts.size} parça): $fileName → varsayılan değerlerle ekleniyor")

                // Ekran türünü parts[1]'den al, yoksa JOURNEY varsayılanı
                val screenType = if (parts.size > 1) {
                    parts[1].uppercase()
                } else {
                    "JOURNEY"
                }

                // Sequence number parts[2]'den al, yoksa "001"
                val sequenceNumber = if (parts.size > 2) {
                    parts[2]
                } else {
                    "001"
                }

                return MediaMetadata(
                    fileName = fileName,
                    fileType = fileType,
                    resourceId = resourceId,
                    screenType = screenType,              // journey, transition, etc.
                    primaryAttribute = "SURVIVAL",         // Nötr attribute (FIRSTUSER için uygun)
                    secondaryAttribute = "NONE",
                    emotion = "CALM",                      // Nötr emotion
                    narrativeAtmosphere = "NONE",
                    psychologicalArchetype = "NONE",
                    depth = "D1",                          // Derinlik 1 (FIRSTUSER için uygun)
                    sequenceNumber = sequenceNumber,
                    tags = listOf("legacy", "screen:${screenType.lowercase()}", "depth:d1")
                )
            }

            // Geçersiz format
            else {
                Log.d(TAG, "⏭️ Geçersiz format: $fileName (${parts.size} parça, prefix: $typePrefix)")
                return null
            }

        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Dosya adı parse edilemedi: $fileName", e)
            return null
        }
    }

    /**
     * Ek etiketler oluşturur (gelecekte genişletilebilir)
     */
    private fun buildTags(parts: List<String>): List<String> {
        val tags = mutableListOf<String>()

        // Ekran türüne göre etiket ekle
        if (parts.size > 1) {
            tags.add("screen:${parts[1].lowercase()}")
        }

        // Derinlik seviyesine göre etiket ekle
        if (parts.size > 4) {
            tags.add("depth:${parts[4].lowercase()}")
        }

        return tags
    }

    /**
     * Dosyanın video olup olmadığını kontrol eder
     */
    private fun isVideoFile(fileName: String): Boolean {
        // R.raw içindeki dosya isimleri uzantısız gelir, bu yüzden bilinen video isimleriyle kontrol ediyoruz
        // V3 Format: v_ ile başlayan dosyalar (v_f1_, v_f2_, v_u1_, vb.)
        // V1/V2 Format: vid_ ile başlayan dosyalar (vid_journey, vid_umbros, vb.)
        return fileName.startsWith("v_", ignoreCase = true) ||
               fileName.startsWith("vid_", ignoreCase = true) ||
               fileName.contains("video", ignoreCase = true)
    }

    /**
     * Dosyanın fotoğraf olup olmadığını kontrol eder
     */
    private fun isImageFile(fileName: String): Boolean {
        // R.drawable içindeki dosya isimleri uzantısız gelir
        // V3 Format: p_ ile başlayan dosyalar (p_f1_, p_f2_, p_u1_, vb.)
        // V1/V2 Format: pht_ ile başlayan dosyalar (pht_journey, vb.)
        return fileName.startsWith("p_", ignoreCase = true) ||
               fileName.startsWith("pht_", ignoreCase = true) ||
               fileName.startsWith("photo", ignoreCase = true)
    }

    /**
     * Veritabanını JSON olarak assets klasörüne kaydeder
     */
    private fun saveToAssets(database: MediaDatabase) {
        try {
            val json = Json {
                prettyPrint = true
                ignoreUnknownKeys = true
            }

            val jsonString = json.encodeToString(database)

            // assets klasörüne yazma (önce internal storage'a yaz)
            val file = File(context.filesDir, OUTPUT_FILENAME)
            file.writeText(jsonString)

            Log.d(TAG, "💾 Veritabanı kaydedildi: ${file.absolutePath}")
            Log.d(TAG, "📊 İstatistikler: ${database.totalVideos} video, ${database.totalPhotos} fotoğraf")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Veritabanı kaydedilirken hata!", e)
        }
    }

    /**
     * Kayıtlı veritabanını okur (eğer varsa)
     */
    fun loadDatabase(): MediaDatabase? {
        return try {
            val file = File(context.filesDir, OUTPUT_FILENAME)
            if (!file.exists()) {
                Log.w(TAG, "⚠️ Veritabanı dosyası bulunamadı: ${file.absolutePath}")
                return null
            }

            val jsonString = file.readText()
            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
            }

            val database = json.decodeFromString<MediaDatabase>(jsonString)
            Log.d(TAG, "✅ Veritabanı yüklendi: ${database.totalVideos} video, ${database.totalPhotos} fotoğraf")
            database

        } catch (e: Exception) {
            Log.e(TAG, "❌ Veritabanı yüklenirken hata!", e)
            null
        }
    }

    /**
     * Belirli bir ekran türü için medya filtreler
     */
    fun filterByScreenType(database: MediaDatabase, screenType: String): List<MediaMetadata> {
        val videos = database.videos.filter { it.screenType.equals(screenType, ignoreCase = true) }
        val photos = database.photos.filter { it.screenType.equals(screenType, ignoreCase = true) }
        return videos + photos
    }

    /**
     * Belirli bir duygu için medya filtreler
     */
    fun filterByEmotion(database: MediaDatabase, emotion: String): List<MediaMetadata> {
        val videos = database.videos.filter { it.emotion.equals(emotion, ignoreCase = true) }
        val photos = database.photos.filter { it.emotion.equals(emotion, ignoreCase = true) }
        return videos + photos
    }

    /**
     * Belirli bir derinlik seviyesi için medya filtreler
     */
    fun filterByDepth(database: MediaDatabase, depth: String): List<MediaMetadata> {
        val videos = database.videos.filter { it.depth.equals(depth, ignoreCase = true) }
        val photos = database.photos.filter { it.depth.equals(depth, ignoreCase = true) }
        return videos + photos
    }
}
