package com.example.isekaikuroshin.engine

import android.content.Context
import com.example.isekaikuroshin.data.PersistentDataManager
import com.example.isekaikuroshin.utils.GameLogger
import dagger.hilt.android.qualifiers.ApplicationContext
// RACE CONDITION FIX: Startup Library ile artık bekleme gerekmediği için import kaldırıldı
// import kotlinx.coroutines.flow.first
// import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AIClientProvider - Reactive AI Client Factory
 *
 * KRİTİK SORUN ÇÖZ ÜMÜ (TODO-FIX-01):
 * Eski sistem: DatabaseModule.provideAIClient() her enjeksiyonda bir snapshot alıyordu.
 * Settings'de API key değiştirildiğinde, ViewModel'ler eski AIClient instance'ını kullanmaya devam ediyordu.
 *
 * Yeni sistem: Bu provider her AI çağrısında en güncel settings'i kontrol eder ve
 * gerekirse yeni bir AIClient oluşturur.
 *
 * Bu sayede:
 * - Journal ve Health Hub artık Settings'de girilen API key'i anında görür
 * - Kullanıcı Google → Local veya Local → Google geçiş yaptığında sistem otomatik adapte olur
 * - API key güncellemeleri için uygulama yeniden başlatmaya gerek kalmaz
 */
@Singleton
class AIClientProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val persistentDataManager: PersistentDataManager
) {

    init {
        android.util.Log.d("DEBUG_API_KEY", "=== INITIALIZATION START ===")
        android.util.Log.d("DEBUG_API_KEY", "Current thread: ${Thread.currentThread().name}")
        android.util.Log.d("DEBUG_API_KEY", "PersistentDataManager.isInitialized: ${persistentDataManager.isInitialized.value}")
        android.util.Log.d("DEBUG_API_KEY", "=== INITIALIZATION END ===")
    }

    // Cache mekanizması - aynı ayarlarla gereksiz yere yeni client oluşturmayı önler
    private var cachedProvider: String? = null
    private var cachedApiKey: String? = null
    private var cachedClient: AIClient? = null

    // Lokal model cache - MemoryManager için isteğe bağlı yükleme
    private var cachedLocalClient: AIClient? = null

    /**
     * RACE CONDITION FIX - Strateji 2: Startup Library garantisi
     * DataInitializer sayesinde PersistentDataManager her zaman hazır
     * Bekleme mekanizmasına gerek yok, direkt veriyi okuyabiliriz
     *
     * En güncel settings'e göre AIClient döndürür
     * Ayarlar değişmediyse cache'den döner, değiştiyse yeni client oluşturur
     */
    suspend fun getCurrentClient(): AIClient {
        // RACE CONDITION FIX: Startup Library ile PersistentDataManager her zaman hazır
        // Bekleme mekanizması artık gereksiz
        GameLogger.logSystem("=== AIClientProvider: Getting current client ===")
        GameLogger.logSystem("(Startup Library ensures PersistentDataManager is ready)")

        val currentSettings = persistentDataManager.gameData.value.settingsData.apiSettings
        val currentProvider = currentSettings.selectedProvider
        val currentApiKey = currentSettings.geminiApiKey // FIX: customAPIKey → geminiApiKey

        GameLogger.logSystem("Current provider: $currentProvider")
        GameLogger.logSystem("API key length: ${currentApiKey.length}")

        // Cache kontrolü - ayarlar değişmemişse mevcut client'ı döndür
        if (cachedProvider == currentProvider && cachedApiKey == currentApiKey && cachedClient != null) {
            GameLogger.logSystem("✅ Using cached AIClient (settings unchanged)")
            return cachedClient!!
        }

        // Ayarlar değişmiş veya ilk çağrı - yeni client oluştur
        GameLogger.logSystem("🔄 Settings changed or first call, creating new AIClient")
        GameLogger.logSystem("Old provider: $cachedProvider → New provider: $currentProvider")
        if (cachedProvider == "GOOGLE" || currentProvider == "GOOGLE") {
            GameLogger.logSystem("Old API key length: ${cachedApiKey?.length} → New API key length: ${currentApiKey.length}")
        }

        val newClient = createAIClient(currentProvider, currentApiKey)

        // Cache'i güncelle
        cachedProvider = currentProvider
        cachedApiKey = currentApiKey
        cachedClient = newClient

        GameLogger.logSystem("✅ New AIClient created and cached")
        return newClient
    }

    /**
     * Ayarlara göre uygun AIClient instance'ı oluşturur
     *
     * GÖREV #6-7 FIX: Auto-fallback mekanizması eklendi
     * - LOCAL model başarısız olursa otomatik GOOGLE API'ye geçer (geminiApiKey varsa)
     * - Timeout 5 saniyeye düşürüldü (eski: 30 saniye)
     */
    private suspend fun createAIClient(provider: String, apiKey: String): AIClient {
        GameLogger.logSystem("=== Creating AIClient ===")
        GameLogger.logSystem("Provider: $provider")

        return when (provider) {
            "GOOGLE" -> {
                if (apiKey.isNotBlank()) {
                    GameLogger.logSystem("✅ Creating GoogleAIClient with provided API key")
                    GoogleAIClient(apiKey)
                } else {
                    GameLogger.logSystem("⚠️ Google provider selected but API key is blank, using NoOpAIClient")

                    // G75 FIX: Kullanıcıya API key eksikliğini bildir
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        android.widget.Toast.makeText(
                            context,
                            "⚠️ Google AI seçildi ancak API Key eksik! Settings'ten API Key ekleyin.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }

                    NoOpAIClient()
                }
            }
            "LOCAL" -> {
                GameLogger.logSystem("✅ Attempting to create LocalAIClient (Gemini Nano via MediaPipe)")

                // LOCAL model yüklemeyi dene (timeout: 5s)
                val localClient = tryLoadLocalClient()

                if (localClient != null) {
                    GameLogger.logSystem("✅ LocalAIClient created successfully")
                    localClient
                } else {
                    // AUTO-FALLBACK: LOCAL başarısız, geminiApiKey varsa GOOGLE'a geç
                    val geminiApiKey = persistentDataManager.gameData.value.settingsData.apiSettings.geminiApiKey

                    if (geminiApiKey.isNotBlank()) {
                        GameLogger.logSystem("⚠️ LOCAL model failed, AUTO-FALLBACK to Google API")
                        GameLogger.logSystem("✅ Creating GoogleAIClient with geminiApiKey")
                        GoogleAIClient(geminiApiKey)
                    } else {
                        GameLogger.logSystem("❌ LOCAL model failed and no geminiApiKey available")
                        GameLogger.logSystem("⚠️ Using NoOpAIClient (no AI features)")

                        // G75 FIX: Kullanıcıya LOCAL model başarısızlığını ve API key eksikliğini bildir
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            android.widget.Toast.makeText(
                                context,
                                "⚠️ LOCAL AI yüklenemedi ve API Key yok! AI özellikleri çalışmayacak. Settings'ten API Key ekleyin.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }

                        NoOpAIClient()
                    }
                }
            }
            else -> {
                GameLogger.logSystem("⚠️ Unknown provider '$provider', using NoOpAIClient")

                // G75 FIX: Kullanıcıya bilinmeyen provider uyarısı
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(
                        context,
                        "⚠️ Bilinmeyen AI provider: '$provider'. AI özellikleri çalışmayacak.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }

                NoOpAIClient()
            }
        }
    }

    /**
     * LOCAL model yüklemeyi dener (30 saniye timeout)
     * TODO-G132: Timeout 5s → 30s artırıldı (yavaş cihazlar için)
     * @return LocalAIClient veya null (başarısız olursa)
     */
    private suspend fun tryLoadLocalClient(): AIClient? {
        return try {
            // GlobalAIManager kontrol et
            if (!com.example.isekaikuroshin.ai.GlobalAIManager.isModelInitialized.value) {
                GameLogger.logSystem("⏳ Local model not initialized, starting load...")
                com.example.isekaikuroshin.ai.GlobalAIManager.startAILoading()

                // 30 saniye bekle (G132 FIX: Kullanıcı talebi - 15s→30s)
                var waitTime = 0
                val maxWaitTime = 30000 // 30 saniye (eski: 15000)
                val checkInterval = 500L

                while (!com.example.isekaikuroshin.ai.GlobalAIManager.isModelInitialized.value && waitTime < maxWaitTime) {
                    kotlinx.coroutines.delay(checkInterval)
                    waitTime += checkInterval.toInt()
                }

                if (!com.example.isekaikuroshin.ai.GlobalAIManager.isModelInitialized.value) {
                    GameLogger.logSystem("❌ LOCAL model loading TIMEOUT after 30s")
                    return null
                }
            }

            GameLogger.logSystem("✅ Local model initialized, creating LocalAIClient")
            LocalAIClient(context)
        } catch (e: Exception) {
            GameLogger.logError("AIClientProvider", "❌ Exception while creating LocalAIClient", e)
            null
        }
    }

    /**
     * Cache'i temizler - test veya debug amaçlı
     */
    fun clearCache() {
        GameLogger.logSystem("🗑️ AIClientProvider cache cleared")
        cachedProvider = null
        cachedApiKey = null
        cachedClient = null
    }

    /**
     * Mevcut provider bilgisini döndürür - debug amaçlı
     */
    fun getCurrentProviderInfo(): String {
        val currentSettings = persistentDataManager.gameData.value.settingsData.apiSettings
        return "Provider: ${currentSettings.selectedProvider}, API Key: ${if (currentSettings.customAPIKey.isNotBlank()) "SET" else "NOT_SET"}"
    }

    /**
     * STRATEJI #3: İsteğe bağlı lokal model yükleme
     * Ana provider GOOGLE olsa bile, MemoryManager için lokal modeli yükler
     *
     * @return LocalAIClient veya null (yükleme başarısız olursa)
     */
    suspend fun getLocalClient(): AIClient? {
        GameLogger.logSystem("🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣")
        GameLogger.logSystem("🤖 [AI-CLIENT-PROVIDER] REQUESTING ON-DEMAND LOCAL CLIENT")
        GameLogger.logSystem("🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣")

        // Cache kontrolü - zaten yüklüyse direkt döndür
        GameLogger.logSystem("🔍 [AI-CLIENT-PROVIDER] Checking cache - cachedLocalClient: ${if (cachedLocalClient != null) "EXISTS" else "NULL"}")

        if (cachedLocalClient != null) {
            GameLogger.logSystem("✅ [AI-CLIENT-PROVIDER] Local client already cached - returning existing instance")
            GameLogger.logSystem("🔍 [AI-CLIENT-PROVIDER] Cached client type: ${cachedLocalClient?.javaClass?.simpleName}")
            return cachedLocalClient
        }

        GameLogger.logSystem("🔄 [AI-CLIENT-PROVIDER] Local client NOT cached - initiating model loading...")
        GameLogger.logSystem("📊 [AI-CLIENT-PROVIDER] Current GlobalAIManager status:")
        GameLogger.logSystem("   - isModelInitialized: ${com.example.isekaikuroshin.ai.GlobalAIManager.isModelInitialized.value}")
        GameLogger.logSystem("   - isLoading: ${com.example.isekaikuroshin.ai.GlobalAIManager.isLoading.value}")
        GameLogger.logSystem("   - loadingProgress: ${com.example.isekaikuroshin.ai.GlobalAIManager.loadingProgress.value}")

        // GlobalAIManager'ı kullanarak modeli yükle
        if (!com.example.isekaikuroshin.ai.GlobalAIManager.isModelInitialized.value) {
            GameLogger.logSystem("⏳ [AI-CLIENT-PROVIDER] Model NOT initialized - Starting local model loading...")

            try {
                GameLogger.logSystem("🔄 [AI-CLIENT-PROVIDER] Calling GlobalAIManager.startAILoading()...")
                com.example.isekaikuroshin.ai.GlobalAIManager.startAILoading()
                GameLogger.logSystem("✅ [AI-CLIENT-PROVIDER] startAILoading() called successfully")
            } catch (e: Exception) {
                GameLogger.logError("AIClientProvider", "❌ Exception during startAILoading!", e)
                GameLogger.logSystem("❌ Exception: ${e.message}")
                return null
            }

            // Model yüklenene kadar bekle (maksimum 60 saniye - G132 FIX #3)
            // 500MB model kopyalama + MediaPipe initialization ~15-30 saniye sürebilir
            var waitTime = 0
            val maxWaitTime = 60000 // 60 saniye (Kullanıcı talebi: yavaş cihazlar için)
            val checkInterval = 500L // 0.5 saniye
            GameLogger.logSystem("⏳ [AI-CLIENT-PROVIDER] Starting wait loop - Max wait: ${maxWaitTime/1000}s")

            while (!com.example.isekaikuroshin.ai.GlobalAIManager.isModelInitialized.value && waitTime < maxWaitTime) {
                kotlinx.coroutines.delay(checkInterval)
                waitTime += checkInterval.toInt()

                if (waitTime % 2000 == 0) { // Her 2 saniyede log
                    val progress = com.example.isekaikuroshin.ai.GlobalAIManager.loadingProgress.value
                    val status = com.example.isekaikuroshin.ai.GlobalAIManager.loadingStatus.value
                    GameLogger.logSystem("⏳ [AI-CLIENT-PROVIDER] Waiting for model... ${waitTime/1000}s elapsed")
                    GameLogger.logSystem("   - Progress: ${(progress * 100).toInt()}%")
                    GameLogger.logSystem("   - Status: $status")
                    GameLogger.logSystem("   - isModelInitialized: ${com.example.isekaikuroshin.ai.GlobalAIManager.isModelInitialized.value}")
                }
            }

            if (!com.example.isekaikuroshin.ai.GlobalAIManager.isModelInitialized.value) {
                GameLogger.logSystem("❌❌❌ [AI-CLIENT-PROVIDER] Local model loading TIMEOUT after ${waitTime/1000} seconds")
                GameLogger.logSystem("❌ Final status: ${com.example.isekaikuroshin.ai.GlobalAIManager.loadingStatus.value}")
                GameLogger.logSystem("❌ Final progress: ${com.example.isekaikuroshin.ai.GlobalAIManager.loadingProgress.value}")
                GameLogger.logSystem("❌ Local client will NOT be available")
                GameLogger.logSystem("🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣")
                return null
            }

            GameLogger.logSystem("✅✅✅ [AI-CLIENT-PROVIDER] Local model loaded successfully after ${waitTime/1000} seconds")
        } else {
            GameLogger.logSystem("✅ [AI-CLIENT-PROVIDER] Model already initialized - skipping load")
        }

        // LocalAIClient oluştur ve cache'le
        try {
            GameLogger.logSystem("🔧 [AI-CLIENT-PROVIDER] Creating LocalAIClient instance...")
            GameLogger.logSystem("🔧 [AI-CLIENT-PROVIDER] Context: ${context.javaClass.simpleName}")

            val localClient = LocalAIClient(context)

            GameLogger.logSystem("✅ [AI-CLIENT-PROVIDER] LocalAIClient instance created")
            GameLogger.logSystem("🔍 [AI-CLIENT-PROVIDER] Client type: ${localClient.javaClass.simpleName}")

            cachedLocalClient = localClient

            GameLogger.logSystem("✅✅✅ [AI-CLIENT-PROVIDER] LocalAIClient created and cached successfully")
            GameLogger.logSystem("🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣")
            return localClient
        } catch (e: Exception) {
            GameLogger.logError("AIClientProvider", "❌❌❌ EXCEPTION while creating LocalAIClient!", e)
            GameLogger.logSystem("❌ Exception type: ${e.javaClass.simpleName}")
            GameLogger.logSystem("❌ Exception message: ${e.message}")
            GameLogger.logSystem("❌ Stack trace: ${e.stackTraceToString()}")
            GameLogger.logSystem("❌ LocalAIClient creation FAILED")
            GameLogger.logSystem("🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣🟣")
            return null
        }
    }
}
