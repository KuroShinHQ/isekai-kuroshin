package com.example.isekaikuroshin.di

import android.content.Context
import androidx.room.Room
import com.example.isekaikuroshin.data.database.AppDatabase
import com.example.isekaikuroshin.data.database.GameStateDao
import com.example.isekaikuroshin.data.database.DocumentChunkDao
import com.example.isekaikuroshin.data.database.DynamicNPCDao
import com.example.isekaikuroshin.data.database.SettlementDao
import com.example.isekaikuroshin.data.database.FactionDao
import com.example.isekaikuroshin.data.PersistentDataManager
import com.example.isekaikuroshin.engine.AIClient
import com.example.isekaikuroshin.engine.AIClientProvider
import com.example.isekaikuroshin.engine.GoogleAIClient
import com.example.isekaikuroshin.engine.LocalAIClient
import com.example.isekaikuroshin.engine.NoOpAIClient
import com.example.isekaikuroshin.engine.BasicStoryEngine
import com.example.isekaikuroshin.data.GameStateManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing database dependencies
 * Manages Room database and DAO instances across the application
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides the main application database
     * Singleton ensures only one instance exists throughout the app lifecycle
     */
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // For development - remove in production
            .build()
    }

    /**
     * Provides GameStateDao from the database
     * This DAO will be injected into GameStateManager
     */
    @Provides
    fun provideGameStateDao(database: AppDatabase): GameStateDao {
        return database.gameStateDao()
    }

    /**
     * Provides DocumentChunkDao from the database
     * This DAO will be injected into GameMasterEngine
     */
    @Provides
    fun provideDocumentChunkDao(database: AppDatabase): DocumentChunkDao {
        return database.documentChunkDao()
    }

    /**
     * Provides DynamicNPCDao from the database
     * This DAO will be injected into WorldUpdateEngine and GameMasterEngine
     */
    @Provides
    fun provideDynamicNPCDao(database: AppDatabase): DynamicNPCDao {
        return database.dynamicNpcDao()
    }

    /**
     * Provides SettlementDao from the database
     * This DAO will be injected into WorldUpdateEngine
     */
    @Provides
    fun provideSettlementDao(database: AppDatabase): SettlementDao {
        return database.settlementDao()
    }

    /**
     * Provides FactionDao from the database
     * This DAO will be injected into WorldUpdateEngine and GameMasterEngine
     */
    @Provides
    fun provideFactionDao(database: AppDatabase): FactionDao {
        return database.factionDao()
    }

    /**
     * Provides SealDao from the database
     * This DAO will be injected into SealRepository
     */
    @Provides
    fun provideSealDao(database: AppDatabase): com.example.isekaikuroshin.data.database.SealDao {
        return database.sealDao()
    }

    /**
     * Provides PoseTemplateDao from the database
     * This DAO will be injected into PoseTemplateRepository
     */
    @Provides
    fun providePoseTemplateDao(database: AppDatabase): com.example.isekaikuroshin.data.database.PoseTemplateDao {
        return database.poseTemplateDao()
    }

    /**
     * Provides PersistentDataManager singleton
     * This is a Kotlin object, so we just return the singleton instance
     */
    @Provides
    @Singleton
    fun providePersistentDataManager(): PersistentDataManager {
        return PersistentDataManager
    }

    /**
     * Provides BasicStoryEngine singleton
     * This engine will be injected into GameMasterEngine
     */
    @Provides
    @Singleton
    fun provideBasicStoryEngine(
        @ApplicationContext context: Context,
        gameStateManager: GameStateManager
    ): BasicStoryEngine {
        return BasicStoryEngine(context, gameStateManager)
    }

    /**
     * Provides AIClientProvider - a reactive wrapper that always returns the latest AIClient
     * based on current settings from PersistentDataManager
     *
     * KRİTİK DÜZELTME: Eski kod her enjeksiyonda static bir AIClient döndürüyordu.
     * Settings'de API key değiştirilse bile, ViewModel'ler eski AIClient'ı kullanmaya devam ediyordu.
     * Yeni kod: Her AI çağrısında en güncel settings'i kontrol eden bir Provider kullanıyor.
     */
    @Provides
    @Singleton
    fun provideAIClientProvider(
        @ApplicationContext context: Context,
        persistentDataManager: PersistentDataManager
    ): AIClientProvider {
        return AIClientProvider(context, persistentDataManager)
    }

    /**
     * DEPRECATED: Legacy support - will be removed in future versions
     * Use AIClientProvider.getCurrentClient() instead
     *
     * HATA #1 FIX: getCurrentClient() artık suspend olduğu için runBlocking kullanıyoruz
     * NOT: Bu provider yalnızca geriye dönük uyumluluk için. Yeni kod AIClientProvider kullanmalı.
     */
    @Provides
    fun provideAIClient(
        aiClientProvider: AIClientProvider
    ): AIClient {
        // HATA #1 FIX: Suspend function'ı runBlocking ile çağır
        return kotlinx.coroutines.runBlocking {
            aiClientProvider.getCurrentClient()
        }
    }

    /**
     * Provides AuthManager for future Google Auth integration
     * Currently provides stub implementation for preparation
     */
    @Provides
    @Singleton
    fun provideAuthManager(): com.example.isekaikuroshin.auth.AuthManager {
        return com.example.isekaikuroshin.auth.AuthManager()
    }
}