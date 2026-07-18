package com.example.isekaikuroshin.ui.spellstudio

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.isekaikuroshin.data.rememberLocalizedText
import kotlinx.coroutines.launch

/**
 * GÖREV E - Faz 1: Sesli Komut Kayd Dialogu
 *
 * Kullanıcının sihirli sözcüğünü kaydetmesi ve hassasiyet ayarlaması için dialog.
 */
@Composable
fun VoiceCommandDialog(
    onDismiss: () -> Unit,
    onConfirm: (command: String, sensitivity: Float) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope() // ⚡ Coroutine scope
    var recordedText by remember { mutableStateOf("") }
    var sensitivity by remember { mutableStateOf(0.80f) } // %80 varsayılan
    var isRecording by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // ⚡ Çeviri referansı (Composable context'ten önce al)
    val micPermissionText = rememberLocalizedText("microphone_permission_required")
    val audioErrorText = rememberLocalizedText("audio_error")
    val clientErrorText = rememberLocalizedText("client_error")
    val permissionDeniedText = rememberLocalizedText("permission_denied_error")
    val networkErrorText = rememberLocalizedText("network_error")
    val timeoutErrorText = rememberLocalizedText("timeout_error")
    val noMatchErrorText = rememberLocalizedText("no_match_error")
    val recognizerBusyText = rememberLocalizedText("recognizer_busy_error")
    val serverErrorText = rememberLocalizedText("server_error")
    val speechTimeoutText = rememberLocalizedText("speech_timeout_error")
    val unknownErrorText = rememberLocalizedText("unknown_error")
    val nothingUnderstoodText = rememberLocalizedText("nothing_understood")
    val speechFailedText = rememberLocalizedText("speech_recognition_failed")

    // Mikrofon izni launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (!isGranted) {
            errorMessage = micPermissionText
            Log.w("VoiceCommandDialog", "❌ Mikrofon izni reddedildi")
        } else {
            Log.d("VoiceCommandDialog", "✅ Mikrofon izni verildi")
        }
    }

    // 🔧 FIX: SpeechRecognizer'ı her seferinde yeniden oluştur (Google bug workaround)
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Başlık
                Text(
                    text = rememberLocalizedText("voice_command_title"),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Açıklama
                Text(
                    text = rememberLocalizedText("voice_command_desc"),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Kaydedilen metin kutusu
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (recordedText.isEmpty()) {
                            Text(
                                text = rememberLocalizedText("no_command_yet"),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            )
                        } else {
                            Text(
                                text = "\"$recordedText\"",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Mikrofon butonu
                AnimatedMicrophoneButton(
                    isRecording = isRecording,
                    onClick = {
                        if (!hasPermission) {
                            // İzin iste
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else if (isRecording) {
                            // ✅ Dinleme aktifse DURDUR
                            Log.d("VoiceCommandDialog", "⏹️ Dinleme durduruldu")
                            try {
                                speechRecognizer?.cancel()
                                speechRecognizer?.destroy()
                                speechRecognizer = null
                            } catch (e: Exception) {
                                Log.e("VoiceCommandDialog", "Durdurma hatası: ${e.message}")
                            }
                            isRecording = false
                            recordedText = ""
                        } else {
                            // ✅ Dinleme kapalıysa BAŞLAT
                            Log.d("VoiceCommandDialog", "▶️ Dinleme başlatılıyor")

                            // 🧪 EMULATOR TEST MODU: Gerçek cihaz yoksa simüle et
                            val isEmulator = android.os.Build.FINGERPRINT.contains("generic") ||
                                    android.os.Build.PRODUCT.contains("sdk")

                            if (isEmulator) {
                                // Test için direkt metin gir
                                Log.w("VoiceCommandDialog", "🧪 EMULATOR TEST MODU aktif!")
                                isRecording = true
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(2000) // 2 saniye bekle
                                    recordedText = "fire" // Test metni
                                    isRecording = false
                                    Log.i("VoiceCommandDialog", "🧪 Test metni: $recordedText")
                                }
                                return@AnimatedMicrophoneButton
                            }

                            // 🔧 FIX: Eski instance'ı yok et ve yenisini oluştur
                            try {
                                speechRecognizer?.destroy()
                            } catch (e: Exception) {
                                // Yoksay
                            }
                            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                            Log.d("VoiceCommandDialog", "🔄 Yeni SpeechRecognizer oluşturuldu")

                            // Kayda başla
                            startVoiceRecognition(
                                speechRecognizer = speechRecognizer!!,
                                context = context,
                                errorTranslations = ErrorTranslations(
                                    audioError = audioErrorText,
                                    clientError = clientErrorText,
                                    permissionDenied = permissionDeniedText,
                                    networkError = networkErrorText,
                                    timeout = timeoutErrorText,
                                    noMatch = noMatchErrorText,
                                    recognizerBusy = recognizerBusyText,
                                    serverError = serverErrorText,
                                    speechTimeout = speechTimeoutText,
                                    unknownError = unknownErrorText,
                                    nothingUnderstood = nothingUnderstoodText,
                                    speechFailed = speechFailedText
                                ),
                                onResult = { text ->
                                    recordedText = text
                                    isRecording = false
                                    Log.d("VoiceCommandDialog", "✅ Kaydedilen: $text")
                                },
                                onError = { error ->
                                    errorMessage = error
                                    isRecording = false
                                    Log.e("VoiceCommandDialog", "❌ Hata: $error")
                                },
                                onStart = {
                                    isRecording = true
                                    errorMessage = null
                                    recordedText = "" // Başlangıçta temizle
                                },
                                onPartialResult = { partialText ->
                                    // Gerçek zamanlı metin gösterimi
                                    recordedText = partialText
                                    Log.d("VoiceCommandDialog", "📝 Anlık: $partialText")
                                }
                            )
                        }
                    }
                )

                // Hata mesajı
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Hassasiyet slider
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = rememberLocalizedText("sensitivity"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${(sensitivity * 100).toInt()}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Slider(
                        value = sensitivity,
                        onValueChange = { sensitivity = it },
                        valueRange = 0.60f..0.95f,
                        steps = 6,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = rememberLocalizedText("flexible"),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = rememberLocalizedText("precise"),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Butonlar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(rememberLocalizedText("cancel"))
                    }

                    Button(
                        onClick = {
                            if (recordedText.isNotEmpty()) {
                                onConfirm(recordedText, sensitivity)
                                Log.d("VoiceCommandDialog", "✅ Kaydedildi: '$recordedText', Hassasiyet: ${(sensitivity * 100).toInt()}%")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = recordedText.isNotEmpty()
                    ) {
                        Text(rememberLocalizedText("save"))
                    }
                }
            }
        }
    }
}

/**
 * Animasyonlu mikrofon butonu
 */
@Composable
private fun AnimatedMicrophoneButton(
    isRecording: Boolean,
    onClick: () -> Unit
) {
    // Pulse animasyonu
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(80.dp)
            .scale(if (isRecording) scale else 1f)
            .background(
                color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                shape = CircleShape
            )
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Record Voice",
            tint = if (isRecording) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(40.dp)
        )
    }

    if (isRecording) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = rememberLocalizedText("listening"),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Hata mesajları çevirileri
 */
private data class ErrorTranslations(
    val audioError: String,
    val clientError: String,
    val permissionDenied: String,
    val networkError: String,
    val timeout: String,
    val noMatch: String,
    val recognizerBusy: String,
    val serverError: String,
    val speechTimeout: String,
    val unknownError: String,
    val nothingUnderstood: String,
    val speechFailed: String
)

/**
 * Ses tanımayı başlatır
 */
private fun startVoiceRecognition(
    speechRecognizer: SpeechRecognizer,
    context: android.content.Context,
    errorTranslations: ErrorTranslations,
    onResult: (String) -> Unit,
    onError: (String) -> Unit,
    onStart: () -> Unit,
    onPartialResult: (String) -> Unit = {}
) {
    // 🔧 FIX: ERROR_NO_MATCH çok erken gelirse yoksay
    var isReady = false
    var hasStartedSpeaking = false

    // 🎯 AKILLI EŞİK SİSTEMİ
    val noiseCalibrationDuration = 500L // İlk 500ms gürültü ölçümü
    val noiseSamples = mutableListOf<Float>()
    var noiseBaseline = 0f // Ortalama gürültü seviyesi
    var speechThreshold = 0f // Konuşma eşiği
    var calibrationComplete = false

    val startTime = System.currentTimeMillis()
    var speechStartTime = 0L
    var lastSpeechTime = 0L
    val silenceDuration = 1000L // 1 saniye sessizlik = konuşma bitti
    var manualStopScheduled = false

    // Mevcut dil kodunu al (tr-TR, en-US vb.)
    val currentLanguageCode = com.example.isekaikuroshin.data.LanguageManager.getCurrentLanguageCode()

    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguageCode)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5) // Daha fazla sonuç al
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true) // ⚡ Gerçek zamanlı metin için

        // 🔧 FIX: Google App sorununu bypass et - OFFLINE tanıma tercih et
        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)

        // 🔧 Mikrofon hassasiyetini artır (düşük sesleri de algıla)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L) // 5 saniye sessizlik
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L) // 3 saniye
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500L) // Minimum 1.5 saniye

        // 🔧 DEBUG: Alternatif diller de tanınsın
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, currentLanguageCode)
        putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("tr-TR", "en-US"))
    }

    speechRecognizer.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            isReady = true // ✅ Artık hazır
            Log.d("VoiceRecognition", "🎤 Dinlemeye hazır...")
            onStart()
        }

        override fun onBeginningOfSpeech() {
            hasStartedSpeaking = true // ✅ Konuşma başladı
            Log.d("VoiceRecognition", "🗣️ Konuşma başladı...")
        }

        override fun onRmsChanged(rmsdB: Float) {
            val currentTime = System.currentTimeMillis()
            val elapsedTime = currentTime - startTime

            // 🎯 FAZ 1: Gürültü Kalibrasyonu (İlk 500ms)
            if (!calibrationComplete && elapsedTime < noiseCalibrationDuration) {
                if (rmsdB > 0) {
                    noiseSamples.add(rmsdB)
                }
                return
            }

            // 🎯 FAZ 2: Kalibrasyon Tamamlandı
            if (!calibrationComplete && elapsedTime >= noiseCalibrationDuration) {
                calibrationComplete = true
                noiseBaseline = if (noiseSamples.isNotEmpty()) {
                    noiseSamples.average().toFloat()
                } else {
                    0.1f // Varsayılan
                }
                speechThreshold = noiseBaseline + 1.5f
                Log.i("VoiceRecognition", "📊 Kalibrasyon: Gürültü=${noiseBaseline.format(2)} dB, Eşik=${speechThreshold.format(2)} dB")
            }

            // 🎯 FAZ 3: Konuşma Algılama
            if (calibrationComplete && rmsdB > 0) {
                Log.d("VoiceRecognition", "🔊 Ses: ${"%.2f".format(rmsdB)} dB (Eşik: ${"%.2f".format(speechThreshold)} dB)")

                // Konuşma eşiğini aşıyor mu?
                if (rmsdB > speechThreshold) {
                    if (speechStartTime == 0L) {
                        speechStartTime = currentTime
                        Log.w("VoiceRecognition", "🗣️ Konuşma algılandı! (${rmsdB.format(2)} dB > ${speechThreshold.format(2)} dB)")
                    }
                    lastSpeechTime = currentTime
                    hasStartedSpeaking = true
                } else {
                    // Sessizlik kontrolü
                    if (hasStartedSpeaking && lastSpeechTime > 0) {
                        val silenceTime = currentTime - lastSpeechTime
                        if (silenceTime > silenceDuration && !manualStopScheduled) {
                            manualStopScheduled = true
                            Log.w("VoiceRecognition", "⏹️ Sessizlik algılandı (${silenceTime}ms). Manuel durdurma...")
                            // Manuel durdurma
                            try {
                                speechRecognizer.stopListening()
                            } catch (e: Exception) {
                                Log.e("VoiceRecognition", "Manuel durdurma hatası: ${e.message}")
                            }
                        }
                    }
                }
            }
        }

        // Helper extension
        private fun Float.format(decimals: Int): String = "%.${decimals}f".format(this)

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            Log.d("VoiceRecognition", "🛑 Konuşma bitti")
        }

        override fun onError(error: Int) {
            // 🔧 FIX: ERROR_NO_MATCH çok erken gelirse yoksay
            if (error == SpeechRecognizer.ERROR_NO_MATCH) {
                if (!isReady || !hasStartedSpeaking) {
                    Log.w("VoiceRecognition", "⚠️ ERROR_NO_MATCH çok erken geldi, yoksayıldı (isReady=$isReady, hasStarted=$hasStartedSpeaking)")
                    return // Yoksay!
                }
            }

            val errorMsg = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> errorTranslations.audioError
                SpeechRecognizer.ERROR_CLIENT -> errorTranslations.clientError
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> errorTranslations.permissionDenied
                SpeechRecognizer.ERROR_NETWORK -> errorTranslations.networkError
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> errorTranslations.timeout
                SpeechRecognizer.ERROR_NO_MATCH -> errorTranslations.noMatch
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> errorTranslations.recognizerBusy
                SpeechRecognizer.ERROR_SERVER -> errorTranslations.serverError
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> errorTranslations.speechTimeout
                else -> errorTranslations.unknownError
            }
            Log.e("VoiceRecognition", "❌ Hata: $errorMsg (Kod: $error)")
            onError(errorMsg)
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                // 🔍 DEBUG: Tüm sonuçları logla
                Log.d("VoiceRecognition", "📋 Tüm sonuçlar (${matches.size}): ${matches.joinToString(", ") { "\"$it\"" }}")

                val recognizedText = matches[0]
                Log.d("VoiceRecognition", "✅ Seçilen: '$recognizedText'")
                onResult(recognizedText)
            } else {
                Log.w("VoiceRecognition", "⚠️ Hiç sonuç dönmedi")
                onError(errorTranslations.nothingUnderstood)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            // Gerçek zamanlı metin geri bildirimi
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val partialText = matches[0]
                Log.d("VoiceRecognition", "📝 Kısmi sonuç: '$partialText'")
                onPartialResult(partialText)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    })

    try {
        speechRecognizer.startListening(intent)
        Log.d("VoiceRecognition", "🎬 Ses tanıma başlatıldı (Dil: $currentLanguageCode)")
    } catch (e: Exception) {
        Log.e("VoiceRecognition", "❌ Başlatma hatası: ${e.message}")
        onError(errorTranslations.speechFailed)
    }
}
