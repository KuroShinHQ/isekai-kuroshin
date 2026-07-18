package com.example.isekaikuroshin.data

/**
 * Gemini Vision API analiz sonucu
 * @property dataType Tespit edilen veri tipi
 * @property extractedValues Görüntüden çıkarılan değerler (key-value pairs)
 * @property confidence Güven skoru (0.0 - 1.0)
 * @property suggestedCorrections AI'ın önerdiği düzeltmeler
 * @property userCorrected Kullanıcının düzeltme yapıp yapmadığı
 */
data class VisionAnalysisResult(
    val dataType: DataType,
    val extractedValues: Map<String, Any>,
    val confidence: Float,
    val suggestedCorrections: List<String>? = null,
    val userCorrected: Boolean = false
)
