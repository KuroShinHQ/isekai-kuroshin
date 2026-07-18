package com.example.isekaikuroshin.engine

import com.example.isekaikuroshin.data.Enemy

// TODO-G106-FIX: ActionType enum moved to ProfileUpdaterEngine.kt (KURAL 16: language-agnostic)
// Old enum removed to avoid redeclaration

/**
 * Combat action intent for parsing player input during battle
 * Uses CombatActionType (separate from archetype ActionType)
 */
enum class CombatActionType {
    ATTACK,       // Physical attack intent
    DEFEND,       // Block or defensive position
    BLIND,        // Blind enemy
    CRIPPLE,      // Reduce enemy movement
    CAST_SPELL,   // Use magical ability
    UNKNOWN       // Default when parser cannot determine intent
}

/**
 * Parsed action intent from player text input
 *
 * @property type Detected action type (e.g. ATTACK, BLIND)
 * @property target Target enemy or body part name
 * @property usedItem Item used in action (e.g. "sword", "dust")
 */
data class ActionIntent(
    val type: CombatActionType = CombatActionType.UNKNOWN,
    val target: String? = null,
    val usedItem: String? = null
)

/**
 * Oyuncunun serbest metin girdilerini analiz ederek yapılandırılmış bir ActionIntent'e dönüştüren singleton bir motor.
 * Bu, "goblinin gözüne toz at" gibi yaratıcı girdileri oyun mekaniklerine bağlamayı sağlar.
 */
object ActionIntentParser {

    // Anahtar kelimeleri ilgili eylem türleriyle eşleştiren harita.
    // TODO-G106-FIX: Updated to use CombatActionType (KURAL 16: EN keys)
    // More specific keywords (e.g. "gözüne") should come before general ones (e.g. "göz")
    private val keywordToActionType = mapOf(
        // Attack
        "saldır" to CombatActionType.ATTACK,
        "attack" to CombatActionType.ATTACK,
        "vur" to CombatActionType.ATTACK,
        "savur" to CombatActionType.ATTACK,
        "hücum et" to CombatActionType.ATTACK,
        "kılıçla" to CombatActionType.ATTACK,

        // Blind
        "gözüne" to CombatActionType.BLIND,
        "gözlerine" to CombatActionType.BLIND,
        "kör et" to CombatActionType.BLIND,
        "blind" to CombatActionType.BLIND,
        "göz" to CombatActionType.BLIND,

        // Cripple
        "bacağına" to CombatActionType.CRIPPLE,
        "dizine" to CombatActionType.CRIPPLE,
        "topallat" to CombatActionType.CRIPPLE,
        "cripple" to CombatActionType.CRIPPLE,
        "bacak" to CombatActionType.CRIPPLE,

        // Defend
        "savun" to CombatActionType.DEFEND,
        "defend" to CombatActionType.DEFEND,
        "savunma yap" to CombatActionType.DEFEND,
        "blokla" to CombatActionType.DEFEND,
        "kaçın" to CombatActionType.DEFEND,

        // Cast Spell
        "büyü yap" to CombatActionType.CAST_SPELL,
        "cast" to CombatActionType.CAST_SPELL,
        "spell" to CombatActionType.CAST_SPELL,
        "ateş topu" to CombatActionType.CAST_SPELL,
        "yıldırım" to CombatActionType.CAST_SPELL
    )

    /**
     * Verilen metin girdisini ve mevcut düşman listesini analiz ederek bir ActionIntent oluşturur.
     *
     * @param input Oyuncunun yazdığı serbest metin (örn: "gobline kılıçla saldır").
     * @param enemies O an savaşta olan düşmanların bir listesi.
     * @return Algılanan niyeti içeren bir ActionIntent nesnesi.
     */
    fun parse(input: String, enemies: List<Enemy>): ActionIntent {
        val lowercasedInput = input.lowercase()

        // 1. Eylem Türünü Belirle (ActionType)
        // Find first keyword in text that matches map
        val actionType = keywordToActionType.entries
            .find { (keyword, _) -> lowercasedInput.contains(keyword) }
            ?.value ?: CombatActionType.UNKNOWN

        // 2. Hedefi Belirle (Target)
        // Metin içinde adı geçen ilk düşmanı bul.
        val target = enemies
            .find { enemy -> lowercasedInput.contains(enemy.name.lowercase()) }
            ?.name

        // 3. Kullanılan Eşyayı Belirle (usedItem) - Gelecek geliştirmeler için yer tutucu
        // Örn: "toz", "kılıç", "hançer" gibi kelimeler aranabilir.
        val usedItem: String? = null // Şimdilik bu mantık eklenmedi.

        return ActionIntent(
            type = actionType,
            target = target,
            usedItem = usedItem
        )
    }
}
