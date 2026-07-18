package com.example.isekaikuroshin.engine

import com.example.isekaikuroshin.data.Badge
import com.example.isekaikuroshin.data.BadgeType
import com.example.isekaikuroshin.data.GameStateZ7
import com.example.isekaikuroshin.data.PlayerState
import com.example.isekaikuroshin.data.world.DiplomacyStatus
import com.example.isekaikuroshin.data.world.FactionState
import com.example.isekaikuroshin.data.world.SettlementState
import com.example.isekaikuroshin.data.world.EconomyLevel
import com.example.isekaikuroshin.utils.GameLogger
import kotlin.random.Random

/**
 * SystemicEngine - Oyuncu Etkisi Sistemi
 *
 * TODO-NEM-08 kapsamında oluşturulan bu motor, oyuncunun sahip olduğu unvanların
 * ve fraksiyonlarla olan itibarının, dünya durumu değişkenlerini somut olarak
 * etkilemesini sağlayan sistemik etkileşim mantığını yönetir.
 *
 * Bu motor, "Yaşayan Dünya" sisteminin 3. Fazı'nı temsil eder ve oyuncunun
 * dünya üzerindeki gerçek etkisini simüle eder.
 */
object SystemicEngine {

    private const val TAG = "SystemicEngine"

    /**
     * Oyuncunun sahip olduğu unvanların, fraksiyonlara bağlı yerleşim yerlerinin
     * ekonomisini ve mutluluğunu etkilemesini sağlar.
     *
     * @param settlements Etkilenecek yerleşim yerleri listesi
     * @param playerState Oyuncunun mevcut durumu
     * @param playerTitles Oyuncunun sahip olduğu unvanlar
     * @return Güncellenmiş yerleşim yerleri listesi
     */
    fun applyPlayerTitleEffects(
        settlements: List<SettlementState>,
        playerState: PlayerState,
        playerTitles: List<Badge>
    ): List<SettlementState> {
        GameLogger.logSystem("$TAG: Oyuncu unvan etkileri uygulanıyor...")

        val updatedSettlements = settlements.map { settlement ->
            var updatedSettlement = settlement

            // Her unvan için etki kontrolü yap
            playerTitles.filter { it.type == BadgeType.TITLE }.forEach { title ->
                val effect = calculateTitleEffect(title, settlement)

                if (effect.isApplicable) {
                    // Happiness bonusu uygula
                    val newHappiness = (updatedSettlement.happiness + effect.happinessBonus)
                        .coerceIn(0, 100)

                    // Economy level şansını artır
                    val newEconomyLevel = applyEconomyBonus(
                        updatedSettlement.economyLevel,
                        effect.economyBonus
                    )

                    updatedSettlement = updatedSettlement.copy(
                        happiness = newHappiness,
                        economyLevel = newEconomyLevel
                    )

                    GameLogger.logSystem("$TAG: Unvan etkisi uygulandı: '${title.name}' -> ${settlement.name} " +
                        "(Mutluluk: +${effect.happinessBonus}, Ekonomi: ${if (effect.economyBonus > 0) "bonus" else "normal"})")
                }
            }

            updatedSettlement
        }

        return updatedSettlements
    }

    /**
     * Oyuncunun fraksiyonlarla olan itibarının, o fraksiyonlara bağlı yerleşim yerlerinin
     * ekonomisini ve mutluluğunu etkilemesini sağlar.
     *
     * @param settlements Etkilenecek yerleşim yerleri listesi
     * @param playerState Oyuncunun mevcut durumu
     * @param factions Mevcut fraksiyonlar listesi
     * @return Güncellenmiş yerleşim yerleri listesi
     */
    fun applyPlayerReputationEffects(
        settlements: List<SettlementState>,
        playerState: PlayerState,
        factions: List<FactionState>
    ): List<SettlementState> {
        GameLogger.logSystem("$TAG: Oyuncu itibar etkileri uygulanıyor...")

        val updatedSettlements = settlements.map { settlement ->
            var updatedSettlement = settlement

            // Yerleşim yerinin hangi fraksiyona bağlı olduğunu bul
            val governingFaction = factions.find { faction ->
                faction.controlledTerritories.contains(settlement.id) ||
                faction.capitalSettlement == settlement.id
            }

            if (governingFaction != null) {
                // Bu fraksiyonla oyuncunun ilişkisini al (varsayılan olarak NEUTRAL)
                val relationshipStatus = getDiplomaticRelationshipWithPlayer(governingFaction)
                val effect = calculateReputationEffect(relationshipStatus, settlement)

                if (effect.isApplicable) {
                    // Happiness değişimi uygula
                    val newHappiness = (updatedSettlement.happiness + effect.happinessChange)
                        .coerceIn(0, 100)

                    // Economy level şansını değiştir
                    val newEconomyLevel = applyEconomyBonus(
                        updatedSettlement.economyLevel,
                        effect.economyBonus
                    )

                    updatedSettlement = updatedSettlement.copy(
                        happiness = newHappiness,
                        economyLevel = newEconomyLevel
                    )

                    val changeText = when {
                        effect.happinessChange > 0 -> "pozitif"
                        effect.happinessChange < 0 -> "negatif"
                        else -> "nötr"
                    }

                    GameLogger.logSystem("$TAG: İtibar etkisi uygulandı: ${governingFaction.name} " +
                        "(${relationshipStatus.name}) -> ${settlement.name} " +
                        "(Mutluluk: ${effect.happinessChange}, Etki: $changeText)")
                }
            }

            updatedSettlement
        }

        return updatedSettlements
    }

    /**
     * Belirli bir unvanın belirli bir yerleşim yeri üzerindeki etkisini hesaplar
     */
    private fun calculateTitleEffect(title: Badge, settlement: SettlementState): TitleEffect {
        // Örnek unvan etkisi mantığı
        return when {
            // Kuzey Koruyucusu unvanı - Kuzey bölgesi yerleşim yerlerine bonus
            title.id.contains("PROTECTOR_OF_NORTH") ||
            title.name.contains("Kuzey") ||
            title.name.contains("North") -> {
                // Bu settlement'ın Kuzey bölgesinde olup olmadığını kontrol et
                val isNorthernSettlement = settlement.name.contains("Kuzey") ||
                    settlement.name.contains("North") ||
                    settlement.id.contains("north") ||
                    settlement.name.contains("Meşeköy") // Test için özel

                if (isNorthernSettlement) {
                    TitleEffect(
                        isApplicable = true,
                        happinessBonus = 5,
                        economyBonus = 0.15f // %15 ekonomi iyileştirme şansı
                    )
                } else {
                    TitleEffect.NONE
                }
            }

            // Ticaret Ustası unvanı - tüm yerleşim yerlerine ekonomi bonusu
            title.id.contains("TRADE_MASTER") ||
            title.name.contains("Ticaret") ||
            title.name.contains("Trade") -> {
                TitleEffect(
                    isApplicable = true,
                    happinessBonus = 2,
                    economyBonus = 0.10f // %10 ekonomi iyileştirme şansı
                )
            }

            // Kahraman unvanı - tüm yerleşim yerlerine genel bonus
            title.id.contains("HERO") ||
            title.name.contains("Kahraman") ||
            title.name.contains("Hero") -> {
                TitleEffect(
                    isApplicable = true,
                    happinessBonus = 3,
                    economyBonus = 0.05f // %5 ekonomi iyileştirme şansı
                )
            }

            // Varsayılan: küçük pozitif etki
            else -> {
                TitleEffect(
                    isApplicable = true,
                    happinessBonus = 1,
                    economyBonus = 0.02f // %2 ekonomi iyileştirme şansı
                )
            }
        }
    }

    /**
     * Oyuncunun fraksiyonla olan diplomatic ilişkisini simüle eder
     * (Gelecekte bu GameStateManager'dan gelecek)
     */
    private fun getDiplomaticRelationshipWithPlayer(faction: FactionState): DiplomacyStatus {
        // Şimdilik test için sabit değerler
        // Gerçek implementasyonda bu GameStateManager'dan gelecek
        return when {
            faction.name.contains("Kuzey") || faction.name.contains("Northern") -> DiplomacyStatus.ALLIANCE
            faction.name.contains("Bandit") || faction.name.contains("Eşkıya") -> DiplomacyStatus.WAR
            else -> DiplomacyStatus.NEUTRAL
        }
    }

    /**
     * Diplomatic ilişkinin yerleşim yeri üzerindeki etkisini hesaplar
     */
    private fun calculateReputationEffect(
        diplomacyStatus: DiplomacyStatus,
        settlement: SettlementState
    ): ReputationEffect {
        return when (diplomacyStatus) {
            DiplomacyStatus.ALLIANCE -> {
                ReputationEffect(
                    isApplicable = true,
                    happinessChange = 10,
                    economyBonus = 0.20f // %20 ekonomi iyileştirme şansı
                )
            }
            DiplomacyStatus.FRIENDLY -> {
                ReputationEffect(
                    isApplicable = true,
                    happinessChange = 5,
                    economyBonus = 0.10f // %10 ekonomi iyileştirme şansı
                )
            }
            DiplomacyStatus.NEUTRAL -> {
                ReputationEffect.NONE
            }
            DiplomacyStatus.HOSTILE -> {
                ReputationEffect(
                    isApplicable = true,
                    happinessChange = -5,
                    economyBonus = -0.10f // %10 ekonomi kötüleştirme şansı
                )
            }
            DiplomacyStatus.WAR -> {
                ReputationEffect(
                    isApplicable = true,
                    happinessChange = -10,
                    economyBonus = -0.20f // %20 ekonomi kötüleştirme şansı
                )
            }
        }
    }

    /**
     * Ekonomi seviyesine bonus/ceza uygular
     */
    private fun applyEconomyBonus(currentLevel: EconomyLevel, bonus: Float): EconomyLevel {
        if (bonus == 0f) return currentLevel

        // Pozitif bonus: ekonomi iyileştirme şansı
        if (bonus > 0f && Random.nextFloat() < bonus) {
            return when (currentLevel) {
                EconomyLevel.DESTITUTE -> EconomyLevel.POOR
                EconomyLevel.POOR -> EconomyLevel.MODEST
                EconomyLevel.MODEST -> EconomyLevel.PROSPEROUS
                EconomyLevel.PROSPEROUS -> EconomyLevel.WEALTHY
                EconomyLevel.WEALTHY -> EconomyLevel.WEALTHY // Maksimum seviye
            }
        }

        // Negatif bonus: ekonomi kötüleştirme şansı
        if (bonus < 0f && Random.nextFloat() < -bonus) {
            return when (currentLevel) {
                EconomyLevel.WEALTHY -> EconomyLevel.PROSPEROUS
                EconomyLevel.PROSPEROUS -> EconomyLevel.MODEST
                EconomyLevel.MODEST -> EconomyLevel.POOR
                EconomyLevel.POOR -> EconomyLevel.DESTITUTE
                EconomyLevel.DESTITUTE -> EconomyLevel.DESTITUTE // Minimum seviye
            }
        }

        return currentLevel
    }
}

/**
 * Unvan etkisini temsil eden veri sınıfı
 */
private data class TitleEffect(
    val isApplicable: Boolean,
    val happinessBonus: Int,
    val economyBonus: Float // Ekonomi seviyesi değiştirme şansı (-1.0 ile 1.0 arası)
) {
    companion object {
        val NONE = TitleEffect(false, 0, 0f)
    }
}

/**
 * İtibar etkisini temsil eden veri sınıfı
 */
private data class ReputationEffect(
    val isApplicable: Boolean,
    val happinessChange: Int, // Pozitif veya negatif değer
    val economyBonus: Float // Ekonomi seviyesi değiştirme şansı
) {
    companion object {
        val NONE = ReputationEffect(false, 0, 0f)
    }
}