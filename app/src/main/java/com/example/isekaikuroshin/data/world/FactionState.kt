package com.example.isekaikuroshin.data.world

import kotlinx.serialization.Serializable

/**
 * Yaşayan Dünya sistemi için Fraksiyon (Faction) durumunu temsil eden ana veri modeli.
 * Bu sınıf, krallıkları, loncaları, dini grupları ve diğer güçleri takip eder.
 *
 * TODO-NEM-06 kapsamında oluşturulmuş, SYS-19 raporundaki diplomasi sistemi temellerini oluşturur.
 */
@Serializable
data class FactionState(
    val id: String,
    val name: String,
    val type: FactionType,
    val powerLevel: Int, // 0-100 arası güç seviyesi
    val influence: Int, // 0-100 arası etki alanı
    val resources: Map<String, Int> = emptyMap(), // Altın, asker sayısı, vb.
    val controlledTerritories: List<String> = emptyList(), // Kontrol ettikleri yerleşim yeri ID'leri
    val allies: List<String> = emptyList(), // Müttefik fraksiyon ID'leri
    val enemies: List<String> = emptyList(), // Düşman fraksiyon ID'leri
    val diplomaticRelations: Map<String, DiplomacyStatus> = emptyMap(), // Diğer fraksiyonlarla ilişkiler
    val status: List<FactionStatus> = emptyList(), // Fraksiyonun özel durumları
    val description: String = "", // Fraksiyon açıklaması
    val leaderName: String? = null, // Lider adı
    val capitalSettlement: String? = null, // Başkent yerleşim yeri ID'si
    val lastUpdateDay: Int = 1
) {
    /**
     * Fraksiyonun genel gücünü hesaplar
     */
    fun getTotalPower(): Int {
        val basePower = powerLevel
        val territoryBonus = controlledTerritories.size * 5
        val allyBonus = allies.size * 3
        return (basePower + territoryBonus + allyBonus).coerceIn(0, 100)
    }

    /**
     * Fraksiyonun durumunu özetleyen metin
     */
    fun getStatusSummary(): String {
        val powerDesc = when {
            getTotalPower() >= 80 -> "Çok Güçlü"
            getTotalPower() >= 60 -> "Güçlü"
            getTotalPower() >= 40 -> "Orta"
            getTotalPower() >= 20 -> "Zayıf"
            else -> "Çok Zayıf"
        }

        val influenceDesc = when {
            influence >= 80 -> "Çok Etkili"
            influence >= 60 -> "Etkili"
            influence >= 40 -> "Orta Etkili"
            influence >= 20 -> "Az Etkili"
            else -> "Etkisiz"
        }

        return "Güç: $powerDesc (${getTotalPower()}%), Etki: $influenceDesc ($influence%)"
    }

    /**
     * Belirli bir fraksiyon ile diplomatik ilişkiyi döndürür
     */
    fun getDiplomaticRelationWith(factionId: String): DiplomacyStatus {
        return diplomaticRelations[factionId] ?: DiplomacyStatus.NEUTRAL
    }

    /**
     * Fraksiyon türünün Türkçe açıklaması
     */
    fun getTypeDescription(): String {
        return when (type) {
            FactionType.KINGDOM -> "Krallık"
            FactionType.GUILD -> "Lonca"
            FactionType.MERCENARY -> "Paralı Asker Grubu"
            FactionType.RELIGIOUS -> "Dini Grup"
            FactionType.BANDIT -> "Eşkıya Grubu"
        }
    }
}

/**
 * Fraksiyonların özel durumlarını temsil eden enum
 */
@Serializable
enum class FactionStatus {
    AT_WAR,          // Savaş halinde
    CIVIL_WAR,       // İç savaş
    EXPANDING,       // Genişliyor
    DECLINING,       // Geriiliyor
    TRADE_EMBARGO,   // Ticaret ambargosu
    DIPLOMATIC_CRISIS, // Diplomatik kriz
    GOLDEN_AGE,      // Altın çağ
    FAMINE,          // Kıtlık
    PLAGUE,          // Veba
    SUCCESSION_CRISIS, // Veraset krizi
    BANDIT_TROUBLES    // Eşkıya sorunu (yeni eklendi)
}

/**
 * Kaynak türlerini temsil eden enum
 */
@Serializable
enum class ResourceType {
    GOLD,        // Altın
    SOLDIERS,    // Asker
    FOOD,        // Yiyecek
    WEAPONS,     // Silah
    POPULATION,  // Nüfus
    INFLUENCE,   // Etki
    TRADE_GOODS  // Ticaret malları
}