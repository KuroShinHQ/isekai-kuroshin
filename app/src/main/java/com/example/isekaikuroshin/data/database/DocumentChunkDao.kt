package com.example.isekaikuroshin.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Data Access Object for DocumentChunkEntity
 * Provides database operations for document chunks
 */
@Dao
interface DocumentChunkDao {

    /**
     * Inserts a list of document chunks into the database
     * Used by DocumentProcessingWorker to bulk insert chunks
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chunks: List<DocumentChunkEntity>)

    /**
     * Retrieves all chunks for a specific document URI
     * Ordered by chunk index for proper text reconstruction
     */
    @Query("SELECT * FROM document_chunks WHERE sourceDocumentUri = :uri ORDER BY chunkIndex ASC")
    suspend fun getChunksByUri(uri: String): List<DocumentChunkEntity>

    /**
     * Deletes all chunks for a specific document URI
     * Useful when a document is removed or re-processed
     */
    @Query("DELETE FROM document_chunks WHERE sourceDocumentUri = :uri")
    suspend fun deleteChunksByUri(uri: String)

    /**
     * Gets the total number of chunks in the database
     * Useful for debugging and statistics
     */
    @Query("SELECT COUNT(*) FROM document_chunks")
    suspend fun getTotalChunkCount(): Int

    /**
     * Retrieves all chunks from all documents
     * Useful for testing and debugging
     */
    @Query("SELECT * FROM document_chunks ORDER BY sourceDocumentUri, chunkIndex")
    suspend fun getAllChunks(): List<DocumentChunkEntity>
}