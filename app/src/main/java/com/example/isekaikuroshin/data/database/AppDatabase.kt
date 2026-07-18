package com.example.isekaikuroshin.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context

/**
 * Main Room Database for Isekai Kuroshin
 * Contains all entities and DAOs for the application
 */
@Database(
    entities = [
        GameStateEntity::class,
        DocumentChunkEntity::class,
        DynamicNPCEntity::class,
        SettlementEntity::class,
        FactionEntity::class,
        DynamicContentEntity::class,  // GM-generated eşsiz içerikler
        LongTermMemoryEntity::class,  // Strateji #3 hafıza özetleri
        SealEntity::class,  // Kadim Mühür Teknikleri
        PoseTemplateEntity::class  // Fiziksel Antrenman Kalibrasyon
    ],
    version = 12,  // G87 FIX: SealEntity schema değişti (name/description/loreText → nameKey/descriptionKey/loreTextKey)
    exportSchema = false
)
@TypeConverters(Converters::class) // Uses Converters from TypeConverters.kt
abstract class AppDatabase : RoomDatabase() {

    /**
     * Provides access to GameStateDao
     */
    abstract fun gameStateDao(): GameStateDao

    /**
     * Provides access to DocumentChunkDao
     */
    abstract fun documentChunkDao(): DocumentChunkDao

    /**
     * Provides access to DynamicNPCDao
     */
    abstract fun dynamicNpcDao(): DynamicNPCDao

    /**
     * Provides access to SettlementDao
     */
    abstract fun settlementDao(): SettlementDao

    /**
     * Provides access to FactionDao
     */
    abstract fun factionDao(): FactionDao

    /**
     * Provides access to DynamicContentDao (GM-generated eşsiz içerikler)
     */
    abstract fun dynamicContentDao(): DynamicContentDao

    /**
     * Provides access to LongTermMemoryDao (Strateji #3 hafıza özetleri)
     */
    abstract fun longTermMemoryDao(): LongTermMemoryDao

    /**
     * Provides access to SealDao (Kadim Mühür Teknikleri)
     */
    abstract fun sealDao(): SealDao

    /**
     * Provides access to PoseTemplateDao (Fiziksel Antrenman Kalibrasyon)
     */
    abstract fun poseTemplateDao(): PoseTemplateDao

    companion object {
        const val DATABASE_NAME = "isekai_kuroshin_database"

        /**
         * Creates and returns a Room database instance
         * This method is typically called from the Hilt module
         */
        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration() // For development - removes this in production
                .build()
        }
    }
}