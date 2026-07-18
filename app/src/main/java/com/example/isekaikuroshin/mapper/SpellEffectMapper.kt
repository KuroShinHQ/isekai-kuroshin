package com.example.isekaikuroshin.mapper

import com.example.isekaikuroshin.data.spell.*
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlinx.serialization.Serializable

/**
 * 🎯 SPELL EFFECT MAPPER
 *
 * VFX Workshop kütüphanesindeki CreateParticles() fonksiyonunu kullanarak
 * kullanıcının seçtiği efekt ayarlarını gerçek parçacık parametrelerine dönüştürür.
 *
 * ⚡⚡⚡ KÜTÜPHANE PARAMETRELERİ:
 * CreateParticles(
 *     x, y,                    // Başlangıç pozisyonu
 *     count,                   // Parçacık sayısı
 *     velocityX, velocityY,    // Hız
 *     lifespan,                // Ömür (ms)
 *     size,                    // Boyut
 *     color,                   // Renk (0xAARRGGBB)
 *     gravity,                 // Yerçekimi
 *     spread                   // Yayılma açısı
 * )
 */
object SpellEffectMapper {

    // ========================================
    // ☄️ METEOR YAĞMURU MAPPER
    // ========================================

    /**
     * Meteor Yağmuru efektini oluşturur
     *
     * Analiz: VFXWorkshop'taki `setupMeteorRain()` örneğinden:
     * - Yukarıdan (y=0) düşen parçacıklar
     * - Turuncu/Kırmızı renk (0xFFFF5722)
     * - Yüksek hız (velocityY = 15-25f)
     * - Orta ömür (~1500ms)
     */
    fun mapMeteorRain(action: SpellAction.MeteorRain): ParticleEffectConfig {
        val count = when (action.meteorCount) {
            MeteorCount.FEW -> 5
            MeteorCount.MEDIUM -> 15
            MeteorCount.MANY -> 30
        }

        val velocityY = when (action.fallSpeed) {
            FallSpeed.SLOW -> 8f
            FallSpeed.NORMAL -> 15f
            FallSpeed.FAST -> 25f
        }

        val spread = when (action.effectArea) {
            EffectArea.NARROW -> 30f
            EffectArea.WIDE -> 90f
        }

        return ParticleEffectConfig(
            count = count,
            velocityX = 0f,
            velocityY = velocityY,
            lifespan = 1500L,
            size = 8f,
            color = 0xFFFF5722.toInt(), // Turuncu-Kırmızı (Meteor)
            gravity = 2f,
            spread = spread
        )
    }

    // ========================================
    // ❄️ KAR FIRTINASI MAPPER
    // ========================================

    /**
     * Kar Fırtınası efektini oluşturur
     *
     * Analiz: VFXWorkshop'taki `setupSnowstorm()` örneğinden:
     * - Yavaş düşüş (velocityY = 2-5f)
     * - Beyaz renk (0xFFFFFFFF)
     * - Uzun ömür (~3000ms)
     * - Yatay rüzgar etkisi (velocityX)
     */
    fun mapSnowstorm(action: SpellAction.Snowstorm): ParticleEffectConfig {
        val count = when (action.snowDensity) {
            SnowDensity.LIGHT -> 10
            SnowDensity.NORMAL -> 25
            SnowDensity.BLIZZARD -> 50
        }

        val size = when (action.flakeSize) {
            FlakeSize.SMALL -> 3f
            FlakeSize.NORMAL -> 5f
            FlakeSize.LARGE -> 8f
        }

        val velocityX = when (action.windDirection) {
            WindDirection.NONE -> 0f
            WindDirection.LEFT -> -3f
            WindDirection.RIGHT -> 3f
        }

        return ParticleEffectConfig(
            count = count,
            velocityX = velocityX,
            velocityY = 3f, // Yavaş düşüş
            lifespan = 3000L,
            size = size,
            color = 0xFFFFFFFF.toInt(), // Beyaz
            gravity = 0.5f,
            spread = 60f
        )
    }

    // ========================================
    // 💥 PATLAMA MAPPER
    // ========================================

    /**
     * Patlama efektini oluşturur
     *
     * Analiz: VFXWorkshop'taki `setupExplosion()` örneğinden:
     * - Merkez noktadan dışa doğru patlama
     * - Yüksek hız (velocityX/Y = 10-20f)
     * - Kısa ömür (~800ms)
     * - 360° yayılma
     */
    fun mapExplosion(action: SpellAction.Explosion): ParticleEffectConfig {
        val count = when (action.explosionPower) {
            ExplosionPower.LOW -> 20
            ExplosionPower.MEDIUM -> 50
            ExplosionPower.HIGH -> 100
        }

        val velocity = when (action.explosionPower) {
            ExplosionPower.LOW -> 8f
            ExplosionPower.MEDIUM -> 15f
            ExplosionPower.HIGH -> 25f
        }

        val lifespan = when (action.effectDuration) {
            EffectDuration.SHORT -> 500L
            EffectDuration.LONG -> 1500L
        }

        return ParticleEffectConfig(
            count = count,
            velocityX = velocity,
            velocityY = velocity,
            lifespan = lifespan,
            size = 6f,
            color = 0xFFFF6D00.toInt(), // Turuncu (Patlama)
            gravity = 0f,
            spread = 360f // Tam daire yayılma
        )
    }

    // ========================================
    // 🎆 HAVAİ FİŞEK MAPPER
    // ========================================

    /**
     * Havai Fişek efektini oluşturur
     *
     * Analiz: VFXWorkshop'taki `setupFirework()` örneğinden:
     * - Önce yukarı fırlatma (velocityY = negatif)
     * - Sonra patlama (ikinci aşama - şimdilik basitleştirilmiş)
     * - Renkli parçacıklar (çoklu renk desteği gelecekte)
     */
    fun mapFirework(action: SpellAction.Firework): ParticleEffectConfig {
        val height = when (action.launchHeight) {
            LaunchHeight.LOW -> -10f
            LaunchHeight.MEDIUM -> -18f
            LaunchHeight.HIGH -> -25f
        }

        val burstCount = when (action.burstParticleCount) {
            BurstParticleCount.FEW -> 15
            BurstParticleCount.MEDIUM -> 30
            BurstParticleCount.MANY -> 60
        }

        return ParticleEffectConfig(
            count = burstCount,
            velocityX = 0f,
            velocityY = height, // Yukarı fırlatma
            lifespan = 2000L,
            size = 5f,
            color = 0xFFFFD600.toInt(), // Altın sarısı
            gravity = 3f,
            spread = 360f
        )
    }

    // ========================================
    // ✨ SOLAN AURA MAPPER
    // ========================================

    /**
     * Solan Aura efektini oluşturur
     *
     * Analiz: VFXWorkshop'taki `setupFadingAura()` örneğinden:
     * - Merkez noktadan yavaş yayılma
     * - Düşük hız (velocityX/Y = 1-3f)
     * - Orta ömür (~2000ms)
     * - Mor/Pembe tonları
     */
    fun mapFadingAura(action: SpellAction.FadingAura): ParticleEffectConfig {
        val count = when (action.auraSize) {
            AuraSize.SMALL -> 15
            AuraSize.MEDIUM -> 30
            AuraSize.LARGE -> 50
        }

        val lifespan = when (action.fadeSpeed) {
            FadeSpeed.SLOW -> 3000L
            FadeSpeed.NORMAL -> 2000L
            FadeSpeed.FAST -> 1000L
        }

        return ParticleEffectConfig(
            count = count,
            velocityX = 2f,
            velocityY = 2f,
            lifespan = lifespan,
            size = 6f,
            color = 0xFFB388FF.toInt(), // Mor (Aura)
            gravity = 0f,
            spread = 360f
        )
    }

    // ========================================
    // 💧 TEMEL EFEKT MAPPER (Basitleştirilmiş)
    // ========================================

    /**
     * Parçacık Püskürtme (Temel Efekt)
     */
    fun mapEmitParticles(action: SpellAction.EmitParticles): ParticleEffectConfig {
        return ParticleEffectConfig(
            count = action.particleCount,
            velocityX = 0f,
            velocityY = -8f, // Yukarı püskürme
            lifespan = 1500L,
            size = 5f,
            color = 0xFF00B4D8.toInt(), // Mavi
            gravity = 1f,
            spread = 45f
        )
    }

    /**
     * Parçacık Birleştirme (Çekim Efekti)
     * Not: Bu merkez çekim efekti olduğu için CreateParticles ile değil,
     * mevcut parçacıklara kuvvet uygulayarak çalışır (şimdilik basitleştirilmiş)
     */
    fun mapAttractParticles(action: SpellAction.AttractParticles): ParticleEffectConfig {
        return ParticleEffectConfig(
            count = 20,
            velocityX = 0f,
            velocityY = 0f,
            lifespan = 2000L,
            size = 4f,
            color = 0xFF9D4EDD.toInt(), // Mor
            gravity = 0f,
            spread = 360f
        )
    }

    /**
     * Parçacık Dağıtma (İtme Efekti)
     */
    fun mapRepelParticles(action: SpellAction.RepelParticles): ParticleEffectConfig {
        return ParticleEffectConfig(
            count = 30,
            velocityX = 10f,
            velocityY = 10f,
            lifespan = 1000L,
            size = 4f,
            color = 0xFFFFB800.toInt(), // Sarı
            gravity = 0f,
            spread = 360f
        )
    }
}

/**
 * 🎨 PARTICLE EFFECT CONFIG
 *
 * CreateParticles() fonksiyonuna geçilecek parametreleri taşıyan data class.
 * Bu sayede mapper'dan gelen sonuçları kolayca VFX kütüphanesine aktarabiliriz.
 */
@Serializable
data class ParticleEffectConfig(
    val count: Int,
    val velocityX: Float,
    val velocityY: Float,
    val lifespan: Long,
    val size: Float,
    val color: Int, // 0xAARRGGBB formatında
    val gravity: Float,
    val spread: Float
)

/**
 * 🎯 EXTENSION FUNCTIONS
 *
 * SpellAction'ı doğrudan ParticleEffectConfig'e dönüştürmek için kolaylık fonksiyonları
 */
fun SpellAction.toParticleEffectConfig(): ParticleEffectConfig {
    return when (this) {
        is SpellAction.MeteorRain -> SpellEffectMapper.mapMeteorRain(this)
        is SpellAction.Snowstorm -> SpellEffectMapper.mapSnowstorm(this)
        is SpellAction.Explosion -> SpellEffectMapper.mapExplosion(this)
        is SpellAction.Firework -> SpellEffectMapper.mapFirework(this)
        is SpellAction.FadingAura -> SpellEffectMapper.mapFadingAura(this)
        is SpellAction.EmitParticles -> SpellEffectMapper.mapEmitParticles(this)
        is SpellAction.AttractParticles -> SpellEffectMapper.mapAttractParticles(this)
        is SpellAction.RepelParticles -> SpellEffectMapper.mapRepelParticles(this)

        // Diğer efektler (henüz implemente edilmemiş)
        is SpellAction.DirectParticles -> ParticleEffectConfig(
            count = 10, velocityX = 5f, velocityY = 5f,
            lifespan = 1000L, size = 4f, color = 0xFFFFFFFF.toInt(),
            gravity = 0f, spread = 30f
        )
        is SpellAction.VortexParticles -> ParticleEffectConfig(
            count = 40, velocityX = 0f, velocityY = 0f,
            lifespan = 2000L, size = 4f, color = 0xFFFFFFFF.toInt(),
            gravity = 0f, spread = 360f
        )
        is SpellAction.ClearAllParticles -> ParticleEffectConfig(
            count = 0, velocityX = 0f, velocityY = 0f,
            lifespan = 0L, size = 0f, color = 0,
            gravity = 0f, spread = 0f
        )
        is SpellAction.ChangeParticleColor -> ParticleEffectConfig(
            count = 0, velocityX = 0f, velocityY = 0f,
            lifespan = 0L, size = 0f, color = this.newColor.hashCode(),
            gravity = 0f, spread = 0f
        )
    }
}
