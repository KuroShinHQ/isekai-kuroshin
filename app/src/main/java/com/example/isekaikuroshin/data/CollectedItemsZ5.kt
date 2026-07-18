package com.example.isekaikuroshin.data

import kotlinx.serialization.Serializable

@Serializable
data class CollectedItemsZ5(
    val items: Map<String, Int> = emptyMap()
) {
    fun totalItems(): Int {
        return items.values.sum()
    }
}