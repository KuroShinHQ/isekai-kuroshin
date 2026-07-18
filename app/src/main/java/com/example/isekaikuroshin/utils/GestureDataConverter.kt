package com.example.isekaikuroshin.utils

import android.util.Log
import com.example.isekaikuroshin.data.NormalizedPoint
import com.example.isekaikuroshin.data.Seal
import com.example.isekaikuroshin.data.SealDifficulty
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

/**
 * ⚡ CRITICAL UTILITY: JSON ↔ Seal Converter
 *
 * SpellTrigger.gestureData (JSON String) ile GestureRecognitionEngine (Seal nesnesi)
 * arasındaki köprüdür. Bu converter olmadan hareket tanıma sistemi çalışmaz.
 *
 * KULLANIM:
 * ```kotlin
 * val gestureDataJson = trigger.gestureData  // JSON String
 * val seal = GestureDataConverter.jsonToSeal(gestureDataJson)
 * val result = recognitionEngine.evaluateGesture(liveHand, seal)
 * ```
 */
object GestureDataConverter {

    private const val TAG = "GestureDataConverter"

    /**
     * JSON formatter (lenient mode for backward compatibility)
     */
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
    }

    /**
     * ⚡ PRIMARY FUNCTION: JSON String → Seal Object
     *
     * SpellTrigger.gestureData'yı (JSON String) alıp, GestureRecognitionEngine'in
     * kullanabileceği bir Seal nesnesine dönüştürür.
     *
     * @param gestureDataJson JSON formatında kaydedilmiş hareket verisi
     * @param gestureName Hareketin adı (Seal.name için)
     * @param acceptanceThreshold Kabul eşiği (0.75 = %75 doğruluk gerekli)
     * @return Seal nesnesi veya null (parse hatası durumunda)
     */
    fun jsonToSeal(
        gestureDataJson: String,
        gestureName: String = "Spell Gesture",
        acceptanceThreshold: Float = 0.75f
    ): Seal? {
        return try {
            Log.d(TAG, "🔄 Converting JSON to Seal: $gestureName")

            // JSON'u GestureData nesnesine parse et
            val gestureData = json.decodeFromString<GestureData>(gestureDataJson)

            // GestureData'yı Seal'e dönüştür
            val seal = Seal(
                id = "spell_gesture_${System.currentTimeMillis()}",
                nameKey = "spell_trigger_gesture",
                descriptionKey = "spell_trigger_gesture_desc",
                loreTextKey = "spell_trigger_gesture_lore",
                difficulty = SealDifficulty.INTERMEDIATE,
                tier = "B",
                templateLandmarksList = mutableListOf(gestureData.landmarks),
                acceptanceThreshold = acceptanceThreshold,
                toleranceThreshold = 0.15f,
                prerequisiteSealId = null,
                minPlayerLevel = 1,
                relatedSkillIds = emptyList(),
                masteryLevel = 0,
                iconPath = null,
                visualGuideAnimation = null
            )

            Log.d(TAG, "✅ Conversion successful: ${seal.templateLandmarksList.size} landmarks")
            seal

        } catch (e: SerializationException) {
            Log.e(TAG, "❌ JSON parse error: ${e.message}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Unexpected error during conversion: ${e.message}", e)
            null
        }
    }

    /**
     * ⚡ REVERSE FUNCTION: Seal Object → JSON String
     *
     * Bir Seal nesnesinin ilk template'ini JSON String'e dönüştürür.
     * SpellTrigger.gestureData'ya kaydetmek için kullanılır.
     *
     * @param seal Seal nesnesi
     * @return JSON String veya null (hata durumunda)
     */
    fun sealToJson(seal: Seal): String? {
        return try {
            if (seal.templateLandmarksList.isEmpty()) {
                Log.w(TAG, "⚠️ Seal has no templates: ${seal.nameKey}")
                return null
            }

            Log.d(TAG, "🔄 Converting Seal to JSON: ${seal.nameKey}")

            // İlk template'i al
            val firstTemplate = seal.templateLandmarksList[0]

            // GestureData nesnesine çevir
            val gestureData = GestureData(
                landmarks = firstTemplate,
                timestamp = System.currentTimeMillis()
            )

            // JSON String'e serialize et
            val jsonString = json.encodeToString(GestureData.serializer(), gestureData)

            Log.d(TAG, "✅ Conversion successful: ${jsonString.length} characters")
            jsonString

        } catch (e: SerializationException) {
            Log.e(TAG, "❌ JSON serialization error: ${e.message}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Unexpected error during conversion: ${e.message}", e)
            null
        }
    }

    /**
     * BATCH CONVERSION: Multiple JSON Strings → Single Seal with Multiple Templates
     *
     * Çift el hareketleri veya multi-angle kalibrasyonları için kullanılır.
     * Her bir JSON String ayrı bir template olarak eklenir.
     *
     * @param gestureDataJsonList JSON String listesi
     * @param gestureName Hareketin adı
     * @param acceptanceThreshold Kabul eşiği
     * @return Seal nesnesi veya null
     */
    fun multiJsonToSeal(
        gestureDataJsonList: List<String>,
        gestureName: String = "Multi-Angle Gesture",
        acceptanceThreshold: Float = 0.75f
    ): Seal? {
        return try {
            if (gestureDataJsonList.isEmpty()) {
                Log.w(TAG, "⚠️ Empty gesture data list")
                return null
            }

            Log.d(TAG, "🔄 Converting ${gestureDataJsonList.size} JSON templates to Seal")

            // Tüm JSON'ları parse et
            val allLandmarks = gestureDataJsonList.mapNotNull { jsonString ->
                try {
                    val gestureData = json.decodeFromString<GestureData>(jsonString)
                    gestureData.landmarks
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Skipping invalid JSON: ${e.message}")
                    null
                }
            }

            if (allLandmarks.isEmpty()) {
                Log.e(TAG, "❌ No valid JSON strings found")
                return null
            }

            // Multi-template Seal oluştur
            val seal = Seal(
                id = "spell_multi_gesture_${System.currentTimeMillis()}",
                nameKey = "spell_multi_angle_gesture",
                descriptionKey = "spell_multi_angle_gesture_desc",
                loreTextKey = "spell_multi_angle_gesture_lore",
                difficulty = SealDifficulty.ADVANCED,
                tier = "A",
                templateLandmarksList = allLandmarks.toMutableList(),
                acceptanceThreshold = acceptanceThreshold,
                toleranceThreshold = 0.15f,
                prerequisiteSealId = null,
                minPlayerLevel = 1,
                relatedSkillIds = emptyList(),
                masteryLevel = 0,
                iconPath = null,
                visualGuideAnimation = null
            )

            Log.d(TAG, "✅ Multi-template conversion successful: ${seal.templateLandmarksList.size} templates")
            seal

        } catch (e: Exception) {
            Log.e(TAG, "❌ Multi-conversion error: ${e.message}", e)
            null
        }
    }
}

/**
 * Internal Data Class: GestureData
 *
 * JSON serialize/deserialize için kullanılan intermediate format.
 * SpellTrigger.gestureData içinde bu formatta saklanır.
 *
 * G67 FIX: landmarks default value eklendi (JSON'da eksik olursa crash olmaması için)
 */
@Serializable
private data class GestureData(
    val landmarks: List<NormalizedPoint> = emptyList(),  // G67: Default value eklendi
    val timestamp: Long = System.currentTimeMillis()
)
