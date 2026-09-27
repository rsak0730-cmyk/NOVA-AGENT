package com.editog.novaagent.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class AgentCommand {
    @Serializable
    @SerialName("open_app")
    data class OpenApp(val app_name: String) : AgentCommand()

    @Serializable
    @SerialName("inspect_screen")
    data class InspectScreen(val reason: String = "watchdog") : AgentCommand()

    @Serializable
    @SerialName("scroll")
    data class Scroll(val direction: String, val times: Int = 1) : AgentCommand() // "up" (next reel) or "down"

    @Serializable
    @SerialName("click")
    data class Click(val target_text: String) : AgentCommand()

    @Serializable
    @SerialName("type_text")
    data class TypeText(val target: String, val text: String) : AgentCommand()

    @Serializable
    @SerialName("find_contact")
    data class FindContact(val name: String) : AgentCommand()

    @Serializable
    @SerialName("make_call")
    data class MakeCall(val phone_number: String, val contact_name: String) : AgentCommand()

    @Serializable
    @SerialName("send_sms")
    data class SendSms(val phone_number: String, val contact_name: String, val message: String) : AgentCommand()

    @Serializable
    @SerialName("general_response")
    data class GeneralResponse(val message: String) : AgentCommand()
}

@Serializable
data class AgentDecision(
    val thought: String = "",
    val command: AgentCommand,
    val assistant_response: String
)
