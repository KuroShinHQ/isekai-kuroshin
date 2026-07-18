package com.example.isekaikuroshin.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Pose Templates
 *
 * Kalibrasyon verilerine erişim sağlar.
 */
@Dao
interface PoseTemplateDao {

    /**
     * Belirli bir egzersiz türü için aktif template'i al
     */
    @Query("SELECT * FROM pose_templates WHERE exerciseType = :exerciseType AND userId = :userId AND isActive = 1 LIMIT 1")
    suspend fun getActiveTemplate(exerciseType: String, userId: String = "default"): PoseTemplateEntity?

    /**
     * Belirli bir egzersiz türü için aktif template'i Flow olarak dinle
     */
    @Query("SELECT * FROM pose_templates WHERE exerciseType = :exerciseType AND userId = :userId AND isActive = 1 LIMIT 1")
    fun observeActiveTemplate(exerciseType: String, userId: String = "default"): Flow<PoseTemplateEntity?>

    /**
     * Tüm template'leri al
     */
    @Query("SELECT * FROM pose_templates WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getAllTemplates(userId: String = "default"): List<PoseTemplateEntity>

    /**
     * Template ekle veya güncelle
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(template: PoseTemplateEntity)

    /**
     * Template sil
     */
    @Delete
    suspend fun delete(template: PoseTemplateEntity)

    /**
     * Belirli bir egzersiz türü için tüm eski template'leri deaktive et
     */
    @Query("UPDATE pose_templates SET isActive = 0 WHERE exerciseType = :exerciseType AND userId = :userId")
    suspend fun deactivateAllForExercise(exerciseType: String, userId: String = "default")

    /**
     * Tüm template'leri sil (test için)
     */
    @Query("DELETE FROM pose_templates")
    suspend fun deleteAll()
}
