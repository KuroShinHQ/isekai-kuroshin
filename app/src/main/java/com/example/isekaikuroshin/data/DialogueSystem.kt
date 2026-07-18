package com.example.isekaikuroshin.data

import kotlinx.serialization.Serializable

/**
 * Interactive Dialogue System - Choice-based conversations and story branching
 */

@Serializable
data class DialogueChoice(
    val id: String,
    val text: String,
    val requiresSkill: String? = null, // "Charisma", "Intelligence", etc.
    val requiresRelationship: Int? = null, // Minimum relationship level
    val requiresItem: String? = null, // Required item in inventory
    val diceCheck: DiceCheck? = null,
    val consequences: DialogueConsequences = DialogueConsequences(),
    val isAvailable: Boolean = true
)

@Serializable
data class DiceCheck(
    val skill: String, // "Charisma", "Deception", "Intimidation"
    val difficulty: String, // "EASY", "MEDIUM", "HARD"
    val successText: String,
    val failureText: String
)

@Serializable
data class DialogueConsequences(
    val relationshipChange: Int = 0,
    val addItem: String? = null,
    val removeItem: String? = null,
    val unlockLocation: String? = null,
    val triggerEvent: String? = null,
    val setFlag: String? = null // Story flags like "knows_secret"
)

@Serializable
data class DialogueNode(
    val id: String,
    val characterId: String,
    val speakerName: String,
    val text: String,
    val choices: List<DialogueChoice> = emptyList(),
    val isEndNode: Boolean = false,
    val nextNodeId: String? = null, // For linear dialogue
    val conditions: List<String> = emptyList() // Conditions to show this node
)

@Serializable
data class ActiveDialogue(
    val characterId: String,
    val currentNodeId: String,
    val conversationHistory: List<String> = emptyList(),
    val availableChoices: List<DialogueChoice> = emptyList(),
    val isActive: Boolean = true
)

/**
 * Predefined dialogue trees for main characters
 */
object PredefinedDialogues {

    // Mehmet Arabacı (Cart Driver) dialogue tree
    val CART_DRIVER_DIALOGUES = listOf(
        DialogueNode(
            id = "cart_driver_greeting",
            characterId = "cart_driver_001",
            speakerName = "Mehmet Arabacı",
            text = "Hoş geldin yolcu! Bu yolları 30 yıldır biliyorum. Nereye gitmek istiyorsun?",
            choices = listOf(
                DialogueChoice(
                    id = "ask_destination",
                    text = "Bu yolculuk ne kadar sürecek?",
                    consequences = DialogueConsequences(relationshipChange = 1)
                ),
                DialogueChoice(
                    id = "ask_dangers",
                    text = "Bu bölgede tehlike var mı?",
                    consequences = DialogueConsequences(relationshipChange = 2)
                ),
                DialogueChoice(
                    id = "ask_stories",
                    text = "Yol hikayeleriniz var mı?",
                    requiresRelationship = 5,
                    consequences = DialogueConsequences(relationshipChange = 3)
                ),
                DialogueChoice(
                    id = "stay_silent",
                    text = "Sessizce yolculuğa devam et.",
                    consequences = DialogueConsequences(relationshipChange = -1)
                )
            )
        ),

        DialogueNode(
            id = "cart_driver_destination",
            characterId = "cart_driver_001",
            speakerName = "Mehmet Arabacı",
            text = "Yaklaşık üç günlük yolumuz var. Atlar yorulmadan gidersek akşama bir köye varırız.",
            choices = listOf(
                DialogueChoice(
                    id = "ask_village",
                    text = "Hangi köy o?",
                    consequences = DialogueConsequences(unlockLocation = "Yeşilvadi Köyü")
                ),
                DialogueChoice(
                    id = "thank_driver",
                    text = "Bilgi için teşekkürler.",
                    consequences = DialogueConsequences(relationshipChange = 1)
                )
            )
        ),

        DialogueNode(
            id = "cart_driver_dangers",
            characterId = "cart_driver_001",
            speakerName = "Mehmet Arabacı",
            text = "Eski savaş zamanından kalma haydutlar dolaşır buralarda. Gece yolculuğu tehlikeli...",
            choices = listOf(
                DialogueChoice(
                    id = "ask_bandits",
                    text = "Haydutlarla karşılaştınız mı?",
                    diceCheck = DiceCheck(
                        skill = "Charisma",
                        difficulty = "MEDIUM",
                        successText = "Mehmet sana güvenir ve deneyimlerini paylaşır.",
                        failureText = "Mehmet konuyu değiştirmek ister."
                    )
                ),
                DialogueChoice(
                    id = "offer_protection",
                    text = "Size yardım edebilirim.",
                    requiresSkill = "Combat",
                    consequences = DialogueConsequences(relationshipChange = 5)
                )
            )
        )
    )

    fun getAllDialogues(): List<DialogueNode> {
        return CART_DRIVER_DIALOGUES
    }

    fun getDialoguesByCharacter(characterId: String): List<DialogueNode> {
        return getAllDialogues().filter { it.characterId == characterId }
    }
}