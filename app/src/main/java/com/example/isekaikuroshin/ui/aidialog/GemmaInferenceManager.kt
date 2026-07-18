package com.example.isekaikuroshin.ui.aidialog

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class GemmaInferenceManager(private val context: Context) {

    private var llmInference: LlmInference? = null
    private val _isModelInitialized = MutableStateFlow(false)
    val isModelInitialized: StateFlow<Boolean> = _isModelInitialized.asStateFlow()

    private val _loadingProgress = MutableStateFlow(0f)
    val loadingProgress: StateFlow<Float> = _loadingProgress.asStateFlow()

    private val _loadingStatus = MutableStateFlow("Preparing AI model...")
    val loadingStatus: StateFlow<String> = _loadingStatus.asStateFlow()

    private val inferenceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        Log.d("GemmaInferenceManager", "Starting model initialization...")
        inferenceScope.launch {
            try {
                // Copy model from assets to external storage with progress
                val modelFile = copyModelFromAssets()

                // Try to initialize with the copied model
                _loadingStatus.value = "Loading AI engine..."
                _loadingProgress.value = 0.9f

                val options = LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .build()

                Log.d("GemmaInferenceManager", "Creating LLM inference from file: ${modelFile.absolutePath}")
                llmInference = LlmInference.createFromOptions(context, options)
                Log.d("GemmaInferenceManager", "Model initialized successfully!")
                _isModelInitialized.value = true
                _loadingProgress.value = 1.0f
                _loadingStatus.value = "AI ready!"

            } catch (e: Exception) {
                Log.e("GemmaInferenceManager", "Model initialization failed: ${e.message}", e)
                _isModelInitialized.value = false
                _loadingStatus.value = "AI model failed to load"
            }
        }
    }

    private suspend fun copyModelFromAssets(): File = withContext(Dispatchers.IO) {
        val fileName = "gemma-1.1-2b-it-cpu-int4.bin"
        val externalDir = context.getExternalFilesDir(null)
        val modelFile = File(externalDir, fileName)

        if (modelFile.exists()) {
            Log.d("GemmaInferenceManager", "Model already exists in external storage")
            return@withContext modelFile
        }

        Log.d("GemmaInferenceManager", "Copying model from assets...")
        _loadingStatus.value = "Copying AI model..."

        val assetPath = "stitch_ai_designs/gemma/$fileName"
        context.assets.open(assetPath).use { inputStream ->
            modelFile.outputStream().use { outputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytes = 0L
                val totalSize = 1346427328L // 1.3GB approximate size

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytes += bytesRead
                    val progress = (totalBytes.toFloat() / totalSize.toFloat()).coerceIn(0f, 0.8f)
                    _loadingProgress.value = progress

                    if (totalBytes % (50 * 1024 * 1024) == 0L) { // Update every 50MB
                        val progressPercent = (progress * 100).toInt()
                        _loadingStatus.value = "Copying AI model... $progressPercent%"
                        Log.d("GemmaInferenceManager", "Copied ${totalBytes / (1024 * 1024)}MB")
                    }
                }
            }
        }

        Log.d("GemmaInferenceManager", "Model copied to: ${modelFile.absolutePath}")
        modelFile
    }

    suspend fun generateResponse(prompt: String): String = withContext(Dispatchers.Default) {
        if (!isModelInitialized.value) {
            return@withContext "Error: AI model not initialized yet."
        }
        llmInference?.let { llm ->
            try {
                val result = llm.generateResponse(prompt)
                result
            } catch (e: Exception) {
                e.printStackTrace()
                "Error: Could not generate response from AI."
            }
        } ?: "Error: AI model not available."
    }

    fun close() {
        llmInference?.close()
    }
}