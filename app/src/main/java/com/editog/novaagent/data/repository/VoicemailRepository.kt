package com.editog.novaagent.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.editog.novaagent.data.model.VoicemailItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class VoicemailRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("nova_voicemails", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _voicemails = MutableStateFlow<List<VoicemailItem>>(loadVoicemails())
    val voicemails: StateFlow<List<VoicemailItem>> = _voicemails.asStateFlow()

    private fun loadVoicemails(): List<VoicemailItem> {
        val raw = prefs.getString("voicemails_json", null)
        return if (raw != null) {
            try {
                json.decodeFromString<List<VoicemailItem>>(raw)
            } catch (e: Exception) {
                sampleVoicemails()
            }
        } else {
            sampleVoicemails()
        }
    }

    private fun sampleVoicemails(): List<VoicemailItem> {
        return listOf(
            VoicemailItem(
                callerName = "Alex Rivera",
                phoneNumber = "+1 (555) 234-8901",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 45,
                durationSeconds = 18,
                transcript = "Hey, just following up on our project review meeting. Call me back when you're free!"
            ),
            VoicemailItem(
                callerName = "Mom",
                phoneNumber = "+1 (555) 890-1234",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 4,
                durationSeconds = 24,
                transcript = "Hi dear! Don't forget Sunday dinner at 6 PM. Let me know if you need anything brought over."
            )
        )
    }

    fun addVoicemail(item: VoicemailItem) {
        val updated = listOf(item) + _voicemails.value
        _voicemails.value = updated
        persist(updated)
    }

    fun togglePlayback(id: String) {
        val updated = _voicemails.value.map { item ->
            if (item.id == id) {
                item.copy(isPlaying = !item.isPlaying)
            } else {
                item.copy(isPlaying = false) // only 1 plays at once
            }
        }
        _voicemails.value = updated
    }

    fun stopAllPlayback() {
        val updated = _voicemails.value.map { it.copy(isPlaying = false) }
        _voicemails.value = updated
    }

    fun deleteVoicemail(id: String) {
        val updated = _voicemails.value.filter { it.id != id }
        _voicemails.value = updated
        persist(updated)
    }

    private fun persist(list: List<VoicemailItem>) {
        prefs.edit().putString("voicemails_json", json.encodeToString(list)).apply()
    }
}
