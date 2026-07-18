package com.example.isekaikuroshin.engine

import com.example.isekaikuroshin.data.combat.LearnedSpell
import com.example.isekaikuroshin.data.combat.TriggerType
import com.example.isekaikuroshin.data.Seal
import kotlin.math.min

/**
 * GÖREV N - FAZ 5: Büyü Performans Analiz Motoru
 *
 * Kullan1c1n1n büyü kullan1m1 s1ras1nda performans1n1 analiz eder:
 * - Voice trigger ’ Ses benzerlii (Levenshtein distance)
 * - Gesture trigger ’ Hareket doruluu (GestureRecognitionEngine)
 * - Timer trigger ’ Otomatik ba_ar1l1
 *
 * Performans skoru (0.0 - 1.0) döndürür.
 */
object SpellPerformanceAnalyzer {

    /**
     * Performans Analiz Sonucu
     */
    data class PerformanceResult(
        val overallScore: Float,        // Genel performans (0.0 - 1.0)
        val stepResults: Map<String, StepPerformance>,  // Her ad1m için detay
        val passingGrade: Boolean        // Geçer not ald1 m1? (>= 0.7)
    )

    /**
     * Tek bir ad1m1n performans detay1
     */
    data class StepPerformance(
        val stepId: String,
        val triggerType: TriggerType,
        val score: Float,               // Bu ad1m için skor (0.0 - 1.0)
        val feedback: String             // Kullan1c1ya geri bildirim mesaj1
    )

    /**
     * Büyü performans1n1 analiz et
     *
     * @param spell Kullan1lan büyü
     * @param capturedData Kullan1c1n1n yakalad11 veri (ses, hareket, vs.)
     * @return Performans sonucu
     */
    fun analyzePerformance(
        spell: LearnedSpell,
        capturedData: Map<String, Any>  // stepId -> (voiceText veya Seal)
    ): PerformanceResult {
        val stepResults = mutableMapOf<String, StepPerformance>()

        // Her ad1m için performans1 hesapla
        spell.personalTriggers.forEach { (stepId, calibration) ->
            val capturedValue = capturedData[stepId]

            val stepScore = when (calibration.triggerType) {
                TriggerType.VOICE -> {
                    val voiceText = capturedValue as? String ?: ""
                    analyzeVoiceTrigger(voiceText, calibration.calibrationData)
                }

                TriggerType.SINGLE_HAND_GESTURE,
                TriggerType.DOUBLE_HAND_GESTURE -> {
                    val capturedSeal = capturedValue as? Seal
                    analyzeGestureTrigger(capturedSeal, calibration.accuracy)
                }

                TriggerType.TIMER -> {
                    1.0f  // Timer her zaman ba_ar1l1
                }
            }

            val feedback = generateFeedback(stepScore, calibration.triggerType)
            stepResults[stepId] = StepPerformance(stepId, calibration.triggerType, stepScore, feedback)
        }

        // Genel skoru hesapla (ortalama)
        val overallScore = if (stepResults.isNotEmpty()) {
            stepResults.values.map { it.score }.average().toFloat()
        } else {
            0.0f
        }

        val passingGrade = overallScore >= 0.7f

        return PerformanceResult(
            overallScore = overallScore,
            stepResults = stepResults,
            passingGrade = passingGrade
        )
    }

    /**
     * Ses trigger performans1n1 analiz et (Levenshtein distance)
     */
    private fun analyzeVoiceTrigger(
        spokenText: String,
        expectedTextJson: String  // Calibration data (basit string olarak sakl1)
    ): Float {
        // JSON'dan beklenen metni ç1kar (_imdilik basit string varsay1yoruz)
        val expectedText = expectedTextJson.trim('"')

        if (expectedText.isEmpty()) return 0.5f  // Kalibrasyon yap1lmam1_

        val distance = levenshteinDistance(spokenText.lowercase(), expectedText.lowercase())
        val maxLength = maxOf(spokenText.length, expectedText.length)

        return if (maxLength == 0) {
            1.0f
        } else {
            val similarity = 1.0f - (distance.toFloat() / maxLength)
            similarity.coerceIn(0.0f, 1.0f)
        }
    }

    /**
     * Hareket trigger performans1n1 analiz et
     */
    private fun analyzeGestureTrigger(
        capturedSeal: Seal?,
        calibratedAccuracy: Float
    ): Float {
        // Eer seal capture edilmemi_se
        if (capturedSeal == null) return 0.0f

        // GestureRecognitionEngine'den gelen accuracy'yi kullan
        // ^imdilik calibratedAccuracy'yi baz al1yoruz
        return calibratedAccuracy.coerceIn(0.0f, 1.0f)
    }

    /**
     * Kullan1c1ya geri bildirim mesaj1 olu_tur
     */
    private fun generateFeedback(score: Float, triggerType: TriggerType): String {
        return when {
            score >= 0.95f -> when (triggerType) {
                TriggerType.VOICE -> "Mükemmel telaffuz! <¯"
                TriggerType.SINGLE_HAND_GESTURE, TriggerType.DOUBLE_HAND_GESTURE -> "Kusursuz hareket! P"
                TriggerType.TIMER -> "Otomatik tetikleyici çal1_t1 "
            }
            score >= 0.85f -> when (triggerType) {
                TriggerType.VOICE -> "Harika! Çok net söyledin =M"
                TriggerType.SINGLE_HAND_GESTURE, TriggerType.DOUBLE_HAND_GESTURE -> "Çok iyi! Hareket doru =M"
                TriggerType.TIMER -> "Otomatik tetikleyici çal1_t1 "
            }
            score >= 0.70f -> when (triggerType) {
                TriggerType.VOICE -> "0yi! Kabul edilebilir "
                TriggerType.SINGLE_HAND_GESTURE, TriggerType.DOUBLE_HAND_GESTURE -> "Kabul edilebilir hareket "
                TriggerType.TIMER -> "Otomatik tetikleyici çal1_t1 "
            }
            score >= 0.50f -> when (triggerType) {
                TriggerType.VOICE -> "Orta seviye. Biraz daha pratik yap =ª"
                TriggerType.SINGLE_HAND_GESTURE, TriggerType.DOUBLE_HAND_GESTURE -> "Hareket belirsiz. Pratik yap =ª"
                TriggerType.TIMER -> "Otomatik tetikleyici çal1_t1 "
            }
            else -> when (triggerType) {
                TriggerType.VOICE -> "Telaffuz zay1f. Tekrar dene  "
                TriggerType.SINGLE_HAND_GESTURE, TriggerType.DOUBLE_HAND_GESTURE -> "Hareket hatal1. Tekrar dene  "
                TriggerType.TIMER -> "Otomatik tetikleyici çal1_t1 "
            }
        }
    }

    /**
     * Levenshtein Distance (String benzerlii)
     * 0ki string aras1ndaki minimum düzenleme mesafesi
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length

        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[len1][len2]
    }

    /**
     * Performans skorunu yüzdeye çevir
     */
    fun scoreToPercentage(score: Float): Int {
        return (score * 100).toInt().coerceIn(0, 100)
    }

    /**
     * Performans skoruna göre rank hesapla
     */
    fun getRank(score: Float): String {
        return when {
            score >= 0.95f -> "S (Efsanevi)"
            score >= 0.85f -> "A (Mükemmel)"
            score >= 0.70f -> "B (0yi)"
            score >= 0.50f -> "C (Orta)"
            else -> "D (Zay1f)"
        }
    }
}
