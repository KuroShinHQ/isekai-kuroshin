package com.example.isekaikuroshin.data

import androidx.compose.ui.graphics.vector.ImageVector

enum class Element {
    FIRE,
    WATER,
    EARTH,
    AIR,
    LIGHTNING,
    ICE,
    LIGHT,
    DARK,
    VOID,
    NATURE
}

data class Spell(
    val name: String,
    val description: String,
    val element: Element,
    val tier: String,
    val manaCost: Int,
    val damage: Long = 0,
    val healing: Long = 0,
    val duration: Int = 0, // in seconds for buffs/debuffs
    val cooldown: Int = 0, // in seconds
    val range: Float = 1.0f, // multiplier for spell range
    val effects: List<SpellEffect> = emptyList(),
    val requiredLevel: Int = 1
)

data class SpellEffect(
    val name: String,
    val description: String,
    val type: EffectType,
    val magnitude: Float,
    val duration: Int = 0 // in seconds, 0 for instant effects
)

enum class EffectType {
    DAMAGE,
    HEALING,
    BUFF_STAT,
    DEBUFF_STAT,
    DOT, // Damage over time
    HOT, // Healing over time
    STUN,
    FREEZE,
    BURN,
    POISON,
    REGENERATION,
    SHIELD,
    TELEPORT,
    SUMMON
}

data class ElementalCombination(
    val primaryElement: Element,
    val secondaryElement: Element,
    val resultElement: Element,
    val bonusMultiplier: Float,
    val newEffects: List<SpellEffect> = emptyList()
)

data class MagicSchool(
    val name: String,
    val description: String,
    val primaryElement: Element,
    val secondaryElements: List<Element> = emptyList(),
    val specialtyBonuses: Map<String, Float> = emptyMap()
)