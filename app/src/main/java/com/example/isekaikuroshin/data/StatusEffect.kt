package com.example.isekaikuroshin.data

import kotlinx.serialization.Serializable

/**
 * G82: Buff/Debuff System
 *
 * Status effect sistemi - poison, burn, strength buff, speed buff vs.
 */

@Serializable
enum class StatusEffectType {
    BUFF,    // Pozitif etki (strength up, speed up)
    DEBUFF   // Negatif etki (poison, burn, slow)
}

/**
 * Tek bir status effect (buff veya debuff)
 *
 * @param effectId Benzersiz ID (örn: "poison", "strength_buff")
 * @param name Görünür isim
 * @param type BUFF veya DEBUFF
 * @param magnitude Etki büyüklüğü (1-100)
 * @param durationTurns Kaç tur sürer (-1 = sonsuz)
 * @param tickDamage Her turda verilen hasar (DoT için, 0 = yok)
 * @param statModifiers Stat değişiklikleri (örn: {"strength": 20})
 * @param appliedTimestamp Ne zaman uygulandı (ms)
 * @param iconEmoji G126: Emoji icon (örn: "🔥", "💪", "🛡️") - AI tarafından seçilir
 */
@Serializable
data class StatusEffect(
    val effectId: String,
    val name: String,
    val type: StatusEffectType,
    val magnitude: Int = 1,
    val durationTurns: Int = 3,
    val tickDamage: Int = 0,
    val statModifiers: Map<String, Int> = emptyMap(),
    val appliedTimestamp: Long = System.currentTimeMillis(),
    val iconEmoji: String? = null  // G126: AI-selected emoji icon
) {
    /**
     * Etkinin süresi doldu mu?
     */
    fun isExpired(currentTurn: Int, startTurn: Int): Boolean {
        if (durationTurns == -1) return false // Sonsuz
        return (currentTurn - startTurn) >= durationTurns
    }

    /**
     * Kalan süre (tur)
     */
    fun remainingTurns(currentTurn: Int, startTurn: Int): Int {
        if (durationTurns == -1) return 999 // Sonsuz göster
        val remaining = durationTurns - (currentTurn - startTurn)
        return remaining.coerceAtLeast(0)
    }
}

/**
 * Oyuncu üzerindeki aktif bir status effect
 * Efekt + ne zaman başladığı bilgisi
 */
@Serializable
data class ActiveStatusEffect(
    val effect: StatusEffect,
    val startTurn: Int = 0 // Hangi turda başladı
)

/**
 * Yaygın status effect örnekleri
 */
object CommonStatusEffects {

    // DEBUFFS (Negatif)
    fun poison(magnitude: Int = 5, duration: Int = 3) = StatusEffect(
        effectId = "poison",
        name = "Poison",
        type = StatusEffectType.DEBUFF,
        magnitude = magnitude,
        durationTurns = duration,
        tickDamage = 10 * magnitude,
        iconEmoji = "☠️"  // G126: Poison icon
    )

    fun burn(magnitude: Int = 5, duration: Int = 2) = StatusEffect(
        effectId = "burn",
        name = "Burn",
        type = StatusEffectType.DEBUFF,
        magnitude = magnitude,
        durationTurns = duration,
        tickDamage = 15 * magnitude,
        iconEmoji = "🔥"  // G126: Burn icon
    )

    fun slow(magnitude: Int = 3, duration: Int = 3) = StatusEffect(
        effectId = "slow",
        name = "Slow",
        type = StatusEffectType.DEBUFF,
        magnitude = magnitude,
        durationTurns = duration,
        statModifiers = mapOf("agility" to -10 * magnitude),
        iconEmoji = "🐌"  // G126: Slow icon
    )

    fun weakened(magnitude: Int = 3, duration: Int = 3) = StatusEffect(
        effectId = "weakened",
        name = "Weakened",
        type = StatusEffectType.DEBUFF,
        magnitude = magnitude,
        durationTurns = duration,
        statModifiers = mapOf("strength" to -10 * magnitude),
        iconEmoji = "💀"  // G126: Weakened icon
    )

    // BUFFS (Pozitif)
    fun strengthBuff(magnitude: Int = 5, duration: Int = 5) = StatusEffect(
        effectId = "strength_buff",
        name = "Strength Up",
        type = StatusEffectType.BUFF,
        magnitude = magnitude,
        durationTurns = duration,
        statModifiers = mapOf("strength" to 10 * magnitude),
        iconEmoji = "💪"  // G126: Strength buff icon
    )

    fun speedBuff(magnitude: Int = 5, duration: Int = 5) = StatusEffect(
        effectId = "speed_buff",
        name = "Speed Up",
        type = StatusEffectType.BUFF,
        magnitude = magnitude,
        durationTurns = duration,
        statModifiers = mapOf("agility" to 10 * magnitude),
        iconEmoji = "⚡"  // G126: Speed buff icon
    )

    fun regeneration(magnitude: Int = 3, duration: Int = 5) = StatusEffect(
        effectId = "regeneration",
        name = "Regeneration",
        type = StatusEffectType.BUFF,
        magnitude = magnitude,
        durationTurns = duration,
        tickDamage = -5 * magnitude, // Negatif = iyileştirme
        iconEmoji = "💚"  // G126: Regeneration icon
    )

    fun shield(magnitude: Int = 5, duration: Int = 3) = StatusEffect(
        effectId = "shield",
        name = "Shield",
        type = StatusEffectType.BUFF,
        magnitude = magnitude,
        durationTurns = duration,
        statModifiers = mapOf("vitality" to 15 * magnitude),
        iconEmoji = "🛡️"  // G126: Shield icon
    )
}
