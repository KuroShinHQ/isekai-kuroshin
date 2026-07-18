package com.example.isekaikuroshin.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.isekaikuroshin.data.*

/**
 * Room Entity for DynamicContent
 * GM tarafından üretilen eşsiz içeriklerin database representation'ı
 */
@Entity(tableName = "dynamic_content")
@TypeConverters(Converters::class)
data class DynamicContentEntity(
    @PrimaryKey
    val id: String,
    val type: DynamicContentType,
    val name: String,
    val description: String,

    // GM Metadata
    val gmGeneratedContext: String = "",
    val createdAtDay: Int = 1,
    val createdAtTimestamp: Long = System.currentTimeMillis(),

    // Icon System
    val iconPath: String? = null,
    val placeholderColor: String = "#FF00BFFF",
    val hasIcon: Boolean = false,

    // Export/Import
    val isExported: Boolean = false,
    val isFavorite: Boolean = false,

    // Element-Specific Fields (JSON string)
    val elementData: String? = null,

    // Skill-Specific Fields (JSON string)
    val skillData: String? = null,

    // Item-Specific Fields (JSON string)
    val itemData: String? = null
)

/**
 * Extension functions to convert between DynamicContent and DynamicContentEntity
 */
fun DynamicContent.toEntity(): DynamicContentEntity {
    return DynamicContentEntity(
        id = this.id,
        type = this.type,
        name = this.name,
        description = this.description,
        gmGeneratedContext = this.gmGeneratedContext,
        createdAtDay = this.createdAtDay,
        createdAtTimestamp = this.createdAtTimestamp,
        iconPath = this.iconPath,
        placeholderColor = this.placeholderColor,
        hasIcon = this.hasIcon,
        isExported = this.isExported,
        isFavorite = this.isFavorite,
        elementData = this.elementData?.let { kotlinx.serialization.json.Json.encodeToString(DynamicElementData.serializer(), it) },
        skillData = this.skillData?.let { kotlinx.serialization.json.Json.encodeToString(DynamicSkillData.serializer(), it) },
        itemData = this.itemData?.let { kotlinx.serialization.json.Json.encodeToString(DynamicItemData.serializer(), it) }
    )
}

fun DynamicContentEntity.toDynamicContent(): DynamicContent {
    return DynamicContent(
        id = this.id,
        type = this.type,
        name = this.name,
        description = this.description,
        gmGeneratedContext = this.gmGeneratedContext,
        createdAtDay = this.createdAtDay,
        createdAtTimestamp = this.createdAtTimestamp,
        iconPath = this.iconPath,
        placeholderColor = this.placeholderColor,
        hasIcon = this.hasIcon,
        isExported = this.isExported,
        isFavorite = this.isFavorite,
        elementData = this.elementData?.let { kotlinx.serialization.json.Json.decodeFromString(DynamicElementData.serializer(), it) },
        skillData = this.skillData?.let { kotlinx.serialization.json.Json.decodeFromString(DynamicSkillData.serializer(), it) },
        itemData = this.itemData?.let { kotlinx.serialization.json.Json.decodeFromString(DynamicItemData.serializer(), it) }
    )
}
