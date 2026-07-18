
package com.example.isekaikuroshin.ui.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.isekaikuroshin.R
import com.example.isekaikuroshin.data.GameStateManager
import com.example.isekaikuroshin.data.LanguageManager
import com.example.isekaikuroshin.data.PersistentDataManager
import com.example.isekaikuroshin.data.StatusEffectManager
import com.example.isekaikuroshin.ui.components.CardData
import com.example.isekaikuroshin.ui.components.CardRarity
import com.example.isekaikuroshin.ui.components.StatType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CharacterStatusViewModel @Inject constructor(
    private val gameStateManager: GameStateManager,
    private val persistentDataManager: PersistentDataManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CharacterStatusUiState())
    val uiState: StateFlow<CharacterStatusUiState> = _uiState.asStateFlow()

    // Temporary stat allocation (önizleme için)
    private val _pendingAllocations = MutableStateFlow<Map<String, Int>>(emptyMap())

    init {
        observeGameData()
        observeLanguageChanges() // 🔥 Dil değişikliklerini dinle
    }

    private fun observeGameData() {
        viewModelScope.launch {
            gameStateManager.gameState.collect { gameState ->
                loadCharacterStatus()
            }
        }
    }

    /**
     * 🔥 Dil değişikliklerini dinle ve UI'yı yeniden yükle
     */
    private fun observeLanguageChanges() {
        viewModelScope.launch {
            LanguageManager.currentLanguage.collect { language ->
                // Dil değişince tüm UI'yı yeniden yükle
                loadCharacterStatus()
            }
        }
    }

    private fun loadCharacterStatus() {
        val gameState = gameStateManager.gameState.value
        val player = gameState.playerState

        // G65: Load persistent data for titles and badges
        val persistentData = persistentDataManager.gameData.value.playerData

        _uiState.value = CharacterStatusUiState(
            characterName = player.playerName.ifEmpty { LanguageManager.getText("hunter") },
            level = "${LanguageManager.getText("level")} ${player.level}",
            unspentStatPoints = player.statPoints,
            lifeStats = listOf(
                LanguageManager.getText("hp") to "${player.currentHealth}/${player.maxHealth}",
                LanguageManager.getText("mp") to "${player.currentMana}/${player.maxMana}",
                LanguageManager.getText("stamina") to "${player.stamina}/${player.maxStamina}",
                LanguageManager.getText("hunger") to "${player.hunger}/${player.maxHunger}",
                LanguageManager.getText("fatigue") to "${player.fatigue}/100"
            ),
            mainStats = listOf(
                LanguageManager.getText("strength") to player.strength.toInt().toString(),
                LanguageManager.getText("agility") to player.agility.toInt().toString(),
                LanguageManager.getText("intelligence") to player.intelligence.toInt().toString(),
                LanguageManager.getText("vitality") to player.vitality.toInt().toString(),
                LanguageManager.getText("spirit") to player.spirit.toString(),
                LanguageManager.getText("luck") to player.luck.toString()
            ),
            titleCard = if (player.equippedTitleId != null) {
                CardData.BadgeCard(
                    id = player.equippedTitleId,
                    name = LanguageManager.getText("active_title"),
                    description = LanguageManager.getText("story_granted_title"),
                    imageRes = null, // FB#6: Map icon kaldırıldı
                    rarity = CardRarity.COMMON,
                    unlockCondition = LanguageManager.getText("story_progress")
                )
            } else {
                CardData.BadgeCard(
                    id = "human_race",
                    name = LanguageManager.getText("human_race"),
                    description = LanguageManager.getText("human_race_desc"),
                    imageRes = null, // FB#6: Map icon kaldırıldı
                    rarity = CardRarity.COMMON,
                    unlockCondition = LanguageManager.getText("starting_badge")
                )
            },
            skillCard = if (gameState.activeSkills.isNotEmpty()) {
                val firstSkill = gameState.activeSkills.first()
                CardData.SkillCard(
                    id = firstSkill.id,
                    name = firstSkill.name,
                    description = firstSkill.description,
                    imageRes = null, // FB#6: Hancer icon kaldırıldı
                    rarity = CardRarity.COMMON,
                    rank = "${LanguageManager.getText("level")} ${firstSkill.level}",
                    statBonuses = emptyMap()
                )
            } else {
                CardData.SkillCard(
                    id = "adaptasyon",
                    name = LanguageManager.getText("adaptation"),
                    description = LanguageManager.getText("adaptation_desc"),
                    imageRes = null, // FB#6: Hancer icon kaldırıldı
                    rarity = CardRarity.COMMON,
                    rank = LanguageManager.getText("starting_skill"),
                    statBonuses = mapOf(
                        StatType.LUCK_PERCENT to 0.05f
                    )
                )
            },
            // G65 + G84: Map all titles to CardData (using LanguageManager, not hardcoded formatting)
            allTitles = persistentData.activeTitles.map { titleId: String ->
                CardData.BadgeCard(
                    id = titleId,
                    name = LanguageManager.getText(titleId), // KURAL 9: No hardcoded text
                    description = LanguageManager.getText("title_earned"),
                    imageRes = R.drawable.icon_race_human,
                    rarity = CardRarity.COMMON,
                    unlockCondition = LanguageManager.getText("achievement_unlocked")
                )
            },
            // G65 + G84 FIX: Map all badges to CardData (with fallback)
            allBadges = if (persistentData.achievements.isEmpty()) {
                // G84 FIX: Default starting badge (like skillCard fallback)
                listOf(
                    CardData.BadgeCard(
                        id = "human_race",
                        name = LanguageManager.getText("human_race"),
                        description = LanguageManager.getText("human_race_desc"),
                        imageRes = R.drawable.icon_race_human,
                        rarity = CardRarity.COMMON,
                        unlockCondition = LanguageManager.getText("starting_badge")
                    )
                )
            } else {
                persistentData.achievements.map { badgeId: String ->
                    CardData.BadgeCard(
                        id = badgeId,
                        name = LanguageManager.getText(badgeId), // KURAL 9: No hardcoded text
                        description = LanguageManager.getText("badge_earned"),
                        imageRes = R.drawable.icon_race_human,
                        rarity = CardRarity.COMMON,
                        unlockCondition = LanguageManager.getText("achievement_unlocked")
                    )
                }
            },
            // G113.5: Active status effects
            activeStatusEffects = gameState.activeStatusEffects.map { activeEffect ->
                StatusEffectDisplayData(
                    effectId = activeEffect.effect.effectId,
                    name = activeEffect.effect.name,
                    type = activeEffect.effect.type.name, // BUFF or DEBUFF
                    remainingTurns = activeEffect.effect.remainingTurns(gameState.currentDay, activeEffect.startTurn),
                    magnitude = activeEffect.effect.magnitude
                )
            },
            // TODO-G106.5: Archetype Profile from PlayerProfile
            // G116: Normalize archetype keys (eski Türkçe key'leri İngilizce'ye çevir)
            dominantArchetype = normalizeArchetypeKey(gameState.playerState.playerProfile.dominantArchetype),
            archetypeScores = gameState.playerState.playerProfile.archetypeScores.mapKeys { (key, _) ->
                normalizeArchetypeKey(key)
            }
        )
    }

    /**
     * G116: Archetype key normalization
     * Eski database'lerde Türkçe key'ler olabilir, bunları İngilizce'ye normalize et
     * KURAL 11: Metadata-based logic - key'ler language-agnostic olmalı
     */
    private fun normalizeArchetypeKey(key: String): String {
        return when (key) {
            // Türkçe → İngilizce mapping
            "Savasci", "Savaşçı", "SAVASCI" -> "Warrior"
            "Kasif", "Kaşif", "KASIF" -> "Explorer"
            "Zanaatkar", "Zanaatçı", "ZANAATKAR" -> "Craftsman"
            "Mistik", "MİSTİK" -> "Mystic"
            "Bilinmiyor", "BİLİNMİYOR" -> "Unknown"
            // Zaten İngilizce ise direkt döndür
            else -> key
        }
    }

    /**
     * Stat allocation UI'dan puan ekleme/çıkarma (önizleme)
     */
    fun modifyPendingAllocation(statName: String, delta: Int) {
        val current = _pendingAllocations.value
        val newValue = (current[statName] ?: 0) + delta
        val totalPending = current.values.sum() + delta

        // Negatif olmamalı ve unspent points'i geçmemeli
        if (newValue >= 0 && totalPending <= _uiState.value.unspentStatPoints) {
            _pendingAllocations.value = current.toMutableMap().apply {
                this[statName] = newValue
            }

            // UI'yı güncelle (geçici bonus göster)
            updateUIWithPendingAllocations()
        }
    }

    /**
     * Pending allocations'ları UI'da göster (+3 Güç formatında)
     */
    private fun updateUIWithPendingAllocations() {
        val player = gameStateManager.gameState.value.playerState
        val pending = _pendingAllocations.value

        _uiState.value = _uiState.value.copy(
            mainStats = listOf(
                LanguageManager.getText("strength") to buildStatText(player.strength.toInt(), pending["str"] ?: 0),
                LanguageManager.getText("agility") to buildStatText(player.agility.toInt(), pending["agi"] ?: 0),
                LanguageManager.getText("intelligence") to buildStatText(player.intelligence.toInt(), pending["int"] ?: 0),
                LanguageManager.getText("vitality") to buildStatText(player.vitality.toInt(), pending["vit"] ?: 0),
                LanguageManager.getText("spirit") to buildStatText(player.spirit, pending["spirit"] ?: 0),
                LanguageManager.getText("luck") to buildStatText(player.luck, pending["luck"] ?: 0)
            ),
            pendingPoints = pending.values.sum()
        )
    }

    private fun buildStatText(base: Int, bonus: Int): String {
        return if (bonus > 0) {
            "$base (+$bonus)"
        } else {
            base.toString()
        }
    }

    /**
     * Pending allocations'ları onayla ve kaydet
     */
    fun confirmAllocations() {
        val pending = _pendingAllocations.value
        if (pending.isEmpty()) return

        pending.forEach { (statName, amount) ->
            if (amount > 0) {
                gameStateManager.allocateStatPoint(statName, amount)
            }
        }

        // Reset pending
        _pendingAllocations.value = emptyMap()
        loadCharacterStatus()
    }

    /**
     * Pending allocations'ları iptal et
     */
    fun cancelAllocations() {
        _pendingAllocations.value = emptyMap()
        loadCharacterStatus()
    }
}
