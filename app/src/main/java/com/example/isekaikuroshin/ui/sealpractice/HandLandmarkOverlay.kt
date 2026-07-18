package com.example.isekaikuroshin.ui.sealpractice

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

/**
 * Hand Landmark Overlay
 *
 * Real-time olarak el iskeletini kamera önizlemesi üzerine çizer.
 *
 * MediaPipe Hands 21 landmark noktası:
 * 0: Wrist (Bilek)
 * 1-4: Thumb (Başparmak)
 * 5-8: Index (İşaret parmağı)
 * 9-12: Middle (Orta parmak)
 * 13-16: Ring (Yüzük parmağı)
 * 17-20: Pinky (Serçe parmak)
 */
@Composable
fun HandLandmarkOverlay(
    handResult: HandLandmarkerResult?,
    imageWidth: Int,
    imageHeight: Int,
    viewWidth: Float,
    viewHeight: Float,
    rotationDegrees: Int = 0,
    isFrontCamera: Boolean = true,
    modifier: Modifier = Modifier
) {
    // ⚡⚡⚡ KÖKTƏN ÇÖZÜM: Tüm filtreleme katmanları KALDIRILDI!
    // MediaPipe'dan gelen RAW veriyi direkt çiz - ZERO filtreleme!

    // ⚡⚡⚡ CRITICAL DEBUG LOG for double hand drawing
    val handCount = handResult?.landmarks()?.size ?: 0
    android.util.Log.e("LandmarkOverlay", "🎨🎨🎨 DRAWING ${handCount} HAND(S)! ImageSize: ${imageWidth}x${imageHeight}, Rotation: ${rotationDegrees}°")

    if (handResult == null || handResult.landmarks().isEmpty()) {
        android.util.Log.e("LandmarkOverlay", "❌ NO HANDS TO DRAW!")
        return
    }

    if (handCount > 1) {
        android.util.Log.e("LandmarkOverlay", "✅✅ DOUBLE HAND DETECTED! Will draw BOTH hands (green + blue)")
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        // DEBUG: ImageProxy ve Canvas metadata'yı ekranda göster
        val debugLines = listOf(
            "ImageProxy: ${imageWidth}x${imageHeight}",
            "Rotation: ${rotationDegrees}°",
            "Canvas: ${size.width.toInt()}x${size.height.toInt()}",
            "Hands: ${handResult.landmarks().size}",  // ⚡ FIX: El sayısını göster
            "Mode: RAW (21/21 points)"
        )

        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 35f
            isAntiAlias = true
            setShadowLayer(8f, 0f, 0f, android.graphics.Color.BLACK)
        }

        debugLines.forEachIndexed { index, line ->
            drawContext.canvas.nativeCanvas.drawText(
                line,
                20f,
                60f + (index * 40f),
                paint
            )
        }

        // ⚡⚡⚡ FIX: TÜM ELLERİ ÇİZ (Sadece ilk el değil!)
        // MediaPipe birden fazla el algıladıysa hepsini göster!
        handResult.landmarks().forEachIndexed { handIndex, rawHand ->
            android.util.Log.d("HandOverlay", "🖐️ Drawing hand #${handIndex + 1}")

            // Raw landmark'ları ekran koordinatlarına dönüştür
            val rawScreenPositions = rawHand.mapIndexed { index, landmark ->
                transformLandmarkToScreen(
                    landmark, imageWidth, imageHeight,
                    size.width, size.height,
                    rotationDegrees, isFrontCamera
                )
            }

            // Her el için farklı renk (ilk el yeşil, ikinci el mavi)
            val handColor = if (handIndex == 0) Color(0xFF00FF88) else Color(0xFF00B4FF)

            // Tüm 21 noktayı çiz (hiçbir filtreleme yok!)
            rawScreenPositions.forEachIndexed { index, position ->
                // Ana nokta
                drawCircle(
                    color = handColor,
                    radius = 8f,
                    center = position
                )

                // İç parlama efekti
                drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = position
                )
            }

            // Bağlantı çizgilerini çiz (tüm 21 nokta kullanılır)
            drawHandConnectionsRaw(rawScreenPositions, handColor)
        }
    }
}

/**
 * MediaPipe landmark'ını ekran koordinatlarına dönüştürür.
 * Rotasyon, aspect ratio ve mirror efekti dikkate alınır.
 */
private fun transformLandmarkToScreen(
    landmark: com.google.mediapipe.tasks.components.containers.NormalizedLandmark,
    imageWidth: Int,
    imageHeight: Int,
    canvasWidth: Float,
    canvasHeight: Float,
    rotationDegrees: Int,
    isFrontCamera: Boolean
): Offset {
    // 1. Normalize koordinatları ImageProxy koordinatlarına dönüştür
    var x = landmark.x() * imageWidth.toFloat()
    var y = landmark.y() * imageHeight.toFloat()

    // 2. Rotasyonu uygula
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
            android.util.Log.w("HandOverlay", "⚠️ Beklenmeyen rotasyon: $rotationDegrees°")
            Tuple4(x, y, imageWidth.toFloat(), imageHeight.toFloat())
        }
    }

    // 3. Ön kamera için ayna efektini rotasyondan sonra, ölçeklemeden ÖNCE uygula
    // TODO-G117: Gerçek cihazda X ekseni mirror sorunu düzeltildi
    // Sadece Y ekseninde mirror yapılıyor (dikey flip), X ekseni olduğu gibi kalıyor
    var mirroredX = rotatedX
    var mirroredY = rotatedY

    if (isFrontCamera) {
        // mirroredX = rotatedWidth - rotatedX  // ❌ KALDIRILDI: Sağ/sol karışıklığına neden oluyordu
        mirroredY = rotatedHeight - mirroredY // ✅ Sadece dikey flip (yukarı/aşağı düzeltme)
    }

    // 4. Aspect ratio korunarak ölçekleme ve offset hesapla
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

    // 5. Ölçekleme ve offset uygula
    val screenX = mirroredX * scale + offsetX
    val screenY = mirroredY * scale + offsetY

    return Offset(screenX, screenY)
}

// Internal to allow HandLandmarkProcessor to use it
internal data class Tuple4<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

/**
 * El bağlantılarını çiz
 *
 * MediaPipe el iskelet yapısı:
 * - Palm (Avuç içi): 0-1, 0-5, 0-9, 0-13, 0-17
 * - Fingers (Parmaklar): Her parmak 4 segment
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHandConnections(
    hand: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>,
    imageWidth: Int,
    imageHeight: Int,
    rotationDegrees: Int,
    isFrontCamera: Boolean
) {
    // El bağlantı indeksleri (başlangıç, bitiş)
    val connections = listOf(
        // Bilek -> Palm
        Pair(0, 1),   // Wrist -> Thumb CMC
        Pair(0, 5),   // Wrist -> Index MCP
        Pair(0, 9),   // Wrist -> Middle MCP
        Pair(0, 13),  // Wrist -> Ring MCP
        Pair(0, 17),  // Wrist -> Pinky MCP

        // Palm çizgileri
        Pair(5, 9),   // Index MCP -> Middle MCP
        Pair(9, 13),  // Middle MCP -> Ring MCP
        Pair(13, 17), // Ring MCP -> Pinky MCP

        // Başparmak (Thumb)
        Pair(1, 2),   // CMC -> MCP
        Pair(2, 3),   // MCP -> IP
        Pair(3, 4),   // IP -> TIP

        // İşaret parmağı (Index)
        Pair(5, 6),   // MCP -> PIP
        Pair(6, 7),   // PIP -> DIP
        Pair(7, 8),   // DIP -> TIP

        // Orta parmak (Middle)
        Pair(9, 10),  // MCP -> PIP
        Pair(10, 11), // PIP -> DIP
        Pair(11, 12), // DIP -> TIP

        // Yüzük parmağı (Ring)
        Pair(13, 14), // MCP -> PIP
        Pair(14, 15), // PIP -> DIP
        Pair(15, 16), // DIP -> TIP

        // Serçe parmak (Pinky)
        Pair(17, 18), // MCP -> PIP
        Pair(18, 19), // PIP -> DIP
        Pair(19, 20)  // DIP -> TIP
    )

    connections.forEach { (startIdx, endIdx) ->
        if (startIdx < hand.size && endIdx < hand.size) {
            val start = hand[startIdx]
            val end = hand[endIdx]

            val startPos = transformLandmarkToScreen(
                landmark = start,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                canvasWidth = size.width,
                canvasHeight = size.height,
                rotationDegrees = rotationDegrees,
                isFrontCamera = isFrontCamera
            )

            val endPos = transformLandmarkToScreen(
                landmark = end,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                canvasWidth = size.width,
                canvasHeight = size.height,
                rotationDegrees = rotationDegrees,
                isFrontCamera = isFrontCamera
            )

            // Çizgi çiz (glow efekti ile)
            drawLine(
                color = Color(0xFF00FF88).copy(alpha = 0.3f),
                start = startPos,
                end = endPos,
                strokeWidth = 12f,
                cap = StrokeCap.Round
            )

            drawLine(
                color = Color(0xFF00FF88),
                start = startPos,
                end = endPos,
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Processed landmark'lar için bağlantı çizimi (perspektif-düzeltilmiş)
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHandConnectionsProcessed(
    processedLandmarks: List<ProcessedLandmark>,
    processor: HandLandmarkProcessor
) {
    if (processedLandmarks.size < 21) return

    // El bağlantı indeksleri (başlangıç, bitiş)
    val connections = listOf(
        // Bilek -> Palm
        Pair(0, 1), Pair(0, 5), Pair(0, 9), Pair(0, 13), Pair(0, 17),
        // Palm çizgileri
        Pair(5, 9), Pair(9, 13), Pair(13, 17),
        // Başparmak
        Pair(1, 2), Pair(2, 3), Pair(3, 4),
        // İşaret parmağı
        Pair(5, 6), Pair(6, 7), Pair(7, 8),
        // Orta parmak
        Pair(9, 10), Pair(10, 11), Pair(11, 12),
        // Yüzük parmağı
        Pair(13, 14), Pair(14, 15), Pair(15, 16),
        // Serçe parmak
        Pair(17, 18), Pair(18, 19), Pair(19, 20)
    )

    connections.forEach { (startIdx, endIdx) ->
        if (startIdx < processedLandmarks.size && endIdx < processedLandmarks.size) {
            val start = processedLandmarks[startIdx]
            val end = processedLandmarks[endIdx]

            // Outlier olmayan landmarkları çiz
            if (!start.wasFiltered && !end.wasFiltered) {
                val avgScale = (processor.getPerspectiveScale(startIdx) +
                               processor.getPerspectiveScale(endIdx)) / 2f

                // Çizgi çiz (glow efekti ile, perspektif-düzeltilmiş kalınlık)
                drawLine(
                    color = Color(0xFF00FF88).copy(alpha = 0.3f),
                    start = start.position,
                    end = end.position,
                    strokeWidth = 12f * avgScale,
                    cap = StrokeCap.Round
                )

                drawLine(
                    color = Color(0xFF00FF88),
                    start = start.position,
                    end = end.position,
                    strokeWidth = 6f * avgScale,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * Template Comparison Overlay
 *
 * Beklenen mühür şablonunu overlay olarak gösterir (ghost mode)
 */
@Composable
fun TemplateOverlay(
    templateLandmarks: List<com.example.isekaikuroshin.data.NormalizedPoint>,
    imageWidth: Int,
    imageHeight: Int,
    viewWidth: Float,
    viewHeight: Float,
    modifier: Modifier = Modifier
) {
    if (templateLandmarks.isEmpty()) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val scaleX = size.width / imageWidth.toFloat()
        val scaleY = size.height / imageHeight.toFloat()

        // Merkezi hesapla (bilek noktası)
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        // Template landmark'larını çiz (yarı saydam)
        templateLandmarks.forEach { point ->
            val x = centerX + (point.x * 200f) // 200f = scale factor
            val y = centerY + (point.y * 200f)

            // Ghost nokta çiz
            drawCircle(
                color = Color(0xFFFFD700).copy(alpha = 0.3f),
                radius = 10f,
                center = Offset(x, y)
            )

            // İç nokta
            drawCircle(
                color = Color(0xFFFFD700).copy(alpha = 0.5f),
                radius = 5f,
                center = Offset(x, y)
            )
        }

        // Template bağlantılarını çiz
        drawTemplateConnections(templateLandmarks, centerX, centerY)
    }
}

/**
 * Template bağlantılarını çiz
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTemplateConnections(
    landmarks: List<com.example.isekaikuroshin.data.NormalizedPoint>,
    centerX: Float,
    centerY: Float
) {
    val connections = listOf(
        Pair(0, 1), Pair(0, 5), Pair(0, 9), Pair(0, 13), Pair(0, 17),
        Pair(5, 9), Pair(9, 13), Pair(13, 17),
        Pair(1, 2), Pair(2, 3), Pair(3, 4),
        Pair(5, 6), Pair(6, 7), Pair(7, 8),
        Pair(9, 10), Pair(10, 11), Pair(11, 12),
        Pair(13, 14), Pair(14, 15), Pair(15, 16),
        Pair(17, 18), Pair(18, 19), Pair(19, 20)
    )

    connections.forEach { (startIdx, endIdx) ->
        if (startIdx < landmarks.size && endIdx < landmarks.size) {
            val start = landmarks[startIdx]
            val end = landmarks[endIdx]

            val startX = centerX + (start.x * 200f)
            val startY = centerY + (start.y * 200f)
            val endX = centerX + (end.x * 200f)
            val endY = centerY + (end.y * 200f)

            // Ghost çizgi çiz
            drawLine(
                color = Color(0xFFFFD700).copy(alpha = 0.2f),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )

            drawLine(
                color = Color(0xFFFFD700).copy(alpha = 0.4f),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Accuracy Indicator Overlay
 *
 * Doğruluk oranını visual olarak gösterir
 */
@Composable
fun AccuracyIndicatorOverlay(
    accuracy: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = 150f

        // Arka plan çemberi
        drawCircle(
            color = Color.White.copy(alpha = 0.1f),
            radius = radius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 20f)
        )

        // Doğruluk çemberi
        val sweepAngle = 360f * accuracy
        drawArc(
            color = getAccuracyColorForIndicator(accuracy),
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(centerX - radius, centerY - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = 20f, cap = StrokeCap.Round)
        )
    }
}

private fun getAccuracyColorForIndicator(accuracy: Float): Color {
    return when {
        accuracy >= 0.9f -> Color(0xFF00FF88)
        accuracy >= 0.7f -> Color(0xFFFFD700)
        accuracy >= 0.5f -> Color(0xFFFF9800)
        else -> Color(0xFFFF5252)
    }
}

/**
 * ⚡⚡⚡核心FONKSYON: Normalize Veriyi Ekran Koordinatlarına Projekte Et
 *
 * Bu fonksiyon, "mavi iskeletin beynini" (normalize şekil) alıp,
 * "yeşil iskeletin gözleriyle" (ekran konumu) birleştirir.
 *
 * ALGORİTMA:
 * 1. Raw landmark'lardan referans noktaları al (bilek ve orta parmak)
 * 2. Normalize landmark'lardan referans noktaları al
 * 3. Dönüşüm matrisini hesapla (translation, rotation, scale)
 * 4. Tüm normalize noktaları bu matris ile ekrana project et
 *
 * @param normalizedLandmarks GestureEngine'den gelen normalize veri (doğru şekil)
 * @param rawScreenPositions Ham landmark'ların ekran pozisyonları (doğru konum)
 * @param canvasWidth Çizim alanı genişliği
 * @param canvasHeight Çizim alanı yüksekliği
 * @return Ekran koordinatlarına project edilmiş landmark pozisyonları
 */
private fun projectNormalizedToScreen(
    normalizedLandmarks: List<com.example.isekaikuroshin.data.NormalizedPoint>,
    rawScreenPositions: List<Offset>,
    canvasWidth: Float,
    canvasHeight: Float
): List<Offset> {
    if (normalizedLandmarks.size != 21 || rawScreenPositions.size != 21) {
        android.util.Log.e("HandOverlay", "⚠️ Projection failed: size mismatch")
        return rawScreenPositions  // Fallback to raw positions
    }

    // ========================================
    // STEP 1: Referans Noktalarını Al
    // ========================================
    // Raw (ekran koordinatlarında)
    val rawWrist = rawScreenPositions[0]         // Bilek
    val rawMiddle = rawScreenPositions[12]       // Orta parmak ucu

    // Normalized (world space'te)
    val normWrist = normalizedLandmarks[0]       // Bilek (origin'de olmalı)
    val normMiddle = normalizedLandmarks[12]     // Orta parmak ucu

    // ========================================
    // STEP 2: Ölçek Hesapla
    // ========================================
    // Raw landmark'ların ekrandaki mesafesi
    val rawDistance = kotlin.math.sqrt(
        (rawMiddle.x - rawWrist.x) * (rawMiddle.x - rawWrist.x) +
        (rawMiddle.y - rawWrist.y) * (rawMiddle.y - rawWrist.y)
    )

    // Normalize landmark'ların world space'teki mesafesi
    val normDistance = kotlin.math.sqrt(
        (normMiddle.x - normWrist.x) * (normMiddle.x - normWrist.x) +
        (normMiddle.y - normWrist.y) * (normMiddle.y - normWrist.y)
    )

    if (normDistance < 0.001f) {
        android.util.Log.e("HandOverlay", "⚠️ Normalized distance too small")
        return rawScreenPositions
    }

    // Ölçek faktörü: Ekran mesafesini world space mesafesine böl
    val scale = rawDistance / normDistance

    // ========================================
    // STEP 3: Rotasyon Hesapla
    // ========================================
    // Raw landmark'ların açısı (ekranda)
    val rawAngle = kotlin.math.atan2(
        (rawMiddle.y - rawWrist.y).toDouble(),
        (rawMiddle.x - rawWrist.x).toDouble()
    ).toFloat()

    // Normalize landmark'ların açısı (world space'te)
    val normAngle = kotlin.math.atan2(
        (normMiddle.y - normWrist.y).toDouble(),
        (normMiddle.x - normWrist.x).toDouble()
    ).toFloat()

    // Rotasyon farkı
    val rotationAngle = rawAngle - normAngle

    val cosAngle = kotlin.math.cos(rotationAngle)
    val sinAngle = kotlin.math.sin(rotationAngle)

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

        // 3. Raw bilek pozisyonuna kaydır
        val screenX = rotatedX + rawWrist.x
        val screenY = rotatedY + rawWrist.y

        Offset(screenX, screenY)
    }
}

/**
 * ⚡⚡⚡ KÖKTƏN ÇÖZÜM: RAW landmark'lar için bağlantı çizimi
 *
 * MediaPipe'dan gelen 21 noktanın TÜMÜ eksiksiz çizilir!
 * ZERO filtreleme, ZERO smoothing, ZERO kalman - sadece RAW çizim!
 *
 * @param handColor Her el için farklı renk (çift el için)
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHandConnectionsRaw(
    rawPositions: List<Offset>,
    handColor: Color = Color(0xFF00FF88)  // ⚡ FIX: Renk parametresi eklendi
) {
    if (rawPositions.size != 21) {
        android.util.Log.e("HandOverlay", "⚠️ Cannot draw connections: expected 21 points, got ${rawPositions.size}")
        return
    }

    // El bağlantı indeksleri (başlangıç, bitiş)
    val connections = listOf(
        // Bilek -> Palm
        Pair(0, 1), Pair(0, 5), Pair(0, 9), Pair(0, 13), Pair(0, 17),
        // Palm çizgileri
        Pair(5, 9), Pair(9, 13), Pair(13, 17),
        // Başparmak
        Pair(1, 2), Pair(2, 3), Pair(3, 4),
        // İşaret parmağı
        Pair(5, 6), Pair(6, 7), Pair(7, 8),
        // Orta parmak
        Pair(9, 10), Pair(10, 11), Pair(11, 12),
        // Yüzük parmağı
        Pair(13, 14), Pair(14, 15), Pair(15, 16),
        // Serçe parmak
        Pair(17, 18), Pair(18, 19), Pair(19, 20)
    )

    // Tüm bağlantıları çiz (hiçbir filtreleme kontrolü YOK!)
    connections.forEach { (startIdx, endIdx) ->
        val start = rawPositions[startIdx]
        val end = rawPositions[endIdx]

        // Glow efekti
        drawLine(
            color = handColor.copy(alpha = 0.3f),
            start = start,
            end = end,
            strokeWidth = 12f,
            cap = StrokeCap.Round
        )

        // Ana çizgi
        drawLine(
            color = handColor,
            start = start,
            end = end,
            strokeWidth = 6f,
            cap = StrokeCap.Round
        )
    }
}
