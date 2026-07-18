package com.example.isekaikuroshin.data

import android.util.Log
import com.example.isekaikuroshin.data.database.SealDao
import com.example.isekaikuroshin.data.database.toEntity
import com.example.isekaikuroshin.data.database.toSeal
import com.example.isekaikuroshin.data.spell.SpellRecipe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Seal data
 *
 * Provides a clean API for accessing seal data from the database.
 * Wraps SealDao and handles Entity <-> Model conversion.
 */
@Singleton
class SealRepository @Inject constructor(
    private val sealDao: SealDao
) {

    /**
     * Get all seals as Flow
     */
    fun getAllSeals(): Flow<List<Seal>> {
        return sealDao.getAllSeals().map { entities ->
            entities.map { it.toSeal() }
        }
    }

    /**
     * Get seal by ID
     */
    suspend fun getSealById(sealId: String): Seal? {
        return sealDao.getSealById(sealId)?.toSeal()
    }

    /**
     * Get seal by ID as Flow (for real-time updates)
     */
    fun getSealByIdFlow(sealId: String): Flow<Seal?> {
        return sealDao.getSealByIdFlow(sealId).map { it?.toSeal() }
    }

    /**
     * Get seals for player level
     */
    suspend fun getSealsForLevel(playerLevel: Int): List<Seal> {
        return sealDao.getSealsForLevel(playerLevel).map { it.toSeal() }
    }

    /**
     * Get seals by difficulty
     */
    suspend fun getSealsByDifficulty(difficulty: SealDifficulty): List<Seal> {
        return sealDao.getSealsByDifficulty(difficulty.name).map { it.toSeal() }
    }

    /**
     * Get seals by tier
     */
    suspend fun getSealsByTier(tier: String): List<Seal> {
        return sealDao.getSealsByTier(tier).map { it.toSeal() }
    }

    /**
     * Get mastered seals
     */
    suspend fun getMasteredSeals(): List<Seal> {
        return sealDao.getMasteredSeals().map { it.toSeal() }
    }

    /**
     * Get seals in progress
     */
    suspend fun getSealsInProgress(): List<Seal> {
        return sealDao.getSealsInProgress().map { it.toSeal() }
    }

    /**
     * Insert or update seal
     */
    suspend fun insertOrUpdate(seal: Seal) {
        sealDao.insertOrUpdate(seal.toEntity())
    }

    /**
     * Insert multiple seals
     */
    suspend fun insertAll(seals: List<Seal>) {
        sealDao.insertAll(seals.map { it.toEntity() })
    }

    /**
     * Update seal
     */
    suspend fun update(seal: Seal) {
        sealDao.update(seal.toEntity())
    }

    /**
     * Update seal (alias for update)
     */
    suspend fun updateSeal(seal: Seal) {
        update(seal)
    }

    /**
     * Delete seal
     */
    suspend fun delete(seal: Seal) {
        sealDao.delete(seal.toEntity())
    }

    /**
     * Delete all seals
     */
    suspend fun deleteAll() {
        sealDao.deleteAll()
    }

    /**
     * Get seal count
     */
    suspend fun getSealCount(): Int {
        return sealDao.getSealCount()
    }

    /**
     * Update seal mastery level
     */
    suspend fun updateMasteryLevel(sealId: String, masteryLevel: Int) {
        sealDao.updateMasteryLevel(sealId, masteryLevel.coerceIn(0, 100))
    }

    /**
     * Update seal practice metrics
     */
    suspend fun updatePracticeMetrics(sealId: String, metrics: PracticeMetrics) {
        val seal = getSealById(sealId) ?: return
        val updated = seal.copy(practiceMetrics = metrics)
        update(updated)
    }

    /**
     * Increment seal mastery by amount
     */
    suspend fun incrementMastery(sealId: String, amount: Int) {
        val seal = getSealById(sealId) ?: return
        val newMastery = (seal.masteryLevel + amount).coerceIn(0, 100)
        updateMasteryLevel(sealId, newMastery)
    }

    // ========================================
    // ⚡⚡⚡ SPELL RECIPE PERSISTENCE
    // ========================================

    companion object {
        private const val TAG = "SealRepository"
        private const val SPELL_RECIPES_KEY = "user_spell_recipes_v1"

        private val json = Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }
    }

    /**
     * ⚡⚡⚡ YENİ: Tüm SpellRecipe'leri getir
     */
    suspend fun getAllSpellRecipes(): List<SpellRecipe> {
        return try {
            val jsonString = PersistentDataManager.getString(SPELL_RECIPES_KEY, "[]")
            if (jsonString.isEmpty() || jsonString == "[]") {
                emptyList()
            } else {
                json.decodeFromString<List<SpellRecipe>>(jsonString)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading SpellRecipes: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * ⚡⚡⚡ YENİ: SpellRecipe kaydet veya güncelle
     */
    suspend fun saveSpellRecipe(spellRecipe: SpellRecipe) {
        try {
            val currentRecipes = getAllSpellRecipes().toMutableList()

            // Aynı ID'ye sahip varsa güncelle, yoksa ekle
            val existingIndex = currentRecipes.indexOfFirst { it.id == spellRecipe.id }
            if (existingIndex >= 0) {
                currentRecipes[existingIndex] = spellRecipe
                Log.d(TAG, "✅ SpellRecipe updated: ${spellRecipe.name} (${spellRecipe.id})")
            } else {
                currentRecipes.add(spellRecipe)
                Log.d(TAG, "✅ SpellRecipe added: ${spellRecipe.name} (${spellRecipe.id})")
            }

            // JSON'a dönüştür ve kaydet
            val jsonString = json.encodeToString(currentRecipes)
            PersistentDataManager.saveString(SPELL_RECIPES_KEY, jsonString)

            Log.d(TAG, "💾 Total SpellRecipes saved: ${currentRecipes.size}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving SpellRecipe: ${e.message}", e)
        }
    }

    /**
     * ⚡⚡⚡ YENİ: SpellRecipe sil
     */
    suspend fun deleteSpellRecipe(spellId: String) {
        try {
            val currentRecipes = getAllSpellRecipes().toMutableList()
            val removed = currentRecipes.removeAll { it.id == spellId }

            if (removed) {
                val jsonString = json.encodeToString(currentRecipes)
                PersistentDataManager.saveString(SPELL_RECIPES_KEY, jsonString)
                Log.d(TAG, "🗑️ SpellRecipe deleted: $spellId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting SpellRecipe: ${e.message}", e)
        }
    }

    /**
     * ⚡⚡⚡ YENİ: Belirli bir SpellRecipe getir
     */
    suspend fun getSpellRecipeById(spellId: String): SpellRecipe? {
        return try {
            getAllSpellRecipes().find { it.id == spellId }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting SpellRecipe: ${e.message}", e)
            null
        }
    }
}
