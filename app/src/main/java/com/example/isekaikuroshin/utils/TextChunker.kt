package com.example.isekaikuroshin.utils

/**
 * Utility class for splitting large texts into manageable chunks
 * Designed for AI processing and database storage optimization
 */
object TextChunker {

    /**
     * Default chunk size optimized for AI processing
     * 1800 characters provides good balance between context and processing efficiency
     */
    const val DEFAULT_CHUNK_SIZE = 1800

    /**
     * Splits text into fixed-size chunks for consistent AI processing
     *
     * @param text The input text to be chunked
     * @param chunkSize The maximum size of each chunk in characters (default: 1800)
     * @return List of text chunks, each with maximum size of chunkSize
     *
     * Example:
     * - Input: "Hello World! This is a long text..."
     * - Output: ["Hello World! This...", "...is a long text..."]
     */
    fun chunkTextFixedSize(text: String, chunkSize: Int = DEFAULT_CHUNK_SIZE): List<String> {
        // Handle edge cases
        if (text.isEmpty()) return emptyList()
        if (text.length <= chunkSize) return listOf(text)

        // Split text into fixed-size chunks
        return text.chunked(chunkSize)
    }

    /**
     * Gets information about chunking result
     * Useful for logging and debugging
     */
    fun getChunkInfo(text: String, chunkSize: Int = DEFAULT_CHUNK_SIZE): ChunkInfo {
        val chunks = chunkTextFixedSize(text, chunkSize)
        return ChunkInfo(
            originalLength = text.length,
            chunkCount = chunks.size,
            chunkSize = chunkSize,
            lastChunkSize = chunks.lastOrNull()?.length ?: 0
        )
    }

    /**
     * Data class containing information about chunking operation
     */
    data class ChunkInfo(
        val originalLength: Int,
        val chunkCount: Int,
        val chunkSize: Int,
        val lastChunkSize: Int
    )
}