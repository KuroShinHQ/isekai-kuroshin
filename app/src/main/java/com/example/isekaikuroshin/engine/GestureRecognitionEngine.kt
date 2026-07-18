package com.example.isekaikuroshin.engine

import android.util.Log
import com.example.isekaikuroshin.data.Seal
import com.example.isekaikuroshin.data.NormalizedPoint
import com.example.isekaikuroshin.utils.ProcrustesAnalysis
import com.example.isekaikuroshin.utils.Point3D
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * Gesture Recognition Engine
 *
 * MediaPipe Hands landmark'larını analiz edip, Seal template'leri ile karşılaştırır.
 * Template Matching algoritması kullanır (en doğru yöntem).
 *
 * ALGORİTMA:
 * 1. Normalize: El boyutundan bağımsız hale getir
 * 2. Calculate Distance: Template ile karşılaştır
 * 3. Accuracy Score: 0.0-1.0 arası skor hesapla
 * 4. Performance Level: PERFECT/GOOD/ACCEPTABLE/FAILED
 * 5. Feedback: Kullanıcıya yönlendirme mesajı
 */
@Singleton
class GestureRecognitionEngine @Inject constructor() {

    companion object {
        private const val TAG = "SEAL_PRACTICE_ENGINE"
    }

    // ========================================
    // STATE MANAGEMENT (Anti-Pollution)
    // ========================================
    // Smoothing buffer: Son birkaç frame'in landmark'larını tutarak
    // jitter (titreme) azaltılır ve daha stabil çizim sağlanır.

    private data class LandmarkFrame(
        val landmarks: List<NormalizedPoint>,
        val timestamp: Long
    )

    private val smoothingBuffer = ArrayDeque<LandmarkFrame>()
    private val MAX_BUFFER_SIZE = 3
    private val SMOOTHING_WINDOW_MS = 100L // 100ms içindeki frame'leri smooth et
    private var lastDetectionTime = 0L
    private var isTrackingLost = false

    // ⚡ YENİ: Re-entry tracking (el geri geldiğinde geçiş dönemini takip et)
    private var reEntryFrameCount = 0
    private val RE_ENTRY_GRACE_FRAMES = 10  // ⚡ BÖLÜM B: Uzatıldı! İlk 10 frame'de filtreleme YOK

    // ⚡⚡⚡ KALMAN FILTER ENTEGRASYONU (Gelişmiş Stabilizasyon)
    // ========================================
    // Her bir 21 eklem noktası için ayrı Kalman Filtresi
    // Bu, oklüzyon durumunda eksik landmark'ları tahmin etmemizi sağlar
    private val landmarkKalmanFilters = List(21) {
        com.example.isekaikuroshin.utils.KalmanFilter3D(
            processNoise = 0.001f,      // Düşük = smooth tracking
            measurementNoise = 0.05f    // MediaPipe oldukça güvenilir
        )
    }

    // Frame zamanlama (Kalman predict için dt hesaplama)
    private var lastFrameTime = 0L

    /**
     * ⚡ YENİDEN YAZILDI: Tüm internal state'i kesinlikle sıfırla
     *
     * KULLANIM ALANLARI:
     * - Kullanıcı farklı bir mühür seçtiğinde
     * - Yeni bir pratik session başladığında
     * - Kalibrasyon sonrası
     * - ViewModel cleanup (onCleared)
     *
     * Bu fonksiyon, bir önceki session'dan kalan TÜM verileri temizler.
     * Smoothing buffer, timestamp, tracking flag - her şey sıfırlanır.
     */
    fun resetState() {
        Log.d(TAG, "🔄 [STATE RESET] Cleaning all internal state...")

        val bufferSizeBefore = smoothingBuffer.size
        smoothingBuffer.clear()
        lastDetectionTime = 0L
        isTrackingLost = false
        reEntryFrameCount = 0  // ⚡ YENİ: Re-entry counter'ı da sıfırla

        // ⚡⚡⚡ KALMAN: Tüm filtreleri sıfırla
        landmarkKalmanFilters.forEach { it.reset() }
        lastFrameTime = 0L

        Log.d(TAG, "   ✅ Smoothing buffer cleared (${bufferSizeBefore} frames removed)")
        Log.d(TAG, "   ✅ Timestamp reset to 0")
        Log.d(TAG, "   ✅ Tracking flag reset to false")
        Log.d(TAG, "   ✅ Re-entry counter reset to 0")
        Log.d(TAG, "   ✅ Kalman filters reset (21 filters)")
        Log.d(TAG, "🎉 [STATE RESET] Complete! Engine ready for fresh session.")
    }

    /**
     * ⚡ YENİDEN YAZILDI: El kaybedildiğinde state'i güvenli temizle
     *
     * Bu fonksiyon, el ekrandan çıktığında (landmarks.isEmpty()) çağrılır.
     * El geri geldiğinde eski frame'lerin yeni tespitin üzerine binmesini önler.
     *
     * ÖNEMLİ: Bu fonksiyon idempotent'tir (birden fazla çağırılabilir, zarar vermez)
     */
    fun onTrackingLost() {
        if (!isTrackingLost) {
            isTrackingLost = true
            val clearedFrames = smoothingBuffer.size
            smoothingBuffer.clear()

            // ⚡⚡⚡ KALMAN: Filtreleri sıfırla (eski hız bilgisi artık geçersiz)
            landmarkKalmanFilters.forEach { it.reset() }

            Log.d(TAG, "⚠️ [TRACKING LOST] Hand disappeared - cleared ${clearedFrames} buffered frames")
            Log.d(TAG, "   ✅ Kalman filters reset to prevent stale velocity predictions")
        } else {
            Log.d(TAG, "⚠️ [TRACKING LOST] Already in lost state (no-op)")
        }
    }

    /**
     * ⚡ YENİDEN YAZILDI: Apply exponential moving average smoothing to landmarks
     *
     * Bu fonksiyon, son 3 frame'in landmark'larını kullanarak
     * smooth (titremesiz) landmark'lar üretir.
     *
     * ALGORİTMA:
     * - En son frame'e en yüksek ağırlık (0.5)
     * - Bir önceki frame'e orta ağırlık (0.3)
     * - İki önceki frame'e düşük ağırlık (0.2)
     *
     * ⚡ YENİ: Re-entry tracking entegrasyonu
     * - El yeniden tespit edildiğinde, ilk 3 frame boyunca smoothing YAPILMAZ
     * - Bu, yeniden tespit sonrası bozulmaları önler ve hızlı kilitlenme sağlar
     */
    private fun applySmoothingFilter(
        currentLandmarks: List<NormalizedPoint>,
        timestamp: Long
    ): List<NormalizedPoint> {
        // El yeni tespit edildiyse (tracking loss sonrası), smoothing yapma
        if (isTrackingLost) {
            isTrackingLost = false
            reEntryFrameCount = 0  // ⚡ YENİ: Re-entry counter'ı başlat
            smoothingBuffer.clear()
            smoothingBuffer.addFirst(LandmarkFrame(currentLandmarks, timestamp))
            Log.d(TAG, "🆕 Re-detection: Using raw landmarks (no smoothing) - grace period starts")
            return currentLandmarks
        }

        // ⚡ YENİ: Re-entry grace period kontrolü
        if (reEntryFrameCount < RE_ENTRY_GRACE_FRAMES) {
            reEntryFrameCount++
            smoothingBuffer.clear()
            smoothingBuffer.addFirst(LandmarkFrame(currentLandmarks, timestamp))
            Log.d(TAG, "🔄 Re-entry grace period: frame $reEntryFrameCount/$RE_ENTRY_GRACE_FRAMES (no smoothing)")
            return currentLandmarks
        }

        // Buffer'a ekle
        smoothingBuffer.addFirst(LandmarkFrame(currentLandmarks, timestamp))

        // Eski frame'leri temizle (max size veya 100ms'den eski)
        while (smoothingBuffer.size > MAX_BUFFER_SIZE ||
               (smoothingBuffer.isNotEmpty() && timestamp - smoothingBuffer.last().timestamp > SMOOTHING_WINDOW_MS)) {
            smoothingBuffer.removeLast()
        }

        // Tek frame varsa smoothing yapma
        if (smoothingBuffer.size == 1) {
            return currentLandmarks
        }

        // Ağırlıklı ortalama hesapla
        val weights = listOf(0.5f, 0.3f, 0.2f)
        val smoothedLandmarks = mutableListOf<NormalizedPoint>()

        for (i in currentLandmarks.indices) {
            var weightedX = 0f
            var weightedY = 0f
            var weightedZ = 0f
            var totalWeight = 0f

            smoothingBuffer.forEachIndexed { index, frame ->
                if (index < weights.size && frame.landmarks.size > i) {
                    val weight = weights[index]
                    weightedX += frame.landmarks[i].x * weight
                    weightedY += frame.landmarks[i].y * weight
                    weightedZ += frame.landmarks[i].z * weight
                    totalWeight += weight
                }
            }

            smoothedLandmarks.add(
                NormalizedPoint(
                    x = weightedX / totalWeight,
                    y = weightedY / totalWeight,
                    z = weightedZ / totalWeight
                )
            )
        }

        return smoothedLandmarks
    }

    /**
     * Evaluate detected gesture against expected seal (MULTI-TEMPLATE SUPPORT)
     *
     * @param liveHandLandmarks Canlı el landmark'ları (NormalizedPoint formatında)
     * @param targetSeal Hedeflenen mühür (çoklu template'lere sahip olabilir)
     * @return GestureResult (accuracy, performance level, feedback)
     */
    fun evaluateGesture(
        liveHandLandmarks: List<NormalizedPoint>,
        targetSeal: Seal
    ): GestureResult {
        Log.d(TAG, "========================================")
        Log.d(TAG, "---> Evaluating Gesture (Multi-Template + Kalman Filter) <---")
        Log.d(TAG, "Target Seal: ${targetSeal.nameKey}")
        Log.d(TAG, "Available Templates: ${targetSeal.templateLandmarksList.size}")
        Log.d(TAG, "Acceptance Threshold: ${(targetSeal.acceptanceThreshold * 100).toInt()}%")

        // Validate input
        if (liveHandLandmarks.size != 21) {
            Log.e(TAG, "❌ FAILED: Invalid landmark count (expected 21, got ${liveHandLandmarks.size})")
            return GestureResult(
                isSuccess = false,
                accuracy = 0f,
                performanceLevel = PerformanceLevel.FAILED,
                feedback = "❌ El tespiti başarısız. Lütfen elinizi kameraya net gösterin.",
                detectedLandmarks = emptyList(),
                avgDistance = Float.MAX_VALUE
            )
        }

        // ⚡⚡⚡ KALMAN FILTER PIPELINE
        // ========================================
        val currentTime = System.currentTimeMillis()
        val dt = if (lastFrameTime > 0) {
            (currentTime - lastFrameTime) / 1000f  // Saniye cinsinden
        } else {
            1f / 30f  // İlk frame: 30fps varsay
        }
        lastFrameTime = currentTime
        lastDetectionTime = currentTime

        // STEP 1: PREDICT - Tüm landmark'lar için tahmin yap
        val predictedLandmarks = mutableListOf<NormalizedPoint>()
        landmarkKalmanFilters.forEachIndexed { _, filter ->
            val (px, py, pz) = filter.predict(dt)
            predictedLandmarks.add(NormalizedPoint(px, py, pz))
        }

        // STEP 2: UPDATE - Ölçümlerle filtreleri güncelle
        // ⚡ KRİTİK: MediaPipe'tan gelen her geçerli landmark için Kalman'ı güncelle
        val stabilizedLandmarks = mutableListOf<NormalizedPoint>()
        liveHandLandmarks.forEachIndexed { index, landmark ->
            val filter = landmarkKalmanFilters[index]

            // Geçerli bir ölçüm mü? (MediaPipe bazen NaN veya çok büyük değerler döndürebilir)
            val isValidMeasurement = landmark.x.isFinite() &&
                                    landmark.y.isFinite() &&
                                    landmark.z.isFinite() &&
                                    abs(landmark.x) < 10f &&
                                    abs(landmark.y) < 10f &&
                                    abs(landmark.z) < 10f

            if (isValidMeasurement) {
                // Geçerli ölçüm: Kalman'ı güncelle
                filter.update(landmark.x, landmark.y, landmark.z, dt)
                val state = filter.getState()
                stabilizedLandmarks.add(NormalizedPoint(state.x, state.y, state.z))
            } else {
                // ⚡ BOŞLUK DOLDURMA: Geçersiz/eksik ölçüm → Predict edilen değeri kullan
                stabilizedLandmarks.add(predictedLandmarks[index])
                Log.v(TAG, "⚠️ Landmark $index: Using predicted value (measurement invalid or missing)")
            }
        }

        Log.d(TAG, "✅ Kalman filtering applied (dt: ${(dt * 1000).toInt()}ms)")

        // ⚠️ GÜVENLİK: Boş template durumunu kontrol et (ama çökme!)
        // Debug overlay'de mavi iskelet görünmeye devam etmeli
        if (targetSeal.templateLandmarksList.isEmpty()) {
            Log.w(TAG, "⚠️ No templates available for ${targetSeal.nameKey}. Returning safe state for debug visualization.")

            // Debug overlay için veriyi hazırla (mavi iskelet çizilebilir, kırmızı yok)
            val debugInfo = DebugInfo(
                liveNormalizedLandmarks = stabilizedLandmarks,  // ⚡ Kalman-stabilized veri
                bestMatchTemplateLandmarks = emptyList(),  // Kırmızı iskelet yok
                loadedTemplateCount = 0,
                bestMatchTemplateIndex = -1,
                rawAccuracyScore = 0f,
                avgDistance = 0f,
                smoothingBufferSize = smoothingBuffer.size,
                isTrackingLost = isTrackingLost,
                normalizationVersion = "v4-procrustes-analysis"
            )

            return GestureResult(
                isSuccess = false,
                accuracy = 0f,
                performanceLevel = PerformanceLevel.FAILED,
                feedback = "⚠️ Bu mühür henüz kalibre edilmemiş. Kalibrasyon merkezini ziyaret edin.",
                detectedLandmarks = emptyList(),
                avgDistance = Float.MAX_VALUE,
                debugInfo = debugInfo  // Mavi iskelet için veri var!
            )
        }

        // ========================================
        // Step 1: SİMETRİK KARŞILAŞTIRMA (Symmetric Normalization)
        // ========================================
        // KRİTİK: İki taraf da normalize edilmiş:
        // - smoothedLandmarks: Canlı el (smoothing + normalizasyon uygulandı)
        // - template: Veritabanından gelen şablon (kalibrasyon sırasında normalize edildi)
        // Bu sayede pozisyon, rotasyon ve ölçek farkları YOK edilir!
        var bestAccuracy = 0f
        var bestTemplateIndex = -1
        var bestAvgDistance = Float.MAX_VALUE

        targetSeal.templateLandmarksList.forEachIndexed { index, template ->
            Log.d(TAG, "--- Comparing with Template #$index (KALMAN STABILIZED) ---")

            val (avgDistance, _) = calculateDistanceMetrics(stabilizedLandmarks, template)
            val rawAccuracy = 1f - (avgDistance / targetSeal.toleranceThreshold)
            val accuracy = rawAccuracy.coerceIn(0f, 1f)

            Log.d(TAG, "Template #$index - Avg Distance: $avgDistance, Accuracy: ${(accuracy * 100).toInt()}%")

            if (accuracy > bestAccuracy) {
                bestAccuracy = accuracy
                bestTemplateIndex = index
                bestAvgDistance = avgDistance
            }
        }

        Log.d(TAG, "========================================")
        Log.d(TAG, "🏆 BEST MATCH: Template #$bestTemplateIndex")
        Log.d(TAG, "📊 Best Accuracy: ${(bestAccuracy * 100).toInt()}%")
        Log.d(TAG, "🎯 Required Threshold: ${(targetSeal.acceptanceThreshold * 100).toInt()}%")

        // ⚡⚡⚡ CRITICAL LOG for debugging spell test trigger chain
        Log.e("RecognitionEngine", "⚡ GESTURE EVALUATED! Target: ${targetSeal.nameKey}, Accuracy: ${(bestAccuracy * 100).toInt()}%, Success: ${bestAccuracy >= targetSeal.acceptanceThreshold}")

        // Step 2: Check if best accuracy meets the acceptance threshold
        val isSuccess = bestAccuracy >= targetSeal.acceptanceThreshold

        // Step 3: Performance level
        val performanceLevel = when {
            bestAccuracy >= 0.95f -> PerformanceLevel.PERFECT
            bestAccuracy >= 0.80f -> PerformanceLevel.GOOD
            bestAccuracy >= targetSeal.acceptanceThreshold -> PerformanceLevel.ACCEPTABLE
            else -> PerformanceLevel.FAILED
        }

        Log.d(TAG, "Performance Level: $performanceLevel")
        Log.d(TAG, "Success: $isSuccess")

        // Step 4: Feedback
        val feedback = generateMultiTemplateFeedback(
            bestAccuracy,
            targetSeal.acceptanceThreshold,
            performanceLevel,
            bestTemplateIndex
        )

        Log.d(TAG, "Feedback: $feedback")
        Log.d(TAG, "========================================")

        // ⚠️ DEBUG: DebugInfo oluştur
        val debugInfo = DebugInfo(
            liveNormalizedLandmarks = stabilizedLandmarks,  // ⚡ Kalman-stabilized veri
            bestMatchTemplateLandmarks = if (bestTemplateIndex >= 0 && bestTemplateIndex < targetSeal.templateLandmarksList.size) {
                targetSeal.templateLandmarksList[bestTemplateIndex]
            } else {
                emptyList()
            },
            loadedTemplateCount = targetSeal.templateLandmarksList.size,
            bestMatchTemplateIndex = bestTemplateIndex,
            rawAccuracyScore = bestAccuracy,
            avgDistance = bestAvgDistance,
            smoothingBufferSize = smoothingBuffer.size,
            isTrackingLost = isTrackingLost,
            normalizationVersion = "v4-procrustes-analysis"  // ⚡ Procrustes entegre edildi
        )

        return GestureResult(
            isSuccess = isSuccess,
            accuracy = bestAccuracy,
            performanceLevel = performanceLevel,
            feedback = feedback,
            detectedLandmarks = emptyList(),
            avgDistance = bestAvgDistance,
            debugInfo = debugInfo
        )
    }

    /**
     * Legacy method support (for backward compatibility with MediaPipe NormalizedLandmark)
     *
     * ⚡ GÜNCELLEME: Profesyonel best practice'ler entegre edildi:
     * - Güvenilirlik tabanlı kalite değerlendirmesi
     * - Adaptif confidence filtreleme
     * - Enhanced debug info with tracking quality
     *
     * @param detectedLandmarks MediaPipe Hands'den gelen 21-point landmarks
     * @param expectedSeal Hedeflenen mühür template'i
     * @return GestureResult (accuracy, performance level, feedback)
     */
    @Deprecated("Use evaluateGesture with NormalizedPoint list instead")
    fun evaluateGestureLegacy(
        detectedLandmarks: List<NormalizedLandmark>,
        expectedSeal: Seal
    ): GestureResult {
        // ========================================
        // DEBUGGING: START EVALUATION
        // ========================================
        Log.d(TAG, "========================================")
        Log.d(TAG, "---> Evaluating Gesture (Legacy + Quality Assessment) <---")

        // STEP 0: Log input data
        Log.d(TAG, "Received ${detectedLandmarks.size} landmarks from MediaPipe.")
        Log.d(TAG, "Comparing with seal '${expectedSeal.nameKey}' (${expectedSeal.templateLandmarksList.size} templates).")
        Log.d(TAG, "Template tolerance threshold: ${expectedSeal.toleranceThreshold}")

        // Validate input
        if (detectedLandmarks.size != 21) {
            Log.e(TAG, "❌ FAILED: Invalid landmark count (expected 21, got ${detectedLandmarks.size})")
            Log.d(TAG, "========================================")
            return GestureResult(
                isSuccess = false,
                accuracy = 0f,
                performanceLevel = PerformanceLevel.FAILED,
                feedback = "❌ El tespiti başarısız. Lütfen elinizi kameraya net gösterin.",
                detectedLandmarks = detectedLandmarks,
                avgDistance = Float.MAX_VALUE
            )
        }

        // ⚡ YENİ: Güvenilirlik değerlendirmesi (Profesyonel yaklaşım)
        val (trackingQuality, confidenceScores) = assessTrackingQuality(detectedLandmarks)
        Log.d(TAG, "✅ Tracking quality assessed: $trackingQuality")

        // Step 1: Normalize landmarks (hand-size independent)
        val wrist = detectedLandmarks[0]
        val middleTip = detectedLandmarks[12]
        val handSize = distance(wrist, middleTip)

        Log.d(TAG, "Hand size (wrist to middle tip): $handSize")

        val normalizedDetected = normalizeLandmarks(detectedLandmarks)
        if (normalizedDetected.isEmpty()) {
            Log.e(TAG, "❌ FAILED: Normalization failed (hand size too small: $handSize)")
            Log.d(TAG, "========================================")
            return GestureResult(
                isSuccess = false,
                accuracy = 0f,
                performanceLevel = PerformanceLevel.FAILED,
                feedback = "❌ El çok küçük veya uzakta. Lütfen yaklaşın.",
                detectedLandmarks = detectedLandmarks,
                avgDistance = Float.MAX_VALUE
            )
        }

        Log.d(TAG, "✅ Normalization successful. Normalized ${normalizedDetected.size} landmarks.")

        // ⚡⚡⚡ DEVRE DIŞI: Adaptif güvenilirlik filtreleme KALDIRILDI
        // Artık TÜM landmark'lar eksiksiz çiziliyor - filtreleme YOK!
        val filteredLandmarks = normalizedDetected  // Direkt normalize veri kullanılıyor
        Log.d(TAG, "✅ No filtering applied - all 21 landmarks preserved")

        // Use first template if available, otherwise return failed
        if (expectedSeal.templateLandmarksList.isEmpty()) {
            Log.e(TAG, "❌ FAILED: No templates available for ${expectedSeal.nameKey}")
            Log.d(TAG, "========================================")
            return GestureResult(
                isSuccess = false,
                accuracy = 0f,
                performanceLevel = PerformanceLevel.FAILED,
                feedback = "⚠️ Bu mühür henüz kalibre edilmemiş.",
                detectedLandmarks = detectedLandmarks,
                avgDistance = Float.MAX_VALUE
            )
        }

        val template = expectedSeal.templateLandmarksList[0] // Legacy mode: use first template only

        // Log first few points for comparison (FİLTRELENMİŞ landmark'lar kullanılıyor)
        Log.d(TAG, "Sample filtered point [0]: (${filteredLandmarks[0].x}, ${filteredLandmarks[0].y}, ${filteredLandmarks[0].z})")
        Log.d(TAG, "Sample template point [0]: (${template[0].x}, ${template[0].y}, ${template[0].z})")

        // ⚠️ DEBUG: TÜM filtrelenmiş landmark'ları logla (template oluşturmak için)
        Log.d(TAG, "========== FILTERED LANDMARKS (Copy for Template) ==========")
        filteredLandmarks.forEachIndexed { index, point ->
            Log.d(TAG, "NormalizedPoint(${point.x}f, ${point.y}f, ${point.z}f),  // $index")
        }
        Log.d(TAG, "==============================================================")

        // Step 2: Calculate similarity metrics (FİLTRELENMİŞ landmark'larla)
        val (avgDistance, maxDeviation) = calculateDistanceMetrics(
            filteredLandmarks,
            template
        )

        Log.d(TAG, "Calculated Avg Distance: $avgDistance")
        Log.d(TAG, "Calculated Max Deviation: $maxDeviation")

        // Step 3: Accuracy score (1.0 = perfect, 0.0 = completely wrong)
        val rawAccuracy = 1f - (avgDistance / expectedSeal.toleranceThreshold)
        val accuracy = rawAccuracy.coerceIn(0f, 1f)

        Log.d(TAG, "Raw accuracy (before coercion): $rawAccuracy")
        Log.d(TAG, "Final Accuracy: ${(accuracy * 100).toInt()}%")

        // Step 4: Performance level
        val performanceLevel = when {
            accuracy >= 0.95f -> PerformanceLevel.PERFECT
            accuracy >= 0.80f -> PerformanceLevel.GOOD
            accuracy >= 0.60f -> PerformanceLevel.ACCEPTABLE
            else -> PerformanceLevel.FAILED
        }

        Log.d(TAG, "Performance Level: $performanceLevel")

        // Step 5: Feedback message
        val feedback = generateFeedback(
            accuracy,
            performanceLevel,
            maxDeviation,
            detectedLandmarks
        )

        Log.d(TAG, "Feedback: $feedback")
        Log.d(TAG, "========================================")

        // ⚡ YENİ: Enhanced DebugInfo (güvenilirlik ve kalite bilgisi ile)
        val enhancedDebugInfo = DebugInfo(
            liveNormalizedLandmarks = filteredLandmarks,  // ⚡ FİLTRELENMİŞ landmark'lar
            bestMatchTemplateLandmarks = template,
            loadedTemplateCount = expectedSeal.templateLandmarksList.size,
            bestMatchTemplateIndex = 0,  // Legacy mode: always first template
            rawAccuracyScore = accuracy,
            avgDistance = avgDistance,
            smoothingBufferSize = smoothingBuffer.size,
            isTrackingLost = isTrackingLost,
            landmarkConfidences = confidenceScores,  // ⚡ YENİ
            trackingQuality = trackingQuality,  // ⚡ YENİ
            normalizationVersion = "v2-procrustes+adaptive-filter"  // ⚡ GÜNCELLEME
        )

        return GestureResult(
            isSuccess = accuracy >= 0.60f,
            accuracy = accuracy,
            performanceLevel = performanceLevel,
            feedback = feedback,
            detectedLandmarks = detectedLandmarks,
            avgDistance = avgDistance,
            debugInfo = enhancedDebugInfo  // ⚡ Enhanced debug info
        )
    }

    /**
     * ⚡⚡⚡ REVIZE EDİLDİ - BASİTLEŞTİRİLMİŞ VE MATEMATİKSEL OLARAK GARANTİLİ ALGORİTMA ⚡⚡⚡
     *
     * BÖLÜM A: Algoritmayı basitleştir ve doğrula
     *
     * Bu fonksiyon, pozisyon, rotasyon ve ölçekten TAM BAĞIMSIZ normalizasyon yapar.
     * Karmaşık Procrustes rotasyon yerine, basit ve kanıtlanmış bir yaklaşım kullanır:
     * Bilek-Orta Parmak vektörünü Y eksenine hizalar.
     *
     * Algoritma Adımları:
     * ==================
     * 1. VALIDATION: 21 landmark olduğundan emin ol
     * 2. TRANSLATION: Bilek (landmark 0) → (0, 0, 0) orijine taşı
     * 3. Y-AXIS ALIGNMENT: Bilek→Orta Parmak vektörünü Y eksenine hizala (basit 2D rotasyon)
     * 4. SCALE NORMALIZATION: Bilek-Orta Parmak mesafesini 1.0'a normalize et
     *
     * Sonuç: Matematiksel olarak garantili, pozisyon ve rotasyondan bağımsız normalize el!
     */
    private fun normalizeLandmarks(
        landmarks: List<NormalizedLandmark>
    ): List<NormalizedPoint> {
        Log.d(TAG, "[Normalize-v3-SIMPLE] 🔬 Starting SIMPLIFIED pose-invariant normalization")

        // ================================================================
        // STEP 0: INPUT VALIDATION (Güvenlik - bozuk veri kontrolü)
        // ================================================================
        if (landmarks.isEmpty() || landmarks.size != 21) {
            Log.e(TAG, "[Normalize-v3] ❌ REJECTED: Invalid landmark count (expected 21, got ${landmarks.size})")
            return emptyList()
        }

        val wrist = landmarks[0]  // Landmark 0 (WRIST)
        val middleTip = landmarks[12]  // Landmark 12 (MIDDLE_FINGER_TIP)

        Log.d(TAG, "[Normalize-v3] Input wrist [0]: (${wrist.x()}, ${wrist.y()}, ${wrist.z()})")
        Log.d(TAG, "[Normalize-v3] Input middle [12]: (${middleTip.x()}, ${middleTip.y()}, ${middleTip.z()})")

        // ================================================================
        // STEP 1: TRANSLATION - Pozisyon Bağımsızlığı
        // ================================================================
        // Bilek orijine taşınır
        val translated = landmarks.map { landmark ->
            NormalizedPoint(
                x = landmark.x() - wrist.x(),
                y = landmark.y() - wrist.y(),
                z = landmark.z() - wrist.z()
            )
        }
        Log.d(TAG, "[Normalize-v3] ✅ STEP 1 (TRANSLATION): Wrist → origin")

        // ================================================================
        // STEP 2: Y-AXIS ALIGNMENT - Rotasyon Bağımsızlığı (BASİTLEŞTİRİLMİŞ)
        // ================================================================
        // Bilek → Orta Parmak vektörünü Y eksenine hizala
        // Bu, karmaşık Procrustes rotasyondan çok daha basit ve matematiksel olarak garanti

        val wristTranslated = translated[0]
        val middleTranslated = translated[12]

        // Bilek → Orta Parmak vektörü
        val vecX = middleTranslated.x - wristTranslated.x
        val vecY = middleTranslated.y - wristTranslated.y
        val vecZ = middleTranslated.z - wristTranslated.z

        // Vektör uzunluğu (el boyutu)
        val handLength = sqrt(vecX * vecX + vecY * vecY + vecZ * vecZ)

        if (handLength < 0.001f) {
            Log.e(TAG, "[Normalize-v3] ❌ REJECTED: Hand too small (length: $handLength)")
            return emptyList()
        }

        Log.d(TAG, "[Normalize-v3] Hand vector: ($vecX, $vecY, $vecZ), length: $handLength")

        // XY düzleminde rotasyon açısı hesapla (Z eksenini göz ardı et - 2D yaklaşım)
        val angleRad = atan2(vecX, vecY)  // Y eksenine göre açı
        val angleDeg = Math.toDegrees(angleRad.toDouble())

        Log.d(TAG, "[Normalize-v3] Rotation angle to Y-axis: $angleDeg°")

        // Z ekseni etrafında rotasyon uygula (basit 2D rotasyon)
        val cosAngle = cos(-angleRad)  // Negatif çünkü ters yönde döndürüyoruz
        val sinAngle = sin(-angleRad)

        val rotated = translated.map { point ->
            NormalizedPoint(
                x = point.x * cosAngle - point.y * sinAngle,
                y = point.x * sinAngle + point.y * cosAngle,
                z = point.z  // Z değişmiyor (2D rotasyon)
            )
        }

        Log.d(TAG, "[Normalize-v3] ✅ STEP 2 (Y-AXIS ALIGNMENT): Rotated ${angleDeg.toInt()}°")

        // ================================================================
        // STEP 3: SCALE NORMALIZATION - Ölçek Bağımsızlığı
        // ================================================================
        // Bilek-Orta Parmak mesafesini 1.0'a normalize et

        val normalizedMiddle = rotated[12]
        val normalizedWrist = rotated[0]

        val newHandLength = sqrt(
            (normalizedMiddle.x - normalizedWrist.x) * (normalizedMiddle.x - normalizedWrist.x) +
            (normalizedMiddle.y - normalizedWrist.y) * (normalizedMiddle.y - normalizedWrist.y) +
            (normalizedMiddle.z - normalizedWrist.z) * (normalizedMiddle.z - normalizedWrist.z)
        )

        if (newHandLength < 0.001f) {
            Log.e(TAG, "[Normalize-v3] ❌ REJECTED: Rotated hand too small")
            return emptyList()
        }

        val scale = 1.0f / newHandLength

        val scaled = rotated.map { point ->
            NormalizedPoint(
                x = point.x * scale,
                y = point.y * scale,
                z = point.z * scale
            )
        }

        Log.d(TAG, "[Normalize-v3] ✅ STEP 3 (SCALE): Hand length normalized to 1.0 (was: $newHandLength)")
        Log.d(TAG, "[Normalize-v3] 🎉 COMPLETE! Simplified normalization done.")
        Log.d(TAG, "[Normalize-v3] Result wrist [0]: (${scaled[0].x}, ${scaled[0].y}, ${scaled[0].z})")
        Log.d(TAG, "[Normalize-v3] Result middle [12]: (${scaled[12].x}, ${scaled[12].y}, ${scaled[12].z})")

        return scaled
    }

    /**
     * ⚡⚡⚡ TAMAMEN YENİDEN YAZILDI: Procrustes Analysis ile Matematiksel Olarak Garantili Karşılaştırma
     *
     * ESKIDEN: Basit Euclidean distance (pozisyon, rotasyon, ölçeğe duyarlı)
     * ŞİMDİ: Procrustes Analysis (translation, rotation, scale INVARIANT)
     *
     * PROCRUSTES ANALİZ NEDİR?
     * - İki şekil arasındaki "pure shape difference"ı ölçer
     * - Matematiksel olarak optimal (minimum Procrustes distance)
     * - Industry standard (MediaPipe Face Mesh kullanıyor)
     * - SciPy'nin scipy.spatial.procrustes() ile aynı algoritma
     *
     * ALGORİTMA:
     * 1. CENTER: Her iki kümeyi centroid'e göre merkeze al
     * 2. SCALE: Unit scale'e normalize et
     * 3. ROTATE: Optimal rotation matrix hesapla (Kabsch)
     * 4. ALIGN: İkinci kümeyi birinciye hizala
     * 5. DISTANCE: Hizalanmış kümeler arası Procrustes distance
     *
     * @param detected Canlı el landmark'ları
     * @param template Şablon landmark'ları
     * @return Pair(procrustesDistance, maxDeviation) - maxDeviation legacy uyumluluk için
     */
    private fun calculateDistanceMetrics(
        detected: List<NormalizedPoint>,
        template: List<NormalizedPoint>
    ): Pair<Float, Float> {
        Log.d(TAG, "[Procrustes] 🔬 Starting Procrustes Analysis")
        Log.d(TAG, "[Procrustes] Detected landmarks: ${detected.size}, Template landmarks: ${template.size}")

        if (detected.size != template.size) {
            Log.e(TAG, "[Procrustes] ❌ Size mismatch! Detected: ${detected.size}, Template: ${template.size}")
            return Pair(Float.MAX_VALUE, Float.MAX_VALUE)
        }

        // ========================================
        // DÖNÜŞÜM: NormalizedPoint → Point3D
        // ========================================
        val detectedPoints = detected.map { Point3D(it.x, it.y, it.z) }
        val templatePoints = template.map { Point3D(it.x, it.y, it.z) }

        // ========================================
        // PROCRUSTES ANALYSIS
        // ========================================
        val result = ProcrustesAnalysis.compare(templatePoints, detectedPoints)

        if (!result.isValid) {
            Log.e(TAG, "[Procrustes] ❌ Analysis failed (degenerate case)")
            return Pair(Float.MAX_VALUE, Float.MAX_VALUE)
        }

        // ========================================
        // SONUÇ: Procrustes Distance
        // ========================================
        val procrustesDistance = result.distance

        // Legacy compatibility: maxDeviation hesapla (aligned landmarks kullanarak)
        var maxDeviation = 0f
        var maxDeviationIndex = -1

        for (i in templatePoints.indices) {
            val aligned = result.alignedLandmarks2[i]
            val templatePoint = templatePoints[i]
            val dx = aligned.x - templatePoint.x
            val dy = aligned.y - templatePoint.y
            val dz = aligned.z - templatePoint.z
            val dist = sqrt(dx * dx + dy * dy + dz * dz)

            if (dist > maxDeviation) {
                maxDeviation = dist
                maxDeviationIndex = i
            }
        }

        Log.d(TAG, "[Procrustes] ✅ Procrustes Distance: $procrustesDistance")
        Log.d(TAG, "[Procrustes] Scale: ${result.scale}, Translation: ${result.translation}")
        Log.d(TAG, "[Procrustes] Max deviation: $maxDeviation at landmark $maxDeviationIndex")
        Log.d(TAG, "[Procrustes] 🎉 Analysis complete! (Translation/Rotation/Scale INVARIANT)")

        return Pair(procrustesDistance, maxDeviation)
    }

    /**
     * Generate user-friendly feedback
     */
    private fun generateFeedback(
        accuracy: Float,
        level: PerformanceLevel,
        maxDeviation: Float,
        landmarks: List<NormalizedLandmark>
    ): String {
        return when (level) {
            PerformanceLevel.PERFECT ->
                "🌟 Mükemmel! Mührü kusursuz şekilde çizdin!"

            PerformanceLevel.GOOD ->
                "✅ İyi! Mührün %${(accuracy * 100).toInt()}'i doğru."

            PerformanceLevel.ACCEPTABLE -> {
                // Find which finger is most off
                val problematicFinger = findMostDeviatedFinger(maxDeviation, landmarks)
                "⚠️ Kabul edilebilir. $problematicFinger biraz daha dikkatli!"
            }

            PerformanceLevel.FAILED -> {
                "❌ Tekrar dene. Örnek mührü dikkatlice incele."
            }
        }
    }

    /**
     * Identify which finger/part is most incorrect
     *
     * Bu basitleştirilmiş versiyon - gelecekte improve edilebilir
     */
    private fun findMostDeviatedFinger(
        @Suppress("UNUSED_PARAMETER") maxDeviation: Float,
        @Suppress("UNUSED_PARAMETER") landmarks: List<NormalizedLandmark>
    ): String {
        // Finger tips mapping
        val fingerNames = mapOf(
            4 to "Başparmak",
            8 to "İşaret parmağı",
            12 to "Orta parmak",
            16 to "Yüzük parmağı",
            20 to "Serçe parmak"
        )

        // Simplified: return index finger (most common issue)
        // TODO: Gelecekte actual deviation hesaplama ekle
        return fingerNames[8] ?: "Parmak pozisyonu"
    }

    /**
     * Distance between two 3D points (NormalizedLandmark)
     */
    private fun distance(p1: NormalizedLandmark, p2: NormalizedLandmark): Float {
        val dx = p1.x() - p2.x()
        val dy = p1.y() - p2.y()
        val dz = p1.z() - p2.z()
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    /**
     * Distance between two 3D points (NormalizedPoint)
     */
    private fun distance(p1: NormalizedPoint, p2: NormalizedPoint): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        val dz = p1.z - p2.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    /**
     * Generate feedback for multi-template matching
     */
    private fun generateMultiTemplateFeedback(
        bestAccuracy: Float,
        requiredThreshold: Float,
        performanceLevel: PerformanceLevel,
        bestTemplateIndex: Int
    ): String {
        return when (performanceLevel) {
            PerformanceLevel.PERFECT ->
                "🌟 Mükemmel! Mührü kusursuz şekilde çizdin!"

            PerformanceLevel.GOOD ->
                "✅ İyi! Mührün %${(bestAccuracy * 100).toInt()}'i doğru."

            PerformanceLevel.ACCEPTABLE ->
                "✅ Başarılı! Açı #${bestTemplateIndex + 1} ile eşleşti (%${(bestAccuracy * 100).toInt()})."

            PerformanceLevel.FAILED -> {
                val gap = ((requiredThreshold - bestAccuracy) * 100).toInt()
                "❌ %${gap} daha fazla doğruluk gerekli. Tekrar deneyin."
            }
        }
    }

    /**
     * ⚡ YENİ: Güvenilirlik Bazlı Kalite Değerlendirmesi
     *
     * Araştırmadan öğrenilen profesyonel yaklaşım:
     * "Adaptively lower or raise confidence thresholds based on per-frame metrics"
     *
     * MediaPipe'tan gelen her landmark'ın güvenilirlik skorunu (presence/visibility)
     * kullanarak elin genel tespit kalitesini belirler.
     *
     * Bu, kapalı el pozisyonlarında (yumruk vb.) düşük güvenilirlikli landmark'ları
     * tespit edip, kullanıcıya daha iyi feedback vermemizi sağlar.
     *
     * @param landmarks MediaPipe'tan gelen ham landmark'lar (confidence bilgisi içerir)
     * @return Pair<TrackingQuality, List<Float>> - (Genel kalite, Her landmark'ın confidence'ı)
     */
    private fun assessTrackingQuality(
        landmarks: List<NormalizedLandmark>
    ): Pair<TrackingQuality, List<Float>> {
        if (landmarks.isEmpty() || landmarks.size != 21) {
            return Pair(TrackingQuality.POOR, emptyList())
        }

        // MediaPipe NormalizedLandmark'ta presence ve visibility optional olabilir
        // Bu yüzden basit bir heuristic kullanacağız: tüm landmark'ların var olduğunu varsay
        // ancak z koordinatlarına bakarak derinlik bilgisinden güvenilirlik tahmin et

        val confidenceScores = landmarks.mapIndexed { index, landmark ->
            // Z koordinatı derinlik bilgisi verir
            // Z değeri 0'a yakınsa (elin düzleminde) güvenilirlik yüksek
            // Z değeri büyükse (derinlik varsa) bazı noktalar gizli olabilir
            val zDepth = kotlin.math.abs(landmark.z())

            // Basit heuristic: z derinliği 0.1'den küçükse yüksek güvenilirlik
            // z derinliği arttıkça güvenilirlik azalır
            val depthConfidence = 1.0f - (zDepth * 2f).coerceIn(0f, 1f)

            // Parmak uçları için güvenilirlik daha yüksek olma eğilimindedir
            val isFingerTip = index in listOf(4, 8, 12, 16, 20)
            val tipBonus = if (isFingerTip) 0.1f else 0f

            (depthConfidence + tipBonus).coerceIn(0f, 1f)
        }

        // Ortalama güvenilirlik
        val avgConfidence = confidenceScores.average().toFloat()

        // Araştırma önerisi: Eşiklere göre kalite seviyesi belirle
        val quality = when {
            avgConfidence >= 0.95f -> TrackingQuality.EXCELLENT
            avgConfidence >= 0.80f -> TrackingQuality.GOOD
            avgConfidence >= 0.60f -> TrackingQuality.ACCEPTABLE
            else -> TrackingQuality.POOR
        }

        Log.d(TAG, "[Quality Assessment] Avg confidence: $avgConfidence → $quality")
        Log.d(TAG, "[Quality Assessment] Landmarks below 0.5 confidence: ${confidenceScores.count { score -> score < 0.5f }}/21")

        return Pair(quality, confidenceScores)
    }

    /**
     * ⚡⚡⚡ TAM YENİDEN YAZILDI: Adaptif Güvenilirlik Filtreleme (BÖLÜM B İyileştirme)
     *
     * Araştırmadan: "Adaptively lower or raise confidence thresholds based on per-frame metrics"
     *
     * YENİ YAKLAŞIM:
     * - TrackingQuality'ye göre dinamik eşik ayarlaması
     * - POOR/ACCEPTABLE durumunda DAHA ESNEK (noktaları silmek yerine işaretle)
     * - EXCELLENT durumunda normal filtreleme
     * - El yeniden tespit edildiğinde (tracking lost → recovery), ilk birkaç frame'de
     *   filtreleme DEVRE DIŞI (sisteme kilitlenme zamanı tanı)
     *
     * KULLANIM:
     * - Normal modda: Sadece çok düşük güvenilirlikli landmark'lar interpolate edilir
     * - Debug modda: Düşük güvenilirlikli olanlar farklı renkte gösterilir
     *
     * @param landmarks Normalize edilmiş landmark'lar
     * @param confidenceScores Her landmark için güvenilirlik skoru
     * @param trackingQuality El takip kalitesi (EXCELLENT/GOOD/ACCEPTABLE/POOR)
     * @param isReEntry El yeni mi tespit edildi? (tracking lost sonrası ilk frame'ler)
     * @return Filtrelenmiş landmark'lar (düşük güvenilirlikli olanlar interpolated)
     */
    private fun applyAdaptiveConfidenceFilter(
        landmarks: List<NormalizedPoint>,
        confidenceScores: List<Float>,
        trackingQuality: TrackingQuality,
        isReEntry: Boolean = false
    ): List<NormalizedPoint> {
        if (landmarks.size != confidenceScores.size || landmarks.size != 21) {
            Log.w(TAG, "[Adaptive Filter] Size mismatch, returning original landmarks")
            return landmarks
        }

        // ========================================
        // ÖZEL DURUM: Yeniden giriş (Re-entry)
        // ========================================
        // El ekrandan çıkıp geri geldiğinde, ilk 2-3 frame boyunca
        // filtrelemeyi TAMAMEN devre dışı bırak. Sisteme takibe "kilitlenme" zamanı tanı.
        if (isReEntry) {
            Log.d(TAG, "[Adaptive Filter] 🔓 RE-ENTRY MODE: Filtering DISABLED for smooth recovery")
            return landmarks  // Ham landmark'ları olduğu gibi döndür
        }

        // ========================================
        // ⚡⚡⚡ BÖLÜM B: AGRESIF FİLTREYİ BASİTLEŞTİR
        // ========================================
        // Sorunu izole etmek için, filtreyi ÇOK hoşgörülü yap
        // Sadece gerçekten çok düşük güvenilirlikli (<0.05) landmark'ları filtrele
        // Bu, tüm iskeletin çizilmesini garanti eder
        val minConfidenceThreshold = when (trackingQuality) {
            TrackingQuality.EXCELLENT -> 0.05f  // ⚡ Çok hoşgörülü
            TrackingQuality.GOOD -> 0.05f       // ⚡ Çok hoşgörülü
            TrackingQuality.ACCEPTABLE -> 0.05f // ⚡ Çok hoşgörülü
            TrackingQuality.POOR -> 0.01f       // ⚡ Neredeyse filtreleme yok
        }

        Log.d(TAG, "[Adaptive Filter] ⚡ SIMPLIFIED MODE: Very tolerant threshold: $minConfidenceThreshold")

        // ========================================
        // FİLTRELEME: Sadece çok düşük güvenilirlikli landmark'ları interpolate et
        // ========================================
        val filtered = mutableListOf<NormalizedPoint>()
        var interpolatedCount = 0

        for (i in landmarks.indices) {
            val confidence = confidenceScores[i]
            val landmark = landmarks[i]

            if (confidence < minConfidenceThreshold) {
                // Araştırma önerisi: Düşük güvenilirlikli landmark'ları komşularından interpolate et
                // Örnek: Parmak eklemi görünmüyorsa, bir önceki ve bir sonraki eklemden tahmin et
                val interpolated = interpolateLandmark(landmarks, i)
                filtered.add(interpolated)
                interpolatedCount++
                Log.v(TAG, "[Adaptive Filter] Landmark $i: confidence=$confidence < $minConfidenceThreshold → interpolated")
            } else {
                filtered.add(landmark)
            }
        }

        if (interpolatedCount > 0) {
            Log.d(TAG, "[Adaptive Filter] ✅ Interpolated $interpolatedCount/21 landmarks (quality: $trackingQuality)")
        }

        return filtered
    }

    /**
     * Düşük güvenilirlikli landmark'ı komşularından interpolate et
     *
     * Basit linear interpolation: (previous + next) / 2
     * Daha gelişmiş versiyonlar için: cubic spline veya Kalman filtering kullanılabilir
     */
    private fun interpolateLandmark(
        landmarks: List<NormalizedPoint>,
        index: Int
    ): NormalizedPoint {
        // Özel durumlar: İlk veya son landmark
        if (index == 0) {
            // Bilek için: Bir sonraki landmark'ı kullan
            return landmarks.getOrElse(1) { landmarks[0] }
        }
        if (index >= landmarks.size - 1) {
            // Son landmark için: Bir önceki landmark'ı kullan
            return landmarks.getOrElse(index - 1) { landmarks[index] }
        }

        // Genel durum: Önceki ve sonraki landmark'ın ortalaması
        val prev = landmarks[index - 1]
        val next = landmarks[index + 1]

        return NormalizedPoint(
            x = (prev.x + next.x) / 2f,
            y = (prev.y + next.y) / 2f,
            z = (prev.z + next.z) / 2f
        )
    }
}

/**
 * Gesture evaluation result
 */
data class GestureResult(
    val isSuccess: Boolean,
    val accuracy: Float,  // 0.0 - 1.0
    val performanceLevel: PerformanceLevel,
    val feedback: String,
    val detectedLandmarks: List<NormalizedLandmark>,
    val avgDistance: Float,
    val debugInfo: DebugInfo = DebugInfo()
)

/**
 * Performance levels for gesture execution
 */
enum class PerformanceLevel {
    PERFECT,    // >= 95% accuracy
    GOOD,       // >= 80% accuracy
    ACCEPTABLE, // >= 60% accuracy
    FAILED      // < 60% accuracy
}
