package com.example.isekaikuroshin.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity for storing document text chunks
 * Used to store extracted and chunked text from uploaded documents (TXT/PDF)
 */
@Entity(tableName = "document_chunks")
data class DocumentChunkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * URI of the source document this chunk came from
     * Links the chunk back to its original document
     */
    val sourceDocumentUri: String,

    /**
     * Index/order of this chunk within the document
     * 0-based indexing (first chunk = 0, second = 1, etc.)
     */
    val chunkIndex: Int,

    /**
     * The actual text content of this chunk
     * Fixed size chunks for consistent AI processing
     */
    val content: String
)