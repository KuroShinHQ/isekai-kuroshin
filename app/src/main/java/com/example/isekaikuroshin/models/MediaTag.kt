package com.example.isekaikuroshin.models

import kotlinx.serialization.Serializable

/**
 * MediaTag v2.0 - Eksen Bazlı Attribute Sistemi
 *
 * Bölüm 8'de tasarlanan yeni etiketleme sistemi:
 * - 6 attribute ekseni (bipolar: VIOLENCE-MERCY, CHAOS-ORDER, vb.)
 * - 4 özel attribute (DIVINE, DARK, MYSTERY, SURVIVAL)
 * - 5 depth seviyesi (D1-D5)
 * - NARRATIVE_ATMOSPHERE (11 seçenek)
 * - PSYCHOLOGICAL_ARCHETYPE (12 Jung arketipi)
 */
@Serializable
data class MediaTag(
    val fileName: String,                      // Dosya adı
    val fileType: MediaType,                   // VIDEO veya PHOTO
    val resourceId: Int,                       // R.raw.xxx veya R.drawable.xxx

    // Temel bilgiler
    val screenType: ScreenType,                // FIRSTUSER, RETURNINGUSER, POSTDEATH, vb.
    val updateMode: UpdateMode,                // GM_UPDATED veya HARDCODED
    val mediaUsage: MediaUsage,                // TRANSITION_SCENE, IN_SCREEN, BACKGROUND

    // v2.0: Eksen bazlı attribute sistemi
    val primaryAxis: AttributeAxis? = null,    // Ana eksen (örn: VIOLENCE_MERCY)
    val primaryAxisValue: Float = 0f,          // -1.0 (VIOLENCE) ... +1.0 (MERCY)

    val secondaryAxis: AttributeAxis? = null,  // İkincil eksen (isteğe bağlı)
    val secondaryAxisValue: Float = 0f,        // -1.0 (negatif) ... +1.0 (pozitif)

    val specialAttribute: SpecialAttribute = SpecialAttribute.NONE, // DIVINE, DARK, MYSTERY, SURVIVAL

    // Duygu ve atmosfer
    val emotion: Emotion = Emotion.NONE,       // JOY, SADNESS, ANGER, CALM, CONFUSION (FEAR kaldırıldı)
    val narrativeAtmosphere: NarrativeAtmosphere = NarrativeAtmosphere.NONE, // 11 seçenek
    val psychologicalArchetype: PsychologicalArchetype = PsychologicalArchetype.NONE, // 12 Jung arketipi

    // Depth (5 seviye)
    val depth: DepthLevel = DepthLevel.D1_SURFACE,

    // Metadata
    val sequenceNumber: String = "001",        // Dosya sıra numarası
    val createdAt: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList()       // Ek etiketler
)

/**
 * Attribute Eksenleri - Bipolar (Zıt Kutuplu) Sistemler
 * Her eksen iki zıt kutup içerir
 */
enum class AttributeAxis(val id: Int, val axisName: String, val negativePole: String, val positivePole: String) {
    AXIS_1_VIOLENCE_MERCY(1, "VIOLENCE_MERCY", "VIOLENCE", "MERCY"),
    AXIS_2_CHAOS_ORDER(2, "CHAOS_ORDER", "CHAOS", "ORDER"),
    AXIS_3_SELFISH_SACRIFICE(3, "SELFISH_SACRIFICE", "SELFISH", "SACRIFICE"),
    AXIS_4_FEAR_COURAGE(4, "FEAR_COURAGE", "FEAR", "COURAGE"),
    AXIS_5_DECEIT_LOYALTY(5, "DECEIT_LOYALTY", "DECEIT", "LOYALTY"),
    AXIS_6_DARKNESS_LIGHT(6, "DARKNESS_LIGHT", "DARK", "LIGHT");

    companion object {
        fun fromId(id: Int): AttributeAxis? = values().find { it.id == id }
        fun fromAxisName(name: String): AttributeAxis? = values().find { it.axisName == name }
    }
}

/**
 * Özel Attribute'lar - Kategorik (Bipolar değil)
 */
enum class SpecialAttribute(val id: Int, val label: String) {
    DIVINE(14, "DIVINE"),           // Kutsal / İlahi
    DARK(11, "DARK"),               // Karanlık (AXIS_6'dan bağımsız)
    MYSTERY(16, "MYSTERY"),         // Gizemli / Esrarengiz
    SURVIVAL(13, "SURVIVAL"),       // Hayatta Kalma
    NONE(0, "NONE");                // Seçilmedi

    companion object {
        fun fromId(id: Int): SpecialAttribute? = values().find { it.id == id }
        fun fromLabel(label: String): SpecialAttribute? = values().find { it.label == label }
    }
}

/**
 * Duygu Durumları (v2.0: FEAR kaldırıldı → AXIS_4'e taşındı)
 */
enum class Emotion(val id: Int, val label: String) {
    ANGER(1, "ANGER"),
    SADNESS(2, "SADNESS"),
    JOY(3, "JOY"),
    CALM(4, "CALM"),
    CONFUSION(5, "CONFUSION"),
    NONE(0, "NONE");

    companion object {
        fun fromId(id: Int): Emotion? = values().find { it.id == id }
        fun fromLabel(label: String): Emotion? = values().find { it.label == label }
    }
}

/**
 * Depth Seviyeleri (v2.0: 3 → 5 seviye)
 */
enum class DepthLevel(val id: Int, val label: String, val tr: String) {
    D1_SURFACE(1, "D1_SURFACE", "Yüzeysel"),
    D2_EMOTIONAL(2, "D2_EMOTIONAL", "Duygusal"),
    D3_SYMBOLIC(3, "D3_SYMBOLIC", "Sembolik"),
    D4_ARCHETYPAL(4, "D4_ARCHETYPAL", "Arketipsel"),
    D5_TRANSCENDENT(5, "D5_TRANSCENDENT", "Aşkın / Transandantal");

    companion object {
        fun fromId(id: Int): DepthLevel? = values().find { it.id == id }
        fun fromLabel(label: String): DepthLevel? = values().find { it.label == label }
    }
}

/**
 * Narrative Atmosphere (v2.0: MORAL_TONE + NARRATIVE_MOOD birleştirildi)
 */
enum class NarrativeAtmosphere(val id: Int, val label: String) {
    TRAGIC_REDEMPTION(1, "TRAGIC_REDEMPTION"),           // Trajik + Kefaret
    EPIC_JUSTICE(2, "EPIC_JUSTICE"),                     // Destansı + Adalet
    DARK_VENGEANCE(3, "DARK_VENGEANCE"),                 // Karanlık + İntikam
    MELANCHOLIC_FORGIVENESS(4, "MELANCHOLIC_FORGIVENESS"), // Melankolik + Af
    TRIUMPHANT_JUDGMENT(5, "TRIUMPHANT_JUDGMENT"),       // Zafer + Yargı
    HORROR_DENIAL(6, "HORROR_DENIAL"),                   // Korku + İnkar
    MYSTICAL_ACCEPTANCE(7, "MYSTICAL_ACCEPTANCE"),       // Mistik + Kabul
    ROMANTIC_TRANSFORMATION(8, "ROMANTIC_TRANSFORMATION"), // Romantik + Dönüşüm
    COMEDIC(9, "COMEDIC"),                               // Komik
    NOIR(10, "NOIR"),                                    // Noir (karanlık dedektif)
    PHILOSOPHICAL(11, "PHILOSOPHICAL"),                  // Felsefi
    NONE(0, "NONE");

    companion object {
        fun fromId(id: Int): NarrativeAtmosphere? = values().find { it.id == id }
        fun fromLabel(label: String): NarrativeAtmosphere? = values().find { it.label == label }
    }
}

/**
 * Psychological Archetype (12 Jung Arketipi - v2.0: PERSONALITY_TRAIT yerine korundu)
 */
enum class PsychologicalArchetype(val id: Int, val label: String) {
    HERO(1, "HERO"),
    SHADOW(2, "SHADOW"),
    SAGE(3, "SAGE"),
    REBEL(4, "REBEL"),
    INNOCENT(5, "INNOCENT"),
    EXPLORER(6, "EXPLORER"),
    RULER(7, "RULER"),
    CAREGIVER(8, "CAREGIVER"),
    JESTER(9, "JESTER"),
    LOVER(10, "LOVER"),
    CREATOR(11, "CREATOR"),
    DESTROYER(12, "DESTROYER"),
    NONE(0, "NONE");

    companion object {
        fun fromId(id: Int): PsychologicalArchetype? = values().find { it.id == id }
        fun fromLabel(label: String): PsychologicalArchetype? = values().find { it.label == label }
    }
}

/**
 * Medya Türü
 */
enum class MediaType {
    VIDEO,
    PHOTO
}

/**
 * Ekran Türleri (v2.0: Değişmedi)
 */
enum class ScreenType(val id: Int, val label: String) {
    FIRSTUSER(1, "FIRSTUSER"),
    RETURNINGUSER(2, "RETURNINGUSER"),
    POSTDEATH(3, "POSTDEATH"),
    UMBROS(4, "UMBROS"),
    JOURNEY(5, "JOURNEY"),
    DEATH_TRANSITION(6, "DEATH_TRANSITION"),
    DEATH_STATISTICS(7, "DEATH_STATISTICS"),
    LAUNCHER_ICON(8, "LAUNCHER_ICON");

    companion object {
        fun fromId(id: Int): ScreenType? = values().find { it.id == id }
        fun fromLabel(label: String): ScreenType? = values().find { it.label == label }
    }
}

/**
 * Update Mode (v2.0: Değişmedi)
 */
enum class UpdateMode(val id: Int, val label: String) {
    GM_UPDATED(1, "GM_UPDATED"),       // GameMaster tarafından otomatik güncellenir
    HARDCODED(2, "HARDCODED");         // Sabit, değişmez

    companion object {
        fun fromId(id: Int): UpdateMode? = values().find { it.id == id }
        fun fromLabel(label: String): UpdateMode? = values().find { it.label == label }
    }
}

/**
 * Media Usage (v2.0: Değişmedi)
 */
enum class MediaUsage(val id: Int, val label: String) {
    TRANSITION_SCENE(1, "TRANSITION_SCENE"), // Sahne geçişlerinde
    IN_SCREEN(2, "IN_SCREEN"),               // Ekran içinde (background)
    BACKGROUND(3, "BACKGROUND");             // Arka plan

    companion object {
        fun fromId(id: Int): MediaUsage? = values().find { it.id == id }
        fun fromLabel(label: String): MediaUsage? = values().find { it.label == label }
    }
}
