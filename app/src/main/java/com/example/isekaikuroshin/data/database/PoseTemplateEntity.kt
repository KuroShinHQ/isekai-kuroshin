package com.example.isekaikuroshin.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room Entity for Pose Templates (Kalibrasyon Verileri)
 *
 * Kullanıcının kalibre ettiği egzersiz pozisyonlarını veritabanında saklar.
 */
@Entity(tableName = "pose_templates")
data class PoseTemplateEntity(
    @PrimaryKey
    val id: String,                    // "user_default_pushup"

    val exerciseType: String,          // "PUSH_UP", "SIT_UP", "ROPE_SKIPPING"
    val userId: String,                // Kullanıcı ID (şimdilik "default")

    // JSON olarak saklanacak
    val upperPositionAngles: String,   // Map<String, Float> -> JSON
    val lowerPositionAngles: String,   // Map<String, Float> -> JSON

    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

/**
 * Type Converters for PoseTemplate
 */
class PoseTemplateConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringMap(value: String): Map<String, Float> {
        val type = object : TypeToken<Map<String, Float>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun toStringMap(map: Map<String, Float>): String {
        return gson.toJson(map)
    }
}
