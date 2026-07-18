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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.isekaikuroshin.data.rememberLocalizedText
import com.example.isekaikuroshin.data.spell.SpellTrigger
import com.example.isekaikuroshin.engine.VoiceCommandMatcher

/**
 * GÖREV E - Faz 3: Test Ekranında Mikrofon Butonu
 *
 * Basılı tutulduğunda ses tanıma başlatan buton.
 * Gerçek zamanlı feedback gösterir.
 */
@Composable
fun VoiceTriggerButton(
    voiceCommands: List<SpellTrigger.VoiceCommand>,
    onVoiceCommandMatched: (SpellTrigger.VoiceCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf<String?>(null) }
    var hasPermission by remember { mutableStateOf(false) }
    var matchStatus by remember { mutableStateOf<MatchStatus?>(null) }

    // ⚡ Çeviri referansları
    val errorText = rememberLocalizedText("error")
    val voiceTriggerDesc = rememberLocalizedText("voice_trigger")

    // Mikrofon izni launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (!isGranted) {
            Log.w("VoiceTriggerButton", "❌ Mikrofon izni reddedildi")
        } else {
            Log.d("VoiceTriggerButton", "✅ Mikrofon izni verildi")
        }
    }

    // SpeechRecognizer instance
    val speechRecognizer = remember {
        SpeechRecognizer.createSpeechRecognizer(context)
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer.destroy()
        }
    }

    // Auto-hide match status
    LaunchedEffect(matchStatus) {
        if (matchStatus != null) {
            kotlinx.coroutines.delay(2000)
            matchStatus = null
            recognizedText = null
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Gerçek zamanlı feedback (tanınan metin)
            AnimatedVisibility(
                visible = recognizedText != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (matchStatus) {
                            is MatchStatus.Success -> MaterialTheme.colorScheme.primaryContainer
                            is MatchStatus.NoMatch -> MaterialTheme.colorScheme.errorContainer
                            null -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = rememberLocalizedText("recognized_text"),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "\"${recognizedText ?: ""}\"",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (matchStatus) {
                                is MatchStatus.Success -> MaterialTheme.colorScheme.primary
                                is MatchStatus.NoMatch -> MaterialTheme.colorScheme.error
                                null -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )

                        // Match status mesajı
                        when (val status = matchStatus) {
                            is MatchStatus.Success -> {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "✅ ${rememberLocalizedText("voice_trigger_success")}: ${status.matchedCommand.command}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            is MatchStatus.NoMatch -> {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "❌ ${rememberLocalizedText("voice_no_match")}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            null -> {}
                        }
                    }
                }
            }

            // Mikrofon butonu (basılı tut)
            PressAndHoldMicButton(
                isListening = isListening,
                contentDescription = voiceTriggerDesc,
                onPressStart = {
                    if (!hasPermission) {
                        // İzin iste
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else if (voiceCommands.isNotEmpty()) {
                        // Dinlemeye başla
                        isListening = true
                        recognizedText = null
                        matchStatus = null
                        startListening(
                            context = context,
                            speechRecognizer = speechRecognizer,
                            onResult = { text ->
                                recognizedText = text
                                isListening = false

                                // Eşleşme kontrolü
                                val matched = findMatchingCommand(text, voiceCommands)
                                if (matched != null) {
                                    matchStatus = MatchStatus.Success(matched)
                                    onVoiceCommandMatched(matched)
                                    Log.d("VoiceTriggerButton", "✅ Büyü tetiklendi: ${matched.command}")
                                } else {
                                    matchStatus = MatchStatus.NoMatch
                                    Log.d("VoiceTriggerButton", "❌ Hiçbir komut eşleşmedi")
                                }
                            },
                            onError = {
                                isListening = false
                                recognizedText = errorText
                            }
                        )
                    }
                },
                onPressEnd = {
                    if (isListening) {
                        speechRecognizer.stopListening()
                        isListening = false
                    }
                }
            )
        }
    }
}

/**
 * Basılı tutma gerektiren mikrofon butonu
 */
@Composable
private fun PressAndHoldMicButton(
    isListening: Boolean,
    contentDescription: String,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit
) {
    // Pulse animasyonu
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .scale(if (isListening) scale else 1f)
                .background(
                    color = if (isListening)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            onPressStart()
                            tryAwaitRelease()
                            onPressEnd()
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Text(
            text = if (isListening)
                rememberLocalizedText("listening")
            else
                rememberLocalizedText("mic_button_hint"),
            fontSize = 11.sp,
            color = if (isListening)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isListening) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * Tanınan metni komutlarla eşleştir
 */
private fun findMatchingCommand(
    recognizedText: String,
    commands: List<SpellTrigger.VoiceCommand>
): SpellTrigger.VoiceCommand? {
    for (command in commands) {
        val isMatch = VoiceCommandMatcher.matches(
            target = command.command,
            recognized = recognizedText,
            sensitivity = command.sensitivity
        )

        if (isMatch) {
            return command
        }
    }
    return null
}

/**
 * Ses dinlemeyi başlat
 */
private fun startListening(
    context: android.content.Context,
    speechRecognizer: SpeechRecognizer,
    onResult: (String) -> Unit,
    onError: () -> Unit
) {
    // Mevcut dil kodunu al (tr-TR, en-US vb.)
    val currentLanguageCode = com.example.isekaikuroshin.data.LanguageManager.getCurrentLanguageCode()

    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguageCode)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }

    speechRecognizer.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d("VoiceTrigger", "🎤 Hazır")
        }

        override fun onBeginningOfSpeech() {
            Log.d("VoiceTrigger", "🗣️ Konuşma başladı")
        }

        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            Log.d("VoiceTrigger", "🛑 Konuşma bitti")
        }

        override fun onError(error: Int) {
            Log.e("VoiceTrigger", "❌ Hata: $error")
            onError()
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                onResult(matches[0])
            } else {
                onError()
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    })

    speechRecognizer.startListening(intent)
}

/**
 * Eşleşme durumu
 */
private sealed class MatchStatus {
    data class Success(val matchedCommand: SpellTrigger.VoiceCommand) : MatchStatus()
    data object NoMatch : MatchStatus()
}
