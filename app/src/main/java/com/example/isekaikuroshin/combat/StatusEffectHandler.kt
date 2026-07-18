package com.example.isekaikuroshin.combat

import com.example.isekaikuroshin.data.Enemy
import com.example.isekaikuroshin.data.PlayerStats
import com.example.isekaikuroshin.utils.GameLogger
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * G95: Status Effect Handler - Phase 2E
 *
 * Manages all status effects in combat:
 * - Poison (damage over time)
 * - Regen (heal over time)
 * - Stun (skip turn)
 * - Burn (damage over time + reduce defense)
 * - Freeze (skip turn + reduce speed)
 * - Attack buffs/debuffs
 * - Defense buffs/debuffs
 * - Speed buffs/debuffs
 *
 * KURAL 11: Metadata-based (effect type enum, not string names)
 * KURAL 14: Performance optimized (efficient list operations)
 * KURAL 15: Root cause analysis (detailed logging for debugging)
 */
@Singleton
class StatusEffectHandler @Inject constructor() {
    companion object {
        private const val TAG = "StatusEffectHandler"

        // Effect potencies (percentage of max HP/stat)
        private const val POISON_DAMAGE_PERCENT = 0.05f // 5% max HP per turn
        private const val REGEN_HEAL_PERCENT = 0.10f // 10% max HP per turn
        private const val BURN_DAMAGE_PERCENT = 0.08f // 8% max HP per turn
        private const val BURN_DEFENSE_REDUCTION = 0.20f // -20% defense

        // Stat modification percentages
        private const val ATTACK_BUFF_PERCENT = 0.25f // +25% attack
        private const val DEFENSE_BUFF_PERCENT = 0.30f // +30% defense
        private const val SPEED_BUFF_PERCENT = 0.20f // +20% speed
        private const val ATTACK_DEBUFF_PERCENT = 0.25f // -25% attack
        private const val DEFENSE_DEBUFF_PERCENT = 0.30f // -30% defense
        private const val SPEED_DEBUFF_PERCENT = 0.20f // -20% speed
    }

    /**
     * Apply all status effects to entity at start of turn
     *
     * @param entity Combat entity (wrapper for Enemy or Player)
     * @return Damage/healing dealt by effects
     */
    suspend fun applyStatusEffectsToEntity(entity: CombatEntity): StatusEffectResult {
        var totalDamage = 0
        var totalHealing = 0
        var isStunned = false
        val messages = mutableListOf<String>()
        val effectsToRemove = mutableListOf<StatusEffect>()

        GameLogger.logSystem("🔮 $TAG: Applying status effects to ${entity.name}...")

        entity.statusEffects.forEach { effect ->
            when (effect.type) {
                StatusEffectType.POISON -> {
                    val damage = (entity.maxHp * POISON_DAMAGE_PERCENT * effect.potency).toInt()
                    entity.hp = max(0, entity.hp - damage)
                    totalDamage += damage
                    messages.add("${entity.name} takes $damage poison damage! 🟢")
                    GameLogger.logSystem("☠️ $TAG: ${entity.name} poisoned for $damage damage")
                }

                StatusEffectType.REGEN -> {
                    val healing = (entity.maxHp * REGEN_HEAL_PERCENT * effect.potency).toInt()
                    entity.hp = min(entity.maxHp, entity.hp + healing)
                    totalHealing += healing
                    messages.add("${entity.name} regenerates $healing HP! ✨")
                    GameLogger.logSystem("💚 $TAG: ${entity.name} regenerated $healing HP")
                }

                StatusEffectType.BURN -> {
                    val damage = (entity.maxHp * BURN_DAMAGE_PERCENT * effect.potency).toInt()
                    entity.hp = max(0, entity.hp - damage)
                    totalDamage += damage
                    messages.add("${entity.name} takes $damage burn damage! 🔥")
                    GameLogger.logSystem("🔥 $TAG: ${entity.name} burned for $damage damage")
                }

                StatusEffectType.STUN -> {
                    isStunned = true
                    messages.add("${entity.name} is stunned and cannot act! 💫")
                    GameLogger.logSystem("💫 $TAG: ${entity.name} is stunned")
                }

                StatusEffectType.FREEZE -> {
                    isStunned = true
                    messages.add("${entity.name} is frozen and cannot act! ❄️")
                    GameLogger.logSystem("❄️ $TAG: ${entity.name} is frozen")
                }

                StatusEffectType.ATTACK_BUFF,
                StatusEffectType.DEFENSE_BUFF,
                StatusEffectType.SPEED_BUFF,
                StatusEffectType.ATTACK_DEBUFF,
                StatusEffectType.DEFENSE_DEBUFF,
                StatusEffectType.SPEED_DEBUFF -> {
                    // Stat modifiers are applied in calculateModifiedStats()
                    GameLogger.logSystem("📊 $TAG: ${entity.name} has ${effect.type} active")
                }
            }

            // Decrement duration
            effect.duration--
            if (effect.duration <= 0) {
                effectsToRemove.add(effect)
                messages.add("${effect.type.displayName} effect on ${entity.name} has worn off.")
                GameLogger.logSystem("⏱️ $TAG: ${effect.type} on ${entity.name} expired")
            }
        }

        // Remove expired effects
        entity.statusEffects.removeAll(effectsToRemove)

        return StatusEffectResult(
            totalDamage = totalDamage,
            totalHealing = totalHealing,
            isStunned = isStunned,
            messages = messages
        )
    }

    /**
     * Add status effect to entity
     *
     * @param entity Target entity
     * @param effectType Type of effect
     * @param duration Turns remaining
     * @param potency Effect strength multiplier (1.0 = normal)
     */
    fun addStatusEffect(
        entity: CombatEntity,
        effectType: StatusEffectType,
        duration: Int,
        potency: Float = 1.0f
    ) {
        // Check if effect already exists
        val existingEffect = entity.statusEffects.find { it.type == effectType }

        if (existingEffect != null) {
            // Refresh duration and update potency if stronger
            existingEffect.duration = max(existingEffect.duration, duration)
            existingEffect.potency = max(existingEffect.potency, potency)
            GameLogger.logSystem("🔄 $TAG: Refreshed $effectType on ${entity.name}")
        } else {
            // Add new effect
            entity.statusEffects.add(
                StatusEffect(
                    type = effectType,
                    duration = duration,
                    potency = potency
                )
            )
            GameLogger.logSystem("➕ $TAG: Added $effectType to ${entity.name} (${duration} turns)")
        }
    }

    /**
     * Remove status effect from entity
     */
    fun removeStatusEffect(entity: CombatEntity, effectType: StatusEffectType) {
        val removed = entity.statusEffects.removeAll { it.type == effectType }
        if (removed) {
            GameLogger.logSystem("➖ $TAG: Removed $effectType from ${entity.name}")
        }
    }

    /**
     * Clear all status effects from entity
     */
    fun clearAllStatusEffects(entity: CombatEntity) {
        entity.statusEffects.clear()
        GameLogger.logSystem("🧹 $TAG: Cleared all status effects from ${entity.name}")
    }

    /**
     * Check if entity has specific status effect
     */
    fun hasStatusEffect(entity: CombatEntity, effectType: StatusEffectType): Boolean {
        return entity.statusEffects.any { it.type == effectType }
    }

    /**
     * Get modified stats based on active buffs/debuffs
     *
     * @param baseStats Base stats
     * @param statusEffects Active status effects
     * @return Modified stats
     */
    fun calculateModifiedStats(
        baseStats: CombatStats,
        statusEffects: List<StatusEffect>
    ): CombatStats {
        var modifiedAttack = baseStats.attack.toFloat()
        var modifiedDefense = baseStats.defense.toFloat()
        var modifiedSpeed = baseStats.speed.toFloat()

        statusEffects.forEach { effect ->
            when (effect.type) {
                StatusEffectType.ATTACK_BUFF -> {
                    modifiedAttack *= (1 + ATTACK_BUFF_PERCENT * effect.potency)
                }
                StatusEffectType.ATTACK_DEBUFF -> {
                    modifiedAttack *= (1 - ATTACK_DEBUFF_PERCENT * effect.potency)
                }
                StatusEffectType.DEFENSE_BUFF -> {
                    modifiedDefense *= (1 + DEFENSE_BUFF_PERCENT * effect.potency)
                }
                StatusEffectType.DEFENSE_DEBUFF -> {
                    modifiedDefense *= (1 - DEFENSE_DEBUFF_PERCENT * effect.potency)
                }
                StatusEffectType.SPEED_BUFF -> {
                    modifiedSpeed *= (1 + SPEED_BUFF_PERCENT * effect.potency)
                }
                StatusEffectType.SPEED_DEBUFF -> {
                    modifiedSpeed *= (1 - SPEED_DEBUFF_PERCENT * effect.potency)
                }
                StatusEffectType.BURN -> {
                    // Burn reduces defense
                    modifiedDefense *= (1 - BURN_DEFENSE_REDUCTION * effect.potency)
                }
                else -> {
                    // No stat modification
                }
            }
        }

        return CombatStats(
            attack = modifiedAttack.toInt(),
            defense = modifiedDefense.toInt(),
            speed = modifiedSpeed.toInt()
        )
    }

    /**
     * Get status effect icon for UI
     */
    fun getStatusEffectIcon(effectType: StatusEffectType): String {
        return when (effectType) {
            StatusEffectType.POISON -> "☠️"
            StatusEffectType.REGEN -> "💚"
            StatusEffectType.BURN -> "🔥"
            StatusEffectType.STUN -> "💫"
            StatusEffectType.FREEZE -> "❄️"
            StatusEffectType.ATTACK_BUFF -> "⚔️↑"
            StatusEffectType.ATTACK_DEBUFF -> "⚔️↓"
            StatusEffectType.DEFENSE_BUFF -> "🛡️↑"
            StatusEffectType.DEFENSE_DEBUFF -> "🛡️↓"
            StatusEffectType.SPEED_BUFF -> "⚡↑"
            StatusEffectType.SPEED_DEBUFF -> "⚡↓"
        }
    }
}

/**
 * Status Effect Data Class
 * KURAL 11: Metadata-based (type enum, not string)
 */
@Serializable
data class StatusEffect(
    val type: StatusEffectType,
    var duration: Int, // Turns remaining
    var potency: Float = 1.0f // Effect strength multiplier
)

/**
 * Status Effect Type Enum
 * KURAL 11: Metadata-based identification
 */
@Serializable
enum class StatusEffectType(val displayName: String) {
    // Damage over time
    POISON("Poison"),
    BURN("Burn"),

    // Healing over time
    REGEN("Regeneration"),

    // Crowd control
    STUN("Stun"),
    FREEZE("Freeze"),

    // Buffs
    ATTACK_BUFF("Attack Up"),
    DEFENSE_BUFF("Defense Up"),
    SPEED_BUFF("Speed Up"),

    // Debuffs
    ATTACK_DEBUFF("Attack Down"),
    DEFENSE_DEBUFF("Defense Down"),
    SPEED_DEBUFF("Speed Down")
}

/**
 * Combat Entity - Wrapper for Player or Enemy
 * Contains mutable HP and status effects list
 */
data class CombatEntity(
    val id: String,
    val name: String,
    var hp: Int,
    val maxHp: Int,
    val baseStats: CombatStats,
    val statusEffects: MutableList<StatusEffect> = mutableListOf()
) {
    /**
     * Get current stats (with status effect modifiers)
     */
    fun getCurrentStats(statusEffectHandler: StatusEffectHandler): CombatStats {
        return statusEffectHandler.calculateModifiedStats(baseStats, statusEffects)
    }

    /**
     * Check if alive
     */
    fun isAlive(): Boolean = hp > 0

    /**
     * Check if dead
     */
    fun isDead(): Boolean = hp <= 0
}

/**
 * Combat Stats - Attack, Defense, Speed
 */
data class CombatStats(
    val attack: Int,
    val defense: Int,
    val speed: Int
)

/**
 * Status Effect Result - Result of applying effects
 */
data class StatusEffectResult(
    val totalDamage: Int,
    val totalHealing: Int,
    val isStunned: Boolean,
    val messages: List<String>
)

/**
 * Extension: Convert Enemy to CombatEntity
 */
fun Enemy.toCombatEntity(): CombatEntity {
    return CombatEntity(
        id = id,
        name = name,
        hp = hp,
        maxHp = maxHp,
        baseStats = CombatStats(
            attack = attack,
            defense = defense,
            speed = this.speed // Uses extension from TurnQueue.kt
        ),
        statusEffects = mutableListOf() // TODO-G95: Load from Enemy data class
    )
}

/**
 * Extension: Convert PlayerStats to CombatEntity
 */
fun PlayerStats.toCombatEntity(name: String = "Player"): CombatEntity {
    return CombatEntity(
        id = "PLAYER",
        name = name,
        hp = hp,
        maxHp = maxHp,
        baseStats = CombatStats(
            attack = attack,
            defense = defense,
            speed = speed
        ),
        statusEffects = mutableListOf() // TODO-G95: Load from PlayerStats
    )
}
