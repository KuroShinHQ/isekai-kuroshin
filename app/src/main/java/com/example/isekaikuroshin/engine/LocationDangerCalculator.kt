
package com.example.isekaikuroshin.engine

import com.example.isekaikuroshin.data.Location
import com.example.isekaikuroshin.data.TimeOfDay

class LocationDangerCalculator {
    /**
     * Calculates the dynamic danger level of a location.
     * @param location The location to calculate danger for.
     * @param playerLevel The player's current level.
     * @param currentTime The current time of day.
     * @return A float representing the calculated danger level, where 1.0 is normal.
     */
    fun calculateDanger(location: Location, playerLevel: Int, currentTime: TimeOfDay): Float {
        var baseDanger: Float = location.dangerLevel.toFloat() // DEĞİŞTİRİLDİ

        // Increase danger if the location's required level is higher than the player's
        val levelDifference = location.requiredLevel - playerLevel
        if (levelDifference > 0) {
            baseDanger += levelDifference * 0.1f
        }

        // Modify danger based on the time of day
        baseDanger *= when (currentTime) {
            TimeOfDay.NIGHT -> 1.3f // Night is 30% more dangerous
            TimeOfDay.MORNING, TimeOfDay.EVENING -> 1.1f // Dawn and dusk are 10% more dangerous
            else -> 1.0f // Day time is normal
        }

        // Coerce the final value to be within a reasonable range (e.g., 0.0 to 2.0)
        return baseDanger.coerceIn(0.0f, 2.0f)
    }
}
