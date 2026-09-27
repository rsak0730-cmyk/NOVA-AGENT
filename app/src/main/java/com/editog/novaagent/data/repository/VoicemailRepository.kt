package com.editog.novaagent.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.editog.novaagent.data.model.VoicemailItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class VoicemailRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("nova_voicemails", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }

    private val _voicemails = MutableStateFlow<List<VoicemailItem>>(loadVoicemailsSafely())
    val voicemails: StateFlow<List<VoicemailItem>> = _voicemails.asStateFlow()

    private val _userPhoneNumber = MutableStateFlow(prefs.getString("user_mobile_number", "") ?: "")
    val userPhoneNumber: StateFlow<String> = _userPhoneNumber.asStateFlow()

    fun saveUserPhoneNumber(number: String) {
        val trimmed = number.trim()
        _userPhoneNumber.value = trimmed
        prefs.edit().putString("user_mobile_number", trimmed).apply()
    }

    private fun loadVoicemailsSafely(): List<VoicemailItem> {
        return try {
            val raw = prefs.getString("voicemails_json", null)
            if (!raw.isNullOrBlank()) {
                json.decodeFromString<List<VoicemailItem>>(raw)
            } else {
                emptyList()
            }
        } catch (e: Throwable) {
            Log.e("VoicemailRepository", "Error decoding voicemails", e)
            emptyList()
        }
    }

    fun addVoicemail(item: VoicemailItem) {
        try {
            val updated = listOf(item) + _voicemails.value
            _voicemails.value = updated
            persist(updated)
        } catch (e: Throwable) {
            Log.e("VoicemailRepository", "Error adding voicemail", e)
        }
    }

    fun togglePlayback(id: String) {
        val updated = _voicemails.value.map { item ->
            if (item.id == id) {
                item.copy(isPlaying = !item.isPlaying)
            } else {
                item.copy(isPlaying = false)
            }
        }
        _voicemails.value = updated
    }

    fun stopAllPlayback() {
        val updated = _voicemails.value.map { it.copy(isPlaying = false) }
        _voicemails.value = updated
    }

    fun deleteVoicemail(id: String) {
        try {
            val updated = _voicemails.value.filter { it.id != id }
            _voicemails.value = updated
            persist(updated)
        } catch (e: Throwable) {
            Log.e("VoicemailRepository", "Error deleting voicemail", e)
        }
    }

    private fun persist(list: List<VoicemailItem>) {
        try {
            prefs.edit().putString("voicemails_json", json.encodeToString(list)).apply()
        } catch (e: Throwable) {
            Log.e("VoicemailRepository", "Error persisting voicemails", e)
        }
    }
}
