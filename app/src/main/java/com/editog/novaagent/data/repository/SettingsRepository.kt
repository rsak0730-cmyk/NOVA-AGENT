package com.editog.novaagent.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.editog.novaagent.data.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("nova_settings", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }

    private val _settings = MutableStateFlow(loadSettingsSafely())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettingsSafely(): AppSettings {
        return try {
            val raw = prefs.getString("app_settings_json", null)
            if (!raw.isNullOrBlank()) {
                json.decodeFromString<AppSettings>(raw)
            } else {
                AppSettings()
            }
        } catch (e: Throwable) {
            Log.e("SettingsRepository", "Error decoding settings, resetting to default", e)
            AppSettings()
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        try {
            _settings.value = newSettings
            prefs.edit().putString("app_settings_json", json.encodeToString(newSettings)).apply()
        } catch (e: Throwable) {
            Log.e("SettingsRepository", "Error persisting settings", e)
        }
    }
}
