package com.editog.novaagent.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.editog.novaagent.data.model.ChatMessage
import com.editog.novaagent.data.model.MessageSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ChatRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("nova_chat_history", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }

    private val _messages = MutableStateFlow<List<ChatMessage>>(loadMessagesSafely())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private fun loadMessagesSafely(): List<ChatMessage> {
        return try {
            val raw = prefs.getString("chat_messages_json", null)
            if (!raw.isNullOrBlank()) {
                json.decodeFromString<List<ChatMessage>>(raw)
            } else {
                defaultWelcomeMessages()
            }
        } catch (e: Throwable) {
            Log.e("ChatRepository", "Error decoding chat messages", e)
            defaultWelcomeMessages()
        }
    }

    private fun defaultWelcomeMessages(): List<ChatMessage> {
        return listOf(
            ChatMessage(
                sender = MessageSender.AGENT,
                content = "Greetings! I am Nova Agent, powered by Gemini AI Studio. I can open any app, inspect screens live (Watchdog), scroll Reels/Shorts, type queries, place calls with contact verification, and handle voicemails."
            )
        )
    }

    fun addMessage(message: ChatMessage) {
        try {
            val updated = _messages.value + message
            _messages.value = updated
            persist(updated)
        } catch (e: Throwable) {
            Log.e("ChatRepository", "Error adding message", e)
        }
    }

    fun updateMessage(id: String, transform: (ChatMessage) -> ChatMessage) {
        try {
            val updated = _messages.value.map { if (it.id == id) transform(it) else it }
            _messages.value = updated
            persist(updated)
        } catch (e: Throwable) {
            Log.e("ChatRepository", "Error updating message", e)
        }
    }

    fun clearChat() {
        val reset = defaultWelcomeMessages()
        _messages.value = reset
        persist(reset)
    }

    private fun persist(list: List<ChatMessage>) {
        try {
            prefs.edit().putString("chat_messages_json", json.encodeToString(list)).apply()
        } catch (e: Throwable) {
            Log.e("ChatRepository", "Error persisting messages", e)
        }
    }
}
