package com.example.isekaikuroshin.data

import kotlinx.serialization.Serializable

@Serializable
data class Quest(
    val title: String,
    val description: String
)