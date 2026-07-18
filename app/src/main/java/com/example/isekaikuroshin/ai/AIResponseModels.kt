package com.example.isekaikuroshin.ai

import kotlinx.serialization.Serializable

/**
 * AI'dan gelen tam yanıtı temsil eder.
 */
@Serializable
data class AIResponse(
    val storyText: String, // Oyuncunun okuyacağı anlatı metni
    val actions: List<GameAction> = emptyList() // Sistemin uygulaması gereken oyun içi eylemler
)

/**
 * AI'ın tetiklemek istediği tek bir oyun içi eylemi temsil eder.
 */
@Serializable
data class GameAction(
    val actionType: String, // Eylemin türü (örn: "CREATE_QUEST", "GIVE_ITEM", "UPDATE_PLAYER_STATE")
    val parameters: Map<String, String> // Eylem için gerekli parametreler (örn: {"itemId": "sword_01", "quantity": "1"})
)