package com.editog.novaagent.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class MessageSender {
    USER,
    AGENT,
    SYSTEM
}

@Serializable
data class ContactRowChoice(
    val rowNumber: Int,
    val name: String,
    val phoneNumber: String,
    val type: String = "Mobile"
)

@Serializable
data class ActionPendingConfirmation(
    val actionType: String, // "CALL" or "SMS"
    val targetName: String,
    val targetNumber: String,
    val messageContent: String? = null,
    val candidateRows: List<ContactRowChoice> = emptyList()
)

@Serializable
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionBadge: String? = null,
    val pendingConfirmation: ActionPendingConfirmation? = null,
    val isActionExecuted: Boolean = false
)
