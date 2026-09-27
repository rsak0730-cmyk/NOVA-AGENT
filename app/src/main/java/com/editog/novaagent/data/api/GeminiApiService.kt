package com.editog.novaagent.data.api

import android.util.Log
import com.editog.novaagent.data.model.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GeminiApiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun generateContent(
        config: ApiConfig,
        systemInstruction: String,
        prompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val rawModel = if (config.modelName.isNotBlank()) config.modelName.trim() else "gemini-2.0-flash"
        val cleanModel = rawModel.removePrefix("models/")
        val baseUrl = if (config.baseUrl.isNotBlank()) config.baseUrl.trim().trimEnd('/') else "https://generativelanguage.googleapis.com"
        val apiKey = config.apiKey.trim()

        // 1. Try with user's selected model
        val firstAttempt = executeGeminiRequest(baseUrl, cleanModel, apiKey, systemInstruction, prompt)
        if (firstAttempt.isSuccess) {
            return@withContext firstAttempt
        }

        // 2. If model not found (404/400) or unrecognized (e.g. gemini-3.1-flash-lite), fallback to gemini-2.0-flash
        if (cleanModel != "gemini-2.0-flash" && cleanModel != "gemini-1.5-flash") {
            Log.w("GeminiApiService", "Model $cleanModel failed, falling back to gemini-2.0-flash")
            val fallbackAttempt = executeGeminiRequest(baseUrl, "gemini-2.0-flash", apiKey, systemInstruction, prompt)
            if (fallbackAttempt.isSuccess) {
                return@withContext fallbackAttempt
            }

            // 3. Fallback to gemini-1.5-flash
            Log.w("GeminiApiService", "Fallback to gemini-1.5-flash")
            val fallbackAttempt2 = executeGeminiRequest(baseUrl, "gemini-1.5-flash", apiKey, systemInstruction, prompt)
            if (fallbackAttempt2.isSuccess) {
                return@withContext fallbackAttempt2
            }
        }

        return@withContext firstAttempt
    }

    private fun executeGeminiRequest(
        baseUrl: String,
        modelName: String,
        apiKey: String,
        systemInstruction: String,
        prompt: String
    ): Result<String> {
        return try {
            val url = "$baseUrl/v1beta/models/$modelName:generateContent?key=$apiKey"

            val requestJson = buildJsonObject {
                putJsonObject("systemInstruction") {
                    putJsonArray("parts") {
                        addJsonObject { put("text", systemInstruction) }
                    }
                }
                putJsonArray("contents") {
                    addJsonObject {
                        put("role", "user")
                        putJsonArray("parts") {
                            addJsonObject { put("text", prompt) }
                        }
                    }
                }
                putJsonObject("generationConfig") {
                    put("temperature", 0.4)
                }
            }

            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("GeminiApiService", "API Error ${response.code} for model $modelName: $responseBody")
                return Result.failure(Exception("Gemini API error ${response.code} for $modelName: $responseBody"))
            }

            val root = json.parseToJsonElement(responseBody).jsonObject
            val candidates = root["candidates"]?.jsonArray
            val firstCandidate = candidates?.firstOrNull()?.jsonObject
            val parts = firstCandidate?.get("content")?.jsonObject?.get("parts")?.jsonArray
            val textContent = parts?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content
                ?: return Result.failure(Exception("No text content returned from Gemini"))

            Result.success(textContent)
        } catch (e: Exception) {
            Log.e("GeminiApiService", "Exception during executeGeminiRequest for $modelName", e)
            Result.failure(e)
        }
    }
}
