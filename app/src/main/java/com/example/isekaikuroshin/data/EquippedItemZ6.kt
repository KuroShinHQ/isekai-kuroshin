package com.example.isekaikuroshin.data

import kotlinx.serialization.Serializable

@Serializable
data class EquippedItemZ6(
    val id: String = System.currentTimeMillis().toString() + "_" + (Math.random() * 1000).toInt(), // Basit benzersiz ID
    val name: String = "Bilinmeyen Eşya",
    val description: String = "Bu eşyanın bir açıklaması yok.",
    val weight: Float = 0.0f, // AĞIRLIK EKLENDİ
    val rarity: String = "COMMON", // NADİRLİK EKLENDİ (InventoryViewModel'de kullanılıyor)
    val type: String? = null, // Item type (e.g., "weapon", "armor", "consumable")
    val equipmentSlot: String? = null, // Equipment slot (e.g., "weapon", "armor", "accessory")
    val statBonuses: Map<String, Float> = emptyMap(), // Equipment stat bonusları (örn: {"STR_FLAT": 5.0, "AGI_FLAT": 3.0})
    val consumableEffects: Map<String, Int> = emptyMap() // G135: Consumable effects (örn: {"hungerRestore": 20, "healthRestore": 50})
    // G135 CONSUMABLE EFFECT KEYS:
    // - "hungerRestore": Açlık barı restore (0-100)
    // - "healthRestore": Can restore (HP)
    // - "manaRestore": Mana restore (MP)
    // - "staminaRestore": Stamina restore
    // - "fatigueReduce": Yorgunluk azaltma
    // val icon: String? = null // İkon için bir alan eklenebilir (G126)
)
