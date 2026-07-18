package com.example.isekaikuroshin.utils

import com.example.isekaikuroshin.data.*
import com.example.isekaikuroshin.engine.DiceSystem
import com.example.isekaikuroshin.engine.StatType

/**
 * Dialogue Manager - Handles interactive conversations and choice consequences
 */
object DialogueManager {

    private var currentDialogue: ActiveDialogue? = null
    private val storyFlags = mutableSetOf<String>()

    /**
     * Start conversation with a character
     */
    fun startDialogue(characterId: String): ActiveDialogue? {
        val character = CharacterManager.getCharacter(characterId) ?: return null
        val dialogues = PredefinedDialogues.getDialoguesByCharacter(characterId)

        if (dialogues.isEmpty()) {
            GameLogger.logStorySystem("No dialogues found for character: $characterId")
            return null
        }

        // Find appropriate starting dialogue
        val startingDialogue = findAvailableDialogue(dialogues, character)
        if (startingDialogue == null) {
            GameLogger.logStorySystem("No available dialogues for character: $characterId")
            return null
        }

        // Filter available choices
        val availableChoices = filterAvailableChoices(startingDialogue.choices, character)

        currentDialogue = ActiveDialogue(
            characterId = characterId,
            currentNodeId = startingDialogue.id,
            availableChoices = availableChoices,
            isActive = true
        )

        // G86: Use nameKey with LanguageManager
        val characterName = com.example.isekaikuroshin.data.LanguageManager.getText(character.nameKey)
        GameLogger.logStorySystem("Started dialogue with $characterName: ${startingDialogue.text}")
        return currentDialogue
    }

    /**
     * Process player's choice in dialogue
     */
    suspend fun processChoice(choiceId: String): DialogueResult {
        val dialogue = currentDialogue ?: return DialogueResult.error("No active dialogue")
        val character = CharacterManager.getCharacter(dialogue.characterId)
            ?: return DialogueResult.error("Character not found")

        val choice = dialogue.availableChoices.find { it.id == choiceId }
            ?: return DialogueResult.error("Invalid choice")

        GameLogger.logStorySystem("Player chose: ${choice.text}")

        // Handle dice check if required
        val diceResult = choice.diceCheck?.let { diceCheck ->
            performDiceCheck(diceCheck, character)
        }

        // Apply consequences
        applyConsequences(choice.consequences, character, diceResult?.success ?: true)

        // Build response text
        val responseText = buildResponseText(choice, diceResult, character)

        // Find next dialogue node or end conversation
        val nextNode = findNextDialogueNode(choice, character)

        if (nextNode != null) {
            // Continue conversation
            val availableChoices = filterAvailableChoices(nextNode.choices, character)
            currentDialogue = dialogue.copy(
                currentNodeId = nextNode.id,
                availableChoices = availableChoices,
                conversationHistory = dialogue.conversationHistory + choice.text
            )

            return DialogueResult.success(
                responseText = responseText,
                nextDialogue = nextNode,
                availableChoices = availableChoices,
                diceRoll = diceResult,
                isConversationEnd = false
            )
        } else {
            // End conversation
            currentDialogue = null
            return DialogueResult.success(
                responseText = responseText,
                nextDialogue = null,
                availableChoices = emptyList(),
                diceRoll = diceResult,
                isConversationEnd = true
            )
        }
    }

    /**
     * End current dialogue
     */
    fun endDialogue() {
        currentDialogue?.let { dialogue ->
            GameLogger.logStorySystem("Ended dialogue with character: ${dialogue.characterId}")
        }
        currentDialogue = null
    }

    /**
     * Get current active dialogue
     */
    fun getCurrentDialogue(): ActiveDialogue? = currentDialogue

    /**
     * Check if dialogue is active
     */
    fun isDialogueActive(): Boolean = currentDialogue?.isActive == true

    // Private helper functions

    private fun findAvailableDialogue(dialogues: List<DialogueNode>, character: GameCharacter): DialogueNode? {
        return dialogues.find { dialogue ->
            dialogue.conditions.all { condition -> checkCondition(condition, character) }
        }
    }

    private fun filterAvailableChoices(choices: List<DialogueChoice>, character: GameCharacter): List<DialogueChoice> {
        return choices.filter { choice ->
            when {
                choice.requiresRelationship != null && character.relationship < choice.requiresRelationship -> false
                choice.requiresItem != null && !hasItem(choice.requiresItem) -> false
                choice.requiresSkill != null && !hasSkill(choice.requiresSkill) -> false
                else -> choice.isAvailable
            }
        }
    }

    private suspend fun performDiceCheck(diceCheck: DiceCheck, character: GameCharacter): DiceSystem.DiceRoll {
        val skillModifier = getSkillModifier(diceCheck.skill, character)
        val difficulty = when (diceCheck.difficulty) {
            "VERY_EASY" -> DiceSystem.Difficulty.VERY_EASY
            "EASY" -> DiceSystem.Difficulty.EASY
            "MEDIUM" -> DiceSystem.Difficulty.MEDIUM
            "HARD" -> DiceSystem.Difficulty.HARD
            "VERY_HARD" -> DiceSystem.Difficulty.VERY_HARD
            else -> DiceSystem.Difficulty.MEDIUM
        }

        return DiceSystem.skillCheck(skillModifier, difficulty)
    }

    private suspend fun applyConsequences(
        consequences: DialogueConsequences,
        character: GameCharacter,
        success: Boolean
    ) {
        // Relationship change
        if (consequences.relationshipChange != 0) {
            CharacterManager.updateRelationship(character.id, consequences.relationshipChange)
        }

        // Add/remove items
        consequences.addItem?.let { item ->
            // TODO: Add item to inventory
            GameLogger.logStorySystem("Added item: $item")
        }

        consequences.removeItem?.let { item ->
            // TODO: Remove item from inventory
            GameLogger.logStorySystem("Removed item: $item")
        }

        // Unlock location
        consequences.unlockLocation?.let { location ->
            GameLogger.logStorySystem("Unlocked location: $location")
        }

        // Set story flag
        consequences.setFlag?.let { flag ->
            storyFlags.add(flag)
            GameLogger.logStorySystem("Set story flag: $flag")
        }

        // Trigger event
        consequences.triggerEvent?.let { event ->
            GameLogger.logStorySystem("Triggered event: $event")
        }
    }

    private fun buildResponseText(
        choice: DialogueChoice,
        diceResult: DiceSystem.DiceRoll?,
        character: GameCharacter
    ): String {
        var responseText = ""

        // Add dice result text if applicable
        if (diceResult != null && choice.diceCheck != null) {
            responseText += if (diceResult.success) {
                "🎲 ${diceResult.total} - ${choice.diceCheck.successText}\n\n"
            } else {
                "🎲 ${diceResult.total} - ${choice.diceCheck.failureText}\n\n"
            }
        }

        return responseText
    }

    private fun findNextDialogueNode(choice: DialogueChoice, character: GameCharacter): DialogueNode? {
        // Find the next dialogue node based on choice ID and current state
        val dialogues = PredefinedDialogues.getDialoguesByCharacter(character.id)

        return when (choice.id) {
            "ask_destination" -> dialogues.find { it.id == "cart_driver_destination" }
            "ask_dangers" -> dialogues.find { it.id == "cart_driver_dangers" }
            else -> null // End conversation for most choices
        }
    }

    private fun checkCondition(condition: String, character: GameCharacter): Boolean {
        return when {
            condition.startsWith("relationship_") -> {
                val requiredRelationship = condition.substringAfter("relationship_").toIntOrNull() ?: 0
                character.relationship >= requiredRelationship
            }
            condition.startsWith("has_flag_") -> {
                val flag = condition.substringAfter("has_flag_")
                storyFlags.contains(flag)
            }
            else -> true
        }
    }

    private fun hasItem(itemName: String): Boolean {
        // TODO: Check player inventory
        return false
    }

    private fun hasSkill(skillName: String): Boolean {
        // TODO: Check player skills/stats
        return true
    }

    private fun getSkillModifier(skillName: String, character: GameCharacter): Int {
        // TODO: Get skill modifier from player stats
        return when (skillName.lowercase()) {
            "charisma" -> 12
            "intelligence" -> 10
            "deception" -> 8
            "intimidation" -> 15
            else -> 10
        }
    }
}

/**
 * Result of a dialogue choice
 */
data class DialogueResult(
    val success: Boolean,
    val responseText: String,
    val nextDialogue: DialogueNode?,
    val availableChoices: List<DialogueChoice>,
    val diceRoll: DiceSystem.DiceRoll?,
    val isConversationEnd: Boolean,
    val errorMessage: String? = null
) {
    companion object {
        fun success(
            responseText: String,
            nextDialogue: DialogueNode?,
            availableChoices: List<DialogueChoice>,
            diceRoll: DiceSystem.DiceRoll?,
            isConversationEnd: Boolean
        ) = DialogueResult(
            success = true,
            responseText = responseText,
            nextDialogue = nextDialogue,
            availableChoices = availableChoices,
            diceRoll = diceRoll,
            isConversationEnd = isConversationEnd
        )

        fun error(message: String) = DialogueResult(
            success = false,
            responseText = "",
            nextDialogue = null,
            availableChoices = emptyList(),
            diceRoll = null,
            isConversationEnd = true,
            errorMessage = message
        )
    }
}