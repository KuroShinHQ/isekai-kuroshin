package com.example.isekaikuroshin.data

import kotlinx.serialization.Serializable

/**
 * Haritadaki bir konumu temsil eder. Şehirler, zindanlar, ormanlar vb. olabilir.
 * Projenin "canlı dünya" felsefesine uygun olarak dinamik bilgiler içerir.
 */
@Serializable
data class Location(
    // --- Temel Bilgiler ---
    val id: String, // Benzersiz kimlik (örn: "city_ironhold", "forest_whispering_woods")
    val name: String, // Konumun oyuncuya gösterilecek adı (örn: "Demir Kale Şehri")
    val description: String, // Konumun atmosferini ve detaylarını anlatan metin

    // --- Mekanik ve Sınıflandırma ---
    val type: LocationType = LocationType.WILDERNESS, // Konumun türü (Şehir, Zindan vb.)
    val dangerLevel: Int = 1, // Konumun tehlike seviyesi (örn: 1-100)
    val requiredLevel: Int = 1, // EKLENDİ - Konum için önerilen/gerekli seviye

    // --- Keşif ve Etkileşim ---
    val knownResources: List<String> = emptyList(), // Bu konumda bulunabilecek kaynakların ID'leri
    val associatedQuests: List<String> = emptyList(), // Bu konumla ilişkili görevlerin ID'leri
    val isDiscovered: Boolean = false, // Oyuncu tarafından keşfedilip keşfedilmediği (Haritadaki "Savaş Sisi" için)

    // --- Dinamik Durum ---
    val currentWeather: String? = null, // Anlık hava durumu (örn: "Güneşli", "Fırtınalı")
    val coordinates: Coordinates? = null // Harita üzerindeki X,Y koordinatları (ileride kullanılacak)
)

/**
 * Konumların türlerini sınıflandırmak için kullanılır.
 * Oyun mantığının farklı konum türlerine göre farklı davranmasını sağlar.
 */
@Serializable
enum class LocationType {
    CITY,
    TOWN,
    VILLAGE,
    WILDERNESS,
    DUNGEON,
    RUIN,
    CAVE,
    SPECIAL // Özel, hikaye odaklı yerler
}

/**
 * Harita üzerinde 2D koordinatları temsil eden basit bir veri sınıfı.
 */
@Serializable
data class Coordinates(
    val x: Int,
    val y: Int
)

object WorldLocations {
    val FOREST_CAMP = Location(
        id = "forest_camp",
        name = "forest_camp", // Localization key
        description = "forest_camp_desc", // Localization key
        type = LocationType.WILDERNESS,
        dangerLevel = 2,
        requiredLevel = 1,
        isDiscovered = true
    )

    val VILLAGE = Location(
        id = "village_oakwood",
        name = "Meşeköy",
        description = "Huzurlu bir çiftçi köyü.",
        type = LocationType.VILLAGE,
        dangerLevel = 1,
        requiredLevel = 1 // EKLENDİ
    )
}