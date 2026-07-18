package com.example.isekaikuroshin.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * 💾 AI Response Cache - AI cevaplarını önbellekte saklar
 *
 * Özellikler:
 * - Günlük özetlerini cache'ler
 * - Çevirileri cache'ler
 * - Tarih bilgisi ile saklar
 * - Eski cache'leri otomatik temizler (30 gün)
 */
class AIResponseCache(private val context: Context) {

    private val gson = Gson()
    private val cacheDir = File(context.cacheDir, "ai_responses")

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    /**
     * Günlük özetini cache'le
     *
     * @param date Tarih (örn: "2025-10-19")
     * @param summary AI tarafından oluşturulan özet
     */
    fun saveJournalSummary(date: String, summary: String) {
        val cached = CachedAIResponse(
            type = "journal_summary",
            key = date,
            response = summary,
            timestamp = System.currentTimeMillis()
        )
        saveCachedResponse(cached)
    }

    /**
     * Cache'lenmiş günlük özetini getir
     *
     * @param date Tarih
     * @return CachedAIResponse veya null
     */
    fun getJournalSummary(date: String): CachedAIResponse? {
        return getCachedResponse("journal_summary", date)
    }

    /**
     * Çeviriyi cache'le
     *
     * @param sourceText Kaynak metin
     * @param translation Çeviri
     * @param sourceLang Kaynak dil
     * @param targetLang Hedef dil
     */
    fun saveTranslation(
        sourceText: String,
        translation: String,
        sourceLang: String,
        targetLang: String
    ) {
        val key = "${sourceLang}_${targetLang}_${sourceText.hashCode()}"
        val cached = CachedAIResponse(
            type = "translation",
            key = key,
            response = translation,
            timestamp = System.currentTimeMillis()
        )
        saveCachedResponse(cached)
    }

    /**
     * Cache'lenmiş çeviriyi getir
     */
    fun getTranslation(
        sourceText: String,
        sourceLang: String,
        targetLang: String
    ): CachedAIResponse? {
        val key = "${sourceLang}_${targetLang}_${sourceText.hashCode()}"
        return getCachedResponse("translation", key)
    }

    /**
     * 30 günden eski cache'leri temizle
     */
    fun clearOldCache() {
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)

        cacheDir.listFiles()?.forEach { file ->
            try {
                val json = file.readText()
                val cached = gson.fromJson(json, CachedAIResponse::class.java)

                if (cached.timestamp < thirtyDaysAgo) {
                    file.delete()
                    android.util.Log.d("AIResponseCache", "🗑️ Deleted old cache: ${file.name}")
                }
            } catch (e: Exception) {
                android.util.Log.e("AIResponseCache", "Error cleaning cache: ${e.message}")
            }
        }
    }

    /**
     * Tüm cache'i temizle
     */
    fun clearAllCache() {
        cacheDir.listFiles()?.forEach { it.delete() }
        android.util.Log.d("AIResponseCache", "🗑️ All cache cleared")
    }

    // Private helpers
    private fun saveCachedResponse(cached: CachedAIResponse) {
        val fileName = "${cached.type}_${cached.key}.json"
        val file = File(cacheDir, fileName)

        try {
            file.writeText(gson.toJson(cached))
            android.util.Log.d("AIResponseCache", "💾 Cached: ${cached.type} - ${cached.key}")
        } catch (e: Exception) {
            android.util.Log.e("AIResponseCache", "Error saving cache: ${e.message}")
        }
    }

    private fun getCachedResponse(type: String, key: String): CachedAIResponse? {
        val fileName = "${type}_${key}.json"
        val file = File(cacheDir, fileName)

        if (!file.exists()) return null

        return try {
            val json = file.readText()
            gson.fromJson(json, CachedAIResponse::class.java)
        } catch (e: Exception) {
            android.util.Log.e("AIResponseCache", "Error reading cache: ${e.message}")
            null
        }
    }
}

/**
 * Cache'lenmiş AI cevabı data modeli
 */
data class CachedAIResponse(
    val type: String,           // "journal_summary", "translation"
    val key: String,            // Unique identifier (örn: tarih, hash)
    val response: String,       // AI'dan gelen cevap
    val timestamp: Long         // Ne zaman cache'lendi (ms)
) {
    /**
     * Cache tarihi - Kullanıcıya gösterilecek format
     * Örn: "18 Ekim 2025"
     */
    fun getFormattedDate(): String {
        val dateFormat = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale("tr"))
        return dateFormat.format(java.util.Date(timestamp))
    }

    /**
     * Cache yaşı - Gün cinsinden
     */
    fun getAgeInDays(): Int {
        val diff = System.currentTimeMillis() - timestamp
        return (diff / (24 * 60 * 60 * 1000)).toInt()
    }

    /**
     * Cache hala geçerli mi? (7 günden eski değilse)
     */
    fun isStillValid(): Boolean {
        return getAgeInDays() <= 7
    }
}
