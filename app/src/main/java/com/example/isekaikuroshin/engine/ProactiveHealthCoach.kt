package com.example.isekaikuroshin.engine

import android.util.Log
import com.example.isekaikuroshin.ui.healthhub.HealthConnectManager
import com.example.isekaikuroshin.utils.GameLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TODO-HUB-08: Proaktif Sağlık Koçu Motoru
 * TODO-HUB-11: Gelişmiş Çok Boyutlu Analiz (Faz 2)
 *
 * SYS-20 raporunda tasarlanan ProactiveHealthCoach motorunun gelişmiş implementasyonu.
 * Bu motor, kullanıcının haftalık aktivite, kilo ve beslenme verilerini birlikte analiz ederek
 * daha derin ve anlamlı sağlık içgörüleri üretir.
 *
 * Faz 2 Yetenekleri:
 * - Çok boyutlu veri analizi (Aktivite + Kilo + Beslenme)
 * - Veriler arası korelasyon tespiti
 * - Gelişmiş desen tanıma
 */
@Singleton
class ProactiveHealthCoach @Inject constructor(
    private val healthConnectManager: HealthConnectManager
) {

    companion object {
        private const val TAG = "ProactiveHealthCoach"

        // Analiz eşikleri - Aktivite
        private const val MIN_WEEKLY_ACTIVITY_MINUTES = 60L // Haftalık minimum aktivite süresi
        private const val OPTIMAL_WEEKLY_ACTIVITY_MINUTES = 150L // WHO önerisi: 150 dakika/hafta
        private const val HIGH_ACTIVITY_THRESHOLD = 300L // Yüksek aktivite eşiği

        // TODO-HUB-11: Analiz eşikleri - Beslenme
        private const val MIN_NUTRITION_ENTRIES_PER_WEEK = 10 // Haftalık minimum besin kaydı sayısı

        // TODO-HUB-11: Analiz eşikleri - Kilo
        private const val SIGNIFICANT_WEIGHT_CHANGE_KG = 0.5 // Önemli kilo değişimi eşiği (kg)
    }

    /**
     * Son 7 günlük aktivite verilerini analiz eder ve sağlık içgörüsü üretir
     */
    suspend fun analyzeWeeklyActivity(): HealthInsight? {
        return withContext(Dispatchers.IO) {
            try {
                GameLogger.logSystem("TODO-HUB-08: Haftalık aktivite analizi başlatılıyor...")

                // Son 7 günlük egzersiz verilerini al
                val exerciseHistory = healthConnectManager.readExerciseHistory(7)

                GameLogger.logSystem("TODO-HUB-08: ${exerciseHistory.size} adet egzersiz kaydı analiz ediliyor")

                // Toplam egzersiz süresini hesapla
                val totalActivityMinutes = exerciseHistory.sumOf { exercise ->
                    val duration = Duration.between(exercise.startTime, exercise.endTime)
                    duration.toMinutes()
                }

                GameLogger.logSystem("TODO-HUB-08: Toplam haftalık aktivite: $totalActivityMinutes dakika")

                // Aktivite seviyesine göre analiz yap
                return@withContext when {
                    totalActivityMinutes < MIN_WEEKLY_ACTIVITY_MINUTES -> {
                        createLowActivityInsight(totalActivityMinutes, exerciseHistory.size)
                    }
                    totalActivityMinutes >= OPTIMAL_WEEKLY_ACTIVITY_MINUTES -> {
                        createHighActivityInsight(totalActivityMinutes, exerciseHistory.size)
                    }
                    else -> {
                        createModerateActivityInsight(totalActivityMinutes, exerciseHistory.size)
                    }
                }

            } catch (e: Exception) {
                GameLogger.logError(TAG, "TODO-HUB-08: Haftalık aktivite analizi başarısız", e)
                Log.e(TAG, "Aktivite analizi hatası", e)
                null
            }
        }
    }

    /**
     * Düşük aktivite seviyesi için içgörü oluşturur
     */
    private fun createLowActivityInsight(totalMinutes: Long, exerciseCount: Int): HealthInsight {
        val title = "⚠️ Düşük Aktivite Haftası"

        val description = if (exerciseCount == 0) {
            "Bu hafta hiç egzersiz yapmadınız. Sağlığınız için günde en az 10-15 dakika yürüyüş yapabilirsiniz."
        } else {
            "Bu hafta sadece $totalMinutes dakika aktivite yaptınız ($exerciseCount aktivite). " +
            "Sağlıklı bir yaşam için haftada en az 150 dakika orta yoğunlukta aktivite önerilir."
        }

        val recommendedAction = "Bugün 15 dakikalık bir yürüyüşle başlayabilirsiniz. " +
            "Merdiven çıkmak, ev işleri yapmak da sayılır!"

        GameLogger.logSystem("TODO-HUB-08: ✅ Düşük aktivite içgörüsü oluşturuldu")

        return HealthInsight(
            title = title,
            description = description,
            severity = HealthInsightSeverity.MEDIUM,
            category = HealthInsightCategory.ACTIVITY,
            recommendedAction = recommendedAction
        )
    }

    /**
     * Yüksek aktivite seviyesi için içgörü oluşturur
     */
    private fun createHighActivityInsight(totalMinutes: Long, exerciseCount: Int): HealthInsight {
        val title = "🎉 Harika Bir Aktivite Haftası!"

        val description = "Tebrikler! Bu hafta $totalMinutes dakika aktivite yaparak ($exerciseCount aktivite) " +
            "sağlıklı yaşam hedeflerinizi fazlasıyla aştınız. Bu motivasyonu koruyun!"

        val recommendedAction = "Mükemmel gidiyorsunuz! Dinlenme günlerini de ihmal etmeyin ve " +
            "aktivitelerinizi çeşitlendirmeyi unutmayın."

        GameLogger.logSystem("TODO-HUB-08: ✅ Yüksek aktivite içgörüsü oluşturuldu")

        return HealthInsight(
            title = title,
            description = description,
            severity = HealthInsightSeverity.LOW,
            category = HealthInsightCategory.ACTIVITY,
            recommendedAction = recommendedAction
        )
    }

    /**
     * Orta seviye aktivite için içgörü oluşturur
     */
    private fun createModerateActivityInsight(totalMinutes: Long, exerciseCount: Int): HealthInsight {
        val remainingMinutes = OPTIMAL_WEEKLY_ACTIVITY_MINUTES - totalMinutes

        val title = "👍 İyi Bir Başlangıç"

        val description = "Bu hafta $totalMinutes dakika aktivite yaptınız ($exerciseCount aktivite). " +
            "Haftalık 150 dakika hedefinize ulaşmak için $remainingMinutes dakika daha aktivite yapabilirsiniz."

        val recommendedAction = "Hedefinize çok yakınsınız! Bu hafta içinde birkaç kısa yürüyüş " +
            "veya merdiven çıkma ile hedefinizi tamamlayabilirsiniz."

        GameLogger.logSystem("TODO-HUB-08: ✅ Orta seviye aktivite içgörüsü oluşturuldu")

        return HealthInsight(
            title = title,
            description = description,
            severity = HealthInsightSeverity.LOW,
            category = HealthInsightCategory.ACTIVITY,
            recommendedAction = recommendedAction
        )
    }

    /**
     * TODO-HUB-11: Gelişmiş haftalık desen analizi
     * Aktivite, kilo ve beslenme verilerini birlikte analiz ederek daha derin içgörüler üretir
     */
    suspend fun analyzeWeeklyPatterns(): HealthInsight? {
        return withContext(Dispatchers.IO) {
            try {
                GameLogger.logSystem("TODO-HUB-11: Çok boyutlu haftalık analiz başlatılıyor...")

                // 1. Tüm veri kaynaklarını topla
                val exerciseHistory = healthConnectManager.readExerciseHistory(7)
                val weightHistory = healthConnectManager.readWeightHistory(7)
                val nutritionHistory = healthConnectManager.readNutritionHistory(
                    java.time.Instant.now().minus(java.time.Duration.ofDays(7)),
                    java.time.Instant.now()
                )

                GameLogger.logSystem("TODO-HUB-11: Veri toplandı - Egzersiz: ${exerciseHistory.size}, Kilo: ${weightHistory.size}, Beslenme: ${nutritionHistory.size}")

                // 2. Toplam aktivite süresini hesapla
                val totalActivityMinutes = exerciseHistory.sumOf { exercise ->
                    val duration = Duration.between(exercise.startTime, exercise.endTime)
                    duration.toMinutes()
                }

                // 3. Öncelikli analiz: Besin kaydı tutarlılığını kontrol et (Kural 1)
                val nutritionInsight = analyzeNutritionConsistency(nutritionHistory.size)
                if (nutritionInsight != null) {
                    return@withContext nutritionInsight
                }

                // 4. Yüksek öncelikli analiz: Aktivite/Kilo korelasyonu (Kural 2)
                val correlationInsight = analyzeActivityWeightCorrelation(
                    totalActivityMinutes,
                    exerciseHistory.size,
                    weightHistory
                )
                if (correlationInsight != null) {
                    return@withContext correlationInsight
                }

                // 5. Fallback: Standart aktivite analizi
                GameLogger.logSystem("TODO-HUB-11: Özel desen bulunamadı, standart aktivite analizi yapılıyor")
                return@withContext when {
                    totalActivityMinutes < MIN_WEEKLY_ACTIVITY_MINUTES -> {
                        createLowActivityInsight(totalActivityMinutes, exerciseHistory.size)
                    }
                    totalActivityMinutes >= OPTIMAL_WEEKLY_ACTIVITY_MINUTES -> {
                        createHighActivityInsight(totalActivityMinutes, exerciseHistory.size)
                    }
                    else -> {
                        createModerateActivityInsight(totalActivityMinutes, exerciseHistory.size)
                    }
                }

            } catch (e: Exception) {
                GameLogger.logError(TAG, "TODO-HUB-11: Çok boyutlu analiz başarısız", e)
                Log.e(TAG, "Haftalık desen analizi hatası", e)
                null
            }
        }
    }

    /**
     * TODO-HUB-11: Kural 1 - Besin kaydı tutarlılığı analizi
     * Kullanıcının beslenme günlüğü tutma disiplinini değerlendirir
     */
    private fun analyzeNutritionConsistency(nutritionEntryCount: Int): HealthInsight? {
        return if (nutritionEntryCount < MIN_NUTRITION_ENTRIES_PER_WEEK) {
            GameLogger.logSystem("TODO-HUB-11: ⚠️ Düşük besin kaydı tespit edildi ($nutritionEntryCount < $MIN_NUTRITION_ENTRIES_PER_WEEK)")

            HealthInsight(
                title = "📝 Besin Günlüğün Biraz Boş Kalmış",
                description = "Bu hafta beslenme günlüğünüze sadece $nutritionEntryCount kayıt yapmışsınız. " +
                    "Düzenli kayıt, beslenme desenlerinizi daha iyi görmemizi ve size daha iyi yardımcı " +
                    "olmamızı sağlar. Küçük atıştırmaları da kaydetmeyi unutmayın!",
                severity = HealthInsightSeverity.MEDIUM,
                category = HealthInsightCategory.NUTRITION,
                recommendedAction = "Hedef: Her gün en az 2-3 öğününüzü kaydedin. " +
                    "Hatırlatıcı kurmak işinizi kolaylaştırabilir."
            )
        } else {
            GameLogger.logSystem("TODO-HUB-11: ✅ Besin kaydı tutarlılığı yeterli ($nutritionEntryCount)")
            null
        }
    }

    /**
     * TODO-HUB-11: Kural 2 - Aktivite/Kilo korelasyon analizi
     * Yüksek aktiviteye rağmen beklenmedik kilo artışlarını tespit eder
     */
    private fun analyzeActivityWeightCorrelation(
        totalActivityMinutes: Long,
        exerciseCount: Int,
        weightHistory: List<androidx.health.connect.client.records.WeightRecord>
    ): HealthInsight? {
        // Yüksek aktivite kontrolü
        if (totalActivityMinutes < HIGH_ACTIVITY_THRESHOLD) {
            GameLogger.logSystem("TODO-HUB-11: Aktivite yeterince yüksek değil ($totalActivityMinutes < $HIGH_ACTIVITY_THRESHOLD)")
            return null
        }

        // En az 2 kilo kaydı olmalı (karşılaştırma için)
        if (weightHistory.size < 2) {
            GameLogger.logSystem("TODO-HUB-11: Yeterli kilo kaydı yok (${weightHistory.size} < 2)")
            return null
        }

        // Son ve ilk kilo kayıtlarını al
        val sortedWeights = weightHistory.sortedBy { it.time }
        val firstWeight = sortedWeights.first().weight.inKilograms
        val lastWeight = sortedWeights.last().weight.inKilograms
        val weightChange = lastWeight - firstWeight

        GameLogger.logSystem("TODO-HUB-11: Kilo değişimi: ${String.format("%.2f", weightChange)} kg (İlk: $firstWeight, Son: $lastWeight)")

        // Yüksek aktiviteye rağmen kilo artışı var mı?
        return if (weightChange > SIGNIFICANT_WEIGHT_CHANGE_KG) {
            GameLogger.logSystem("TODO-HUB-11: 🔍 Beklenmedik desen: Yüksek aktivite + Kilo artışı tespit edildi!")

            HealthInsight(
                title = "🏃 Harika Aktivite, Beklenmedik Sonuç",
                description = "Bu hafta tam $totalActivityMinutes dakika aktivite yaparak ($exerciseCount aktivite) " +
                    "muhteşem bir performans sergileydiniz! Ancak kilonuzda ${String.format("%.1f", weightChange)} kg " +
                    "artış gözlemleniyor. Bu durum kaslanma, su tutma veya beslenme dengesi ile ilgili olabilir.",
                severity = HealthInsightSeverity.MEDIUM,
                category = HealthInsightCategory.GENERAL_HEALTH,
                recommendedAction = "Beslenme kayıtlarınızı gözden geçirin, özellikle porsiyon boyutlarına " +
                    "ve öğün zamanlamalarına dikkat edin. Gerekirse bir beslenme uzmanına danışabilirsiniz. " +
                    "Aktiviteniz harika, sadece beslenme dengesi üzerinde çalışın!"
            )
        } else {
            GameLogger.logSystem("TODO-HUB-11: ✅ Aktivite/Kilo dengesi normal (Değişim: ${String.format("%.2f", weightChange)} kg)")
            null
        }
    }

    /**
     * Gelişmiş aktivite analizi (gelecek versiyonlar için)
     */
    suspend fun analyzeActivityPatterns(): List<HealthInsight> {
        // TODO: Gelecek versiyonlarda implementasyonu yapılacak
        // - Haftalık aktivite trendleri
        // - Aktivite türü çeşitliliği analizi
        // - En aktif günler analizi
        // - Düzenlilik skorlaması

        return emptyList()
    }

    /**
     * Kilo ve aktivite korelasyon analizi (gelecek versiyonlar için)
     * @deprecated TODO-HUB-11'de analyzeWeeklyPatterns() içine entegre edildi
     */
    @Deprecated("Use analyzeWeeklyPatterns() instead", ReplaceWith("analyzeWeeklyPatterns()"))
    suspend fun analyzeWeightActivityCorrelation(): HealthInsight? {
        // TODO-HUB-11: Bu fonksiyon analyzeWeeklyPatterns() içine entegre edildi
        return null
    }
}