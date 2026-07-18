package com.example.isekaikuroshin

import android.app.Application
import android.util.Log
import com.example.isekaikuroshin.ai.GlobalAIManager
import com.example.isekaikuroshin.data.database.AppDatabase
import com.example.isekaikuroshin.utils.GameLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
// import com.tom_roush.pdfbox.util.PDFBoxResourceLoader // Temporarily commented out
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import com.example.isekaikuroshin.engine.AIClientProvider
import com.example.isekaikuroshin.game.GameStateManager

/**
 * Custom Application class for global initialization
 * Manages application-level singletons and services
 */
@HiltAndroidApp
class IsekaiKuroshinApplication : Application() {

    @Inject
    lateinit var database: AppDatabase

    @Inject
    lateinit var aiClientProvider: AIClientProvider

    @Inject
    lateinit var gameStateManager: GameStateManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        Log.d("IsekaiKuroshinApplication", "Application starting...")

        // Initialize Game Logger FIRST
        GameLogger.initialize(this)
        GameLogger.logSession("IsekaiKuroshin Application started")

        // Initialize PDFBox Resource Loader - Temporarily disabled
        // try {
        //     PDFBoxResourceLoader.init(this)
        //     GameLogger.logSession("PDFBoxResourceLoader initialized successfully")
        // } catch (e: Exception) {
        //     GameLogger.logError("PDFBox", "PDFBoxResourceLoader initialization failed", e)
        // }

        // Initialize Global AI Manager
        GlobalAIManager.initialize(this)
        GameLogger.logSession("GlobalAIManager initialized")

        // Initialize MemoryManager with database (Strateji #3)
        // GÜNCELLEME: Dile duyarlı hibrit AI mimarisi
        applicationScope.launch {
            try {
                GlobalAIManager.initializeMemoryManager(
                    context = this@IsekaiKuroshinApplication,
                    database = database,
                    aiClientProvider = aiClientProvider,
                    gameStateManager = gameStateManager
                )
                GameLogger.logSession("MemoryManager initialized with hybrid AI architecture (Strateji #3)")
            } catch (e: Exception) {
                GameLogger.logError("IsekaiKuroshinApplication", "MemoryManager initialization failed", e)
            }
        }

        Log.d("IsekaiKuroshinApplication", "Application initialized successfully")
        GameLogger.logSession("Application initialization completed")
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.d("IsekaiKuroshinApplication", "Application terminating...")

        // Clean up AI resources
        GlobalAIManager.cleanup()
    }
}