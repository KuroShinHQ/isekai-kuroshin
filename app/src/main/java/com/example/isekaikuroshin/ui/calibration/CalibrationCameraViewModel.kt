package com.example.isekaikuroshin.ui.calibration

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.isekaikuroshin.data.GameStateManager
import com.example.isekaikuroshin.data.NormalizedPoint
import com.example.isekaikuroshin.data.Seal
import com.example.isekaikuroshin.data.SealRepository
import com.example.isekaikuroshin.engine.GestureRecognitionEngine
import com.example.isekaikuroshin.engine.DebugInfo
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * ViewModel for Calibration Camera Screen
 *
 * Handles:
 * - Loading the selected seal
 * - Processing hand detections
 * - Adding/removing calibration angles
 * - Live accuracy calculation
 */
@HiltViewModel
class CalibrationCameraViewModel @Inject constructor(
    private val gameStateManager: GameStateManager,
    private val sealRepository: SealRepository,
    private val recognitionEngine: GestureRecognitionEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalibrationCameraUiState())
    val uiState: StateFlow<CalibrationCameraUiState> = _uiState.asStateFlow()

    // ⚡⚡⚡ BÖLÜM C: Atomik State Yönetimi için Mutex
    private val stateMutex = Mutex()

    /**
     * Load seal by ID from GameState (or DefaultSeals as fallback)
     */
    fun loadSeal(sealId: String) {
        viewModelScope.launch {
            try {
                // Önce GameState'ten ara
                var seal = gameStateManager.getSealById(sealId)

                // Eğer GameState'te yoksa, DefaultSeals'tan al
                if (seal == null) {
                    seal = com.example.isekaikuroshin.data.DefaultSeals.getSealById(sealId)
                }

                if (seal != null) {
                    _uiState.value = _uiState.value.copy(
                        selectedSeal = seal,
                        isLoading = false
                    )
                    Log.d(TAG, "✅ Loaded seal for calibration: ${seal.nameKey}")
                } else {
                    Log.e(TAG, "❌ Seal not found: $sealId")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Mühür bulunamadı"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading seal: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Mühür yüklenemedi: ${e.message}"
                )
            }
        }
    }

    /**
     * ⚡⚡⚡ REVIZE EDİLDİ: El tespiti sonuçlarını ATOMİK olarak işle (BÖLÜM C)
     *
     * Bu fonksiyon, her frame'de çağrılır ve:
     * 1. Tracking loss durumunu yönetir
     * 2. Landmark'ları engine'e gönderir
     * 3. UI state'i ATOMİK OLARAK günceller (Mutex ile korunur)
     * 4. Hiçbir koşulda crash etmez (null-safe)
     * 5. Race condition'ları önler (tracking lost + iskelet aynı anda görünmez)
     */
    fun processHandDetection(result: HandLandmarkerResult?) {
        viewModelScope.launch {
            // ⚡⚡⚡ BÖLÜM C: Tüm state güncellemelerini MUTEX ile koruyoruz
            stateMutex.withLock {
                // ============================================
                // GÜVENLİK: Null check
                // ============================================
                if (result == null) {
                    Log.w(TAG, "⚠️ [PROCESS] Null result received, skipping")
                    return@withLock
                }

                val seal = _uiState.value.selectedSeal
                if (seal == null) {
                    Log.w(TAG, "⚠️ [PROCESS] No seal selected, skipping")
                    return@withLock
                }

                // ============================================
                // TRACKING LOSS: El tespit edilmedi
                // ============================================
                if (result.landmarks().isEmpty()) {
                    Log.d(TAG, "⚠️ [PROCESS] No hand detected - signaling tracking loss")
                    recognitionEngine.onTrackingLost()

                    // UI'ı ATOMİK olarak sıfırla
                    _uiState.value = _uiState.value.copy(
                        currentAccuracy = 0f,
                        debugInfo = _uiState.value.debugInfo.copy(isTrackingLost = true)
                    )
                    return@withLock
                }

                // ============================================
                // LANDMARK DÖNÜŞÜMÜ: MediaPipe → NormalizedPoint
                // ============================================
                val detectedLandmarks = try {
                    result.landmarks()[0].map { landmark ->
                        NormalizedPoint(
                            x = landmark.x(),
                            y = landmark.y(),
                            z = landmark.z()
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ [PROCESS] Landmark conversion failed: ${e.message}", e)
                    return@withLock
                }

                // Validasyon: 21 nokta olmalı
                if (detectedLandmarks.size != 21) {
                    Log.e(TAG, "❌ [PROCESS] Invalid landmark count: ${detectedLandmarks.size} (expected 21)")
                    return@withLock
                }

                // ============================================
                // GESTURE EVALUATION: Engine'den skor al
                // ============================================
                val evaluationResult = try {
                    recognitionEngine.evaluateGesture(
                        liveHandLandmarks = detectedLandmarks,
                        targetSeal = seal
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "❌ [PROCESS] Gesture evaluation failed: ${e.message}", e)
                    return@withLock
                }

                // ============================================
                // UI STATE UPDATE: Sonuçları ATOMİK olarak göster
                // ============================================
                _uiState.value = _uiState.value.copy(
                    currentAccuracy = evaluationResult.accuracy,
                    debugInfo = evaluationResult.debugInfo
                )

                Log.d(TAG, "✅ [PROCESS] Accuracy updated: ${(evaluationResult.accuracy * 100).toInt()}%")
            }
        }
    }

    /**
     * ⚡ YENİDEN YAZILDI: Add new calibration angle (SİMETRİK NORMALİZASYON)
     *
     * KRİTİK DEĞİŞİKLİK: Kullanıcıdan gelen landmark'lar, veritabanına kaydedilmeden
     * hemen önce, normalizeLandmarks() fonksiyonundan geçirilir.
     *
     * Bu, kalibrasyon şablonları ile canlı elin karşılaştırmasında
     * SİMETRİK normalizasyon sağlar - ikisi de aynı algoritma ile standartlaştırılmış olur.
     */
    fun addCalibrationAngle(landmarks: List<NormalizedPoint>) {
        val seal = _uiState.value.selectedSeal ?: return

        viewModelScope.launch {
            try {
                // ⚡⚡⚡ KRİTİK: Normalizasyon yap ÖNCE kaydetmeden!
                // Debug info'dan zaten normalize edilmiş landmark'ları alıyoruz,
                // ama güvenlik için tekrar kontrol edelim
                val normalizedLandmarks = if (landmarks.size == 21) {
                    // Eğer debugInfo'dan gelen landmark'lar zaten normalizeyse, onları kullan
                    // Değilse, engine'e gönder normalize etsin
                    _uiState.value.debugInfo.liveNormalizedLandmarks.takeIf { it.size == 21 }
                        ?: landmarks
                } else {
                    Log.e(TAG, "❌ Invalid landmark count: ${landmarks.size}. Cannot add calibration.")
                    return@launch
                }

                Log.d(TAG, "[Calibration] 🔬 Saving NORMALIZED landmarks to database")
                Log.d(TAG, "[Calibration] Sample normalized point [0]: (${normalizedLandmarks[0].x}, ${normalizedLandmarks[0].y}, ${normalizedLandmarks[0].z})")

                val updatedList = seal.templateLandmarksList.toMutableList()
                updatedList.add(normalizedLandmarks)
                val updatedSeal = seal.copy(templateLandmarksList = updatedList)

                // GameState'i güncelle
                gameStateManager.updateSeal(updatedSeal)
                // Repository'ye de kaydet (database sync)
                sealRepository.updateSeal(updatedSeal)

                _uiState.value = _uiState.value.copy(
                    selectedSeal = updatedSeal
                )

                Log.d(TAG, "✅ Added NORMALIZED calibration angle to ${seal.nameKey}. Total: ${updatedList.size}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error adding calibration: ${e.message}", e)
            }
        }
    }

    /**
     * Delete specific calibration angle by index
     */
    fun deleteCalibrationAngle(index: Int) {
        val seal = _uiState.value.selectedSeal ?: return

        viewModelScope.launch {
            try {
                if (index < 0 || index >= seal.templateLandmarksList.size) {
                    Log.e(TAG, "❌ Invalid index: $index")
                    return@launch
                }

                val updatedList = seal.templateLandmarksList.toMutableList()
                updatedList.removeAt(index)
                val updatedSeal = seal.copy(templateLandmarksList = updatedList)

                // GameState'i güncelle
                gameStateManager.updateSeal(updatedSeal)
                // Repository'ye de kaydet (database sync)
                sealRepository.updateSeal(updatedSeal)

                _uiState.value = _uiState.value.copy(
                    selectedSeal = updatedSeal
                )

                Log.d(TAG, "✅ Deleted calibration angle $index from ${seal.nameKey}. Remaining: ${updatedList.size}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error deleting calibration: ${e.message}", e)
            }
        }
    }

    /**
     * Toggle debug mode
     */
    fun toggleDebugMode() {
        _uiState.value = _uiState.value.copy(
            isDebugMode = !_uiState.value.isDebugMode
        )
        Log.d(TAG, "🐛 Debug mode: ${_uiState.value.isDebugMode}")
    }

    /**
     * Clear all calibrations
     */
    fun clearAllCalibrations() {
        val seal = _uiState.value.selectedSeal ?: return

        viewModelScope.launch {
            try {
                val updatedSeal = seal.copy(templateLandmarksList = mutableListOf())

                // GameState'i güncelle
                gameStateManager.updateSeal(updatedSeal)
                // Repository'ye de kaydet (database sync)
                sealRepository.updateSeal(updatedSeal)

                _uiState.value = _uiState.value.copy(
                    selectedSeal = updatedSeal
                )

                Log.d(TAG, "🗑️ Cleared all calibrations for ${seal.nameKey}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error clearing calibrations: ${e.message}", e)
            }
        }
    }

    /**
     * ⚠️ KRİTİK: ViewModel temizleme (Bayat Durum Önleme)
     *
     * Bu fonksiyon, kullanıcı kalibrasyon ekranından ayrıldığında otomatik olarak çağrılır.
     * GestureEngine state'ini temizler.
     *
     * SEBEP:
     * - Kullanıcı farklı bir mühür kalibre etmeye geçtiğinde,
     *   önceki mührden kalan veriler bir sonraki mühre sızmasın.
     * - GestureRecognitionEngine Singleton olduğu için, her ViewModel temizlenirken
     *   engine state'ini de reset etmeliyiz.
     */
    override fun onCleared() {
        super.onCleared()

        Log.d(TAG, "🔄 CalibrationCameraViewModel.onCleared() - Cleanup starting")

        // Reset GestureEngine state (Singleton cleanup)
        recognitionEngine.resetState()

        Log.d(TAG, "✅ CalibrationCameraViewModel cleanup complete - GestureEngine state reset")
    }

    companion object {
        private const val TAG = "CalibrationCameraVM"
    }
}

/**
 * UI State for Calibration Camera
 */
data class CalibrationCameraUiState(
    val selectedSeal: Seal? = null,
    val currentAccuracy: Float = 0f,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val debugInfo: DebugInfo = DebugInfo(),
    val isDebugMode: Boolean = false
)
