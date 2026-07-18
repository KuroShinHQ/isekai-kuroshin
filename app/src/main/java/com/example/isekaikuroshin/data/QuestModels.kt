package com.example.isekaikuroshin.data

import kotlinx.serialization.Serializable

@Serializable
data class ClassQuest(
    val id: String,
    val triggeringArchetype: String,
    val quest: RegistryQuest
)