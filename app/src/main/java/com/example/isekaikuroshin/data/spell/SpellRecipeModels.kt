package com.example.isekaikuroshin.data.spell

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.isekaikuroshin.engine.vfx.EmitterConfig
import com.example.isekaikuroshin.engine.vfx.ForceConfig
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * G66 FIX: Serializable wrapper for Compose Color
 * Stores ARGB as ULong for JSON serialization
 */
@Serializable
data class SerializableColor(val argb: ULong) {
    fun toColor(): Color = Color(argb)

    companion object {
        fun fromColor(color: Color) = SerializableColor(color.value)

        val White = fromColor(Color.White)
        val Red = fromColor(Color.Red)
        val Green = fromColor(Color.Green)
        val Blue = fromColor(Color.Blue)
    }
}

/**
 * G66 FIX: Serializable wrapper for Compose Offset
 * Stores x, y as Floats for JSON serialization
 */
@Serializable
data class SerializableOffset(val x: Float, val y: Float) {
    fun toOffset(): Offset = Offset(x, y)

    companion object {
        fun fromOffset(offset: Offset) = SerializableOffset(offset.x, offset.y)

        val Zero = SerializableOffset(0f, 0f)
    }
}

/**
 * Büyü Tarifi (Spell Recipe)
 *
 * Kullanıcının oluşturduğu çok adımlı büyü tarifi.
 * Her adım bir tetikleyici (trigger) ve bir eylem (action) içerir.
 *
 * FAZ 8.1: "Potansiyel Sistemi" - Her büyünün doğum kartı
 */
@Serializable
data class SpellRecipe(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val steps: List<SpellStep> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // FAZ 8.1: Potansiyel Sistemi
    /**
     * Büyünün ham gücü (1-100)
     * Spell Studio'da otomatik hesaplanır (step sayısı, action complexity)
     */
    val basePower: Int = 10,

    /**
     * Gereken Mana (MP) maliyeti
     * Otomatik: basePower * complexity * 1.5
     */
    val manaCost: Int = 15,

    /**
     * Büyü kompleksitesi/zorluk (0.1-1.0)
     * Düşük = Öğrenmesi kolay, yavaş gelişir
     * Yüksek = Öğrenmesi zor, hızlı gelişir
     * Otomatik: step sayısı + trigger/action kombinasyonu
     */
    val complexity: Float = 0.5f
)

/**
 * Büyü Adımı (Spell Step)
 *
 * Tek bir tetikleyici + eylem kombinasyonu
 */
@Serializable
data class SpellStep(
    val id: String = UUID.randomUUID().toString(),
    val stepNumber: Int,
    val trigger: SpellTrigger,
    val action: SpellAction
)

/**
 * Büyü Tetikleyicisi (Spell Trigger)
 *
 * Kullanıcının büyüyü nasıl tetikleyeceği
 */
@Serializable
sealed class SpellTrigger {
    /**
     * Tek el hareketi tetikleyicisi
     */
    data class SingleHandGesture(
        val gestureName: String,
        val gestureData: String // JSON formatında kaydedilmiş jest verisi
    ) : SpellTrigger()

    /**
     * Çift el hareketi tetikleyicisi
     */
    data class DoubleHandGesture(
        val leftGestureName: String,
        val rightGestureName: String,
        val leftGestureData: String,
        val rightGestureData: String
    ) : SpellTrigger()

    /**
     * Sesli komut tetikleyicisi (GÖREV E: Revize)
     * @param command Kullanıcının kaydettiği sihirli sözcük (örn: "Lumos Maxima!")
     * @param sensitivity Eşleşme hassasiyeti 0.0 (çok esnek) - 1.0 (çok hassas)
     * @param language Ses tanıma dili
     */
    data class VoiceCommand(
        val command: String,
        val sensitivity: Float = 0.80f, // %80 varsayılan
        val language: String = "tr-TR"
    ) : SpellTrigger()

    /**
     * Zamanlayıcı tetikleyicisi (opsiyonel, gelecekte eklenebilir)
     */
    data class Timer(
        val delayMs: Long
    ) : SpellTrigger()
}

/**
 * Büyü Eylemi (Spell Action)
 *
 * Tetikleyici gerçekleştiğinde ne olacağı
 */
@Serializable
sealed class SpellAction {
    /**
     * Parçacık püskürtme eylemi
     */
    data class EmitParticles(
        val particleCount: Int = 100,
        val particleColor: SerializableColor = SerializableColor.White,
        val spawnPosition: SerializableOffset = SerializableOffset.Zero,
        val velocity: ParticleVelocityConfig = ParticleVelocityConfig.Stationary,
        val lifeTime: Long = 5000L,
        val emissionRate: Float = 20f,
        val particleSizeMin: Float = 2f,
        val particleSizeMax: Float = 8f,
        val maxParticles: Int = 500  // ✅ MISSING PARAMETER FIX
    ) : SpellAction()

    /**
     * Parçacıkları birleştirme eylemi (Attractor)
     */
    data class AttractParticles(
        val attractorPosition: SerializableOffset = SerializableOffset.Zero,
        val strength: Float = 1f,
        val radius: Float = 300f
    ) : SpellAction()

    /**
     * Parçacıkları dağıtma eylemi (Repulsor)
     */
    data class RepelParticles(
        val repulsorPosition: SerializableOffset = SerializableOffset.Zero,
        val strength: Float = 1f,
        val radius: Float = 300f
    ) : SpellAction()

    /**
     * Parçacıkları yönlendirme eylemi
     */
    data class DirectParticles(
        val direction: SerializableOffset,
        val strength: Float = 1f
    ) : SpellAction()

    /**
     * Parçacıkları döndürme eylemi (Vortex)
     */
    data class VortexParticles(
        val center: SerializableOffset = SerializableOffset.Zero,
        val strength: Float = 1f,
        val radius: Float = 300f,
        val clockwise: Boolean = true
    ) : SpellAction()

    /**
     * Tüm parçacıkları temizleme eylemi
     */
    data object ClearAllParticles : SpellAction()

    /**
     * Parçacık rengini değiştirme eylemi
     */
    data class ChangeParticleColor(
        val newColor: SerializableColor
    ) : SpellAction()

    // ========================================
    // 🌟 GELİŞMİŞ EFEKT ŞABLONLARI
    // ========================================

    /**
     * ☄️ Meteor Yağmuru Şablonu
     * Gökyüzünden düşen ateş topları efekti
     */
    data class MeteorRain(
        val meteorCount: MeteorCount = MeteorCount.MEDIUM,
        val fallSpeed: FallSpeed = FallSpeed.NORMAL,
        val fireColor: SerializableColor = SerializableColor(0xFFFF6B00u),
        val effectArea: EffectArea = EffectArea.WIDE,
        val duration: Long = 3000L
    ) : SpellAction()

    /**
     * ❄️ Kar Fırtınası Şablonu
     * Yavaş yavaş düşen kar taneleri efekti
     */
    data class Snowstorm(
        val snowDensity: SnowDensity = SnowDensity.NORMAL,
        val flakeSize: FlakeSize = FlakeSize.NORMAL,
        val windDirection: WindDirection = WindDirection.NONE,
        val duration: Long = 5000L
    ) : SpellAction()

    /**
     * 💥 Patlama Şablonu
     * Merkezden dışa doğru patlayan parçacıklar
     */
    data class Explosion(
        val explosionPower: ExplosionPower = ExplosionPower.MEDIUM,
        val particleColor: SerializableColor = SerializableColor(0xFFFF4444u),
        val effectDuration: EffectDuration = EffectDuration.SHORT,
        val shockwaveEnabled: Boolean = true
    ) : SpellAction()

    /**
     * 🎆 Havai Fişek Şablonu
     * Yukarı fırlayıp patlayan renkli efekt
     */
    data class Firework(
        val launchHeight: LaunchHeight = LaunchHeight.HIGH,
        val burstColor: SerializableColor = SerializableColor(0xFFFFD700u),
        val burstParticleCount: BurstParticleCount = BurstParticleCount.MEDIUM,
        val trailEnabled: Boolean = true,
        val cascadeCount: Int = 1 // Kaç kere art arda patlayacağı
    ) : SpellAction()

    /**
     * ✨ Solan Aura Şablonu
     * Yavaşça kaybolan etrafı saran enerji
     */
    data class FadingAura(
        val auraColor: SerializableColor = SerializableColor(0xFF9C27B0u),
        val auraSize: AuraSize = AuraSize.MEDIUM,
        val fadeSpeed: FadeSpeed = FadeSpeed.NORMAL,
        val pulseEnabled: Boolean = true,
        val duration: Long = 4000L
    ) : SpellAction()
}

// ========================================
// 🎨 EFEKT ŞABLONLARı İÇİN PARAMETRE ENUMLARı
// ========================================

/**
 * Meteor sayısı seçenekleri
 */
@Serializable
enum class MeteorCount(val count: Int, val displayName: String) {
    FEW(5, "Az (5)"),
    MEDIUM(15, "Orta (15)"),
    MANY(30, "Çok (30)")
}

/**
 * Düşme hızı seçenekleri
 */
@Serializable
enum class FallSpeed(val speed: Float, val displayName: String) {
    SLOW(100f, "Yavaş"),
    NORMAL(200f, "Normal"),
    FAST(400f, "Hızlı")
}

/**
 * Etki alanı seçenekleri
 */
@Serializable
enum class EffectArea(val radius: Float, val displayName: String) {
    NARROW(150f, "Dar"),
    WIDE(300f, "Geniş")
}

/**
 * Kar yoğunluğu seçenekleri
 */
@Serializable
enum class SnowDensity(val particlesPerSecond: Float, val displayName: String) {
    LIGHT(20f, "Hafif"),
    NORMAL(50f, "Normal"),
    BLIZZARD(100f, "Tipi")
}

/**
 * Kar tanesi büyüklüğü
 */
@Serializable
enum class FlakeSize(val size: Float, val displayName: String) {
    SMALL(3f, "Küçük"),
    NORMAL(6f, "Normal"),
    LARGE(10f, "Büyük")
}

/**
 * Rüzgar yönü
 */
@Serializable
enum class WindDirection(val displayName: String) {
    NONE("Yok"),
    LEFT("Soldan"),
    RIGHT("Sağdan")
}

/**
 * Patlama gücü
 */
@Serializable
enum class ExplosionPower(val force: Float, val particleCount: Int, val displayName: String) {
    LOW(300f, 50, "Düşük"),
    MEDIUM(600f, 100, "Orta"),
    HIGH(1000f, 200, "Yüksek")
}

/**
 * Efekt süresi
 */
@Serializable
enum class EffectDuration(val durationMs: Long, val displayName: String) {
    SHORT(500L, "Kısa"),
    LONG(1500L, "Uzun")
}

/**
 * Fırlatma yüksekliği
 */
@Serializable
enum class LaunchHeight(val height: Float, val displayName: String) {
    LOW(200f, "Alçak"),
    MEDIUM(400f, "Orta"),
    HIGH(600f, "Yüksek")
}

/**
 * Patlama parçacık sayısı
 */
@Serializable
enum class BurstParticleCount(val count: Int, val displayName: String) {
    FEW(30, "Az"),
    MEDIUM(60, "Orta"),
    MANY(120, "Çok")
}

/**
 * Aura büyüklüğü
 */
@Serializable
enum class AuraSize(val radius: Float, val displayName: String) {
    SMALL(100f, "Küçük"),
    MEDIUM(200f, "Orta"),
    LARGE(350f, "Büyük")
}

/**
 * Solma hızı
 */
@Serializable
enum class FadeSpeed(val alphaDecreaseRate: Float, val displayName: String) {
    SLOW(0.3f, "Yavaş"),
    NORMAL(0.5f, "Normal"),
    FAST(0.8f, "Hızlı")
}

/**
 * Parçacık hız konfigürasyonu
 */
@Serializable
sealed class ParticleVelocityConfig {
    @Serializable
    data object Stationary : ParticleVelocityConfig()
    @Serializable
    data class Fixed(val velocityX: Float, val velocityY: Float) : ParticleVelocityConfig()
    @Serializable
    data class Random(val minVelocity: Float, val maxVelocity: Float) : ParticleVelocityConfig()
    @Serializable
    data class Radial(val speed: Float) : ParticleVelocityConfig()
}

/**
 * Büyü kategorisi (gelecekte filtreleme için)
 */
@Serializable
enum class SpellCategory {
    OFFENSIVE,    // Saldırı büyüleri
    DEFENSIVE,    // Savunma büyüleri
    UTILITY,      // Yardımcı büyüler
    HEALING,      // İyileştirme büyüleri
    SUMMONING,    // Çağırma büyüleri
    ELEMENTAL,    // Element büyüleri
    CUSTOM        // Özel/Deneysel
}

/**
 * Büyü şablonu (önceden tanımlı büyüler için)
 *
 * ⚠️ NOT: thumbnailResourceId Int olarak kaydedilir ama runtime'da kullanılmalıdır
 */
@Serializable
data class SpellTemplate(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val category: SpellCategory,
    val thumbnailResourceId: Int? = null,
    val recipe: SpellRecipe
)
