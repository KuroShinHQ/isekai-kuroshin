package com.example.isekaikuroshin.ui.spellstudio

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.isekaikuroshin.data.*
import com.example.isekaikuroshin.data.spell.*
import com.example.isekaikuroshin.engine.GestureRecognitionEngine
import com.example.isekaikuroshin.engine.vfx.ParticleSystemManager
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Büyü Tasarım Stüdyosu ViewModel
 *
 * ⚡⚡⚡ VFX WORKSHOP ENTEGRASYONU:
 * - SealRepository ile veri kalıcılığı
 * - GestureRecognitionEngine ile canlı doğruluk hesaplama
 * - StateFlow ile reaktif veri yönetimi
 */
@HiltViewModel
class SpellStudioViewModel @Inject constructor(
    private val gameStateManager: GameStateManager,
    private val sealRepository: SealRepository,
    val particleSystemManager: ParticleSystemManager,  // ⚡ GERÇEK EFEKTLER İÇİN
    val spellActionExecutor: com.example.isekaikuroshin.engine.vfx.SpellActionExecutor  // ⚡ SPELL EXECUTOR
) : ViewModel() {

    // Kullanıcının oluşturduğu büyüler
    private val _userSpells = MutableStateFlow<List<SpellRecipe>>(emptyList())
    val userSpells: StateFlow<List<SpellRecipe>> = _userSpells.asStateFlow()

    // Şu anda düzenlenen büyü
    private val _currentSpell = MutableStateFlow<SpellRecipe?>(null)
    val currentSpell: StateFlow<SpellRecipe?> = _currentSpell.asStateFlow()

    // UI durumu
    private val _uiState = MutableStateFlow<SpellStudioUiState>(SpellStudioUiState.SpellList)
    val uiState: StateFlow<SpellStudioUiState> = _uiState.asStateFlow()

    // ⚡⚡⚡ YENİ: Kalibrasyon için Seal'lar (her SpellStep için ayrı)
    private val _calibrationSeals = MutableStateFlow<Map<String, Seal>>(emptyMap())
    val calibrationSeals: StateFlow<Map<String, Seal>> = _calibrationSeals.asStateFlow()

    // ⚡⚡⚡ YENİ: Canlı doğruluk hesaplama (her SpellStep için)
    private val _currentAccuracy = MutableStateFlow<Map<String, Float>>(emptyMap())
    val currentAccuracy: StateFlow<Map<String, Float>> = _currentAccuracy.asStateFlow()

    // FAZ 7: Level up bildirimi için
    private val _levelUpSpellId = MutableStateFlow<String?>(null)
    val levelUpSpellId: StateFlow<String?> = _levelUpSpellId.asStateFlow()

    // GÖREV 3: XP Kazanım Eventi
    private val _xpGainEvent = MutableStateFlow<XPGainEvent?>(null)
    val xpGainEvent: StateFlow<XPGainEvent?> = _xpGainEvent.asStateFlow()

    /**
     * GÖREV 3: XP event'i temizle (tek seferlik gösterim için)
     */
    fun clearXPGainEvent() {
        _xpGainEvent.value = null
    }

    companion object {
        private const val TAG = "SpellStudioViewModel"

        // ⚡⚡⚡ Seal ID formatı: "spell_{spellId}_step_{stepId}"
        fun getSealIdForStep(spellId: String, stepId: String): String {
            return "spell_${spellId}_step_${stepId}"
        }
    }

    init {
        loadUserSpells()
    }

    /**
     * Kullanıcının büyülerini yükle
     */
    private fun loadUserSpells() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "📂 Loading user spells from SealRepository...")
                // ⚡⚡⚡ KRİTİK: SealRepository'den SpellRecipe'leri yükle
                val spells = sealRepository.getAllSpellRecipes()
                _userSpells.value = spells
                Log.d(TAG, "✅ Loaded ${spells.size} spell(s) from storage")
                spells.forEachIndexed { index, spell ->
                    Log.d(TAG, "   ${index + 1}. ${spell.name} - ${spell.steps.size} step(s)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading spells: ${e.message}", e)
                _userSpells.value = emptyList()
            }
        }
    }

    /**
     * Yeni büyü oluştur
     */
    fun createNewSpell(name: String) {
        val newSpell = SpellRecipe(
            name = name,
            description = "",
            steps = emptyList()
        )
        _currentSpell.value = newSpell
        _uiState.value = SpellStudioUiState.EditingRecipe(newSpell)
    }

    /**
     * Mevcut büyüyü düzenle
     */
    fun editSpell(spell: SpellRecipe) {
        _currentSpell.value = spell
        _uiState.value = SpellStudioUiState.EditingRecipe(spell)
    }

    /**
     * Büyü adımı ekle
     */
    fun addStep(trigger: SpellTrigger, action: SpellAction) {
        val current = _currentSpell.value ?: return
        val newStep = SpellStep(
            stepNumber = current.steps.size + 1,
            trigger = trigger,
            action = action
        )
        val updatedSteps = current.steps + newStep
        _currentSpell.value = current.copy(
            steps = updatedSteps,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * G88: Tetikleyici kalibrasyonu başlat (22 Ekim backup referans)
     * Hand Gesture seçildiğinde calibration ekranına geçiş yapar
     */
    fun startTriggerCalibration(triggerType: String) {
        val current = _currentSpell.value ?: return

        // Default trigger ve action ile yeni step oluştur
        val defaultTrigger = when (triggerType) {
            "SINGLE_HAND" -> SpellTrigger.SingleHandGesture(
                gestureName = "Uncalibrated",
                gestureData = "{}"
            )
            "DOUBLE_HAND" -> SpellTrigger.DoubleHandGesture(
                leftGestureName = "Uncalibrated",
                rightGestureName = "Uncalibrated",
                leftGestureData = "{}",
                rightGestureData = "{}"
            )
            else -> return
        }

        val defaultAction = SpellAction.EmitParticles()

        val newStep = SpellStep(
            stepNumber = current.steps.size + 1,
            trigger = defaultTrigger,
            action = defaultAction
        )

        // Calibration ekranına geç
        _uiState.value = SpellStudioUiState.CalibratingTrigger(newStep)
    }

    /**
     * Büyü adımını sil
     */
    fun removeStep(stepId: String) {
        val current = _currentSpell.value ?: return
        val updatedSteps = current.steps.filter { it.id != stepId }
            .mapIndexed { index, step -> step.copy(stepNumber = index + 1) }
        _currentSpell.value = current.copy(
            steps = updatedSteps,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Büyü adımını güncelle
     */
    fun updateStep(stepId: String, newTrigger: SpellTrigger?, newAction: SpellAction?) {
        Log.e(TAG, "🔥🔥🔥 UPDATE_STEP called for stepId: $stepId")
        Log.e(TAG, "   New Trigger: ${newTrigger?.let { it::class.simpleName }}")
        Log.e(TAG, "   New Action: ${newAction?.let { it::class.simpleName }}")

        val current = _currentSpell.value ?: run {
            Log.e(TAG, "❌ UPDATE_STEP failed: currentSpell is null")
            return
        }

        val updatedSteps = current.steps.map { step ->
            if (step.id == stepId) {
                step.copy(
                    trigger = newTrigger ?: step.trigger,
                    action = newAction ?: step.action
                )
            } else {
                step
            }
        }

        val updatedSpell = current.copy(
            steps = updatedSteps,
            updatedAt = System.currentTimeMillis()
        )

        _currentSpell.value = updatedSpell

        // ⚡⚡⚡ KRİTİK: UI State'i de güncelle!
        _uiState.value = SpellStudioUiState.EditingRecipe(updatedSpell)

        Log.e(TAG, "✅ UPDATE_STEP completed. Updated spell has ${updatedSteps.size} steps")
        updatedSteps.find { it.id == stepId }?.let { updatedStep ->
            Log.e(TAG, "   Step $stepId now has trigger: ${updatedStep.trigger::class.simpleName}")
            Log.e(TAG, "   Step $stepId now has action: ${updatedStep.action::class.simpleName}")
        }
    }

    /**
     * Büyüyü kaydet
     */
    fun saveSpell() {
        Log.e(TAG, "💾💾💾 SAVE_SPELL called")
        val current = _currentSpell.value ?: run {
            Log.e(TAG, "❌ SAVE_SPELL failed: currentSpell is null")
            return
        }

        Log.e(TAG, "   Saving spell: ${current.name}")
        Log.e(TAG, "   Steps: ${current.steps.size}")
        current.steps.forEachIndexed { index, step ->
            Log.e(TAG, "   Step ${index + 1}: ${step.trigger::class.simpleName} -> ${step.action::class.simpleName}")
        }

        viewModelScope.launch {
            try {
                // Listeye ekle veya güncelle
                val existingIndex = _userSpells.value.indexOfFirst { it.id == current.id }
                if (existingIndex >= 0) {
                    // Güncelle
                    Log.e(TAG, "   Updating existing spell at index $existingIndex")
                    val updated = _userSpells.value.toMutableList()
                    updated[existingIndex] = current
                    _userSpells.value = updated
                } else {
                    // Yeni ekle
                    Log.e(TAG, "   Adding new spell to list")
                    _userSpells.value = _userSpells.value + current
                }

                // ⚡⚡⚡ KRİTİK: SealRepository'ye kaydet
                Log.e(TAG, "   Saving to SealRepository...")
                sealRepository.saveSpellRecipe(current)
                Log.e(TAG, "   ✅ Saved to persistent storage")

                // G83: Log spell creation/update to Journal
                if (existingIndex >= 0) {
                    com.example.isekaikuroshin.utils.EventLogger.logGenericActivity(
                        "I updated my spell '${current.name}' with ${current.steps.size} step(s)"
                    )
                } else {
                    com.example.isekaikuroshin.utils.EventLogger.logGenericActivity(
                        "I created a new spell: '${current.name}' with ${current.steps.size} step(s)"
                    )
                }

                // Ana ekrana dön
                _currentSpell.value = null
                _uiState.value = SpellStudioUiState.SpellList
                Log.e(TAG, "✅ SAVE_SPELL completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "❌ SAVE_SPELL error: ${e.message}", e)
            }
        }
    }

    /**
     * Düzenlemeyi iptal et
     */
    fun cancelEditing() {
        _currentSpell.value = null
        _uiState.value = SpellStudioUiState.SpellList
    }

    /**
     * FAZ 7: Level up dialog'u kapat
     */
    fun dismissLevelUpDialog() {
        _levelUpSpellId.value = null
    }

    /**
     * FAZ 7: Spell ID'ye göre LearnedSpell bilgisini al
     */
    fun getLearnedSpellInfo(spellId: String): Pair<String, Int>? {
        val gameData = com.example.isekaikuroshin.data.PersistentDataManager.gameData.value
        val learnedSpell = gameData.playerData.learnedSpells.find { it.id == spellId }
        return if (learnedSpell != null) {
            val spellRecipe = _userSpells.value.find { it.id == spellId }
            Pair(spellRecipe?.name ?: spellId, learnedSpell.skillTreeLevel)
        } else {
            null
        }
    }

    /**
     * Büyüyü sil
     */
    fun deleteSpell(spellId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🗑️ Deleting spell: $spellId")
                _userSpells.value = _userSpells.value.filter { it.id != spellId }
                // ⚡⚡⚡ KRİTİK: SealRepository'den de sil
                sealRepository.deleteSpellRecipe(spellId)
                Log.d(TAG, "✅ Spell deleted from storage")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error deleting spell: ${e.message}", e)
            }
        }
    }

    /**
     * Büyüyü test et
     */
    fun testSpell(spell: SpellRecipe) {
        Log.e(TAG, "🧪🧪🧪 TEST_SPELL called")
        Log.e(TAG, "   Requested spell: ${spell.name}, ${spell.steps.size} steps")

        // ⚡⚡⚡ KRİTİK: Eğer düzenleme modundaysak, _currentSpell'i kullan!
        val spellToTest = if (_currentSpell.value != null && _currentSpell.value!!.id == spell.id) {
            Log.e(TAG, "   Using _currentSpell (editing mode)")
            _currentSpell.value!!
        } else {
            Log.e(TAG, "   Using parameter spell (list mode)")
            spell
        }

        spellToTest.steps.forEachIndexed { index, step ->
            Log.e(TAG, "   Step ${index + 1}: Trigger=${step.trigger::class.simpleName}, Action=${step.action::class.simpleName}")
            when (val trigger = step.trigger) {
                is SpellTrigger.SingleHandGesture -> {
                    Log.e(TAG, "      Gesture: ${trigger.gestureName}, Data length: ${trigger.gestureData.length}")
                }
                else -> {}
            }
        }

        _uiState.value = SpellStudioUiState.TestingSpell(spellToTest)
        Log.e(TAG, "✅ TEST_SPELL completed")
    }

    /**
     * Test modundan çık
     */
    fun exitTestMode() {
        _uiState.value = SpellStudioUiState.SpellList
    }

    /**
     * Tetikleyici kalibrasyonuna git
     */
    // Calibration özelliği kaldırıldı - kullanılmıyor

    /**
     * Eylem ayarlarına git
     */
    fun configureAction(stepId: String) {
        val current = _currentSpell.value ?: return
        val step = current.steps.find { it.id == stepId } ?: return
        _uiState.value = SpellStudioUiState.ConfiguringAction(step)
    }

    /**
     * Kalibrasyon/Ayardan geri dön
     */
    fun returnToRecipeEditor() {
        Log.e(TAG, "🔙🔙🔙 RETURN_TO_RECIPE called")
        val current = _currentSpell.value ?: run {
            Log.e(TAG, "❌ RETURN_TO_RECIPE failed: currentSpell is null")
            return
        }

        // G88: Eğer calibration/configuration'dan geliyorsak ve step henüz spell'de yoksa ekle
        val currentState = _uiState.value
        when (currentState) {
            is SpellStudioUiState.CalibratingTrigger -> {
                val step = currentState.step
                if (!current.steps.any { it.id == step.id }) {
                    // Step henüz spell'de yok, ekle
                    val updatedSteps = current.steps + step
                    _currentSpell.value = current.copy(
                        steps = updatedSteps,
                        updatedAt = System.currentTimeMillis()
                    )
                    Log.d(TAG, "✅ Added new step from calibration: ${step.id}")
                }
            }
            is SpellStudioUiState.ConfiguringAction -> {
                val step = currentState.step
                if (!current.steps.any { it.id == step.id }) {
                    // Step henüz spell'de yok, ekle
                    val updatedSteps = current.steps + step
                    _currentSpell.value = current.copy(
                        steps = updatedSteps,
                        updatedAt = System.currentTimeMillis()
                    )
                    Log.d(TAG, "✅ Added new step from action config: ${step.id}")
                }
            }
            else -> {
                // Başka state'lerden geliyorsa hiçbir şey yapma
            }
        }

        Log.e(TAG, "   Returning with spell: ${current.name}, ${_currentSpell.value?.steps?.size} steps")
        _currentSpell.value?.steps?.forEachIndexed { index, step ->
            Log.e(TAG, "   Step ${index + 1}: Trigger=${step.trigger::class.simpleName}, Action=${step.action::class.simpleName}")
        }
        _uiState.value = SpellStudioUiState.EditingRecipe(_currentSpell.value!!)
        Log.e(TAG, "✅ RETURN_TO_RECIPE completed")
    }

    /**
     * ⚡⚡⚡ YENİ: Seal kalibrasyonlarını Trigger gestureData'ya sync et
     */
    private fun syncCalibrationsToTriggers() {
        val current = _currentSpell.value ?: return

        Log.e(TAG, "🔄 Syncing calibrations to triggers...")

        val updatedSteps = current.steps.map { step ->
            val seal = _calibrationSeals.value[step.id]

            if (seal != null && seal.templateLandmarksList.isNotEmpty()) {
                // Seal'dan JSON oluştur
                val gestureDataJson = com.example.isekaikuroshin.utils.GestureDataConverter.sealToJson(seal)

                if (gestureDataJson != null) {
                    Log.e(TAG, "   Step ${step.id}: Syncing ${seal.templateLandmarksList.size} angle(s)")

                    when (val trigger = step.trigger) {
                        is SpellTrigger.SingleHandGesture -> {
                            step.copy(
                                trigger = trigger.copy(
                                    gestureData = gestureDataJson,
                                    gestureName = "${trigger.gestureName} (${seal.templateLandmarksList.size} açı)"
                                )
                            )
                        }
                        else -> step
                    }
                } else {
                    Log.w(TAG, "   Step ${step.id}: Failed to convert Seal to JSON")
                    step
                }
            } else {
                step
            }
        }

        _currentSpell.value = current.copy(
            steps = updatedSteps,
            updatedAt = System.currentTimeMillis()
        )

        Log.e(TAG, "✅ Calibration sync completed")
    }

    // ========================================
    // ⚡⚡⚡ VFX WORKSHOP ENTEGRASYONU
    // ========================================

    /**
     * ⚡⚡⚡ YENİ: Spell Step için Seal oluştur veya yükle
     */
    fun loadOrCreateSealForStep(stepId: String) {
        viewModelScope.launch {
            try {
                val currentSpell = _currentSpell.value ?: run {
                    Log.e(TAG, "❌ Error: currentSpell is null when loading seal")
                    return@launch
                }
                val sealId = getSealIdForStep(currentSpell.id, stepId)

                Log.d(TAG, "🔄 Loading seal for step: $stepId, sealId: $sealId")

                // Repository'den Seal'ı yükle
                var seal = sealRepository.getSealById(sealId)

                if (seal == null) {
                    // Yoksa yeni oluştur
                    Log.d(TAG, "🔄 Creating new seal (not found in DB)")
                    seal = createDefaultSealForStep(currentSpell.id, stepId)
                    sealRepository.insertOrUpdate(seal)
                    gameStateManager.updateSeal(seal)
                } else {
                    Log.d(TAG, "✅ Success: Seal loaded from DB with ${seal.templateLandmarksList.size} angle(s)")
                    gameStateManager.updateSeal(seal)
                }

                // StateFlow'u güncelle
                val updatedMap = _calibrationSeals.value.toMutableMap()
                updatedMap[stepId] = seal
                _calibrationSeals.value = updatedMap

                Log.d(TAG, "✅ Success: Seal loaded for step: $stepId")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading seal: ${e.message}", e)
            }
        }
    }

    /**
     * ⚡⚡⚡ YENİ: Default Seal oluştur
     */
    private fun createDefaultSealForStep(spellId: String, stepId: String): Seal {
        val sealId = getSealIdForStep(spellId, stepId)
        return Seal(
            id = sealId,
            nameKey = "spell_step_$stepId",
            descriptionKey = "spell_step_calibrated_desc",
            loreTextKey = "spell_step_created_in_studio",
            difficulty = SealDifficulty.NOVICE,
            tier = "Spell",
            templateLandmarksList = mutableListOf(),
            toleranceThreshold = 0.30f,
            acceptanceThreshold = 0.65f
        )
    }

    /**
     * ⚡⚡⚡ YENİ: Kalibrasyon tamamla (yeni açı ekle)
     */
    fun completeCalibrationForStep(stepId: String, landmarks: List<NormalizedPoint>) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 Processing calibration for step: $stepId")

                val currentSpell = _currentSpell.value ?: run {
                    Log.e(TAG, "❌ Error: currentSpell is null")
                    return@launch
                }
                val sealId = getSealIdForStep(currentSpell.id, stepId)

                // Repository'den Seal'ı al
                val currentSeal = sealRepository.getSealById(sealId)
                    ?: createDefaultSealForStep(currentSpell.id, stepId)

                Log.d(TAG, "🔄 Current seal has ${currentSeal.templateLandmarksList.size} angle(s)")

                // Yeni açıyı ekle
                val updatedTemplates = currentSeal.templateLandmarksList.toMutableList().apply {
                    add(landmarks)
                }

                val updatedSeal = currentSeal.copy(templateLandmarksList = updatedTemplates)

                // Repository'ye kaydet
                Log.d(TAG, "🔄 Saving to repository: sealId=$sealId")
                sealRepository.insertOrUpdate(updatedSeal)
                gameStateManager.updateSeal(updatedSeal)

                // StateFlow'u güncelle
                val updatedMap = _calibrationSeals.value.toMutableMap()
                updatedMap[stepId] = updatedSeal.copy()
                _calibrationSeals.value = updatedMap

                // FAZ 7: Pratik tamamlandı, XP kazan!
                val didLevelUp = gameStateManager.incrementSpellPracticeCount(currentSpell.id)
                if (didLevelUp) {
                    _levelUpSpellId.value = currentSpell.id // Level up dialog göster
                }

                // G83: Log spell practice to Journal
                com.example.isekaikuroshin.utils.EventLogger.logGenericActivity(
                    "I practiced the spell '${currentSpell.name}' and added a new gesture calibration angle (Total angles: ${updatedTemplates.size})"
                )

                // GÖREV 3 & 4: Skill Tree XP + Element Affinity Entegrasyonu
                // TODO: SpellRecipe'ye element field eklendiğinde bunu kullan
                // Şimdilik sabit "FIRE" element için XP ver
                val elementType = "FIRE" // TODO: SpellRecipe'den al
                val xpGained = calculateXPForPractice(updatedTemplates.size)

                // XP Gain Event (UI için)
                _xpGainEvent.value = XPGainEvent(
                    xpAmount = xpGained,
                    elementType = elementType
                )

                // GÖREV 4: Element Affinity XP ekle
                val affinityLeveledUp = com.example.isekaikuroshin.data.PersistentDataManager.addElementAffinityXP(
                    elementType,
                    xpGained / 2  // Affinity XP, Skill Tree XP'nin yarısı
                )
                Log.d(TAG, "🎯 Skill Tree XP: +$xpGained, Affinity XP: +${xpGained/2} (Element: $elementType, Affinity LevelUp: $affinityLeveledUp)")

                // G88 FIX: Seal'ı JSON'a çevir ve SpellStep'in trigger.gestureData'sını güncelle
                // Bu sayede test modunda gesture tanıma çalışır!
                val gestureDataJson = com.example.isekaikuroshin.utils.GestureDataConverter.sealToJson(updatedSeal)
                if (gestureDataJson != null) {
                    // ⚡ KRİTİK: Önce UI State'teki step'i güncelle (CalibratingTrigger state'inde)
                    val currentState = _uiState.value
                    if (currentState is SpellStudioUiState.CalibratingTrigger && currentState.step.id == stepId) {
                        val updatedTrigger = when (currentState.step.trigger) {
                            is SpellTrigger.SingleHandGesture -> currentState.step.trigger.copy(
                                gestureName = updatedSeal.nameKey,
                                gestureData = gestureDataJson
                            )
                            is SpellTrigger.DoubleHandGesture -> {
                                // Double hand için: şimdilik sol el verisini kullan (TODO: sağ el ayrı kaydedilecek)
                                currentState.step.trigger.copy(
                                    leftGestureName = updatedSeal.nameKey,
                                    leftGestureData = gestureDataJson
                                )
                            }
                            else -> currentState.step.trigger
                        }
                        val updatedStep = currentState.step.copy(trigger = updatedTrigger)
                        _uiState.value = SpellStudioUiState.CalibratingTrigger(updatedStep)
                        Log.d(TAG, "✅ G88: Updated CalibratingTrigger state with calibration data")
                    }

                    // Eğer step zaten spell'de varsa (pratik modu), onu da güncelle
                    val updatedSteps = currentSpell.steps.map { step ->
                        if (step.id == stepId) {
                            val updatedTrigger = when (step.trigger) {
                                is SpellTrigger.SingleHandGesture -> step.trigger.copy(
                                    gestureName = updatedSeal.nameKey,
                                    gestureData = gestureDataJson
                                )
                                is SpellTrigger.DoubleHandGesture -> {
                                    step.trigger.copy(
                                        leftGestureName = updatedSeal.nameKey,
                                        leftGestureData = gestureDataJson
                                    )
                                }
                                else -> step.trigger
                            }
                            step.copy(trigger = updatedTrigger)
                        } else {
                            step
                        }
                    }

                    if (updatedSteps != currentSpell.steps) {
                        _currentSpell.value = currentSpell.copy(
                            steps = updatedSteps,
                            updatedAt = System.currentTimeMillis()
                        )
                        Log.d(TAG, "✅ G88: Updated currentSpell step trigger with calibration data")
                    }
                } else {
                    Log.e(TAG, "❌ G88: Failed to convert Seal to JSON")
                }

                Log.d(TAG, "✅ Success: Calibration saved. Total angles: ${updatedTemplates.size}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error saving calibration: ${e.message}", e)
            }
        }
    }

    /**
     * ⚡⚡⚡ YENİ: Kalibrasyon açısını sil
     */
    fun deleteCalibrationAngle(stepId: String, angleIndex: Int) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🗑️ [DELETE] Starting for step: $stepId, index: $angleIndex")

                val currentSpell = _currentSpell.value ?: return@launch
                val sealId = getSealIdForStep(currentSpell.id, stepId)

                val currentSeal = sealRepository.getSealById(sealId) ?: return@launch

                val updatedTemplates = currentSeal.templateLandmarksList.toMutableList().apply {
                    if (angleIndex in indices) {
                        removeAt(angleIndex)
                    }
                }

                val updatedSeal = currentSeal.copy(templateLandmarksList = updatedTemplates)

                sealRepository.insertOrUpdate(updatedSeal)
                gameStateManager.updateSeal(updatedSeal)

                val updatedMap = _calibrationSeals.value.toMutableMap()
                updatedMap[stepId] = updatedSeal.copy()
                _calibrationSeals.value = updatedMap

                Log.d(TAG, "✅ [DELETE] Angle deleted. Remaining: ${updatedTemplates.size}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ [DELETE] Error: ${e.message}", e)
            }
        }
    }

    /**
     * ⚡⚡⚡ YENİ: Tüm kalibrasyonları sil
     */
    fun clearAllCalibrationsForStep(stepId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🗑️ [CLEAR] Starting for step: $stepId")

                val currentSpell = _currentSpell.value ?: return@launch
                val sealId = getSealIdForStep(currentSpell.id, stepId)

                val currentSeal = sealRepository.getSealById(sealId) ?: return@launch

                val updatedSeal = currentSeal.copy(templateLandmarksList = mutableListOf())

                sealRepository.insertOrUpdate(updatedSeal)
                gameStateManager.updateSeal(updatedSeal)

                val updatedMap = _calibrationSeals.value.toMutableMap()
                updatedMap[stepId] = updatedSeal.copy()
                _calibrationSeals.value = updatedMap

                Log.d(TAG, "✅ [CLEAR] All angles cleared")
            } catch (e: Exception) {
                Log.e(TAG, "❌ [CLEAR] Error: ${e.message}", e)
            }
        }
    }

    /**
     * ⚡⚡⚡ YENİ: El tespiti işle ve canlı doğruluk hesapla
     */
    fun processHandDetectionForStep(
        stepId: String,
        result: HandLandmarkerResult?,
        recognitionEngine: GestureRecognitionEngine
    ) {
        viewModelScope.launch {
            if (result == null || result.landmarks().isEmpty()) {
                recognitionEngine.onTrackingLost()
                val updatedAccuracy = _currentAccuracy.value.toMutableMap()
                updatedAccuracy[stepId] = 0f
                _currentAccuracy.value = updatedAccuracy
                return@launch
            }

            // Landmark dönüşümü
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

            if (detectedLandmarks.size != 21) {
                Log.e(TAG, "❌ [PROCESS] Invalid landmark count: ${detectedLandmarks.size}")
                return@launch
            }

            // Seal'ı al
            val targetSeal = _calibrationSeals.value[stepId]

            if (targetSeal == null || targetSeal.templateLandmarksList.isEmpty()) {
                recognitionEngine.onTrackingLost()
                val updatedAccuracy = _currentAccuracy.value.toMutableMap()
                updatedAccuracy[stepId] = 0f
                _currentAccuracy.value = updatedAccuracy
                return@launch
            }

            // Gesture değerlendir
            val evaluationResult = try {
                recognitionEngine.evaluateGesture(
                    liveHandLandmarks = detectedLandmarks,
                    targetSeal = targetSeal
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ [PROCESS] Gesture evaluation failed: ${e.message}", e)
                return@launch
            }

            // Doğruluğu güncelle
            val updatedAccuracy = _currentAccuracy.value.toMutableMap()
            updatedAccuracy[stepId] = evaluationResult.accuracy
            _currentAccuracy.value = updatedAccuracy

            Log.d(TAG, "✅ [PROCESS] Accuracy for step $stepId: ${(evaluationResult.accuracy * 100).toInt()}%")
        }
    }

    /**
     * ⚡⚡⚡ YENİ: Normalizasyon ile açı ekle
     */
    fun addAngleWithNormalization(
        stepId: String,
        result: HandLandmarkerResult?,
        recognitionEngine: GestureRecognitionEngine
    ) {
        Log.d(TAG, "🔵 [ADD_ANGLE_NORM] Function called for step: $stepId")

        if (result == null || result.landmarks().isEmpty()) {
            Log.w(TAG, "⚠️ [ADD_ANGLE_NORM] No hand detected - ABORT")
            return
        }

        viewModelScope.launch {
            try {
                // RAW landmark'ları al
                val detectedLandmarks = result.landmarks()[0].map { landmark ->
                    NormalizedPoint(
                        x = landmark.x(),
                        y = landmark.y(),
                        z = landmark.z()
                    )
                }

                Log.d(TAG, "🔵 [ADD_ANGLE_NORM] Detected ${detectedLandmarks.size} landmarks")

                // Temporary Seal oluştur (normalizasyon için)
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

                // Engine'i çağır (normalize eder)
                val evaluationResult = recognitionEngine.evaluateGesture(
                    liveHandLandmarks = detectedLandmarks,
                    targetSeal = tempSeal
                )

                // Normalize edilmiş landmark'ları al
                val normalizedLandmarks = evaluationResult.debugInfo.liveNormalizedLandmarks

                Log.d(TAG, "🔵 [ADD_ANGLE_NORM] Normalized landmark count: ${normalizedLandmarks.size}")

                if (normalizedLandmarks.size != 21) {
                    Log.e(TAG, "❌ [ADD_ANGLE_NORM] Invalid normalized landmark count")
                    return@launch
                }

                // ViewModel'e kaydet
                completeCalibrationForStep(stepId, normalizedLandmarks)

                Log.d(TAG, "✅ [ADD_ANGLE_NORM] NORMALIZED angle added for step $stepId")
            } catch (e: Exception) {
                Log.e(TAG, "❌ [ADD_ANGLE_NORM] Error: ${e.message}", e)
            }
        }
    }

    /**
     * ⚡⚡⚡ YENİ: Seal kalibre edildi mi?
     */
    fun isSealCalibratedForStep(stepId: String): Boolean {
        val seal = _calibrationSeals.value[stepId]
        return seal != null && seal.templateLandmarksList.isNotEmpty()
    }

    /**
     * GÖREV 3: Pratik için kazanılan XP'yi hesapla
     * @param totalAngles Toplam kalibrasyon açısı sayısı
     * @return Kazanılan XP
     */
    private fun calculateXPForPractice(totalAngles: Int): Int {
        // İlk birkaç pratik daha fazla XP ver, sonra azal
        return when (totalAngles) {
            1 -> 50  // İlk pratik
            2 -> 40
            3 -> 30
            4 -> 25
            5 -> 20
            else -> 15  // 6+ pratikler
        }
    }

    /**
     * ⚡⚡⚡ YENİ: Açı sayısını al
     */
    fun getAngleCountForStep(stepId: String): Int {
        return _calibrationSeals.value[stepId]?.templateLandmarksList?.size ?: 0
    }

    /**
     * GÖREV N - FAZ 1: Büyü tarifini öğrenilmiş büyüye dönüştür
     *
     * Bu fonksiyon, Büyü Stüdyosu'nda oluşturulan bir SpellRecipe'i alır ve
     * kullanıcının kişisel LearnedSpell'ine dönüştürür.
     *
     * @param recipe Öğrenilecek büyü tarifi
     * @param customName Kullanıcının büyüye verdiği özel isim
     */
    fun learnSpell(recipe: SpellRecipe, customName: String) {
        viewModelScope.launch {
            // 1. Mevcut kalibrasyon verilerini topla
            val personalTriggers = recipe.steps.associate { step ->
                val seal = _calibrationSeals.value[step.id]
                step.id to com.example.isekaikuroshin.data.combat.TriggerCalibration(
                    stepId = step.id,
                    triggerType = when (step.trigger) {
                        is SpellTrigger.VoiceCommand -> com.example.isekaikuroshin.data.combat.TriggerType.VOICE
                        is SpellTrigger.SingleHandGesture -> com.example.isekaikuroshin.data.combat.TriggerType.SINGLE_HAND_GESTURE
                        is SpellTrigger.DoubleHandGesture -> com.example.isekaikuroshin.data.combat.TriggerType.DOUBLE_HAND_GESTURE
                        is SpellTrigger.Timer -> com.example.isekaikuroshin.data.combat.TriggerType.TIMER
                    },
                    calibrationData = "{}",  // Seal data serialization kaldırıldı
                    accuracy = 0f,  // Başlangıç accuracy
                    lastCalibrated = System.currentTimeMillis()
                )
            }

            // 2. LearnedSpell nesnesi oluştur
            val learnedSpell = com.example.isekaikuroshin.data.combat.LearnedSpell(
                recipeId = recipe.id,
                customName = customName,
                learnedAt = System.currentTimeMillis(),
                skillTreeLevel = 0,  // Başlangıç seviyesi
                practiceCount = 0,
                combatUsageCount = 0,
                personalTriggers = personalTriggers,
                element = determineElement(recipe)  // Büyünün element tipini belirle
            )

            // 3. GameStateManager'a kaydet
            gameStateManager.addLearnedSpell(learnedSpell)

            // G83: Log spell learning to Journal
            com.example.isekaikuroshin.utils.EventLogger.logGenericActivity(
                "I learned a new spell: '$customName' with ${recipe.steps.size} step(s)"
            )

            Log.d(TAG, "✅ Büyü öğrenildi: $customName (ID: ${learnedSpell.id})")
        }
    }

    /**
     * Büyünün element tipini belirle (heuristic)
     * Gelecekte daha sofistike bir analiz yapılabilir
     */
    private fun determineElement(recipe: SpellRecipe): com.example.isekaikuroshin.data.combat.ElementType {
        // Büyü adından veya açıklamasından element tahmin et
        val text = "${recipe.name} ${recipe.description}".lowercase()
        return when {
            text.contains("fire") || text.contains("ateş") || text.contains("alev") ->
                com.example.isekaikuroshin.data.combat.ElementType.FIRE
            text.contains("water") || text.contains("su") || text.contains("buz") ->
                com.example.isekaikuroshin.data.combat.ElementType.WATER
            text.contains("earth") || text.contains("toprak") || text.contains("kaya") ->
                com.example.isekaikuroshin.data.combat.ElementType.EARTH
            text.contains("air") || text.contains("hava") || text.contains("rüzgar") ->
                com.example.isekaikuroshin.data.combat.ElementType.AIR
            text.contains("light") || text.contains("ışık") || text.contains("aydınlık") ->
                com.example.isekaikuroshin.data.combat.ElementType.LIGHT
            text.contains("dark") || text.contains("karanlık") || text.contains("gölge") ->
                com.example.isekaikuroshin.data.combat.ElementType.DARK
            else -> com.example.isekaikuroshin.data.combat.ElementType.NEUTRAL
        }
    }
}

/**
 * GÖREV 3: XP Kazanım Eventi
 */
data class XPGainEvent(
    val xpAmount: Int,
    val elementType: String
)

/**
 * Büyü Stüdyosu UI Durumu
 */
sealed class SpellStudioUiState {
    /**
     * Büyü listesi gösteriliyor
     */
    data object SpellList : SpellStudioUiState()

    /**
     * Büyü tarifi düzenleniyor
     */
    data class EditingRecipe(val spell: SpellRecipe) : SpellStudioUiState()

    /**
     * G88: Tetikleyici kalibre ediliyor (22 Ekim backup'tan geri eklendi)
     */
    data class CalibratingTrigger(val step: SpellStep) : SpellStudioUiState()

    /**
     * Eylem ayarları yapılıyor
     */
    data class ConfiguringAction(val step: SpellStep) : SpellStudioUiState()

    /**
     * Büyü test ediliyor
     */
    data class TestingSpell(val spell: SpellRecipe) : SpellStudioUiState()
}
