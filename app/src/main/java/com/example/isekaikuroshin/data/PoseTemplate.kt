package com.example.isekaikuroshin.data

/**
 * Pose Template - Kalibre Edilmiş Egzersiz Pozisyonu
 *
 * Kullanıcının "üst" ve "alt" pozisyonlarını kaydeder.
 * Bu sayede tekrar sayma kullanıcıya özel olur.
 *
 * ÖRNEK:
 * - Şınav için: üst = kollar uzanmış, alt = kollar bükülmüş
 * - Mekik için: üst = oturmuş, alt = sırt yerde
 */
data class PoseTemplate(
    val id: String,                    // Unique ID
    val exerciseType: String,          // "PUSH_UP", "SIT_UP", "ROPE_SKIPPING"
    val userId: String,                // Kullanıcı ID (şimdilik "default")

    // Üst pozisyon (örn: şınav yukarıda)
    val upperPosition: PosePosition,

    // Alt pozisyon (örn: şınav aşağıda)
    val lowerPosition: PosePosition,

    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true       // Aktif template mı?
)

/**
 * Pose Position - Belirli bir andaki vücut pozisyonu
 *
 * Önemli açıları ve landmark pozisyonlarını saklar
 */
data class PosePosition(
    // Temel açılar (derece cinsinden)
    val angles: Map<String, Float>,

    // Opsiyonel: Raw landmark verileri (debug için)
    val landmarkData: Map<String, LandmarkPoint>? = null,

    // Snapshot metadatası
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Landmark Point - Tek bir vücut noktasının 3D pozisyonu
 */
data class LandmarkPoint(
    val x: Float,      // Normalize X (0.0 - 1.0)
    val y: Float,      // Normalize Y (0.0 - 1.0)
    val z: Float,      // Derinlik
    val visibility: Float = 1.0f  // Görünürlük skoru (0.0 - 1.0)
)

/**
 * Helper: Varsayılan template oluştur (kalibrasyon yoksa)
 */
object PoseTemplateDefaults {

    fun getDefaultPushUpTemplate(): PoseTemplate {
        return PoseTemplate(
            id = "default_pushup",
            exerciseType = "PUSH_UP",
            userId = "default",
            upperPosition = PosePosition(
                angles = mapOf("elbow_angle" to 165f)  // Kollar uzanmış
            ),
            lowerPosition = PosePosition(
                angles = mapOf("elbow_angle" to 90f)   // Kollar bükülmüş
            )
        )
    }

    fun getDefaultSitUpTemplate(): PoseTemplate {
        return PoseTemplate(
            id = "default_situp",
            exerciseType = "SIT_UP",
            userId = "default",
            upperPosition = PosePosition(
                angles = mapOf("torso_angle" to 70f)   // Oturmuş
            ),
            lowerPosition = PosePosition(
                angles = mapOf("torso_angle" to 160f)  // Yatar
            )
        )
    }

    fun getDefaultRopeSkippingTemplate(): PoseTemplate {
        return PoseTemplate(
            id = "default_rope",
            exerciseType = "ROPE_SKIPPING",
            userId = "default",
            upperPosition = PosePosition(
                angles = mapOf("ankle_y" to 0.3f)  // Havada (normalize Y)
            ),
            lowerPosition = PosePosition(
                angles = mapOf("ankle_y" to 0.7f)  // Yerde (normalize Y)
            )
        )
    }
}
