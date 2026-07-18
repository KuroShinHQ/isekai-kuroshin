
package com.example.isekaikuroshin.ui.character

import com.example.isekaikuroshin.ui.components.CardData

data class CharacterStatusUiState(
    val characterName: String = "",
    val level: String = "",
    val unspentStatPoints: Int = 0,  // Dağıtılmamış stat puanları
    val pendingPoints: Int = 0,      // UI'da henüz onaylanmamış allocation'lar
    val lifeStats: List<Pair<String, String>> = emptyList(),
    val mainStats: List<Pair<String, String>> = emptyList(),
    val titleCard: CardData.BadgeCard? = null,
    val skillCard: CardData.SkillCard? = null,
    // G65: Achievement/Title Display
    val allTitles: List<CardData.BadgeCard> = emptyList(),  // Tüm title'lar
    val allBadges: List<CardData.BadgeCard> = emptyList(),   // Tüm badge'ler
    // G113.5: Active Status Effects Display
    val activeStatusEffects: List<StatusEffectDisplayData> = emptyList(),
    // TODO-G106.5: Archetype Profile Display
    val dominantArchetype: String = "Unknown",
    val archetypeScores: Map<String, Int> = emptyMap()
)

/**
 * G113.5: Status effect display data for UI
 */
data class StatusEffectDisplayData(
    val effectId: String,
    val name: String,
    val type: String, // "BUFF" or "DEBUFF"
    val remainingTurns: Int,
    val magnitude: Int
)
