package com.example.isekaikuroshin.engine

import kotlinx.serialization.Serializable

/**
 * Player stat types for character progression and gameplay mechanics
 * Supports both percentage and flat modifiers for advanced stat calculation
 */
@Serializable
enum class StatType {
    // Legacy stat types for backward compatibility
    HP,         // Health Points
    MP,         // Magic Points
    STAMINA,    // Stamina/Energy
    HUNGER,     // Hunger level
    FATIGUE,    // Fatigue level
    STR,        // Strength (legacy)
    AGI,        // Agility (legacy)
    INT,        // Intelligence (legacy)
    VIT,        // Vitality (legacy)
    SPIRIT,     // Spirit (legacy)
    LUCK,       // Luck (legacy)
    PER,        // Perception (legacy) - EKLENDİ
    WIS,        // Wisdom (legacy) - Kadim Mühür için
    END,        // Endurance (legacy) - Kadim Mühür için

    // New stat types with percentage and flat modifiers
    STR_PERCENT,    // +X% Strength
    STR_FLAT,       // +X Strength
    AGI_PERCENT,    // +X% Agility
    AGI_FLAT,       // +X Agility
    INT_PERCENT,    // +X% Intelligence
    INT_FLAT,       // +X Intelligence
    VIT_PERCENT,    // +X% Vitality
    VIT_FLAT,       // +X Vitality
    SPIRIT_PERCENT, // +X% Spirit
    SPIRIT_FLAT,    // +X Spirit
    LUCK_PERCENT,   // +X% Luck
    LUCK_FLAT,      // +X Luck
    PER_PERCENT,    // +X% Perception - EKLENDİ
    PER_FLAT        // +X Perception - EKLENDİ
}