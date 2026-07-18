package com.example.isekaikuroshin.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for GameState operations
 * Provides methods to persist and retrieve game state from Room database
 */
@Dao
interface GameStateDao {

    /**
     * Insert or update the game state
     * Uses REPLACE strategy to overwrite existing data with the same primary key
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(gameState: GameStateEntity)

    /**
     * Get the current game state as a Flow for reactive updates
     * Returns null if no game state exists in database
     */
    @Query("SELECT * FROM game_state WHERE id = 1")
    fun getGameState(): Flow<GameStateEntity?>

    /**
     * Get the current game state synchronously (for initialization)
     * Returns null if no game state exists in database
     */
    @Query("SELECT * FROM game_state WHERE id = 1")
    suspend fun getGameStateSync(): GameStateEntity?

    /**
     * Delete all game state data (for reset/new game scenarios)
     */
    @Query("DELETE FROM game_state")
    suspend fun deleteAllGameState()

    /**
     * Check if any game state exists in the database
     */
    @Query("SELECT COUNT(*) FROM game_state")
    suspend fun hasGameState(): Int

    /**
     * Update only player state for performance optimization
     * Can be used for frequent stat updates without full game state replacement
     */
    @Query("""
        UPDATE game_state
        SET playerState = :playerStateJson, lastActionTime = :lastActionTime
        WHERE id = 1
    """)
    suspend fun updatePlayerStats(playerStateJson: String, lastActionTime: Long) // Parameter name also updated for clarity

    /**
     * Update only resources for performance optimization
     */
    @Query("""
        UPDATE game_state
        SET resources = :resourcesJson, lastActionTime = :lastActionTime
        WHERE id = 1
    """)
    suspend fun updateResources(resourcesJson: String, lastActionTime: Long)

    /**
     * Update time and location data
     */
    @Query("""
        UPDATE game_state
        SET currentDay = :currentDay,
            currentTimeOfDay = :currentTimeOfDay,
            currentLocationId = :currentLocationId,
            lastActionTime = :lastActionTime
        WHERE id = 1
    """)
    suspend fun updateTimeAndLocation(
        currentDay: Int,
        currentTimeOfDay: String, // Kept as String as per original, assuming TimeOfDay is converted by Room/TypeConverter implicitly for SET
        currentLocationId: String,
        lastActionTime: Long
    )
}