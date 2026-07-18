package com.example.isekaikuroshin.ui.vfx

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.isekaikuroshin.data.DefaultSeals
import com.example.isekaikuroshin.data.GestureType
import com.example.isekaikuroshin.data.NormalizedPoint
import com.example.isekaikuroshin.data.Seal
import com.example.isekaikuroshin.data.SealDifficulty
import com.example.isekaikuroshin.data.VFXLevel
import com.example.isekaikuroshin.engine.GestureRecognitionEngine
import com.example.isekaikuroshin.utils.GameLogger
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VFXWorkshopViewModel @Inject constructor(
    private val gameStateManager: com.example.isekaikuroshin.data.GameStateManager,
    private val sealRepository: com.example.isekaikuroshin.data.SealRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VFXWorkshopUiState())
    val uiState: StateFlow<VFXWorkshopUiState> = _uiState

    // ⚡⚡⚡ KÖKTEN ÇÖZÜM: Repository'den reaktif veri akışı
    // Artık lokal _calibrationData YOK! Repository = Single Source of Truth
    private val _creationSealFlow = MutableStateFlow<Seal?>(null)
    private val _directionSealFlow = MutableStateFlow<Seal?>(null)

    // ⚡⚡⚡ PUBLIC: UI için tek veri kaynağı
    val creationSeal: StateFlow<Seal?> = _creationSealFlow
    val directionSeal: StateFlow<Seal?> = _directionSealFlow

    // ⚡⚡⚡ YENİ: Canlı doğruluk state'i (Çalışan kodun mantığını taklit ediyor)
    private val _currentAccuracy = MutableStateFlow<Map<GestureType, Float>>(emptyMap())
    val currentAccuracy: StateFlow<Map<GestureType, Float>> = _currentAccuracy

    // ⚡⚡⚡ KÖKTEN ÇÖZÜM: VFX Sealları için BENZERSİZ ID'ler (Kalibrasyon Merkezi ile çakışmaz!)
    companion object {
        private const val TAG = "VFXWorkshopViewModel"
        private const val VFX_CREATION_SEAL_ID = "vfx_gesture_creation"
        private const val VFX_DIRECTION_SEAL_ID = "vfx_gesture_direction"

        fun getSealIdForGesture(gestureType: GestureType): String {
            return when (gestureType) {
                GestureType.CREATION -> VFX_CREATION_SEAL_ID
                GestureType.DIRECTION -> VFX_DIRECTION_SEAL_ID
            }
        }
    }

    init {
        // ⚡⚡⚡ KÖKTEN ÇÖZÜM: Repository'yi gözlemle (StateFlow)
        observeSealsFromRepository()
    }

    /**
     * ⚡⚡⚡ KÖKTEN ÇÖZÜM: Repository'den Seal'ları gözlemle (Single Source of Truth)
     *
     * ⚠️ DÜZELTİLDİ: Her ViewModel oluşturulduğunda ESKİ Seal'ların ÜZERİNE YAZMA!
     * Sadece Seal yoksa oluştur, varsa mevcut olanı kullan!
     */
    private fun observeSealsFromRepository() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔍 [OBSERVE] Loading VFX Seals from repository...")

                // ⚡⚡⚡ DÜZELTİLDİ: Her iki Seal için de ÖNCELİKLE veritabanından yükle
                var creationSeal = sealRepository.getSealById(VFX_CREATION_SEAL_ID)
                var directionSeal = sealRepository.getSealById(VFX_DIRECTION_SEAL_ID)

                Log.d(TAG, "🔍 [OBSERVE] Creation Seal from DB: ${creationSeal?.templateLandmarksList?.size ?: 0} angles")
                Log.d(TAG, "🔍 [OBSERVE] Direction Seal from DB: ${directionSeal?.templateLandmarksList?.size ?: 0} angles")

                // ⚡⚡⚡ DÜZELTİLDİ: Sadece NULL ise oluştur (BOŞ olanları KORUMAK!)
                if (creationSeal == null) {
                    Log.d(TAG, "🆕 [OBSERVE] Creation Seal not found - creating new empty Seal")
                    creationSeal = createDefaultVFXSeal(GestureType.CREATION, VFX_CREATION_SEAL_ID)
                    sealRepository.insertOrUpdate(creationSeal)
                    gameStateManager.updateSeal(creationSeal)
                } else {
                    Log.d(TAG, "✅ [OBSERVE] Creation Seal found with ${creationSeal.templateLandmarksList.size} angles - NOT overwriting!")
                    // ⚡⚡⚡ ÖZEL: GameStateManager'ı da güncelle (memory cache)
                    gameStateManager.updateSeal(creationSeal)
                }

                if (directionSeal == null) {
                    Log.d(TAG, "🆕 [OBSERVE] Direction Seal not found - creating new empty Seal")
                    directionSeal = createDefaultVFXSeal(GestureType.DIRECTION, VFX_DIRECTION_SEAL_ID)
                    sealRepository.insertOrUpdate(directionSeal)
                    gameStateManager.updateSeal(directionSeal)
                } else {
                    Log.d(TAG, "✅ [OBSERVE] Direction Seal found with ${directionSeal.templateLandmarksList.size} angles - NOT overwriting!")
                    // ⚡⚡⚡ ÖZEL: GameStateManager'ı da güncelle (memory cache)
                    gameStateManager.updateSeal(directionSeal)
                }

                _creationSealFlow.value = creationSeal
                _directionSealFlow.value = directionSeal

                Log.d(TAG, "✅ [OBSERVE] Loaded VFX Seals - Creation: ${creationSeal.templateLandmarksList.size}, Direction: ${directionSeal.templateLandmarksList.size}")

            } catch (e: Exception) {
                Log.e(TAG, "❌ [OBSERVE] Error observing seals: ${e.message}", e)
                e.printStackTrace()
            }
        }
    }

    fun selectVFXLevel(level: VFXLevel) {
        _uiState.value = _uiState.value.copy(selectedVFXLevel = level)
    }

    fun setSensitivityThreshold(threshold: Float) {
        _uiState.value = _uiState.value.copy(sensitivityThreshold = threshold)
    }

    /**
     * ⚡⚡⚡ KÖKTEN ÇÖZÜM: Açı ekle (Repository'ye direkt yaz)
     *
     * Artık lokal state YOK! Direkt Repository'ye yazıyoruz.
     * StateFlow otomatik güncellenir.
     */
    fun completeCalibration(gestureType: GestureType, result: CalibrationResult) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔵 [COMPLETE_CALIB] Starting for $gestureType")

                val sealId = getSealIdForGesture(gestureType)
                Log.d(TAG, "🔵 [COMPLETE_CALIB] Seal ID: $sealId")

                // ⚡⚡⚡ DÜZELTİLDİ: SealRepository kullan (GameStateManager değil!)
                val currentSeal = sealRepository.getSealById(sealId)
                    ?: createDefaultVFXSeal(gestureType, sealId)

                Log.d(TAG, "🔵 [COMPLETE_CALIB] Current Seal angles BEFORE: ${currentSeal.templateLandmarksList.size}")

                // Yeni açıyı ekle
                val newLandmarks = result.landmarks.map { floatArray ->
                    NormalizedPoint(x = floatArray[0], y = floatArray[1], z = floatArray[2])
                }

                Log.d(TAG, "🔵 [COMPLETE_CALIB] New landmarks count: ${newLandmarks.size}")

                val updatedTemplates = currentSeal.templateLandmarksList.toMutableList().apply {
                    add(newLandmarks)
                }

                Log.d(TAG, "🔵 [COMPLETE_CALIB] Updated templates count: ${updatedTemplates.size}")

                val updatedSeal = currentSeal.copy(templateLandmarksList = updatedTemplates)

                Log.d(TAG, "🔵 [COMPLETE_CALIB] Saving to GameStateManager...")
                // Repository'ye kaydet (Single Source of Truth)
                gameStateManager.updateSeal(updatedSeal)

                Log.d(TAG, "🔵 [COMPLETE_CALIB] Saving to SealRepository...")
                sealRepository.insertOrUpdate(updatedSeal)

                Log.d(TAG, "🔵 [COMPLETE_CALIB] Updating StateFlow...")
                // ⚡⚡⚡ DÜZELTME: StateFlow'u güncelle (Compose için yeni kopya)
                when (gestureType) {
                    GestureType.CREATION -> {
                        _creationSealFlow.value = updatedSeal.copy() // YENİ KOPYA!
                        Log.d(TAG, "🔵 [COMPLETE_CALIB] Creation StateFlow updated")
                    }
                    GestureType.DIRECTION -> {
                        _directionSealFlow.value = updatedSeal.copy() // YENİ KOPYA!
                        Log.d(TAG, "🔵 [COMPLETE_CALIB] Direction StateFlow updated")
                    }
                }

                Log.d(TAG, "✅ [ADD] New angle added for $gestureType. Total: ${updatedTemplates.size}")
                Log.d(TAG, "✅ [ADD] StateFlow updated with new seal copy for immediate UI refresh")

                // G83 P4: EventLogger integration
                com.example.isekaikuroshin.utils.EventLogger.logGenericActivity(
                    "Calibrated a new ${gestureType.name} gesture angle in VFX Workshop. Total angles: ${updatedTemplates.size}. This improves VFX gesture recognition accuracy."
                )

            } catch (e: Exception) {
                Log.e(TAG, "❌ [ADD] Error adding calibration: ${e.message}", e)
                e.printStackTrace()
            }
        }
    }


    /**
     * ⚡⚡⚡ BÖLÜM A: VFX için default Seal oluştur
     */
    private fun createDefaultVFXSeal(gestureType: GestureType, sealId: String): Seal {
        return Seal(
            id = sealId,
            nameKey = when (gestureType) {
                GestureType.CREATION -> "vfx_creation_gesture"
                GestureType.DIRECTION -> "vfx_direction_gesture"
            },
            descriptionKey = "vfx_user_calibrated_desc",
            loreTextKey = "vfx_special_movement_lore",
            difficulty = SealDifficulty.NOVICE,
            tier = "VFX",
            templateLandmarksList = mutableListOf(),
            toleranceThreshold = 0.30f,
            acceptanceThreshold = 0.65f
        )
    }

    /**
     * ⚡⚡⚡ KÖKTEN ÇÖZÜM: Açı sil (Repository'ye direkt yaz)
     */
    fun deleteCalibrationAngle(gestureType: GestureType, angleIndex: Int) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🗑️ [DELETE] Starting delete for $gestureType, index: $angleIndex")

                val sealId = getSealIdForGesture(gestureType)
                Log.d(TAG, "🗑️ [DELETE] Seal ID: $sealId")

                // ⚡⚡⚡ DÜZELTİLDİ: SealRepository kullan (GameStateManager değil!)
                val currentSeal = sealRepository.getSealById(sealId)

                if (currentSeal == null) {
                    Log.e(TAG, "❌ [DELETE] Seal not found in repository: $sealId")
                    return@launch
                }

                Log.d(TAG, "🗑️ [DELETE] Current Seal angles BEFORE: ${currentSeal.templateLandmarksList.size}")

                val updatedTemplates = currentSeal.templateLandmarksList.toMutableList().apply {
                    if (angleIndex in indices) {
                        removeAt(angleIndex)
                        Log.d(TAG, "🗑️ [DELETE] Removed angle at index $angleIndex")
                    } else {
                        Log.e(TAG, "❌ [DELETE] Invalid index $angleIndex (list size: ${currentSeal.templateLandmarksList.size})")
                    }
                }

                val updatedSeal = currentSeal.copy(templateLandmarksList = updatedTemplates)

                Log.d(TAG, "🗑️ [DELETE] Saving to Repository...")
                // Repository'ye kaydet
                sealRepository.insertOrUpdate(updatedSeal)
                gameStateManager.updateSeal(updatedSeal)

                Log.d(TAG, "🗑️ [DELETE] Updating StateFlow...")
                // StateFlow'u güncelle
                when (gestureType) {
                    GestureType.CREATION -> _creationSealFlow.value = updatedSeal.copy()
                    GestureType.DIRECTION -> _directionSealFlow.value = updatedSeal.copy()
                }

                Log.d(TAG, "✅ [DELETE] Angle $angleIndex deleted. Remaining: ${updatedTemplates.size}")

            } catch (e: Exception) {
                Log.e(TAG, "❌ [DELETE] Error: ${e.message}", e)
                e.printStackTrace()
            }
        }
    }

    /**
     * ⚡⚡⚡ KÖKTEN ÇÖZÜM: Tüm açıları sil (Repository'ye direkt yaz)
     */
    fun clearAllCalibrations(gestureType: GestureType) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🗑️ [CLEAR] Starting clear for $gestureType")

                val sealId = getSealIdForGesture(gestureType)
                // ⚡⚡⚡ DÜZELTİLDİ: SealRepository kullan (GameStateManager değil!)
                val currentSeal = sealRepository.getSealById(sealId)

                if (currentSeal == null) {
                    Log.e(TAG, "❌ [CLEAR] Seal not found in repository: $sealId")
                    return@launch
                }

                Log.d(TAG, "🗑️ [CLEAR] Current Seal angles BEFORE: ${currentSeal.templateLandmarksList.size}")

                val updatedSeal = currentSeal.copy(templateLandmarksList = mutableListOf())

                // Repository'ye kaydet
                sealRepository.insertOrUpdate(updatedSeal)
                gameStateManager.updateSeal(updatedSeal)

                // StateFlow'u güncelle
                when (gestureType) {
                    GestureType.CREATION -> _creationSealFlow.value = updatedSeal.copy()
                    GestureType.DIRECTION -> _directionSealFlow.value = updatedSeal.copy()
                }

                Log.d(TAG, "✅ [CLEAR] All angles cleared for $gestureType")

            } catch (e: Exception) {
                Log.e(TAG, "❌ [CLEAR] Error: ${e.message}", e)
                e.printStackTrace()
            }
        }
    }

    fun startVFCTest() {
        _uiState.value = _uiState.value.copy(isTestActive = true)

        // ⚡⚡⚡ BÖLÜM D: Test başlangıcını logla
        val state = _uiState.value
        GameLogger.logVfxTest("========================================")
        GameLogger.logVfxTest("VFX TEST STARTED")
        GameLogger.logVfxTest("========================================")
        GameLogger.logVfxTest("VFX Level: ${state.selectedVFXLevel}")
        GameLogger.logVfxTest("Sensitivity Threshold: ${(state.sensitivityThreshold * 100).toInt()}%")
        GameLogger.logVfxTest("VFX Color: ${state.vfxColor}")
        GameLogger.logVfxTest("VFX Size: ${(state.vfxSize * 100).toInt()}%")
        GameLogger.logVfxTest("VFX Offset: (${state.vfxOffsetX}, ${state.vfxOffsetY})")
        GameLogger.logVfxTest("VFX Speed: ${(state.vfxSpeed * 100).toInt()}%")
        GameLogger.logVfxTest("Reference Point: ${state.referencePoint}")
        GameLogger.logVfxTest("Ghost Hand Enabled: ${state.isGhostHandEnabled}")
        GameLogger.logVfxTest("FPS Monitor Enabled: ${state.isFpsMonitorEnabled}")
        GameLogger.logVfxTest("Creation Gesture Angles: ${_creationSealFlow.value?.templateLandmarksList?.size ?: 0}")
        GameLogger.logVfxTest("Direction Gesture Angles: ${_directionSealFlow.value?.templateLandmarksList?.size ?: 0}")
        GameLogger.logVfxTest("========================================")
    }

    fun stopVFCTest() {
        _uiState.value = _uiState.value.copy(isTestActive = false)

        // ⚡⚡⚡ BÖLÜM D: Test sonucunu logla
        GameLogger.logVfxTest("========================================")
        GameLogger.logVfxTest("VFX TEST ENDED")
        GameLogger.logVfxTest("========================================")
    }

    // ⚡⚡⚡ BÖLÜM A: Yeni State Güncelleme Fonksiyonları

    /**
     * Efekt rengini güncelle
     */
    fun updateVfxColor(color: androidx.compose.ui.graphics.Color) {
        _uiState.value = _uiState.value.copy(vfxColor = color)
        Log.d(TAG, "🎨 [VFX_COLOR] Color updated: $color")
    }

    /**
     * Efekt boyutunu güncelle (0.5f - 2.0f)
     */
    fun updateVfxSize(size: Float) {
        val clampedSize = size.coerceIn(0.5f, 2.0f)
        _uiState.value = _uiState.value.copy(vfxSize = clampedSize)
        Log.d(TAG, "📏 [VFX_SIZE] Size updated: $clampedSize")
    }

    /**
     * Efekt şeklini güncelle (Daire, Üçgen, Dikdörtgen, Yıldız)
     */
    fun updateVfxShape(shape: com.example.isekaikuroshin.data.VFXShape) {
        _uiState.value = _uiState.value.copy(vfxShape = shape)
        Log.d(TAG, "🔷 [VFX_SHAPE] Shape updated: $shape")
    }

    /**
     * Efekt transparanlığını güncelle (0.1f - 1.0f)
     */
    fun updateVfxAlpha(alpha: Float) {
        val clampedAlpha = alpha.coerceIn(0.1f, 1.0f)
        _uiState.value = _uiState.value.copy(vfxAlpha = clampedAlpha)
        Log.d(TAG, "🌫️ [VFX_ALPHA] Alpha updated: $clampedAlpha")
    }

    /**
     * ⚡⚡⚡ BÖLÜM E: Efekt türünü güncelle (STANDARD, SMOKE, FIRE, vb.)
     */
    fun updateVfxEffectType(effectType: com.example.isekaikuroshin.data.VFXEffectType) {
        _uiState.value = _uiState.value.copy(vfxEffectType = effectType)
        Log.d(TAG, "💫 [VFX_EFFECT_TYPE] Effect type updated: $effectType")
    }

    /**
     * Efekt offset'ini güncelle (X ve Y)
     */
    fun updateVfxOffset(offsetX: Float, offsetY: Float) {
        val clampedX = offsetX.coerceIn(-200f, 200f)
        val clampedY = offsetY.coerceIn(-200f, 200f)
        _uiState.value = _uiState.value.copy(
            vfxOffsetX = clampedX,
            vfxOffsetY = clampedY
        )
        Log.d(TAG, "📍 [VFX_OFFSET] Offset updated: ($clampedX, $clampedY)")
    }

    /**
     * Hareket hızını/hassasiyetini güncelle (0.1f - 2.0f)
     */
    fun updateVfxSpeed(speed: Float) {
        val clampedSpeed = speed.coerceIn(0.1f, 2.0f)
        _uiState.value = _uiState.value.copy(vfxSpeed = clampedSpeed)
        Log.d(TAG, "⚡ [VFX_SPEED] Speed updated: $clampedSpeed")
    }

    /**
     * Referans noktayı güncelle (Avuç içi veya İşaret parmağı)
     */
    fun updateReferencePoint(point: HandReferencePoint) {
        _uiState.value = _uiState.value.copy(referencePoint = point)
        Log.d(TAG, "👆 [VFX_REF_POINT] Reference point updated: $point")
    }

    /**
     * Hayalet el özelliğini aç/kapat
     */
    fun toggleGhostHand(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isGhostHandEnabled = enabled)
        Log.d(TAG, "👻 [VFX_GHOST_HAND] Ghost hand: ${if (enabled) "ENABLED" else "DISABLED"}")
    }

    /**
     * FPS monitörünü aç/kapat
     */
    fun toggleFpsMonitor(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isFpsMonitorEnabled = enabled)
        Log.d(TAG, "📊 [VFX_FPS_MONITOR] FPS monitor: ${if (enabled) "ENABLED" else "DISABLED"}")
    }

    /**
     * ⚡⚡⚡ YENİ: Başarılı VFX testini Kalibrasyon Merkezi'ne kaydet
     *
     * Bu fonksiyon, VFX test sonucunu "Pratik Mühür" olarak Kalibrasyon Merkezi'ne kaydeder.
     * Böylece kullanıcı Kalibrasyon Merkezi'nden kalibre ettiği hareketlerini görebilir.
     */
    fun saveVFXTestAsPracticeSeal(gestureType: GestureType) {
        viewModelScope.launch {
            try {
                val vfxSealId = getSealIdForGesture(gestureType)
                val vfxSeal = gameStateManager.getSealById(vfxSealId)

                if (vfxSeal == null || vfxSeal.templateLandmarksList.isEmpty()) {
                    Log.w(TAG, "⚠️ [SAVE_PRACTICE] VFX Seal not found or empty: $gestureType")
                    return@launch
                }

                // Yeni "Pratik Mühür" ID'si oluştur (unique)
                val practiceSealId = "practice_vfx_${gestureType.name.lowercase()}_${System.currentTimeMillis()}"

                // Pratik Mührü oluştur (VFX Seal'ın kopyası olarak)
                val practiceSeal = Seal(
                    id = practiceSealId,
                    nameKey = "practice_vfx_${gestureType.name.lowercase()}",
                    descriptionKey = "vfx_saved_from_test_desc",
                    loreTextKey = "vfx_calibrated_in_workshop_lore",
                    difficulty = SealDifficulty.NOVICE,
                    tier = "Pratik",
                    templateLandmarksList = vfxSeal.templateLandmarksList.toMutableList(), // Kopyala
                    toleranceThreshold = vfxSeal.toleranceThreshold,
                    acceptanceThreshold = vfxSeal.acceptanceThreshold,
                    prerequisiteSealId = null,
                    minPlayerLevel = 1,
                    relatedSkillIds = emptyList(),
                    masteryLevel = 0,
                    practiceMetrics = com.example.isekaikuroshin.data.PracticeMetrics()
                )

                // Kalibrasyon Merkezi'ne kaydet
                gameStateManager.updateSeal(practiceSeal)
                sealRepository.insertOrUpdate(practiceSeal)

                Log.d(TAG, "✅ [SAVE_PRACTICE] VFX Test saved as practice seal: $practiceSealId")
                Log.d(TAG, "✅ [SAVE_PRACTICE] Angles: ${practiceSeal.templateLandmarksList.size}")

            } catch (e: Exception) {
                Log.e(TAG, "❌ [SAVE_PRACTICE] Error: ${e.message}", e)
            }
        }
    }

    /**
     * ⚡⚡⚡ YENİ: Tüm VFX testini Kalibrasyon Merkezi'ne kaydet
     * Her iki hareket için de pratik mühür oluşturur
     */
    fun saveCompleteVFXTestAsPracticeSeals() {
        viewModelScope.launch {
            // Her iki hareketi de kaydet
            saveVFXTestAsPracticeSeal(GestureType.CREATION)
            saveVFXTestAsPracticeSeal(GestureType.DIRECTION)

            Log.d(TAG, "🎉 [SAVE_PRACTICE] Complete VFX Test saved to Calibration Center")
        }
    }

    /**
     * ⚡⚡⚡ KÖKTEN ÇÖZÜM: Gesture kalibre edildi mi? (Repository'den oku)
     */
    fun isGestureCalibrated(gestureType: GestureType): Boolean {
        val seal = when (gestureType) {
            GestureType.CREATION -> _creationSealFlow.value
            GestureType.DIRECTION -> _directionSealFlow.value
        }
        return seal != null && seal.templateLandmarksList.isNotEmpty()
    }

    /**
     * ⚡⚡⚡ KÖKTEN ÇÖZÜM: Açı sayısını al (Repository'den oku)
     */
    fun getAngleCount(gestureType: GestureType): Int {
        val seal = when (gestureType) {
            GestureType.CREATION -> _creationSealFlow.value
            GestureType.DIRECTION -> _directionSealFlow.value
        }
        return seal?.templateLandmarksList?.size ?: 0
    }

    /**
     * ⚡⚡⚡ YENİ FONKSİYON: Normalizasyon ile açı ekle
     *
     * Bu fonksiyon, çalışan Kalibrasyon Merkezi kodundaki addCalibrationAngle() mantığını
     * taklit eder. Engine'den normalize edilmiş landmark'ları alır.
     */
    fun addAngleWithNormalization(
        result: HandLandmarkerResult?,
        gestureType: GestureType,
        recognitionEngine: GestureRecognitionEngine
    ) {
        Log.d(TAG, "🔵 [ADD_ANGLE_NORM] Function called for $gestureType")
        Log.d(TAG, "🔵 [ADD_ANGLE_NORM] Hand result: ${result != null}, landmarks: ${result?.landmarks()?.size ?: 0}")

        if (result == null || result.landmarks().isEmpty()) {
            Log.w(TAG, "⚠️ [ADD_ANGLE_NORM] No hand detected - ABORT")
            return
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "🔵 [ADD_ANGLE_NORM] Converting landmarks to NormalizedPoint...")

                // RAW landmark'ları al
                val detectedLandmarks = result.landmarks()[0].map { landmark ->
                    NormalizedPoint(
                        x = landmark.x(),
                        y = landmark.y(),
                        z = landmark.z()
                    )
                }

                Log.d(TAG, "🔵 [ADD_ANGLE_NORM] Detected ${detectedLandmarks.size} landmarks")
                Log.d(TAG, "🔵 [ADD_ANGLE_NORM] Sample RAW point [0]: (${detectedLandmarks[0].x}, ${detectedLandmarks[0].y}, ${detectedLandmarks[0].z})")

                // ⚡⚡⚡ KRİTİK: Temporary Seal oluştur (sadece normalizasyon için)
                val tempSeal = Seal(
                    id = "temp_normalization",
                    nameKey = "temp_seal",
                    descriptionKey = "temp_seal_desc",
                    loreTextKey = "temp_seal_lore",
                    difficulty = SealDifficulty.NOVICE,
                    tier = "Temp",
                    templateLandmarksList = mutableListOf(),
                    toleranceThreshold = 0.30f,
                    acceptanceThreshold = 0.65f
                )

                Log.d(TAG, "🔵 [ADD_ANGLE_NORM] Calling recognitionEngine.evaluateGesture()...")

                // Engine'i çağır (bu, landmark'ları normalize eder)
                val evaluationResult = recognitionEngine.evaluateGesture(
                    liveHandLandmarks = detectedLandmarks,
                    targetSeal = tempSeal
                )

                Log.d(TAG, "🔵 [ADD_ANGLE_NORM] Engine evaluation complete!")

                // ⚡⚡⚡ Normalize edilmiş landmark'ları al (debugInfo'dan)
                val normalizedLandmarks = evaluationResult.debugInfo.liveNormalizedLandmarks

                Log.d(TAG, "🔵 [ADD_ANGLE_NORM] Normalized landmark count: ${normalizedLandmarks.size}")

                if (normalizedLandmarks.size != 21) {
                    Log.e(TAG, "❌ [ADD_ANGLE_NORM] Invalid normalized landmark count: ${normalizedLandmarks.size}")
                    return@launch
                }

                Log.d(TAG, "🔵 [ADD_ANGLE_NORM] 🔬 Saving NORMALIZED landmarks to Repository")
                Log.d(TAG, "🔵 [ADD_ANGLE_NORM] Sample normalized point [0]: (${normalizedLandmarks[0].x}, ${normalizedLandmarks[0].y}, ${normalizedLandmarks[0].z})")

                // CalibrationResult oluştur
                val calibrationResult = CalibrationResult(
                    landmarks = normalizedLandmarks.map { floatArrayOf(it.x, it.y, it.z) },
                    confidence = 1.0f,
                    timestamp = System.currentTimeMillis()
                )

                Log.d(TAG, "🔵 [ADD_ANGLE_NORM] Calling completeCalibration()...")

                // ViewModel'e kaydet
                completeCalibration(gestureType, calibrationResult)

                Log.d(TAG, "✅ [ADD_ANGLE_NORM] NORMALIZED angle added for $gestureType - DONE!")
            } catch (e: Exception) {
                Log.e(TAG, "❌ [ADD_ANGLE_NORM] Error: ${e.message}", e)
                e.printStackTrace()
            }
        }
    }

    /**
     * ⚡⚡⚡ YENİ: 3D Offset Konfigürasyonu Kaydet
     * İnteraktif kalibrasyon ekranından gelen offset'i kaydeder
     */
    fun saveOffsetConfiguration(gestureType: GestureType, offsetConfig: com.example.isekaikuroshin.data.VFXOffsetConfig) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "💾 [SAVE_OFFSET] Saving offset config for $gestureType")
                Log.d(TAG, "💾 [SAVE_OFFSET] Offset: ${offsetConfig.offset3D}")

                // UiState'i güncelle
                _uiState.value = when (gestureType) {
                    GestureType.CREATION -> _uiState.value.copy(creationOffsetConfig = offsetConfig)
                    GestureType.DIRECTION -> _uiState.value.copy(directionOffsetConfig = offsetConfig)
                }

                Log.d(TAG, "✅ [SAVE_OFFSET] Offset configuration saved successfully")
            } catch (e: Exception) {
                Log.e(TAG, "❌ [SAVE_OFFSET] Error: ${e.message}", e)
            }
        }
    }

    /**
     * ⚡⚡⚡ YENİ: 3D Offset Konfigürasyonu Sil
     */
    fun deleteOffsetConfiguration(gestureType: GestureType) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🗑️ [DELETE_OFFSET] Deleting offset config for $gestureType")

                // UiState'i güncelle
                _uiState.value = when (gestureType) {
                    GestureType.CREATION -> _uiState.value.copy(creationOffsetConfig = null)
                    GestureType.DIRECTION -> _uiState.value.copy(directionOffsetConfig = null)
                }

                Log.d(TAG, "✅ [DELETE_OFFSET] Offset configuration deleted successfully")
            } catch (e: Exception) {
                Log.e(TAG, "❌ [DELETE_OFFSET] Error: ${e.message}", e)
            }
        }
    }

    /**
     * ⚡⚡⚡ YENİ FONKSİYON: El tespiti sonuçlarını işle ve canlı doğruluğu hesapla
     *
     * Bu fonksiyon, çalışan Kalibrasyon Merkezi kodundaki processHandDetection() mantığını
     * VFX Workshop için adapte eder.
     *
     * ÖNEMLİ: Her frame'de çağrılır, ancak Seal objesi SADECE BİR KEZ oluşturulur.
     */
    fun processHandDetection(
        result: HandLandmarkerResult?,
        gestureType: GestureType,
        recognitionEngine: GestureRecognitionEngine
    ) {
        viewModelScope.launch {
            // ============================================
            // GÜVENLİK: Null check
            // ============================================
            if (result == null || result.landmarks().isEmpty()) {
                recognitionEngine.onTrackingLost()

                // Doğruluğu sıfırla
                val updatedAccuracy = _currentAccuracy.value.toMutableMap()
                updatedAccuracy[gestureType] = 0f
                _currentAccuracy.value = updatedAccuracy

                return@launch
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
                return@launch
            }

            // Validasyon: 21 nokta olmalı
            if (detectedLandmarks.size != 21) {
                Log.e(TAG, "❌ [PROCESS] Invalid landmark count: ${detectedLandmarks.size} (expected 21)")
                return@launch
            }

            // ============================================
            // ⚡⚡⚡ KÖKTEN ÇÖZÜM: Repository'den Seal al (Single Source of Truth)
            // ============================================
            val targetSeal = when (gestureType) {
                GestureType.CREATION -> _creationSealFlow.value
                GestureType.DIRECTION -> _directionSealFlow.value
            }

            // Eğer Seal yoksa veya template'ler boşsa, işleme
            if (targetSeal == null || targetSeal.templateLandmarksList.isEmpty()) {
                recognitionEngine.onTrackingLost()
                val updatedAccuracy = _currentAccuracy.value.toMutableMap()
                updatedAccuracy[gestureType] = 0f
                _currentAccuracy.value = updatedAccuracy
                return@launch
            }

            // ============================================
            // GESTURE EVALUATION: Engine'den skor al
            // ============================================
            val evaluationResult = try {
                recognitionEngine.evaluateGesture(
                    liveHandLandmarks = detectedLandmarks,
                    targetSeal = targetSeal
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ [PROCESS] Gesture evaluation failed: ${e.message}", e)
                return@launch
            }

            // ============================================
            // UI STATE UPDATE: Doğruluğu güncelle
            // ============================================
            val updatedAccuracy = _currentAccuracy.value.toMutableMap()
            updatedAccuracy[gestureType] = evaluationResult.accuracy
            _currentAccuracy.value = updatedAccuracy

            Log.d(TAG, "✅ [PROCESS] Accuracy for $gestureType: ${(evaluationResult.accuracy * 100).toInt()}%")
        }
    }
}

data class VFXWorkshopUiState(
    val selectedVFXLevel: VFXLevel = VFXLevel.SPRITE,
    val sensitivityThreshold: Float = 0.65f,
    val isTestActive: Boolean = false,
    // ⚡⚡⚡ BÖLÜM A: Yeni Davranış Ayarları
    val vfxColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF00BFFF), // Efekt rengi (varsayılan: Turkuaz cyan blue)
    val vfxSize: Float = 1.0f, // Efekt boyutu (0.5f - 2.0f arası)
    val vfxShape: com.example.isekaikuroshin.data.VFXShape = com.example.isekaikuroshin.data.VFXShape.CIRCLE, // Efekt şekli (Daire, Üçgen, Dikdörtgen, Yıldız)
    val vfxAlpha: Float = 1.0f, // Transparanlık (0.1f - 1.0f arası, 1.0f = tam opak)
    // ⚡⚡⚡ BÖLÜM D-E: Efekt Türü (Duman, Ateş, Patlama vb.)
    val vfxEffectType: com.example.isekaikuroshin.data.VFXEffectType = com.example.isekaikuroshin.data.VFXEffectType.STANDARD,
    val vfxOffsetX: Float = 0f, // Ele göre X uzaklığı (-200f - 200f arası)
    val vfxOffsetY: Float = 0f, // Ele göre Y uzaklığı (-200f - 200f arası)
    val vfxSpeed: Float = 1.0f, // Takip hızı/hassasiyeti (0.1f - 2.0f arası)
    val referencePoint: HandReferencePoint = HandReferencePoint.INDEX_FINGER, // Referans nokta
    val isGhostHandEnabled: Boolean = true, // Hayalet el özelliği açık/kapalı
    val isFpsMonitorEnabled: Boolean = true, // FPS göstergesi açık/kapalı
    // ⚡⚡⚡ YENİ: 3D Offset Konfigürasyonları
    val creationOffsetConfig: com.example.isekaikuroshin.data.VFXOffsetConfig? = null, // Yaratma hareketi için 3D offset
    val directionOffsetConfig: com.example.isekaikuroshin.data.VFXOffsetConfig? = null // Yönlendirme hareketi için 3D offset
)

/**
 * El referans noktası (Efektin hangi noktayı takip edeceği)
 */
enum class HandReferencePoint {
    PALM_CENTER,       // Avuç İçi (landmark index 9)
    INDEX_FINGER       // İşaret Parmağı Ucu (landmark index 8)
}

data class CalibrationResult(
    val landmarks: List<FloatArray>, // El landmarks verisi
    val confidence: Float,
    val timestamp: Long
)