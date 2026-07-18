package com.example.isekaikuroshin.data

import com.example.isekaikuroshin.engine.StatType
import kotlinx.serialization.Serializable

enum class BadgeType {
    BADGE, // Mekanik bonus verir
    TITLE, // Anlatısal etkiye sahiptir, sadece biri aktif olabilir
    ACHIEVEMENT // Sadece kazanılan bir başarıyı temsil eder
}

@Serializable
data class Badge(
    val id: String, // Benzersiz bir kimlik
    val name: String,
    val description: String,
    val tier: String,
    val type: BadgeType, // YENİ: Rozet, Unvan veya Başarım olduğunu belirtir
    val statBonuses: Map<StatType, Float> = emptyMap(),
    val narrativeEffect: String? = null // YENİ: Unvanların anlatısal etkisini tutar
)

/**
 * Kadim Mühür Mastery Badge'leri
 */
object SealMasteryBadges {

    /**
     * Seal mastery tamamlandığında verilen badge'i döndür
     */
    fun getBadgeForMasteredSeal(sealId: String): Badge? {
        return when (sealId) {
            "seal_fire_basic" -> Badge(
                id = "badge_fire_seal_master",
                name = "Ateş Üstadı",
                description = "Ateş mührünü tamamen ustalaştınız",
                tier = "Bronze",
                type = BadgeType.ACHIEVEMENT,
                statBonuses = mapOf(StatType.INT to 2f)
            )
            "seal_water_basic" -> Badge(
                id = "badge_water_seal_master",
                name = "Su Akışı Ustası",
                description = "Su mührünü tamamen ustalaştınız",
                tier = "Bronze",
                type = BadgeType.ACHIEVEMENT,
                statBonuses = mapOf(StatType.WIS to 2f)
            )
            "seal_earth_basic" -> Badge(
                id = "badge_earth_seal_master",
                name = "Taş Kalbli",
                description = "Toprak mührünü tamamen ustalaştınız",
                tier = "Silver",
                type = BadgeType.ACHIEVEMENT,
                statBonuses = mapOf(StatType.END to 3f)
            )
            "seal_wind_basic" -> Badge(
                id = "badge_wind_seal_master",
                name = "Rüzgar Yolcusu",
                description = "Rüzgar mührünü tamamen ustalaştınız",
                tier = "Silver",
                type = BadgeType.ACHIEVEMENT,
                statBonuses = mapOf(StatType.AGI to 3f)
            )
            "seal_lightning_basic" -> Badge(
                id = "badge_lightning_seal_master",
                name = "Şimşek Efendisi",
                description = "Şimşek mührünü tamamen ustalaştınız",
                tier = "Gold",
                type = BadgeType.ACHIEVEMENT,
                statBonuses = mapOf(
                    StatType.INT to 3f,
                    StatType.AGI to 2f
                )
            )
            else -> null
        }
    }

    /**
     * Tüm seal master badge'lerini döndür
     */
    fun getAllSealMasteryBadges(): List<Badge> {
        return listOf(
            "seal_fire_basic",
            "seal_water_basic",
            "seal_earth_basic",
            "seal_wind_basic",
            "seal_lightning_basic"
        ).mapNotNull { getBadgeForMasteredSeal(it) }
    }
}