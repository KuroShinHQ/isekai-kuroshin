package com.example.isekaikuroshin.engine

import com.example.isekaikuroshin.data.PlayerState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PacingEngine @Inject constructor() {

    fun calculateThreatThreshold(playerState: PlayerState, storyMoodModifier: Float = 1.0f): Int {
        val baseThreshold = 100 // Temel eşik değeri
        val moralityModifier = 1.0f + playerState.moralityScore
        val finalThreshold = (baseThreshold * moralityModifier * storyMoodModifier).toInt()
        return finalThreshold.coerceAtLeast(10) // Eşiğin çok düşmesini önlemek için minimum 10
    }
}