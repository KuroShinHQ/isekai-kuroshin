package com.example.isekaikuroshin.data

import kotlinx.serialization.Serializable

/**
 * Element System - Ana ve Hibrit Elementler
 * GM tarafından dinamik olarak hibrit elementler üretilebilir
 */
@Serializable
enum class ElementType(
    val displayName: String,
    val isPrimary: Boolean = false,  // Ana element mi?
    val parents: List<ElementType> = emptyList()  // Hibrit ise hangi elementlerden oluşur
) {
    // ========== ANA ELEMENTLER (Primary Elements) ==========
    FIRE("Ateş", isPrimary = true),
    WATER("Su", isPrimary = true),
    EARTH("Toprak", isPrimary = true),
    AIR("Hava", isPrimary = true),
    LIGHTNING("Yıldırım", isPrimary = true),
    ICE("Buz", isPrimary = true),
    LIGHT("Işık", isPrimary = true),
    DARK("Karanlık", isPrimary = true),
    NATURE("Doğa", isPrimary = true),
    METAL("Metal", isPrimary = true),

    // ========== HİBRİT ELEMENTLER (Hybrid Elements) ==========
    // GM tarafından üretilebilir, kullanıcının hikayesine göre dinamik
    MAGMA("Magma", parents = listOf(FIRE, EARTH)),          // Ateş + Toprak
    STEAM("Buhar", parents = listOf(FIRE, WATER)),          // Ateş + Su (zıt elementler)
    PLASMA("Plazma", parents = listOf(FIRE, AIR)),          // Ateş + Hava
    STORM("Fırtına", parents = listOf(AIR, LIGHTNING)),     // Hava + Yıldırım
    CRYSTAL("Kristal", parents = listOf(EARTH, ICE)),       // Toprak + Buz
    MIST("Sis", parents = listOf(WATER, AIR)),              // Su + Hava
    MUD("Çamur", parents = listOf(WATER, EARTH)),           // Su + Toprak
    THUNDER("Gök Gürültüsü", parents = listOf(AIR, LIGHT)), // Hava + Işık
    SHADOW("Gölge", parents = listOf(DARK, AIR)),           // Karanlık + Hava
    BLOOD("Kan", parents = listOf(WATER, DARK)),            // Su + Karanlık
    POISON("Zehir", parents = listOf(NATURE, DARK)),        // Doğa + Karanlık
    LAVA("Lav", parents = listOf(FIRE, METAL)),             // Ateş + Metal

    // ========== ÖZEL ELEMENTLER (GM Dinamik Üretimi için placeholder) ==========
    GM_GENERATED_1("Özel Element 1"),  // GM tarafından hikayeye göre üretilir
    GM_GENERATED_2("Özel Element 2"),
    GM_GENERATED_3("Özel Element 3"),

    // ========== NEUTRAL ==========
    NEUTRAL("Nötr");  // Element yok

    companion object {
        /**
         * İki elementin birleşiminden hangi hibrit element oluşur?
         */
        fun getCombinedElement(element1: ElementType, element2: ElementType): ElementType? {
            val sorted = listOf(element1, element2).sortedBy { it.name }
            return values().find {
                it.parents.isNotEmpty() &&
                it.parents.sortedBy { p -> p.name } == sorted
            }
        }

        /**
         * Element çarpışması kontrolü (zıt elementler mi?)
         */
        fun areOpposite(element1: ElementType, element2: ElementType): Boolean {
            return when {
                element1 == FIRE && element2 == WATER -> true
                element1 == WATER && element2 == FIRE -> true
                element1 == LIGHT && element2 == DARK -> true
                element1 == DARK && element2 == LIGHT -> true
                element1 == ICE && element2 == FIRE -> true
                element1 == FIRE && element2 == ICE -> true
                else -> false
            }
        }

        /**
         * Element uyumluluğu (güçlendirici kombinasyon mu?)
         */
        fun areCompatible(element1: ElementType, element2: ElementType): Boolean {
            return when {
                element1 == FIRE && element2 == AIR -> true
                element1 == AIR && element2 == FIRE -> true
                element1 == WATER && element2 == ICE -> true
                element1 == ICE && element2 == WATER -> true
                element1 == EARTH && element2 == NATURE -> true
                element1 == NATURE && element2 == EARTH -> true
                element1 == LIGHTNING && element2 == AIR -> true
                element1 == AIR && element2 == LIGHTNING -> true
                else -> false
            }
        }
    }
}

/**
 * Skill Rarity System - Genişletilmiş nadirlik sistemi
 */
@Serializable
enum class SkillRarity(
    val displayName: String,
    val tier: Int,
    val statMultiplier: Float,  // Stat bonusları için çarpan
    val xpMultiplier: Float      // XP kazanımı için çarpan
) {
    G("G Sınıfı", 1, 0.8f, 0.9f),
    F("F Sınıfı", 2, 1.0f, 1.0f),
    E("E Sınıfı", 3, 1.1f, 1.1f),
    D("D Sınıfı", 4, 1.2f, 1.2f),
    C("C Sınıfı", 5, 1.3f, 1.3f),
    B("B Sınıfı", 6, 1.5f, 1.4f),
    A("A Sınıfı", 7, 1.7f, 1.5f),
    S("S Sınıfı", 8, 2.0f, 1.7f),
    SS("SS Sınıfı", 9, 2.5f, 2.0f),
    SSS("SSS Sınıfı", 10, 3.0f, 2.5f),
    SSS_PLUS("SSS+ Sınıfı", 11, 4.0f, 3.0f);

    companion object {
        fun fromTier(tier: Int): SkillRarity {
            return values().find { it.tier == tier } ?: F
        }
    }
}

/**
 * Element Affinity - Kullanıcının bir elemente olan yatkınlığı
 * Tüm aynı elementteki skilllere etki eder
 */
@Serializable
data class ElementAffinity(
    val elementType: ElementType,
    val affinityLevel: Int = 0,  // 0-100 arası (100 = tam uyum)
    val purityLevel: Float = 1.0f,  // 0.0-1.0 arası (1.0 = saf, hibrit kullanımda azalır)
    // GÖREV 4: XP Sistemi
    val level: Int = 1,
    val currentXP: Int = 0,
    val xpToNextLevel: Int = 100
) {
    /**
     * GÖREV 4: Affinity bonusu hesapla (her level %5 bonus)
     */
    fun getAffinityBonus(): Float {
        return (level - 1) * 0.05f
    }

    /**
     * GÖREV 4: İlerleme yüzdesi
     */
    fun getProgressPercentage(): Float {
        return currentXP.toFloat() / xpToNextLevel.toFloat()
    }
}

/**
 * Element Interaction Result - Element çarpışması sonucu
 */
@Serializable
data class ElementInteraction(
    val element1: ElementType,
    val element2: ElementType,
    val resultType: InteractionType,
    val powerMultiplier: Float,  // Güç çarpanı (1.0 = normal, >1.0 güçlenme, <1.0 zayıflama)
    val purityLoss: Float = 0.0f,  // Saflık kaybı (0.0-1.0)
    val hybridElement: ElementType? = null  // Oluşan hibrit element (varsa)
)

@Serializable
enum class InteractionType {
    OPPOSITE,      // Zıt elementler (ör: Ateş vs Su)
    COMPATIBLE,    // Uyumlu elementler (ör: Ateş + Hava)
    NEUTRAL,       // Nötr (etkileşim yok)
    HYBRID_CREATION // Hibrit element oluşumu
}

/**
 * GÖREV 4: Element Affinity Container
 * Tüm element affinity'lerini yöneten container
 */
@Serializable
data class ElementAffinityContainer(
    val affinities: Map<String, ElementAffinity> = createDefaultAffinities()
) {
    companion object {
        /**
         * Varsayılan affinity haritası oluştur (tüm elementler level 1'den başlar)
         */
        fun createDefaultAffinities(): Map<String, ElementAffinity> {
            return mapOf(
                "FIRE" to ElementAffinity(ElementType.FIRE),
                "WATER" to ElementAffinity(ElementType.WATER),
                "EARTH" to ElementAffinity(ElementType.EARTH),
                "AIR" to ElementAffinity(ElementType.AIR),
                "LIGHTNING" to ElementAffinity(ElementType.LIGHTNING),
                "LIGHT" to ElementAffinity(ElementType.LIGHT),
                "DARK" to ElementAffinity(ElementType.DARK),
                "NEUTRAL" to ElementAffinity(ElementType.NEUTRAL)
            )
        }
    }

    /**
     * Bir elemente XP ekle ve level up kontrolü yap
     */
    fun addXP(elementType: String, xp: Int): Pair<ElementAffinityContainer, Boolean> {
        val elementTypeEnum = try {
            ElementType.valueOf(elementType.uppercase())
        } catch (e: Exception) {
            ElementType.NEUTRAL
        }

        val currentAffinity = affinities[elementType]
            ?: ElementAffinity(elementTypeEnum)

        var newXP = currentAffinity.currentXP + xp
        var newLevel = currentAffinity.level
        var leveledUp = false

        // Level up kontrolü
        while (newXP >= currentAffinity.xpToNextLevel) {
            newXP -= currentAffinity.xpToNextLevel
            newLevel++
            leveledUp = true
        }

        val updatedAffinity = currentAffinity.copy(
            level = newLevel,
            currentXP = newXP,
            xpToNextLevel = calculateXPForNextLevel(newLevel)
        )

        val updatedAffinities = affinities.toMutableMap()
        updatedAffinities[elementType] = updatedAffinity

        return Pair(ElementAffinityContainer(updatedAffinities), leveledUp)
    }

    /**
     * Affinity bonus'unu al
     */
    fun getAffinityBonus(elementType: String): Float {
        return affinities[elementType]?.getAffinityBonus() ?: 0f
    }

    /**
     * Level için gereken XP'yi hesapla
     */
    private fun calculateXPForNextLevel(level: Int): Int {
        return (100 * Math.pow(1.15, (level - 1).toDouble())).toInt()
    }
}
