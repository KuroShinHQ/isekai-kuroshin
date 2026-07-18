package com.example.isekaikuroshin.utils

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sqrt

/**
 * G61d: Level Calculation Logic
 *
 * Utility object for calculating player level from experience points.
 * Uses a square root formula to provide diminishing returns on XP.
 *
 * Formula: level = floor(sqrt(XP / 100)) + 1
 *
 * Examples:
 * - 0 XP = Level 1
 * - 100 XP = Level 2
 * - 400 XP = Level 3
 * - 900 XP = Level 4
 * - 10000 XP = Level 11
 */
object LevelCalculator {

    /**
     * XP to Level conversion
     *
     * @param experience Current experience points
     * @return Calculated level (minimum 1)
     */
    fun calculateLevel(experience: Long): Int {
        if (experience < 0) return 1
        return max(1, floor(sqrt(experience.toDouble() / 100.0)).toInt() + 1)
    }

    /**
     * XP required for a specific level
     *
     * @param level Target level
     * @return Total XP needed to reach that level
     */
    fun xpForLevel(level: Int): Long {
        if (level <= 1) return 0
        return ((level - 1) * (level - 1) * 100).toLong()
    }

    /**
     * XP required until next level
     *
     * @param currentXP Current experience points
     * @return XP needed to level up
     */
    fun xpUntilNextLevel(currentXP: Long): Long {
        val currentLevel = calculateLevel(currentXP)
        val nextLevelXP = xpForLevel(currentLevel + 1)
        return nextLevelXP - currentXP
    }

    /**
     * Progress percentage towards next level
     *
     * @param currentXP Current experience points
     * @return Progress as percentage (0.0 to 1.0)
     */
    fun levelProgress(currentXP: Long): Float {
        val currentLevel = calculateLevel(currentXP)
        val currentLevelXP = xpForLevel(currentLevel)
        val nextLevelXP = xpForLevel(currentLevel + 1)
        val xpInCurrentLevel = currentXP - currentLevelXP
        val xpNeededForLevel = nextLevelXP - currentLevelXP

        return if (xpNeededForLevel > 0) {
            (xpInCurrentLevel.toFloat() / xpNeededForLevel.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }
}
