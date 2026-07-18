package com.example.isekaikuroshin.data

import com.example.isekaikuroshin.data.database.PoseTemplateDao
import com.example.isekaikuroshin.data.database.PoseTemplateEntity
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Pose Templates
 *
 * Kalibrasyon verilerinin veritabanı işlemlerini yönetir.
 */
@Singleton
class PoseTemplateRepository @Inject constructor(
    private val poseTemplateDao: PoseTemplateDao
) {

    private val gson = Gson()

    /**
     * Aktif template'i al
     */
    suspend fun getActiveTemplate(exerciseType: String): PoseTemplate? {
        val entity = poseTemplateDao.getActiveTemplate(exerciseType) ?: return null
        return entityToModel(entity)
    }

    /**
     * Aktif template'i Flow olarak dinle
     */
    fun observeActiveTemplate(exerciseType: String): Flow<PoseTemplate?> {
        return poseTemplateDao.observeActiveTemplate(exerciseType).map { entity ->
            entity?.let { entityToModel(it) }
        }
    }

    /**
     * Template kaydet (eski olanları deaktive et)
     */
    suspend fun saveTemplate(template: PoseTemplate) {
        // Önce aynı egzersiz türündeki eski template'leri deaktive et
        poseTemplateDao.deactivateAllForExercise(template.exerciseType)

        // Yeni template'i kaydet
        val entity = modelToEntity(template)
        poseTemplateDao.insertOrUpdate(entity)
    }

    /**
     * Template sil
     */
    suspend fun deleteTemplate(template: PoseTemplate) {
        val entity = modelToEntity(template)
        poseTemplateDao.delete(entity)
    }

    /**
     * Tüm template'leri al
     */
    suspend fun getAllTemplates(): List<PoseTemplate> {
        return poseTemplateDao.getAllTemplates().map { entityToModel(it) }
    }

    // ========================================
    // HELPER: Entity <-> Model Conversion
    // ========================================

    private fun modelToEntity(model: PoseTemplate): PoseTemplateEntity {
        return PoseTemplateEntity(
            id = model.id,
            exerciseType = model.exerciseType,
            userId = model.userId,
            upperPositionAngles = gson.toJson(model.upperPosition.angles),
            lowerPositionAngles = gson.toJson(model.lowerPosition.angles),
            createdAt = model.createdAt,
            isActive = model.isActive
        )
    }

    private fun entityToModel(entity: PoseTemplateEntity): PoseTemplate {
        val upperAngles = gson.fromJson<Map<String, Float>>(
            entity.upperPositionAngles,
            object : com.google.gson.reflect.TypeToken<Map<String, Float>>() {}.type
        )

        val lowerAngles = gson.fromJson<Map<String, Float>>(
            entity.lowerPositionAngles,
            object : com.google.gson.reflect.TypeToken<Map<String, Float>>() {}.type
        )

        return PoseTemplate(
            id = entity.id,
            exerciseType = entity.exerciseType,
            userId = entity.userId,
            upperPosition = PosePosition(angles = upperAngles),
            lowerPosition = PosePosition(angles = lowerAngles),
            createdAt = entity.createdAt,
            isActive = entity.isActive
        )
    }
}
