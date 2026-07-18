package com.example.isekaikuroshin.data

import android.util.Log
import com.example.isekaikuroshin.utils.GameLogger
import kotlinx.serialization.Serializable

/**
 * G82: Status Effect Manager (LEGACY)
 * G118: Status Effect System - YENİ MİMARİ (Yarı Katı, Yarı Esnek)
 * G104: Critical logging added
 *
 * KATILMIŞ: Predefined template'ler (30 efekt)
 * ESNEK: AI custom emoji/name override edebilir
 *
 * YENİ MİMARİ (G118):
 * - StatusEffectCategory enum (BUFF, DEBUFF, ENVIRONMENTAL, SPECIAL)
 * - StatusEffectTemplate (30 predefined efekt)
 * - ActiveStatusEffectG118 (AI-generated data)
 * - StatusEffectTemplateRegistry (Singleton pattern)
 *
 * ESKİ MİMARİ (G82 - DEPRECATED):
 * - StatusEffect, ActiveStatusEffect (Turn-based DoT/stat modifiers)
 */

// ==================== YENİ MİMARİ (G118) ====================

// TODO-G118: Status Effect Category Enum
enum class StatusEffectCategory {
    BUFF,           // Olumlu efektler (hız, güç, savunma artışı)
    DEBUFF,         // Olumsuz efektler (zehir, yavaşlama, yorgunluk)
    ENVIRONMENTAL,  // Çevre etkileri (yağmur, kar, sıcaklık)
    SPECIAL         // Özel durumlar (kutsal koruma, lanet)
}

// TODO-G118: Predefined Status Effect Template
data class StatusEffectTemplate(
    val id: String,              // "SPEED_BOOST", "POISON", "RAIN_WET"
    val category: StatusEffectCategory,
    val defaultEmoji: String,    // "⚡", "🤢", "🌧️"
    val defaultNameKey: String,  // Translation key (KURAL 9)
    val stackable: Boolean,      // Aynı efekt üst üste binebilir mi?
    val priority: Int            // Gösterim önceliği (1=en düşük, 10=en yüksek)
)

// TODO-G118: Active Status Effect (AI-Generated + Template)
@Serializable
data class ActiveStatusEffectG118(
    val effectId: String,        // Template ID ile eşleşir (veya custom)
    val customEmoji: String?,    // AI isterse override edebilir
    val customName: String?,     // AI isterse isim override
    val description: String,     // AI tarafından üretilen açıklama
    val duration: Int?,          // Kalan süre (turn/saat, null = kalıcı)
    val intensity: Float         // 0.0-1.0 (zayıf/orta/güçlü)
) {
    /**
     * Efektin gösterilecek emoji'sini döndürür (custom varsa onu, yoksa template'den)
     */
    fun getDisplayEmoji(template: StatusEffectTemplate?): String {
        return customEmoji ?: template?.defaultEmoji ?: "❓"
    }

    /**
     * Efektin gösterilecek ismini döndürür (custom varsa onu, yoksa template'den)
     */
    fun getDisplayName(template: StatusEffectTemplate?): String {
        return customName ?: template?.defaultNameKey ?: effectId
    }
}

/**
 * Status Effect Template Registry - Singleton (G118)
 * Predefined template'leri yönetir ve AI-generated efektleri valide eder
 */
object StatusEffectTemplateRegistry {
    private const val TAG = "StatusEffectRegistry"

    // TODO-G118: Predefined Template Registry (30 efekt)
    private val templates = mapOf(
        // =============== BUFF (Olumlu Efektler) ===============
        "SPEED_BOOST" to StatusEffectTemplate(
            id = "SPEED_BOOST",
            category = StatusEffectCategory.BUFF,
            defaultEmoji = "⚡",
            defaultNameKey = "effect_speed_boost",
            stackable = false,
            priority = 8
        ),
        "STRENGTH_UP" to StatusEffectTemplate(
            id = "STRENGTH_UP",
            category = StatusEffectCategory.BUFF,
            defaultEmoji = "💪",
            defaultNameKey = "effect_strength_up",
            stackable = false,
            priority = 8
        ),
        "DEFENSE_UP" to StatusEffectTemplate(
            id = "DEFENSE_UP",
            category = StatusEffectCategory.BUFF,
            defaultEmoji = "🛡️",
            defaultNameKey = "effect_defense_up",
            stackable = false,
            priority = 8
        ),
        "MANA_REGEN" to StatusEffectTemplate(
            id = "MANA_REGEN",
            category = StatusEffectCategory.BUFF,
            defaultEmoji = "✨",
            defaultNameKey = "effect_mana_regen",
            stackable = true,
            priority = 6
        ),
        "HEALTH_REGEN" to StatusEffectTemplate(
            id = "HEALTH_REGEN",
            category = StatusEffectCategory.BUFF,
            defaultEmoji = "❤️‍🩹",
            defaultNameKey = "effect_health_regen",
            stackable = true,
            priority = 7
        ),
        "LUCKY" to StatusEffectTemplate(
            id = "LUCKY",
            category = StatusEffectCategory.BUFF,
            defaultEmoji = "🍀",
            defaultNameKey = "effect_lucky",
            stackable = false,
            priority = 5
        ),
        "STEALTH" to StatusEffectTemplate(
            id = "STEALTH",
            category = StatusEffectCategory.BUFF,
            defaultEmoji = "👁️‍🗨️",
            defaultNameKey = "effect_stealth",
            stackable = false,
            priority = 7
        ),
        "BERSERK" to StatusEffectTemplate(
            id = "BERSERK",
            category = StatusEffectCategory.BUFF,
            defaultEmoji = "😤",
            defaultNameKey = "effect_berserk",
            stackable = false,
            priority = 9
        ),
        "AGILITY_UP" to StatusEffectTemplate(
            id = "AGILITY_UP",
            category = StatusEffectCategory.BUFF,
            defaultEmoji = "🏃",
            defaultNameKey = "effect_agility_up",
            stackable = false,
            priority = 6
        ),

        // =============== DEBUFF (Olumsuz Efektler) ===============
        "POISON" to StatusEffectTemplate(
            id = "POISON",
            category = StatusEffectCategory.DEBUFF,
            defaultEmoji = "🤢",
            defaultNameKey = "effect_poison",
            stackable = true,
            priority = 9
        ),
        "BLEEDING" to StatusEffectTemplate(
            id = "BLEEDING",
            category = StatusEffectCategory.DEBUFF,
            defaultEmoji = "🩸",
            defaultNameKey = "effect_bleeding",
            stackable = true,
            priority = 9
        ),
        "SLOW" to StatusEffectTemplate(
            id = "SLOW",
            category = StatusEffectCategory.DEBUFF,
            defaultEmoji = "🐌",
            defaultNameKey = "effect_slow",
            stackable = false,
            priority = 7
        ),
        "WEAKENED" to StatusEffectTemplate(
            id = "WEAKENED",
            category = StatusEffectCategory.DEBUFF,
            defaultEmoji = "😓",
            defaultNameKey = "effect_weakened",
            stackable = false,
            priority = 7
        ),
        "CURSED" to StatusEffectTemplate(
            id = "CURSED",
            category = StatusEffectCategory.DEBUFF,
            defaultEmoji = "💀",
            defaultNameKey = "effect_cursed",
            stackable = false,
            priority = 10
        ),
        "BLINDED" to StatusEffectTemplate(
            id = "BLINDED",
            category = StatusEffectCategory.DEBUFF,
            defaultEmoji = "👁️",
            defaultNameKey = "effect_blinded",
            stackable = false,
            priority = 8
        ),
        "SILENCED" to StatusEffectTemplate(
            id = "SILENCED",
            category = StatusEffectCategory.DEBUFF,
            defaultEmoji = "🔇",
            defaultNameKey = "effect_silenced",
            stackable = false,
            priority = 8
        ),
        "EXHAUSTED" to StatusEffectTemplate(
            id = "EXHAUSTED",
            category = StatusEffectCategory.DEBUFF,
            defaultEmoji = "😴",
            defaultNameKey = "effect_exhausted",
            stackable = false,
            priority = 6
        ),
        "BURNING" to StatusEffectTemplate(
            id = "BURNING",
            category = StatusEffectCategory.DEBUFF,
            defaultEmoji = "🔥",
            defaultNameKey = "effect_burning",
            stackable = true,
            priority = 9
        ),
        "FROZEN" to StatusEffectTemplate(
            id = "FROZEN",
            category = StatusEffectCategory.DEBUFF,
            defaultEmoji = "🧊",
            defaultNameKey = "effect_frozen",
            stackable = false,
            priority = 10
        ),
        "STUNNED" to StatusEffectTemplate(
            id = "STUNNED",
            category = StatusEffectCategory.DEBUFF,
            defaultEmoji = "😵‍💫",
            defaultNameKey = "effect_stunned",
            stackable = false,
            priority = 10
        ),

        // =============== ENVIRONMENTAL (Çevre Etkileri) ===============
        "RAIN_WET" to StatusEffectTemplate(
            id = "RAIN_WET",
            category = StatusEffectCategory.ENVIRONMENTAL,
            defaultEmoji = "🌧️",
            defaultNameKey = "effect_rain_wet",
            stackable = false,
            priority = 4
        ),
        "COLD" to StatusEffectTemplate(
            id = "COLD",
            category = StatusEffectCategory.ENVIRONMENTAL,
            defaultEmoji = "❄️",
            defaultNameKey = "effect_cold",
            stackable = false,
            priority = 5
        ),
        "HOT" to StatusEffectTemplate(
            id = "HOT",
            category = StatusEffectCategory.ENVIRONMENTAL,
            defaultEmoji = "🌡️",
            defaultNameKey = "effect_hot",
            stackable = false,
            priority = 5
        ),
        "WINDY" to StatusEffectTemplate(
            id = "WINDY",
            category = StatusEffectCategory.ENVIRONMENTAL,
            defaultEmoji = "🌪️",
            defaultNameKey = "effect_windy",
            stackable = false,
            priority = 3
        ),
        "FOGGY" to StatusEffectTemplate(
            id = "FOGGY",
            category = StatusEffectCategory.ENVIRONMENTAL,
            defaultEmoji = "🌫️",
            defaultNameKey = "effect_foggy",
            stackable = false,
            priority = 3
        ),
        "MUDDY" to StatusEffectTemplate(
            id = "MUDDY",
            category = StatusEffectCategory.ENVIRONMENTAL,
            defaultEmoji = "🟤",
            defaultNameKey = "effect_muddy",
            stackable = false,
            priority = 2
        ),

        // =============== SPECIAL (Özel Durumlar) ===============
        "BLESSED" to StatusEffectTemplate(
            id = "BLESSED",
            category = StatusEffectCategory.SPECIAL,
            defaultEmoji = "🙏",
            defaultNameKey = "effect_blessed",
            stackable = false,
            priority = 9
        ),
        "DEMONIC_PACT" to StatusEffectTemplate(
            id = "DEMONIC_PACT",
            category = StatusEffectCategory.SPECIAL,
            defaultEmoji = "😈",
            defaultNameKey = "effect_demonic_pact",
            stackable = false,
            priority = 10
        ),
        "TRANSFORMATION" to StatusEffectTemplate(
            id = "TRANSFORMATION",
            category = StatusEffectCategory.SPECIAL,
            defaultEmoji = "🦇",
            defaultNameKey = "effect_transformation",
            stackable = false,
            priority = 10
        ),
        "INVISIBLE" to StatusEffectTemplate(
            id = "INVISIBLE",
            category = StatusEffectCategory.SPECIAL,
            defaultEmoji = "👻",
            defaultNameKey = "effect_invisible",
            stackable = false,
            priority = 9
        )
    )

    /**
     * Template ID'den template'i getirir
     */
    fun getTemplate(effectId: String): StatusEffectTemplate? {
        return templates[effectId]
    }

    /**
     * Tüm template'leri category bazlı gruplar
     */
    fun getTemplatesByCategory(category: StatusEffectCategory): List<StatusEffectTemplate> {
        return templates.values.filter { it.category == category }
    }

    /**
     * Aktif efektleri priority'ye göre sıralar (yüksek priority önce)
     */
    fun sortEffectsByPriority(effects: List<ActiveStatusEffectG118>): List<ActiveStatusEffectG118> {
        return effects.sortedByDescending { effect ->
            getTemplate(effect.effectId)?.priority ?: 0
        }
    }

    /**
     * AI'dan gelen efektleri valide eder ve log basar
     */
    fun validateEffect(effect: ActiveStatusEffectG118): Boolean {
        val template = getTemplate(effect.effectId)

        if (template == null) {
            Log.w(TAG, "⚠️ Unknown effect ID: ${effect.effectId} (AI used custom ID)")
            return true // Custom ID'lere izin ver (esnek sistem)
        }

        // Intensity check
        if (effect.intensity < 0f || effect.intensity > 1f) {
            Log.e(TAG, "❌ Invalid intensity for ${effect.effectId}: ${effect.intensity}")
            return false
        }

        Log.d(TAG, "✅ Effect validated: ${effect.effectId} (${template.category})")
        return true
    }

    /**
     * Efektleri maximum 5'e limit'ler (UI overflow önleme)
     */
    fun limitEffectsForDisplay(effects: List<ActiveStatusEffectG118>, maxCount: Int = 5): List<ActiveStatusEffectG118> {
        val sorted = sortEffectsByPriority(effects)
        return sorted.take(maxCount)
    }

    /**
     * Debug: Tüm template'leri listele
     */
    fun logAllTemplates() {
        Log.d(TAG, "=== STATUS EFFECT TEMPLATES (${templates.size}) ===")
        StatusEffectCategory.values().forEach { category ->
            val categoryTemplates = getTemplatesByCategory(category)
            Log.d(TAG, "[$category] ${categoryTemplates.size} effects:")
            categoryTemplates.forEach { template ->
                Log.d(TAG, "  - ${template.id} ${template.defaultEmoji} (priority: ${template.priority})")
            }
        }
    }
}

// ==================== ESKİ MİMARİ (G82 - DEPRECATED) ====================

/**
 * LEGACY Status Effect Manager (G82)
 * Turn-based DoT/stat modifier sistemi (deprecated, G118 kullanılacak)
 */
class StatusEffectManager {

    private val TAG = "StatusEffectManager"

    /**
     * Oyuncuya yeni bir status effect uygula
     *
     * @param effect Uygulanacak effect
     * @param currentTurn Mevcut turn sayısı
     * @param activeEffects Mevcut aktif effectler
     * @return Güncellenmiş effect listesi
     */
    fun applyEffect(
        effect: StatusEffect,
        currentTurn: Int,
        activeEffects: List<ActiveStatusEffect>
    ): List<ActiveStatusEffect> {
        // G104: Critical logging with GameLogger
        GameLogger.logSystem("StatusEffectManager: Applying ${effect.name} (turn $currentTurn)")

        // Aynı effect zaten var mı kontrol et
        val existingEffect = activeEffects.find { it.effect.effectId == effect.effectId }

        return if (existingEffect != null) {
            // Effect zaten var - yenile (süreyi resetle)
            GameLogger.logSystem("StatusEffectManager: Refreshing ${effect.name}")
            activeEffects.map {
                if (it.effect.effectId == effect.effectId) {
                    ActiveStatusEffect(effect, currentTurn)
                } else {
                    it
                }
            }
        } else {
            // Yeni effect ekle
            GameLogger.logSystem("StatusEffectManager: Added ${effect.name} (duration: ${effect.durationTurns} turns)")
            activeEffects + ActiveStatusEffect(effect, currentTurn)
        }
    }

    /**
     * Turn bazlı effect işlemleri (DoT damage, duration check vb.)
     *
     * @param currentTurn Mevcut turn sayısı
     * @param activeEffects Aktif effectler
     * @return (Güncellenmiş effect listesi, Bu turn'deki toplam DoT damage)
     */
    fun processTurnEffects(
        currentTurn: Int,
        activeEffects: List<ActiveStatusEffect>
    ): Pair<List<ActiveStatusEffect>, Int> {
        var totalDotDamage = 0

        // Süresi dolmuş effectleri temizle
        val validEffects = activeEffects.filter { activeEffect ->
            val expired = activeEffect.effect.isExpired(currentTurn, activeEffect.startTurn)
            if (expired) {
                // G104: Critical logging
                GameLogger.logSystem("StatusEffectManager: ${activeEffect.effect.name} expired")
            }
            !expired
        }

        // DoT damage hesapla
        validEffects.forEach { activeEffect ->
            val tickDamage = activeEffect.effect.tickDamage
            if (tickDamage != 0) {
                totalDotDamage += tickDamage
                val damageType = if (tickDamage > 0) "damage" else "healing"
                // G104: Critical logging
                GameLogger.logSystem("StatusEffectManager: ${activeEffect.effect.name} deals $tickDamage $damageType")
            }
        }

        return Pair(validEffects, totalDotDamage)
    }

    /**
     * Belirli bir effecti kaldır
     *
     * @param effectId Kaldırılacak effect ID'si
     * @param activeEffects Mevcut aktif effectler
     * @return Güncellenmiş effect listesi
     */
    fun removeEffect(
        effectId: String,
        activeEffects: List<ActiveStatusEffect>
    ): List<ActiveStatusEffect> {
        // G104: Critical logging
        GameLogger.logSystem("StatusEffectManager: Removing effect $effectId")
        return activeEffects.filter { it.effect.effectId != effectId }
    }

    /**
     * Tüm effectleri temizle
     *
     * @return Boş liste
     */
    fun clearAllEffects(): List<ActiveStatusEffect> {
        // G104: Critical logging
        GameLogger.logSystem("StatusEffectManager: Clearing all effects")
        return emptyList()
    }

    /**
     * Belirli bir türdeki effectleri temizle (BUFF veya DEBUFF)
     *
     * @param type Temizlenecek effect türü
     * @param activeEffects Mevcut aktif effectler
     * @return Güncellenmiş effect listesi
     */
    fun clearEffectsByType(
        type: StatusEffectType,
        activeEffects: List<ActiveStatusEffect>
    ): List<ActiveStatusEffect> {
        // G104: Critical logging
        GameLogger.logSystem("StatusEffectManager: Clearing effects of type $type")
        return activeEffects.filter { it.effect.type != type }
    }

    /**
     * Stat modifikatörlerini hesapla
     *
     * Aktif effectlerden gelen stat bonuslarını/penaltylerini toplar.
     *
     * @param activeEffects Aktif effectler
     * @return Stat ismi -> Toplam modifier (Map)
     */
    fun calculateStatModifiers(
        activeEffects: List<ActiveStatusEffect>
    ): Map<String, Int> {
        val modifiers = mutableMapOf<String, Int>()

        activeEffects.forEach { activeEffect ->
            activeEffect.effect.statModifiers.forEach { (stat, value) ->
                modifiers[stat] = (modifiers[stat] ?: 0) + value
            }
        }

        // G104: Critical logging
        if (modifiers.isNotEmpty()) {
            GameLogger.logSystem("StatusEffectManager: Stat modifiers calculated - $modifiers")
        }

        return modifiers
    }

    /**
     * Effect sayısını döndür (tip bazlı)
     *
     * @param type BUFF veya DEBUFF
     * @param activeEffects Aktif effectler
     * @return Effect sayısı
     */
    fun countEffectsByType(
        type: StatusEffectType,
        activeEffects: List<ActiveStatusEffect>
    ): Int {
        return activeEffects.count { it.effect.type == type }
    }

    /**
     * Belirli bir effect aktif mi?
     *
     * @param effectId Effect ID
     * @param activeEffects Aktif effectler
     * @return true/false
     */
    fun hasEffect(
        effectId: String,
        activeEffects: List<ActiveStatusEffect>
    ): Boolean {
        return activeEffects.any { it.effect.effectId == effectId }
    }

    /**
     * Kalan süreyi al (turn)
     *
     * @param effectId Effect ID
     * @param currentTurn Mevcut turn
     * @param activeEffects Aktif effectler
     * @return Kalan turn sayısı (null = effect yok)
     */
    fun getRemainingTurns(
        effectId: String,
        currentTurn: Int,
        activeEffects: List<ActiveStatusEffect>
    ): Int? {
        val activeEffect = activeEffects.find { it.effect.effectId == effectId }
        return activeEffect?.effect?.remainingTurns(currentTurn, activeEffect.startTurn)
    }

    /**
     * UI için effect özet bilgisi
     *
     * @param activeEffects Aktif effectler
     * @param currentTurn Mevcut turn
     * @return Effect özet listesi
     */
    fun getEffectSummary(
        activeEffects: List<ActiveStatusEffect>,
        currentTurn: Int
    ): List<StatusEffectSummary> {
        return activeEffects.map { activeEffect ->
            StatusEffectSummary(
                effectId = activeEffect.effect.effectId,
                name = activeEffect.effect.name,
                type = activeEffect.effect.type,
                remainingTurns = activeEffect.effect.remainingTurns(currentTurn, activeEffect.startTurn),
                magnitude = activeEffect.effect.magnitude,
                iconPath = null // TODO: Icon path support
            )
        }
    }
}

/**
 * UI için status effect özet bilgisi
 */
data class StatusEffectSummary(
    val effectId: String,
    val name: String,
    val type: StatusEffectType,
    val remainingTurns: Int,
    val magnitude: Int,
    val iconPath: String?
)
