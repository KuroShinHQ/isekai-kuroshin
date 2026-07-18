package com.example.isekaikuroshin.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.isekaikuroshin.data.Seal
import com.example.isekaikuroshin.data.SealDifficulty
import com.example.isekaikuroshin.data.NormalizedPoint
import com.example.isekaikuroshin.data.PracticeMetrics

/**
 * Room Entity for storing Seal data in the database
 *
 * Bu entity, Kadim Mühür Teknikleri sisteminin mühür verilerini saklar.
 * Seal template'leri, mastery progress, practice metrics hepsi burada.
 */
@Entity(tableName = "seals")
data class SealEntity(
    @PrimaryKey
    val id: String,
    // G87 FIX: KURAL 9 - Hardcoded text yerine translation keys
    val nameKey: String,
    val descriptionKey: String,
    val loreTextKey: String,

    // Difficulty
    val difficulty: String,  // SealDifficulty enum as string
    val tier: String,

    // Template matching data (Multi-angle support)
    val templateLandmarksList: List<List<NormalizedPoint>>,
    val acceptanceThreshold: Float = 0.80f,
    val toleranceThreshold: Float = 0.15f,

    // Requirements
    val prerequisiteSealId: String?,
    val minPlayerLevel: Int,

    // Related skills
    val relatedSkillIds: List<String>,

    // Mastery progress
    val masteryLevel: Int,
    val practiceMetrics: PracticeMetrics,

    // Visual
    val iconPath: String?,
    val visualGuideAnimation: String?
)

/**
 * Extension functions to convert between Seal and SealEntity
 */
fun Seal.toEntity(): SealEntity {
    return SealEntity(
        id = this.id,
        nameKey = this.nameKey,
        descriptionKey = this.descriptionKey,
        loreTextKey = this.loreTextKey,
        difficulty = this.difficulty.name,
        tier = this.tier,
        templateLandmarksList = this.templateLandmarksList,
        acceptanceThreshold = this.acceptanceThreshold,
        toleranceThreshold = this.toleranceThreshold,
        prerequisiteSealId = this.prerequisiteSealId,
        minPlayerLevel = this.minPlayerLevel,
        relatedSkillIds = this.relatedSkillIds,
        masteryLevel = this.masteryLevel,
        practiceMetrics = this.practiceMetrics,
        iconPath = this.iconPath,
        visualGuideAnimation = this.visualGuideAnimation
    )
}

fun SealEntity.toSeal(): Seal {
    return Seal(
        id = this.id,
        nameKey = this.nameKey,
        descriptionKey = this.descriptionKey,
        loreTextKey = this.loreTextKey,
        difficulty = SealDifficulty.valueOf(this.difficulty),
        tier = this.tier,
        templateLandmarksList = this.templateLandmarksList.toMutableList(),
        acceptanceThreshold = this.acceptanceThreshold,
        toleranceThreshold = this.toleranceThreshold,
        prerequisiteSealId = this.prerequisiteSealId,
        minPlayerLevel = this.minPlayerLevel,
        relatedSkillIds = this.relatedSkillIds,
        masteryLevel = this.masteryLevel,
        practiceMetrics = this.practiceMetrics,
        iconPath = this.iconPath,
        visualGuideAnimation = this.visualGuideAnimation
    )
}
