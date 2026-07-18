package com.example.isekaikuroshin.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.isekaikuroshin.R
import com.example.isekaikuroshin.data.Currency
import com.example.isekaikuroshin.data.EquippedItemZ6
import com.example.isekaikuroshin.game.GameStateManager // G135.4: Fixed package (game, not data)
// Import PlayerState if it's not implicitly available through GameStateManager's gameState
import com.example.isekaikuroshin.data.PlayerState
import com.example.isekaikuroshin.ui.components.CardData
import com.example.isekaikuroshin.ui.components.CardRarity
import com.example.isekaikuroshin.ui.components.StatType // engine.StatType olmalı, kontrol edilecek
import com.example.isekaikuroshin.ui.inventory.InventoryUiState // G135.4: Data class import
import com.example.isekaikuroshin.ui.inventory.EquipmentSlotState // G135.4: Data class import
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

enum class SortType {
    NAME, GEAR_SCORE, RARITY
}

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val gameStateManager: GameStateManager
) : ViewModel() {

    private val allEquipmentSlots = listOf(
        "Kafa", "Boyun", "Göğüs", "Bacaklar", "Eldivenler", "Ayakkabılar",
        "Ana El", "Yan El", "Yüzük 1", "Yüzük 2", "Küpe 1", "Küpe 2",
        "Aura", "Relic 1", "Relic 2", "Evcil Hayvan"
    )

    private val _sortType = MutableStateFlow(SortType.NAME)
    val sortType: StateFlow<SortType> = _sortType

    fun setSortType(type: SortType) {
        _sortType.value = type
    }

    val uiState: StateFlow<InventoryUiState> = combine(
        gameStateManager.gameState,
        _sortType
    ) { gameState, sortType ->
        val equipmentSlots = allEquipmentSlots.map { slotName ->
            val equippedItem = gameState?.equippedItems?.get(slotName)
            EquipmentSlotState(
                slotName = slotName,
                item = equippedItem?.toItemCard(slotName) // G119: Pass slot name
            )
        }

        val inventoryItems = (gameState?.inventory ?: emptyList()).map { it.toItemCard(null) }.let { items ->
            when (sortType) {
                SortType.NAME -> items.sortedBy { it.name }
                SortType.GEAR_SCORE -> items.sortedByDescending { it.gearScore }
                SortType.RARITY -> items.sortedByDescending { it.rarity.ordinal }
            }
        }
        val gearScore = gameState?.equippedItems?.values?.sumOf { it.weight.toInt() * 100 } ?: 0

        // G135.4: Calculate weight inline (getCurrentWeightString doesn't exist)
        val currentWeight = gameState?.inventory?.sumOf { it.weight.toDouble() } ?: 0.0
        val maxWeight = 50.0f // Default capacity
        val capacityString = "${"%.1f".format(currentWeight)}/${"%.1f".format(maxWeight)} kg"

        InventoryUiState(
            capacity = capacityString,
            currency = Currency(gold = gameState?.playerState?.gold ?: 0),
            gearScore = gearScore.toString(),
            equipmentSlots = equipmentSlots,
            inventoryItems = inventoryItems
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = InventoryUiState()
        )

    fun deleteItem(itemId: String) {
        com.example.isekaikuroshin.utils.GameLogger.logSystem("Item deletion requested: $itemId")
        gameStateManager.removeItemFromInventory(itemId, 1)
    }

    // G135.4: Item action functions

    /**
     * Equip an item to a specific equipment slot
     * @param itemId The inventory item ID
     * @param slotName Turkish slot name (e.g., "Ana El", "Göğüs")
     */
    fun equipItem(itemId: String, slotName: String) {
        com.example.isekaikuroshin.utils.GameLogger.logSystem("🎯 G135.4: Equipping item $itemId to $slotName")
        gameStateManager.equipItemToSlot(itemId, slotName)
    }

    /**
     * Use/consume an item (food, potion, etc.)
     * @param itemId The inventory item ID
     */
    fun useItem(itemId: String) {
        com.example.isekaikuroshin.utils.GameLogger.logSystem("🍽️ G135.4: Using/consuming item $itemId")
        gameStateManager.consumeItem(itemId)
    }

    /**
     * Drop/delete an item from inventory
     * @param itemId The inventory item ID
     */
    fun dropItem(itemId: String) {
        com.example.isekaikuroshin.utils.GameLogger.logSystem("🗑️ G135.4: Dropping item $itemId")
        gameStateManager.removeItemFromInventory(itemId, 1)
    }

    /**
     * Unequip an item from equipment slot back to inventory
     * @param slotName Turkish slot name (e.g., "Ana El", "Göğüs")
     */
    fun unequipItem(slotName: String) {
        com.example.isekaikuroshin.utils.GameLogger.logSystem("🔓 G135.4: Unequipping item from $slotName")
        gameStateManager.unequipItemFromSlot(slotName)
    }

    /**
     * G119: Convert EquippedItemZ6 to CardData.ItemCard
     * @param slotName The equipment slot name from equippedItems map key (e.g., "Ana El", "Göğüs")
     *                 Pass null for inventory items
     */
    private fun EquippedItemZ6.toItemCard(slotName: String?): CardData.ItemCard {
        return CardData.ItemCard(
            id = this.id.toString(),
            name = cleanItemName(this.name), // G119: Clean raw key format
            description = this.description,
            imageRes = R.drawable.icon_kilic_seviye1,
            rarity = CardRarity.valueOf(this.rarity.uppercase()),
            itemType = com.example.isekaikuroshin.data.ItemType.MISC, // TODO: Add type to EquippedItemZ6
            durability = 100,
            gearScore = (this.weight * 100).toInt(),
            statBonuses = this.statBonuses.mapKeys { (key, _) ->
                try {
                    // key is already a String, so we use it directly
                    com.example.isekaikuroshin.ui.components.StatType.valueOf(key)
                } catch (e: IllegalArgumentException) {
                    com.example.isekaikuroshin.ui.components.StatType.STR_FLAT
                }
            },
            equipmentSlot = mapSlotNameToFilterKey(slotName) // G119: Map Turkish slot to English filter key
        )
    }

    /**
     * G119: Map Turkish slot names to English filter keys for InventoryScreen filtering
     * KURAL 11: Metadata-based mapping (slot names are language-specific, filter keys are not)
     */
    private fun mapSlotNameToFilterKey(slotName: String?): String {
        return when (slotName) {
            "Ana El", "Yan El" -> "MAIN_HAND" // Weapons
            "Kafa" -> "HEAD" // Armor
            "Göğüs" -> "CHEST"
            "Bacaklar" -> "LEGS"
            "Eldivenler" -> "GLOVES"
            "Ayakkabılar" -> "BOOTS"
            "Boyun" -> "NECK" // Accessories
            "Yüzük 1", "Yüzük 2" -> "RING_1"
            "Küpe 1", "Küpe 2" -> "EARRING_1"
            "Aura" -> "AURA"
            "Relic 1", "Relic 2" -> "RELIC"
            "Evcil Hayvan" -> "PET"
            else -> "CONSUMABLE" // Default for inventory items or unknown slots
        }
    }

    /**
     * G119: Clean item names - Remove "Item_item_" prefix and format properly
     * Similar to G116 archetype key normalization (KURAL 11)
     * Converts "Item_item_apple" → "Apple", "Item_item_devil_wood" → "Devil Wood"
     */
    private fun cleanItemName(rawName: String): String {
        // Remove common prefixes
        val cleaned = rawName
            .removePrefix("Item_item_")
            .removePrefix("item_")
            .removePrefix("ITEM_")

        // Replace underscores with spaces and title case each word
        return cleaned
            .split("_")
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }
}

// Define InventoryUiState and EquipmentSlotState if they are not already in a separate file.
// For brevity, assuming they exist. Example:
// data class InventoryUiState(
//     val capacity: String = "0/0 kg",
//     val currency: Currency = Currency(),
//     val gearScore: String = "0",
//     val equipmentSlots: List<EquipmentSlotState> = emptyList(),
//     val inventoryItems: List<CardData.ItemCard> = emptyList()
// )
// data class EquipmentSlotState(
//     val slotName: String,
//     val item: CardData.ItemCard? = null
// )
