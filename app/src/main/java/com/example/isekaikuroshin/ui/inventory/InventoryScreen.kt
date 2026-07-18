package com.example.isekaikuroshin.ui.inventory

import androidx.compose.animation.core.*
import com.example.isekaikuroshin.data.Currency
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.isekaikuroshin.ui.components.StandardCard
import com.example.isekaikuroshin.ui.components.CardData
import com.example.isekaikuroshin.ui.components.CardRarity
import com.example.isekaikuroshin.ui.components.StatType
import com.example.isekaikuroshin.ui.theme.IsekaiKuroshinTheme
import com.example.isekaikuroshin.data.rememberLocalizedText
import androidx.compose.ui.graphics.graphicsLayer // G135.4: For arrow rotation


// Filter types for inventory
enum class InventoryFilter(val displayNameKey: String) {
    ALL("all"),
    WEAPONS("weapons"),
    ARMOR("armor"),
    ACCESSORIES("accessories"),
    CONSUMABLES("consumables")
}

@Composable
fun InventoryScreen(
    modifier: Modifier = Modifier,
    viewModel: InventoryViewModel = hiltViewModel(),
    onBackPressed: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sortType by viewModel.sortType.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf(InventoryFilter.ALL) }
    var itemToDelete by remember { mutableStateOf<CardData.ItemCard?>(null) }

    // G135.4: Item context menu states
    var selectedItem by remember { mutableStateOf<CardData.ItemCard?>(null) }
    var showItemActionsDialog by remember { mutableStateOf(false) }
    var showSlotSelectionDialog by remember { mutableStateOf(false) }

    // Drag and Drop States
    var draggedItem by remember { mutableStateOf<CardData.ItemCard?>(null) }
    var currentPointerPosition by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    var initialTouchOffset by remember { mutableStateOf(Offset.Zero) }

    // Equipment slot bounds for drop detection
    val equipmentSlotBounds = remember { mutableStateMapOf<String, Rect>() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput("drag_detection") {
                detectDragGestures(
                    onDragStart = { _ ->
                        if (draggedItem != null) {
                            isDragging = true
                        }
                    },
                    onDragEnd = {
                        draggedItem?.let { item ->
                            if (isDragging) {
                                // Check if dropped on equipment slot
                                equipmentSlotBounds.forEach { (slotName, rect) ->
                                    if (rect.contains(currentPointerPosition)) {
                                        // Handle equipment - you can add logic here
                                        println("🎯 Item ${item.name} dropped on $slotName")
                                    }
                                }
                            }
                        }
                        // Reset drag state
                        draggedItem = null
                        isDragging = false
                        currentPointerPosition = Offset.Zero
                        initialTouchOffset = Offset.Zero
                    },
                    onDrag = { change, _ ->
                        currentPointerPosition = change.position
                    }
                )
            }
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
        // Top Info Panel
        TopInfoPanel(
            capacity = uiState.capacity,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            currency = uiState.currency
        )

        // Sort Panel
        SortPanel(
            currentSort = sortType,
            onSortChange = { viewModel.setSortType(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Main Content Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Column - Equipment Slots
            EquipmentSlotsPanel(
                slots = uiState.equipmentSlots,
                draggedItem = draggedItem,
                isDragging = isDragging,
                onSlotPositioned = { slotName, rect ->
                    equipmentSlotBounds[slotName] = rect
                },
                onSlotClick = { slotName, item ->
                    // G135.4: Click equipped item to unequip
                    if (item != null) {
                        viewModel.unequipItem(slotName)
                    }
                },
                modifier = Modifier.weight(0.4f)
            )

            // Right Column - Inventory Items
            InventoryItemsPanel(
                items = uiState.inventoryItems,
                selectedFilter = selectedFilter,
                onFilterChanged = { selectedFilter = it },
                onItemDragStart = { item, offset ->
                    draggedItem = item
                    initialTouchOffset = offset
                },
                onItemLongClick = { item ->
                    // G135.4: Open context menu instead of delete dialog
                    selectedItem = item
                    showItemActionsDialog = true
                },
                modifier = Modifier.weight(0.6f)
            )
        }

        // Bottom Info Panel
        BottomInfoPanel(
            gearScore = uiState.gearScore,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        // Dragged item overlay
        draggedItem?.let { item ->
            if (isDragging) {
                DraggedItemOverlay(
                    item = item,
                    offset = currentPointerPosition - initialTouchOffset,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Delete confirmation dialog
        itemToDelete?.let { item ->
            AlertDialog(
                onDismissRequest = { itemToDelete = null },
                title = { Text(rememberLocalizedText("delete_item"), color = MaterialTheme.colorScheme.error) },
                text = {
                    Text(
                        "\"${item.name}\" ${rememberLocalizedText("delete_item_confirm")}",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteItem(item.id)
                        itemToDelete = null
                    }) {
                        Text(rememberLocalizedText("delete"), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemToDelete = null }) {
                        Text(rememberLocalizedText("cancel"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface
            )
        }

        // G135.4: Item Actions Dialog (Context Menu)
        if (showItemActionsDialog && selectedItem != null) {
            ItemActionsDialog(
                item = selectedItem!!,
                onDismiss = {
                    showItemActionsDialog = false
                    selectedItem = null
                },
                onEquipClick = {
                    showItemActionsDialog = false
                    showSlotSelectionDialog = true
                },
                onUseClick = {
                    viewModel.useItem(selectedItem!!.id)
                    showItemActionsDialog = false
                    selectedItem = null
                },
                onDropClick = {
                    viewModel.dropItem(selectedItem!!.id)
                    showItemActionsDialog = false
                    selectedItem = null
                }
            )
        }

        // G135.4: Equipment Slot Selection Dialog
        if (showSlotSelectionDialog && selectedItem != null) {
            val allSlots = listOf(
                "Kafa", "Boyun", "Göğüs", "Bacaklar", "Eldivenler", "Ayakkabılar",
                "Ana El", "Yan El", "Yüzük 1", "Yüzük 2", "Küpe 1", "Küpe 2",
                "Aura", "Relic 1", "Relic 2", "Evcil Hayvan"
            )
            SlotSelectionDialog(
                item = selectedItem!!,
                availableSlots = allSlots,
                onDismiss = {
                    showSlotSelectionDialog = false
                    selectedItem = null
                },
                onSlotSelected = { slotName ->
                    viewModel.equipItem(selectedItem!!.id, slotName)
                    showSlotSelectionDialog = false
                    selectedItem = null
                }
            )
        }

        }
    }
}

@Composable
private fun DraggedItemOverlay(
    item: CardData.ItemCard,
    offset: Offset,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = offset.x.roundToInt(),
                        y = offset.y.roundToInt()
                    )
                }
                .size(80.dp)
                .alpha(0.8f),
            colors = CardDefaults.cardColors(
                containerColor = item.rarity.color.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = getEquipmentTypeIcon(item.name),
                        contentDescription = item.name,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.name.take(6),
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun TopInfoPanel(
    capacity: String,
    modifier: Modifier = Modifier,
    currency: Currency = Currency()
) {
    val infiniteTransition = rememberInfiniteTransition(label = "top_panel_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = modifier
            .height(48.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = rememberLocalizedText("inventory_title"),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )

                Text(
                    text = "• $capacity",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }

            // Currency Display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "💰",
                    fontSize = 16.sp
                )
                Text(
                    text = currency.toDisplayString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = rememberLocalizedText("currency"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun BottomInfoPanel(
    gearScore: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bottom_panel_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = modifier
            .height(48.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "GS: $gearScore",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun EquipmentSlotsPanel(
    slots: List<EquipmentSlotState>,
    draggedItem: CardData.ItemCard?,
    isDragging: Boolean,
    onSlotPositioned: (String, Rect) -> Unit,
    onSlotClick: (String, CardData.ItemCard?) -> Unit = { _, _ -> }, // G135.4: Click handler
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(slots) { slot ->
            CompactEquipmentSlot(
                slotName = slot.slotName,
                equippedItem = slot.item,
                draggedItem = draggedItem,
                isDragging = isDragging,
                onSlotPositioned = onSlotPositioned,
                onClick = { onSlotClick(slot.slotName, slot.item) } // G135.4: Pass click handler
            )
        }
    }
}

@Composable
private fun CompactEquipmentSlot(
    slotName: String,
    equippedItem: CardData.ItemCard?,
    draggedItem: CardData.ItemCard?,
    isDragging: Boolean,
    onSlotPositioned: (String, Rect) -> Unit,
    onClick: () -> Unit = {}, // G135.4: Click handler
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "compact_slot_glow")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_alpha"
    )

    val isValidDropTarget = isDragging && draggedItem != null && canEquipToSlot(draggedItem, slotName)

    Box(
        modifier = modifier
            .size(64.dp) // Çok daha küçük
            .onGloballyPositioned { coordinates ->
                onSlotPositioned(slotName, coordinates.boundsInRoot())
            }
            .clickable(enabled = equippedItem != null) { onClick() } // G135.4: Click to unequip
            .background(
                color = if (equippedItem != null)
                    equippedItem.rarity.color.copy(alpha = 0.1f)
                else if (isValidDropTarget)
                    Color.Green.copy(alpha = 0.2f)
                else
                    MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = if (isValidDropTarget) 2.dp else 1.dp,
                color = if (equippedItem != null)
                    equippedItem.rarity.color.copy(alpha = 0.6f)
                else if (isValidDropTarget)
                    Color.Green.copy(alpha = 0.8f)
                else
                    MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha),
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (equippedItem != null) {
            // G135.4 FIX: Show equipped item details, not just slot icon
            Column(
                modifier = Modifier.fillMaxSize().padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Item icon with rarity color
                Icon(
                    imageVector = getEquipmentTypeIcon(equippedItem.name),
                    contentDescription = equippedItem.name,
                    tint = equippedItem.rarity.color.copy(alpha = 0.9f),
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Item name (first 6 chars)
                Text(
                    text = equippedItem.name.take(6),
                    fontSize = 7.sp,
                    color = equippedItem.rarity.color.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            // Empty slot - show slot icon and name
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = getSlotIcon(slotName),
                    contentDescription = slotName,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Slot ismi
                Text(
                    text = rememberLocalizedText(getSlotLocalizationKey(slotName)),
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EquipmentSlot(
    slotName: String,
    equippedItem: CardData.ItemCard?,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "slot_glow")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_alpha"
    )

    if (equippedItem != null) {
        // Equipped slot with miniature StandardCard using equipment type icon
        Box(
            modifier = modifier.height(120.dp)
        ) {
            StandardCard(
                data = equippedItem,
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        // Empty slot
        Box(
            modifier = modifier
                .height(120.dp)
                .background(
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = getSlotIcon(slotName),
                    contentDescription = slotName,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = slotName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * G119: Filter buttons for inventory categories
 */
@Composable
private fun FilterButtons(
    selectedFilter: InventoryFilter,
    onFilterChanged: (InventoryFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        InventoryFilter.entries.forEach { filter ->
            val isSelected = filter == selectedFilter
            val displayText = rememberLocalizedText(filter.displayNameKey)

            OutlinedButton(
                onClick = { onFilterChanged(filter) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    } else {
                        Color.Transparent
                    },
                    contentColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    }
                ),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    }
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
            ) {
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun InventoryItemsPanel(
    items: List<CardData.ItemCard>,
    selectedFilter: InventoryFilter,
    onFilterChanged: (InventoryFilter) -> Unit,
    onItemDragStart: (CardData.ItemCard, Offset) -> Unit,
    onItemLongClick: (CardData.ItemCard) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // G119: Apply filter to items
    val filteredItems = remember(items, selectedFilter) {
        when (selectedFilter) {
            InventoryFilter.ALL -> items
            InventoryFilter.WEAPONS -> items.filter {
                it.equipmentSlot in listOf("MAIN_HAND", "OFF_HAND")
            }
            InventoryFilter.ARMOR -> items.filter {
                it.equipmentSlot in listOf("HEAD", "CHEST", "LEGS", "GLOVES", "BOOTS")
            }
            InventoryFilter.ACCESSORIES -> items.filter {
                it.equipmentSlot in listOf("NECK", "RING_1", "RING_2", "EARRING_1", "EARRING_2", "AURA")
            }
            InventoryFilter.CONSUMABLES -> items.filter {
                it.equipmentSlot == "CONSUMABLE" || it.equipmentSlot == null || it.equipmentSlot.isEmpty()
            }
        }
    }

    Column(modifier = modifier) {
        // Filter buttons
        FilterButtons(
            selectedFilter = selectedFilter,
            onFilterChanged = onFilterChanged,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(filteredItems) { item ->
                StandardCard(
                    data = item,
                    onLongClick = { onItemLongClick(item) },
                    modifier = Modifier.fillMaxWidth()
                    // Drag removed - conflicts with long click
                )
            }
        }
    }
}

@Composable
private fun SortPanel(
    currentSort: SortType,
    onSortChange: (SortType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = rememberLocalizedText("sort_by"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            fontSize = 12.sp
        )

        SortType.entries.forEach { sortType ->
            SortButton(
                sortType = sortType,
                isSelected = currentSort == sortType,
                onClick = { onSortChange(sortType) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SortButton(
    sortType: SortType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayName = when (sortType) {
        SortType.NAME -> rememberLocalizedText("name")
        SortType.GEAR_SCORE -> rememberLocalizedText("power")
        SortType.RARITY -> rememberLocalizedText("rarity")
    }

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
            contentColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isSelected) 1f else 0.6f)
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = if (isSelected) 0.8f else 0.3f)
        ),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier.height(32.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = displayName,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// G119: FilterButtons removed - duplicate definition (new version at line 601)

@Composable
private fun FilterButton(
    filter: InventoryFilter,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.6f,
        animationSpec = tween(200),
        label = "filter_alpha"
    )

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
            contentColor = MaterialTheme.colorScheme.primary.copy(alpha = animatedAlpha)
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = if (isSelected) 0.8f else 0.4f)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.height(36.dp)
    ) {
        Text(
            text = rememberLocalizedText(filter.displayNameKey),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Helper function to get equipment type icon resource based on item type
private fun getEquipmentTypeIcon(itemName: String): ImageVector {
    val lowerCaseName = itemName.lowercase()
    return when {
        lowerCaseName.contains("sword") || lowerCaseName.contains("kilic") || lowerCaseName.contains("kılıç") -> Icons.Default.Build
        lowerCaseName.contains("helmet") || lowerCaseName.contains("migfer") || lowerCaseName.contains("kafa") -> Icons.Default.Security
        lowerCaseName.contains("dagger") || lowerCaseName.contains("hancer") || lowerCaseName.contains("hançer") -> Icons.Default.Build
        lowerCaseName.contains("bow") || lowerCaseName.contains("yay") -> Icons.Default.SportsEsports
        lowerCaseName.contains("potion") || lowerCaseName.contains("iksir") || lowerCaseName.contains("şişe") -> Icons.Default.LocalDrink
        lowerCaseName.contains("armor") || lowerCaseName.contains("zırh") -> Icons.Default.Shield
        else -> Icons.Default.Build // Default to build icon
    }
}

// Helper function to convert Turkish slot names to localization keys
private fun getSlotLocalizationKey(slotName: String): String {
    return when (slotName) {
        "Kafa" -> "slot_head"
        "Boyun" -> "slot_neck"
        "Göğüs" -> "slot_chest"
        "Bacaklar" -> "slot_legs"
        "Eldivenler" -> "slot_gloves"
        "Ayakkabılar" -> "slot_boots"
        "Ana El" -> "slot_main_hand"
        "Yan El" -> "slot_off_hand"
        "Yüzük 1" -> "slot_ring_1"
        "Yüzük 2" -> "slot_ring_2"
        "Küpe 1" -> "slot_earring_1"
        "Küpe 2" -> "slot_earring_2"
        "Aura" -> "slot_aura"
        "Relic 1" -> "slot_relic_1"
        "Relic 2" -> "slot_relic_2"
        "Evcil Hayvan" -> "slot_pet"
        else -> slotName // Fallback to original if no mapping
    }
}

// Helper function to get appropriate icon for equipment slots
private fun getSlotIcon(slotName: String): ImageVector {
    return when (slotName) {
        "Kafa" -> Icons.Default.Security
        "Boyun" -> Icons.Default.Circle
        "Göğüs" -> Icons.Default.Shield
        "Bacaklar" -> Icons.Default.Straighten
        "Eldivenler" -> Icons.Default.PanTool
        "Ayakkabılar" -> Icons.AutoMirrored.Filled.DirectionsWalk
        "Ana El", "Yan El" -> Icons.Default.Build
        "Yüzük 1", "Yüzük 2" -> Icons.Default.Circle
        "Küpe 1", "Küpe 2" -> Icons.Default.Brightness1
        "Aura" -> Icons.Default.Flare
        "Relic 1", "Relic 2" -> Icons.Default.AutoAwesome
        "Evcil Hayvan" -> Icons.Default.Pets
        else -> Icons.Default.Category
    }
}

/**
 * G80: Equipment slot matching using metadata instead of hardcoded strings (KURAL 9 FIX)
 * Maps localized UI slot names to equipment slot codes from GMItemGained
 */
private fun canEquipToSlot(item: CardData.ItemCard, slotName: String): Boolean {
    // Map Turkish UI slot names to equipment slot codes
    val expectedSlot = when (slotName) {
        "Ana El" -> "MAIN_HAND"
        "Yan El" -> "OFF_HAND"
        "Kafa" -> "HEAD"
        "Göğüs" -> "CHEST"
        "Bacaklar" -> "LEGS"
        "Eldivenler" -> "GLOVES"
        "Ayakkabılar" -> "BOOTS"
        "Yüzük 1", "Yüzük 2" -> listOf("RING_1", "RING_2")
        "Küpe 1", "Küpe 2" -> listOf("EARRING_1", "EARRING_2")
        "Boyun" -> "NECK"
        "Aura" -> "AURA"
        "Relic 1", "Relic 2" -> listOf("RELIC_1", "RELIC_2")
        "Evcil Hayvan" -> "PET"
        else -> return false
    }

    // Check if item's equipment slot matches expected slot
    return if (expectedSlot is List<*>) {
        expectedSlot.contains(item.equipmentSlot)
    } else {
        item.equipmentSlot == expectedSlot
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0F1A)
@Composable
private fun InventoryScreenPreview() {
    IsekaiKuroshinTheme {
        InventoryScreen()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0F1A, widthDp = 400, heightDp = 200)
@Composable
private fun EquipmentSlotPreview() {
    IsekaiKuroshinTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Empty slot
            EquipmentSlot(
                slotName = "Kafa",
                equippedItem = null,
                modifier = Modifier.fillMaxWidth()
            )

            // Equipped slot
            EquipmentSlot(
                slotName = "Ana El",
                equippedItem = CardData.ItemCard(
                    id = "sample_sword",
                    name = "Test Kılıcı",
                    description = "Test için örnek kılıç",
                    imageRes = android.R.drawable.star_on,
                    rarity = CardRarity.RARE,
                    durability = 90,
                    gearScore = 150,
                    statBonuses = mapOf(StatType.STR_PERCENT to 0.10f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// --- Sub-components for Inventory Screen ---

// Old components removed - replaced with new StandardCard-based design

/**
 * G135.4: Item Actions Dialog (Context Menu)
 * Shows available actions based on item type
 */
@Composable
private fun ItemActionsDialog(
    item: CardData.ItemCard,
    onDismiss: () -> Unit,
    onEquipClick: () -> Unit,
    onUseClick: () -> Unit,
    onDropClick: () -> Unit
) {
    // KURAL 11: Determine actions based on item metadata (equipmentSlot), not name
    val isEquippable = item.equipmentSlot !in listOf("CONSUMABLE", "")
    val isConsumable = item.equipmentSlot == "CONSUMABLE" || item.equipmentSlot.isEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = rememberLocalizedText("item_actions"),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Equip button (WEAPON/ARMOR only)
                if (isEquippable) {
                    ItemActionButton(
                        icon = Icons.Default.CheckCircle,
                        text = rememberLocalizedText("equip"),
                        color = MaterialTheme.colorScheme.primary,
                        onClick = {
                            onEquipClick()
                        }
                    )
                }

                // Use button (CONSUMABLE only)
                if (isConsumable) {
                    ItemActionButton(
                        icon = Icons.Default.LocalDrink,
                        text = rememberLocalizedText("use"),
                        color = MaterialTheme.colorScheme.tertiary,
                        onClick = {
                            onUseClick()
                        }
                    )
                }

                // Drop button (ALL items)
                ItemActionButton(
                    icon = Icons.Default.Delete,
                    text = rememberLocalizedText("drop"),
                    color = MaterialTheme.colorScheme.error,
                    onClick = {
                        onDropClick()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = rememberLocalizedText("cancel"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * G135.4: Item Action Button (Context Menu Option)
 */
@Composable
private fun ItemActionButton(
    icon: ImageVector,
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = color.copy(alpha = 0.1f),
            contentColor = color
        ),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

/**
 * G135.4: Slot Selection Dialog
 * Shows available equipment slots for equipping an item
 */
@Composable
private fun SlotSelectionDialog(
    item: CardData.ItemCard,
    availableSlots: List<String>,
    onDismiss: () -> Unit,
    onSlotSelected: (String) -> Unit
) {
    // KURAL 11: Filter slots based on item's equipmentSlot metadata
    val compatibleSlots = availableSlots.filter { slotName ->
        canEquipToSlot(item, slotName)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = rememberLocalizedText("select_equipment_slot"),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        },
        text = {
            if (compatibleSlots.isEmpty()) {
                Text(
                    text = rememberLocalizedText("no_compatible_slots"),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(compatibleSlots) { slotName ->
                        SlotSelectionButton(
                            slotName = slotName,
                            onClick = { onSlotSelected(slotName) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = rememberLocalizedText("cancel"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * G135.4: Slot Selection Button
 */
@Composable
private fun SlotSelectionButton(
    slotName: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            contentColor = MaterialTheme.colorScheme.primary
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getSlotIcon(slotName),
                    contentDescription = slotName,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = rememberLocalizedText(getSlotLocalizationKey(slotName)),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Select",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer(rotationZ = 180f)
            )
        }
    }
}