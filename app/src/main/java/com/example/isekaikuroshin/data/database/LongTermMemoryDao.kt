package com.example.isekaikuroshin.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO for long-term memory operations
 */
@Dao
interface LongTermMemoryDao {

    /**
     * Insert a new memory summary
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: LongTermMemoryEntity)

    /**
     * Get recent N memories for context building
     */
    @Query("SELECT * FROM long_term_memories ORDER BY day DESC LIMIT :limit")
    suspend fun getRecentMemories(limit: Int): List<LongTermMemoryEntity>

    /**
     * Get memory for a specific day
     */
    @Query("SELECT * FROM long_term_memories WHERE day = :day")
    suspend fun getMemoryByDay(day: Int): LongTermMemoryEntity?

    /**
     * Delete all memories
     */
    @Query("DELETE FROM long_term_memories")
    suspend fun deleteAllMemories()
}
