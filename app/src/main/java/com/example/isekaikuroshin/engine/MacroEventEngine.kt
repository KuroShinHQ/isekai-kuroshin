package com.example.isekaikuroshin.engine

import com.example.isekaikuroshin.data.GameStateManager
import com.example.isekaikuroshin.data.database.FactionDao
import com.example.isekaikuroshin.data.database.SettlementDao
import com.example.isekaikuroshin.data.database.toFactionState
import com.example.isekaikuroshin.data.database.toSettlementState
import com.example.isekaikuroshin.data.world.FactionState
import com.example.isekaikuroshin.data.world.DiplomacyStatus
import com.example.isekaikuroshin.utils.GameLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MacroEventEngine - AI Güdümlü Makro Olaylar Motoru
 *
 * TODO-NEM-09 kapsamında oluşturulan bu motor, GameMaster AI'ın dünya durumunu analiz ederek
 * fraksiyonlar arasında savaş başlatma, barış yapma gibi makro-politik kararları tetiklemesini sağlar.
 *
 * Bu motor, "Yaşayan Dünya" sisteminin 4. Fazı'nı temsil eder ve dünyada AI güdümlü,
 * büyük ölçekli değişikliklerin otomatik olarak gerçekleşmesini sağlar.
 */
@Singleton
class MacroEventEngine @Inject constructor(
    private val gameMasterEngine: GameMasterEngine,
    private val factionDao: FactionDao,
    private val settlementDao: SettlementDao
) {

    companion object {
        private const val TAG = "MacroEventEngine"
        private const val MACRO_EVENT_INTERVAL = 7 // Her 7 günde bir makro olay kontrolü

        // AI'a stratejik karar vermesi için özel prompt
        private const val STRATEGIC_DECISION_PROMPT = """
SEN BİR STRATEJİK ANALİZCİSİN. Aşağıdaki dünya durumunu analiz et ve büyük ölçekli politik kararlar ver.

[DÜNYA DURUMU RAPORU]
%s

[GÖREV]
Bu bilgilere dayanarak, dünyada gerçekleşmesi gereken büyük olayları değerlendir:

1. Hangi fraksiyonlar arasında SAVAŞ başlamalı? (Güç dengesizlikleri, düşmanlıklar, kaynak çatışmaları)
2. Hangi savaşlar BİTMELİ? (Uzun süren çatışmalar, güçsüz taraflar, ekonomik zararlar)
3. Hangi fraksiyonlar BARIŞ yapmalı? (Karşılıklı fayda, ortak tehditler, ticaret fırsatları)

CEVABINI SADECE AŞAĞIDAKİ JSON FORMATINDA VER (başka metin YAZMA):

{
  "journalEntry": "Dünyada büyük politik değişiklikler yaşanıyor...",
  "macroEvent": {
    "eventType": "WAR_DECLARATION | PEACE_TREATY | null",
    "factionIds": ["faction1_id", "faction2_id"],
    "reason": "Stratejik sebep açıklaması"
  }
}

EĞER HİÇBİR MAKRO OLAY GEREKMİYORSA: macroEvent: null yazacaksın.
"""
    }

    /**
     * Makro olay tetikleme kararını AI'a danışarak verir
     * Bu fonksiyon WorldUpdateEngine tarafından periyodik olarak çağrılır
     */
    suspend fun considerTriggeringMacroEvent(): Boolean {
        try {
            GameLogger.logSystem("$TAG: Makro olay analizi başlatılıyor...")

            // 1. Dünya durumu analizini topla
            val worldAnalysis = generateWorldStateAnalysis()

            if (worldAnalysis.isEmpty()) {
                GameLogger.logSystem("$TAG: Dünya durumu analizi boş, makro olay atlandı")
                return false
            }

            // 2. AI'a stratejik karar prompt'u gönder
            val strategicPrompt = STRATEGIC_DECISION_PROMPT.format(worldAnalysis)

            GameLogger.logSystem("$TAG: AI'a stratejik rapor gönderiliyor...")
            GameLogger.logSystem("$TAG: === STRATEJİK RAPOR PROMPT ===")
            GameLogger.logSystem(strategicPrompt)
            GameLogger.logSystem("$TAG: === RAPOR SONU ===")

            // 3. GameMasterEngine üzerinden AI'ı çağır
            val aiResponse = gameMasterEngine.processPromptDirectly(strategicPrompt)

            if (aiResponse != null) {
                GameLogger.logSystem("$TAG: AI'dan makro olay yanıtı alındı")
                GameLogger.logSystem("$TAG: AI YANITI: ${aiResponse.journalEntry}")

                // 4. Makro olayı execute et
                if (aiResponse.macroEvent != null) {
                    GameLogger.logSystem("$TAG: === MAKRO OLAY TETİKLENDİ ===")
                    GameLogger.logSystem("$TAG: Olay Tipi: ${aiResponse.macroEvent.eventType}")
                    GameLogger.logSystem("$TAG: Fraksiyonlar: ${aiResponse.macroEvent.factionIds}")
                    GameLogger.logSystem("$TAG: Sebep: ${aiResponse.macroEvent.reason}")

                    // executeGMActions'ı çağır (UI callback olmadan)
                    // Bu kısımda GameStateManager'a erişmemiz gerekecek, ancak şimdilik loglayalım
                    GameLogger.logSystem("$TAG: MAKRO OLAY executeGMActions çağrısı gerekiyor!")

                    return true
                } else {
                    GameLogger.logSystem("$TAG: AI makro olay gereksiz buldu")
                }
            } else {
                GameLogger.logSystem("$TAG: AI'dan yanıt alınamadı")
            }

            return false

        } catch (e: Exception) {
            GameLogger.logSystem("$TAG: Makro olay analizi hatası: ${e.message}")
            return false
        }
    }

    /**
     * Dünya durumunu analiz ederek AI'ın anlayacağı stratejik rapor oluşturur
     */
    private suspend fun generateWorldStateAnalysis(): String {
        try {
            // Tüm fraksiyonları ve settlement'ları çek
            val factionEntities = factionDao.getAllFactions()
            val factions = factionEntities.map { it.toFactionState() }

            val settlementEntities = settlementDao.getAllSettlements()
            val settlements = settlementEntities.map { it.toSettlementState() }

            if (factions.isEmpty()) {
                return "Dünyada henüz aktif fraksiyon bulunmuyor."
            }

            val analysis = StringBuilder()

            analysis.appendLine("=== FRAKSIYON ANALİZİ ===")
            factions.forEach { faction ->
                val territories = settlements.filter {
                    faction.controlledTerritories.contains(it.id) || faction.capitalSettlement == it.id
                }

                analysis.appendLine("📊 ${faction.name}:")
                analysis.appendLine("   • Güç Seviyesi: ${faction.powerLevel}/100")
                analysis.appendLine("   • Etki Alanı: ${faction.influence}/100")
                analysis.appendLine("   • Kontrollü Bölge: ${territories.size} yerleşim")
                analysis.appendLine("   • Toplam Güç: ${faction.getTotalPower()}")
                analysis.appendLine("   • Müttefik: ${faction.allies.size} fraksiyon")
                analysis.appendLine("   • Düşman: ${faction.enemies.size} fraksiyon")
                analysis.appendLine("   • Durum: ${faction.status.joinToString(", ")}")
                analysis.appendLine()
            }

            analysis.appendLine("=== DİPLOMATİK İLİŞKİLER ===")
            factions.forEach { faction1 ->
                factions.filter { it.id != faction1.id }.forEach { faction2 ->
                    val relation = faction1.getDiplomaticRelationWith(faction2.id)
                    if (relation != DiplomacyStatus.NEUTRAL) {
                        analysis.appendLine("🤝 ${faction1.name} ↔ ${faction2.name}: ${relation.name}")
                    }
                }
            }

            analysis.appendLine("\n=== EKONOMİK DURUM ===")
            val economicData = settlements.groupBy { it.economyLevel }
            economicData.forEach { (level, settlementList) ->
                analysis.appendLine("💰 ${level.name}: ${settlementList.size} yerleşim")
            }

            analysis.appendLine("\n=== POTANSİYEL ÇATIŞMA BÖLGELERİ ===")
            factions.forEach { faction1 ->
                factions.filter { it.id != faction1.id }.forEach { faction2 ->
                    val powerDifference = kotlin.math.abs(faction1.getTotalPower() - faction2.getTotalPower())
                    val relation = faction1.getDiplomaticRelationWith(faction2.id)

                    if (powerDifference > 30 && relation == DiplomacyStatus.HOSTILE) {
                        analysis.appendLine("⚠️ ${faction1.name} vs ${faction2.name}: Güç farkı $powerDifference, ilişki $relation")
                    }
                }
            }

            return analysis.toString()

        } catch (e: Exception) {
            GameLogger.logSystem("$TAG: Dünya analizi hatası: ${e.message}")
            return "Dünya durumu analizi yapılamadı: ${e.message}"
        }
    }

    /**
     * Makro olay kontrolünün yapılması gereken günleri belirler
     */
    fun shouldCheckMacroEvents(currentDay: Int): Boolean {
        return currentDay % MACRO_EVENT_INTERVAL == 0 && currentDay > 0
    }
}