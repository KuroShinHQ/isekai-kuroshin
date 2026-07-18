package com.example.isekaikuroshin.engine

import com.example.isekaikuroshin.data.*
import com.example.isekaikuroshin.utils.GameLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObserverEngine @Inject constructor() {

    // G108.3: Track how often this is called (DirectorEngine trigger frequency)
    private var summaryCallCount = 0

    fun createWorldStateSummary(gameState: GameStateZ7): String {
        summaryCallCount++
        val playerState = gameState.playerState
        val umbrosContract = gameState.umbrosContract

        // G108.3: Log to verify DirectorEngine trigger frequency
        GameLogger.logSystem("ObserverEngine: Creating world summary #$summaryCallCount (Day ${gameState.currentDay}, ThreatCounter: ${gameState.threatCounter})")

        return buildString {
            appendLine("### DÜNYA DURUM ÖZETİ ###")
            appendLine()

            // OYUNCU DURUMU
            appendLine("# OYUNCU DURUMU")
            appendLine("- İsim: ${playerState.playerName}")
            appendLine("- Seviye: ${playerState.level} (${playerState.experience}/${playerState.experienceToNextLevel} EXP)")
            appendLine("- Sınıf: ${playerState.playerClass}")
            appendLine("- Sağlık: ${playerState.currentHealth}/${playerState.maxHealth}")
            appendLine("- Mana: ${playerState.currentMana}/${playerState.maxMana}")
            appendLine("- Nitelikler: STR(${playerState.strength}), VIT(${playerState.vitality}), AGI(${playerState.agility}), INT(${playerState.intelligence})")
            appendLine("- Ahlak Puanı: ${playerState.moralityScore} (-1.0 ile 1.0 arası)")
            appendLine("- Harcanabilir Yetenek Puanı: ${playerState.statPoints}")
            appendLine()

            // GÖREV DURUMU
            appendLine("# GÖREV DURUMU")
            val activeQuestTitles = if (gameState.activeQuests.isNotEmpty()) {
                gameState.activeQuests.joinToString(", ") { it.title }
            } else {
                "Yok"
            }
            appendLine("- Aktif Görevler: $activeQuestTitles")

            val completedQuestTitles = if (gameState.completedQuests.isNotEmpty()) {
                gameState.completedQuests.takeLast(3).joinToString(", ") { it.title }
            } else {
                "Yok"
            }
            appendLine("- Tamamlanan Görevler (Son 3): $completedQuestTitles")
            appendLine()

            // DÜNYA DURUMU
            appendLine("# DÜNYA DURUMU")
            appendLine("- Gün: ${gameState.currentDay}, Zaman: ${gameState.currentTimeOfDay.displayName}, Mevsim: ${gameState.currentSeason.displayName}")
            appendLine("- Hava Durumu: ${gameState.currentWeather.displayName}")
            appendLine()

            // KONUM BİLGİSİ
            appendLine("# KONUM BİLGİSİ")
            val currentLocation = findLocationById(gameState.currentLocationId, gameState.knownLocations)
            appendLine("- Mevcut Konum: ${currentLocation?.name ?: "Bilinmeyen Konum"}")
            appendLine("- Konum Tipi: ${currentLocation?.type?.name ?: "Bilinmiyor"}")
            appendLine("- Tehlike Seviyesi: ${currentLocation?.dangerLevel ?: "Bilinmiyor"}")
            appendLine()

            // SOSYAL DURUM
            appendLine("# SOSYAL DURUM")
            val sortedRelationships = gameState.npcRelationships.values.sortedByDescending { it.relationshipPoints }

            val topRelationships = sortedRelationships.take(3)
            val topRelationshipsText = if (topRelationships.isNotEmpty()) {
                topRelationships.joinToString(", ") { "${it.npcId}(${it.relationshipPoints})" }
            } else {
                "Yok"
            }
            appendLine("- En Yüksek İlişkili NPC'ler: $topRelationshipsText")

            val bottomRelationships = sortedRelationships.takeLast(3).reversed()
            val bottomRelationshipsText = if (bottomRelationships.isNotEmpty()) {
                bottomRelationships.joinToString(", ") { "${it.npcId}(${it.relationshipPoints})" }
            } else {
                "Yok"
            }
            appendLine("- En Düşük İlişkili NPC'ler: $bottomRelationshipsText")
            appendLine()

            // ENVANTER ÖZETİ
            appendLine("# ENVANTER ÖZETİ")
            val currentWeight = calculateCurrentWeight(gameState)
            val maxCapacity = calculateMaxCarryCapacity(gameState.playerState)
            appendLine("- Taşıma Kapasitesi: ${String.format("%.1f", currentWeight)} / ${String.format("%.1f", maxCapacity)}")
            appendLine("- Altın: ${playerState.gold}")
            appendLine()

            // AKTİF ETKİLER
            appendLine("# AKTİF ETKİLER")
            val umbrosStatus = if (umbrosContract?.isActive == true) "Aktif" else "Aktif Değil"
            val remainingDays = umbrosContract?.remainingCurse ?: 0
            appendLine("- Umbros'un Laneti: $umbrosStatus (Kalan Süre: $remainingDays gün)")
        }
    }


    private fun findLocationById(locationId: String, knownLocations: List<Location>): Location? {
        return knownLocations.find { it.id == locationId }
    }

    private fun calculateCurrentWeight(gameState: GameStateZ7): Float {
        val inventoryWeight = gameState.inventory.sumOf { it.weight.toDouble() }.toFloat()
        val equippedWeight = gameState.equippedItems.values.sumOf { it.weight.toDouble() }.toFloat()
        return inventoryWeight + equippedWeight
    }

    private fun calculateMaxCarryCapacity(playerState: PlayerState): Float {
        return playerState.strength * 5.0f // CARRY_CAPACITY_MULTIPLIER = 5.0f
    }
}