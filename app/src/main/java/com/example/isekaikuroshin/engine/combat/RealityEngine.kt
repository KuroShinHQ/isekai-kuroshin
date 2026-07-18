package com.example.isekaikuroshin.engine.combat

import com.example.isekaikuroshin.data.PlayerStatsPersistent
import com.example.isekaikuroshin.data.combat.LearnedSpell
import com.example.isekaikuroshin.data.spell.SpellRecipe
import com.example.isekaikuroshin.utils.GameLogger
import kotlin.math.max
import kotlin.math.min

/**
 * FAZ 8.2: GERÇEKL İK MOTORU (REALITY ENGINE)
 *
 * Kullanıcının tüm eylemlerini analiz eden merkezi beyin.
 * Her eylemin bir potansiyeli ve bir bedeli olduğunu garanti eder.
 *
 * ## Vizyon
 * Bir büyünün gücü:
 * - Sadece öğrenilmiş olmasıyla değil
 * - Potansiyeli, pratikle ne kadar geliştirildiği
 * - Kullanıcının o anki statlarıyla ne kadar "gerçekçi" olduğuyla belirlenir
 *
 * ## LUCK Mekanizması
 * Kullanıcının kendini aştığı "kader anları", LUCK stat'ı ile korunur.
 */
object RealityEngine {
    private const val TAG = "RealityEngine"

    /**
     * FAZ 8.2: Gerçeklik Kontrolü Ana Fonksiyonu
     *
     * @param action LearnedSpell veya String (fantastik eylem)
     * @param stats Oyuncunun mevcut statları
     * @return RealityCheckResult (diceModifier + GM yorumu)
     */
    fun analyzeAction(action: Any, stats: PlayerStatsPersistent): RealityCheckResult {
        return when (action) {
            is LearnedSpell -> analyzeSpellCast(action, stats)
            is String -> analyzeFantasyAction(action, stats)
            else -> RealityCheckResult(
                diceModifier = 0,
                gmComment = "Bilinmeyen eylem tipi.",
                isValid = false
            )
        }
    }

    /**
     * Büyü kullanımını analiz et
     *
     * Kontroller:
     * 1. MP Yeterliliği (manaCost vs currentMP)
     * 2. Skill Level vs Complexity
     * 3. Element Affinity
     */
    private fun analyzeSpellCast(
        spell: LearnedSpell,
        stats: PlayerStatsPersistent
    ): RealityCheckResult {
        var diceModifier = 0
        val comments = mutableListOf<String>()

        // NOT: SpellRecipe bilgisi şu anda LearnedSpell'de yok
        // Geçici olarak varsayılan değerler kullanıyoruz
        // TODO FAZ 8.4: LearnedSpell'e SpellRecipe referansı ekle
        val estimatedManaCost = calculateEstimatedManaCost(spell)

        // 1. MP Kontrolü
        val mpCostRatio = estimatedManaCost.toFloat() / stats.maxMp.toFloat()

        when {
            stats.mp < estimatedManaCost -> {
                // MP yetersiz - BÜYÜK CEZA
                val deficit = estimatedManaCost - stats.mp
                diceModifier -= min(10, (deficit / 10).toInt())
                comments.add("Manan yetersiz! (Eksik: $deficit MP)")
                GameLogger.logSystem("[REALITY] ⚠️ MP yetersiz: ${stats.mp}/${estimatedManaCost}")
            }
            mpCostRatio > 0.8f -> {
                // MP neredeyse tükeniyor - hafif ceza
                diceModifier -= 2
                comments.add("Mana rezervin tehlikeli derecede düşük.")
            }
            mpCostRatio < 0.3f -> {
                // MP bol - bonus
                diceModifier += 1
                comments.add("Manan taşıyor, büyü rahatça şekilleniyor.")
            }
        }

        // 2. Skill Level Analizi
        val skillBonus = spell.skillTreeLevel / 2  // Lv10 = +5 bonus
        diceModifier += skillBonus

        when {
            spell.skillTreeLevel >= 8 -> {
                comments.add("Bu büyüde ustalık seviyesindesin! (+$skillBonus)")
            }
            spell.skillTreeLevel >= 5 -> {
                comments.add("Büyüye hakimsin. (+$skillBonus)")
            }
            spell.skillTreeLevel <= 2 -> {
                comments.add("Henüz acemisin, büyü kontrolden çıkabilir.")
            }
        }

        // 3. Element Affinity Kontrolü
        val elementAffinityKey = spell.element.name
        val affinity = stats.elementAffinities[elementAffinityKey] ?: 0

        when {
            affinity >= 70 -> {
                diceModifier += 2
                comments.add("Element affinityn güçlü! (+2)")
            }
            affinity >= 40 -> {
                diceModifier += 1
                comments.add("Element uyum sağlıyor. (+1)")
            }
            affinity < 20 -> {
                diceModifier -= 1
                comments.add("Bu element senin doğana yabancı. (-1)")
            }
        }

        // 4. Accuracy (Kalibrasyon Kalitesi)
        val accuracy = spell.getAverageAccuracy()
        val accuracyBonus = ((accuracy - 0.5f) * 4).toInt()  // 0.5 = nötr, 1.0 = +2, 0.0 = -2
        diceModifier += accuracyBonus

        if (accuracy >= 0.8f) {
            comments.add("Tetikleyici kalibrasyonun mükemmel! (+$accuracyBonus)")
        } else if (accuracy < 0.5f) {
            comments.add("Tetikleyici kalibrasyonun zayıf. ($accuracyBonus)")
        }

        val gmComment = comments.joinToString(" ")
        GameLogger.logSystem("[REALITY] Spell: ${spell.customName}, Modifier: $diceModifier, MP: ${stats.mp}/$estimatedManaCost")

        return RealityCheckResult(
            diceModifier = diceModifier,
            gmComment = gmComment,
            isValid = stats.mp >= estimatedManaCost
        )
    }

    /**
     * Fantastik eylem analizi (String input)
     *
     * TODO FAZ 8.5: Gemini API entegrasyonu
     * Şu an basit kurallarla çalışıyor
     */
    fun analyzeFantasyAction(
        action: String,
        stats: PlayerStatsPersistent
    ): RealityCheckResult {
        var diceModifier = 0
        val comments = mutableListOf<String>()

        // Basit kompleksite analizi (kelime sayısı)
        val wordCount = action.split(" ").size
        val complexity = when {
            wordCount > 15 -> "very_complex"
            wordCount > 10 -> "complex"
            wordCount > 5 -> "moderate"
            else -> "simple"
        }

        when (complexity) {
            "very_complex" -> {
                diceModifier -= 3
                comments.add("Çok karmaşık bir eylem, başarısı zor.")
            }
            "complex" -> {
                diceModifier -= 1
                comments.add("Karmaşık bir girişim.")
            }
            "simple" -> {
                diceModifier += 1
                comments.add("Basit ve net bir eylem.")
            }
        }

        // Fiziksel eylem mi, mental eylem mi?
        val isPhysical = action.contains(Regex("(koş|zıpla|vur|saldır|kaç|at)", RegexOption.IGNORE_CASE))
        val isMental = action.contains(Regex("(düşün|hatırla|analiz|gözlemle|okuma)", RegexOption.IGNORE_CASE))

        if (isPhysical) {
            val physicalBonus = ((stats.strength + stats.agility + stats.vitality) / 30).toInt() - 1  // Ortalama 10 = 0 bonus
            diceModifier += physicalBonus
            comments.add("Fiziksel stat bonusu: $physicalBonus")
        }

        if (isMental) {
            val mentalBonus = ((stats.intelligence + stats.spirit) / 20).toInt() - 1
            diceModifier += mentalBonus
            comments.add("Mental stat bonusu: $mentalBonus")
        }

        // Stamina kontrolü
        if (isPhysical && stats.stamina < stats.maxStamina * 0.3f) {
            diceModifier -= 2
            comments.add("Yorgunsun, fiziksel performans düşük. (-2)")
        }

        val gmComment = comments.joinToString(" ")
        GameLogger.logSystem("[REALITY] Fantasy Action: \"$action\", Modifier: $diceModifier")

        return RealityCheckResult(
            diceModifier = diceModifier,
            gmComment = gmComment,
            isValid = true
        )
    }

    /**
     * Tahmini Mana Maliyeti Hesaplama
     * (SpellRecipe referansı olmadan geçici çözüm)
     */
    private fun calculateEstimatedManaCost(spell: LearnedSpell): Int {
        // Basit formül: skillLevel càršà azalır, complexity arttıràrsa artar
        val baselineCost = 20
        val skillReduction = spell.skillTreeLevel * 1  // Her level -1 MP
        val elementMultiplier = when (spell.element.name) {
            "NEUTRAL" -> 0.8f
            "FIRE", "LIGHTNING" -> 1.2f
            "WATER", "EARTH" -> 1.0f
            else -> 1.1f
        }

        return max(5, ((baselineCost - skillReduction) * elementMultiplier).toInt())
    }

    /**
     * FAZ 8.3: LUCK Mekanizması - "Kader Anı" Kontrolü
     *
     * Zar atıldıktan SONRA çağrılır.
     * Eğer sonuç "neredeyse başarısız" ise, LUCK stat'ına göre kurtarma şansı verir.
     *
     * @param diceResult Zar sonucu (1-20)
     * @param targetDC Hedef zorluk (Difficulty Class)
     * @param luckStat Oyuncunun LUCK değeri
     * @return Kurtarıldı mı?
     */
    fun checkLuckIntervention(
        diceResult: Int,
        targetDC: Int,
        luckStat: Long
    ): LuckInterventionResult {
        // "Neredeyse başarısız" = DC'ye 3 puan içinde
        val deficit = targetDC - diceResult
        if (deficit !in 1..3) {
            return LuckInterventionResult(saved = false, message = "")
        }

        // LUCK'a göre kurtarma şansı
        // LUCK 10 = %10, LUCK 20 = %20, LUCK 30 = %30
        val luckChance = min(luckStat.toInt(), 50)  // Max %50
        val roll = (1..100).random()

        val saved = roll <= luckChance

        val message = if (saved) {
            "✨ KADER MÜDAHALESI! Şansın seni $deficit puan kurtardı! (LUCK: $luckStat, Zarın: $roll/$luckChance)"
        } else {
            ""
        }

        if (saved) {
            GameLogger.logSystem("[REALITY] 🍀 LUCK SAVE! DC: $targetDC, Roll: $diceResult → SAVED (Luck: $luckStat)")
        }

        return LuckInterventionResult(saved = saved, message = message)
    }
}

/**
 * Gerçeklik Kontrolü Sonucu
 */
data class RealityCheckResult(
    /**
     * Zar atışına eklenecek/çıkarılacak modifier
     * Pozitif = Kolaylaştırıcı, Negatif = Zorlaştırıcı
     */
    val diceModifier: Int,

    /**
     * GM yorumu (oyuncuya gösterilecek)
     */
    val gmComment: String,

    /**
     * Eylem geçerli mi? (MP yetersizse false)
     */
    val isValid: Boolean = true
)

/**
 * LUCK Müdahalesi Sonucu
 */
data class LuckInterventionResult(
    /**
     * Kurtarıldı mı?
     */
    val saved: Boolean,

    /**
     * Oyuncuya gösterilecek mesaj
     */
    val message: String
)
