package com.example.isekaikuroshin.engine.vfx

import android.util.Log
import androidx.compose.ui.geometry.Offset
import com.example.isekaikuroshin.data.spell.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Büyü Eylem Yürütücü (BASİTLEŞTİRİLDİ - G89)
 *
 * SpellAction'ları basit parçacık sistemine dönüştürür.
 * Quarks kütüphanesi kaldırıldı.
 */
@Singleton
class SpellActionExecutor @Inject constructor(
    val particleManager: ParticleSystemManager  // ⚡ Public - ParticleRenderer için gerekli
) {

    companion object {
        private const val TAG = "SpellActionExecutor"
    }

    /**
     * Bir büyü eylemini çalıştır
     */
    fun executeAction(action: SpellAction, context: ActionContext = ActionContext()) {
        try {
            // 🔄 Processing
            Log.d(TAG, "🔄 Processing: ${action::class.simpleName}")
            Log.d(TAG, "   HandPos: ${context.handPosition}, ScreenSize: ${context.screenSize}")

            when (action) {
                is SpellAction.EmitParticles -> executeEmitParticles(action, context)
                is SpellAction.AttractParticles -> executeAttractParticles(action, context)
                is SpellAction.RepelParticles -> executeRepelParticles(action, context)
                is SpellAction.DirectParticles -> executeDirectParticles(action, context)
                is SpellAction.VortexParticles -> executeVortexParticles(action, context)
                is SpellAction.ClearAllParticles -> executeClearAll()
                is SpellAction.ChangeParticleColor -> executeChangeColor(action, context)
                // 🌟 Gelişmiş Efekt Şablonları
                is SpellAction.MeteorRain -> executeMeteorRain(action, context)
                is SpellAction.Snowstorm -> executeSnowstorm(action, context)
                is SpellAction.Explosion -> executeExplosion(action, context)
                is SpellAction.Firework -> executeFirework(action, context)
                is SpellAction.FadingAura -> executeFadingAura(action, context)
            }

            // ✅ Success
            Log.d(TAG, "✅ Success: ${action::class.simpleName} executed")
        } catch (e: Exception) {
            // ❌ Error
            Log.e(TAG, "❌ Error executing ${action::class.simpleName}: ${e.message}", e)
        }
    }

    /**
     * Parçacık püskürtme (BASİTLEŞTİRİLDİ - G89)
     */
    private fun executeEmitParticles(action: SpellAction.EmitParticles, context: ActionContext) {
        val spawnPosition = context.handPosition ?: action.spawnPosition.toOffset()

        val emitterConfig = EmitterConfig(
            id = "emitter_${System.currentTimeMillis()}",
            position = spawnPosition,
            particleColor = action.particleColor.toColor()
        )

        particleManager.emitParticles(emitterConfig)
        Log.d(TAG, "Emitted particles at $spawnPosition")
    }

    /**
     * Parçacıkları çekme (Attractor)
     */
    private fun executeAttractParticles(action: SpellAction.AttractParticles, context: ActionContext) {
        val attractorPosition = context.handPosition ?: action.attractorPosition.toOffset()

        val forceConfig = ForceConfig(
            id = "attractor_${System.currentTimeMillis()}",
            type = ForceType.Attractor,
            strength = action.strength,
            position = attractorPosition,
            radius = action.radius
        )

        particleManager.applyForce(forceConfig)
        Log.d(TAG, "Applied attractor force at $attractorPosition")
    }

    /**
     * Parçacıkları itme (Repulsor)
     */
    private fun executeRepelParticles(action: SpellAction.RepelParticles, context: ActionContext) {
        val repulsorPosition = context.handPosition ?: action.repulsorPosition.toOffset()

        val forceConfig = ForceConfig(
            id = "repulsor_${System.currentTimeMillis()}",
            type = ForceType.Repulsor,
            strength = action.strength,
            position = repulsorPosition,
            radius = action.radius
        )

        particleManager.applyForce(forceConfig)
        Log.d(TAG, "Applied repulsor force at $repulsorPosition")
    }

    /**
     * Parçacıkları yönlendirme
     */
    private fun executeDirectParticles(action: SpellAction.DirectParticles, context: ActionContext) {
        val forceConfig = ForceConfig(
            id = "directional_${System.currentTimeMillis()}",
            type = ForceType.Directional(action.direction.toOffset()),
            strength = action.strength
        )

        particleManager.applyForce(forceConfig)
        Log.d(TAG, "Applied directional force: ${action.direction}")
    }

    /**
     * Parçacıkları döndürme (Vortex)
     */
    private fun executeVortexParticles(action: SpellAction.VortexParticles, context: ActionContext) {
        val vortexCenter = context.handPosition ?: action.center.toOffset()

        val forceConfig = ForceConfig(
            id = "vortex_${System.currentTimeMillis()}",
            type = ForceType.Vortex(action.clockwise),
            strength = action.strength,
            position = vortexCenter,
            radius = action.radius
        )

        particleManager.applyForce(forceConfig)
        Log.d(TAG, "Applied vortex force at $vortexCenter")
    }

    /**
     * Tüm parçacıkları temizle
     */
    private fun executeClearAll() {
        particleManager.clearAll()
        Log.d(TAG, "Cleared all particles")
    }

    /**
     * Parçacık rengini değiştir
     */
    private fun executeChangeColor(action: SpellAction.ChangeParticleColor, context: ActionContext) {
        // TODO: Mevcut parçacıkların rengini değiştirme
        // Bu, Quarks kütüphanesinin desteklemediği bir özellik olabilir
        // Geçici çözüm: Yeni renkle yeni parçacıklar oluştur
        Log.d(TAG, "Color change requested: ${action.newColor} (not fully implemented)")
    }

    // ========================================
    // 🌟 GELİŞMİŞ EFEKT ŞABLONLARI
    // ========================================

    /**
     * ☄️ Meteor Yağmuru - Gökyüzünden düşen ateş topları (BASİTLEŞTİRİLDİ - G89)
     */
    private fun executeMeteorRain(action: SpellAction.MeteorRain, context: ActionContext) {
        val screenSize = context.screenSize ?: Offset(1080f, 2400f)
        val meteorCount = action.meteorCount.count

        repeat(meteorCount) { index ->
            val startX = (screenSize.x * index / meteorCount) + (Math.random() * 100).toFloat()
            val startY = -50f

            val emitterConfig = EmitterConfig(
                id = "meteor_${System.currentTimeMillis()}_$index",
                position = Offset(startX, startY),
                particleColor = action.fireColor.toColor()
            )

            particleManager.emitParticles(emitterConfig)
        }

        Log.d(TAG, "Meteor rain executed: $meteorCount meteors")
    }

    /**
     * ❄️ Kar Fırtınası - Yavaş düşen kar taneleri (BASİTLEŞTİRİLDİ - G89)
     */
    private fun executeSnowstorm(action: SpellAction.Snowstorm, context: ActionContext) {
        val screenSize = context.screenSize ?: Offset(1080f, 2400f)

        val emitterConfig = EmitterConfig(
            id = "snowstorm_${System.currentTimeMillis()}",
            position = Offset(screenSize.x / 2, -50f),
            particleColor = androidx.compose.ui.graphics.Color.White
        )

        particleManager.emitParticles(emitterConfig)
        Log.d(TAG, "Snowstorm executed")
    }

    /**
     * 💥 Patlama - Merkezden dışa doğru patlayan parçacıklar (BASİTLEŞTİRİLDİ - G89)
     */
    private fun executeExplosion(action: SpellAction.Explosion, context: ActionContext) {
        val explosionCenter = context.handPosition ?: Offset(540f, 1200f)

        Log.e("ActionExecutor", "💥💥💥 EXPLOSION! Center: $explosionCenter")

        val emitterConfig = EmitterConfig(
            id = "explosion_${System.currentTimeMillis()}",
            position = explosionCenter,
            particleColor = action.particleColor.toColor()
        )

        particleManager.emitParticles(emitterConfig)
        Log.d(TAG, "Explosion executed at $explosionCenter")
    }

    /**
     * 🎆 Havai Fişek - Yukarı fırlayıp patlayan renkli efekt (BASİTLEŞTİRİLDİ - G89)
     */
    private fun executeFirework(action: SpellAction.Firework, context: ActionContext) {
        val launchPosition = context.handPosition ?: Offset(540f, 2000f)
        val burstPosition = Offset(
            launchPosition.x,
            launchPosition.y - action.launchHeight.height
        )

        // İz parçacıkları
        if (action.trailEnabled) {
            val trailConfig = EmitterConfig(
                id = "firework_trail_${System.currentTimeMillis()}",
                position = launchPosition,
                particleColor = action.burstColor.toColor().copy(alpha = 0.5f)
            )
            particleManager.emitParticles(trailConfig)
        }

        // Ana patlama
        repeat(action.cascadeCount) { cascade ->
            val burstConfig = EmitterConfig(
                id = "firework_burst_${System.currentTimeMillis()}_$cascade",
                position = burstPosition,
                particleColor = action.burstColor.toColor()
            )
            particleManager.emitParticles(burstConfig)
        }

        Log.d(TAG, "Firework executed: ${action.cascadeCount} bursts")
    }

    /**
     * ✨ Solan Aura - Yavaşça kaybolan etrafı saran enerji (BASİTLEŞTİRİLDİ - G89)
     */
    private fun executeFadingAura(action: SpellAction.FadingAura, context: ActionContext) {
        val auraCenter = context.handPosition ?: Offset(540f, 1200f)

        val emitterConfig = EmitterConfig(
            id = "aura_${System.currentTimeMillis()}",
            position = auraCenter,
            particleColor = action.auraColor.toColor().copy(alpha = 0.6f)
        )

        particleManager.emitParticles(emitterConfig)
        Log.d(TAG, "Fading aura executed at $auraCenter")
    }

    /**
     * Bir büyü tarifini sırayla çalıştır (test modu için)
     */
    suspend fun executeSpellRecipe(
        recipe: SpellRecipe,
        contextProvider: suspend (SpellStep) -> ActionContext
    ) {
        Log.d(TAG, "Executing spell recipe: ${recipe.name} (${recipe.steps.size} steps)")

        for (step in recipe.steps) {
            // Tetikleyiciyi bekle
            // TODO: Gerçek tetikleyici algılaması
            Log.d(TAG, "Waiting for trigger: ${step.trigger::class.simpleName}")

            // Context al ve eylemi çalıştır
            val context = contextProvider(step)
            executeAction(step.action, context)
        }

        Log.d(TAG, "Spell recipe execution completed: ${recipe.name}")
    }
}

/**
 * Eylem çalıştırma bağlamı
 *
 * Çalışma zamanında dinamik bilgiler (el pozisyonu vb.) içerir
 */
data class ActionContext(
    val handPosition: Offset? = null,
    val leftHandPosition: Offset? = null,
    val rightHandPosition: Offset? = null,
    val screenSize: Offset? = null,
    val timestamp: Long = System.currentTimeMillis()
)
