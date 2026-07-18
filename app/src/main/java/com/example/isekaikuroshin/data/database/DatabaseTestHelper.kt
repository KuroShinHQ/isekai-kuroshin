package com.example.isekaikuroshin.data.database

import android.util.Log
import com.example.isekaikuroshin.data.world.EconomyLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Veritabanı testleri için yardımcı sınıf.
 * TODO-NEM-06 için fraksiyon sistemi test ve doğrulama fonksiyonları.
 */
object DatabaseTestHelper {

    private const val TAG = "DatabaseTestHelper"

    /**
     * Veritabanını seed data ile doldur ve test et
     */
    fun initializeAndTestWorldData(database: AppDatabase) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "🌍 Yaşayan Dünya sistemi başlatılıyor...")

                // Seed data'yı yükle
                WorldSeedData.seedDatabase(database)
                Log.d(TAG, "✅ Seed data başarıyla yüklendi")

                // Fraksiyon verilerini test et
                testFactionData(database)

                // Settlement verilerini test et
                testSettlementData(database)

                // İlişkileri test et
                testFactionSettlementRelations(database)

                Log.d(TAG, "🎉 Tüm testler başarıyla tamamlandı!")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Veritabanı testi sırasında hata oluştu: ${e.message}", e)
            }
        }
    }

    private suspend fun testFactionData(database: AppDatabase) {
        val factionDao = database.factionDao()

        // Tüm fraksiyonları getir
        val allFactions = factionDao.getAllFactions()
        Log.d(TAG, "📊 Toplam ${allFactions.size} fraksiyon bulundu")

        allFactions.forEach { entity ->
            val faction = entity.toFactionState()
            Log.d(TAG, "🏰 Fraksiyon: ${faction.name} (${faction.getTypeDescription()}) - Güç: ${faction.getTotalPower()}%")
        }

        // Kuzey Krallığı'nı test et
        val northernKingdom = factionDao.getFactionById("faction_northern_kingdom")
        if (northernKingdom != null) {
            val faction = northernKingdom.toFactionState()
            Log.d(TAG, "👑 Kuzey Krallığı bulundu: ${faction.getStatusSummary()}")
            Log.d(TAG, "🏘️ Kontrol ettikleri topraklar: ${faction.controlledTerritories}")
        } else {
            Log.e(TAG, "❌ Kuzey Krallığı bulunamadı!")
        }

        // Güçlü fraksiyonları test et
        val powerfulFactions = factionDao.getFactionsWithMinPower(60)
        Log.d(TAG, "💪 60+ güç seviyesindeki fraksiyonlar: ${powerfulFactions.size}")

        // Krallık türünde fraksiyonları test et
        val kingdoms = factionDao.getFactionsByType("KINGDOM")
        Log.d(TAG, "👑 Krallık türünde fraksiyon sayısı: ${kingdoms.size}")
    }

    private suspend fun testSettlementData(database: AppDatabase) {
        val settlementDao = database.settlementDao()

        // Tüm settlement'ları getir
        val allSettlements = settlementDao.getAllSettlements()
        Log.d(TAG, "🏘️ Toplam ${allSettlements.size} yerleşim yeri bulundu")

        allSettlements.forEach { entity ->
            val settlement = entity.toSettlementState()
            Log.d(TAG, "🏠 ${settlement.name} (${settlement.getSizeCategory()}) - ${settlement.getStatusSummary()}")
            if (settlement.governingFaction != null) {
                Log.d(TAG, "   ↳ Yönetici Fraksiyon: ${settlement.governingFaction}")
            }
        }

        // Meşeköy'ü özel olarak test et
        val mesekoy = settlementDao.getSettlementById("settlement_mesekoy")
        if (mesekoy != null) {
            val settlement = mesekoy.toSettlementState()
            Log.d(TAG, "🌳 Meşeköy bulundu!")
            Log.d(TAG, "   ↳ Yönetici Fraksiyon: ${settlement.governingFaction}")
            Log.d(TAG, "   ↳ Durum: ${settlement.getStatusSummary()}")
        } else {
            Log.e(TAG, "❌ Meşeköy bulunamadı!")
        }
    }

    private suspend fun testFactionSettlementRelations(database: AppDatabase) {
        val factionDao = database.factionDao()
        val settlementDao = database.settlementDao()

        Log.d(TAG, "🔗 Fraksiyon-Settlement ilişkileri test ediliyor...")

        // Her fraksiyon için kontrol ettikleri settlement'ları kontrol et
        val allFactions = factionDao.getAllFactions()
        for (factionEntity in allFactions) {
            val faction = factionEntity.toFactionState()

            Log.d(TAG, "🏰 ${faction.name} fraksiyonu:")

            for (settlementId in faction.controlledTerritories) {
                val settlementEntity = settlementDao.getSettlementById(settlementId)
                if (settlementEntity != null) {
                    val settlement = settlementEntity.toSettlementState()
                    val isCorrectlyLinked = settlement.governingFaction == faction.id

                    val status = if (isCorrectlyLinked) "✅" else "❌"
                    Log.d(TAG, "   $status ${settlement.name} - Bağlantı durumu: $isCorrectlyLinked")

                    if (!isCorrectlyLinked) {
                        Log.e(TAG, "      Beklenen: ${faction.id}, Gerçek: ${settlement.governingFaction}")
                    }
                } else {
                    Log.e(TAG, "   ❌ Settlement bulunamadı: $settlementId")
                }
            }
        }

        // Özel test: Meşeköy - Kuzey Krallığı bağlantısı
        val mesekoy = settlementDao.getSettlementById("settlement_mesekoy")
        val northernKingdom = factionDao.getFactionById("faction_northern_kingdom")

        if (mesekoy != null && northernKingdom != null) {
            val settlementState = mesekoy.toSettlementState()
            val factionState = northernKingdom.toFactionState()

            val isLinked = settlementState.governingFaction == factionState.id
            val controlsSettlement = factionState.controlledTerritories.contains(settlementState.id)

            Log.d(TAG, "🎯 Meşeköy-Kuzey Krallığı özel testi:")
            Log.d(TAG, "   Settlement → Faction bağlantısı: ${if (isLinked) "✅" else "❌"}")
            Log.d(TAG, "   Faction → Settlement bağlantısı: ${if (controlsSettlement) "✅" else "❌"}")

            if (isLinked && controlsSettlement) {
                Log.d(TAG, "🎉 Meşeköy-Kuzey Krallığı bağlantısı mükemmel!")
            } else {
                Log.e(TAG, "❌ Meşeköy-Kuzey Krallığı bağlantısında sorun var!")
            }
        }
    }

    /**
     * TODO-TEST-01 için uzun süreli "Yaşayan Dünya" simülasyon testi
     * 100+ oyun günü simüle eder ve sistemi test eder
     */
    fun runLongTermSimulation(
        database: AppDatabase,
        worldUpdateEngine: com.example.isekaikuroshin.engine.WorldUpdateEngine,
        targetDays: Int = 100
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "🚀 TODO-TEST-01: Uzun Süreli Yaşayan Dünya Simülasyonu Başlıyor...")
                Log.d(TAG, "📅 Hedef: $targetDays oyun günü simülasyonu")

                val startTime = System.currentTimeMillis()

                // Başlangıç durumunu kaydet
                logWorldStateSnapshot(database, 0)

                // Simülasyon döngüsü
                for (day in 1..targetDays) {
                    Log.d(TAG, "🌅 Gün $day simüle ediliyor...")

                    // WorldUpdateEngine'i tetikle
                    worldUpdateEngine.forceWorldTick()

                    // Her 25 günde bir durum raporu
                    if (day % 25 == 0) {
                        Log.d(TAG, "📊 Gün $day: Durum Raporu")
                        logWorldStateSnapshot(database, day)

                        // Engine durumu raporu
                        val engineStatus = worldUpdateEngine.getEngineStatusReport()
                        Log.d(TAG, "🔧 Engine Durumu:\n$engineStatus")
                    }

                    // Kısa bekleme (performansı izlemek için)
                    kotlinx.coroutines.delay(50)
                }

                val endTime = System.currentTimeMillis()
                val totalTimeSeconds = (endTime - startTime) / 1000.0

                // Final raporu
                Log.d(TAG, "✅ TODO-TEST-01: Simülasyon Tamamlandı!")
                Log.d(TAG, "⏱️ Toplam süre: ${totalTimeSeconds} saniye")
                Log.d(TAG, "🔢 Günlük ortalama: ${totalTimeSeconds / targetDays} saniye/gün")

                // Son durum raporu
                logWorldStateSnapshot(database, targetDays)

                // Performans sonuçları
                logPerformanceResults(database, targetDays, totalTimeSeconds)

            } catch (e: Exception) {
                Log.e(TAG, "❌ TODO-TEST-01 simülasyonu sırasında hata: ${e.message}", e)
            }
        }
    }

    /**
     * Belirli bir gündeki dünya durumunun snapshot'ını loglar
     */
    private suspend fun logWorldStateSnapshot(database: AppDatabase, day: Int) {
        Log.d(TAG, "📸 === GÜN $day: DÜNYA DURUMU SNAPSHOT'I ===")

        // NPC durumları
        val allNpcs = database.dynamicNpcDao().getAllNpcStates()
        val nemesisCount = allNpcs.count { it.nemesisLevel > 0 }
        val averageLevel = allNpcs.map { it.level }.average()

        Log.d(TAG, "👥 NPC İstatistikleri:")
        Log.d(TAG, "   Toplam NPC: ${allNpcs.size}")
        Log.d(TAG, "   Nemesis sayısı: $nemesisCount")
        Log.d(TAG, "   Ortalama seviye: ${String.format("%.1f", averageLevel)}")

        // Fraksiyon durumları
        val allFactions = database.factionDao().getAllFactions()
        val avgPower = allFactions.map { it.toFactionState().getTotalPower() }.average()

        Log.d(TAG, "🏰 Fraksiyon İstatistikleri:")
        Log.d(TAG, "   Toplam fraksiyon: ${allFactions.size}")
        Log.d(TAG, "   Ortalama güç: ${String.format("%.1f", avgPower)}")

        // Settlement durumları
        val allSettlements = database.settlementDao().getAllSettlements()
        val avgProsperity = allSettlements.map { it.toSettlementState().economyLevel.ordinal }.average()

        Log.d(TAG, "🏘️ Settlement İstatistikleri:")
        Log.d(TAG, "   Toplam settlement: ${allSettlements.size}")
        Log.d(TAG, "   Ortalama refah: ${String.format("%.1f", avgProsperity)}")

        Log.d(TAG, "=== SNAPSHOT SONU ===")
    }

    /**
     * Performans sonuçlarını analiz eder ve loglar
     */
    private suspend fun logPerformanceResults(database: AppDatabase, totalDays: Int, totalTime: Double) {
        Log.d(TAG, "🎯 === PERFORMANS ANALİZİ ===")

        val memoryUsage = Runtime.getRuntime().let { runtime ->
            val totalMemory = runtime.totalMemory()
            val freeMemory = runtime.freeMemory()
            val usedMemory = totalMemory - freeMemory
            "Kullanılan: ${usedMemory / 1024 / 1024}MB, Toplam: ${totalMemory / 1024 / 1024}MB"
        }

        Log.d(TAG, "💾 Bellek Kullanımı: $memoryUsage")
        Log.d(TAG, "⚡ Performans Metrikleri:")
        Log.d(TAG, "   Simülasyon hızı: ${totalDays / totalTime} gün/saniye")
        Log.d(TAG, "   Günlük işlem süresi: ${(totalTime * 1000) / totalDays} ms/gün")

        // Veritabanı büyüklüğü kontrolleri
        val npcCount = database.dynamicNpcDao().getAllNpcStates().size
        val factionCount = database.factionDao().getAllFactions().size
        val settlementCount = database.settlementDao().getAllSettlements().size

        Log.d(TAG, "🗃️ Veritabanı Durumu:")
        Log.d(TAG, "   NPC kayıtları: $npcCount")
        Log.d(TAG, "   Fraksiyon kayıtları: $factionCount")
        Log.d(TAG, "   Settlement kayıtları: $settlementCount")

        Log.d(TAG, "=== PERFORMANS ANALİZİ SONU ===")
    }
}
