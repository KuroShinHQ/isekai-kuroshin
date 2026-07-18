package com.example.isekaikuroshin.engine.mind

import android.util.Log
import com.example.isekaikuroshin.data.PersistentDataManager
import com.example.isekaikuroshin.game.GameStateManager
import com.example.isekaikuroshin.utils.GameLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * TODO-HUB-14: Dil Öğrenimi İlerleme Takip Motoru
 *
 * Bu motor, kullanıcının dil öğrenimindeki başarısını ölçer ve
 * bu başarıyı oyun içi karakter stat'larına yansıtır.
 *
 * Temel görevler:
 * 1. Kullanıcı cevaplarının doğruluğunu ölçmek
 * 2. CEFR benzeri seviye sistemi üzerinden ilerleme hesaplamak
 * 3. İlerlemeyi karakter "Zeka" stat'ına bonus olarak yansıtmak
 */
@Singleton
class LanguageProgressTracker @Inject constructor(
    private val persistentDataManager: PersistentDataManager,
    private val gameStateManager: GameStateManager
) {
    companion object {
        private const val TAG = "LanguageProgressTracker"

        // CEFR Seviyeleri ve gereken ilerleme puanları
        private val CEFR_LEVELS = listOf("A1", "A2", "B1", "B2", "C1", "C2")
        private const val PROGRESS_TO_LEVEL_UP = 100f // Her seviyeye geçmek için gereken puan

        // Stat bonus hesaplama
        private const val STAT_BONUS_PER_LEVEL = 2 // Her tam seviye başına +2 zeka
    }

    /**
     * TODO-HUB-14: Dil öğrenimi ilerleme verisi
     */
    @kotlinx.serialization.Serializable
    data class LanguageProgress(
        val language: String,
        val cefrLevel: String, // "A1", "A2", "B1", "B2", "C1", "C2"
        val progressPercent: Float, // 0-100 arası, seviye içindeki ilerleme
        val totalCorrectAnswers: Int,
        val totalQuestions: Int,
        val lastUpdated: Long = System.currentTimeMillis()
    ) {
        /**
         * Genel başarı oranı (doğruluk yüzdesi)
         */
        val accuracy: Float
            get() = if (totalQuestions > 0) {
                (totalCorrectAnswers.toFloat() / totalQuestions.toFloat()) * 100f
            } else 0f

        /**
         * Stat bonus hesaplama
         */
        fun calculateStatBonus(): Int {
            val levelIndex = CEFR_LEVELS.indexOf(cefrLevel)
            return (levelIndex + 1) * STAT_BONUS_PER_LEVEL
        }
    }

    /**
     * TODO-HUB-14: Kullanıcının cevabını değerlendir ve ilerlemeyi güncelle
     *
     * @param language Öğrenilen dil
     * @param expectedResponse AI'ın beklediği ideal cevap
     * @param actualResponse Kullanıcının verdiği cevap
     * @return Güncellenen ilerleme verisi ve doğruluk skoru
     */
    suspend fun evaluateAndUpdateProgress(
        language: String,
        expectedResponse: String?,
        actualResponse: String
    ): Result<Pair<LanguageProgress, Float>> {
        return withContext(Dispatchers.IO) {
            try {
                GameLogger.logSystem("TODO-HUB-14: Cevap değerlendiriliyor...")
                GameLogger.logSystem("TODO-HUB-14: Beklenen: $expectedResponse")
                GameLogger.logSystem("TODO-HUB-14: Gerçek: $actualResponse")

                // 1. Doğruluk hesapla
                val accuracy = if (expectedResponse != null) {
                    calculateAccuracy(expectedResponse, actualResponse)
                } else {
                    // Eğer AI'dan expectedResponse gelmemişse, varsayılan orta değer
                    0.7f
                }

                GameLogger.logSystem("TODO-HUB-14: Doğruluk Skoru: ${(accuracy * 100).toInt()}%")

                // 2. Mevcut ilerlemeyi yükle veya yeni oluştur
                var progress = loadProgress(language) ?: createInitialProgress(language)

                // 3. İlerlemeyi güncelle
                progress = updateProgress(progress, accuracy)

                // 4. Veritabanına kaydet
                saveProgress(progress)

                GameLogger.logSystem("TODO-HUB-14: ✅ İlerleme güncellendi")
                GameLogger.logSystem("TODO-HUB-14: Seviye: ${progress.cefrLevel} - ${progress.progressPercent.toInt()}%")
                GameLogger.logSystem("TODO-HUB-14: Toplam Doğru: ${progress.totalCorrectAnswers}/${progress.totalQuestions}")

                // 5. Stat bonusunu uygula
                applyStatBonus(progress)

                Result.success(progress to accuracy)

            } catch (e: Exception) {
                GameLogger.logError(TAG, "TODO-HUB-14: İlerleme güncellenirken hata", e)
                Log.e(TAG, "Evaluate and update progress error", e)
                Result.failure(e)
            }
        }
    }

    /**
     * TODO-HUB-14: İki metin arasındaki benzerlik skorunu hesaplar
     *
     * Levenshtein Distance algoritmasını kullanarak iki string arasındaki
     * benzerliği 0.0 (tamamen farklı) ile 1.0 (tamamen aynı) arasında bir
     * değer olarak döndürür.
     *
     * @param expected Beklenen metin
     * @param actual Kullanıcının yazdığı metin
     * @return Benzerlik skoru (0.0 - 1.0)
     */
    private fun calculateAccuracy(expected: String, actual: String): Float {
        val expectedNormalized = expected.trim().lowercase()
        val actualNormalized = actual.trim().lowercase()

        // Tam eşleşme kontrolü
        if (expectedNormalized == actualNormalized) {
            return 1.0f
        }

        // Levenshtein Distance hesaplama
        val distance = levenshteinDistance(expectedNormalized, actualNormalized)
        val maxLength = max(expectedNormalized.length, actualNormalized.length)

        // Distance'ı similarity'ye çevir (0.0 - 1.0)
        val similarity = if (maxLength > 0) {
            1.0f - (distance.toFloat() / maxLength.toFloat())
        } else {
            0.0f
        }

        // Negatif değerleri engelle
        return max(0.0f, similarity)
    }

    /**
     * TODO-HUB-14: Levenshtein Distance algoritması
     *
     * İki string arasındaki minimum düzenleme mesafesini hesaplar
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length

        // DP matrisi
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        // İlk satır ve sütunu doldur
        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        // DP hesaplama
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // Silme
                    dp[i][j - 1] + 1,      // Ekleme
                    dp[i - 1][j - 1] + cost // Değiştirme
                )
            }
        }

        return dp[len1][len2]
    }

    /**
     * TODO-HUB-14: Mevcut ilerleme verisini yükle
     */
    private suspend fun loadProgress(language: String): LanguageProgress? {
        return withContext(Dispatchers.IO) {
            try {
                val key = "language_progress_${language.lowercase()}"
                persistentDataManager.loadData(key, LanguageProgress.serializer())
            } catch (e: Exception) {
                Log.w(TAG, "Progress loading failed, will create new", e)
                null
            }
        }
    }

    /**
     * TODO-HUB-14: İlk kez öğrenmeye başlayan kullanıcı için başlangıç verisi
     */
    private fun createInitialProgress(language: String): LanguageProgress {
        GameLogger.logSystem("TODO-HUB-14: Yeni dil öğrenimi kaydı oluşturuluyor: $language")
        return LanguageProgress(
            language = language,
            cefrLevel = "A1",
            progressPercent = 0f,
            totalCorrectAnswers = 0,
            totalQuestions = 0
        )
    }

    /**
     * TODO-HUB-14: İlerleme verisini güncelle
     *
     * @param current Mevcut ilerleme
     * @param accuracy Son cevabın doğruluk skoru (0.0 - 1.0)
     * @return Güncellenmiş ilerleme
     */
    private fun updateProgress(current: LanguageProgress, accuracy: Float): LanguageProgress {
        val isCorrect = accuracy >= 0.6f // %60'ın üzeri doğru sayılır

        // Toplam istatistikleri güncelle
        val newTotalQuestions = current.totalQuestions + 1
        val newTotalCorrect = if (isCorrect) current.totalCorrectAnswers + 1 else current.totalCorrectAnswers

        // İlerleme puanı kazanımı (doğruluk oranına göre)
        val progressGain = when {
            accuracy >= 0.9f -> 15f  // Mükemmel cevap
            accuracy >= 0.7f -> 10f  // İyi cevap
            accuracy >= 0.5f -> 5f   // Orta cevap
            else -> 2f               // Zayıf cevap (yine de bir şey öğrenildi)
        }

        var newProgress = current.progressPercent + progressGain
        var newLevel = current.cefrLevel

        // Seviye atlama kontrolü
        if (newProgress >= PROGRESS_TO_LEVEL_UP) {
            val currentLevelIndex = CEFR_LEVELS.indexOf(current.cefrLevel)
            if (currentLevelIndex < CEFR_LEVELS.size - 1) {
                newLevel = CEFR_LEVELS[currentLevelIndex + 1]
                newProgress = 0f // Yeni seviyede sıfırdan başla

                GameLogger.logSystem("TODO-HUB-14: 🎉 SEVİYE ATLANDI! Yeni seviye: $newLevel")
            } else {
                // Maksimum seviyeye ulaşıldı
                newProgress = min(newProgress, PROGRESS_TO_LEVEL_UP)
            }
        }

        return current.copy(
            cefrLevel = newLevel,
            progressPercent = newProgress,
            totalCorrectAnswers = newTotalCorrect,
            totalQuestions = newTotalQuestions,
            lastUpdated = System.currentTimeMillis()
        )
    }

    /**
     * TODO-HUB-14: İlerleme verisini kaydet
     */
    private suspend fun saveProgress(progress: LanguageProgress) {
        withContext(Dispatchers.IO) {
            try {
                val key = "language_progress_${progress.language.lowercase()}"
                persistentDataManager.saveData(key, progress, LanguageProgress.serializer())
                GameLogger.logSystem("TODO-HUB-14: İlerleme kaydedildi")
            } catch (e: Exception) {
                Log.e(TAG, "Progress saving failed", e)
                GameLogger.logError(TAG, "TODO-HUB-14: İlerleme kaydedilemedi", e)
            }
        }
    }

    /**
     * TODO-HUB-14: Dil öğrenimi başarısını karakter stat'ına uygula
     *
     * Her seviye için belirli bir zeka bonusu verir
     */
    private fun applyStatBonus(progress: LanguageProgress) {
        try {
            GameLogger.logSystem("TODO-HUB-14: Stat bonusu uygulanıyor...")

            val bonus = progress.calculateStatBonus()

            gameStateManager.applyLanguageLearningBonus(progress)

            GameLogger.logSystem("TODO-HUB-14: ✅ Stat bonusu uygulandı: +$bonus Zeka")

        } catch (e: Exception) {
            Log.e(TAG, "Apply stat bonus failed", e)
            GameLogger.logError(TAG, "TODO-HUB-14: Stat bonusu uygulanamadı", e)
        }
    }

    /**
     * TODO-HUB-14: Kullanıcının mevcut dil öğrenim istatistiklerini getir
     */
    suspend fun getProgressStats(language: String): LanguageProgress? {
        return loadProgress(language)
    }
}
