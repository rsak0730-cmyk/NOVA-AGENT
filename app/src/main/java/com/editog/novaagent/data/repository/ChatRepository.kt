package com.editog.novaagent.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.editog.novaagent.data.model.ChatMessage
import com.editog.novaagent.data.model.MessageSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ChatRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("nova_chat_history", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _messages = MutableStateFlow<List<ChatMessage>>(loadMessages())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private fun loadMessages(): List<ChatMessage> {
        val raw = prefs.getString("chat_messages_json", null)
        return if (raw != null) {
            try {
                json.decodeFromString<List<ChatMessage>>(raw)
            } catch (e: Exception) {
                defaultWelcomeMessages()
            }
        } else {
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
        val updated = _messages.value + message
        _messages.value = updated
        persist(updated)
    }

    fun updateMessage(id: String, transform: (ChatMessage) -> ChatMessage) {
        val updated = _messages.value.map { if (it.id == id) transform(it) else it }
        _messages.value = updated
        persist(updated)
    }

    fun clearChat() {
        val reset = defaultWelcomeMessages()
        _messages.value = reset
        persist(reset)
    }

    private fun persist(list: List<ChatMessage>) {
        prefs.edit().putString("chat_messages_json", json.encodeToString(list)).apply()
    }
}
