package com.editog.novaagent.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.editog.novaagent.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("nova_settings", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        val raw = prefs.getString("app_settings_json", null)
        return if (raw != null) {
            try {
                json.decodeFromString<AppSettings>(raw)
            } catch (e: Exception) {
                AppSettings()
            }
        } else {
            AppSettings()
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        _settings.value = newSettings
        prefs.edit().putString("app_settings_json", json.encodeToString(newSettings)).apply()
    }
}
