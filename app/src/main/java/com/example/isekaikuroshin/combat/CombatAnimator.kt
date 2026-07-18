package com.example.isekaikuroshin.combat

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import com.example.isekaikuroshin.utils.GameLogger
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * G95: Combat Animator - Phase 2D
 *
 * Handles all combat animations:
 * - Attack animations (slide, impact, shake)
 * - Spell projectiles (fireball, lightning, etc.)
 * - Damage numbers (fly up + fade out)
 * - HP bar animations
 * - Victory/Defeat animations
 * - Screen shake on critical hits
 *
 * KURAL 14: Performance optimized (use Animatable, limit particle count)
 * KURAL 15: Root cause analysis (animations logged for debugging)
 *
 * Architecture:
 * - Suspend functions for sequential animation steps
 * - Compose State for reactive UI updates
 * - Kotlin Coroutines for timing control
 */
@Singleton
class CombatAnimator @Inject constructor() {
    companion object {
        private const val TAG = "CombatAnimator"

        // Animation durations (milliseconds)
        private const val SLIDE_DURATION = 200L
        private const val IMPACT_DURATION = 100L
        private const val SHAKE_DURATION = 300L
        private const val DAMAGE_NUMBER_DURATION = 1000L
        private const val HP_BAR_DURATION = 500L
        private const val PROJECTILE_DURATION = 400L

        // Animation intensities
        private const val SHAKE_INTENSITY_NORMAL = 5f
        private const val SHAKE_INTENSITY_CRITICAL = 10f
        private const val SLIDE_DISTANCE = 50f
    }

    // Animation states (for Compose UI to observe)
    private val _animationState = mutableStateOf<AnimationState>(AnimationState.Idle)
    val animationState: State<AnimationState> = _animationState

    /**
     * Play attack animation sequence
     *
     * Sequence:
     * 1. Attacker slides forward
     * 2. Impact effect
     * 3. Damage number flies up
     * 4. Target shakes
     * 5. HP bar decreases
     * 6. Attacker slides back
     *
     * @param attacker ID of attacker
     * @param target ID of target
     * @param damage Damage dealt
     * @param isCritical Is critical hit?
     */
    suspend fun playAttackAnimation(
        attacker: String,
        target: String,
        damage: Int,
        isCritical: Boolean = false
    ) {
        GameLogger.logSystem("🎬 $TAG: Attack animation: $attacker → $target ($damage dmg)")

        // 1. Attacker slides forward
        _animationState.value = AnimationState.AttackerSlideForward(attacker)
        delay(SLIDE_DURATION)

        // 2. Impact effect
        _animationState.value = AnimationState.Impact(target, isCritical)
        delay(IMPACT_DURATION)

        // 3. Damage number + shake simultaneously
        _animationState.value = AnimationState.DamageNumber(
            target = target,
            damage = damage,
            isCritical = isCritical,
            damageType = DamageType.PHYSICAL
        )

        // 4. Target shakes
        val shakeIntensity = if (isCritical) SHAKE_INTENSITY_CRITICAL else SHAKE_INTENSITY_NORMAL
        _animationState.value = AnimationState.Shake(target, shakeIntensity)
        delay(SHAKE_DURATION)

        // 5. HP bar decrease (handled by HP bar component)
        delay(HP_BAR_DURATION)

        // 6. Attacker slides back
        _animationState.value = AnimationState.AttackerSlideBack(attacker)
        delay(SLIDE_DURATION)

        // Reset to idle
        _animationState.value = AnimationState.Idle
        GameLogger.logSystem("✅ $TAG: Attack animation complete")
    }

    /**
     * Play spell animation sequence
     *
     * @param caster ID of spell caster
     * @param spellType Type of spell (FIRE, ICE, LIGHTNING, etc.)
     * @param target ID of target
     * @param damage Damage dealt
     */
    suspend fun playSpellAnimation(
        caster: String,
        spellType: SpellType,
        target: String,
        damage: Int
    ) {
        GameLogger.logSystem("🎬 $TAG: Spell animation: $caster casts $spellType → $target ($damage dmg)")

        when (spellType) {
            SpellType.FIRE -> {
                // Fireball projectile
                _animationState.value = AnimationState.Projectile(
                    from = caster,
                    to = target,
                    projectileType = ProjectileType.FIREBALL
                )
                delay(PROJECTILE_DURATION)

                // Explosion impact
                _animationState.value = AnimationState.Impact(target, isCritical = false)
                delay(IMPACT_DURATION)
            }

            SpellType.ICE -> {
                // Ice shard projectile
                _animationState.value = AnimationState.Projectile(
                    from = caster,
                    to = target,
                    projectileType = ProjectileType.ICE_SHARD
                )
                delay(PROJECTILE_DURATION)

                // Freeze effect
                _animationState.value = AnimationState.StatusEffect(target, AnimationEffectType.FREEZE)
                delay(300L)
            }

            SpellType.LIGHTNING -> {
                // Instant lightning bolt
                _animationState.value = AnimationState.Lightning(caster, target)
                delay(200L)
            }

            SpellType.HEAL -> {
                // Healing sparkles
                _animationState.value = AnimationState.Heal(target, damage) // damage = heal amount
                delay(500L)
            }

            SpellType.BUFF -> {
                // Buff aura
                _animationState.value = AnimationState.StatusEffect(target, AnimationEffectType.BUFF)
                delay(400L)
            }

            SpellType.DEBUFF -> {
                // Debuff cloud
                _animationState.value = AnimationState.StatusEffect(target, AnimationEffectType.DEBUFF)
                delay(400L)
            }
        }

        // Show damage number (or heal number)
        if (spellType != SpellType.HEAL) {
            _animationState.value = AnimationState.DamageNumber(
                target = target,
                damage = damage,
                isCritical = false,
                damageType = DamageType.MAGICAL
            )
            delay(DAMAGE_NUMBER_DURATION)
        }

        // Reset to idle
        _animationState.value = AnimationState.Idle
        GameLogger.logSystem("✅ $TAG: Spell animation complete")
    }

    /**
     * Play victory animation
     *
     * - Gold coins rain down
     * - XP bar fills up
     * - Item cards flip in
     */
    suspend fun playVictoryAnimation(
        xpGained: Int,
        goldGained: Int,
        itemsGained: List<String>
    ) {
        GameLogger.logSystem("🎬 $TAG: Victory animation start")

        // 1. Victory fanfare
        _animationState.value = AnimationState.Victory
        delay(500L)

        // 2. Gold coins rain
        _animationState.value = AnimationState.GoldRain(goldGained)
        delay(1000L)

        // 3. XP bar fill
        _animationState.value = AnimationState.XPBarFill(xpGained)
        delay(1000L)

        // 4. Item cards flip in (one by one)
        itemsGained.forEachIndexed { index, item ->
            _animationState.value = AnimationState.ItemCardFlip(item, index)
            delay(300L)
        }

        // Reset to idle
        _animationState.value = AnimationState.Idle
        GameLogger.logSystem("✅ $TAG: Victory animation complete")
    }

    /**
     * Play defeat animation
     *
     * - Screen fades to dark
     * - Defeat text appears
     */
    suspend fun playDefeatAnimation() {
        GameLogger.logSystem("🎬 $TAG: Defeat animation start")

        // Fade to black
        _animationState.value = AnimationState.Defeat
        delay(1500L)

        // Reset to idle
        _animationState.value = AnimationState.Idle
        GameLogger.logSystem("✅ $TAG: Defeat animation complete")
    }

    /**
     * Play screen shake (for critical hits or powerful attacks)
     *
     * @param intensity Shake intensity (pixels)
     */
    suspend fun playScreenShake(intensity: Float = SHAKE_INTENSITY_NORMAL) {
        GameLogger.logSystem("🎬 $TAG: Screen shake (intensity: $intensity)")

        _animationState.value = AnimationState.ScreenShake(intensity)
        delay(SHAKE_DURATION)

        _animationState.value = AnimationState.Idle
    }

    /**
     * Show floating damage number
     */
    suspend fun showDamageNumber(
        target: String,
        damage: Int,
        isCritical: Boolean = false,
        damageType: DamageType = DamageType.PHYSICAL
    ) {
        GameLogger.logSystem("🎬 $TAG: Damage number: $damage on $target")

        _animationState.value = AnimationState.DamageNumber(
            target = target,
            damage = damage,
            isCritical = isCritical,
            damageType = damageType
        )
        delay(DAMAGE_NUMBER_DURATION)

        _animationState.value = AnimationState.Idle
    }

    /**
     * Cancel current animation (emergency stop)
     */
    fun cancelAnimation() {
        GameLogger.logSystem("⏹️ $TAG: Animation cancelled")
        _animationState.value = AnimationState.Idle
    }

    /**
     * Check if animation is playing
     */
    fun isAnimating(): Boolean {
        return _animationState.value !is AnimationState.Idle
    }
}

/**
 * Animation State - Represents current animation
 */
sealed class AnimationState {
    object Idle : AnimationState()

    // Attack animations
    data class AttackerSlideForward(val attackerId: String) : AnimationState()
    data class AttackerSlideBack(val attackerId: String) : AnimationState()
    data class Impact(val targetId: String, val isCritical: Boolean) : AnimationState()
    data class Shake(val targetId: String, val intensity: Float) : AnimationState()

    // Damage/Heal numbers
    data class DamageNumber(
        val target: String,
        val damage: Int,
        val isCritical: Boolean,
        val damageType: DamageType
    ) : AnimationState()

    data class Heal(val targetId: String, val amount: Int) : AnimationState()

    // Spell animations
    data class Projectile(
        val from: String,
        val to: String,
        val projectileType: ProjectileType
    ) : AnimationState()

    data class Lightning(val from: String, val to: String) : AnimationState()

    // Status effects
    data class StatusEffect(val targetId: String, val effectType: AnimationEffectType) : AnimationState()

    // Victory/Defeat
    object Victory : AnimationState()
    data class GoldRain(val amount: Int) : AnimationState()
    data class XPBarFill(val amount: Int) : AnimationState()
    data class ItemCardFlip(val itemName: String, val index: Int) : AnimationState()
    object Defeat : AnimationState()

    // Screen effects
    data class ScreenShake(val intensity: Float) : AnimationState()
}

/**
 * Spell Type - Different spell visual effects
 */
enum class SpellType {
    FIRE,       // Fireball projectile
    ICE,        // Ice shard projectile + freeze
    LIGHTNING,  // Instant lightning bolt
    HEAL,       // Healing sparkles
    BUFF,       // Buff aura (glow)
    DEBUFF      // Debuff cloud (dark)
}

/**
 * Projectile Type - Visual appearance of projectile
 */
enum class ProjectileType {
    FIREBALL,   // 🔥 Fire ball
    ICE_SHARD,  // ❄️ Ice shard
    ARROW,      // 🏹 Arrow
    ROCK,       // 🪨 Rock
    MAGIC       // ✨ Magic sparkle
}

/**
 * Damage Type - Affects damage number color
 */
enum class DamageType {
    PHYSICAL,   // Red/orange
    MAGICAL,    // Blue/purple
    POISON,     // Green
    HEAL        // Green/yellow (positive)
}

/**
 * Animation Status Effect Type - Simplified for visual effects
 * Maps to StatusEffectHandler.StatusEffectType
 */
enum class AnimationEffectType {
    FREEZE,     // ❄️ Ice crystals
    BURN,       // 🔥 Fire particles
    POISON,     // 🟢 Poison bubbles
    STUN,       // ⭐ Stars circling
    BUFF,       // ✨ Golden glow
    DEBUFF      // 💨 Dark cloud
}

/**
 * Animation Helper Functions
 */
object AnimationHelper {
    /**
     * Calculate projectile trajectory
     */
    fun calculateTrajectory(from: Offset, to: Offset, progress: Float): Offset {
        // Simple linear interpolation
        return Offset(
            x = from.x + (to.x - from.x) * progress,
            y = from.y + (to.y - from.y) * progress
        )
    }

    /**
     * Calculate arc trajectory (for arrows/rocks)
     */
    fun calculateArcTrajectory(from: Offset, to: Offset, progress: Float, arcHeight: Float = 100f): Offset {
        // Parabolic arc
        val x = from.x + (to.x - from.x) * progress
        val y = from.y + (to.y - from.y) * progress - arcHeight * kotlin.math.sin(progress * Math.PI).toFloat()
        return Offset(x, y)
    }

    /**
     * Calculate shake offset
     */
    fun calculateShakeOffset(intensity: Float, time: Long): Offset {
        // Random shake based on time
        val seed = time / 50 // Change every 50ms
        val random = kotlin.random.Random(seed.toInt())
        return Offset(
            x = random.nextFloat() * intensity * 2 - intensity,
            y = random.nextFloat() * intensity * 2 - intensity
        )
    }
}
