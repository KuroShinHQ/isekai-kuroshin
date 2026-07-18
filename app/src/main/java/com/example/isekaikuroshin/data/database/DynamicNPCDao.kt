package com.example.isekaikuroshin.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for Dynamic NPC State operations
 *
 * Provides database operations for managing evolving NPC states
 * in the Nemesis System.
 */
@Dao
interface DynamicNPCDao {

    /**
     * Get NPC state by ID
     */
    @Query("SELECT * FROM dynamic_npc_state WHERE npcId = :npcId")
    suspend fun getNpcStateById(npcId: String): DynamicNPCEntity?

    /**
     * Get all NPC states
     */
    @Query("SELECT * FROM dynamic_npc_state")
    suspend fun getAllNpcStates(): List<DynamicNPCEntity>

    /**
     * Get all alive NPCs
     */
    @Query("SELECT * FROM dynamic_npc_state WHERE isAlive = 1")
    suspend fun getAllAliveNpcs(): List<DynamicNPCEntity>

    /**
     * Get NPCs by nemesis level
     */
    @Query("SELECT * FROM dynamic_npc_state WHERE nemesisLevel >= :minLevel AND isAlive = 1")
    suspend fun getNpcsByNemesisLevel(minLevel: Int): List<DynamicNPCEntity>

    /**
     * Get NPCs at specific location
     */
    @Query("SELECT * FROM dynamic_npc_state WHERE currentLocationId = :locationId AND isAlive = 1")
    suspend fun getNpcsAtLocation(locationId: String): List<DynamicNPCEntity>

    /**
     * Get NPCs with specific loyalty range toward player
     */
    @Query("SELECT * FROM dynamic_npc_state WHERE loyaltyToPlayer BETWEEN :minLoyalty AND :maxLoyalty AND isAlive = 1")
    suspend fun getNpcsByLoyaltyRange(minLoyalty: Int, maxLoyalty: Int): List<DynamicNPCEntity>

    /**
     * Insert or update NPC state
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNpcState(npcState: DynamicNPCEntity)

    /**
     * Insert or update multiple NPC states
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNpcStates(npcStates: List<DynamicNPCEntity>)

    /**
     * Update NPC state
     */
    @Update
    suspend fun updateNpcState(npcState: DynamicNPCEntity)

    /**
     * Delete NPC state
     */
    @Delete
    suspend fun deleteNpcState(npcState: DynamicNPCEntity)

    /**
     * Delete NPC state by ID
     */
    @Query("DELETE FROM dynamic_npc_state WHERE npcId = :npcId")
    suspend fun deleteNpcStateById(npcId: String)

    /**
     * Update NPC level
     */
    @Query("UPDATE dynamic_npc_state SET level = :newLevel, lastUpdateTime = :timestamp WHERE npcId = :npcId")
    suspend fun updateNpcLevel(npcId: String, newLevel: Int, timestamp: Long)

    /**
     * Update NPC loyalty
     */
    @Query("UPDATE dynamic_npc_state SET loyaltyToPlayer = :newLoyalty, lastUpdateTime = :timestamp WHERE npcId = :npcId")
    suspend fun updateNpcLoyalty(npcId: String, newLoyalty: Int, timestamp: Long)

    /**
     * Update NPC nemesis level
     */
    @Query("UPDATE dynamic_npc_state SET nemesisLevel = :newNemesisLevel, lastUpdateTime = :timestamp WHERE npcId = :npcId")
    suspend fun updateNemesisLevel(npcId: String, newNemesisLevel: Int, timestamp: Long)

    /**
     * Update NPC location
     */
    @Query("UPDATE dynamic_npc_state SET currentLocationId = :newLocationId, lastUpdateTime = :timestamp WHERE npcId = :npcId")
    suspend fun updateNpcLocation(npcId: String, newLocationId: String, timestamp: Long)

    /**
     * Mark NPC as dead
     */
    @Query("UPDATE dynamic_npc_state SET isAlive = 0, isDead = 1, lastUpdateTime = :timestamp WHERE npcId = :npcId")
    suspend fun markNpcAsDead(npcId: String, timestamp: Long)

    /**
     * Mark NPC as exiled
     */
    @Query("UPDATE dynamic_npc_state SET isExiled = 1, lastUpdateTime = :timestamp WHERE npcId = :npcId")
    suspend fun markNpcAsExiled(npcId: String, timestamp: Long)

    /**
     * Increment interaction count
     */
    @Query("UPDATE dynamic_npc_state SET interactionCount = interactionCount + 1, timesSeen = timesSeen + 1, lastSeenDate = :timestamp, lastUpdateTime = :timestamp WHERE npcId = :npcId")
    suspend fun incrementInteractionCount(npcId: String, timestamp: Long)

    /**
     * Increment times defeated
     */
    @Query("UPDATE dynamic_npc_state SET timesDefeated = timesDefeated + 1, lastUpdateTime = :timestamp WHERE npcId = :npcId")
    suspend fun incrementTimesDefeated(npcId: String, timestamp: Long)

    /**
     * Increment times helped
     */
    @Query("UPDATE dynamic_npc_state SET timesHelped = timesHelped + 1, lastUpdateTime = :timestamp WHERE npcId = :npcId")
    suspend fun incrementTimesHelped(npcId: String, timestamp: Long)

    /**
     * Get NPCs that haven't been seen for a while (for world evolution)
     */
    @Query("SELECT * FROM dynamic_npc_state WHERE lastSeenDate < :cutoffTime AND isAlive = 1")
    suspend fun getNpcsNotSeenSince(cutoffTime: Long): List<DynamicNPCEntity>

    /**
     * Observable flow for all NPC states (for UI observation)
     */
    @Query("SELECT * FROM dynamic_npc_state")
    fun observeAllNpcStates(): Flow<List<DynamicNPCEntity>>

    /**
     * Observable flow for NPCs at specific location
     */
    @Query("SELECT * FROM dynamic_npc_state WHERE currentLocationId = :locationId AND isAlive = 1")
    fun observeNpcsAtLocation(locationId: String): Flow<List<DynamicNPCEntity>>
}