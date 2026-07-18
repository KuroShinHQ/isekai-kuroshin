package com.example.isekaikuroshin.ui.exercise

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.isekaikuroshin.data.NormalizedPoint
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.atan2

/**
 * ⚡⚡⚡ TAMAMEN YENİDEN YAZILDI: Gelişmiş Projeksiyon ve Stabilizasyon
 *
 * Bu Composable, "Kadim Mühür" modülünden öğrenilen en gelişmiş teknikleri
 * "Fiziksel Antrenman" modülüne uygular:
 *
 * 1. İKİLİ VERİ SİSTEMİ:
 *    - RAW landmarks: Ekran konumu için (nerede çizilecek)
 *    - NORMALIZED landmarks: Doğru şekil için (nasıl çizilecek)
 *
 * 2. ADVANCED PROJECTION:
 *    - Normalize iskeletin ham landmark pozisyonlarına projeksiyon
 *    - Dönüşüm matrisi (translation, rotation, scale)
 *
 * 3. EMA SMOOTHING:
 *    - Projekte edilmiş koordinatlara hafif yumuşatma
 *    - "Pırpır etme" sorununu ortadan kaldırır
 *
 * KULLANIM:
 * ```
 * PoseOverlay(
 *     pose = poseResult.smoothedPose,           // Ham landmark'lar (konum için)
 *     normalizedLandmarks = poseResult.normalizedLandmarks,  // Normalize veri (şekil için)
 *     imageWidth = imageProxy.width,
 *     imageHeight = imageProxy.height,
 *     viewWidth = size.width,
 *     viewHeight = size.height,
 *     formQuality = formQuality
 * )
 * ```
 */
@Composable
fun PoseOverlay(
    pose: Pose?,
    normalizedLandmarks: List<NormalizedPoint>,
    imageWidth: Int,
    imageHeight: Int,
    viewWidth: Float,
    viewHeight: Float,
    modifier: Modifier = Modifier,
    formQuality: FormQuality = FormQuality.NEUTRAL,
    relevantLandmarkTypes: Set<Int> = emptySet()  // ⚡ YENİ: Egzersize özel landmark filtreleme
) {
    if (pose == null || pose.allPoseLandmarks.isEmpty()) {
        return
    }

    // ═══ PERFORMANCE: Skeleton bağlantıları cache'le ═══
    val connections = remember {
        listOf(
            // Sol kol
            Pair(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW),
            Pair(PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST),

            // Sağ kol
            Pair(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW),
            Pair(PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST),

            // Omuzlar
            Pair(PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER),

            // Gövde
            Pair(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP),
            Pair(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP),

            // Kalçalar
            Pair(PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP),

            // Sol bacak
            Pair(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE),
            Pair(PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE),

            // Sağ bacak
            Pair(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE),
            Pair(PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE)
        )
    }

    // ═══ MYSTICAL ENERGY ANIMATION ═══
    val infiniteTransition = rememberInfiniteTransition(label = "mystical_energy")
    val energyPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "energy_pulse"
    )

    val glowIntensity by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_intensity"
    )

    // ═══ PERFORMANCE: Sabit değerleri cache'le ═══
    val strokeWidth = remember { 16f }
    val jointRadius = remember { 18f }

    // ═══ RENK: Form kalitesine göre ═══
    val baseColor = remember(formQuality) {
        when (formQuality) {
            FormQuality.EXCELLENT, FormQuality.GOOD -> Color(0xFF00FF88)
            FormQuality.NEUTRAL -> Color(0xFFFFD700)
            FormQuality.POOR -> Color(0xFFFF6600)
            FormQuality.INVALID -> Color(0xFFFF0044)
        }
    }

    val coreColor = remember { Color.White.copy(alpha = 0.9f) }

    // ⚡⚡⚡ EMA SMOOTHING STATE (Titreme önleme)
    var smoothedPositions by remember { mutableStateOf<List<Offset>?>(null) }

    Canvas(modifier = modifier.fillMaxSize()) {
        // TODO-G122: İskelet koordinat mapping düzeltmesi
        // El Hareketleri ekranından alınan gelişmiş transformation tekniği

        // ========================================
        // STEP 1: HAM LANDMARK'LARI EKRAN KOORDİNATLARINA DÖNÜŞTÜR
        // ADVANCED: Aspect ratio korumalı, offset'li, rotation destekli
        // ========================================
        val rawScreenPositions = pose.allPoseLandmarks.map { landmark ->
            transformPoseLandmarkToScreen(
                landmark = landmark,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                canvasWidth = size.width,
                canvasHeight = size.height,
                rotationDegrees = 0,  // Genellikle 0 veya 90
                isFrontCamera = false  // Arka kamera (şınav/ip atlama)
            )
        }

        // ========================================
        // STEP 2: ADVANCED PROJECTION (Normalize → Screen)
        // ========================================
        val projectedPositions = if (normalizedLandmarks.isNotEmpty() && normalizedLandmarks.size == rawScreenPositions.size) {
            projectNormalizedToScreen(
                normalizedLandmarks = normalizedLandmarks,
                rawScreenPositions = rawScreenPositions,
                canvasWidth = size.width,
                canvasHeight = size.height
            )
        } else {
            // Fallback: Normalize veri yoksa ham pozisyonları kullan
            rawScreenPositions
        }

        // ========================================
        // STEP 3: EMA SMOOTHING (Titreme önleme)
        // ========================================
        val finalPositions = if (smoothedPositions == null) {
            // İlk frame: Smoothing yok, doğrudan kullan
            smoothedPositions = projectedPositions
            projectedPositions
        } else {
            // EMA smoothing uygula
            // ⚡ BÖLÜM C.2: Alpha 0.3 → 0.5 (Daha güçlü smoothing)
            val alpha = 0.5f  // Smoothing faktörü (0 = çok smooth, 1 = hiç smooth yok)
            val smoothed = projectedPositions.mapIndexed { index, newPos ->
                val oldPos = smoothedPositions!!.getOrNull(index) ?: newPos
                Offset(
                    x = oldPos.x * (1 - alpha) + newPos.x * alpha,
                    y = oldPos.y * (1 - alpha) + newPos.y * alpha
                )
            }
            smoothedPositions = smoothed
            smoothed
        }

        // ========================================
        // STEP 4: ÇİZİM (3 Katmanlı Glow Efekti)
        // ========================================
        val glowColor = baseColor.copy(alpha = glowIntensity)

        // ═══ MYSTICAL SKELETON LINES ═══
        connections.forEach { (fromType, toType) ->
            val fromLandmark = pose.getPoseLandmark(fromType)
            val toLandmark = pose.getPoseLandmark(toType)

            if (fromLandmark != null && toLandmark != null &&
                fromLandmark.inFrameLikelihood > 0.5f &&
                toLandmark.inFrameLikelihood > 0.5f
            ) {
                val fromIndex = fromLandmark.landmarkType
                val toIndex = toLandmark.landmarkType

                // ⚡ BÖLÜM A: Egzersize özel landmark filtreleme
                // Eğer relevantLandmarkTypes set'i doluysa, sadece ilgili landmark'ları çiz
                val shouldDraw = if (relevantLandmarkTypes.isEmpty()) {
                    true  // Filtre yoksa tümünü çiz
                } else {
                    // Her iki endpoint de ilgili landmark'lardan olmalı
                    fromIndex in relevantLandmarkTypes && toIndex in relevantLandmarkTypes
                }

                if (shouldDraw && fromIndex < finalPositions.size && toIndex < finalPositions.size) {
                    val startPos = finalPositions[fromIndex]
                    val endPos = finalPositions[toIndex]

                    // LAYER 1: Outer Glow (Dış Işıltı)
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                glowColor.copy(alpha = 0.3f),
                                glowColor.copy(alpha = 0.6f),
                                glowColor.copy(alpha = 0.3f)
                            )
                        ),
                        start = startPos,
                        end = endPos,
                        strokeWidth = strokeWidth + 12f,
                        cap = StrokeCap.Round,
                        blendMode = BlendMode.Plus
                    )

                    // LAYER 2: Middle Energy
                    drawLine(
                        color = baseColor.copy(alpha = 0.8f),
                        start = startPos,
                        end = endPos,
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )

                    // LAYER 3: Inner Core (Beyaz ışık)
                    drawLine(
                        color = coreColor,
                        start = startPos,
                        end = endPos,
                        strokeWidth = strokeWidth * 0.4f,
                        cap = StrokeCap.Round,
                        blendMode = BlendMode.Screen
                    )
                }
            }
        }

        // ═══ MYSTICAL JOINTS ═══
        pose.allPoseLandmarks.forEach { landmark ->
            // ⚡ BÖLÜM A: Egzersize özel landmark filtreleme
            val shouldDraw = if (relevantLandmarkTypes.isEmpty()) {
                true  // Filtre yoksa tümünü çiz
            } else {
                landmark.landmarkType in relevantLandmarkTypes
            }

            if (shouldDraw && landmark.inFrameLikelihood > 0.5f && landmark.landmarkType < finalPositions.size) {
                val center = finalPositions[landmark.landmarkType]

                // LAYER 1: Energy Orb Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.6f),
                            glowColor.copy(alpha = 0.3f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = jointRadius + 20f
                    ),
                    radius = jointRadius + 20f,
                    center = center,
                    blendMode = BlendMode.Plus
                )

                // LAYER 2: Outer Ring
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            baseColor.copy(alpha = 0.8f),
                            baseColor.copy(alpha = 0.4f),
                            baseColor.copy(alpha = 0.8f)
                        ),
                        center = center
                    ),
                    radius = jointRadius + 6f,
                    center = center,
                    style = Stroke(width = 3f)
                )

                // LAYER 3: Middle Energy
                drawCircle(
                    color = baseColor.copy(alpha = 0.9f),
                    radius = jointRadius,
                    center = center
                )

                // LAYER 4: Inner Core
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 1.0f),
                            coreColor.copy(alpha = 0.8f),
                            baseColor.copy(alpha = 0.6f)
                        ),
                        center = center,
                        radius = jointRadius * 0.6f
                    ),
                    radius = jointRadius * 0.6f,
                    center = center,
                    blendMode = BlendMode.Screen
                )

                // LAYER 5: Rotating Energy Particles
                for (i in 0..3) {
                    val angle = (energyPulse + (i * 90f)) * (Math.PI / 180f).toFloat()
                    val particleRadius = jointRadius + 12f
                    val particleX = center.x + particleRadius * cos(angle)
                    val particleY = center.y + particleRadius * sin(angle)

                    drawCircle(
                        color = glowColor.copy(alpha = 0.7f),
                        radius = 3f,
                        center = Offset(particleX, particleY),
                        blendMode = BlendMode.Plus
                    )
                }
            }
        }
    }
}

/**
 * ⚡⚡⚡ ÇEKİRDEK FONKSİYON: Normalize Veriyi Ekran Koordinatlarına Projekte Et
 *
 * Bu fonksiyon, "Kadim Mühür" modülünde başarıyla kullanılan projectNormalizedToScreen()
 * fonksiyonunun pose skeleton için uyarlanmış versiyonudur.
 *
 * ALGORİTMA:
 * 1. Raw landmark'lardan referans noktaları al (kalça merkezi ve omuz merkezi)
 * 2. Normalize landmark'lardan referans noktaları al
 * 3. Dönüşüm matrisini hesapla (translation, rotation, scale)
 * 4. Tüm normalize noktaları bu matris ile ekrana project et
 *
 * @param normalizedLandmarks PoseEstimationEngine'den gelen normalize veri (doğru şekil)
 * @param rawScreenPositions Ham landmark'ların ekran pozisyonları (doğru konum)
 * @param canvasWidth Çizim alanı genişliği
 * @param canvasHeight Çizim alanı yüksekliği
 * @return Ekran koordinatlarına project edilmiş landmark pozisyonları
 */
private fun projectNormalizedToScreen(
    normalizedLandmarks: List<NormalizedPoint>,
    rawScreenPositions: List<Offset>,
    canvasWidth: Float,
    canvasHeight: Float
): List<Offset> {
    if (normalizedLandmarks.size != 33 || rawScreenPositions.size != 33) {
        android.util.Log.e("PoseOverlay", "⚠️ Projection failed: size mismatch (expected 33)")
        return rawScreenPositions  // Fallback
    }

    // ========================================
    // STEP 1: Referans Noktalarını Al
    // ========================================
    // Pose için referans noktaları: Kalça merkezi ve omuz merkezi
    val leftHipIndex = 23   // PoseLandmark.LEFT_HIP
    val rightHipIndex = 24  // PoseLandmark.RIGHT_HIP
    val leftShoulderIndex = 11   // PoseLandmark.LEFT_SHOULDER
    val rightShoulderIndex = 12  // PoseLandmark.RIGHT_SHOULDER

    // Raw (ekran koordinatlarında)
    val rawLeftHip = rawScreenPositions[leftHipIndex]
    val rawRightHip = rawScreenPositions[rightHipIndex]
    val rawHipsCenter = Offset(
        (rawLeftHip.x + rawRightHip.x) / 2f,
        (rawLeftHip.y + rawRightHip.y) / 2f
    )

    val rawLeftShoulder = rawScreenPositions[leftShoulderIndex]
    val rawRightShoulder = rawScreenPositions[rightShoulderIndex]
    val rawShouldersCenter = Offset(
        (rawLeftShoulder.x + rawRightShoulder.x) / 2f,
        (rawLeftShoulder.y + rawRightShoulder.y) / 2f
    )

    // Normalized (world space'te)
    val normLeftHip = normalizedLandmarks[leftHipIndex]
    val normRightHip = normalizedLandmarks[rightHipIndex]
    val normHipsCenter = NormalizedPoint(
        (normLeftHip.x + normRightHip.x) / 2f,
        (normLeftHip.y + normRightHip.y) / 2f,
        (normLeftHip.z + normRightHip.z) / 2f
    )

    val normLeftShoulder = normalizedLandmarks[leftShoulderIndex]
    val normRightShoulder = normalizedLandmarks[rightShoulderIndex]
    val normShouldersCenter = NormalizedPoint(
        (normLeftShoulder.x + normRightShoulder.x) / 2f,
        (normLeftShoulder.y + normRightShoulder.y) / 2f,
        (normLeftShoulder.z + normRightShoulder.z) / 2f
    )

    // ========================================
    // STEP 2: Ölçek Hesapla
    // ========================================
    val rawDistance = sqrt(
        (rawShouldersCenter.x - rawHipsCenter.x) * (rawShouldersCenter.x - rawHipsCenter.x) +
        (rawShouldersCenter.y - rawHipsCenter.y) * (rawShouldersCenter.y - rawHipsCenter.y)
    )

    val normDistance = sqrt(
        (normShouldersCenter.x - normHipsCenter.x) * (normShouldersCenter.x - normHipsCenter.x) +
        (normShouldersCenter.y - normHipsCenter.y) * (normShouldersCenter.y - normHipsCenter.y)
    )

    if (normDistance < 0.001f) {
        android.util.Log.e("PoseOverlay", "⚠️ Normalized distance too small")
        return rawScreenPositions
    }

    val scale = rawDistance / normDistance

    // ========================================
    // STEP 3: Rotasyon Hesapla
    // ========================================
    val rawAngle = atan2(
        (rawShouldersCenter.y - rawHipsCenter.y).toDouble(),
        (rawShouldersCenter.x - rawHipsCenter.x).toDouble()
    ).toFloat()

    val normAngle = atan2(
        (normShouldersCenter.y - normHipsCenter.y).toDouble(),
        (normShouldersCenter.x - normHipsCenter.x).toDouble()
    ).toFloat()

    val rotationAngle = rawAngle - normAngle

    val cosAngle = cos(rotationAngle)
    val sinAngle = sin(rotationAngle)

    // ========================================
    // STEP 4: Tüm Noktaları Project Et
    // ========================================
    return normalizedLandmarks.map { point ->
        // Normalize koordinattan başla
        var x = point.x
        var y = point.y

        // 1. Ölçekle
        x *= scale
        y *= scale

        // 2. Döndür
        val rotatedX = x * cosAngle - y * sinAngle
        val rotatedY = x * sinAngle + y * cosAngle

        // 3. Raw kalça pozisyonuna kaydır
        val screenX = rotatedX + rawHipsCenter.x
        val screenY = rotatedY + rawHipsCenter.y

        Offset(screenX, screenY)
    }
}

/**
 * TODO-G122: El Hareketleri ekranından uyarlanan gelişmiş koordinat transformation
 *
 * ML Kit Pose landmark'ını ekran koordinatlarına dönüştürür.
 * Rotasyon, aspect ratio ve mirror efekti dikkate alınır.
 *
 * Kaynak: HandLandmarkOverlay.kt - transformLandmarkToScreen()
 * Uyarlama: Pose skeleton için optimize edildi
 */
private fun transformPoseLandmarkToScreen(
    landmark: PoseLandmark,
    imageWidth: Int,
    imageHeight: Int,
    canvasWidth: Float,
    canvasHeight: Float,
    rotationDegrees: Int,
    isFrontCamera: Boolean
): Offset {
    // 1. ML Kit landmark pozisyonunu al (zaten piksel koordinatlarında)
    var x = landmark.position.x
    var y = landmark.position.y

    // 2. Rotasyonu uygula
    data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    val (rotatedX, rotatedY, rotatedWidth, rotatedHeight) = when (rotationDegrees) {
        0 -> {
            // Rotasyon yok
            Tuple4(x, y, imageWidth.toFloat(), imageHeight.toFloat())
        }
        90 -> {
            // 90° saat yönünde rotasyon
            Tuple4(
                y,
                imageWidth.toFloat() - x,
                imageHeight.toFloat(),
                imageWidth.toFloat()
            )
        }
        180 -> {
            // 180° rotasyon
            Tuple4(
                imageWidth.toFloat() - x,
                imageHeight.toFloat() - y,
                imageWidth.toFloat(),
                imageHeight.toFloat()
            )
        }
        270 -> {
            // 270° saat yönünde rotasyon
            Tuple4(
                imageHeight.toFloat() - y,
                x,
                imageHeight.toFloat(),
                imageWidth.toFloat()
            )
        }
        else -> {
            android.util.Log.w("PoseOverlay", "⚠️ Beklenmeyen rotasyon: $rotationDegrees°")
            Tuple4(x, y, imageWidth.toFloat(), imageHeight.toFloat())
        }
    }

    // 3. Ön kamera için ayna efekti (genellikle şınav/ip atlama için arka kamera)
    var mirroredX = rotatedX
    var mirroredY = rotatedY

    if (isFrontCamera) {
        mirroredX = rotatedWidth - rotatedX
        // Y ekseninde mirror genellikle gerekmiyor
    }

    // 4. Aspect ratio korunarak ölçekleme ve offset hesapla
    // BU KISIM KRİTİK: El Hareketleri ekranındaki başarılı teknik!
    val srcAspect = rotatedWidth / rotatedHeight
    val dstAspect = canvasWidth / canvasHeight

    val (scale, offsetX, offsetY) = if (srcAspect > dstAspect) {
        // Kaynak daha geniş - yatay pillarbox (üst-alt boşluk)
        val s = canvasWidth / rotatedWidth
        val scaledHeight = rotatedHeight * s
        val offY = (canvasHeight - scaledHeight) / 2f
        Triple(s, 0f, offY)
    } else {
        // Kaynak daha dar - dikey letterbox (sağ-sol boşluk)
        val s = canvasHeight / rotatedHeight
        val scaledWidth = rotatedWidth * s
        val offX = (canvasWidth - scaledWidth) / 2f
        Triple(s, offX, 0f)
    }

    // 5. Ölçekleme ve offset uygula - FİNAL KOORDİNATLAR
    val screenX = mirroredX * scale + offsetX
    val screenY = mirroredY * scale + offsetY

    return Offset(screenX, screenY)
}

