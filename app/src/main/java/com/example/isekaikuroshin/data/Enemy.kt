package com.example.isekaikuroshin.data

import kotlinx.serialization.Serializable

@Serializable
data class Enemy(
    val id: String = "", // G93: Unique enemy ID
    val name: String, // G93: Metadata ID (e.g., "enemy_goblin_1")
    val displayName: String = "", // G144: Localized display name from AI (e.g., "Goblin" / "Goblin")
    var hp: Int, // G93: Changed to var (mutable in combat)
    val maxHp: Int,
    val attack: Int = 15, // G93: Attack power (default 15)
    val defense: Int = 10 // G93: Defense (default 10)
)