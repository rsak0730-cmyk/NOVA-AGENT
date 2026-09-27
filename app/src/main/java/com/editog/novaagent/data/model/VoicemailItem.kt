package com.editog.novaagent.data.model

import kotlinx.serialization.Serializable

@Serializable
data class VoicemailItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val callerName: String,
    val phoneNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 12,
    val audioPath: String? = null,
    val transcript: String = "Caller left a voicemail after 20s unanswered ring.",
    val isPlaying: Boolean = false,
    val isRead: Boolean = false
)
