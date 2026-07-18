package com.example.isekaikuroshin.engine

import com.example.isekaikuroshin.data.PlayerState // DEĞİŞTİRİLDİ

class EncumbranceCalculator {
    companion object {
        const val BASE_CARRY_CAPACITY = 100.0f
        const val VIT_BONUS_PER_POINT = 2.0f
        const val STR_BONUS_PER_POINT = 1.5f
    }

    fun calculateMaxCapacity(playerState: PlayerState): Float { // DEĞİŞTİRİLDİ
        return BASE_CARRY_CAPACITY +
               (playerState.vitality * VIT_BONUS_PER_POINT) +
               (playerState.strength * STR_BONUS_PER_POINT) // DEĞİŞTİRİLDİ (vit -> vitality, str -> strength)
    }

    fun calculateEncumbranceLevel(currentWeight: Float, maxCapacity: Float): EncumbranceLevel {
        val ratio = currentWeight / maxCapacity
        return when {
            ratio <= 0.5f -> EncumbranceLevel.LIGHT
            ratio <= 0.8f -> EncumbranceLevel.MEDIUM
            ratio <= 1.0f -> EncumbranceLevel.HEAVY
            else -> EncumbranceLevel.OVERLOADED
        }
    }

    fun getTravelSpeedMultiplier(encumbranceLevel: EncumbranceLevel): Float {
        return when (encumbranceLevel) {
            EncumbranceLevel.LIGHT -> 1.2f
            EncumbranceLevel.MEDIUM -> 1.0f
            EncumbranceLevel.HEAVY -> 0.7f
            EncumbranceLevel.OVERLOADED -> 0.4f
        }
    }

    fun getStaminaCostMultiplier(encumbranceLevel: EncumbranceLevel): Float {
        return when (encumbranceLevel) {
            EncumbranceLevel.LIGHT -> 0.8f
            EncumbranceLevel.MEDIUM -> 1.0f
            EncumbranceLevel.HEAVY -> 1.4f
            EncumbranceLevel.OVERLOADED -> 2.0f
        }
    }
}

enum class EncumbranceLevel {
    LIGHT,    // %0-50 kapasite
    MEDIUM,   // %50-80 kapasite
    HEAVY,    // %80-100 kapasite
    OVERLOADED // %100+ kapasite
}