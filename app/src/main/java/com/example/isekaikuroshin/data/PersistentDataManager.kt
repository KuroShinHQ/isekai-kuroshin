package com.example.isekaikuroshin.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Serializable
data class PersistentGameData(
    val playerData: PlayerPersistentData = PlayerPersistentData(),
    val storyData: StoryPersistentData = StoryPersistentData(),
    val catalogData: CharacterCatalogData = CharacterCatalogData(),
    val questData: QuestData = QuestData(),
    val nemesisData: NemesisData = NemesisData(),  // G77: Nemesis tracking
    val settingsData: SettingsData = SettingsData(),
    val deathArchive: List<DeathRecord> = emptyList(),
    val externalStorySource: String = "", // URI for GameMasterEngine document chunks
    val usageStats: UsageStatsData = UsageStatsData(),
    val dynamicContent: DynamicContentLibrary = DynamicContentLibrary(),  // GM-generated eşsiz içerikler
    val analyticsData: UserAnalyticsData = UserAnalyticsData()  // GÖREV I: Session tracking ve peak hours
)

@Serializable
data class PlayerPersistentData(
    val name: String = "",
    val age: Int = 18,
    val gender: String = "Belirtilmemiş",
    val isAlive: Boolean = true,
    val level: Int = 1,
    val experience: Long = 0,
    val moralityScore: Float = 0.0f,
    val stats: PlayerStatsPersistent = PlayerStatsPersistent(),
    val inventory: List<InventoryItemPersistent> = emptyList(),
    val equippedItems: Map<String, String> = emptyMap(), // slot -> item id
    val skills: List<SkillPersistent> = emptyList(),  // Kazanılan yetenekler (core data only)
    // GÖREV N - FAZ 1: Öğrenilmiş ve kişiselleştirilmiş büyüler
    val learnedSpells: List<com.example.isekaikuroshin.data.combat.LearnedSpell> = emptyList(),
    // GÖREV 2: Death Archive için ek field'lar
    val activeTitles: List<String> = emptyList(), // Aktif ünvanlar
    val achievements: List<String> = emptyList(), // Kazanılan başarımlar
    // GÖREV 4: Element Affinity Sistemi
    val elementAffinities: ElementAffinityContainer = ElementAffinityContainer(),
    // G82: Status Effect System (Buff/Debuff)
    val activeStatusEffects: List<ActiveStatusEffect> = emptyList()
)

@Serializable
data class PlayerStatsPersistent(
    val hp: Long = 100,
    val maxHp: Long = 100,
    val mp: Long = 100,
    val maxMp: Long = 100,
    val stamina: Long = 100,
    val maxStamina: Long = 100,
    val hunger: Long = 100,
    val maxHunger: Long = 100,
    val fatigue: Long = 0,
    val strength: Long = 10,
    val agility: Long = 10,
    val intelligence: Long = 10,
    val vitality: Long = 10,
    val spirit: Long = 10,
    val luck: Long = 10,
    val elementAffinities: Map<String, Int> = emptyMap()  // ElementType.name -> affinity level (0-100)
)

@Serializable
data class InventoryItemPersistent(
    val id: String,
    val name: String,
    val description: String,
    val rarity: String,
    val type: String,
    val slot: String,
    val weight: Float,
    val durability: Int,
    val stats: Map<String, Float> = emptyMap()  // StatType.name -> bonus value (FIXED: String -> Float)
)

@Serializable
data class StoryPersistentData(
    val currentDay: Int = 1,
    val currentPage: Int = 1,
    val storyPages: List<String> = emptyList(),
    val currentLocation: String = "AT_ARABASI",
    val currentSeason: String = "SPRING",
    val currentWeather: String = "SUNNY",
    val totalPlayTime: Long = 0, // in milliseconds
    val dynamicPlaylist: List<String> = emptyList(),
    val dynamicPhotoPlaylist: List<Int> = emptyList()
)

@Serializable
data class CharacterCatalogData(
    val knownCharacters: List<CharacterEntry> = emptyList()
)

@Serializable
data class CharacterEntry(
    val id: String,
    val name: String,
    val age: Int = 0,
    val personality: String = "",
    val appearance: String = "",
    val backstory: String = "",
    val relationship: Float = 0.0f, // -1.0 to 1.0
    val firstMetDay: Int = 1,
    val lastInteractionDay: Int = 1,
    val location: String = "",
    val isAlive: Boolean = true
)

@Serializable
data class QuestData(
    val activeQuests: List<QuestEntry> = emptyList(),
    val completedQuests: List<QuestEntry> = emptyList()
)

/**
 * G77: Nemesis system persistent storage
 */
@Serializable
data class NemesisData(
    val activeNemeses: List<NemesisEntry> = emptyList()
)

/**
 * G77: NemesisEntry - Stores persistent data about a nemesis character
 * Tracks NPCs that have evolved into nemeses through player interactions
 */
@Serializable
data class NemesisEntry(
    val npcId: String,                          // Original NPC ID
    val nemesisLevel: Int,                      // 1-5 (escalation tier)
    val vendettaReason: String,                 // Why they became a nemesis
    val adaptationTraits: List<String>,         // Special traits they developed
    val evolutionDay: Int,                      // Game day they evolved
    val evolutionDescription: String,           // Dramatic narrative of evolution
    val lastEncounteredDay: Int = -1           // Last time player encountered them
)

@Serializable
data class QuestEntry(
    val id: String,
    val title: String,
    val description: String,
    val giver: String, // character who gave the quest
    val startDay: Int,
    val dueDay: Int = -1, // -1 means no deadline
    val progress: Float = 0.0f, // 0.0 to 1.0
    val isCompleted: Boolean = false,
    val rewards: List<String> = emptyList(),
    val isDeveloperQuest: Boolean = false // true = Ana Görev (hardcoded), false = Yan Görev (GM generated)
)

/**
 * SkillPersistent - Core skill data (minimal, GM'i yormadan)
 * StatBonuses vs. runtime hesaplanır, sadece progress data saklanır
 */
@Serializable
data class SkillPersistent(
    val id: String,
    val name: String,
    val level: Int = 0,
    val currentXP: Int = 0,
    val rarity: String = "F",  // SkillRarity enum name
    val elementType: String = "NEUTRAL",  // ElementType enum name
    val usageCount: Int = 0,  // Rarity progression için
    val tier: String = "Temel"  // Varyasyon seviyesi
)

/**
 * DynamicContentLibrary - GM'in ürettiği eşsiz içeriklerin kütüphanesi
 * Kullanıcı beğendiklerini export eder, yeni oyunda import edilir
 * GM bu hazır verileri kullanır (token tasarrufu)
 */
@Serializable
data class DynamicContentLibrary(
    val contents: List<DynamicContentSnapshot> = emptyList()
)

/**
 * DynamicContentSnapshot - Minimal GM-generated content data
 * Sadece ID + icon path + metadata saklanır
 * Detaylar GM tarafından RAG/kitaplardan yeniden üretilebilir
 */
@Serializable
data class DynamicContentSnapshot(
    val id: String,
    val type: String,  // "ELEMENT", "SKILL", "ITEM"
    val name: String,
    val iconPath: String? = null,  // Kullanıcının eklediği icon
    val createdAtDay: Int = 1,
    val metadata: Map<String, String> = emptyMap()  // Minimal metadata (GM yeniden üretebilir)
)

/**
 * LegalConsentData - Yasal rıza ve gizlilik onayı verileri
 * KVKK/GDPR uyumluluğu için kullanıcı onaylarını saklar
 */
@Serializable
data class LegalConsentData(
    val hasAcceptedTerms: Boolean = false,
    val hasAcceptedPrivacy: Boolean = false,
    val consentVersion: String = "1.0",  // Yasal metin güncellenirse versiyon artar
    val consentDate: Long = 0,  // Unix timestamp
    val ageConfirmed18Plus: Boolean = false
)

/**
 * GÖREV I: UserAnalyticsData - Session tracking ve akıllı zamanlama için kullanıcı analitikleri
 */
@Serializable
data class UserAnalyticsData(
    val sessionLogs: List<UserSessionLog> = emptyList(),
    val peakHoursStart: Int = 18,  // Default: 18:00
    val peakHoursEnd: Int = 21,    // Default: 21:00
    val lastAnalysisDate: Long = 0  // En son peak hours analizi yapılan tarih
)

/**
 * GÖREV I: UserSessionLog - Tek bir uygulama oturumu kaydı
 */
@Serializable
data class UserSessionLog(
    val sessionStart: Long,    // Unix timestamp
    val sessionEnd: Long,      // Unix timestamp (0 = henüz bitmedi)
    val dayOfWeek: Int,        // 1-7 (Calendar.SUNDAY to Calendar.SATURDAY)
    val hourOfDay: Int         // 0-23
)

/**
 * GÖREV I: NotificationSettingsData - Bildirim ayarları
 */
@Serializable
data class NotificationSettingsData(
    val gameProgressNotifications: Boolean = true,      // Mikro geri bildirimler
    val questUpdateNotifications: Boolean = true,       // System Overlay
    val smartRemindersEnabled: Boolean = true,          // Push notifications (ana kontrol)
    val exerciseRemindersEnabled: Boolean = true,       // Alt kontrol: 3 gün
    val meditationRemindersEnabled: Boolean = true,     // Alt kontrol: 2 gün
    val journalRemindersEnabled: Boolean = true         // Alt kontrol: 7 gün
)

/**
 * G57: PermissionsGrantedData - Runtime permission durumları
 */
@Serializable
data class PermissionsGrantedData(
    val cameraPermission: Boolean = false,
    val audioPermission: Boolean = false,
    val notificationPermission: Boolean = false
)

/**
 * G59: PerformanceMode - Performans profili seçenekleri
 */
enum class PerformanceMode {
    HIGH,       // Maksimum kalite, 60 FPS, tüm efektler
    BALANCED,   // Dengeli, 45 FPS, optimize edilmiş efektler
    BATTERY     // Batarya tasarrufu, 30 FPS, minimal efektler
}

/**
 * G59: PerformanceSettingsData - Performans ayarları
 */
@Serializable
data class PerformanceSettingsData(
    val performanceMode: String = "BALANCED",  // HIGH, BALANCED, BATTERY
    val targetFps: Int = 45,                   // 30, 45, 60
    val enableParticleEffects: Boolean = true,
    val enablePostProcessing: Boolean = true,
    val enableShadows: Boolean = true,
    val thermalMonitoringEnabled: Boolean = true,
    val autoThrottleOnHeat: Boolean = true,     // Isınma durumunda otomatik throttle
    val maxThermalThreshold: Float = 45f        // °C cinsinden maksimum sıcaklık eşiği
)

@Serializable
data class SettingsData(
    val gameSettings: GameSettingsData = GameSettingsData(),
    val uiSettings: UISettingsData = UISettingsData(),
    val storySettings: StorySettingsData = StorySettingsData(),
    val apiSettings: APISettingsData = APISettingsData(),
    val debugSettings: DebugSettingsData = DebugSettingsData(),
    val legalConsent: LegalConsentData = LegalConsentData(),
    val notificationSettings: NotificationSettingsData = NotificationSettingsData(),  // GÖREV I
    val permissionsGranted: PermissionsGrantedData = PermissionsGrantedData(),  // G57: Runtime permissions
    val performanceSettings: PerformanceSettingsData = PerformanceSettingsData()  // G59: Performance settings
)

@Serializable
data class GameSettingsData(
    val autoSaveEnabled: Boolean = true,
    val autoSaveInterval: Int = 300, // seconds
    val language: String = "EN", // TR, EN - Default: EN (#27)
    val difficulty: String = "NORMAL", // EASY, NORMAL, HARD, NIGHTMARE
    val enableNotifications: Boolean = true,
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    // HATA #3 FIX: Dil pratiği için tercih edilen dil
    val preferredPracticeLanguage: String = "" // Boşsa sistem diline fallback, dolu ise bu kullanılır
)

@Serializable
data class CustomThemeColors(
    val primary: String = "#6650a4",      // Default purple
    val surface: String = "#1F1429",      // Default dark surface
    val background: String = "#101C22"    // Default dark background
)

@Serializable
data class UISettingsData(
    val theme: String = "BLUE", // BLUE (default), GREY, PINK, CUSTOM1, CUSTOM2, CUSTOM3
    val fontSize: String = "MEDIUM", // SMALL, MEDIUM, LARGE
    val animationSpeed: Float = 1.0f, // 0.5f to 2.0f
    val enableAnimations: Boolean = true,
    val enableHapticFeedback: Boolean = true,
    val enableAmbientSound: Boolean = true, // Ortam sesi (hava durumu sesleri)
    // GÖREV 4: Element Affinity Display Toggle
    val showElementAffinity: Boolean = true, // Profil ekranında element affinity göster/gizle
    // GÖREV #18: İskelet Özelleştirme Sistemi
    val skeletonGridStrokeWidth: Float = 0.5f, // 0.5 - 5.0 dp arası
    val skeletonGridColor: Long = 0xFFFFFFFF, // ARGB formatında renk (default: beyaz)
    val skeletonGridAlpha: Float = 0.1f, // 0.0 - 1.0 arası şeffaflık
    // Custom Theme Colors
    val customTheme1: CustomThemeColors = CustomThemeColors(),
    val customTheme2: CustomThemeColors = CustomThemeColors(),
    val customTheme3: CustomThemeColors = CustomThemeColors()
)

@Serializable
data class StorySettingsData(
    val hideJSON: Boolean = true,
    val suggestionFilter: String = "MEDIUM", // LOW, MEDIUM, HIGH
    val realismLevel: String = "MEDIUM", // LOW, MEDIUM, HIGH, ULTRA
    val showCharacterHints: Boolean = true,
    val autoAdvanceText: Boolean = false,
    val externalStorySource: String = "",
    val storyAdherence: Float = 0.7f, // 0.0 ile 1.0 arası, varsayılan %70
    val aiStoryMode: String = "INTERACTIVE" // PDF_BOUND veya INTERACTIVE
)

@Serializable
data class APISettingsData(
    val selectedProvider: String = "GOOGLE", // CRASH FIX: LOCAL model çöküyor, GOOGLE varsayılan
    val customAPIKey: String = "",
    val apiTimeout: Int = 30, // seconds
    val maxTokens: Int = 2048,
    val lastSuccessfulProvider: String = "GOOGLE", // Track what worked last
    val googleApiHealthy: Boolean = true,
    val localModelHealthy: Boolean = false,
    val costPerMillionTokens: Double = 0.50, // Varsayılan değer, örn: Gemini Flash
    // KARMA FIX: Gemini Nano Toggle
    val useGeminiNano: Boolean = false, // Default KAPALI (512 token limit + crash issues)
    // Çeviri Servisi Ayarları (Faz 1)
    val translationProvider: String = "ML_KIT", // ML_KIT (offline, default) veya GOOGLE_CLOUD (online)
    val googleCloudTranslateApiKey: String = "", // Google Cloud Translation API anahtarı
    val tokenQuotaLimit: Long = 500, // TEST için düşük limit - normalde 1000000
    val enableQuotaWarning: Boolean = true, // Uyarıların açık/kapalı olması

    // ============================
    // GÖREV: AI MODEL SEÇİMİ VE GÖRSEL ANALİZ
    // ============================

    // Text Generation Model (Journal/Umbros/Death Stats)
    val textGenerationModel: String = "GEMINI_CLOUD", // GEMMA_LOCAL (Gemma 3-bit, EN only) veya GEMINI_CLOUD (TR+EN)

    // Google Gemini API Key (Cloud model için gerekli)
    val geminiApiKey: String = "", // Google AI Studio'dan alınacak

    // Image Analysis (Health Hub)
    val imageAnalysisEnabled: Boolean = false, // Default KAPALI (kota koruma)

    // API Key validasyon durumu
    val isGeminiApiKeyValid: Boolean = false // API key test edildi mi?
)

@Serializable
data class DebugSettingsData(
    val showInventoryDebugFlag: Boolean = false,
    val showPerformanceMetrics: Boolean = false,
    val enableDetailedLogging: Boolean = false,
    val showDeveloperOptions: Boolean = false,
    val isDeveloperModeEnabled: Boolean = false
)

@Serializable
data class DeathRecord(
    val id: String = java.util.UUID.randomUUID().toString(), // Unique ID
    val characterName: String,
    val deathDay: Int,
    val deathLocation: String,
    val deathCause: String,
    val totalPlayTime: Long,
    val finalStats: PlayerStatsPersistent,
    val achievements: List<String> = emptyList(),
    val activeTitles: List<String> = emptyList(), // Aktif ünvanlar
    val screenshotPath: String? = null, // Ölüm anı screenshot'u
    val gmDeathSummary: String? = null, // GM'den 2-3 satırlık dramatik özet
    val xpGainedFromDeath: Int = 0, // Ölümden kazanılan XP (varsa)
    val wasUmbrosOfferAccepted: Boolean = false, // Umbros teklifi kabul edildi mi?
    val deathTimestamp: Long = System.currentTimeMillis()
)

@Serializable
data class UsageStatsData(
    val totalTokensUsed: Long = 0,
    val sessionTokensUsed: Long = 0,
    val totalApiCalls: Long = 0,
    val sessionApiCalls: Long = 0,
    val lastSessionStart: Long = System.currentTimeMillis(),
    val quotaWarningSent: Boolean = false // Uyarı gönderildi mi?
)

object PersistentDataManager {
    private const val PREFS_NAME = "isekai_kuroshin_data"
    private const val KEY_GAME_DATA = "persistent_game_data"
    private const val KEY_FIRST_LAUNCH = "is_first_launch"
    private const val KEY_FIRST_JOURNAL_VISIT = "is_first_journal_visit"
    private const val KEY_FIRST_AI_DIALOG_VISIT = "is_first_ai_dialog_visit"

    private lateinit var sharedPrefs: SharedPreferences
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _gameData = MutableStateFlow(PersistentGameData())
    val gameData: StateFlow<PersistentGameData> = _gameData.asStateFlow()

    // RACE CONDITION FIX: Initialization completion flag
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    // DEADLOCK FIX: Coroutine scope for async initialization
    private val initScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO
    )

    /**
     * RACE CONDITION FIX - Strateji 2: Synchronous initialization for Startup Library
     * Bu fonksiyon DataInitializer tarafından çağrılır, BLOCKING/SENKRON çalışır
     * Uygulama başlamadan önce tamamlanır, böylece race condition ortadan kalkar
     */
    fun initializeSynchronously(context: Context) {
        try {
            android.util.Log.d("PersistentDataManager", "RACE CONDITION FIX: Starting synchronous initialization")
            android.util.Log.d("PersistentDataManager", "Thread: ${Thread.currentThread().name}")

            // YENİ EncryptedSharedPreferences KODU:
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

            sharedPrefs = EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            loadGameData()

            // RACE CONDITION FIX: Mark initialization as complete
            _isInitialized.value = true
            android.util.Log.d("PersistentDataManager", "RACE CONDITION FIX: Synchronous initialization complete ✅")
        } catch (e: Exception) {
            android.util.Log.e("PersistentDataManager", "RACE CONDITION FIX: Initialization failed", e)
            // Even on failure, mark as initialized to prevent blocking
            _isInitialized.value = true
        }
    }

    /**
     * DEPRECATED: Async initialization - replaced by synchronous version
     * Kept for backward compatibility with MainActivity
     */
    @Deprecated("Use initializeSynchronously() instead", ReplaceWith("initializeSynchronously(context)"))
    fun initialize(context: Context) {
        // DEADLOCK FIX: Launch initialization in background thread to prevent main thread blocking
        initScope.launch {
            try {
                android.util.Log.d("PersistentDataManager", "DEPRECATED: Async initialize called, use initializeSynchronously instead")

                // YENİ EncryptedSharedPreferences KODU:
                val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

                sharedPrefs = EncryptedSharedPreferences.create(
                    PREFS_NAME,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )

                loadGameData()

                // RACE CONDITION FIX: Mark initialization as complete
                _isInitialized.value = true
                android.util.Log.d("PersistentDataManager", "DEPRECATED: Async initialization complete")
            } catch (e: Exception) {
                android.util.Log.e("PersistentDataManager", "DEPRECATED: Initialization failed", e)
                // Even on failure, mark as initialized to prevent deadlock
                _isInitialized.value = true
            }
        }
    }

    fun isFirstLaunch(): Boolean {
        return sharedPrefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    fun setFirstLaunchCompleted() {
        sharedPrefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    fun isFirstJournalVisit(): Boolean {
        return sharedPrefs.getBoolean(KEY_FIRST_JOURNAL_VISIT, true)
    }

    fun setFirstJournalVisitCompleted() {
        sharedPrefs.edit().putBoolean(KEY_FIRST_JOURNAL_VISIT, false).apply()
    }

    fun isFirstAIDialogVisit(): Boolean {
        return sharedPrefs.getBoolean(KEY_FIRST_AI_DIALOG_VISIT, true)
    }

    fun setFirstAIDialogVisitCompleted() {
        sharedPrefs.edit().putBoolean(KEY_FIRST_AI_DIALOG_VISIT, false).apply()
    }

    private fun loadGameData() {
        try {
            val jsonString = sharedPrefs.getString(KEY_GAME_DATA, null)
            if (jsonString != null) {
                val loadedData = json.decodeFromString<PersistentGameData>(jsonString)
                _gameData.value = loadedData
            }
        } catch (e: Exception) {
            // If loading fails, start with default data
            _gameData.value = PersistentGameData()
        }
    }

    /**
     * TODO-HUB-14: Generic save method for any data type
     */
    fun <T> saveData(key: String, data: T, serializer: kotlinx.serialization.KSerializer<T>) {
        try {
            val jsonString = json.encodeToString(serializer, data)
            sharedPrefs.edit().putString(key, jsonString).apply()
        } catch (e: Exception) {
            android.util.Log.e("PersistentDataManager", "Failed to save data for key: $key", e)
        }
    }

    /**
     * TODO-HUB-14: Generic load method for any data type
     */
    fun <T> loadData(key: String, serializer: kotlinx.serialization.KSerializer<T>): T? {
        return try {
            val jsonString = sharedPrefs.getString(key, null)
            if (jsonString != null) {
                json.decodeFromString(serializer, jsonString)
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("PersistentDataManager", "Failed to load data for key: $key", e)
            null
        }
    }

    fun saveGameData() {
        try {
            val jsonString = json.encodeToString(_gameData.value)
            sharedPrefs.edit().putString(KEY_GAME_DATA, jsonString).apply()

            // Trigger auto-save notification if enabled
            if (_gameData.value.settingsData.gameSettings.autoSaveEnabled) {
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val notificationManager = Class.forName("com.example.isekaikuroshin.utils.NotificationManager")
                        val notifyAutoSaveMethod = notificationManager.getMethod("notifyAutoSave")
                        val instance = notificationManager.getDeclaredField("INSTANCE").get(null)
                        notifyAutoSaveMethod.invoke(instance)
                    } catch (e: Exception) {
                        // Notification system not available yet
                    }
                }
            }
        } catch (e: Exception) {
            // Handle save error
        }
    }

    fun updatePlayerData(updater: (PlayerPersistentData) -> PlayerPersistentData) {
        _gameData.value = _gameData.value.copy(
            playerData = updater(_gameData.value.playerData)
        )
        saveGameData()
    }

    fun updateStoryData(updater: (StoryPersistentData) -> StoryPersistentData) {
        _gameData.value = _gameData.value.copy(
            storyData = updater(_gameData.value.storyData)
        )
        saveGameData()
    }

    fun updateDynamicPlaylist(playlist: List<String>) {
        updateStoryData { it.copy(dynamicPlaylist = playlist) }
    }

    fun updateDynamicPhotoPlaylist(photoPlaylist: List<Int>) {
        updateStoryData { it.copy(dynamicPhotoPlaylist = photoPlaylist) }
    }

    fun updateCatalogData(updater: (CharacterCatalogData) -> CharacterCatalogData) {
        _gameData.value = _gameData.value.copy(
            catalogData = updater(_gameData.value.catalogData)
        )
        saveGameData()
    }

    fun updateQuestData(updater: (QuestData) -> QuestData) {
        _gameData.value = _gameData.value.copy(
            questData = updater(_gameData.value.questData)
        )
        saveGameData()
    }

    // Ana görev ekleme fonksiyonu (RegistryQuest'ten QuestEntry'ye dönüştür)
    fun addMainQuest(registryQuest: com.example.isekaikuroshin.data.RegistryQuest, currentDay: Int) {
        updateQuestData { questData ->
            // Zaten aktif mi kontrol et
            val isAlreadyActive = questData.activeQuests.any { it.id == registryQuest.id }

            if (!isAlreadyActive) {
                val questEntry = QuestEntry(
                    id = registryQuest.id,
                    title = registryQuest.title,
                    description = registryQuest.description,
                    giver = "Sistem", // Ana görevler sistem tarafından verilir
                    startDay = currentDay,
                    dueDay = -1, // Ana görevlerin genelde deadline'ı yok
                    progress = 0.0f,
                    isCompleted = false,
                    rewards = registryQuest.rewards.entries.map { "${it.key} x${it.value}" },
                    isDeveloperQuest = true // ANA GÖREV
                )

                com.example.isekaikuroshin.utils.GameLogger.logSystem("[MAIN QUEST] ⭐ Ana görev eklendi: ${questEntry.title}")

                questData.copy(
                    activeQuests = questData.activeQuests + questEntry
                )
            } else {
                questData
            }
        }
    }

    // Hikaye ilerledikten sonra ana quest tetikleme kontrol et
    fun checkAndTriggerMainQuest(currentDay: Int, latestStoryText: String = ""): QuestEntry? {
        val questData = _gameData.value.questData

        // Tamamlanmış ana görev ID'lerini al
        val completedMainQuestIds = (questData.activeQuests + questData.completedQuests)
            .filter { it.isDeveloperQuest && it.isCompleted }
            .map { it.id }

        // Yerleşkeye ulaşma kontrolü
        val reachedVillage = ContentRegistry.MainQuests.checkIfReachedSettlement(latestStoryText)

        // Yeni ana görev var mı kontrol et
        val nextMainQuest = ContentRegistry.MainQuests.getNextMainQuestForStory(
            currentDay = currentDay,
            completedMainQuestIds = completedMainQuestIds,
            reachedVillage = reachedVillage
        )

        // Eğer yeni ana görev varsa ekle ve döndür
        if (nextMainQuest != null) {
            addMainQuest(nextMainQuest, currentDay)

            // QuestEntry olarak döndür (SystemOverlay için)
            return questData.activeQuests.find { it.id == nextMainQuest.id }
                ?: QuestEntry(
                    id = nextMainQuest.id,
                    title = nextMainQuest.title,
                    description = nextMainQuest.description,
                    giver = "Sistem",
                    startDay = currentDay,
                    dueDay = -1,
                    progress = 0.0f,
                    isCompleted = false,
                    rewards = nextMainQuest.rewards.entries.map { "${it.key} x${it.value}" },
                    isDeveloperQuest = true
                )
        }

        return null
    }

    fun updateSettingsData(updater: (SettingsData) -> SettingsData) {
        val oldSettings = _gameData.value.settingsData
        val newSettings = updater(oldSettings)

        // Kota limiti değişti ise uyarı durumunu sıfırla
        val quotaLimitChanged = oldSettings.apiSettings.tokenQuotaLimit != newSettings.apiSettings.tokenQuotaLimit

        _gameData.value = _gameData.value.copy(
            settingsData = newSettings,
            usageStats = if (quotaLimitChanged) {
                _gameData.value.usageStats.copy(quotaWarningSent = false)
            } else {
                _gameData.value.usageStats
            }
        )

        if (quotaLimitChanged) {
            android.util.Log.d("QuotaManager", "Kota limiti değişti, uyarı durumu sıfırlandı. " +
                "Eski: ${oldSettings.apiSettings.tokenQuotaLimit}, " +
                "Yeni: ${newSettings.apiSettings.tokenQuotaLimit}")
        }

        saveGameData()
    }

    // GameMasterEngine support function
    fun updateGameData(updater: (PersistentGameData) -> PersistentGameData) {
        _gameData.value = updater(_gameData.value)
        saveGameData()
    }

    fun updateExternalStorySource(uri: String) {
        _gameData.value = _gameData.value.copy(externalStorySource = uri)
        saveGameData()
    }

    fun addCharacterToCatalog(character: CharacterEntry) {
        updateCatalogData { catalogData ->
            val existingCharacter = catalogData.knownCharacters.find { it.id == character.id }
            if (existingCharacter == null) {
                catalogData.copy(
                    knownCharacters = catalogData.knownCharacters + character
                )
            } else {
                // Update existing character
                catalogData.copy(
                    knownCharacters = catalogData.knownCharacters.map {
                        if (it.id == character.id) character else it
                    }
                )
            }
        }
    }

    fun addQuestToActive(quest: QuestEntry) {
        updateQuestData { questData ->
            questData.copy(
                activeQuests = questData.activeQuests + quest
            )
        }
    }

    fun completeQuest(questId: String) {
        updateQuestData { questData ->
            val quest = questData.activeQuests.find { it.id == questId }
            if (quest != null) {
                val completedQuest = quest.copy(isCompleted = true, progress = 1.0f)
                questData.copy(
                    activeQuests = questData.activeQuests.filter { it.id != questId },
                    completedQuests = questData.completedQuests + completedQuest
                )
            } else {
                questData
            }
        }
    }

    suspend fun recordDeath(
        deathCause: String,
        wasUmbrosOfferAccepted: Boolean = false,
        screenshotPath: String? = null
    ) {
        val currentData = _gameData.value

        // Aktif ünvanları al
        val activeTitles = currentData.playerData.activeTitles

        // Son 3 günün hikaye özetini oluştur (basit versiyon)
        val recentStoryContext = if (currentData.storyData.storyPages.isNotEmpty()) {
            currentData.storyData.storyPages.takeLast(3).joinToString(". ")
        } else {
            "Yolculuk henüz başlamıştı."
        }

        // GM'den dramatik ölüm özeti al (basit versiyon - AI entegrasyonu için TODO)
        val gmSummary = try {
            // TODO: BasicStoryEngine instance gerektirir, şimdilik basit özet
            val titlesText = if (activeTitles.isNotEmpty()) {
                "\"${activeTitles.joinToString(", ")}\" ünvanlarıyla bilinen"
            } else {
                ""
            }
            "${currentData.playerData.name} $titlesText, ${currentData.storyData.currentLocation} bölgesinde $deathCause. " +
            "Hikayesi ${currentData.storyData.currentDay}. günde sona erdi. $recentStoryContext"
        } catch (e: Exception) {
            "Bir kahraman düştü. ${currentData.playerData.name}'ın destanı burada sona erdi."
        }

        val deathRecord = DeathRecord(
            id = java.util.UUID.randomUUID().toString(),
            characterName = currentData.playerData.name,
            deathDay = currentData.storyData.currentDay,
            deathLocation = currentData.storyData.currentLocation,
            deathCause = deathCause,
            totalPlayTime = currentData.storyData.totalPlayTime,
            finalStats = currentData.playerData.stats,
            achievements = currentData.playerData.achievements,
            activeTitles = activeTitles,
            screenshotPath = screenshotPath,
            gmDeathSummary = gmSummary,
            xpGainedFromDeath = 0, // TODO: Ölümden XP kazanma sistemi eklenebilir
            wasUmbrosOfferAccepted = wasUmbrosOfferAccepted
        )

        _gameData.value = currentData.copy(
            deathArchive = currentData.deathArchive + deathRecord,
            playerData = currentData.playerData.copy(isAlive = false)
        )
        saveGameData()
    }

    fun startNewCharacter(playerName: String) {
        val currentData = _gameData.value
        _gameData.value = currentData.copy(
            playerData = PlayerPersistentData(name = playerName, isAlive = true),
            storyData = StoryPersistentData()
        )
        saveGameData()
    }

    /**
     * GÖREV 4: Element Affinity'ye XP ekle
     * @param elementType Element türü ("FIRE", "WATER", etc.)
     * @param xp Eklenecek XP miktarı
     * @return Level up oldu mu?
     */
    fun addElementAffinityXP(elementType: String, xp: Int): Boolean {
        val currentData = _gameData.value
        val (updatedAffinities, leveledUp) = currentData.playerData.elementAffinities.addXP(elementType, xp)

        _gameData.value = currentData.copy(
            playerData = currentData.playerData.copy(
                elementAffinities = updatedAffinities
            )
        )
        saveGameData()

        return leveledUp
    }

    /**
     * GÖREV 4: Element Affinity bonus'unu al
     * @param elementType Element türü
     * @return Affinity bonus (0.0 - 1.0+)
     */
    fun getElementAffinityBonus(elementType: String): Float {
        return _gameData.value.playerData.elementAffinities.getAffinityBonus(elementType)
    }

    /**
     * TODO-FIX-01: API Key'i temizler
     * Oyun sıfırlandığında veya kullanıcı istediğinde API key'i temizler
     */
    fun clearAPIKey() {
        android.util.Log.d("PersistentDataManager", "TODO-FIX-01: Clearing API key")
        updateSettingsData { settings ->
            settings.copy(
                apiSettings = settings.apiSettings.copy(
                    customAPIKey = "",
                    selectedProvider = "LOCAL" // Default'a dön
                )
            )
        }
        android.util.Log.d("PersistentDataManager", "TODO-FIX-01: API key cleared, provider reset to LOCAL")
    }

    /**
     * TODO-FIX-01 + HATA #2 FIX + DEATH-LOOP-FIX: Tüm kullanıcı verilerini sıfırlar
     * "Ölüm Testi" veya "Tüm Verileri Sıfırla" butonları için
     *
     * @param keepDeathArchive true ise ölüm arşivi korunur (varsayılan: true)
     */
    fun resetAllData(keepDeathArchive: Boolean = true) {
        android.util.Log.d("PersistentDataManager", "TODO-FIX-01: Resetting ALL user data including API key")

        // DEATH-LOOP-FIX: Mevcut ölüm arşivini kaydet
        val existingDeathArchive = if (keepDeathArchive) {
            _gameData.value.deathArchive
        } else {
            emptyList()
        }

        android.util.Log.d("PersistentDataManager", "💀 DeathArchive korunuyor: ${existingDeathArchive.size} kayıt")

        // Tüm oyun verilerini sıfırla AMA ölüm arşivini koru
        _gameData.value = PersistentGameData(
            deathArchive = existingDeathArchive  // ← RUH PARÇASI KORUNDU!
        )
        saveGameData()

        // HATA #2 FIX: Dil pratiği ilerlemesini de sıfırla
        android.util.Log.d("PersistentDataManager", "HATA #2 FIX: Clearing language practice progress")

        // Tüm diller için kaydedilmiş ilerleme verilerini sil
        val languagesToClear = listOf("İngilizce", "Japanese", "Español", "Français", "Deutsch")
        for (language in languagesToClear) {
            val progressKey = "language_progress_$language"
            try {
                sharedPrefs.edit().remove(progressKey).apply()
                android.util.Log.d("PersistentDataManager", "HATA #2 FIX: Cleared progress for: $language")
            } catch (e: Exception) {
                android.util.Log.e("PersistentDataManager", "Failed to clear progress for $language", e)
            }
        }

        // game.GameStateManager için saklanmış GameStateZ7'yi de sıfırla
        try {
            sharedPrefs.edit().remove("current_game_state").apply()
            android.util.Log.d("PersistentDataManager", "HATA #2 FIX: Cleared game state")
        } catch (e: Exception) {
            android.util.Log.e("PersistentDataManager", "Failed to clear game state", e)
        }

        android.util.Log.d("PersistentDataManager", "TODO-FIX-01: All data reset complete (DeathArchive preserved: ${existingDeathArchive.size} records)")
    }

    /**
     * EMERGENCY FIX: Resurrect dead player to fix save/load flow
     */
    fun resurrectPlayer() {
        val currentData = _gameData.value
        _gameData.value = currentData.copy(
            playerData = currentData.playerData.copy(isAlive = true)
        )
        saveGameData()
    }

    fun exportData(): String {
        return json.encodeToString(_gameData.value)
    }

    fun importData(jsonString: String): Boolean {
        return try {
            val importedData = json.decodeFromString<PersistentGameData>(jsonString)
            _gameData.value = importedData
            saveGameData()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Token kullanım sayaçlarını artırır ve kota kontrolü yapar
     * @param tokenCount AI çağrısında kullanılan token sayısı
     */
    fun incrementTokenCounters(tokenCount: Int) {
        updateGameData { currentData ->
            val newStats = currentData.usageStats.copy(
                totalTokensUsed = currentData.usageStats.totalTokensUsed + tokenCount,
                sessionTokensUsed = currentData.usageStats.sessionTokensUsed + tokenCount,
                totalApiCalls = currentData.usageStats.totalApiCalls + 1,
                sessionApiCalls = currentData.usageStats.sessionApiCalls + 1
            )
            currentData.copy(usageStats = newStats)
        }

        // Token usage log
        android.util.Log.d("TokenCounter", "AI yanıtı $tokenCount token kullandı. " +
            "Yeni toplam: ${_gameData.value.usageStats.totalTokensUsed}, " +
            "Yeni oturum: ${_gameData.value.usageStats.sessionTokensUsed}")

        // KOTA KONTROL MANTĞI
        val currentData = _gameData.value
        val settings = currentData.settingsData.apiSettings
        val stats = currentData.usageStats

        // Üç koşulu kontrol et: uyarı etkin, daha önce uyarılmamış, kota aşıldı
        if (settings.enableQuotaWarning &&
            !stats.quotaWarningSent &&
            stats.totalTokensUsed >= settings.tokenQuotaLimit) {

            // WARN seviyesinde uyarı logu
            android.util.Log.w("QuotaManager", "KULLANICI KOTAYI AŞTI! " +
                "Toplam kullanım: ${stats.totalTokensUsed}, " +
                "Limit: ${settings.tokenQuotaLimit}")

            // Tekrar uyarı göndermemek için bayrağı true yap
            updateGameData { data ->
                data.copy(usageStats = data.usageStats.copy(quotaWarningSent = true))
            }
        }
    }

    /**
     * Oturum sayaçlarını sıfırlar (uygulama başlangıcında çağrılır)
     */
    fun resetSessionCounters() {
        updateGameData { currentData ->
            val newStats = currentData.usageStats.copy(
                sessionTokensUsed = 0,
                sessionApiCalls = 0,
                lastSessionStart = System.currentTimeMillis()
            )
            currentData.copy(usageStats = newStats)
        }
    }

    /**
     * Usage statistics'leri günceller
     */
    fun updateUsageStats(updater: (UsageStatsData) -> UsageStatsData) {
        updateGameData { currentData ->
            currentData.copy(usageStats = updater(currentData.usageStats))
        }
    }

    // ========================================
    // ⚡⚡⚡ GENERIC STRING GET/SET (for SpellRecipe persistence)
    // ========================================

    /**
     * ⚡⚡⚡ YENİ: String değer kaydet
     */
    fun saveString(key: String, value: String) {
        sharedPrefs.edit().putString(key, value).apply()
    }

    /**
     * ⚡⚡⚡ YENİ: String değer oku
     */
    fun getString(key: String, defaultValue: String = ""): String {
        return sharedPrefs.getString(key, defaultValue) ?: defaultValue
    }

    /**
     * GÖREV I.2: Yasal rıza verilerini günceller
     */
    fun updateLegalConsent(consent: LegalConsentData) {
        updateSettingsData { settings ->
            settings.copy(legalConsent = consent)
        }
        android.util.Log.d("PersistentDataManager", "Legal consent updated: version=${consent.consentVersion}, accepted=${consent.hasAcceptedTerms}")
    }

    /**
     * KARMA FIX: Blocking settings data getter (for non-coroutine contexts)
     * GlobalAIManager gibi object'lerden settings okumak için
     */
    fun getSettingsDataBlocking(context: Context): SettingsData {
        // Eğer zaten initialize edilmişse, mevcut değeri dön
        if (_isInitialized.value) {
            return _gameData.value.settingsData
        }

        // Initialize edilmemişse, synchronous olarak yükle
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val prefs = EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val jsonString = prefs.getString(KEY_GAME_DATA, null)
            if (jsonString != null) {
                val data = json.decodeFromString<PersistentGameData>(jsonString)
                return data.settingsData
            }
        } catch (e: Exception) {
            android.util.Log.e("PersistentDataManager", "Failed to load settings blocking: ${e.message}")
        }

        // Fallback: Return default
        return SettingsData()
    }

    /**
     * GÖREV: Death Statistics Screen - Player data'yı sıfırla (First User için)
     * Oyuncu öldükten sonra Death Statistics'ten çıkınca yeni başlangıç yapabilsin
     */
    fun resetPlayerData() {
        android.util.Log.d("PersistentDataManager", "👤 [G140] resetPlayerData() ÇAĞRILDI!")
        val currentData = _gameData.value
        val emptyPlayerData = PlayerPersistentData() // Default boş player

        _gameData.value = currentData.copy(
            playerData = emptyPlayerData
        )

        // SYNC SAVE - commit() kullan (apply() değil)
        saveGameDataSync()
        android.util.Log.d("PersistentDataManager", "✅ Player data sıfırlandı - Yeni başlangıç")
    }

    /**
     * GÖREV: Death Statistics Screen - Death Archive'i temizle
     */
    fun clearDeathArchive() {
        android.util.Log.d("PersistentDataManager", "💀 [G140] clearDeathArchive() ÇAĞRILDI!")
        val currentData = _gameData.value

        _gameData.value = currentData.copy(
            deathArchive = emptyList()
        )

        // SYNC SAVE - commit() kullan (apply() değil)
        saveGameDataSync()
        android.util.Log.d("PersistentDataManager", "✅ Death Archive temizlendi")
    }

    /**
     * TODO-G140: Death Statistics Screen - API Keys'leri sıfırla
     * Kullanıcı yeni oyun başlatınca API key'lerini tekrar girmesi için
     *
     * KURAL 15: ROOT CAUSE - API key'ler korunuyordu, yeni oyunda eski key kullanılıyordu!
     */
    fun resetAPIKeys() {
        android.util.Log.d("PersistentDataManager", "🔑 [G140] resetAPIKeys() ÇAĞRILDI!")
        val currentData = _gameData.value

        // API Settings içindeki key'leri temizle (NESTED PATH FIX)
        val resetApiSettings = currentData.settingsData.apiSettings.copy(
            geminiApiKey = "",           // API key sıfırla
            isGeminiApiKeyValid = false  // Validation flag sıfırla
        )

        val resetSettings = currentData.settingsData.copy(
            apiSettings = resetApiSettings
        )

        _gameData.value = currentData.copy(
            settingsData = resetSettings
        )

        // SYNC SAVE - commit() kullan
        saveGameDataSync()
        android.util.Log.d("PersistentDataManager", "🔑 [G140] API Keys sıfırlandı")
    }

    /**
     * GÖREV: Synchronous save - killProcess öncesi kullan
     * commit() kullanır, apply() değil - disk'e hemen yazar
     */
    private fun saveGameDataSync() {
        try {
            val jsonString = json.encodeToString(_gameData.value)
            sharedPrefs.edit().putString(KEY_GAME_DATA, jsonString).commit() // SYNC!
            android.util.Log.d("PersistentDataManager", "💾 SYNC save tamamlandı")
        } catch (e: Exception) {
            android.util.Log.e("PersistentDataManager", "❌ Sync save hatası: ${e.message}")
        }
    }

    // ========================================
    // G61a: GM RESPONSE APPLICATION - STATS UPDATE API
    // ========================================

    /**
     * G61a: Updates player stats from GM response
     * @param statsChanged Map of stat changes (e.g., {"experience": 50, "strength": 2})
     */
    fun updatePlayerStats(statsChanged: Map<String, Int>) {
        updatePlayerData { player ->
            // Calculate new experience and level
            val newExperience = player.experience + (statsChanged["experience"] ?: 0)
            val newLevel = calculateLevel(newExperience)

            // Update stats (nested in PlayerStatsPersistent)
            val updatedStats = player.stats.copy(
                strength = player.stats.strength + (statsChanged["strength"] ?: 0),
                agility = player.stats.agility + (statsChanged["agility"] ?: 0),
                intelligence = player.stats.intelligence + (statsChanged["intelligence"] ?: 0),
                vitality = player.stats.vitality + (statsChanged["vitality"] ?: 0),
                spirit = player.stats.spirit + (statsChanged["spirit"] ?: 0),
                luck = player.stats.luck + (statsChanged["luck"] ?: 0),
                hp = (player.stats.hp + (statsChanged["health"] ?: 0)).coerceAtLeast(0),
                mp = (player.stats.mp + (statsChanged["mana"] ?: 0)).coerceAtLeast(0),
                stamina = (player.stats.stamina + (statsChanged["stamina"] ?: 0)).coerceAtLeast(0)
            )

            player.copy(
                experience = newExperience,
                level = newLevel,
                stats = updatedStats
            )
        }
        android.util.Log.d("PersistentDataManager", "G61a: Stats updated - $statsChanged")
    }

    /**
     * G61a: Calculates player level from experience
     * G61d: Now uses LevelCalculator utility
     */
    private fun calculateLevel(experience: Long): Int {
        return com.example.isekaikuroshin.utils.LevelCalculator.calculateLevel(experience)
    }

    /**
     * G61a: Adds a skill to player's learned skills
     * @param skillId Skill identifier (e.g., "skill_fireball")
     */
    fun addSkill(skillId: String) {
        updatePlayerData { player ->
            // Check if skill already exists
            val skillExists = player.skills.any { it.id == skillId }

            if (!skillExists) {
                // Create a basic SkillPersistent entry
                val newSkill = SkillPersistent(
                    id = skillId,
                    name = skillId.replace("skill_", "").replace("_", " ").replaceFirstChar { it.uppercase() },
                    level = 1,
                    currentXP = 0,
                    rarity = "F",
                    elementType = "NEUTRAL",
                    usageCount = 0,
                    tier = "Temel"
                )
                player.copy(skills = player.skills + newSkill)
            } else {
                player
            }
        }
        android.util.Log.d("PersistentDataManager", "G61a: Skill added - $skillId")
    }

    /**
     * G61a: Adds a title to player's active titles
     * @param titleId Title identifier (e.g., "title_dragonslayer")
     */
    fun addTitle(titleId: String) {
        updatePlayerData { player ->
            if (titleId !in player.activeTitles) {
                player.copy(activeTitles = player.activeTitles + titleId)
            } else {
                player
            }
        }
        android.util.Log.d("PersistentDataManager", "G61a: Title added - $titleId")
    }

    /**
     * G61a: Adds a badge/achievement to player
     * @param badgeId Badge identifier (e.g., "badge_first_kill")
     */
    fun addBadge(badgeId: String) {
        updatePlayerData { player ->
            if (badgeId !in player.achievements) {
                player.copy(achievements = player.achievements + badgeId)
            } else {
                player
            }
        }
        android.util.Log.d("PersistentDataManager", "G61a: Badge added - $badgeId")
    }

    // ========================================
    // G77: NEMESIS SYSTEM API
    // ========================================

    /**
     * G77: Adds or updates a nemesis entry
     * @param nemesisEntry The nemesis data from GM
     */
    fun addNemesis(nemesisEntry: NemesisEntry) {
        updateGameData { currentData ->
            val existingNemesis = currentData.nemesisData.activeNemeses.find { it.npcId == nemesisEntry.npcId }

            val updatedNemeses = if (existingNemesis != null) {
                // Update existing nemesis (level escalation)
                currentData.nemesisData.activeNemeses.map {
                    if (it.npcId == nemesisEntry.npcId) nemesisEntry else it
                }
            } else {
                // Add new nemesis
                currentData.nemesisData.activeNemeses + nemesisEntry
            }

            currentData.copy(
                nemesisData = currentData.nemesisData.copy(
                    activeNemeses = updatedNemeses
                )
            )
        }
        android.util.Log.d("PersistentDataManager", "G77: Nemesis added/updated - ${nemesisEntry.npcId} (Level ${nemesisEntry.nemesisLevel})")
    }

    /**
     * G77: Get all active nemeses
     */
    fun getActiveNemeses(): List<NemesisEntry> {
        return _gameData.value.nemesisData.activeNemeses
    }

    /**
     * G77: Check if an NPC is a nemesis
     */
    fun isNemesis(npcId: String): Boolean {
        return _gameData.value.nemesisData.activeNemeses.any { it.npcId == npcId }
    }

    // ========================================
    // G82: STATUS EFFECT SYSTEM API
    // ========================================

    /**
     * G82: Applies a status effect to the player
     * Converts StatusEffect to ActiveStatusEffect and adds to player's active effects
     *
     * @param effect The status effect to apply
     */
    fun applyStatusEffect(effect: StatusEffect) {
        updatePlayerData { player ->
            // Get current turn from stats (we'll need to add this to PlayerStatsPersistent)
            // For now, use a simple counter based on active effects size
            val currentTurn = player.stats.hp.toInt() // Placeholder - should be proper turn counter

            // Create ActiveStatusEffect with current turn
            val activeEffect = ActiveStatusEffect(
                effect = effect,
                startTurn = currentTurn
            )

            // Check if this effect already exists (by effectId)
            val existingEffect = player.activeStatusEffects.find { it.effect.effectId == effect.effectId }

            val updatedEffects = if (existingEffect != null) {
                // Replace existing effect (refresh duration)
                player.activeStatusEffects.map {
                    if (it.effect.effectId == effect.effectId) activeEffect else it
                }
            } else {
                // Add new effect
                player.activeStatusEffects + activeEffect
            }

            player.copy(activeStatusEffects = updatedEffects)
        }
        android.util.Log.d("PersistentDataManager", "G82: Status effect applied - ${effect.name} (${effect.type})")
    }

    /**
     * G82: Remove expired status effects
     * Should be called at the start of each turn
     *
     * @param currentTurn Current turn number
     */
    fun removeExpiredStatusEffects(currentTurn: Int) {
        updatePlayerData { player ->
            val activeEffects = player.activeStatusEffects.filter { activeEffect ->
                !activeEffect.effect.isExpired(currentTurn, activeEffect.startTurn)
            }

            val expiredCount = player.activeStatusEffects.size - activeEffects.size
            if (expiredCount > 0) {
                android.util.Log.d("PersistentDataManager", "G82: Removed $expiredCount expired status effects")
            }

            player.copy(activeStatusEffects = activeEffects)
        }
    }

    /**
     * G82: Get all active status effects on player
     */
    fun getActiveStatusEffects(): List<ActiveStatusEffect> {
        return _gameData.value.playerData.activeStatusEffects
    }

    /**
     * G82: Check if player has a specific status effect
     */
    fun hasStatusEffect(effectId: String): Boolean {
        return _gameData.value.playerData.activeStatusEffects.any { it.effect.effectId == effectId }
    }

}