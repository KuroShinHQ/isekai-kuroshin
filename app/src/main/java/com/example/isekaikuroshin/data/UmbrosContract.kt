package com.example.isekaikuroshin.data

import kotlinx.serialization.Serializable

@Serializable
data class UmbrosContract(
    val isActive: Boolean = false,
    val remainingCurse: Int = 0, // Remaining curse duration
    val totalContracts: Int = 0, // Total number of contracts made
    val penaltyMultiplier: Float = 1.0f // Penalty multiplier for future actions
)