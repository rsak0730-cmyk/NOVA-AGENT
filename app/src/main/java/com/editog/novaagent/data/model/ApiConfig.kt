package com.editog.novaagent.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class ApiProvider(val displayName: String, val defaultBaseUrl: String, val defaultModel: String) {
    GEMINI("Google Gemini (AI Studio)", "https://generativelanguage.googleapis.com", "gemini-1.5-flash"),
    OPENROUTER("OpenRouter", "https://openrouter.ai/api/v1", "meta-llama/llama-3.1-8b-instruct:free"),
    OPENAI("OpenAI", "https://api.openai.com/v1", "gpt-4o-mini")
}

@Serializable
data class ApiConfig(
    val provider: ApiProvider = ApiProvider.GEMINI,
    val profileName: String = "Primary AI Key",
    val apiKey: String = "",
    val modelName: String = "gemini-1.5-flash",
    val baseUrl: String = "https://generativelanguage.googleapis.com"
)
