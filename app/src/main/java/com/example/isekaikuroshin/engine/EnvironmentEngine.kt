package com.example.isekaikuroshin.engine

import com.example.isekaikuroshin.data.Effect
import com.example.isekaikuroshin.data.GameStateZ7
import com.example.isekaikuroshin.engine.Weather // DEĞİŞTİRİLDİ (data.Weather -> engine.Weather)
import com.example.isekaikuroshin.engine.StatType

object EnvironmentEngine {

    fun getActiveEffects(gameState: GameStateZ7): List<Effect> {
        return when (gameState.currentWeather) {
            Weather.RAINY -> {
                listOf(
                    Effect(
                        name = "Sırılsıklam",
                        description = "Yağmur hareketlerini yavaşlatıyor ve ok atmayı zorlaştırıyor.",
                        statModifier = mapOf(StatType.AGI_PERCENT to -0.20f)
                    )
                )
            }
            Weather.FOGGY -> {
                listOf(
                    Effect(
                        name = "Kısıtlı Görüş",
                        description = "Sis görüş mesafesini azaltıyor ve saldırı isabetini düşürüyor.",
                        statModifier = mapOf(StatType.AGI_PERCENT to -0.10f, StatType.LUCK_PERCENT to -0.15f)
                    )
                )
            }
            Weather.WINDY -> {
                listOf(
                    Effect(
                        name = "Rüzgar Esintisi",
                        description = "Güçlü rüzgar hızınızı artırıyor ama dengeyi zorlaştırıyor.",
                        statModifier = mapOf(StatType.AGI_PERCENT to 0.15f, StatType.STR_PERCENT to -0.05f)
                    )
                )
            }
            Weather.SUNNY -> {
                emptyList()
            }
            Weather.STORMY -> {
                emptyList()
            }
            Weather.SNOWY -> {
                emptyList()
            }
            Weather.CLOUDY -> {
                emptyList()
            }
            Weather.HOT -> { // EKLENDİ
                emptyList() // Şimdilik etki yok
            }
            Weather.COLD -> { // EKLENDİ
                emptyList() // Şimdilik etki yok
            }
            Weather.BLIZZARD -> { // EKLENDİ
                emptyList() // Şimdilik etki yok
            }
        }
    }
}