package com.example.isekaikuroshin.engine

import com.example.isekaikuroshin.data.NormalizedPoint

/**
 * Hata Ayıklama Verileri
 *
 * Jest tanıma motorunun iç işleyişini görselleştirmek için
 * kullanılan debug bilgilerini içerir.
 *
 * ⚡ GÜNCELLEME: Profesyonel best practice'lerden öğrenilen
 * güvenilirlik tabanlı filtreleme desteği eklendi.
 */
data class DebugInfo(
    /**
     * Kullanıcının o anki, normalleştirilmiş el iskeleti verisi
     */
    val liveNormalizedLandmarks: List<NormalizedPoint> = emptyList(),

    /**
     * Sistemdeki şablonlardan, o anki ele en çok benzeyen (skoru en yüksek olan)
     * şablonun iskelet verisi
     */
    val bestMatchTemplateLandmarks: List<NormalizedPoint> = emptyList(),

    /**
     * Seçili mühür için hafızaya yüklenmiş toplam kalibrasyon açısı sayısı
     */
    val loadedTemplateCount: Int = 0,

    /**
     * En çok benzeyen şablonun listedeki sırası (index'i)
     */
    val bestMatchTemplateIndex: Int = -1,

    /**
     * O anki en iyi eşleşmenin yüzdesel doğruluk skoru (0.0 - 1.0)
     */
    val rawAccuracyScore: Float = 0f,

    /**
     * Ortalama mesafe (template ile canlı el arasındaki)
     */
    val avgDistance: Float = 0f,

    /**
     * Smoothing buffer boyutu (kaç frame smoothing yapılıyor)
     */
    val smoothingBufferSize: Int = 0,

    /**
     * Tracking kayboldu mu?
     */
    val isTrackingLost: Boolean = false,

    /**
     * ⚡ YENİ: Her bir landmark için MediaPipe'tan gelen güvenilirlik skorları (0.0 - 1.0)
     *
     * Bu veriler, kapalı pozisyonlarda (örn. yumruk) hangi landmark'ların düşük
     * güvenilirlikte olduğunu gösterir. Debug overlay'de yarı saydam çizim için kullanılır.
     *
     * Araştırma önerisi: "Confidence scores representing system's certainty about detected gestures"
     */
    val landmarkConfidences: List<Float> = emptyList(),

    /**
     * ⚡ YENİ: Elin genel tespit kalitesi
     *
     * Araştırmadan: "Adaptively lower or raise confidence thresholds based on per-frame metrics"
     * - EXCELLENT: >= 0.95 (Tüm landmark'lar net görünüyor)
     * - GOOD: >= 0.80 (Çoğu landmark güvenilir)
     * - ACCEPTABLE: >= 0.60 (Bazı landmark'lar belirsiz, ama kullanılabilir)
     * - POOR: < 0.60 (Kapalı el veya oklüzyon, düşük güvenilirlik)
     */
    val trackingQuality: TrackingQuality = TrackingQuality.GOOD,

    /**
     * ⚡ YENİ: Normalizasyon algoritması versiyonu
     *
     * Hangi algoritma versiyonunun kullanıldığını gösterir (debug için)
     */
    val normalizationVersion: String = "v2-procrustes"
)

/**
 * El takip kalitesi seviyeleri
 *
 * Araştırma önerisi: Güvenilirlik bazlı adaptif eşik yönetimi
 */
enum class TrackingQuality {
    EXCELLENT,  // >= 0.95 average confidence
    GOOD,       // >= 0.80 average confidence
    ACCEPTABLE, // >= 0.60 average confidence
    POOR        // < 0.60 average confidence
}
