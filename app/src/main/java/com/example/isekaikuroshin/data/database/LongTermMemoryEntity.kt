package com.example.isekaikuroshin.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing long-term memory summaries
 * Used by MemoryManager for hierarchical memory architecture
 */
@Entity(tableName = "long_term_memories")
data class LongTermMemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val day: Int,
    val summaryText: String
)
