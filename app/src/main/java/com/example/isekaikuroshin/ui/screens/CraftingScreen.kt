package com.example.isekaikuroshin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.isekaikuroshin.data.rememberLocalizedText
import com.example.isekaikuroshin.data.*
import com.example.isekaikuroshin.ui.crafting.CraftingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CraftingScreen(
    navController: NavController, // TODO-FIX-CAMP-06: NavController eklendi
    viewModel: CraftingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // TODO-FIX-CAMP-06: Scaffold + 9 ikonlu navigation bar eklendi
    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                currentRoute = "crafting",
                selectedItem = com.example.isekaikuroshin.ui.navigation.BottomNavItem.Camp
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0E1A))
                .padding(paddingValues)
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = rememberLocalizedText("craftsmanship"),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = rememberLocalizedText("craft_and_develop"),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    ) {
                        Text(
                            text = "Seviye: ${uiState.playerLevel}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Resources display
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Malzemelerim",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(MaterialType.values()) { material ->
                            ResourceCard(
                                material = material,
                                amount = when (material) {
                                    MaterialType.DAL -> uiState.playerResources.dal
                                    MaterialType.TAS -> uiState.playerResources.tas
                                    MaterialType.ODUN -> uiState.playerResources.odun
                                    MaterialType.DERI -> uiState.playerResources.deri
                                    MaterialType.DEMIR -> uiState.playerResources.demir
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category selector
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                LazyRow(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(CraftingCategory.values()) { category ->
                        CategoryChip(
                            category = category,
                            isSelected = category == uiState.selectedCategory,
                            onClick = { viewModel.onCategorySelected(category) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Recipes list
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = getCategoryDisplayName(uiState.selectedCategory),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val categoryRecipes = viewModel.getRecipesByCategory(uiState.selectedCategory)

                    if (categoryRecipes.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = rememberLocalizedText("no_recipes_in_category"),
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(categoryRecipes) { recipe ->
                                RecipeCard(
                                    recipe = recipe,
                                    canCraft = viewModel.canCraftRecipe(recipe),
                                    missingMaterials = viewModel.getMissingMaterials(recipe),
                                    isSelected = recipe.id == uiState.selectedRecipe?.id,
                                    onClick = {
                                        if (uiState.selectedRecipe?.id == recipe.id) {
                                            viewModel.onRecipeDeselected()
                                        } else {
                                            viewModel.onRecipeSelected(recipe)
                                        }
                                    },
                                    onCraft = {
                                        viewModel.onCraftItem(recipe)

                                        // G83: Log crafting activity
                                        com.example.isekaikuroshin.utils.EventLogger.logCrafting(
                                            itemName = recipe.name,
                                            rarity = recipe.resultItem.rarity,
                                            craftingType = getCategoryDisplayName(recipe.category)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // TODO-FIX-CAMP-06: FAB kaldırıldı - navigation bar ile değiştirildi

        // Recipe details dialog
        uiState.selectedRecipe?.let { recipe ->
            RecipeDetailsDialog(
                recipe = recipe,
                canCraft = viewModel.canCraftRecipe(recipe),
                missingMaterials = viewModel.getMissingMaterials(recipe),
                playerLevel = uiState.playerLevel,
                onDismiss = viewModel::onRecipeDeselected,
                onCraft = {
                    viewModel.onCraftItem(recipe)
                    viewModel.onRecipeDeselected()

                    // G83: Log crafting activity
                    com.example.isekaikuroshin.utils.EventLogger.logCrafting(
                        itemName = recipe.name,
                        rarity = recipe.resultItem.rarity,
                        craftingType = getCategoryDisplayName(recipe.category)
                    )
                }
            )
        }

        // Crafting progress overlay
        if (uiState.isCrafting) {
            CraftingProgressOverlay(
                progress = uiState.craftingProgress
            )
        }
        } // TODO-FIX-CAMP-06: Closing brace for Box
    } // TODO-FIX-CAMP-06: Closing brace for Scaffold
}

// TODO-FIX-CAMP-06: 9 ikonlu navigation bar component (NavController tabanlı)
@Composable
private fun BottomNavigationBar(
    navController: NavController,
    currentRoute: String,
    selectedItem: com.example.isekaikuroshin.ui.navigation.BottomNavItem
) {
    val items = listOf(
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.Status,
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.Inventory,
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.Quests,
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.Catalog,
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.Camp,
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.Map,
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.Journal,
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.HealthHub,
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.Settings
    )

    NavigationBar {
        items.forEach { item ->
            val localizedTitle = com.example.isekaikuroshin.data.rememberLocalizedText(item.titleKey)
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = localizedTitle) },
                label = { Text(text = localizedTitle) },
                selected = item == selectedItem,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo("main") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
private fun ResourceCard(
    material: MaterialType,
    amount: Int
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = getMaterialIcon(material),
                contentDescription = material.displayName,
                tint = getMaterialColor(material),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = material.displayName,
                fontSize = 11.sp,
                color = Color.Gray
            )

            Text(
                text = amount.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun CategoryChip(
    category: CraftingCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = getCategoryDisplayName(category),
                fontSize = 12.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = getCategoryIcon(category),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = Color.White,
            selectedLeadingIconColor = Color.White
        )
    )
}

@Composable
private fun RecipeCard(
    recipe: CraftingRecipe,
    canCraft: Boolean,
    missingMaterials: Map<String, Int>,
    isSelected: Boolean,
    onClick: () -> Unit,
    onCraft: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder() else null
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recipe.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = recipe.description,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (canCraft) {
                    IconButton(
                        onClick = onCraft,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                Color(0xFF4CAF50),
                                RoundedCornerShape(18.dp)
                            )
                    ) {
                        Icon(
                            Icons.Filled.Build,
                            contentDescription = "Üret",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Required materials
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(recipe.requiredMaterials.entries.toList()) { (materialKey, amount) ->
                    val materialType = MaterialType.values().find { it.resourceKey == materialKey }
                    if (materialType != null) {
                        val isMissing = missingMaterials.containsKey(materialKey)
                        RequiredMaterialChip(
                            material = materialType,
                            requiredAmount = amount,
                            isMissing = isMissing
                        )
                    }
                }
            }

            if (!canCraft && missingMaterials.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Eksik malzemeler var",
                    fontSize = 11.sp,
                    color = Color(0xFFFF5722)
                )
            }
        }
    }
}

@Composable
private fun RequiredMaterialChip(
    material: MaterialType,
    requiredAmount: Int,
    isMissing: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isMissing)
                Color(0xFFFF5722).copy(alpha = 0.2f) else
                Color(0xFF4CAF50).copy(alpha = 0.2f)
        ),
        modifier = Modifier.clip(RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = getMaterialIcon(material),
                contentDescription = null,
                tint = if (isMissing) Color(0xFFFF5722) else Color(0xFF4CAF50),
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = requiredAmount.toString(),
                fontSize = 10.sp,
                color = if (isMissing) Color(0xFFFF5722) else Color(0xFF4CAF50),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RecipeDetailsDialog(
    recipe: CraftingRecipe,
    canCraft: Boolean,
    missingMaterials: Map<String, Int>,
    playerLevel: Int,
    onDismiss: () -> Unit,
    onCraft: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Recipe header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = getCategoryIcon(recipe.category),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = recipe.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = getCategoryDisplayName(recipe.category),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                Text(
                    text = recipe.description,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Recipe stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RecipeStatCard(
                        label = "Gereken Seviye",
                        value = "${recipe.requiredLevel}",
                        color = if (playerLevel >= recipe.requiredLevel)
                            Color(0xFF4CAF50) else Color(0xFFFF5722),
                        modifier = Modifier.weight(1f)
                    )
                    RecipeStatCard(
                        label = "Süre",
                        value = "${recipe.craftingTime}dk",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Required materials
                Text(
                    text = "Gerekli Malzemeler",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                recipe.requiredMaterials.entries.forEach { (materialKey, amount) ->
                    val materialType = MaterialType.values().find { it.resourceKey == materialKey }
                    if (materialType != null) {
                        val isMissing = missingMaterials.containsKey(materialKey)
                        val missingAmount = missingMaterials[materialKey] ?: 0

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = getMaterialIcon(materialType),
                                    contentDescription = null,
                                    tint = getMaterialColor(materialType),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = materialType.displayName,
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }

                            Text(
                                text = if (isMissing) {
                                    "$amount (${missingAmount} eksik)"
                                } else {
                                    amount.toString()
                                },
                                color = if (isMissing) Color(0xFFFF5722) else Color(0xFF4CAF50),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Kapat")
                    }

                    Button(
                        onClick = onCraft,
                        enabled = canCraft,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
                            disabledContainerColor = Color.Gray
                        )
                    ) {
                        Text(
                            text = if (canCraft) "Üret" else "Üretilemiyor",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeStatCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CraftingProgressOverlay(
    progress: Float
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = rememberLocalizedText("crafting_in_progress"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF4CAF50)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

private fun getMaterialIcon(material: MaterialType): ImageVector {
    return when (material) {
        MaterialType.DAL -> Icons.Filled.Grass
        MaterialType.TAS -> Icons.Filled.Landscape
        MaterialType.ODUN -> Icons.Filled.Forest
        MaterialType.DERI -> Icons.Filled.Pets
        MaterialType.DEMIR -> Icons.Filled.Hardware
    }
}

private fun getMaterialColor(material: MaterialType): Color {
    return when (material) {
        MaterialType.DAL -> Color(0xFF8BC34A)
        MaterialType.TAS -> Color(0xFF9E9E9E)
        MaterialType.ODUN -> Color(0xFF795548)
        MaterialType.DERI -> Color(0xFFFF9800)
        MaterialType.DEMIR -> Color(0xFF607D8B)
    }
}

private fun getCategoryIcon(category: CraftingCategory): ImageVector {
    return when (category) {
        CraftingCategory.BASIC -> Icons.Filled.Home
        CraftingCategory.WEAPONS -> Icons.Filled.Security
        CraftingCategory.ARMOR -> Icons.Filled.Shield
        CraftingCategory.TOOLS -> Icons.Filled.Build
        CraftingCategory.POTIONS -> Icons.Filled.Science
        CraftingCategory.SPECIAL -> Icons.Filled.Star
    }
}

private fun getCategoryDisplayName(category: CraftingCategory): String {
    return when (category) {
        CraftingCategory.BASIC -> "Temel"
        CraftingCategory.WEAPONS -> "Silahlar"
        CraftingCategory.ARMOR -> "Zırhlar"
        CraftingCategory.TOOLS -> "Aletler"
        CraftingCategory.POTIONS -> "İksirler"
        CraftingCategory.SPECIAL -> "Özel"
    }
}