package com.editog.novaagent.data.api

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
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun generateContent(
        config: ApiConfig,
        systemInstruction: String,
        prompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val model = if (config.modelName.isNotBlank()) config.modelName else "gemini-1.5-flash"
            val url = "${config.baseUrl.trimEnd('/')}/v1beta/models/$model:generateContent?key=${config.apiKey.trim()}"

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
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
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
                return@withContext Result.failure(Exception("Gemini API error ${response.code}: $responseBody"))
            }

            val root = json.parseToJsonElement(responseBody).jsonObject
            val candidates = root["candidates"]?.jsonArray
            val firstCandidate = candidates?.firstOrNull()?.jsonObject
            val parts = firstCandidate?.get("content")?.jsonObject?.get("parts")?.jsonArray
            val textContent = parts?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content
                ?: return@withContext Result.failure(Exception("No content returned from Gemini"))

            Result.success(textContent)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
