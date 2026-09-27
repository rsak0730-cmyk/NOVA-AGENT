package com.editog.novaagent.data.api

import android.util.Log
import com.editog.novaagent.data.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AgentBrainOrchestrator(
    private val geminiService: GeminiApiService = GeminiApiService(),
    private val openAiService: OpenAiCompatibleService = OpenAiCompatibleService()
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    private val systemPrompt = """
        You are JARVIS (Nova Agent), an ultra-intelligent, sophisticated Android AI agent and autonomous personal companion.
        You speak with the charm, wit, crisp politeness, and high capability of Tony Stark's J.A.R.V.I.S.

        BEHAVIOR GUIDELINES:
        1. HUMAN & CONVERSATIONAL: If the user greets you, asks for explanations, chats casually, or asks general knowledge questions, answer directly, smartly, and conversationally.
        2. SMART INTENT DETECTION: Discern user commands effortlessly:
           - App Opener: "Open YouTube", "Launch Instagram", "Can you start WhatsApp", "Play Spotify" -> type: "open_app"
           - Reels / Media Navigation: "Next video", "Scroll down", "Swipe up", "Scroll next" -> type: "scroll" (direction: "up" or "down")
           - Calling & Contacts: "Call Mom", "Phone Alex", "Dial John" -> type: "find_contact" or "make_call"
           - Messaging: "Send text to John saying I'm on my way" -> type: "send_sms"
           - Screen Interaction: "Click Subscribe", "Tap Search", "Type lo-fi beats into search" -> type: "click" or "type_text"
           - Watchdog Analysis: "What is on my screen?", "Analyze this" -> type: "inspect_screen"
           - General Conversation: Any chit-chat, advice, math, science, questions -> type: "general_response"

        OUTPUT FORMAT:
        Always respond with a single valid JSON object:
        {
          "thought": "<Brief internal reasoning, e.g. 'User wants to watch reels, scrolling up.'>",
          "command": {
             "type": "open_app" | "inspect_screen" | "scroll" | "click" | "type_text" | "find_contact" | "make_call" | "send_sms" | "general_response",
             "app_name": "<app to open if open_app>",
             "direction": "<'up' for next reel / scroll down, 'down' for previous reel>",
             "times": 1,
             "target_text": "<text of button to click>",
             "target": "search_box" | "chat",
             "text": "<text to type>",
             "name": "<contact name to search>",
             "phone_number": "<phone number>",
             "contact_name": "<contact name>",
             "message": "<SMS message text>"
          },
          "assistant_response": "<Natural, conversational Jarvis voice response to be spoken aloud and shown in chat>"
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

    fun parseDecision(rawJson: String, fallbackPrompt: String): AgentDecision {
        return try {
            val startIdx = rawJson.indexOf('{')
            val endIdx = rawJson.lastIndexOf('}')
            val cleanJson = if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                rawJson.substring(startIdx, endIdx + 1)
            } else {
                rawJson.trim()
            }

            val root = json.parseToJsonElement(cleanJson).jsonObject
            val thought = root["thought"]?.jsonPrimitive?.content ?: "Jarvis processing"
            val assistantResponse = root["assistant_response"]?.jsonPrimitive?.content
                ?: "At your service, sir. Processing your request."

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
            Log.w("AgentBrainOrchestrator", "Fallback parsing for response: $rawJson", e)
            val cleanText = rawJson.replace("```json", "").replace("```", "").trim()
            AgentDecision(
                thought = "Jarvis direct reasoning",
                command = AgentCommand.GeneralResponse(cleanText.take(150)),
                assistant_response = cleanText.ifBlank { "At your service, sir." }
            )
        }
    }
}
