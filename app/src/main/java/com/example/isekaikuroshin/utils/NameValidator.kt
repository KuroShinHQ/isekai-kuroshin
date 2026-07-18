package com.example.isekaikuroshin.utils

/**
 * G142: Name Validation - Ensures NPC/Enemy names follow proper format
 *
 * RULES:
 * - NO underscores (_)
 * - NO numbers (0-9)
 * - NO special characters (except spaces, hyphens, apostrophes)
 * - Title Case format recommended
 */
object NameValidator {

    /**
     * Validates NPC/Enemy display name format
     *
     * @param name The display name to validate
     * @return true if valid, false if invalid
     */
    fun isValidDisplayName(name: String): Boolean {
        if (name.isBlank()) return false

        // G142: NO underscores
        if (name.contains('_')) {
            GameLogger.logWarning("NameValidator", "Invalid name (underscore): $name")
            return false
        }

        // G142: NO numbers
        if (name.any { it.isDigit() }) {
            GameLogger.logWarning("NameValidator", "Invalid name (contains digit): $name")
            return false
        }

        // G142: NO special chars (except space, hyphen, apostrophe)
        val allowedSpecialChars = setOf(' ', '-', '\'')
        if (name.any { !it.isLetterOrDigit() && it !in allowedSpecialChars }) {
            GameLogger.logWarning("NameValidator", "Invalid name (special char): $name")
            return false
        }

        return true
    }

    /**
     * Sanitizes an invalid name to a fallback format
     *
     * Examples:
     * - "enemy_goblin_1" → "Enemy"
     * - "bandit_23" → "Bandit"
     * - "wolf.alpha" → "Wolf"
     *
     * @param name The invalid name
     * @return A sanitized fallback name
     */
    fun sanitizeName(name: String): String {
        // Remove underscores, numbers, and special chars
        val cleaned = name
            .replace('_', ' ')  // underscore → space
            .replace(Regex("[0-9]"), "")  // remove numbers
            .replace(Regex("[^a-zA-Z\\s-']"), "")  // remove special chars (keep space, hyphen, apostrophe)
            .trim()
            .split(" ")
            .firstOrNull()  // Take first word
            ?.replaceFirstChar { it.uppercase() }  // Capitalize first letter
            ?: "Unknown"

        GameLogger.logWarning("NameValidator", "Sanitized name: \"$name\" → \"$cleaned\"")
        return cleaned
    }

    /**
     * Validates and sanitizes a name if needed
     *
     * @param name The name to check
     * @return The original name if valid, or a sanitized version if invalid
     */
    fun validateOrSanitize(name: String): String {
        return if (isValidDisplayName(name)) {
            name
        } else {
            sanitizeName(name)
        }
    }
}
