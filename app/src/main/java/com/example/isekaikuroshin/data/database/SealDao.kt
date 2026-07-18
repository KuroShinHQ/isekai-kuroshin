package com.example.isekaikuroshin.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Seal entities
 *
 * Kadim Mühür Teknikleri sistemi için database operations.
 */
@Dao
interface SealDao {

    /**
     * Get all seals
     */
    @Query("SELECT * FROM seals")
    fun getAllSeals(): Flow<List<SealEntity>>

    /**
     * Get seal by ID
     */
    @Query("SELECT * FROM seals WHERE id = :sealId")
    suspend fun getSealById(sealId: String): SealEntity?

    /**
     * Get seals for player level
     *
     * Returns seals that meet the minPlayerLevel requirement
     */
    @Query("SELECT * FROM seals WHERE minPlayerLevel <= :playerLevel")
    suspend fun getSealsForLevel(playerLevel: Int): List<SealEntity>

    /**
     * Get seals by difficulty
     */
    @Query("SELECT * FROM seals WHERE difficulty = :difficulty")
    suspend fun getSealsByDifficulty(difficulty: String): List<SealEntity>

    /**
     * Get seals by tier
     */
    @Query("SELECT * FROM seals WHERE tier = :tier")
    suspend fun getSealsByTier(tier: String): List<SealEntity>

    /**
     * Get mastered seals (masteryLevel >= 100)
     */
    @Query("SELECT * FROM seals WHERE masteryLevel >= 100")
    suspend fun getMasteredSeals(): List<SealEntity>

    /**
     * Get seals in progress (masteryLevel > 0 AND masteryLevel < 100)
     */
    @Query("SELECT * FROM seals WHERE masteryLevel > 0 AND masteryLevel < 100")
    suspend fun getSealsInProgress(): List<SealEntity>

    /**
     * Insert or update seal
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(seal: SealEntity)

    /**
     * Insert multiple seals
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(seals: List<SealEntity>)

    /**
     * Update seal
     */
    @Update
    suspend fun update(seal: SealEntity)

    /**
     * Delete seal
     */
    @Delete
    suspend fun delete(seal: SealEntity)

    /**
     * Delete all seals (for testing/reset)
     */
    @Query("DELETE FROM seals")
    suspend fun deleteAll()

    /**
     * Get seal count
     */
    @Query("SELECT COUNT(*) FROM seals")
    suspend fun getSealCount(): Int

    /**
     * Update seal mastery level
     */
    @Query("UPDATE seals SET masteryLevel = :masteryLevel WHERE id = :sealId")
    suspend fun updateMasteryLevel(sealId: String, masteryLevel: Int)

    /**
     * Update seal's acceptance threshold
     */
    @Query("UPDATE seals SET acceptanceThreshold = :threshold WHERE id = :sealId")
    suspend fun updateAcceptanceThreshold(sealId: String, threshold: Float)

    /**
     * Get seal by ID as Flow (for real-time updates)
     */
    @Query("SELECT * FROM seals WHERE id = :sealId")
    fun getSealByIdFlow(sealId: String): Flow<SealEntity?>
}
