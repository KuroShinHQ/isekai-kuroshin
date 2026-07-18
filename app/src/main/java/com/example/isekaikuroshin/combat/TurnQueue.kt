package com.example.isekaikuroshin.combat

import com.example.isekaikuroshin.data.Enemy
import com.example.isekaikuroshin.data.PlayerStats
import com.example.isekaikuroshin.utils.GameLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * G95: Turn Queue System - Phase 2C
 *
 * Speed-based turn order management:
 * - Sorts entities by speed (fastest first)
 * - Round-robin turn cycling
 * - Handles entity death (removes from queue)
 * - Handles stun (skips turn but stays in queue)
 *
 * KURAL 11: Metadata-based (uses speed field, not names)
 * KURAL 14: Performance optimized (efficient queue operations)
 * KURAL 15: Root cause analysis (detailed logging for debugging turn order issues)
 */
@Singleton
class TurnQueue @Inject constructor() {
    companion object {
        private const val TAG = "TurnQueue"
        private const val PLAYER_ENTITY_ID = "PLAYER"
    }

    /**
     * Turn Entry - Represents one entity in turn queue
     */
    data class TurnEntry(
        val entityId: String,
        val entityType: EntityType,
        val speed: Int,
        var isStunned: Boolean = false
    ) {
        override fun toString(): String {
            return when (entityType) {
                EntityType.PLAYER -> "🧙 Player"
                EntityType.ENEMY -> "👹 $entityId"
            } + if (isStunned) " (STUNNED)" else ""
        }
    }

    enum class EntityType {
        PLAYER,
        ENEMY
    }

    // Turn queue (sorted by speed, highest first)
    private val queue = mutableListOf<TurnEntry>()
    private var currentTurnIndex = 0

    /**
     * Initialize turn queue with player and enemies
     * Sorts by speed (fastest first)
     */
    fun initialize(player: PlayerStats, enemies: List<Enemy>) {
        queue.clear()
        currentTurnIndex = 0

        // Add player
        queue.add(
            TurnEntry(
                entityId = PLAYER_ENTITY_ID,
                entityType = EntityType.PLAYER,
                speed = player.speed
            )
        )

        // Add enemies
        enemies.forEach { enemy ->
            queue.add(
                TurnEntry(
                    entityId = enemy.id,
                    entityType = EntityType.ENEMY,
                    speed = enemy.speed
                )
            )
        }

        // Sort by speed (descending) - fastest first
        queue.sortByDescending { it.speed }

        // Log turn order
        val turnOrder = queue.joinToString(" → ") { it.toString() }
        GameLogger.logSystem("⏳ $TAG: Turn order initialized: $turnOrder")
    }

    /**
     * Get current turn entity
     * Skips stunned entities automatically
     *
     * @return Current turn entry, or null if queue empty
     */
    fun getCurrentTurn(): TurnEntry? {
        if (queue.isEmpty()) {
            GameLogger.logSystem("⚠️ $TAG: Turn queue is empty!")
            return null
        }

        var attempts = 0
        val maxAttempts = queue.size

        // Find next non-stunned entity
        while (attempts < maxAttempts) {
            val current = queue[currentTurnIndex]

            if (current.isStunned) {
                GameLogger.logSystem("⏭️ $TAG: ${current.entityId} is stunned, skipping turn!")
                current.isStunned = false // Clear stun after skipping one turn
                nextTurn() // Move to next
                attempts++
            } else {
                GameLogger.logSystem("▶️ $TAG: Current turn: ${current.entityId} (Speed: ${current.speed})")
                return current
            }
        }

        // All entities stunned (edge case)
        GameLogger.logSystem("⚠️ $TAG: All entities stunned! Returning first entity.")
        return queue.firstOrNull()
    }

    /**
     * Advance to next turn (round-robin)
     */
    fun nextTurn() {
        if (queue.isEmpty()) {
            GameLogger.logSystem("⚠️ $TAG: Cannot advance turn, queue is empty!")
            return
        }

        currentTurnIndex = (currentTurnIndex + 1) % queue.size

        // Check if we completed a full round
        if (currentTurnIndex == 0) {
            GameLogger.logSystem("🔄 $TAG: Completed full round, starting new round!")
        }
    }

    /**
     * Remove dead enemy from queue
     *
     * @param enemyId ID of enemy to remove
     */
    fun removeEnemy(enemyId: String) {
        val removed = queue.removeAll { it.entityId == enemyId && it.entityType == EntityType.ENEMY }

        if (removed) {
            GameLogger.logSystem("💀 $TAG: Removed $enemyId from turn queue")

            // Adjust current turn index if needed
            if (currentTurnIndex >= queue.size && queue.isNotEmpty()) {
                currentTurnIndex = 0
                GameLogger.logSystem("🔄 $TAG: Adjusted turn index to 0 after removal")
            }
        } else {
            GameLogger.logSystem("⚠️ $TAG: Tried to remove $enemyId but not found in queue")
        }
    }

    /**
     * Stun entity for next turn
     *
     * @param entityId ID of entity to stun
     */
    fun stunEntity(entityId: String) {
        val entity = queue.find { it.entityId == entityId }

        if (entity != null) {
            entity.isStunned = true
            GameLogger.logSystem("😵 $TAG: ${entity.entityId} is now stunned!")
        } else {
            GameLogger.logSystem("⚠️ $TAG: Tried to stun $entityId but not found in queue")
        }
    }

    /**
     * Clear stun from entity
     *
     * @param entityId ID of entity to clear stun
     */
    fun clearStun(entityId: String) {
        val entity = queue.find { it.entityId == entityId }

        if (entity != null) {
            entity.isStunned = false
            GameLogger.logSystem("✅ $TAG: ${entity.entityId} is no longer stunned")
        }
    }

    /**
     * Check if entity is currently stunned
     */
    fun isStunned(entityId: String): Boolean {
        return queue.find { it.entityId == entityId }?.isStunned ?: false
    }

    /**
     * Get all entities in turn order (for UI display)
     */
    fun getTurnOrder(): List<TurnEntry> {
        return queue.toList()
    }

    /**
     * Check if player's turn
     */
    fun isPlayerTurn(): Boolean {
        val current = getCurrentTurn()
        return current?.entityType == EntityType.PLAYER
    }

    /**
     * Check if enemy's turn
     */
    fun isEnemyTurn(): Boolean {
        val current = getCurrentTurn()
        return current?.entityType == EntityType.ENEMY
    }

    /**
     * Get current enemy ID (if it's enemy turn)
     */
    fun getCurrentEnemyId(): String? {
        val current = getCurrentTurn()
        return if (current?.entityType == EntityType.ENEMY) current.entityId else null
    }

    /**
     * Get remaining entities count
     */
    fun getRemainingCount(): Int {
        return queue.size
    }

    /**
     * Check if queue is empty
     */
    fun isEmpty(): Boolean {
        return queue.isEmpty()
    }

    /**
     * Clear the entire queue
     */
    fun clear() {
        queue.clear()
        currentTurnIndex = 0
        GameLogger.logSystem("🧹 $TAG: Turn queue cleared")
    }

    /**
     * Debug: Print current turn queue state
     */
    fun debugPrintQueue() {
        GameLogger.logSystem("=== TURN QUEUE DEBUG ===")
        GameLogger.logSystem("Current Index: $currentTurnIndex / ${queue.size}")
        queue.forEachIndexed { index, entry ->
            val marker = if (index == currentTurnIndex) "➤" else " "
            GameLogger.logSystem("$marker $index: ${entry.entityId} (Speed: ${entry.speed}, Stunned: ${entry.isStunned})")
        }
        GameLogger.logSystem("========================")
    }
}

/**
 * Enemy extension: Add speed property
 * KURAL 11: Metadata field
 *
 * TODO-G95: Add speed field to Enemy data class
 * For now: Generate from enemy stats
 */
val Enemy.speed: Int
    get() {
        // TODO-G95: Use actual speed field from Enemy data class
        // For now: Calculate from attack (higher attack = faster)
        return attack + 10 // Base speed 10
    }
