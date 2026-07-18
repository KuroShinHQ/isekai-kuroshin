package com.example.isekaikuroshin.data

import com.example.isekaikuroshin.engine.StatType
import kotlinx.serialization.Serializable

/**
 * Skill Data Model - Genişletilmiş Yetenek Sistemi
 *
 * ÖZET:
 * - Element affinity: Tüm aynı element skilllerine etki eder
 * - Rarity progression: Kullanım sıklığına göre rütbe artar
 * - Dice modifier: Savaş/eylem sırasında zar atışına bonus/malus
 * - Badge multiplier: Rozet sistemi ile otomatik çarpan
 * - Weapon durability damage: S+ rarity skilllerde silah kırılma riski
 */
@Serializable
data class Skill(
    // ========== TEMEL BİLGİLER ==========
    val id: String = "",  // GM tarafından üretilirken veya manuel eklenirken doldurulur
    val name: String,
    val description: String,

    // ========== ELEMENT SİSTEMİ ==========
    val elementType: ElementType = ElementType.NEUTRAL,

    // ========== RARİTY VE TIER ==========
    val rarity: SkillRarity = SkillRarity.F,  // Nadirlik (G-F-E-D-C-B-A-S-SS-SSS-SSS+)
    val tier: String,  // Varyasyon seviyesi ("Temel", "Gelişmiş", "Usta")

    // ========== LEVEL VE XP ==========
    val level: Int = 0,  // Skill seviyesi (0'dan başlar)
    val maxLevel: Int = 100,  // Max seviye
    val currentXP: Int = 0,
    val xpToNextLevel: Int = 100,

    // ========== EVRIM VE VARYASYON ==========
    val evolutionProgress: Float = 0f,  // Varyasyon açmak için (0.0-1.0)
    val parentSkillId: String? = null,  // Varyasyon ağacı (ör: "fire_ball" → "fire_storm")
    val availableVariations: List<String> = emptyList(),  // Seçilebilir varyasyonlar

    // ========== MANA VE COOLDOWN ==========
    val manaCost: Int,
    val baseCooldown: Int = 0,  // Saniye cinsinden temel cooldown
    val currentCooldown: Int = 0,  // Mevcut kalan cooldown

    // ========== DİCE VE COMBAT ==========
    val diceModifier: Int = 0,  // Zar atışına bonus/malus (-10 ile +10 arası)
    val weaponDurabilityDamage: Float = 0.0f,  // Silah durability'sine hasar (0.0-1.0)

    // ========== STAT BONUSLARI ==========
    val statBonuses: Map<StatType, Float> = emptyMap(),

    // ========== ROZET SİSTEMİ ==========
    val linkedBadgeId: String? = null,  // Bağlı rozet ID'si
    val badgeMultiplier: Float = 1.0f,  // Rozet çarpanı (otomatik hesaplanır)

    // ========== KULLANIM İSTATİSTİKLERİ ==========
    val usageCount: Int = 0,  // Kullanım sayısı (rarity artışı için)
    val lastUsedTimestamp: Long = 0,  // Son kullanım zamanı

    // ========== GM GENERATED SKILLS ==========
    val isGMGenerated: Boolean = false,  // GM tarafından üretildi mi?
    val iconPath: String? = null,  // GM-generated skillller için icon yolu (DEPRECATED)
    val iconEmoji: String? = null,  // G126: Emoji icon (örn: "⚔️", "🔥", "✨") - AI tarafından seçilir
    val gmMetadata: Map<String, String> = emptyMap(),  // GM tarafından eklenen ekstra veriler

    // ========== KADIM MÜHÜR TEKNİKLERİ ==========
    val linkedSealId: String? = null,  // Bu skill hangi mühürle ilişkili?
    val practiceMetrics: PracticeMetrics? = null  // Pratik istatistikleri
) {
    /**
     * Skill seviye atlayabilir mi?
     */
    fun canLevelUp(): Boolean = currentXP >= xpToNextLevel && level < maxLevel

    /**
     * Varyasyon açılabilir mi?
     */
    fun canUnlockVariation(): Boolean = evolutionProgress >= 1.0f && availableVariations.isNotEmpty()

    /**
     * Efektif mana maliyeti (rarity ve badge çarpanlarıyla)
     */
    fun getEffectiveManaCost(): Int {
        val rarityMultiplier = when {
            rarity.tier >= SkillRarity.S.tier -> 1.5f  // S+ rarity'de mana maliyeti artar
            rarity.tier >= SkillRarity.A.tier -> 1.2f
            else -> 1.0f
        }
        val badgeReduction = 1.0f - (badgeMultiplier - 1.0f) * 0.1f  // Badge çarpanı mana maliyetini azaltır
        return (manaCost * rarityMultiplier * badgeReduction).toInt().coerceAtLeast(1)
    }

    /**
     * Efektif cooldown (rarity ve seviye ile azalır)
     */
    fun getEffectiveCooldown(): Int {
        val levelReduction = level * 0.01f  // Her seviye %1 cooldown azaltır
        val rarityReduction = rarity.tier * 0.05f  // Her rarity tier %5 cooldown azaltır
        val reduction = (levelReduction + rarityReduction).coerceAtMost(0.7f)  // Max %70 azalma
        return (baseCooldown * (1.0f - reduction)).toInt().coerceAtLeast(0)
    }

    /**
     * Dice roll için toplam modifier (skill + badge bonusları)
     */
    fun getTotalDiceModifier(): Int {
        val badgeBonus = ((badgeMultiplier - 1.0f) * 5).toInt()  // Badge çarpanı dice'a bonus verir
        return diceModifier + badgeBonus
    }
}