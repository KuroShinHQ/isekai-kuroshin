package com.example.isekaikuroshin.ui.crafting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.isekaikuroshin.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class CraftingUiState(
    val availableRecipes: List<CraftingRecipe> = emptyList(),
    val playerResources: ResourcesZ4 = ResourcesZ4(),
    val inventory: List<EquippedItemZ6> = emptyList(),
    val playerLevel: Int = 1,
    val selectedCategory: CraftingCategory = CraftingCategory.BASIC,
    val selectedRecipe: CraftingRecipe? = null,
    val isCrafting: Boolean = false,
    val craftingProgress: Float = 0f
)

@HiltViewModel
class CraftingViewModel @Inject constructor(
    private val gameStateManager: GameStateManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CraftingUiState())
    val uiState: StateFlow<CraftingUiState> = _uiState.asStateFlow()

    init {
        // Load all available recipes
        _uiState.update { it.copy(availableRecipes = CraftingRecipes.getAllRecipes()) }

        // Subscribe to game state changes
        gameStateManager.gameState
            .onEach { gameState ->
                _uiState.update { currentState ->
                    currentState.copy(
                        playerResources = gameState.resources,
                        inventory = gameState.inventory,
                        playerLevel = gameState.playerState.level
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onCategorySelected(category: CraftingCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun onRecipeSelected(recipe: CraftingRecipe) {
        _uiState.update { it.copy(selectedRecipe = recipe) }
    }

    fun onRecipeDeselected() {
        _uiState.update { it.copy(selectedRecipe = null) }
    }

    fun canCraftRecipe(recipe: CraftingRecipe): Boolean {
        val currentState = _uiState.value

        // Check level requirement
        if (currentState.playerLevel < recipe.requiredLevel) {
            return false
        }

        // Check if player has required materials
        return recipe.requiredMaterials.all { (materialKey, requiredAmount) ->
            getPlayerMaterialAmount(materialKey, currentState.playerResources) >= requiredAmount
        }
    }

    private fun getPlayerMaterialAmount(materialKey: String, resources: ResourcesZ4): Int {
        return when (materialKey) {
            "dal" -> resources.dal
            "tas" -> resources.tas
            "odun" -> resources.odun
            "deri" -> resources.deri
            "demir" -> resources.demir
            else -> 0
        }
    }

    fun onCraftItem(recipe: CraftingRecipe) {
        if (!canCraftRecipe(recipe) || _uiState.value.isCrafting) {
            return
        }

        // Start crafting process
        _uiState.update { it.copy(isCrafting = true, craftingProgress = 0f) }

        // Here we would integrate with GameStateManager to:
        // 1. Consume resources
        // 2. Add crafted item to inventory
        // 3. Add experience
        // 4. Update game state

        // For now, this is a placeholder following the unidirectional data flow pattern
        // Example implementation:
        // gameStateManager.craftItem(recipe)

        // G83: Log crafting to Journal
        com.example.isekaikuroshin.utils.EventLogger.logGenericActivity(
            "I crafted '${recipe.resultItem.name}' using ${recipe.requiredMaterials.size} material(s)"
        )

        // Simulate crafting completion (in real implementation this would be handled by backend)
        _uiState.update { it.copy(isCrafting = false, craftingProgress = 1f, selectedRecipe = null) }
    }

    fun getRecipesByCategory(category: CraftingCategory): List<CraftingRecipe> {
        return _uiState.value.availableRecipes.filter { it.category == category }
    }

    fun getMissingMaterials(recipe: CraftingRecipe): Map<String, Int> {
        val currentResources = _uiState.value.playerResources
        val missingMaterials = mutableMapOf<String, Int>()

        recipe.requiredMaterials.forEach { (materialKey, requiredAmount) ->
            val playerAmount = getPlayerMaterialAmount(materialKey, currentResources)
            if (playerAmount < requiredAmount) {
                missingMaterials[materialKey] = requiredAmount - playerAmount
            }
        }

        return missingMaterials
    }
}