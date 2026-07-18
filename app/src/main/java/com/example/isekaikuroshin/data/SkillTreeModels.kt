package com.example.isekaikuroshin.data

import kotlinx.serialization.Serializable

@Serializable
data class SkillNode(
    val id: String,
    val name: String,
    val description: String,
    val x: Float, // 0-1 arası koordinat
    val y: Float, // 0-1 arası koordinat
    val parentId: String? = null,
    // GÖREV 3: XP Sistemi
    val elementType: String? = null, // "FIRE", "WATER", "EARTH", "AIR", etc.
    val level: Int = 1,
    val currentXP: Int = 0,
    val xpToNextLevel: Int = 100 // Her level için gereken XP
)

data class SkillTreeUiState(
    val skillNodes: List<SkillNode> = emptyList(),
    val unlockedSkills: Set<String> = emptySet(),
    val skillPoints: Int = 0
)