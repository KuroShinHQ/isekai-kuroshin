package com.example.isekaikuroshin.engine

import android.util.Log
import kotlin.math.max
import kotlin.math.min

/**
 * GÖREV E: Voice Command Matcher
 *
 * Fuzzy matching ile sesli komutları eşleştirir.
 * Levenshtein distance algoritması kullanır.
 */
object VoiceCommandMatcher {

    /**
     * İki metni hassasiyete göre karşılaştırır
     *
     * @param target Hedef metin (büyüde kaydedilmiş komut)
     * @param recognized Tanınan metin (SpeechRecognizer'dan gelen)
     * @param sensitivity 0.0 (çok esnek) - 1.0 (çok hassas)
     * @return Eşleşme başarılı mı?
     */
    fun matches(target: String, recognized: String, sensitivity: Float): Boolean {
        val similarity = calculateSimilarity(target, recognized)
        val threshold = sensitivity

        val isMatch = similarity >= threshold

        Log.d("VoiceCommandMatcher", "🎯 Target: '$target'")
        Log.d("VoiceCommandMatcher", "🗣️ Recognized: '$recognized'")
        Log.d("VoiceCommandMatcher", "📊 Similarity: ${(similarity * 100).toInt()}%")
        Log.d("VoiceCommandMatcher", "🎚️ Threshold: ${(threshold * 100).toInt()}%")
        Log.d("VoiceCommandMatcher", if (isMatch) "✅ MATCH!" else "❌ NO MATCH")

        return isMatch
    }

    /**
     * İki metin arasındaki benzerlik oranını hesaplar
     *
     * @return 0.0 (tamamen farklı) - 1.0 (tamamen aynı)
     */
    private fun calculateSimilarity(s1: String, s2: String): Float {
        // Normalizasyon (küçük harf, Türkçe karakterler)
        val str1 = normalizeText(s1)
        val str2 = normalizeText(s2)

        // Tam eşleşme kontrolü
        if (str1 == str2) return 1.0f

        // Levenshtein distance hesapla
        val distance = levenshteinDistance(str1, str2)
        val maxLength = max(str1.length, str2.length)

        // Benzerlik oranı = 1 - (distance / maxLength)
        return if (maxLength > 0) {
            1.0f - (distance.toFloat() / maxLength)
        } else {
            0.0f
        }
    }

    /**
     * Metni normalize eder (küçük harf, Türkçe karakter dönüşümü)
     */
    private fun normalizeText(text: String): String {
        return text
            .lowercase()
            .trim()
            // Türkçe karakter normalizasyonu (opsiyonel)
            .replace("ı", "i")
            .replace("ğ", "g")
            .replace("ü", "u")
            .replace("ş", "s")
            .replace("ö", "o")
            .replace("ç", "c")
            // Noktalama işaretlerini kaldır
            .replace(Regex("[.,!?;:]"), "")
    }

    /**
     * Levenshtein Distance algoritması
     * İki string arasındaki minimum düzenleme mesafesini hesaplar
     *
     * @return Minimum değişiklik sayısı
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length

        // DP tablosu
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        // İlk satır ve sütunu doldur
        for (i in 0..len1) {
            dp[i][0] = i
        }
        for (j in 0..len2) {
            dp[0][j] = j
        }

        // DP tablosunu doldur
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1

                dp[i][j] = min(
                    min(
                        dp[i - 1][j] + 1,      // Silme
                        dp[i][j - 1] + 1       // Ekleme
                    ),
                    dp[i - 1][j - 1] + cost    // Değiştirme
                )
            }
        }

        return dp[len1][len2]
    }

    /**
     * Detaylı eşleşme analizi döndürür (debug için)
     */
    fun analyzeMatch(target: String, recognized: String, sensitivity: Float): MatchAnalysis {
        val similarity = calculateSimilarity(target, recognized)
        val isMatch = similarity >= sensitivity

        return MatchAnalysis(
            target = target,
            recognized = recognized,
            similarity = similarity,
            threshold = sensitivity,
            isMatch = isMatch,
            normalizedTarget = normalizeText(target),
            normalizedRecognized = normalizeText(recognized)
        )
    }

    /**
     * Eşleşme analiz sonucu
     */
    data class MatchAnalysis(
        val target: String,
        val recognized: String,
        val similarity: Float,
        val threshold: Float,
        val isMatch: Boolean,
        val normalizedTarget: String,
        val normalizedRecognized: String
    ) {
        fun toLogString(): String {
            return """
                |Voice Command Match Analysis:
                |  Target: '$target' → '$normalizedTarget'
                |  Recognized: '$recognized' → '$normalizedRecognized'
                |  Similarity: ${(similarity * 100).toInt()}%
                |  Threshold: ${(threshold * 100).toInt()}%
                |  Result: ${if (isMatch) "✅ MATCH" else "❌ NO MATCH"}
            """.trimMargin()
        }
    }
}
