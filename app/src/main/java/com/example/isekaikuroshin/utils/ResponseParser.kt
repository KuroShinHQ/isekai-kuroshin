package com.example.isekaikuroshin.utils

import kotlinx.serialization.json.*

object ResponseParser {

    data class ParsedResponse(
        val storyText: String,
        val newCharacter: CharacterInfo? = null,
        val questUpdate: QuestInfo? = null,
        val statusUpdate: StatusInfo? = null,
        val environmentUpdate: EnvironmentInfo? = null
    )

    data class CharacterInfo(
        val name: String,
        val age: Int = 0,
        val personality: String = "",
        val appearance: String = "",
        val relationship: Float = 0.0f
    )

    data class QuestInfo(
        val id: String,
        val title: String,
        val description: String,
        val giver: String,
        val progress: Float = 0.0f
    )

    data class StatusInfo(
        val healthChange: Long = 0,
        val manaChange: Long = 0,
        val staminaChange: Long = 0,
        val statChanges: Map<String, Long> = emptyMap()
    )

    data class EnvironmentInfo(
        val location: String? = null,
        val weather: String? = null,
        val timeChange: Boolean = false
    )

    fun parseAIResponse(response: String, hideJSON: Boolean = true): ParsedResponse {
        if (!hideJSON) {
            // If JSON hiding is disabled, return the raw response
            return ParsedResponse(storyText = response)
        }

        return try {
            val cleanedResponse = cleanResponseText(response)
            val extractedData = extractStructuredData(response)

            val storyText = if (extractedData.newCharacter != null) {
                val charInfo = extractedData.newCharacter
                val ageText = if (charInfo.age > 0) " (${charInfo.age})" else ""
                val personalityText = if (charInfo.personality.isNotEmpty()) ", ${charInfo.personality}" else ""

                "${cleanedResponse}\n\nNew character: ${charInfo.name}${ageText}${personalityText}"
            } else {
                cleanedResponse
            }

            extractedData.copy(storyText = storyText)
        } catch (e: Exception) {
            // If parsing fails, return cleaned text
            ParsedResponse(storyText = cleanResponseText(response))
        }
    }

    private fun cleanResponseText(response: String): String {
        var cleaned = response

        // Remove JSON blocks (anything between { and })
        cleaned = cleaned.replace(Regex("\\{[^{}]*\\}"), "")

        // Remove JSON arrays (anything between [ and ])
        cleaned = cleaned.replace(Regex("\\[[^\\[\\]]*\\]"), "")

        // Remove common JSON patterns
        cleaned = cleaned.replace(Regex("\"[^\"]*\"\\s*:\\s*\"[^\"]*\""), "")
        cleaned = cleaned.replace(Regex("\"[^\"]*\"\\s*:\\s*\\d+"), "")
        cleaned = cleaned.replace(Regex("\"[^\"]*\"\\s*:\\s*(true|false)"), "")

        // Remove parentheses that might contain JSON-like content
        cleaned = cleaned.replace(Regex("\\([^)]*\"[^\"]*\"[^)]*\\)"), "")

        // Clean up multiple spaces and newlines
        cleaned = cleaned.replace(Regex("\\s+"), " ")
        cleaned = cleaned.replace(Regex("\\n\\s*\\n"), "\n\n")

        // Remove leading/trailing whitespace
        cleaned = cleaned.trim()

        return cleaned
    }

    private fun extractStructuredData(response: String): ParsedResponse {
        val newCharacter = extractCharacterInfo(response)
        val questUpdate = extractQuestInfo(response)
        val statusUpdate = extractStatusInfo(response)
        val environmentUpdate = extractEnvironmentInfo(response)

        return ParsedResponse(
            storyText = "",
            newCharacter = newCharacter,
            questUpdate = questUpdate,
            statusUpdate = statusUpdate,
            environmentUpdate = environmentUpdate
        )
    }

    private fun extractCharacterInfo(response: String): CharacterInfo? {
        return try {
            // Look for character patterns in various formats
            val patterns = listOf(
                // JSON-like patterns
                Regex("\"name\"\\s*:\\s*\"([^\"]+)\""),
                Regex("\"character\"\\s*:\\s*\"([^\"]+)\""),
                // Natural language patterns
                Regex("(?i)(meets?|encounter[s]?|introduced? to)\\s+([A-Z][a-z]+)"),
                Regex("(?i)new character:?\\s*([A-Z][a-z]+)"),
                Regex("(?i)character name:?\\s*([A-Z][a-z]+)")
            )

            for (pattern in patterns) {
                val match = pattern.find(response)
                if (match != null) {
                    val name = match.groupValues.getOrNull(1) ?: match.groupValues.getOrNull(2) ?: continue

                    // Extract additional info
                    val age = extractAge(response, name)
                    val personality = extractPersonality(response, name)
                    val appearance = extractAppearance(response, name)

                    return CharacterInfo(
                        name = name,
                        age = age,
                        personality = personality,
                        appearance = appearance,
                        relationship = 0.0f // Default neutral
                    )
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun extractAge(response: String, characterName: String): Int {
        val agePatterns = listOf(
            Regex("(?i)${characterName}.*?(\\d{1,2})\\s*(?:year|age|yaş)"),
            Regex("(?i)(\\d{1,2})\\s*(?:year|age|yaş).*?${characterName}"),
            Regex("\"age\"\\s*:\\s*(\\d+)")
        )

        for (pattern in agePatterns) {
            val match = pattern.find(response)
            if (match != null) {
                return match.groupValues[1].toIntOrNull() ?: 0
            }
        }
        return 0
    }

    private fun extractPersonality(response: String, characterName: String): String {
        val personalityPatterns = listOf(
            Regex("(?i)${characterName}.*?(friendly|hostile|mysterious|brave|coward|wise|foolish|kind|cruel|calm|angry|confident|shy)"),
            Regex("(?i)(friendly|hostile|mysterious|brave|coward|wise|foolish|kind|cruel|calm|angry|confident|shy).*?${characterName}"),
            Regex("\"personality\"\\s*:\\s*\"([^\"]+)\"")
        )

        for (pattern in personalityPatterns) {
            val match = pattern.find(response)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return ""
    }

    private fun extractAppearance(response: String, characterName: String): String {
        val appearancePatterns = listOf(
            Regex("(?i)${characterName}.*?(tall|short|thin|fat|blonde|brunette|dark|light|beard|mustache)"),
            Regex("\"appearance\"\\s*:\\s*\"([^\"]+)\"")
        )

        for (pattern in appearancePatterns) {
            val match = pattern.find(response)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return ""
    }

    private fun extractQuestInfo(response: String): QuestInfo? {
        return try {
            val questPatterns = listOf(
                Regex("\"quest\"\\s*:\\s*\"([^\"]+)\""),
                Regex("(?i)quest:?\\s*([^\\n]+)"),
                Regex("(?i)task:?\\s*([^\\n]+)")
            )

            for (pattern in questPatterns) {
                val match = pattern.find(response)
                if (match != null) {
                    val title = match.groupValues[1]
                    return QuestInfo(
                        id = "quest_${System.currentTimeMillis()}",
                        title = title,
                        description = title,
                        giver = "Unknown"
                    )
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun extractStatusInfo(response: String): StatusInfo? {
        return try {
            val healthPattern = Regex("(?i)health:?\\s*([+-]?\\d+)")
            val manaPattern = Regex("(?i)mana:?\\s*([+-]?\\d+)")
            val staminaPattern = Regex("(?i)stamina:?\\s*([+-]?\\d+)")

            val healthMatch = healthPattern.find(response)
            val manaMatch = manaPattern.find(response)
            val staminaMatch = staminaPattern.find(response)

            if (healthMatch != null || manaMatch != null || staminaMatch != null) {
                return StatusInfo(
                    healthChange = healthMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0,
                    manaChange = manaMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0,
                    staminaChange = staminaMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0
                )
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun extractEnvironmentInfo(response: String): EnvironmentInfo? {
        return try {
            val locationPattern = Regex("(?i)location:?\\s*([^\\n]+)")
            val weatherPattern = Regex("(?i)weather:?\\s*([^\\n]+)")

            val locationMatch = locationPattern.find(response)
            val weatherMatch = weatherPattern.find(response)

            if (locationMatch != null || weatherMatch != null) {
                return EnvironmentInfo(
                    location = locationMatch?.groupValues?.get(1)?.trim(),
                    weather = weatherMatch?.groupValues?.get(1)?.trim()
                )
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    fun formatCharacterSummary(character: CharacterInfo): String {
        val parts = mutableListOf<String>()

        if (character.age > 0) {
            parts.add("${character.age}")
        }

        if (character.personality.isNotEmpty()) {
            parts.add(character.personality)
        }

        if (character.appearance.isNotEmpty()) {
            parts.add(character.appearance)
        }

        return if (parts.isNotEmpty()) {
            " (${parts.joinToString(", ")})"
        } else {
            ""
        }
    }
}