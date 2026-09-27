package com.editog.novaagent.data.api

import com.editog.novaagent.data.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AgentBrainOrchestrator(
    private val geminiService: GeminiApiService = GeminiApiService(),
    private val openAiService: OpenAiCompatibleService = OpenAiCompatibleService()
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val systemPrompt = """
        You are Nova Agent, an autonomous Android AI assistant with the brain of Gemini.
        You control the Android device according to the user's intent.

        CAPABILITIES YOU MUST FULFILL:
        1. App Opener: Open any installed app or modded app (e.g., YouTube, Instagram, WhatsApp, TikTok, Chrome, Settings).
        2. Watchdog: See live screen content and predict what the user wants or clarify if confused.
        3. Interact: Click elements, scroll Reels/Shorts (Instagram, YouTube, Facebook), type text into search boxes or chats.
        4. Make Call: Search contacts, show row-numbered candidates if multiple matches exist, confirm Yes/No before dialing.
        5. Send SMS: Confirm contact number, prompt/confirm message content, then send.
        6. General Chat: Answer questions, provide information conversationally.

        RESPONSE FORMAT:
        You MUST respond ONLY with valid JSON in this exact structure:
        {
          "thought": "<Brief internal reasoning>",
          "command": {
             "type": "open_app" | "inspect_screen" | "scroll" | "click" | "type_text" | "find_contact" | "make_call" | "send_sms" | "general_response",
             // appropriate parameters:
             // open_app: "app_name": "..."
             // scroll: "direction": "up"|"down", "times": 1
             // click: "target_text": "..."
             // type_text: "target": "search_box"|"chat", "text": "..."
             // find_contact: "name": "..."
             // make_call: "phone_number": "...", "contact_name": "..."
             // send_sms: "phone_number": "...", "contact_name": "...", "message": "..."
             // general_response: "message": "..."
          },
          "assistant_response": "<Natural conversational message to display to user and speak aloud>"
        }
    """.trimIndent()

    suspend fun processIntent(
        userPrompt: String,
        screenContext: String?,
        config: ApiConfig
    ): Result<AgentDecision> {
        val fullPrompt = buildString {
            append("User Command: ").append(userPrompt).append("\n")
            if (!screenContext.isNullOrBlank()) {
                append("Live Screen Context (Watchdog):\n").append(screenContext).append("\n")
            }
        }

        val rawResult = when (config.provider) {
            ApiProvider.GEMINI -> geminiService.generateContent(config, systemPrompt, fullPrompt)
            ApiProvider.OPENROUTER, ApiProvider.OPENAI -> openAiService.chatCompletion(config, systemPrompt, fullPrompt)
        }

        return rawResult.mapCatching { rawJson ->
            parseDecision(rawJson, userPrompt)
        }
    }

    private fun parseDecision(rawJson: String, fallbackPrompt: String): AgentDecision {
        return try {
            val root = json.parseToJsonElement(rawJson).jsonObject
            val thought = root["thought"]?.jsonPrimitive?.content ?: ""
            val assistantResponse = root["assistant_response"]?.jsonPrimitive?.content
                ?: "I have processed your request."

            val cmdObj = root["command"]?.jsonObject
            val type = cmdObj?.get("type")?.jsonPrimitive?.content ?: "general_response"

            val command: AgentCommand = when (type) {
                "open_app" -> AgentCommand.OpenApp(cmdObj?.get("app_name")?.jsonPrimitive?.content ?: "")
                "inspect_screen" -> AgentCommand.InspectScreen()
                "scroll" -> AgentCommand.Scroll(
                    direction = cmdObj?.get("direction")?.jsonPrimitive?.content ?: "up",
                    times = cmdObj?.get("times")?.jsonPrimitive?.content?.toIntOrNull() ?: 1
                )
                "click" -> AgentCommand.Click(cmdObj?.get("target_text")?.jsonPrimitive?.content ?: "")
                "type_text" -> AgentCommand.TypeText(
                    target = cmdObj?.get("target")?.jsonPrimitive?.content ?: "search_box",
                    text = cmdObj?.get("text")?.jsonPrimitive?.content ?: ""
                )
                "find_contact" -> AgentCommand.FindContact(cmdObj?.get("name")?.jsonPrimitive?.content ?: "")
                "make_call" -> AgentCommand.MakeCall(
                    phone_number = cmdObj?.get("phone_number")?.jsonPrimitive?.content ?: "",
                    contact_name = cmdObj?.get("contact_name")?.jsonPrimitive?.content ?: ""
                )
                "send_sms" -> AgentCommand.SendSms(
                    phone_number = cmdObj?.get("phone_number")?.jsonPrimitive?.content ?: "",
                    contact_name = cmdObj?.get("contact_name")?.jsonPrimitive?.content ?: "",
                    message = cmdObj?.get("message")?.jsonPrimitive?.content ?: ""
                )
                else -> AgentCommand.GeneralResponse(assistantResponse)
            }

            AgentDecision(thought, command, assistantResponse)
        } catch (e: Exception) {
            // Fallback heuristics if model returns plain text
            AgentDecision(
                thought = "Direct response parse",
                command = AgentCommand.GeneralResponse(rawJson.take(200)),
                assistant_response = rawJson
            )
        }
    }
}
