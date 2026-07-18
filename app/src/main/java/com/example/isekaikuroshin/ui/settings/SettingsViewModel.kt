package com.example.isekaikuroshin.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.isekaikuroshin.ai.GameAction
import com.example.isekaikuroshin.data.GameStateManager
import com.example.isekaikuroshin.engine.ActionExecutorEngine
import com.example.isekaikuroshin.utils.GameLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Processing state for file processing UI feedback
 */
enum class ProcessingState {
    IDLE,           // No processing ongoing
    PROCESSING,     // File is being processed
    SUCCESS,        // Processing completed successfully
    FAILED          // Processing failed
}

/**
 * Data class to hold processing status and result information
 */
data class ProcessingStatus(
    val state: ProcessingState = ProcessingState.IDLE,
    val chunkCount: Int = 0,
    val errorMessage: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val gameStateManager: GameStateManager,
    private val actionExecutorEngine: ActionExecutorEngine,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // WorkManager instance
    private val workManager = WorkManager.getInstance(context)

    // Current work request UUID (null when no work in progress)
    private var currentWorkId: UUID? = null

    // Processing status StateFlow for UI observation
    private val _processingStatus = MutableStateFlow(ProcessingStatus())
    val processingStatus: StateFlow<ProcessingStatus> = _processingStatus.asStateFlow()

    /**
     * Start monitoring a WorkManager task by its UUID
     */
    fun startWorkMonitoring(workId: UUID) {
        currentWorkId = workId
        _processingStatus.value = ProcessingStatus(state = ProcessingState.PROCESSING)

        viewModelScope.launch {
            // Observe the work status
            workManager.getWorkInfoByIdFlow(workId).collect { workInfo ->
                if (workInfo != null) {
                    when (workInfo.state) {
                        WorkInfo.State.RUNNING -> {
                            _processingStatus.value = ProcessingStatus(state = ProcessingState.PROCESSING)
                            GameLogger.logSystem("File processing: RUNNING")
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            val chunkCount = workInfo.outputData.getInt("CHUNK_COUNT", 0)
                            _processingStatus.value = ProcessingStatus(
                                state = ProcessingState.SUCCESS,
                                chunkCount = chunkCount
                            )
                            GameLogger.logSystem("File processing: SUCCESS with $chunkCount chunks")
                            currentWorkId = null
                        }
                        WorkInfo.State.FAILED -> {
                            _processingStatus.value = ProcessingStatus(
                                state = ProcessingState.FAILED,
                                errorMessage = "Dosya işleme başarısız oldu"
                            )
                            GameLogger.logSystem("File processing: FAILED")
                            currentWorkId = null
                        }
                        WorkInfo.State.CANCELLED -> {
                            _processingStatus.value = ProcessingStatus(state = ProcessingState.IDLE)
                            GameLogger.logSystem("File processing: CANCELLED")
                            currentWorkId = null
                        }
                        else -> {
                            // ENQUEUED or BLOCKED states
                            _processingStatus.value = ProcessingStatus(state = ProcessingState.PROCESSING)
                        }
                    }
                }
            }
        }
    }

    /**
     * Reset processing status to IDLE
     */
    fun resetProcessingStatus() {
        _processingStatus.value = ProcessingStatus(state = ProcessingState.IDLE)
        currentWorkId = null
    }

    suspend fun resetGame() {
        gameStateManager.resetGame()
    }

    fun loadTestData() {
        gameStateManager.loadTestData()
    }

    fun runDirectorEngineTest() {
        gameStateManager.testDirectorEngine(actionExecutorEngine)
    }

    fun runActionExecutorTest() {
        viewModelScope.launch {
            GameLogger.logSystem("=== ACTION EXECUTOR ENGINE TEST START ===")

            // Test farklı eylem tiplerini içeren örnek action listesi
            val testActions = listOf(
                // Altın ekleme testi
                GameAction(
                    actionType = "UPDATE_GOLD",
                    parameters = mapOf("changeAmount" to "100")
                ),
                // Deneyim ekleme testi
                GameAction(
                    actionType = "ADD_EXPERIENCE",
                    parameters = mapOf("amount" to "50")
                ),
                // Sağlık azaltma testi
                GameAction(
                    actionType = "UPDATE_HEALTH",
                    parameters = mapOf("changeAmount" to "-10")
                ),
                // Envantere eşya ekleme testi
                GameAction(
                    actionType = "ADD_ITEM_TO_INVENTORY",
                    parameters = mapOf(
                        "itemId" to "test_sword",
                        "quantity" to "1"
                    )
                ),
                // Hikaye sayfası ekleme testi
                GameAction(
                    actionType = "ADD_STORY_PAGE",
                    parameters = mapOf("content" to "🧪 ActionExecutorEngine test tamamlandı! AI eylemleri başarıyla uygulandı.")
                ),
                // Geçersiz eylem testi (hata handling)
                GameAction(
                    actionType = "INVALID_ACTION",
                    parameters = mapOf("test" to "value")
                )
            )

            // Mevcut oyuncu durumunu logla
            val currentState = gameStateManager.gameState.value
            GameLogger.logSystem("TEST ÖNCESI - Altın: ${currentState.playerState.gold}, Deneyim: ${currentState.playerState.experience}, Sağlık: ${currentState.playerState.currentHealth}")

            // ActionExecutorEngine'i test et
            try {
                actionExecutorEngine.executeActions(testActions)
                GameLogger.logSystem("✅ Tüm test eylemleri başarıyla uygulandı")

                // Test sonrası durumu logla
                val updatedState = gameStateManager.gameState.value
                GameLogger.logSystem("TEST SONRASI - Altın: ${updatedState.playerState.gold}, Deneyim: ${updatedState.playerState.experience}, Sağlık: ${updatedState.playerState.currentHealth}")

            } catch (e: Exception) {
                GameLogger.logError("ActionExecutorTest", "Test sırasında hata oluştu", e)
            }

            GameLogger.logSystem("=== ACTION EXECUTOR ENGINE TEST END ===")
        }
    }
}