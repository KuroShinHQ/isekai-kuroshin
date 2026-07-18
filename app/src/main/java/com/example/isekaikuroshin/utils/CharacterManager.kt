package com.example.isekaikuroshin.utils

import android.content.Context
import com.example.isekaikuroshin.data.GameCharacter
import com.example.isekaikuroshin.data.PredefinedCharacters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Character Manager - Handles all character data persistence and management
 */
object CharacterManager {

    private var context: Context? = null
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    // In-memory character database
    private val characters = mutableMapOf<String, GameCharacter>()

    fun initialize(appContext: Context) {
        context = appContext.applicationContext
        GameLogger.logStorySystem("CharacterManager initialized")

        // Load predefined characters
        PredefinedCharacters.ALL_PREDEFINED.forEach { character ->
            characters[character.id] = character
        }

        // Load saved characters
        loadAllCharacters()
    }

    // Get character by ID
    fun getCharacter(id: String): GameCharacter? {
        return characters[id]
    }

    // Get all characters
    fun getAllCharacters(): List<GameCharacter> {
        return try {
            if (context == null) {
                GameLogger.logError("CharacterManager", "Context not initialized", null)
                return emptyList()
            }
            characters.values.toList()
        } catch (e: Exception) {
            GameLogger.logError("CharacterManager", "Error getting all characters", e)
            emptyList()
        }
    }

    // G86: Get characters by location (using locationKey with LanguageManager)
    fun getCharactersAtLocation(location: String): List<GameCharacter> {
        return characters.values.filter {
            if (it.locationKey.isNotEmpty()) {
                com.example.isekaikuroshin.data.LanguageManager.getText(it.locationKey) == location
            } else {
                false
            }
        }
    }

    // Get companions
    fun getCompanions(): List<GameCharacter> {
        return characters.values.filter { it.isCompanion && it.isAlive }
    }

    // G86: Add new character (using nameKey with LanguageManager)
    suspend fun addCharacter(character: GameCharacter): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            characters[character.id] = character
            saveCharacter(character)
            val name = com.example.isekaikuroshin.data.LanguageManager.getText(character.nameKey)
            val surname = if (character.surnameKey.isNotEmpty()) {
                com.example.isekaikuroshin.data.LanguageManager.getText(character.surnameKey)
            } else ""
            GameLogger.logStorySystem("New character added: $name $surname")
            true
        } catch (e: Exception) {
            GameLogger.logError("CHARACTER_SYSTEM", "Failed to add character ${character.id}", e)
            false
        }
    }

    // G86: Update character (using nameKey with LanguageManager)
    suspend fun updateCharacter(character: GameCharacter): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            characters[character.id] = character
            saveCharacter(character)
            val name = com.example.isekaikuroshin.data.LanguageManager.getText(character.nameKey)
            val surname = if (character.surnameKey.isNotEmpty()) {
                com.example.isekaikuroshin.data.LanguageManager.getText(character.surnameKey)
            } else ""
            GameLogger.logStorySystem("Character updated: $name $surname")
            true
        } catch (e: Exception) {
            GameLogger.logError("CHARACTER_SYSTEM", "Failed to update character ${character.id}", e)
            false
        }
    }

    // Add dialogue to character history
    suspend fun addDialogue(characterId: String, dialogue: String): Boolean {
        val character = characters[characterId] ?: return false
        val updatedCharacter = character.copy(
            dialogueHistory = character.dialogueHistory + dialogue
        )
        return updateCharacter(updatedCharacter)
    }

    // G86: Update relationship with character (using nameKey)
    suspend fun updateRelationship(characterId: String, change: Int): Boolean {
        val character = characters[characterId] ?: return false
        val newRelationship = (character.relationship + change).coerceIn(-100, 100)
        val updatedCharacter = character.copy(relationship = newRelationship)
        val name = com.example.isekaikuroshin.data.LanguageManager.getText(character.nameKey)
        GameLogger.logStorySystem("Relationship with $name: ${character.relationship} -> $newRelationship")
        return updateCharacter(updatedCharacter)
    }

    // G86: Move character to new location (locationKey parameter expected)
    // Note: newLocation should be a translation key, not a literal string
    suspend fun moveCharacter(characterId: String, newLocationKey: String): Boolean {
        val character = characters[characterId] ?: return false
        val updatedCharacter = character.copy(locationKey = newLocationKey)
        val name = com.example.isekaikuroshin.data.LanguageManager.getText(character.nameKey)
        val location = com.example.isekaikuroshin.data.LanguageManager.getText(newLocationKey)
        GameLogger.logStorySystem("$name moved to: $location")
        return updateCharacter(updatedCharacter)
    }

    // Generate new character with AI
    fun generateCharacterPrompt(
        name: String = "",
        profession: String = "",
        location: String = "",
        context: String = ""
    ): String {
        return """
            Yeni bir RPG karakteri oluştur:
            İsim: ${name.ifEmpty { "Rastgele bir Türk ismi seç" }}
            Meslek: ${profession.ifEmpty { "Bölgeye uygun bir meslek" }}
            Konum: ${location.ifEmpty { "Mevcut konum" }}
            Bağlam: $context

            Bu karakter için şunları üret:
            - Tam isim (ad ve soyad)
            - Yaş
            - Kısa geçmiş hikayesi (2-3 cümle)
            - Kişilik özellikleri
            - Görünüş
            - Özel yetenekleri
            - İlişki durumu (dostane/düşmanca/nötr)

            JSON formatında yanıt ver.
        """.trimIndent()
    }

    // Save single character
    private suspend fun saveCharacter(character: GameCharacter): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val characterFile = getCharacterFile(character.id)
            val jsonString = json.encodeToString(GameCharacter.serializer(), character)
            characterFile.writeText(jsonString)
            true
        } catch (e: Exception) {
            GameLogger.logError("CHARACTER_SYSTEM", "Failed to save character ${character.id}", e)
            false
        }
    }

    // Load all characters
    private fun loadAllCharacters() {
        try {
            val characterDir = getCharacterDirectory()
            if (!characterDir.exists()) return

            characterDir.listFiles()
                ?.filter { it.name.endsWith(".json") }
                ?.forEach { file ->
                    try {
                        val jsonString = file.readText()
                        val character = json.decodeFromString(GameCharacter.serializer(), jsonString)
                        characters[character.id] = character
                    } catch (e: Exception) {
                        GameLogger.logError("CHARACTER_SYSTEM", "Failed to load character from ${file.name}", e)
                    }
                }

            GameLogger.logStorySystem("Loaded ${characters.size} characters")
        } catch (e: Exception) {
            GameLogger.logError("CHARACTER_SYSTEM", "Failed to load characters", e)
        }
    }

    // Helper functions
    private fun getCharacterDirectory(): File {
        val appContext = context ?: throw IllegalStateException("CharacterManager not initialized")
        val characterDir = File(appContext.filesDir, "characters")
        if (!characterDir.exists()) {
            characterDir.mkdirs()
            GameLogger.logStorySystem("Created characters directory")
        }
        return characterDir
    }

    private fun getCharacterFile(characterId: String): File {
        return File(getCharacterDirectory(), "character_${characterId}.json")
    }

    // G86: Debug: Log all characters (using translation keys)
    fun debugLogAllCharacters() {
        GameLogger.logStorySystem("=== ALL CHARACTERS ===")
        characters.values.forEach { character ->
            val name = com.example.isekaikuroshin.data.LanguageManager.getText(character.nameKey)
            val surname = if (character.surnameKey.isNotEmpty()) {
                com.example.isekaikuroshin.data.LanguageManager.getText(character.surnameKey)
            } else ""
            val profession = if (character.professionKey.isNotEmpty()) {
                com.example.isekaikuroshin.data.LanguageManager.getText(character.professionKey)
            } else ""
            val location = if (character.locationKey.isNotEmpty()) {
                com.example.isekaikuroshin.data.LanguageManager.getText(character.locationKey)
            } else ""
            GameLogger.logStorySystem("$name $surname ($profession) - Location: $location, Relationship: ${character.relationship}")
        }
        GameLogger.logStorySystem("=== END CHARACTERS ===")
    }
}