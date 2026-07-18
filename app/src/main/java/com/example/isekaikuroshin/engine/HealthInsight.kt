package com.example.isekaikuroshin.engine

/**
 * TODO-HUB-08: Sağlık İçgörüsü Veri Sınıfı
 *
 * ProactiveHealthCoach tarafından üretilen analiz sonuçlarını temsil eder.
 * SYS-20 raporunda tasarlanan HealthInsight modelinin prototip implementasyonu.
 */
data class HealthInsight(
    /**
     * İçgörünün başlığı (örn: "Düşük Aktivite Haftası")
     */
    val title: String,

    /**
     * İçgörünün detaylı açıklaması
     */
    val description: String,

    /**
     * İçgörünün önem seviyesi
     */
    val severity: HealthInsightSeverity,

    /**
     * İçgörünün kategorisi
     */
    val category: HealthInsightCategory,

    /**
     * Önerilen aksiyon
     */
    val recommendedAction: String? = null,

    /**
     * İçgörünün oluşturulma zamanı
     */
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Sağlık içgörüsü önem seviyesi
 */
enum class HealthInsightSeverity {
    LOW,        // Bilgilendirme amaçlı
    MEDIUM,     // Dikkat gerektiren
    HIGH,       // Acil aksiyon gerektiren
    CRITICAL    // Kritik durum
}

/**
 * Sağlık içgörüsü kategorisi
 */
enum class HealthInsightCategory {
    ACTIVITY,       // Aktivite ile ilgili
    WEIGHT,         // Kilo ile ilgili
    NUTRITION,      // Beslenme ile ilgili
    SLEEP,          // Uyku ile ilgili
    GENERAL_HEALTH, // Genel sağlık
    MENTAL_HEALTH   // Ruh sağlığı
}