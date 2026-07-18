package com.example.isekaikuroshin.ui.calibration

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.isekaikuroshin.data.GameStateManager
import com.example.isekaikuroshin.data.NormalizedPoint
import com.example.isekaikuroshin.data.Seal
import com.example.isekaikuroshin.data.SealRepository
import com.example.isekaikuroshin.data.updateAcceptanceThreshold
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Calibration Center Screen
 *
 * Handles:
 * - Loading available seals for calibration
 * - Updating acceptance thresholds
 * - Managing calibration data
 *
 * NOTE: Kalibrasyon merkezi SEVIYE FİLTRELEMESİ YAPMAZ - tüm mühürleri gösterir!
 */
@HiltViewModel
class CalibrationCenterViewModel @Inject constructor(
    private val gameStateManager: GameStateManager,
    private val sealRepository: SealRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalibrationCenterUiState())
    val uiState: StateFlow<CalibrationCenterUiState> = _uiState.asStateFlow()

    init {
        // İlk başlatmada default seals'ları yükle
        gameStateManager.initializeDefaultSeals()
        loadSeals()
    }

    /**
     * Load ALL available seals for calibration (NO LEVEL FILTERING!)
     *
     * Kalibrasyon merkezi, oyuncunun tüm mühürleri kalibre edebilmesi için
     * seviye veya kilit durumuna bakmaksızın TÜM mühürleri gösterir.
     */
    private fun loadSeals() {
        viewModelScope.launch {
            try {
                // DefaultSeals'tan TÜM seals'ları al (seviye/unlock filtresi YOK!)
                val allSeals = com.example.isekaikuroshin.data.DefaultSeals.getAllDefaultSeals()

                _uiState.value = _uiState.value.copy(
                    availableSeals = allSeals,
                    isLoading = false
                )
                Log.d(TAG, "✅ Loaded ${allSeals.size} seals for calibration (NO LEVEL FILTER)")
                allSeals.forEach { seal ->
                    Log.d(TAG, "  • ${seal.nameKey} (ID: ${seal.id}, Tier: ${seal.tier})")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading seals: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Mühürler yüklenemedi: ${e.message}"
                )
            }
        }
    }

    /**
     * Select a seal for calibration
     */
    fun selectSealForCalibration(seal: Seal) {
        _uiState.value = _uiState.value.copy(
            selectedSeal = seal
        )
        Log.d(TAG, "🎯 Selected seal for calibration: ${seal.nameKey}")
    }

    /**
     * Update seal's acceptance threshold
     */
    fun updateSealThreshold(seal: Seal, newThreshold: Float) {
        viewModelScope.launch {
            try {
                val updatedSeal = seal.updateAcceptanceThreshold(newThreshold)
                // GameState'i güncelle
                gameStateManager.updateSeal(updatedSeal)
                // Repository'ye de kaydet (database sync)
                sealRepository.updateSeal(updatedSeal)
                Log.d(TAG, "✅ Updated threshold for ${seal.nameKey}: ${(newThreshold * 100).toInt()}%")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error updating threshold: ${e.message}", e)
            }
        }
    }

    /**
     * Add new calibration angle to seal
     */
    fun addCalibrationAngle(seal: Seal, landmarks: List<NormalizedPoint>) {
        viewModelScope.launch {
            try {
                val updatedList = seal.templateLandmarksList.toMutableList()
                updatedList.add(landmarks)
                val updatedSeal = seal.copy(templateLandmarksList = updatedList)
                // GameState'i güncelle
                gameStateManager.updateSeal(updatedSeal)
                // Repository'ye de kaydet (database sync)
                sealRepository.updateSeal(updatedSeal)
                Log.d(TAG, "✅ Added new calibration angle to ${seal.nameKey}. Total: ${updatedList.size}")

                // G83 P4: EventLogger integration
                com.example.isekaikuroshin.utils.EventLogger.logSealPractice(
                    sealName = seal.nameKey,
                    details = "Calibrated a new hand position for ${seal.nameKey} seal. Total calibration angles: ${updatedList.size}. This improves seal casting accuracy."
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error adding calibration: ${e.message}", e)
            }
        }
    }

    /**
     * Clear all calibrations for a seal
     *
     * ⚠️ KRİTİK DEĞİŞİKLİK (Bayat Durum Düzeltmesi):
     * Bu fonksiyon artık template listesini BOŞ bırakmıyor.
     * Bunun yerine, DefaultSeals.kt'deki ORİJİNAL template'e geri dönüyor!
     *
     * SEBEP:
     * - Kullanıcı kalibrasyon eklerken hata yaparsa ve "sıfırla"ya basarsa,
     *   mühür tamamen çalışmaz hale geliyordu (0% accuracy).
     * - Şimdi, sıfırlama = "fabrika ayarlarına dön" demek.
     */
    fun clearCalibrations(seal: Seal) {
        viewModelScope.launch {
            try {
                // DefaultSeals'tan orijinal template'i al
                val originalSeal = com.example.isekaikuroshin.data.DefaultSeals.getSealById(seal.id)

                if (originalSeal != null) {
                    // Orijinal template'i kullan (kullanıcı eklentilerini sil, orijinale dön)
                    val updatedSeal = seal.copy(
                        templateLandmarksList = originalSeal.templateLandmarksList.toMutableList(),
                        toleranceThreshold = originalSeal.toleranceThreshold,
                        acceptanceThreshold = originalSeal.acceptanceThreshold
                    )

                    // GameState'i güncelle
                    gameStateManager.updateSeal(updatedSeal)
                    // Repository'ye de kaydet (database sync)
                    sealRepository.updateSeal(updatedSeal)

                    Log.d(TAG, "✅ Reset ${seal.nameKey} to ORIGINAL template (${originalSeal.templateLandmarksList.size} templates)")
                    Log.d(TAG, "   • Tolerance threshold: ${originalSeal.toleranceThreshold}")
                    Log.d(TAG, "   • Acceptance threshold: ${originalSeal.acceptanceThreshold}")
                } else {
                    // Eğer DefaultSeals'ta yoksa (custom seal olabilir), o zaman boşalt
                    val updatedSeal = seal.copy(templateLandmarksList = mutableListOf())
                    gameStateManager.updateSeal(updatedSeal)
                    sealRepository.updateSeal(updatedSeal)
                    Log.w(TAG, "⚠️ ${seal.nameKey} is not in DefaultSeals, cleared all templates (custom seal)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error clearing calibrations: ${e.message}", e)
            }
        }
    }

    companion object {
        private const val TAG = "CalibrationCenterVM"
    }
}

/**
 * UI State for Calibration Center
 */
data class CalibrationCenterUiState(
    val availableSeals: List<Seal> = emptyList(),
    val selectedSeal: Seal? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
