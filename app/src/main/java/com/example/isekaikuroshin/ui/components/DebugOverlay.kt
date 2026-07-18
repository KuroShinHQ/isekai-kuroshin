package com.example.isekaikuroshin.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.isekaikuroshin.data.rememberLocalizedText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.isekaikuroshin.data.NormalizedPoint
import com.example.isekaikuroshin.engine.DebugInfo

/**
 * Hata Ayıklama Overlay
 *
 * Jest tanıma motorunun iç işleyişini ekran üzerinde görselleştirir.
 * Kullanıcının canlı elini (mavi) ve en iyi eşleşen şablonu (kırmızı) çizer.
 */
@Composable
fun DebugOverlay(
    debugInfo: DebugInfo,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // İskelet çizimi (Canvas)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Canlı eli çiz (MAVİ)
            if (debugInfo.liveNormalizedLandmarks.isNotEmpty()) {
                drawHandSkeleton(
                    landmarks = debugInfo.liveNormalizedLandmarks,
                    canvasWidth = canvasWidth,
                    canvasHeight = canvasHeight,
                    color = Color.Blue,
                    alpha = 0.7f
                )
            }

            // En iyi şablonu çiz (KIRMIZI)
            if (debugInfo.bestMatchTemplateLandmarks.isNotEmpty()) {
                drawHandSkeleton(
                    landmarks = debugInfo.bestMatchTemplateLandmarks,
                    canvasWidth = canvasWidth,
                    canvasHeight = canvasHeight,
                    color = Color.Red,
                    alpha = 0.6f
                )
            }
        }

        // Debug bilgileri (Metin)
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color.Black.copy(alpha = 0.8f),
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "🐛 DEBUG MODU",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00FF88),
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = rememberLocalizedText("loaded_angles").replace("{count}", debugInfo.loadedTemplateCount.toString()),
                    fontSize = 12.sp,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = rememberLocalizedText("best_match").replace("{match}", if (debugInfo.bestMatchTemplateIndex >= 0) "Açı #${debugInfo.bestMatchTemplateIndex + 1}" else "Yok"),
                    fontSize = 12.sp,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = rememberLocalizedText("raw_accuracy").replace("{score}", (debugInfo.rawAccuracyScore * 100).toInt().toString()),
                    fontSize = 12.sp,
                    color = when {
                        debugInfo.rawAccuracyScore >= 0.9f -> Color(0xFF00FF88)
                        debugInfo.rawAccuracyScore >= 0.7f -> Color(0xFFFFD700)
                        debugInfo.rawAccuracyScore >= 0.5f -> Color(0xFFFF9800)
                        else -> Color(0xFFFF5252)
                    },
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = "Ort. Mesafe: ${"%.4f".format(debugInfo.avgDistance)}",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = "Smoothing Buffer: ${debugInfo.smoothingBufferSize}",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    fontFamily = FontFamily.Monospace
                )

                // ⚠️ DURUM UYARILARI
                // Tracking lost
                if (debugInfo.isTrackingLost) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⚠️ Tracking Lost",
                        fontSize = 11.sp,
                        color = Color(0xFFFF5252),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Boş template uyarısı
                if (debugInfo.loadedTemplateCount == 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⚠️ No Templates",
                        fontSize = 11.sp,
                        color = Color(0xFFFFAA00),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = rememberLocalizedText("no_skeleton"),
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Renk açıklaması (Legend)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color.Black.copy(alpha = 0.8f),
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color.Blue, RoundedCornerShape(2.dp))
                    )
                    Text(
                        text = rememberLocalizedText("live_hand"),
                        fontSize = 11.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color.Red, RoundedCornerShape(2.dp))
                    )
                    Text(
                        text = rememberLocalizedText("template"),
                        fontSize = 11.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * El iskeletini çizer
 *
 * @param landmarks Normalize edilmiş landmark'lar
 * @param canvasWidth Canvas genişliği
 * @param canvasHeight Canvas yüksekliği
 * @param color Çizim rengi
 * @param alpha Şeffaflık
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHandSkeleton(
    landmarks: List<NormalizedPoint>,
    canvasWidth: Float,
    canvasHeight: Float,
    color: Color,
    alpha: Float
) {
    if (landmarks.size != 21) return

    // El iskeletinin bağlantıları (MediaPipe Hands standardı)
    val connections = listOf(
        // Bilek → Parmak kökleri
        0 to 1, 0 to 5, 0 to 9, 0 to 13, 0 to 17,
        // Başparmak
        1 to 2, 2 to 3, 3 to 4,
        // İşaret parmağı
        5 to 6, 6 to 7, 7 to 8,
        // Orta parmak
        9 to 10, 10 to 11, 11 to 12,
        // Yüzük parmağı
        13 to 14, 14 to 15, 15 to 16,
        // Serçe parmak
        17 to 18, 18 to 19, 19 to 20,
        // Avuç içi bağlantıları
        5 to 9, 9 to 13, 13 to 17
    )

    // Landmark'ları canvas koordinatlarına dönüştür
    // NOT: Normalize edilmiş landmark'lar -1 ile 1 arasında değil,
    // bizim normalizasyon algoritmasının çıktısı farklı bir formatta.
    // Bu yüzden scale ve offset hesaplamaları farklı yapılmalı.

    // Landmark'ların bounding box'ını bul
    val minX = landmarks.minOfOrNull { it.x } ?: -0.5f
    val maxX = landmarks.maxOfOrNull { it.x } ?: 0.5f
    val minY = landmarks.minOfOrNull { it.y } ?: -0.5f
    val maxY = landmarks.maxOfOrNull { it.y } ?: 0.5f

    val landmarkWidth = maxX - minX
    val landmarkHeight = maxY - minY

    // Canvas'ın %70'ini kullan (padding için)
    val targetSize = minOf(canvasWidth, canvasHeight) * 0.7f
    val scale = if (landmarkWidth > 0 && landmarkHeight > 0) {
        targetSize / maxOf(landmarkWidth, landmarkHeight)
    } else {
        1f
    }

    // Merkeze yerleştir
    val offsetX = (canvasWidth - landmarkWidth * scale) / 2f - minX * scale
    val offsetY = (canvasHeight - landmarkHeight * scale) / 2f - minY * scale

    // Bağlantıları çiz
    connections.forEach { (start, end) ->
        if (start < landmarks.size && end < landmarks.size) {
            val p1 = landmarks[start]
            val p2 = landmarks[end]

            drawLine(
                color = color.copy(alpha = alpha),
                start = Offset(
                    x = p1.x * scale + offsetX,
                    y = p1.y * scale + offsetY
                ),
                end = Offset(
                    x = p2.x * scale + offsetX,
                    y = p2.y * scale + offsetY
                ),
                strokeWidth = 3.dp.toPx()
            )
        }
    }

    // Landmark noktalarını çiz
    landmarks.forEach { landmark ->
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = 4.dp.toPx(),
            center = Offset(
                x = landmark.x * scale + offsetX,
                y = landmark.y * scale + offsetY
            )
        )
    }
}
