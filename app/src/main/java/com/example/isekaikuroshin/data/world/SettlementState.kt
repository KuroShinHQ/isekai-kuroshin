package com.example.isekaikuroshin.data.world

import com.example.isekaikuroshin.data.LocationType
import kotlinx.serialization.Serializable

/**
 * Yaşayan Dünya sistemi için Settlement (Yerleşim Yeri) durumunu temsil eden ana veri modeli.
 * Bu sınıf, şehir, kasaba ve köylerin dinamik özelliklerini (ekonomi, nüfus, mutluluk) takip eder.
 *
 * TODO-NEM-05 kapsamında oluşturulmuş, zamanla daha karmaşık özellikler eklenecektir.
 */
@Serializable
data class SettlementState(
    val id: String,
    val name: String,
    val type: LocationType,
    val population: Int,
    val economyLevel: EconomyLevel,
    val happiness: Int, // 0-100 arası değer
    val governingFaction: String? = null, // Gelecekte fraksiyon sistemi için
    val status: List<SettlementStatus> = emptyList(),
    val resources: Map<String, Int> = emptyMap(),
    val lastUpdateDay: Int = 1
) {
    /**
     * Yerleşim yerinin genel durumunu özetleyen insan okunabilir metin
     */
    fun getStatusSummary(): String {
        val economyDesc = when (economyLevel) {
            EconomyLevel.DESTITUTE -> "Yoksul"
            EconomyLevel.POOR -> "Fakir"
            EconomyLevel.MODEST -> "Mütevazı"
            EconomyLevel.PROSPEROUS -> "Müreffeh"
            EconomyLevel.WEALTHY -> "Zengin"
        }

        val happinessDesc = when {
            happiness >= 80 -> "Çok Mutlu"
            happiness >= 60 -> "Mutlu"
            happiness >= 40 -> "Kararsız"
            happiness >= 20 -> "Mutsuz"
            else -> "Çok Mutsuz"
        }

        return "Ekonomi: $economyDesc, Halkın Mutluluğu: $happinessDesc (${happiness}%)"
    }

    /**
     * Yerleşim yerinin boyut kategorisini döndürür
     */
    fun getSizeCategory(): String {
        return when (type) {
            LocationType.CITY -> "Şehir"
            LocationType.TOWN -> "Kasaba"
            LocationType.VILLAGE -> "Köy"
            else -> "Yerleşim"
        }
    }
}

/**
 * Yerleşim yerinin ekonomik durumunu temsil eden enum
 */
@Serializable
enum class EconomyLevel {
    DESTITUTE,  // Yoksul
    POOR,       // Fakir
    MODEST,     // Mütevazı
    PROSPEROUS, // Müreffeh
    WEALTHY     // Zengin
}

/**
 * Yerleşim yerinin özel durumlarını temsil eden enum
 */
@Serializable
enum class SettlementStatus {
    UNDER_SIEGE,    // Kuşatma altında
    PLAGUE,         // Veba
    FAMINE,         // Kıtlık
    FESTIVAL,       // Festival
    TRADE_BOOM,     // Ticaret patlaması
    BANDIT_TROUBLES // Eşkıya sorunu
}

/**
 * Gelecekte fraksiyon sistemi için diplomasi durumları
 */
@Serializable
enum class DiplomacyStatus {
    WAR,        // Savaş
    HOSTILE,    // Düşman
    NEUTRAL,    // Tarafsız
    FRIENDLY,   // Dost
    ALLIANCE    // Müttefik
}

/**
 * Gelecekte fraksiyon sistemi için fraksiyon türleri
 */
@Serializable
enum class FactionType {
    KINGDOM,    // Krallık
    GUILD,      // Lonca
    MERCENARY,  // Paralı asker grubu
    RELIGIOUS,  // Dini grup
    BANDIT      // Eşkıya grubu
}