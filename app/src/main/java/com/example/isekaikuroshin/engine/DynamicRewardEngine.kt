package com.example.isekaikuroshin.engine

import android.util.Log
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.WeightRecord
import com.example.isekaikuroshin.di.mock.MockFirebaseAuth
import com.example.isekaikuroshin.di.mock.MockFirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * FAZ 5: Dynamic Reward Engine - AI-powered badge system
 *
 * Kullanıcının sağlık verilerindeki benzersiz desenleri analiz eder
 * ve dinamik olarak rozetler/başarılar oluşturur.
 *
 * Özellikler:
 * - Streak detection (ardışık günler)
 * - Personal records (kişisel rekorlar)
 * - Consistency patterns (düzenli davranışlar)
 * - Challenge completion (zorlu hedefler)
 * - Firebase Firestore persistence (cloud sync)
 */
@Singleton
class DynamicRewardEngine @Inject constructor(
    private val firestore: MockFirebaseFirestore,
    private val auth: MockFirebaseAuth
) {

    companion object {
        private const val TAG = "DynamicRewardEngine"

        // Rozet seviyeleri
        const val BADGE_BRONZE = "🥉 Bronze"
        const val BADGE_SILVER = "🥈 Silver"
        const val BADGE_GOLD = "🥇 Gold"
        const val BADGE_PLATINUM = "💎 Platinum"
        const val BADGE_LEGENDARY = "👑 Legendary"

        // Firebase collection names
        private const val COLLECTION_BADGES = "user_badges"
    }

    private val _earnedBadges = MutableStateFlow<List<DynamicBadge>>(emptyList())
    val earnedBadges: StateFlow<List<DynamicBadge>> = _earnedBadges.asStateFlow()

    private val _pendingBadge = MutableStateFlow<DynamicBadge?>(null)
    val pendingBadge: StateFlow<DynamicBadge?> = _pendingBadge.asStateFlow()

    /**
     * Sağlık verilerini analiz et ve yeni rozetleri tespit et
     */
    suspend fun analyzeHealthData(
        weightHistory: List<WeightRecord>,
        exerciseHistory: List<ExerciseSessionRecord>,
        nutritionHistory: List<NutritionRecord>
    ): List<DynamicBadge> {
        Log.d(TAG, "FAZ 5: Analyzing health data for badge opportunities...")
        val newBadges = mutableListOf<DynamicBadge>()

        // 1. Egzersiz streak analizi
        val exerciseStreak = calculateExerciseStreak(exerciseHistory)
        if (exerciseStreak >= 3 && !hasBadge("exercise_streak_$exerciseStreak")) {
            newBadges.add(
                DynamicBadge(
                    id = "exercise_streak_$exerciseStreak",
                    title = "🔥 $exerciseStreak Gün Streak!",
                    description = "$exerciseStreak gün üst üste egzersiz yaptın!",
                    icon = "🔥",
                    tier = getBadgeTier(exerciseStreak),
                    earnedAt = Instant.now(),
                    category = BadgeCategory.EXERCISE,
                    rarityScore = exerciseStreak * 10
                )
            )
        }

        // 2. Kilo hedefi analizi
        val weightGoalBadge = analyzeWeightGoal(weightHistory)
        if (weightGoalBadge != null && !hasBadge(weightGoalBadge.id)) {
            newBadges.add(weightGoalBadge)
        }

        // 3. Egzersiz çeşitliliği analizi
        val varietyBadge = analyzeExerciseVariety(exerciseHistory)
        if (varietyBadge != null && !hasBadge(varietyBadge.id)) {
            newBadges.add(varietyBadge)
        }

        // 4. Beslenme dengesi analizi
        val nutritionBadge = analyzeNutritionBalance(nutritionHistory)
        if (nutritionBadge != null && !hasBadge(nutritionBadge.id)) {
            newBadges.add(nutritionBadge)
        }

        // 5. Sabah egzersizi erken kuş rozeti
        val earlyBirdBadge = analyzeEarlyMorningWorkouts(exerciseHistory)
        if (earlyBirdBadge != null && !hasBadge(earlyBirdBadge.id)) {
            newBadges.add(earlyBirdBadge)
        }

        Log.d(TAG, "FAZ 5: Found ${newBadges.size} new badges!")
        return newBadges
    }

    /**
     * Egzersiz streak hesapla (ardışık günler)
     */
    private fun calculateExerciseStreak(exerciseHistory: List<ExerciseSessionRecord>): Int {
        if (exerciseHistory.isEmpty()) return 0

        val uniqueDays = exerciseHistory
            .map { it.startTime.atZone(ZoneId.systemDefault()).toLocalDate() }
            .distinct()
            .sortedDescending()

        var streak = 0
        var currentDate = LocalDate.now()

        for (date in uniqueDays) {
            if (date == currentDate || date == currentDate.minusDays(1)) {
                streak++
                currentDate = date.minusDays(1)
            } else {
                break
            }
        }

        return streak
    }

    /**
     * Kilo hedefi analizi - Tutarlı kilo kaybı/alımı
     */
    private fun analyzeWeightGoal(weightHistory: List<WeightRecord>): DynamicBadge? {
        if (weightHistory.size < 7) return null

        val sortedWeights = weightHistory.sortedBy { it.time }
        val startWeight = sortedWeights.first().weight.inKilograms
        val currentWeight = sortedWeights.last().weight.inKilograms
        val weightChange = startWeight - currentWeight

        return when {
            weightChange >= 5.0 -> DynamicBadge(
                id = "weight_loss_5kg",
                title = "💪 5 Kilo Kaybı!",
                description = "5 kilo kaybettin! Harika iş!",
                icon = "💪",
                tier = BADGE_GOLD,
                earnedAt = Instant.now(),
                category = BadgeCategory.WEIGHT,
                rarityScore = 80
            )
            weightChange >= 2.0 -> DynamicBadge(
                id = "weight_loss_2kg",
                title = "🎯 2 Kilo Kaybı",
                description = "2 kilo kaybettin! Devam et!",
                icon = "🎯",
                tier = BADGE_SILVER,
                earnedAt = Instant.now(),
                category = BadgeCategory.WEIGHT,
                rarityScore = 50
            )
            abs(weightChange) <= 0.5 && weightHistory.size >= 14 -> DynamicBadge(
                id = "weight_stability",
                title = "⚖️ Denge Ustası",
                description = "2 hafta boyunca ideal kilonu korudun!",
                icon = "⚖️",
                tier = BADGE_BRONZE,
                earnedAt = Instant.now(),
                category = BadgeCategory.WEIGHT,
                rarityScore = 40
            )
            else -> null
        }
    }

    /**
     * Egzersiz çeşitliliği analizi - Farklı aktivite türleri
     */
    private fun analyzeExerciseVariety(exerciseHistory: List<ExerciseSessionRecord>): DynamicBadge? {
        if (exerciseHistory.size < 5) return null

        val uniqueTypes = exerciseHistory.map { it.exerciseType }.distinct().size

        return when {
            uniqueTypes >= 5 -> DynamicBadge(
                id = "exercise_variety_5",
                title = "🌈 Çeşitlilik Şampiyonu",
                description = "$uniqueTypes farklı egzersiz türü denedin!",
                icon = "🌈",
                tier = BADGE_GOLD,
                earnedAt = Instant.now(),
                category = BadgeCategory.EXERCISE,
                rarityScore = 90
            )
            uniqueTypes >= 3 -> DynamicBadge(
                id = "exercise_variety_3",
                title = "🎨 Çeşitlilik Arayıcısı",
                description = "$uniqueTypes farklı egzersiz türü denedin!",
                icon = "🎨",
                tier = BADGE_SILVER,
                earnedAt = Instant.now(),
                category = BadgeCategory.EXERCISE,
                rarityScore = 60
            )
            else -> null
        }
    }

    /**
     * Beslenme dengesi analizi - Düzenli 3 öğün
     */
    private fun analyzeNutritionBalance(nutritionHistory: List<NutritionRecord>): DynamicBadge? {
        if (nutritionHistory.size < 9) return null // En az 3 gün x 3 öğün

        val daysWithThreeMeals = nutritionHistory
            .groupBy { it.startTime.atZone(ZoneId.systemDefault()).toLocalDate() }
            .count { (_, meals) -> meals.size >= 3 }

        return when {
            daysWithThreeMeals >= 7 -> DynamicBadge(
                id = "nutrition_balance_7days",
                title = "🥗 Beslenme Uzmanı",
                description = "7 gün boyunca düzenli beslendim!",
                icon = "🥗",
                tier = BADGE_GOLD,
                earnedAt = Instant.now(),
                category = BadgeCategory.NUTRITION,
                rarityScore = 85
            )
            daysWithThreeMeals >= 3 -> DynamicBadge(
                id = "nutrition_balance_3days",
                title = "🍎 Düzenli Beslenme",
                description = "3 gün boyunca düzenli beslendim!",
                icon = "🍎",
                tier = BADGE_BRONZE,
                earnedAt = Instant.now(),
                category = BadgeCategory.NUTRITION,
                rarityScore = 40
            )
            else -> null
        }
    }

    /**
     * Erken sabah egzersizi analizi (07:00 öncesi)
     */
    private fun analyzeEarlyMorningWorkouts(exerciseHistory: List<ExerciseSessionRecord>): DynamicBadge? {
        val earlyWorkouts = exerciseHistory.filter {
            val hour = it.startTime.atZone(ZoneId.systemDefault()).hour
            hour < 7
        }

        return when {
            earlyWorkouts.size >= 5 -> DynamicBadge(
                id = "early_bird_5",
                title = "🌅 Erken Kuş",
                description = "5 kez sabah 7'den önce antrenmana çıktın!",
                icon = "🌅",
                tier = BADGE_GOLD,
                earnedAt = Instant.now(),
                category = BadgeCategory.LIFESTYLE,
                rarityScore = 95
            )
            earlyWorkouts.size >= 3 -> DynamicBadge(
                id = "early_bird_3",
                title = "🐦 Sabahçı",
                description = "3 kez sabah 7'den önce antrenmana çıktın!",
                icon = "🐦",
                tier = BADGE_SILVER,
                earnedAt = Instant.now(),
                category = BadgeCategory.LIFESTYLE,
                rarityScore = 70
            )
            else -> null
        }
    }

    /**
     * Rozet seviyesi belirle (streak uzunluğuna göre)
     */
    private fun getBadgeTier(streakDays: Int): String {
        return when {
            streakDays >= 30 -> BADGE_LEGENDARY
            streakDays >= 21 -> BADGE_PLATINUM
            streakDays >= 14 -> BADGE_GOLD
            streakDays >= 7 -> BADGE_SILVER
            else -> BADGE_BRONZE
        }
    }

    /**
     * Rozet zaten kazanılmış mı kontrol et
     */
    private fun hasBadge(badgeId: String): Boolean {
        return _earnedBadges.value.any { it.id == badgeId }
    }

    /**
     * Yeni rozet kazanıldığında çağrılır
     */
    fun awardBadge(badge: DynamicBadge) {
        _earnedBadges.value = _earnedBadges.value + badge
        _pendingBadge.value = badge
        Log.d(TAG, "FAZ 5: Badge awarded: ${badge.title}")
    }

    /**
     * Pending badge'i temizle
     */
    fun clearPendingBadge() {
        _pendingBadge.value = null
    }

    /**
     * Tüm rozetleri temizle (test için)
     */
    fun clearAllBadges() {
        _earnedBadges.value = emptyList()
        _pendingBadge.value = null
    }

    // ========= FIREBASE PERSISTENCE =========

    /**
     * Rozeti Firebase'e kaydet
     */
    suspend fun saveBadgeToFirebase(badge: DynamicBadge): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Log.w(TAG, "FAZ 5: User not authenticated, skipping Firebase save")
                return Result.success(Unit) // Offline mode: başarılı say
            }

            val badgeData = hashMapOf(
                "id" to badge.id,
                "title" to badge.title,
                "description" to badge.description,
                "icon" to badge.icon,
                "tier" to badge.tier,
                "earnedAt" to badge.earnedAt.toEpochMilli(),
                "category" to badge.category.name,
                "rarityScore" to badge.rarityScore
            )

            firestore.collection(COLLECTION_BADGES)
                .document(userId)
                .collection("badges")
                .document(badge.id)
                .set(badgeData)
                .await()

            Log.d(TAG, "FAZ 5: Badge saved to Firebase: ${badge.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "FAZ 5: Failed to save badge to Firebase", e)
            Result.failure(e)
        }
    }

    /**
     * Firebase'den rozetleri yükle
     */
    suspend fun loadBadgesFromFirebase(): Result<List<DynamicBadge>> {
        return try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Log.w(TAG, "FAZ 5: User not authenticated, returning empty list")
                return Result.success(emptyList())
            }

            val snapshot = firestore.collection(COLLECTION_BADGES)
                .document(userId)
                .collection("badges")
                .get()
                .await()

            val badges = snapshot.documents.mapNotNull { doc ->
                try {
                    DynamicBadge(
                        id = doc.getString("id") ?: return@mapNotNull null,
                        title = doc.getString("title") ?: return@mapNotNull null,
                        description = doc.getString("description") ?: "",
                        icon = doc.getString("icon") ?: "🏆",
                        tier = doc.getString("tier") ?: BADGE_BRONZE,
                        earnedAt = Instant.ofEpochMilli(doc.getLong("earnedAt") ?: 0L),
                        category = BadgeCategory.valueOf(doc.getString("category") ?: "SPECIAL"),
                        rarityScore = doc.getLong("rarityScore")?.toInt() ?: 0
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "FAZ 5: Failed to parse badge from Firebase", e)
                    null
                }
            }

            _earnedBadges.value = badges
            Log.d(TAG, "FAZ 5: Loaded ${badges.size} badges from Firebase")
            Result.success(badges)
        } catch (e: Exception) {
            Log.e(TAG, "FAZ 5: Failed to load badges from Firebase", e)
            Result.failure(e)
        }
    }

    /**
     * Firebase'de kullanıcı oturum açınca rozetleri yükle
     */
    suspend fun syncBadgesWithFirebase() {
        loadBadgesFromFirebase()
    }
}

/**
 * Dinamik rozet veri modeli
 */
data class DynamicBadge(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val tier: String,
    val earnedAt: Instant,
    val category: BadgeCategory,
    val rarityScore: Int // 0-100, daha yüksek = daha nadir
)

/**
 * Rozet kategorileri
 */
enum class BadgeCategory {
    EXERCISE,
    WEIGHT,
    NUTRITION,
    LIFESTYLE,
    SPECIAL
}
