package com.example.isekaikuroshin.engine

object LocationDiscoveryEngine {

    private val actionKeywords = listOf("keşfettim", "buldum", "gördüm", "rastladım", "fark ettim")
    private val locationKeywords = listOf("kule", "mağara", "harabe", "köy", "tapınak", "şehir", "orman", "göl", "nehir", "dağ", "vadi")

    fun analyzeForNewLocation(playerInput: String, aiResponse: String): String? {
        val combinedText = "$playerInput $aiResponse".lowercase()

        val actionFound = actionKeywords.any { combinedText.contains(it) }
        if (!actionFound) return null

        val foundLocationType = locationKeywords.find { combinedText.contains(it) }
        if (foundLocationType != null) {
            // This is a simple implementation. A more advanced implementation would use NLP to extract the full name.
            // For now, we'll just return the found location type as the name.
            return foundLocationType.replaceFirstChar { it.uppercase() }
        }

        return null
    }
}
