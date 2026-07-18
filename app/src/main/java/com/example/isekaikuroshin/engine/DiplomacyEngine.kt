package com.example.isekaikuroshin.engine

import com.example.isekaikuroshin.data.world.FactionState
import com.example.isekaikuroshin.data.world.DiplomacyStatus
import com.example.isekaikuroshin.data.world.FactionStatus
import com.example.isekaikuroshin.utils.GameLogger
import kotlin.random.Random

/**
 * Diplomacy Engine - TODO-NEM-07: Dinamik Fraksiyon ve Diplomasi Motoru
 *
 * Bu engine, WorldUpdateEngine tarafından çağrılan diplomatik simülasyon işlemlerini gerçekleştirir.
 * Fraksiyonlar arası ilişkileri dinamik olarak değiştirir ve yaşayan dünya sisteminin
 * diplomatik boyutunu yönetir.
 *
 * Yaşayan Dünya Sistemi 2. Fazı'nın parçasıdır ve fraksiyonların zaman içinde
 * birbirleriyle olan ilişkilerini simüle eder.
 */
object DiplomacyEngine {

    private const val TAG = "DiplomacyEngine"

    // Diplomatik değişim olasılıkları
    private const val DIPLOMATIC_CHANGE_CHANCE = 0.02f // %2 şans her fraksiyon çifti için
    private const val POWER_INFLUENCE_FACTOR = 0.1f // Güç dengesizliğinin diplomatik değişime etkisi
    private const val WAR_TO_PEACE_CHANCE = 0.05f // Savaştan barışa geçiş şansı
    private const val ALLIANCE_BREAK_CHANCE = 0.01f // İttifak bozulma şansı

    /**
     * Ana diplomatik simülasyon fonksiyonu
     * Her World Tick'te çağrılır ve fraksiyonların diplomatik durumlarını günceller
     *
     * @param factions Mevcut fraksiyon durumları listesi
     * @return Güncellenmiş fraksiyon durumları listesi
     */
    fun simulateDiplomaticEvents(factions: List<FactionState>): List<FactionState> {
        if (factions.size < 2) {
            GameLogger.logSystem("$TAG: En az 2 fraksiyon gerekli, diplomatik simülasyon atlandı")
            return factions
        }

        GameLogger.logSystem("$TAG: === DİPLOMATİK SİMÜLASYON BAŞLADI ===")
        GameLogger.logSystem("$TAG: ${factions.size} fraksiyon arasında diplomatik olaylar simüle ediliyor")

        val updatedFactions = factions.toMutableList()
        var diplomaticChanges = 0

        // Her fraksiyon çifti için diplomatik değişim kontrolü
        for (i in 0 until updatedFactions.size) {
            for (j in (i + 1) until updatedFactions.size) {
                val faction1 = updatedFactions[i]
                val faction2 = updatedFactions[j]

                // Diplomatik değişim şansı kontrolü
                if (Random.nextFloat() < calculateDiplomaticChangeChance(faction1, faction2)) {
                    val (updatedFaction1, updatedFaction2) = processDiplomaticChange(faction1, faction2)

                    updatedFactions[i] = updatedFaction1
                    updatedFactions[j] = updatedFaction2
                    diplomaticChanges++

                    GameLogger.logSystem("$TAG: 🏛️ Diplomatik değişim: ${faction1.name} ↔ ${faction2.name}")
                    val newRelation = updatedFaction1.getDiplomaticRelationWith(faction2.id)
                    GameLogger.logSystem("$TAG: Yeni ilişki: ${diplomaticStatusToTurkish(newRelation)}")
                }
            }
        }

        // Güç seviyesi değişimleri
        val factionsWithPowerChanges = updatedFactions.map { faction ->
            simulatePowerChange(faction)
        }

        GameLogger.logSystem("$TAG: === DİPLOMATİK SİMÜLASYON TAMAMLANDI ===")
        GameLogger.logSystem("$TAG: Toplam $diplomaticChanges diplomatik değişim gerçekleşti")

        return factionsWithPowerChanges
    }

    /**
     * İki fraksiyon arasındaki diplomatik değişim şansını hesaplar
     * Güç dengesi, mevcut ilişki gibi faktörleri dikkate alır
     */
    private fun calculateDiplomaticChangeChance(faction1: FactionState, faction2: FactionState): Float {
        var chance = DIPLOMATIC_CHANGE_CHANCE

        // Güç dengesizliği faktörü - güçlü fraksiyonlar daha aktif
        val powerDifference = kotlin.math.abs(faction1.getTotalPower() - faction2.getTotalPower())
        chance += powerDifference * POWER_INFLUENCE_FACTOR / 100f

        // Mevcut ilişkinin stabilite faktörü
        val currentRelation = faction1.getDiplomaticRelationWith(faction2.id)
        chance *= when (currentRelation) {
            DiplomacyStatus.WAR -> 1.5f // Savaş durumu daha hızlı değişebilir
            DiplomacyStatus.HOSTILE -> 1.2f // Düşmanlık durumu da dinamik
            DiplomacyStatus.NEUTRAL -> 1.0f // Normal değişim hızı
            DiplomacyStatus.FRIENDLY -> 0.8f // Dostluk daha stabil
            DiplomacyStatus.ALLIANCE -> 0.5f // İttifak en stabil
        }

        return chance.coerceIn(0f, 0.1f) // Maksimum %10 şans
    }

    /**
     * İki fraksiyon arasındaki diplomatik değişimi işler
     */
    private fun processDiplomaticChange(
        faction1: FactionState,
        faction2: FactionState
    ): Pair<FactionState, FactionState> {

        val currentRelation = faction1.getDiplomaticRelationWith(faction2.id)
        val newRelation = generateNewDiplomaticRelation(currentRelation, faction1, faction2)

        if (newRelation == currentRelation) {
            return Pair(faction1, faction2) // Değişiklik yok
        }

        // Her iki fraksiyonun diplomatik ilişkiler haritasını güncelle
        val updatedFaction1 = updateFactionDiplomacy(faction1, faction2.id, newRelation)
        val updatedFaction2 = updateFactionDiplomacy(faction2, faction1.id, newRelation)

        // Savaş durumuna geçişte özel status ekle
        val finalFaction1 = if (newRelation == DiplomacyStatus.WAR) {
            addWarStatus(updatedFaction1)
        } else {
            removeWarStatus(updatedFaction1, faction2.id)
        }

        val finalFaction2 = if (newRelation == DiplomacyStatus.WAR) {
            addWarStatus(updatedFaction2)
        } else {
            removeWarStatus(updatedFaction2, faction1.id)
        }

        return Pair(finalFaction1, finalFaction2)
    }

    /**
     * Mevcut diplomatik duruma göre yeni bir diplomatik durum üretir
     */
    private fun generateNewDiplomaticRelation(
        current: DiplomacyStatus,
        faction1: FactionState,
        faction2: FactionState
    ): DiplomacyStatus {

        // Güç dengesi faktörü
        val powerRatio = faction1.getTotalPower().toFloat() / faction2.getTotalPower().toFloat()
        val powerImbalance = kotlin.math.abs(powerRatio - 1.0f)

        return when (current) {
            DiplomacyStatus.WAR -> {
                // Savaştan çıkış şansı düşük
                if (Random.nextFloat() < WAR_TO_PEACE_CHANCE) {
                    if (Random.nextBoolean()) DiplomacyStatus.HOSTILE else DiplomacyStatus.NEUTRAL
                } else {
                    DiplomacyStatus.WAR
                }
            }

            DiplomacyStatus.HOSTILE -> {
                when {
                    Random.nextFloat() < 0.3f -> DiplomacyStatus.WAR // Savaşa escalation
                    Random.nextFloat() < 0.5f -> DiplomacyStatus.NEUTRAL // Barışa doğru
                    else -> DiplomacyStatus.HOSTILE // Değişim yok
                }
            }

            DiplomacyStatus.NEUTRAL -> {
                // En çok değişken durum
                when {
                    powerImbalance > 0.5f && Random.nextFloat() < 0.4f -> DiplomacyStatus.HOSTILE // Güç dengesizliği gerginlik yaratır
                    Random.nextFloat() < 0.3f -> DiplomacyStatus.FRIENDLY // Dostluğa doğru
                    Random.nextFloat() < 0.2f -> DiplomacyStatus.HOSTILE // Düşmanlığa doğru
                    else -> DiplomacyStatus.NEUTRAL
                }
            }

            DiplomacyStatus.FRIENDLY -> {
                when {
                    Random.nextFloat() < 0.2f -> DiplomacyStatus.ALLIANCE // İttifaka yükselme
                    Random.nextFloat() < 0.15f -> DiplomacyStatus.NEUTRAL // Soğuma
                    else -> DiplomacyStatus.FRIENDLY
                }
            }

            DiplomacyStatus.ALLIANCE -> {
                // İttifak en stabil durum
                if (Random.nextFloat() < ALLIANCE_BREAK_CHANCE) {
                    DiplomacyStatus.FRIENDLY // İttifak bozulur ama dostluk kalır
                } else {
                    DiplomacyStatus.ALLIANCE
                }
            }
        }
    }

    /**
     * Fraksiyonun diplomatik ilişkiler haritasını günceller
     */
    private fun updateFactionDiplomacy(
        faction: FactionState,
        targetFactionId: String,
        newRelation: DiplomacyStatus
    ): FactionState {
        val updatedRelations = faction.diplomaticRelations.toMutableMap()
        updatedRelations[targetFactionId] = newRelation

        // Aynı zamanda allies ve enemies listelerini de güncelle
        val updatedAllies = faction.allies.toMutableList()
        val updatedEnemies = faction.enemies.toMutableList()

        // Eski durumu temizle
        updatedAllies.remove(targetFactionId)
        updatedEnemies.remove(targetFactionId)

        // Yeni duruma göre ekle
        when (newRelation) {
            DiplomacyStatus.ALLIANCE -> {
                if (!updatedAllies.contains(targetFactionId)) {
                    updatedAllies.add(targetFactionId)
                }
            }
            DiplomacyStatus.WAR, DiplomacyStatus.HOSTILE -> {
                if (!updatedEnemies.contains(targetFactionId)) {
                    updatedEnemies.add(targetFactionId)
                }
            }
            else -> {
                // NEUTRAL veya FRIENDLY durumunda listelerden çıkar
            }
        }

        return faction.copy(
            diplomaticRelations = updatedRelations,
            allies = updatedAllies,
            enemies = updatedEnemies
        )
    }

    /**
     * Fraksiyona savaş durumu ekler
     */
    private fun addWarStatus(faction: FactionState): FactionState {
        val updatedStatuses = faction.status.toMutableList()
        if (!updatedStatuses.contains(FactionStatus.AT_WAR)) {
            updatedStatuses.add(FactionStatus.AT_WAR)
        }
        return faction.copy(status = updatedStatuses)
    }

    /**
     * Fraksiyondan savaş durumunu kaldırır (eğer başka savaş yoksa)
     */
    private fun removeWarStatus(faction: FactionState, enemyFactionId: String): FactionState {
        // Başka savaş durumu var mı kontrol et
        val hasOtherWars = faction.diplomaticRelations.any { (factionId, status) ->
            factionId != enemyFactionId && status == DiplomacyStatus.WAR
        }

        if (!hasOtherWars) {
            val updatedStatuses = faction.status.toMutableList()
            updatedStatuses.remove(FactionStatus.AT_WAR)
            return faction.copy(status = updatedStatuses)
        }

        return faction
    }

    /**
     * Fraksiyonun güç seviyesinde rastgele küçük değişiklikler yapar
     */
    private fun simulatePowerChange(faction: FactionState): FactionState {
        // %10 şansla güç seviyesi değişir
        if (Random.nextFloat() < 0.1f) {
            val powerChange = Random.nextInt(-5, 6) // -5 ile +5 arası
            val newPowerLevel = (faction.powerLevel + powerChange).coerceIn(10, 100)

            if (newPowerLevel != faction.powerLevel) {
                GameLogger.logSystem("$TAG: 💪 ${faction.name} güç seviyesi değişti: ${faction.powerLevel} → $newPowerLevel")
                return faction.copy(powerLevel = newPowerLevel)
            }
        }

        return faction
    }

    /**
     * DiplomacyStatus'u Türkçe metne çevirir
     */
    private fun diplomaticStatusToTurkish(status: DiplomacyStatus): String {
        return when (status) {
            DiplomacyStatus.WAR -> "SAVAŞ HALİNDE"
            DiplomacyStatus.HOSTILE -> "DÜŞMANCA"
            DiplomacyStatus.NEUTRAL -> "TARAFSIZ"
            DiplomacyStatus.FRIENDLY -> "DOSTANE"
            DiplomacyStatus.ALLIANCE -> "MÜTTEFİK"
        }
    }

    /**
     * Diplomatik motor durumu raporu
     */
    fun getDiplomacyReport(factions: List<FactionState>): String {
        if (factions.isEmpty()) {
            return "🏛️ DİPLOMASİ RAPORU: Fraksiyon bulunamadı"
        }

        val totalRelations = factions.sumOf { it.diplomaticRelations.size }
        val warCount = factions.sumOf { faction ->
            faction.diplomaticRelations.count { it.value == DiplomacyStatus.WAR }
        }
        val allianceCount = factions.sumOf { faction ->
            faction.diplomaticRelations.count { it.value == DiplomacyStatus.ALLIANCE }
        }

        return """
            🏛️ DİPLOMASİ MOTORU RAPORU 🏛️

            📊 Genel İstatistikler:
            • Toplam Fraksiyon: ${factions.size}
            • Toplam Diplomatik İlişki: $totalRelations
            • Aktif Savaş: $warCount
            • Aktif İttifak: $allianceCount

            🏴 Savaş Halindeki Fraksiyonlar:
            ${factions.filter { it.status.contains(FactionStatus.AT_WAR) }
                .joinToString("\n") { "• ${it.name} (Güç: ${it.getTotalPower()})" }
                .ifEmpty { "• Hiçbiri" }}

            🤝 En Güçlü İttifaklar:
            ${findStrongestAlliances(factions)}

            ⚙️ Motor Durumu: AKTİF
        """.trimIndent()
    }

    /**
     * En güçlü ittifakları bulur
     */
    private fun findStrongestAlliances(factions: List<FactionState>): String {
        val alliances = mutableListOf<Pair<String, Int>>()

        factions.forEach { faction ->
            faction.allies.forEach { allyId ->
                val ally = factions.find { it.id == allyId }
                if (ally != null) {
                    val totalPower = faction.getTotalPower() + ally.getTotalPower()
                    alliances.add("${faction.name} + ${ally.name}" to totalPower)
                }
            }
        }

        return alliances
            .sortedByDescending { it.second }
            .take(3)
            .joinToString("\n") { "• ${it.first} (Toplam Güç: ${it.second})" }
            .ifEmpty { "• Aktif ittifak bulunamadı" }
    }

    // ============ MACRO EVENT SUPPORT (TODO-NEM-09) ============

    /**
     * İki fraksiyon arasında savaş başlatır (MacroEvent için)
     */
    fun declareFactionWar(factionIdA: String, factionIdB: String) {
        GameLogger.logSystem("$TAG: === MAKRO OLAY: SAVAŞ İLANI ===")
        GameLogger.logSystem("$TAG: $factionIdA ve $factionIdB arasında savaş başlatılıyor...")

        // Bu fonksiyon şimdilik sadece loglama yapıyor
        // Gerçek implementasyon veritabanı güncellemesi gerektirir
        GameLogger.logSystem("$TAG: SAVAŞ İLAN EDİLDİ: $factionIdA vs $factionIdB")
    }

    /**
     * İki fraksiyon arasında barış yapar (MacroEvent için)
     */
    fun makeFactionPeace(factionIdA: String, factionIdB: String) {
        GameLogger.logSystem("$TAG: === MAKRO OLAY: BARIŞ ANLAŞMASI ===")
        GameLogger.logSystem("$TAG: $factionIdA ve $factionIdB arasında barış yapılıyor...")

        // Bu fonksiyon şimdilik sadece loglama yapıyor
        // Gerçek implementasyon veritabanı güncellemesi gerektirir
        GameLogger.logSystem("$TAG: BARIŞ YAPILDI: $factionIdA <-> $factionIdB")
    }
}