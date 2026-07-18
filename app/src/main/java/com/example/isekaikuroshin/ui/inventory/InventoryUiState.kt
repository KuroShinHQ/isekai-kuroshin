
package com.example.isekaikuroshin.ui.inventory

import com.example.isekaikuroshin.ui.components.CardData
import com.example.isekaikuroshin.data.Currency

/**
 * Represents the UI state for the Inventory screen.
 */
data class InventoryUiState(
    val capacity: String = "",
    val currency: Currency = Currency(),
    val gearScore: String = "",
    val equipmentSlots: List<EquipmentSlotState> = emptyList(),
    val inventoryItems: List<CardData.ItemCard> = emptyList()
)

/**
 * Represents the state of a single equipment slot.
 * @param slotName The name of the equipment slot (e.g., "Kafa", "Ana El").
 * @param item The item currently in the slot, or null if it's empty.
 */
data class EquipmentSlotState(
    val slotName: String,
    val item: CardData.ItemCard?
)
