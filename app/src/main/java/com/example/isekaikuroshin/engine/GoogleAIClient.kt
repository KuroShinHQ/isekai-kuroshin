package com.example.isekaikuroshin.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@Serializable
data class GoogleAIRequest(
    val contents: List<Content>
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String
)

@Serializable
data class GoogleAIResponse(
    val candidates: List<Candidate>,
    val usageMetadata: UsageMetadata? = null
)

@Serializable
data class Candidate(
    val content: Content,
    val finishReason: String? = null
)

@Serializable
data class UsageMetadata(
    val promptTokenCount: Int = 0,
    val candidatesTokenCount: Int = 0,
    val totalTokenCount: Int = 0
)

class GoogleAIClient(private val apiKey: String) : AIClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS) // AI yanıt süresi için timeout artırıldı
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // API key is injected via constructor — never hardcode secrets in source.
    // Configure your key in BuildConfig or local.properties (see README → Configuration).
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

    override suspend fun generateStory(prompt: String): Result<AIResponse> = withContext(Dispatchers.IO) {
        try {
            println("🔍 DEBUG: Starting Google AI API call...")
            println("🔍 DEBUG: API Key length: ${apiKey.length}")
            println("🔍 DEBUG: Base URL: $baseUrl")
            println("🔍 DEBUG: Using X-goog-api-key header authentication")

            val request = GoogleAIRequest(
                contents = listOf(
                    Content(
                        parts = listOf(Part(text = prompt))
                    )
                )
            )

            val requestBody = json.encodeToString(GoogleAIRequest.serializer(), request)
                .toRequestBody("application/json".toMediaType())

            println("🔍 DEBUG: Request body created, length: ${requestBody.contentLength()}")

            val httpRequest = Request.Builder()
                .url(baseUrl) // Query parameter yok artık
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-goog-api-key", apiKey) // DOĞRU Google AI Studio header
                .build()

            println("🔍 DEBUG: HTTP request built, making call...")

            val response = client.newCall(httpRequest).execute()

            println("🔍 DEBUG: Response received - Code: ${response.code}, Message: ${response.message}")

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                println("🔍 DEBUG: Response body length: ${responseBody?.length ?: 0}")

                if (responseBody != null) {
                    println("🔍 DEBUG: Parsing JSON response...")
                    val aiResponse = json.decodeFromString(GoogleAIResponse.serializer(), responseBody)
                    val generatedText = aiResponse.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    val tokenCount = aiResponse.usageMetadata?.totalTokenCount ?: 0

                    if (generatedText != null) {
                        println("✅ DEBUG: Successfully got AI response: ${generatedText.take(100)}...")
                        println("🔢 DEBUG: Token usage: $tokenCount tokens")
                        Result.success(AIResponse(generatedText, tokenCount))
                    } else {
                        println("❌ DEBUG: AI response text is null")
                        Result.failure(Exception("AI response is empty"))
                    }
                } else {
                    println("❌ DEBUG: Response body is null")
                    Result.failure(Exception("Response body is null"))
                }
            } else {
                val errorBody = response.body?.string()
                println("❌ DEBUG: HTTP Error - Code: ${response.code}, Message: ${response.message}")
                println("❌ DEBUG: Error body: $errorBody")
                Result.failure(Exception("HTTP Error: ${response.code} - ${response.message} - Body: $errorBody"))
            }
        } catch (e: Exception) {
            println("❌ DEBUG: Exception in Google AI call: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
}