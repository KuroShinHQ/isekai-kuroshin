package com.example.isekaikuroshin.data

import kotlinx.serialization.Serializable

/**
 * DynamicContent System - GM Tarafından Üretilen Eşsiz İçerikler
 *
 * Bu sistem GM'in hikaye sırasında ürettiği tüm eşsiz içerikleri saklar:
 * - Elementler (hibrit, özel)
 * - Yetenekler (skill varyasyonları)
 * - Itemler (eşsiz ekipmanlar)
 *
 * Kullanıcı beğendiklerini Settings'den export edip icon ekleyebilir.
 * Yeni oyunda import edildiğinde GM bu içerikleri kullanır.
 */

@Serializable
enum class DynamicContentType {
    ELEMENT,        // Eşsiz element (ör: "Gölge Ateşi")
    SKILL,          // Eşsiz skill (ör: "Yıldırım Dansı")
    ITEM,           // Eşsiz item (ör: "Kırık Kral Kılıcı")
    NPC,            // Eşsiz NPC (farklı bir sistem ama aynı export/import)
    LOCATION        // Eşsiz location
}

@Serializable
data class DynamicContent(
    val id: String,
    val type: DynamicContentType,
    val name: String,
    val description: String,

    // GM Metadata
    val gmGeneratedContext: String = "",  // GM'in ürettiği bağlam (hikaye, kitap referansı)
    val createdAtDay: Int = 1,  // Hangi günde üretildi
    val createdAtTimestamp: Long = System.currentTimeMillis(),

    // Icon System
    val iconPath: String? = null,  // Kullanıcı tarafından eklenen icon yolu
    val placeholderColor: String = "#FF00BFFF",  // Gradient placeholder rengi (hex)
    val hasIcon: Boolean = false,  // Icon yüklendi mi?

    // Export/Import
    val isExported: Boolean = false,  // Kullanıcı export etti mi?
    val isFavorite: Boolean = false,  // Kullanıcı beğendi mi?

    // Element-Specific Fields
    val elementData: DynamicElementData? = null,

    // Skill-Specific Fields
    val skillData: DynamicSkillData? = null,

    // Item-Specific Fields
    val itemData: DynamicItemData? = null
)

/**
 * Dynamic Element Data - GM'in ürettiği eşsiz elementler
 */
@Serializable
data class DynamicElementData(
    val displayName: String,
    val isPrimary: Boolean = false,
    val parentElements: List<String> = emptyList(),  // Ana elementlerin ID'leri
    val powerMultiplier: Float = 1.0f,
    val specialEffect: String = ""  // GM'in verdiği özel efekt açıklaması
)

/**
 * Dynamic Skill Data - GM'in ürettiği eşsiz yetenekler
 */
@Serializable
data class DynamicSkillData(
    val elementType: String,  // ElementType ID'si
    val rarity: String,  // SkillRarity enum string
    val tier: String,  // "Temel", "Gelişmiş", "Usta"
    val manaCost: Int,
    val cooldown: Int,
    val diceModifier: Int,
    val statBonuses: Map<String, Float> = emptyMap(),  // StatType string keys
    val parentSkillId: String? = null,  // Hangi skill'den türedi
    val specialMechanic: String = ""  // GM'in verdiği özel mekanik açıklaması
)

/**
 * Dynamic Item Data - GM'in ürettiği eşsiz itemler
 */
@Serializable
data class DynamicItemData(
    val itemType: String,  // "WEAPON", "ARMOR", "CONSUMABLE", etc.
    val rarity: String,
    val durability: Int,
    val maxDurability: Int,
    val gearScore: Int,
    val statBonuses: Map<String, Float> = emptyMap(),
    val specialEffect: String = "",  // GM'in verdiği özel efekt
    val loreText: String = ""  // Item'ın hikayesi (GM-generated)
)

/**
 * Export Package - Kullanıcının export ettiği içerik paketi
 */
@Serializable
data class ContentExportPackage(
    val packageName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val userNote: String = "",
    val contents: List<DynamicContent> = emptyList()
)
