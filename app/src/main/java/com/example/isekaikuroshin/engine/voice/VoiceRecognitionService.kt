package com.example.isekaikuroshin.engine.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sesli Komut Tanıma Servisi
 *
 * Bu servis, Android'in yerleşik SpeechRecognizer API'sini kullanarak
 * kullanıcının sesini metne çevirir. Büyü Tasarım Stüdyosu'nda tetikleyici
 * olarak kullanılabilir.
 *
 * Not: Google Cloud Speech-to-Text API yerine, yerel Android API kullanılmıştır.
 * Bu sayede:
 * - İnternet bağlantısı gerektirmez
 * - Daha hızlı yanıt verir
 * - API key gerektirmez
 * - Kullanıcı gizliliğini korur
 */
@Singleton
class VoiceRecognitionService @Inject constructor() {

    companion object {
        private const val TAG = "VoiceRecognitionService"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var recognitionIntent: Intent? = null

    // Tanıma durumu
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    // Tanınan metinler
    private val _recognizedText = Channel<String>(Channel.BUFFERED)
    val recognizedText: Flow<String> = _recognizedText.receiveAsFlow()

    // Anlık metin (kullanıcı konuşurken)
    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    // Hatalar
    private val _errors = Channel<VoiceRecognitionError>(Channel.BUFFERED)
    val errors: Flow<VoiceRecognitionError> = _errors.receiveAsFlow()

    /**
     * Sesli tanımayı başlat
     */
    fun startListening(context: Context, language: String = "tr-TR") {
        if (_isListening.value) {
            Log.w(TAG, "Already listening, ignoring start request")
            return
        }

        try {
            // SpeechRecognizer oluştur
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer?.setRecognitionListener(recognitionListener)
            }

            // Intent oluştur
            recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true) // Anlık sonuçlar
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            // Dinlemeyi başlat
            speechRecognizer?.startListening(recognitionIntent)
            _isListening.value = true
            Log.d(TAG, "✅ Voice recognition started (language: $language)")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start voice recognition: ${e.message}", e)
            _errors.trySend(VoiceRecognitionError.InitializationError(e.message ?: "Unknown error"))
            _isListening.value = false
        }
    }

    /**
     * Sesli tanımayı durdur
     */
    fun stopListening() {
        if (!_isListening.value) {
            return
        }

        try {
            speechRecognizer?.stopListening()
            _isListening.value = false
            _partialText.value = ""
            Log.d(TAG, "🔄 Voice recognition stopped")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to stop voice recognition: ${e.message}", e)
        }
    }

    /**
     * Dinlemeyi iptal et
     */
    fun cancelListening() {
        try {
            speechRecognizer?.cancel()
            _isListening.value = false
            _partialText.value = ""
            Log.d(TAG, "🔄 Voice recognition cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to cancel voice recognition: ${e.message}", e)
        }
    }

    /**
     * Kaynakları serbest bırak
     */
    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            recognitionIntent = null
            _isListening.value = false
            _partialText.value = ""
            Log.d(TAG, "🔄 Voice recognition service destroyed")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to destroy voice recognition: ${e.message}", e)
        }
    }

    /**
     * Recognition Listener
     */
    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "🎤 Ready for speech")
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "🎤 Speech started")
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Ses seviyesi değişti (opsiyonel: UI'da gösterilebilir)
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            // Ses buffer'ı alındı
        }

        override fun onEndOfSpeech() {
            Log.d(TAG, "🎤 Speech ended")
            _isListening.value = false
        }

        override fun onError(error: Int) {
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                SpeechRecognizer.ERROR_CLIENT -> "Client error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No match"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                else -> "Unknown error ($error)"
            }

            Log.e(TAG, "❌ Recognition error: $errorMessage")
            _errors.trySend(VoiceRecognitionError.RecognitionError(errorMessage))
            _isListening.value = false
            _partialText.value = ""
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val text = matches[0]
                Log.d(TAG, "✅ Recognition result: $text")
                _recognizedText.trySend(text)
                _partialText.value = ""
            }
            _isListening.value = false
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val text = matches[0]
                Log.d(TAG, "🔄 Partial result: $text")
                _partialText.value = text
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            // Opsiyonel event handling
        }
    }

    /**
     * Check if speech recognition is available
     */
    fun isAvailable(context: Context): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }
}

/**
 * Sesli tanıma hataları
 */
sealed class VoiceRecognitionError {
    data class InitializationError(val message: String) : VoiceRecognitionError()
    data class RecognitionError(val message: String) : VoiceRecognitionError()
}
