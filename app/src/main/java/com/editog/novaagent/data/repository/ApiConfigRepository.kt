package com.editog.novaagent.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.editog.novaagent.data.model.ApiConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ApiConfigRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("nova_api_config", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }

    private val _config = MutableStateFlow(loadConfigSafely())
    val config: StateFlow<ApiConfig> = _config.asStateFlow()

    private fun loadConfigSafely(): ApiConfig {
        return try {
            val raw = prefs.getString("api_config_json", null)
            if (!raw.isNullOrBlank()) {
                json.decodeFromString<ApiConfig>(raw)
            } else {
                ApiConfig()
            }
        } catch (e: Throwable) {
            Log.e("ApiConfigRepository", "Error decoding API config", e)
            ApiConfig()
        }
    }

    fun saveConfig(newConfig: ApiConfig) {
        try {
            _config.value = newConfig
            prefs.edit().putString("api_config_json", json.encodeToString(newConfig)).apply()
        } catch (e: Throwable) {
            Log.e("ApiConfigRepository", "Error saving config", e)
        }
    }
}
