package com.example.isekaikuroshin.engine.vfx

import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parçacık Sistemi Yöneticisi (BASİTLEŞTİRİLDİ - G89)
 *
 * Basit parçacık sistemi için yönetici.
 * Büyü Tasarım Stüdyosu'nda kullanıcının oluşturduğu eylemler,
 * bu manager üzerinden parçacık sistemini kontrol eder.
 */
@Singleton
class ParticleSystemManager @Inject constructor() {

    // Aktif parçacık konfigürasyonu
    private val _activeEmitters = MutableStateFlow<List<EmitterConfig>>(emptyList())
    val activeEmitters: StateFlow<List<EmitterConfig>> = _activeEmitters.asStateFlow()

    // Aktif kuvvetler
    private val _activeForces = MutableStateFlow<List<ForceConfig>>(emptyList())
    val activeForces: StateFlow<List<ForceConfig>> = _activeForces.asStateFlow()

    /**
     * Belirtilen ayarlar ile parçacık püskürtür
     */
    fun emitParticles(config: EmitterConfig) {
        Log.e("ParticleSystemMgr", "🎆🎆🎆 EMIT PARTICLES CALLED!")
        Log.e("ParticleSystemMgr", "   ID: ${config.id}")
        Log.e("ParticleSystemMgr", "   Position: ${config.position}")
        Log.e("ParticleSystemMgr", "   Color: ${config.particleColor}")

        val currentEmitters = _activeEmitters.value.toMutableList()
        currentEmitters.add(config)
        _activeEmitters.value = currentEmitters

        Log.e("ParticleSystemMgr", "✅ Emitter added! Total active emitters: ${currentEmitters.size}")
        Log.e("ParticleSystemMgr", "📊 StateFlow value updated: ${_activeEmitters.value.size} emitters")
    }

    /**
     * Mevcut parçacıklara bir kuvvet (çekim, itme vb.) uygular
     */
    fun applyForce(force: ForceConfig) {
        val currentForces = _activeForces.value.toMutableList()
        currentForces.add(force)
        _activeForces.value = currentForces
    }

    /**
     * Belirli bir emitter'ı durdurur
     */
    fun stopEmitter(emitterId: String) {
        _activeEmitters.value = _activeEmitters.value.filter { it.id != emitterId }
    }

    /**
     * Belirli bir kuvveti kaldırır
     */
    fun removeForce(forceId: String) {
        _activeForces.value = _activeForces.value.filter { it.id != forceId }
    }

    /**
     * Tüm parçacıkları ve kuvvetleri temizler
     */
    fun clearAll() {
        _activeEmitters.value = emptyList()
        _activeForces.value = emptyList()
    }

    /**
     * Tüm emitter'ları durdur (ama kuvvetleri koru)
     */
    fun stopAllEmitters() {
        _activeEmitters.value = emptyList()
    }
}

/**
 * Parçacık Püskürtücü Konfigürasyonu (BASİTLEŞTİRİLDİ - G89)
 * Artık sadece SimpleParticle için gerekli alanlar
 */
data class EmitterConfig(
    val id: String,
    val position: Offset,
    val particleColor: Color = Color.White
)

/**
 * Kuvvet Konfigürasyonu
 */
data class ForceConfig(
    val id: String,
    val type: ForceType,
    val strength: Float = 1f,
    val position: Offset = Offset.Zero, // Çekim/İtme merkezi
    val radius: Float = 300f // Etki yarıçapı
)

/**
 * Kuvvet Türleri
 */
sealed class ForceType {
    /**
     * Çekim kuvveti - parçacıkları bir noktaya çeker (Attractor)
     */
    data object Attractor : ForceType()

    /**
     * İtme kuvveti - parçacıkları bir noktadan uzaklaştırır (Repulsor)
     */
    data object Repulsor : ForceType()

    /**
     * Yönlü kuvvet - parçacıkları belirli bir yönde hareket ettirir
     */
    data class Directional(val direction: Offset) : ForceType()

    /**
     * Girdap kuvveti - parçacıkları döndürür (Vortex)
     */
    data class Vortex(val clockwise: Boolean = true) : ForceType()
}
