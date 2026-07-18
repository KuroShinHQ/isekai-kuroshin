package com.example.isekaikuroshin.data

import com.example.isekaikuroshin.engine.StatType
import kotlinx.serialization.Serializable

@Serializable
data class Effect(
    val name: String,
    val description: String,
    val statModifier: Map<StatType, Float>
)

@Serializable
enum class WeatherType {
    GUNESLI,
    YAGMURLU,
    SISLI,
    RUZGARLI
}