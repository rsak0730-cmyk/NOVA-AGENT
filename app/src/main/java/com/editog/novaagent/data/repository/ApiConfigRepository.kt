package com.editog.novaagent.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.editog.novaagent.data.model.ApiConfig
import com.editog.novaagent.data.model.ApiProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ApiConfigRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("nova_api_config", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<ApiConfig> = _config.asStateFlow()

    private fun loadConfig(): ApiConfig {
        val raw = prefs.getString("api_config_json", null)
        return if (raw != null) {
            try {
                json.decodeFromString<ApiConfig>(raw)
            } catch (e: Exception) {
                ApiConfig()
            }
        } else {
            ApiConfig()
        }
    }

    fun saveConfig(newConfig: ApiConfig) {
        _config.value = newConfig
        prefs.edit().putString("api_config_json", json.encodeToString(newConfig)).apply()
    }
}
