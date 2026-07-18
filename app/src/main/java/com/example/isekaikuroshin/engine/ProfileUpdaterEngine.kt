package com.example.isekaikuroshin.engine

import com.example.isekaikuroshin.data.PlayerProfile

/**
 * TODO-G106.1: ActionType enum - Oyuncu aksiyonlarını kategorize eder
 * KURAL 16: Language-agnostic enum (EN kodları, UI'da LanguageManager ile çevrilir)
 */
enum class ActionType {
    // Warrior actions
    ATTACK,
    DEFEND,
    WEAPON_USE,

    // Explorer actions
    STEALTH,
    DISABLE_TRAP,
    HIDE,
    TRACK,

    // Mystic actions
    CAST_SPELL,
    RITUAL,

    // Craftsman actions
    CRAFT,
    REPAIR,

    // Other
    OTHER
}

object ProfileUpdaterEngine {

    fun updateProfile(action: ActionType, currentProfile: PlayerProfile): PlayerProfile {
        val newScores = currentProfile.archetypeScores.toMutableMap()

        // TODO-G106.1: ActionType mapping (EN kodları, archetype keys language-agnostic)
        when (action) {
            // Warrior actions (+5 points)
            ActionType.ATTACK, ActionType.DEFEND, ActionType.WEAPON_USE -> {
                newScores["Warrior"] = (newScores["Warrior"] ?: 0) + 5
            }

            // Explorer actions (+3 points)
            ActionType.STEALTH, ActionType.DISABLE_TRAP, ActionType.HIDE, ActionType.TRACK -> {
                newScores["Explorer"] = (newScores["Explorer"] ?: 0) + 3
            }

            // Mystic actions (+5 points)
            ActionType.CAST_SPELL, ActionType.RITUAL -> {
                newScores["Mystic"] = (newScores["Mystic"] ?: 0) + 5
            }

            // Craftsman actions (+4 points)
            ActionType.CRAFT, ActionType.REPAIR -> {
                newScores["Craftsman"] = (newScores["Craftsman"] ?: 0) + 4
            }

            ActionType.OTHER -> {
                // Neutral action, no score change
            }
        }

        val dominantArchetype = newScores.maxByOrNull { it.value }?.key ?: "Unknown" // KURAL 16: EN default

        return currentProfile.copy(
            archetypeScores = newScores,
            dominantArchetype = dominantArchetype
        )
    }
}