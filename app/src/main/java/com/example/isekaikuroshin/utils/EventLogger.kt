package com.example.isekaikuroshin.utils

import android.util.Log
import com.example.isekaikuroshin.ui.journal.JournalViewModel

/**
 * G83: MEGA CRITICAL - Event Logger Singleton
 *
 * 48 ekranın Journal/AI sistemine entegrasyonunu sağlayan merkezi olay kaydedici.
 *
 * PROBLEM:
 * - PhysicalTrainingScreen, ManaCultivationScreen, SpellStudioScreen vb. ekranlar
 *   aktiviteleri tamamlıyor ama JournalViewModel'e bildirim göndermiyor
 * - AI tetiklenmiyor → GMResponse dönmüyor → Skill/XP kazanılmıyor
 *
 * ÇÖZÜM:
 * - EventLogger, JournalViewModel referansını tutar (MainActivity'de init edilir)
 * - Her ekran, aktivite tamamlandığında EventLogger.logXXX() çağırır
 * - EventLogger → JournalViewModel.sendPrompt() → AI tetiklenir → GMResponse uygulanır
 *
 * KULLANIM:
 * ```kotlin
 * // MainActivity onCreate:
 * EventLogger.init(journalViewModel)
 *
 * // PhysicalTrainingScreen:
 * EventLogger.logTraining("Push-ups", 30, "high")
 *
 * // ManaCultivationScreen:
 * EventLogger.logCultivation("Mana Breathing", breakthroughLevel = 5)
 * ```
 *
 * ROOT CAUSE FIX:
 * Bu singleton olmadan, 48 ekran Journal'a entegre edilemez.
 * Çünkü her ekranın ViewModel'ine JournalViewModel inject etmek, mimariyi bozar
 * ve circular dependency yaratır.
 */
object EventLogger {

    private const val TAG = "EventLogger"
    private var journalViewModel: JournalViewModel? = null
    private var isInitialized = false

    /**
     * EventLogger'ı başlat (MainActivity onCreate'de çağrılmalı)
     *
     * @param viewModel JournalViewModel instance
     */
    fun init(viewModel: JournalViewModel) {
        if (isInitialized) {
            Log.w(TAG, "⚠️ EventLogger already initialized, skipping re-init")
            return
        }

        journalViewModel = viewModel
        isInitialized = true
        Log.d(TAG, "✅ EventLogger initialized successfully")
    }

    /**
     * Cleanup (ViewModel onCleared'da çağrılabilir)
     */
    fun cleanup() {
        journalViewModel = null
        isInitialized = false
        Log.d(TAG, "🧹 EventLogger cleaned up")
    }

    // ESKİ logTraining ve logCultivation fonksiyonları SİLİNDİ
    // Yeni detaylı versiyonlar satır 275 ve 305'te bulunuyor

    /**
     * Büyü oluşturma aktivitesi kaydet
     *
     * @param spellName Büyü adı
     * @param elements Element listesi (Fire, Water, Earth vb.)
     */
    fun logSpellCreation(spellName: String, elements: List<String>) {
        if (!checkInitialized()) return

        val elementsStr = elements.joinToString(", ")
        val prompt = "I created a new spell called '$spellName' using $elementsStr elements. This complex magical work should increase my intelligence and grant me experience."

        journalViewModel?.processUserActions(prompt)
        GameLogger.logSystem("📝 EventLogger: Spell creation logged - $prompt")
    }

    /**
     * El işçiliği/crafting aktivitesi kaydet
     *
     * @param itemName Oluşturulan item adı
     * @param rarity Nadirlik (Common, Rare, Epic vb.)
     * @param craftingType Crafting türü (Blacksmithing, Alchemy vb.)
     */
    fun logCrafting(itemName: String, rarity: String, craftingType: String) {
        if (!checkInitialized()) return

        val prompt = "I crafted a $rarity quality '$itemName' through $craftingType. This skilled work should grant me experience."

        journalViewModel?.processUserActions(prompt)
        GameLogger.logSystem("📝 EventLogger: Crafting logged - $prompt")
    }

    /**
     * Egzersiz aktivitesi kaydet (HealthHub)
     *
     * @param exerciseType Egzersiz türü (Cardio, Strength, Flexibility vb.)
     * @param reps Tekrar sayısı
     * @param calories Yakılan kalori (tahmini)
     */
    fun logExercise(exerciseType: String, reps: Int, calories: Int) {
        if (!checkInitialized()) return

        val prompt = "I completed $reps reps of $exerciseType exercise, burning approximately $calories calories"

        journalViewModel?.processUserActions(prompt)
        GameLogger.logSystem("📝 EventLogger: Exercise logged - $prompt")
    }

    /**
     * G83 P4: Kilo kaydı aktivitesi kaydet (HealthHub)
     *
     * @param weightInKg Kilogram cinsinden kilo
     */
    fun logWeight(weightInKg: Double) {
        if (!checkInitialized()) return

        val prompt = "I logged my weight: $weightInKg kg. This health tracking activity shows my commitment to self-improvement."

        journalViewModel?.processUserActions(prompt)
        GameLogger.logSystem("📝 EventLogger: Weight logged - $prompt")
    }

    /**
     * G83 P4: Beslenme kaydı aktivitesi kaydet (HealthHub)
     *
     * @param mealName Yemek adı
     * @param mealType Öğün türü (Breakfast, Lunch, Dinner, Snack)
     */
    fun logNutrition(mealName: String, mealType: String) {
        if (!checkInitialized()) return

        val prompt = "I logged my $mealType meal: $mealName. Tracking my nutrition helps me maintain a healthy lifestyle."

        journalViewModel?.processUserActions(prompt)
        GameLogger.logSystem("📝 EventLogger: Nutrition logged - $prompt")
    }

    /**
     * G83 P4: Egzersiz oturumu kaydet (HealthHub - Exercise Session)
     *
     * @param exerciseType Egzersiz türü kodu
     * @param durationMinutes Süre (dakika)
     */
    fun logExerciseSession(exerciseType: Int, durationMinutes: Long) {
        if (!checkInitialized()) return

        val prompt = "I completed an exercise session (type: $exerciseType) lasting $durationMinutes minutes. This physical training should improve my strength and vitality."

        journalViewModel?.processUserActions(prompt)
        GameLogger.logSystem("📝 EventLogger: Exercise session logged - $prompt")
    }

    /**
     * Savaş aktivitesi kaydet
     *
     * @param enemyType Düşman türü
     * @param outcome Sonuç (Victory, Defeat, Fled)
     * @param experienceGained Kazanılan XP (null ise AI hesaplar)
     */
    fun logCombat(enemyType: String, outcome: String, experienceGained: Int? = null) {
        if (!checkInitialized()) return

        val prompt = if (experienceGained != null) {
            "I fought against a $enemyType. Outcome: $outcome. Gained $experienceGained XP"
        } else {
            "I fought against a $enemyType. Outcome: $outcome"
        }

        journalViewModel?.processUserActions(prompt)
        GameLogger.logSystem("📝 EventLogger: Combat logged - $prompt")
    }

    /**
     * Quest aktivitesi kaydet
     *
     * @param questName Quest adı
     * @param action Aksiyon (Started, Completed, Failed, Abandoned)
     * @param progress İlerleme yüzdesi (0-100)
     */
    fun logQuest(questName: String, action: String, progress: Int? = null) {
        if (!checkInitialized()) return

        val prompt = if (progress != null) {
            "$action quest '$questName' (Progress: $progress%)"
        } else {
            "$action quest '$questName'"
        }

        journalViewModel?.processUserActions(prompt)
        GameLogger.logSystem("📝 EventLogger: Quest logged - $prompt")
    }

    /**
     * Sosyal etkileşim kaydet (NPC konuşması, faction etkileşimi vb.)
     *
     * @param npcName NPC adı
     * @param interactionType Etkileşim türü (Conversation, Trade, Quest Given vb.)
     * @param relationshipChange İlişki değişimi (null, pozitif veya negatif)
     */
    fun logSocialInteraction(npcName: String, interactionType: String, relationshipChange: Int? = null) {
        if (!checkInitialized()) return

        val prompt = if (relationshipChange != null) {
            val change = if (relationshipChange > 0) "+$relationshipChange" else "$relationshipChange"
            "I had a $interactionType with $npcName (Relationship: $change)"
        } else {
            "I had a $interactionType with $npcName"
        }

        journalViewModel?.processUserActions(prompt)
        GameLogger.logSystem("📝 EventLogger: Social interaction logged - $prompt")
    }

    /**
     * Lokasyon keşfi kaydet
     *
     * @param locationName Lokasyon adı
     * @param locationType Lokasyon türü (Dungeon, Settlement, Landmark vb.)
     */
    fun logLocationDiscovery(locationName: String, locationType: String) {
        if (!checkInitialized()) return

        val prompt = "I discovered a new $locationType: $locationName"

        journalViewModel?.processUserActions(prompt)
        GameLogger.logSystem("📝 EventLogger: Location discovery logged - $prompt")
    }

    /**
     * Genel aktivite logger (özel durumlar için)
     *
     * @param activityDescription Aktivite açıklaması (doğal dil)
     */
    fun logGenericActivity(activityDescription: String) {
        if (!checkInitialized()) return

        journalViewModel?.processUserActions(activityDescription)
        GameLogger.logSystem("📝 EventLogger: Generic activity logged - $activityDescription")
    }

    /**
     * G83 P4: Mühür pratiği kaydet
     *
     * @param sealName Mühür adı
     * @param details Detaylar
     */
    fun logSealPractice(sealName: String, details: String) {
        if (!checkInitialized()) return

        val prompt = "I practiced the $sealName seal. $details"
        journalViewModel?.processUserActions(prompt)
        GameLogger.logSystem("📝 EventLogger: Seal practice logged - $prompt")
    }

    /**
     * G83 P4: Keşif/seyahat kaydet
     *
     * @param location Lokasyon adı
     * @param details Detaylar
     */
    fun logExploration(location: String, details: String) {
        if (!checkInitialized()) return

        val prompt = "I explored $location. $details"
        journalViewModel?.processUserActions(prompt)
        GameLogger.logSystem("📝 EventLogger: Exploration logged - $prompt")
    }

    /**
     * G83 P4: Kültüvasyon (detaylı versiyon)
     *
     * @param type Kültüvasyon türü
     * @param duration Süre (dakika, opsiyonel)
     * @param details Detaylar (opsiyonel)
     * @param breakthroughLevel Atılım seviyesi (opsiyonel)
     */
    fun logCultivation(type: String, duration: Int? = null, details: String? = null, breakthroughLevel: Int? = null) {
        if (!checkInitialized()) return

        val prompt = buildString {
            if (breakthroughLevel != null) {
                append("I achieved a breakthrough in $type cultivation, reaching level $breakthroughLevel!")
            } else {
                if (duration != null) {
                    append("I practiced $type cultivation for $duration minutes")
                } else {
                    append("I practiced $type cultivation")
                }
                if (details != null) {
                    append(". $details")
                }
            }
        }

        journalViewModel?.processUserActions(prompt)
        GameLogger.logSystem("📝 EventLogger: Cultivation logged - $prompt")
    }

    /**
     * G83 P4: Antrenman (detaylı versiyon)
     *
     * @param type Antrenman türü
     * @param duration Süre (dakika, opsiyonel)
     * @param intensity Yoğunluk (opsiyonel)
     * @param details Detaylar (opsiyonel)
     */
    fun logTraining(type: String, duration: Int? = null, intensity: String? = null, details: String? = null) {
        if (!checkInitialized()) return

        val prompt = buildString {
            append("I completed ")
            if (duration != null) {
                append("$duration minutes of ")
            }
            append("$type training")
            if (intensity != null) {
                append(" at $intensity intensity")
            }
            if (details != null) {
                append(". $details")
            } else {
                append(". This physical exercise should improve my strength and give me experience.")
            }
        }

        journalViewModel?.processUserActions(prompt)
        GameLogger.logSystem("📝 EventLogger: Training logged - $prompt")
    }

    /**
     * Initialized kontrolü (internal)
     */
    private fun checkInitialized(): Boolean {
        if (!isInitialized || journalViewModel == null) {
            Log.e(TAG, "❌ EventLogger not initialized! Call EventLogger.init() in MainActivity")
            return false
        }
        return true
    }
}
