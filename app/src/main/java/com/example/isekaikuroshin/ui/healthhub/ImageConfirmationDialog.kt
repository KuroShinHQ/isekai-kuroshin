package com.example.isekaikuroshin.ui.healthhub

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.isekaikuroshin.data.VisionAnalysisResult
import com.example.isekaikuroshin.data.rememberLocalizedText
import kotlinx.coroutines.launch

/**
 * # Health Hub: Görüntü Analiz Onay Dialogu
 *
 * Kullanıcıya AI'ın tespit ettiği sağlık verilerini gösterir ve düzeltme/onaylama imkanı sunar.
 *
 * ## Özellikler
 * - 📊 Veri tipi gösterimi (WEIGHT, FOOD, BLOOD_TEST, FITNESS, UNKNOWN)
 * - 📋 Tespit edilen değerler listesi (key-value pairs)
 * - 🎯 Güven skoru progress bar (0.0 - 1.0)
 * - ⚠️ Düşük güven skoru uyarısı (<0.5) - Bulanık fotoğraf
 * - ✏️ Manuel düzeltme modu (TextField ile kullanıcı girişi)
 * - ✅ Başarı animasyonu (konfeti efekti + auto-dismiss)
 *
 * ## Akış
 * 1. **Loading State**: Analiz devam ederken CircularProgressIndicator
 * 2. **Sonuç Gösterimi**: Veri tipi, değerler, güven skoru
 * 3. **Düşük Güven (<0.5)**: Özel butonlar (Tekrar Çek / Manuel Giriş / İptal)
 * 4. **Normal Güven (≥0.5)**: Standart butonlar (Doğru / Düzelt)
 * 5. **Başarı**: 1.5 saniyelik konfeti animasyonu → onConfirm() → onDismiss()
 *
 * ## Parametreler
 * @param analysis AI analiz sonucu (VisionAnalysisResult)
 * @param isProcessing Loading state kontrolü
 * @param onConfirm Kullanıcı "Doğru" butonuna tıkladığında callback
 * @param onCorrect Kullanıcı manuel düzeltme yaptığında callback (düzeltilmiş değer)
 * @param onDismiss Dialog kapatıldığında callback
 *
 * ## Dosya Referansları
 * - ViewModel: HealthHubViewModel.kt:confirmImageData(), applyUserCorrection()
 * - Kullanım: HealthHubScreen.kt:542-550
 *
 * @since FAZ 2.2
 * @see VisionAnalysisResult
 * @see SuccessConfettiAnimation
 */
@Composable
fun ImageConfirmationDialog(
    analysis: VisionAnalysisResult,
    isProcessing: Boolean = false,
    onConfirm: () -> Unit,
    onCorrect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showCorrectionField by remember { mutableStateOf(false) }
    var correctionText by remember { mutableStateOf("") }
    var showSuccessAnimation by remember { mutableStateOf(false) }

    // Başarı animasyonu tamamlandığında dialog'u kapat
    LaunchedEffect(showSuccessAnimation) {
        if (showSuccessAnimation) {
            kotlinx.coroutines.delay(1500) // 1.5 saniye konfeti göster
            onConfirm()
            onDismiss()
        }
    }

    // Loading animasyonu için
    val infiniteTransition = rememberInfiniteTransition(label = "loading_animation")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha_animation"
    )

    Dialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Başlık
                Text(
                    text = "📊 Analiz Sonucu",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isProcessing) {
                    // Loading state
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = rememberLocalizedText("analyzing_image"),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                        )
                    }
                } else {
                    // Analiz sonuçları
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        // Veri tipi
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "Veri Tipi",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = getDataTypeDisplayName(analysis.dataType),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Tespit edilen değerler
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = rememberLocalizedText("detected_values"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                if (analysis.extractedValues.isEmpty()) {
                                    Text(
                                        text = rememberLocalizedText("value_not_detected"),
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                                    )
                                } else {
                                    analysis.extractedValues.forEach { (key, value) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "$key:",
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Text(
                                                text = value.toString(),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // FAZ 2.4: Düşük güven skoru uyarısı
                        if (analysis.confidence < 0.5f) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = rememberLocalizedText("blurry_image_warning"),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = rememberLocalizedText("low_confidence_warning"),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Güven skoru
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = rememberLocalizedText("confidence_score"),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = "${(analysis.confidence * 100).toInt()}%",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { analysis.confidence },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                        }

                        // Önerilen düzeltmeler
                        analysis.suggestedCorrections?.let { corrections ->
                            if (corrections.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Text(
                                            text = "⚠️ Dikkat Edilmesi Gerekenler",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        corrections.forEach { correction ->
                                            Text(
                                                text = "• $correction",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Düzeltme alanı
                        if (showCorrectionField) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = correctionText,
                                onValueChange = { correctionText = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Düzeltme") },
                                placeholder = { Text("Doğru değeri girin...") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // FAZ 2.4: Aksiyon butonları - Güven skoruna göre farklı seçenekler
                    if (analysis.confidence < 0.5f && !showCorrectionField) {
                        // Düşük güven skoru - Özel butonlar
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onDismiss() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(rememberLocalizedText("cancel_button"), fontSize = 13.sp)
                                }
                                Button(
                                    onClick = { showCorrectionField = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(rememberLocalizedText("manual_entry"), fontSize = 13.sp)
                                }
                            }
                            Button(
                                onClick = { onDismiss() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text(rememberLocalizedText("retake_photo"), fontSize = 13.sp)
                            }
                        }
                    } else {
                        // Normal güven skoru - Standart butonlar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (showCorrectionField) {
                                // Düzeltme modunda
                                OutlinedButton(
                                    onClick = {
                                        showCorrectionField = false
                                        correctionText = ""
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(rememberLocalizedText("cancel_button"))
                                }
                                Button(
                                    onClick = {
                                        if (correctionText.isNotBlank()) {
                                            onCorrect(correctionText)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = correctionText.isNotBlank()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(rememberLocalizedText("save_button"))
                                }
                            } else {
                                // Normal mod
                                OutlinedButton(
                                    onClick = { showCorrectionField = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Düzelt")
                                }
                                Button(
                                    onClick = {
                                        showSuccessAnimation = true
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Doğru")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Başarı animasyonu overlay
        if (showSuccessAnimation) {
            SuccessConfettiAnimation()
        }
    }
}

/**
 * DataType enum'ı kullanıcı dostu metne çevir
 */
private fun getDataTypeDisplayName(dataType: com.example.isekaikuroshin.data.DataType): String {
    return when (dataType) {
        com.example.isekaikuroshin.data.DataType.WEIGHT -> "Kilo Ölçümü"
        com.example.isekaikuroshin.data.DataType.FOOD -> "Yemek/Besin"
        com.example.isekaikuroshin.data.DataType.BLOOD_TEST -> "Kan Tahlili"
        com.example.isekaikuroshin.data.DataType.FITNESS -> "Fitness/Egzersiz"
        com.example.isekaikuroshin.data.DataType.UNKNOWN -> "Bilinmeyen"
    }
}

/**
 * Başarılı kayıt sonrası konfeti animasyonu
 */
@Composable
private fun SuccessConfettiAnimation() {
    // Basit konfeti efekti - gerçek konfeti için Lottie kullanılabilir
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(
                targetValue = 1.2f,
                animationSpec = tween(
                    durationMillis = 600,
                    easing = FastOutSlowInEasing
                )
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 300)
            )
            kotlinx.coroutines.delay(800)
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 400)
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .graphicsLayer(
                    scaleX = scale.value,
                    scaleY = scale.value,
                    alpha = alpha.value
                )
        ) {
            Text(
                text = "✅",
                fontSize = 64.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = rememberLocalizedText("successfully_saved"),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
