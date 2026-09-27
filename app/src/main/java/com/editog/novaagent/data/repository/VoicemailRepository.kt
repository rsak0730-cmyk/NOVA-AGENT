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

data class VoicemailUserProfile(
    val ownerName: String = "",
    val phoneNumber: String = "",
    val greetingMessage: String = "Hello, I am unavailable right now. Please leave your name and message for Nova Agent.",
    val ringSeconds: Int = 20,
    val isGuardianActive: Boolean = true,
    val screeningMode: String = "All Incoming Callers"
)

class VoicemailRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("nova_voicemails", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }

    private val _voicemails = MutableStateFlow<List<VoicemailItem>>(loadVoicemailsSafely())
    val voicemails: StateFlow<List<VoicemailItem>> = _voicemails.asStateFlow()

    private val _userProfile = MutableStateFlow(loadUserProfile())
    val userProfile: StateFlow<VoicemailUserProfile> = _userProfile.asStateFlow()

    // Backward-compatible flow
    val userPhoneNumber: StateFlow<String> get() = MutableStateFlow(_userProfile.value.phoneNumber)

    private fun loadUserProfile(): VoicemailUserProfile {
        return VoicemailUserProfile(
            ownerName = prefs.getString("user_owner_name", "") ?: "",
            phoneNumber = prefs.getString("user_mobile_number", "") ?: "",
            greetingMessage = prefs.getString("user_greeting_message", "Hello, I am unavailable right now. Please leave your name and message for Nova Agent.") ?: "",
            ringSeconds = prefs.getInt("user_ring_seconds", 20),
            isGuardianActive = prefs.getBoolean("user_guardian_active", true),
            screeningMode = prefs.getString("user_screening_mode", "All Incoming Callers") ?: "All Incoming Callers"
        )
    }

    fun saveVoicemailProfile(
        ownerName: String,
        phoneNumber: String,
        greetingMessage: String,
        ringSeconds: Int = 20,
        isGuardianActive: Boolean = true,
        screeningMode: String = "All Incoming Callers"
    ) {
        val updated = VoicemailUserProfile(
            ownerName = ownerName.trim(),
            phoneNumber = phoneNumber.trim(),
            greetingMessage = greetingMessage.trim(),
            ringSeconds = ringSeconds.coerceIn(5, 60),
            isGuardianActive = isGuardianActive,
            screeningMode = screeningMode.trim()
        )
        _userProfile.value = updated

        prefs.edit()
            .putString("user_owner_name", updated.ownerName)
            .putString("user_mobile_number", updated.phoneNumber)
            .putString("user_greeting_message", updated.greetingMessage)
            .putInt("user_ring_seconds", updated.ringSeconds)
            .putBoolean("user_guardian_active", updated.isGuardianActive)
            .putString("user_screening_mode", updated.screeningMode)
            .apply()
    }

    fun saveUserPhoneNumber(number: String) {
        saveVoicemailProfile(
            ownerName = _userProfile.value.ownerName,
            phoneNumber = number,
            greetingMessage = _userProfile.value.greetingMessage,
            ringSeconds = _userProfile.value.ringSeconds,
            isGuardianActive = _userProfile.value.isGuardianActive,
            screeningMode = _userProfile.value.screeningMode
        )
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
