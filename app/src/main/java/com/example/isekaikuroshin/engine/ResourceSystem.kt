package com.example.isekaikuroshin.engine

import com.example.isekaikuroshin.data.GameStateManager
import com.example.isekaikuroshin.data.ResourcesZ4
import com.example.isekaikuroshin.utils.GameLogger
import kotlin.math.min

class ResourceSystem(private val gameStateManager: GameStateManager) {

    // Define maximum limits for each resource (max 10 items per stack)
    private val maxResourceLimits = mapOf(
        "dal" to 10,
        "tas" to 10,
        "odun" to 10,
        "deri" to 10,
        "demir" to 10
    )

    /**
     * Attempts to add a specified amount of a resource.
     * Respects the maximum resource limits.
     *
     * @param resourceType The type of resource (e.g., "dal", "tas").
     * @param amount The amount to add.
     * @return The actual amount of resource added (may be less than requested if limit is hit).
     */
    fun addResource(resourceType: String, amount: Int): Int {
        val currentState = gameStateManager.gameState.value
        val currentResources = currentState.resources
        val maxLimit = maxResourceLimits[resourceType.lowercase()] ?: Int.MAX_VALUE

        val currentAmount = when (resourceType.lowercase()) {
            "dal" -> currentResources.dal
            "tas" -> currentResources.tas
            "odun" -> currentResources.odun
            "deri" -> currentResources.deri
            "demir" -> currentResources.demir
            else -> 0
        }

        val actualAmountToAdd = min(amount, maxLimit - currentAmount)

        // Log when stacking limit is hit
        if (amount > actualAmountToAdd) {
            GameLogger.logStorySystem("Resource $resourceType hit stack limit: tried to add $amount, could only add $actualAmountToAdd (current: $currentAmount, max: $maxLimit)")
        }

        if (actualAmountToAdd > 0) {
            val newResources = when (resourceType.lowercase()) {
                "dal" -> currentResources.copy(dal = currentResources.dal + actualAmountToAdd)
                "tas" -> currentResources.copy(tas = currentResources.tas + actualAmountToAdd)
                "odun" -> currentResources.copy(odun = currentResources.odun + actualAmountToAdd)
                "deri" -> currentResources.copy(deri = currentResources.deri + actualAmountToAdd)
                "demir" -> currentResources.copy(demir = currentResources.demir + actualAmountToAdd)
                else -> currentResources
            }
            gameStateManager.updatePlayerResources(newResources) // DEĞİŞTİRİLDİ
        }
        return actualAmountToAdd
    }

    /**
     * Attempts to remove a specified amount of a resource.
     *
     * @param resourceType The type of resource.
     * @param amount The amount to remove.
     * @return True if resources were successfully removed, false otherwise (e.g., not enough resources).
     */
    fun removeResource(resourceType: String, amount: Int): Boolean {
        val currentState = gameStateManager.gameState.value
        val currentResources = currentState.resources

        val currentAmount = when (resourceType.lowercase()) {
            "dal" -> currentResources.dal
            "tas" -> currentResources.tas
            "odun" -> currentResources.odun
            "deri" -> currentResources.deri
            "demir" -> currentResources.demir
            else -> 0
        }

        if (currentAmount >= amount) {
            val newResources = when (resourceType.lowercase()) {
                "dal" -> currentResources.copy(dal = currentResources.dal - amount)
                "tas" -> currentResources.copy(tas = currentResources.tas - amount)
                "odun" -> currentResources.copy(odun = currentResources.odun - amount)
                "deri" -> currentResources.copy(deri = currentResources.deri - amount)
                "demir" -> currentResources.copy(demir = currentResources.demir - amount)
                else -> currentResources
            }
            gameStateManager.updatePlayerResources(newResources) // DEĞİŞTİRİLDİ
            return true
        }
        return false
    }

    /**
     * Checks if the player has at least the specified amount of a resource.
     */
    fun hasResource(resourceType: String, amount: Int): Boolean {
        val currentState = gameStateManager.gameState.value
        val currentResources = currentState.resources

        val currentAmount = when (resourceType.lowercase()) {
            "dal" -> currentResources.dal
            "tas" -> currentResources.tas
            "odun" -> currentResources.odun
            "deri" -> currentResources.deri
            "demir" -> currentResources.demir
            else -> 0
        }
        return currentAmount >= amount
    }

    /**
     * Gets the current amount of a specific resource.
     */
    fun getResourceAmount(resourceType: String): Int {
        val currentState = gameStateManager.gameState.value
        val currentResources = currentState.resources
        return when (resourceType.lowercase()) {
            "dal" -> currentResources.dal
            "tas" -> currentResources.tas
            "odun" -> currentResources.odun
            "deri" -> currentResources.deri
            "demir" -> currentResources.demir
            else -> 0
        }
    }
}