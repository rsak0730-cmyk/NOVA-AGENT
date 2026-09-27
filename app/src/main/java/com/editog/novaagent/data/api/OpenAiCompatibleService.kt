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

class OpenAiCompatibleService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun chatCompletion(
        config: ApiConfig,
        systemInstruction: String,
        prompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val endpoint = if (config.baseUrl.endsWith("/chat/completions")) {
                config.baseUrl
            } else {
                "${config.baseUrl.trimEnd('/')}/chat/completions"
            }

            val requestJson = buildJsonObject {
                put("model", config.modelName)
                put("temperature", 0.2)
                putJsonArray("messages") {
                    addJsonObject {
                        put("role", "system")
                        put("content", systemInstruction)
                    }
                    addJsonObject {
                        put("role", "user")
                        put("content", prompt)
                    }
                }
                putJsonObject("response_format") {
                    put("type", "json_object")
                }
            }

            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val requestBuilder = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer ${config.apiKey.trim()}")
                .post(body)

            val response = client.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("API error ${response.code}: $responseBody"))
            }

            val root = json.parseToJsonElement(responseBody).jsonObject
            val choices = root["choices"]?.jsonArray
            val firstChoice = choices?.firstOrNull()?.jsonObject
            val content = firstChoice?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content
                ?: return@withContext Result.failure(Exception("Empty response message"))

            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
