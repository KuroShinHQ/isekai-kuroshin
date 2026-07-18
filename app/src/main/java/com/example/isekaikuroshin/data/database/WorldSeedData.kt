package com.example.isekaikuroshin.data.database

import com.example.isekaikuroshin.data.LocationType
import com.example.isekaikuroshin.data.world.*

/**
 * Yaşayan Dünya sistemi için örnek verileri oluşturan yardımcı sınıf.
 * TODO-NEM-06 kapsamında oluşturulmuş, fraksiyon ve settlement verilerini başlatır.
 */
object WorldSeedData {

    /**
     * Örnek fraksiyon verilerini oluşturur
     */
    fun createSampleFactions(): List<FactionState> {
        return listOf(
            // Kuzey Krallığı - Meşeköy'ün yöneticisi
            FactionState(
                id = "faction_northern_kingdom",
                name = "Kuzey Krallığı",
                type = FactionType.KINGDOM,
                powerLevel = 75,
                influence = 80,
                resources = mapOf(
                    "GOLD" to 50000,
                    "SOLDIERS" to 1500,
                    "FOOD" to 8000,
                    "POPULATION" to 25000
                ),
                controlledTerritories = listOf("settlement_mesekoy", "settlement_kuzey_kasabasi"),
                allies = listOf("faction_merchants_guild"),
                enemies = listOf("faction_shadow_bandits"),
                diplomaticRelations = mapOf(
                    "faction_merchants_guild" to DiplomacyStatus.ALLIANCE,
                    "faction_shadow_bandits" to DiplomacyStatus.WAR,
                    "faction_forest_druids" to DiplomacyStatus.FRIENDLY
                ),
                status = listOf(FactionStatus.EXPANDING),
                description = "Kuzeyden gelen güçlü bir krallık. Adalet ve düzeni sağlamaya odaklanmış, güçlü ordusu ile tanınır.",
                leaderName = "Kral Thormund Kuzeyci",
                capitalSettlement = "settlement_kuzey_kasabasi"
            ),

            // Tüccarlar Loncası
            FactionState(
                id = "faction_merchants_guild",
                name = "Tüccarlar Loncası",
                type = FactionType.GUILD,
                powerLevel = 60,
                influence = 90,
                resources = mapOf(
                    "GOLD" to 80000,
                    "SOLDIERS" to 500,
                    "TRADE_GOODS" to 15000,
                    "POPULATION" to 5000
                ),
                controlledTerritories = listOf("settlement_ticaret_limani"),
                allies = listOf("faction_northern_kingdom"),
                enemies = emptyList(),
                diplomaticRelations = mapOf(
                    "faction_northern_kingdom" to DiplomacyStatus.ALLIANCE,
                    "faction_shadow_bandits" to DiplomacyStatus.HOSTILE,
                    "faction_forest_druids" to DiplomacyStatus.NEUTRAL
                ),
                status = listOf(FactionStatus.TRADE_EMBARGO, FactionStatus.GOLDEN_AGE),
                description = "Zengin ve etkili tüccarların oluşturduğu lonca. Ticaret yollarını kontrol eder ve ekonomik güce sahiptir.",
                leaderName = "Lonca Başkanı Goldwin Parasaç",
                capitalSettlement = "settlement_ticaret_limani"
            ),

            // Orman Druidleri
            FactionState(
                id = "faction_forest_druids",
                name = "Orman Druidleri",
                type = FactionType.RELIGIOUS,
                powerLevel = 45,
                influence = 30,
                resources = mapOf(
                    "GOLD" to 5000,
                    "SOLDIERS" to 200,
                    "FOOD" to 12000,
                    "POPULATION" to 1500
                ),
                controlledTerritories = listOf("settlement_kutsal_kosumu"),
                allies = emptyList(),
                enemies = emptyList(),
                diplomaticRelations = mapOf(
                    "faction_northern_kingdom" to DiplomacyStatus.FRIENDLY,
                    "faction_merchants_guild" to DiplomacyStatus.NEUTRAL,
                    "faction_shadow_bandits" to DiplomacyStatus.HOSTILE
                ),
                status = listOf(FactionStatus.DECLINING),
                description = "Doğa ile uyum içinde yaşayan druidlerin oluşturduğu mistik grup. Ormanları korur ve büyü gücüne sahiptir.",
                leaderName = "Başdruid Elderoak",
                capitalSettlement = "settlement_kutsal_kosumu"
            ),

            // Gölge Eşkıyaları
            FactionState(
                id = "faction_shadow_bandits",
                name = "Gölge Eşkıyaları",
                type = FactionType.BANDIT,
                powerLevel = 35,
                influence = 25,
                resources = mapOf(
                    "GOLD" to 15000,
                    "SOLDIERS" to 300,
                    "WEAPONS" to 800,
                    "POPULATION" to 800
                ),
                controlledTerritories = listOf("settlement_gizli_kampment"),
                allies = emptyList(),
                enemies = listOf("faction_northern_kingdom", "faction_merchants_guild"),
                diplomaticRelations = mapOf(
                    "faction_northern_kingdom" to DiplomacyStatus.WAR,
                    "faction_merchants_guild" to DiplomacyStatus.HOSTILE,
                    "faction_forest_druids" to DiplomacyStatus.HOSTILE
                ),
                status = listOf(FactionStatus.AT_WAR, FactionStatus.BANDIT_TROUBLES),
                description = "Yasa dışı faaliyetlerle uğraşan eşkıya grubu. Ticaret yollarını hedef alır ve kaosa neden olur.",
                leaderName = "Eşkıya Reisi Karanlık Bıçak",
                capitalSettlement = "settlement_gizli_kampment"
            )
        )
    }

    /**
     * Örnek settlement verilerini oluşturur
     */
    fun createSampleSettlements(): List<SettlementState> {
        return listOf(
            // Meşeköy - Kuzey Krallığı'nın kontrolünde
            SettlementState(
                id = "settlement_mesekoy",
                name = "Meşeköy",
                type = LocationType.VILLAGE,
                population = 450,
                economyLevel = EconomyLevel.MODEST,
                happiness = 72,
                governingFaction = "faction_northern_kingdom", // Kuzey Krallığı'na bağlı
                status = listOf(SettlementStatus.FESTIVAL),
                resources = mapOf(
                    "FOOD" to 300,
                    "WOOD" to 150,
                    "STONE" to 50
                )
            ),

            // Kuzey Kasabası - Kuzey Krallığı'nın başkenti
            SettlementState(
                id = "settlement_kuzey_kasabasi",
                name = "Kuzey Kasabası",
                type = LocationType.TOWN,
                population = 2500,
                economyLevel = EconomyLevel.PROSPEROUS,
                happiness = 78,
                governingFaction = "faction_northern_kingdom",
                status = listOf(SettlementStatus.TRADE_BOOM),
                resources = mapOf(
                    "FOOD" to 1200,
                    "GOLD" to 800,
                    "SOLDIERS" to 200,
                    "STONE" to 300
                )
            ),

            // Ticaret Limanı - Tüccarlar Loncası'nın kontrolünde
            SettlementState(
                id = "settlement_ticaret_limani",
                name = "Ticaret Limanı",
                type = LocationType.CITY,
                population = 5000,
                economyLevel = EconomyLevel.WEALTHY,
                happiness = 85,
                governingFaction = "faction_merchants_guild",
                status = listOf(SettlementStatus.TRADE_BOOM),
                resources = mapOf(
                    "FOOD" to 2000,
                    "GOLD" to 3000,
                    "TRADE_GOODS" to 1500,
                    "POPULATION" to 5000
                )
            ),

            // Kutsal Koşumu - Orman Druidleri'nin kontrolünde
            SettlementState(
                id = "settlement_kutsal_kosumu",
                name = "Kutsal Koşumu",
                type = LocationType.VILLAGE,
                population = 200,
                economyLevel = EconomyLevel.POOR,
                happiness = 65,
                governingFaction = "faction_forest_druids",
                status = emptyList(),
                resources = mapOf(
                    "FOOD" to 150,
                    "WOOD" to 300,
                    "HERBS" to 100
                )
            ),

            // Gizli Kampment - Gölge Eşkıyaları'nın kontrolünde
            SettlementState(
                id = "settlement_gizli_kampment",
                name = "Gizli Kampment",
                type = LocationType.VILLAGE,
                population = 120,
                economyLevel = EconomyLevel.DESTITUTE,
                happiness = 35,
                governingFaction = "faction_shadow_bandits",
                status = listOf(SettlementStatus.BANDIT_TROUBLES, SettlementStatus.FAMINE),
                resources = mapOf(
                    "FOOD" to 30,
                    "WEAPONS" to 80,
                    "STOLEN_GOODS" to 200
                )
            )
        )
    }

    /**
     * Veritabanını örnek verilerle doldurur
     */
    suspend fun seedDatabase(database: AppDatabase) {
        val factionDao = database.factionDao()
        val settlementDao = database.settlementDao()

        // Önce mevcut verileri temizle
        factionDao.deleteAllFactions()
        settlementDao.deleteAllSettlements()

        // Örnek fraksiyon verilerini ekle
        val sampleFactions = createSampleFactions()
        val factionEntities = sampleFactions.map { it.toEntity() }
        factionDao.insertFactions(factionEntities)

        // Örnek settlement verilerini ekle
        val sampleSettlements = createSampleSettlements()
        val settlementEntities = sampleSettlements.map { it.toEntity() }
        settlementDao.insertSettlements(settlementEntities)
    }
}