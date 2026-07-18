package com.example.isekaikuroshin.ui.vfx

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.isekaikuroshin.data.rememberLocalizedText
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.isekaikuroshin.data.VFXLevel
import com.example.isekaikuroshin.data.VFXEffectType

/**
 * ⚡⚡⚡ BÖLÜM B: Davranış Ayarları Kartı
 * Kullanıcının efektin görünümünü, konumlandırmasını ve kontrol davranışlarını kişiselleştirmesini sağlar
 */
@Composable
fun BehaviorSettingsCard(
    uiState: VFXWorkshopUiState,
    onColorChange: (Color) -> Unit,
    onSizeChange: (Float) -> Unit,
    onShapeChange: (com.example.isekaikuroshin.data.VFXShape) -> Unit,
    onAlphaChange: (Float) -> Unit,
    onOffsetChange: (Float, Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onReferencePointChange: (HandReferencePoint) -> Unit,
    onGhostHandToggle: (Boolean) -> Unit,
    onFpsMonitorToggle: (Boolean) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Başlık (tıklanabilir - genişlet/daralt)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rememberLocalizedText("behavior_settings"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
                Text(
                    text = if (isExpanded) "▼" else "▶",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            if (isExpanded) {
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.2f),
                    thickness = 1.dp
                )

                // ⚡⚡⚡ Görünüm Ayarları
                Text(
                    text = rememberLocalizedText("appearance_settings"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6A4C93)
                )

                // Şekil Seçici
                ShapePickerRow(
                    selectedShape = uiState.vfxShape,
                    onShapeSelected = onShapeChange
                )

                // Renk Seçici
                ColorPickerRow(
                    selectedColor = uiState.vfxColor,
                    onColorSelected = onColorChange
                )

                // Boyut Slider'ı
                SliderRow(
                    label = "Efekt Boyutu",
                    value = uiState.vfxSize,
                    valueRange = 0.5f..2.0f,
                    valueLabel = "${(uiState.vfxSize * 100).toInt()}%",
                    onValueChange = onSizeChange
                )

                // Alpha (Transparanlık) Slider'ı
                AlphaSliderRow(
                    alpha = uiState.vfxAlpha,
                    onAlphaChange = onAlphaChange
                )

                // ⚡⚡⚡ BÖLÜM C: Mini Önizleme
                VFXPreviewBox(
                    vfxLevel = uiState.selectedVFXLevel,
                    vfxShape = uiState.vfxShape,
                    vfxColor = uiState.vfxColor,
                    vfxSize = uiState.vfxSize,
                    vfxAlpha = uiState.vfxAlpha,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.1f),
                    thickness = 1.dp
                )

                // ⚡⚡⚡ YENİ: Konumlandırma Ayarları (3D Offset)
                Text(
                    text = rememberLocalizedText("positioning"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6A4C93)
                )

                // Bilgilendirme
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ℹ️",
                            fontSize = 16.sp
                        )
                        Text(
                            text = rememberLocalizedText("positioning_desc"),
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            lineHeight = 14.sp
                        )
                    }
                }

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.1f),
                    thickness = 1.dp
                )

                // ⚡⚡⚡ Kontrol Ayarları
                Text(
                    text = rememberLocalizedText("control_settings"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6A4C93)
                )

                // Hareket Hassasiyeti Slider'ı
                SliderRow(
                    label = "Takip Hızı",
                    value = uiState.vfxSpeed,
                    valueRange = 0.1f..2.0f,
                    valueLabel = "${(uiState.vfxSpeed * 100).toInt()}%",
                    onValueChange = onSpeedChange
                )

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.1f),
                    thickness = 1.dp
                )

                // ⚡⚡⚡ Test Araçları
                Text(
                    text = rememberLocalizedText("test_tools"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6A4C93)
                )

                // Hayalet El Switch
                SwitchRow(
                    label = "Hayalet El",
                    description = "El takibi kaybolduğunda iskelet göster",
                    checked = uiState.isGhostHandEnabled,
                    onCheckedChange = onGhostHandToggle
                )

                // FPS Monitör Switch
                SwitchRow(
                    label = "Performans Monitörü",
                    description = "FPS ve performans bilgilerini göster",
                    checked = uiState.isFpsMonitorEnabled,
                    onCheckedChange = onFpsMonitorToggle
                )
            }
        }
    }
}

/**
 * Renk Seçici Satırı (Önceden tanımlı renkler)
 */
@Composable
private fun ColorPickerRow(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    val colorOptions = listOf(
        MaterialTheme.colorScheme.primary to "Turkuaz",
        Color(0xFF0096C7) to "Mavi",
        Color(0xFF4CAF50) to "Yeşil",
        Color(0xFFFF5252) to "Kırmızı",
        Color(0xFFFFEB3B) to "Sarı",
        Color(0xFF9C27B0) to "Mor",
        Color(0xFFFF9800) to "Turuncu",
        Color.White to "Beyaz"
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Efekt Rengi",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.7f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            colorOptions.forEach { (color, _) ->
                ColorCircle(
                    color = color,
                    isSelected = selectedColor == color,
                    onClick = { onColorSelected(color) }
                )
            }
        }
    }
}

/**
 * Renk Dairesi (Seçilebilir)
 */
@Composable
private fun ColorCircle(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier.padding(4.dp)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Seçili",
                tint = if (color == Color.White) Color.Black else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Slider Satırı (Genel kullanım)
 */
@Composable
private fun SliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = valueLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6A4C93)
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF6A4C93),
                activeTrackColor = Color(0xFF6A4C93),
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}

/**
 * Referans Nokta Seçici (Dropdown)
 */
@Composable
private fun ReferencePointSelector(
    selectedPoint: HandReferencePoint,
    onPointSelected: (HandReferencePoint) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = "Referans Nokta",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.7f)
        )

        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (selectedPoint) {
                        HandReferencePoint.PALM_CENTER -> "🖐️ Avuç İçi"
                        HandReferencePoint.INDEX_FINGER -> "👆 İşaret Parmağı Ucu"
                    },
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = "▼",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("🖐️ Avuç İçi") },
                onClick = {
                    onPointSelected(HandReferencePoint.PALM_CENTER)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("👆 İşaret Parmağı Ucu") },
                onClick = {
                    onPointSelected(HandReferencePoint.INDEX_FINGER)
                    expanded = false
                }
            )
        }
    }
}

/**
 * Switch Satırı (Toggle ayarları için)
 */
@Composable
private fun SwitchRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF6A4C93),
                checkedTrackColor = Color(0xFF6A4C93).copy(alpha = 0.5f),
                uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}

/**
 * ⚡⚡⚡ YENİ: Şekil Seçici Satırı
 * Kullanıcının VFX efekti için şekil seçmesini sağlar
 */
@Composable
fun ShapePickerRow(
    selectedShape: com.example.isekaikuroshin.data.VFXShape,
    onShapeSelected: (com.example.isekaikuroshin.data.VFXShape) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = rememberLocalizedText("shape"),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShapeButton(
                shape = com.example.isekaikuroshin.data.VFXShape.CIRCLE,
                isSelected = selectedShape == com.example.isekaikuroshin.data.VFXShape.CIRCLE,
                onSelect = onShapeSelected,
                emoji = "⚪",
                label = "Daire",
                modifier = Modifier.weight(1f)
            )
            ShapeButton(
                shape = com.example.isekaikuroshin.data.VFXShape.TRIANGLE,
                isSelected = selectedShape == com.example.isekaikuroshin.data.VFXShape.TRIANGLE,
                onSelect = onShapeSelected,
                emoji = "🔺",
                label = "Üçgen",
                modifier = Modifier.weight(1f)
            )
            ShapeButton(
                shape = com.example.isekaikuroshin.data.VFXShape.RECTANGLE,
                isSelected = selectedShape == com.example.isekaikuroshin.data.VFXShape.RECTANGLE,
                onSelect = onShapeSelected,
                emoji = "🟦",
                label = "Dikdörtgen",
                modifier = Modifier.weight(1f)
            )
            ShapeButton(
                shape = com.example.isekaikuroshin.data.VFXShape.STAR,
                isSelected = selectedShape == com.example.isekaikuroshin.data.VFXShape.STAR,
                onSelect = onShapeSelected,
                emoji = "⭐",
                label = "Yıldız",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Şekil Butonu (Tek bir şekil seçeneği)
 */
@Composable
private fun ShapeButton(
    shape: com.example.isekaikuroshin.data.VFXShape,
    isSelected: Boolean,
    onSelect: (com.example.isekaikuroshin.data.VFXShape) -> Unit,
    emoji: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = { onSelect(shape) },
        modifier = modifier.height(70.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF6A4C93) else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        shadowElevation = if (isSelected) 8.dp else 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

/**
 * ⚡⚡⚡ YENİ: Alpha (Transparanlık) Slider'ı
 * Kullanıcının VFX efekti için transparanlık ayarlamasını sağlar (0.1 - 1.0)
 */
@Composable
fun AlphaSliderRow(
    alpha: Float,
    onAlphaChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = rememberLocalizedText("transparency"),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "${(alpha * 100).toInt()}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6A4C93)
            )
        }

        Slider(
            value = alpha,
            onValueChange = onAlphaChange,
            valueRange = 0.1f..1.0f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF6A4C93),
                activeTrackColor = Color(0xFF6A4C93),
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )

        Text(
            text = rememberLocalizedText("transparency_range"),
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

/**
 * ⚡⚡⚡ BÖLÜM C + F: VFX Önizleme Kutusu (Performans Optimize Edilmiş)
 * Kullanıcının seçtiği ayarlarla VFX efektinin mini önizlemesini gösterir
 *
 * PERFORMANS İYİLEŞTİRMELERİ:
 * - remember() ile gereksiz recomposition önlenir
 * - derivedStateOf ile sadece gerekli değişikliklerde render
 */
@Composable
fun VFXPreviewBox(
    vfxLevel: VFXLevel,
    vfxShape: com.example.isekaikuroshin.data.VFXShape,
    vfxColor: Color,
    vfxSize: Float,
    vfxAlpha: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Başlık
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rememberLocalizedText("preview"),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = when (vfxLevel) {
                        VFXLevel.SPRITE -> "💧"
                        VFXLevel.PARTICLE -> "✨"
                        VFXLevel.METABALL -> "🌊"
                    },
                    fontSize = 16.sp
                )
            }

            // Önizleme Canvas'ı
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = vfxAlpha }
                ) {
                    val centerPos = Offset(size.width / 2f, size.height / 2f)

                    // Şekle ve VFX seviyesine göre çiz
                    drawVFXShapePreview(
                        shape = vfxShape,
                        position = centerPos,
                        color = vfxColor,
                        vfxSize = vfxSize * 0.5f, // Önizleme için daha küçük
                        vfxType = vfxLevel
                    )
                }
            }

            // Bilgilendirme
            Text(
                text = rememberLocalizedText("realtime_preview"),
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

/**
 * Önizleme için şekil çizim fonksiyonu
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawVFXShapePreview(
    shape: com.example.isekaikuroshin.data.VFXShape,
    position: Offset,
    color: Color,
    vfxSize: Float,
    vfxType: VFXLevel
) {
    when (shape) {
        com.example.isekaikuroshin.data.VFXShape.CIRCLE -> {
            drawCircleVFXPreview(position, color, vfxSize, vfxType)
        }
        com.example.isekaikuroshin.data.VFXShape.TRIANGLE -> {
            drawTriangleVFXPreview(position, color, vfxSize, vfxType)
        }
        com.example.isekaikuroshin.data.VFXShape.RECTANGLE -> {
            drawRectangleVFXPreview(position, color, vfxSize, vfxType)
        }
        com.example.isekaikuroshin.data.VFXShape.STAR -> {
            drawStarVFXPreview(position, color, vfxSize, vfxType)
        }
    }
}

/**
 * Daire önizlemesi
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCircleVFXPreview(
    position: Offset,
    color: Color,
    size: Float,
    vfxType: VFXLevel
) {
    when (vfxType) {
        VFXLevel.SPRITE -> {
            drawCircle(color = color.copy(alpha = 0.3f), radius = 60f * size, center = position)
            drawCircle(color = color, radius = 40f * size, center = position, alpha = 0.7f)
            drawCircle(color = color.copy(alpha = 0.9f), radius = 25f * size, center = position)
        }
        VFXLevel.PARTICLE -> {
            for (i in 0 until 8) {
                val angle = (i * 45f) * (Math.PI / 180f).toFloat()
                val particlePos = Offset(
                    x = position.x + kotlin.math.cos(angle) * 40f * size,
                    y = position.y + kotlin.math.sin(angle) * 40f * size
                )
                drawCircle(color = color, radius = 10f * size, center = particlePos, alpha = 0.9f)
            }
            drawCircle(color = color, radius = 20f * size, center = position, alpha = 1.0f)
        }
        VFXLevel.METABALL -> {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.8f), color.copy(alpha = 0.5f), Color.Transparent),
                    center = position,
                    radius = 80f * size
                ),
                radius = 80f * size,
                center = position
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.95f), color.copy(alpha = 0.7f), Color.Transparent),
                    center = position,
                    radius = 60f * size
                ),
                radius = 60f * size,
                center = position
            )
            drawCircle(color = color, radius = 35f * size, center = position, alpha = 1.0f)
        }
    }
}

/**
 * Üçgen önizlemesi
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTriangleVFXPreview(
    position: Offset,
    color: Color,
    size: Float,
    vfxType: VFXLevel
) {
    when (vfxType) {
        VFXLevel.SPRITE -> {
            drawTrianglePath(position, 60f * size, color, alpha = 0.3f)
            drawTrianglePath(position, 40f * size, color, alpha = 0.7f)
            drawTrianglePath(position, 25f * size, color, alpha = 0.9f)
        }
        VFXLevel.PARTICLE -> {
            for (i in 0 until 8) {
                val angle = (i * 45f) * (Math.PI / 180f).toFloat()
                val particlePos = Offset(
                    x = position.x + kotlin.math.cos(angle) * 40f * size,
                    y = position.y + kotlin.math.sin(angle) * 40f * size
                )
                drawTrianglePath(particlePos, 10f * size, color, alpha = 0.9f)
            }
            drawTrianglePath(position, 20f * size, color, alpha = 1.0f)
        }
        VFXLevel.METABALL -> {
            drawTrianglePath(position, 80f * size, color, alpha = 0.8f)
            drawTrianglePath(position, 60f * size, color, alpha = 0.95f)
            drawTrianglePath(position, 35f * size, color, alpha = 1.0f)
        }
    }
}

/**
 * Dikdörtgen önizlemesi
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRectangleVFXPreview(
    position: Offset,
    color: Color,
    size: Float,
    vfxType: VFXLevel
) {
    when (vfxType) {
        VFXLevel.SPRITE -> {
            drawRectPath(position, 60f * size, color, alpha = 0.3f)
            drawRectPath(position, 40f * size, color, alpha = 0.7f)
            drawRectPath(position, 25f * size, color, alpha = 0.9f)
        }
        VFXLevel.PARTICLE -> {
            for (i in 0 until 8) {
                val angle = (i * 45f) * (Math.PI / 180f).toFloat()
                val particlePos = Offset(
                    x = position.x + kotlin.math.cos(angle) * 40f * size,
                    y = position.y + kotlin.math.sin(angle) * 40f * size
                )
                drawRectPath(particlePos, 10f * size, color, alpha = 0.9f)
            }
            drawRectPath(position, 20f * size, color, alpha = 1.0f)
        }
        VFXLevel.METABALL -> {
            drawRectPath(position, 80f * size, color, alpha = 0.8f)
            drawRectPath(position, 60f * size, color, alpha = 0.95f)
            drawRectPath(position, 35f * size, color, alpha = 1.0f)
        }
    }
}

/**
 * Yıldız önizlemesi
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStarVFXPreview(
    position: Offset,
    color: Color,
    size: Float,
    vfxType: VFXLevel
) {
    when (vfxType) {
        VFXLevel.SPRITE -> {
            drawStarPath(position, 60f * size, color, alpha = 0.3f)
            drawStarPath(position, 40f * size, color, alpha = 0.7f)
            drawStarPath(position, 25f * size, color, alpha = 0.9f)
        }
        VFXLevel.PARTICLE -> {
            for (i in 0 until 8) {
                val angle = (i * 45f) * (Math.PI / 180f).toFloat()
                val particlePos = Offset(
                    x = position.x + kotlin.math.cos(angle) * 40f * size,
                    y = position.y + kotlin.math.sin(angle) * 40f * size
                )
                drawStarPath(particlePos, 10f * size, color, alpha = 0.9f)
            }
            drawStarPath(position, 20f * size, color, alpha = 1.0f)
        }
        VFXLevel.METABALL -> {
            drawStarPath(position, 80f * size, color, alpha = 0.8f)
            drawStarPath(position, 60f * size, color, alpha = 0.95f)
            drawStarPath(position, 35f * size, color, alpha = 1.0f)
        }
    }
}

// Helper: Üçgen path çiz
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTrianglePath(
    center: Offset,
    radius: Float,
    color: Color,
    alpha: Float = 1.0f
) {
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + radius * 0.866f, center.y + radius * 0.5f)
        lineTo(center.x - radius * 0.866f, center.y + radius * 0.5f)
        close()
    }
    drawPath(path = path, color = color, alpha = alpha)
}

// Helper: Dikdörtgen path çiz
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRectPath(
    center: Offset,
    rectSize: Float,
    color: Color,
    alpha: Float = 1.0f
) {
    drawRect(
        color = color,
        topLeft = Offset(center.x - rectSize/2, center.y - rectSize/2),
        size = androidx.compose.ui.geometry.Size(rectSize, rectSize),
        alpha = alpha
    )
}

// Helper: Yıldız path çiz
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStarPath(
    center: Offset,
    radius: Float,
    color: Color,
    alpha: Float = 1.0f
) {
    val path = androidx.compose.ui.graphics.Path().apply {
        val innerRadius = radius * 0.4f
        for (i in 0 until 10) {
            val angle = (i * 36f - 90f) * (Math.PI / 180f).toFloat()
            val r = if (i % 2 == 0) radius else innerRadius
            val x = center.x + r * kotlin.math.cos(angle)
            val y = center.y + r * kotlin.math.sin(angle)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path = path, color = color, alpha = alpha)
}

/**
 * ⚡⚡⚡ BÖLÜM E: Efekt Türü Bilgilendirme Kartı
 * Gelişmiş efektler için bilgilendirme (Duman, Ateş, vb.)
 */
@Composable
fun EffectTypeInfoCard(
    currentEffectType: VFXEffectType,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rememberLocalizedText("effect_type"),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = when (currentEffectType) {
                        VFXEffectType.STANDARD -> "⚪ Standart"
                        VFXEffectType.SMOKE -> "💨 Duman"
                        VFXEffectType.FIRE -> "🔥 Ateş"
                        VFXEffectType.EXPLOSION -> "💥 Patlama"
                        VFXEffectType.TRAIL -> "〰️ İz"
                        VFXEffectType.GLOW -> "✨ Parıltı"
                        VFXEffectType.CONFETTI -> "🎊 Konfeti"
                        VFXEffectType.DISSOLVE -> "🌫️ Erime"
                        VFXEffectType.ENERGY -> "⚡ Enerji"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6A4C93)
                )
            }

            if (currentEffectType != VFXEffectType.STANDARD) {
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.1f),
                    thickness = 1.dp
                )

                Text(
                    text = rememberLocalizedText("advanced_effects_future"),
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    lineHeight = 14.sp
                )
            }
        }
    }
}
