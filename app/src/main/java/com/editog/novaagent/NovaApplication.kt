package com.editog.novaagent

import android.app.Application
import android.os.Looper
import android.util.Log
import com.editog.novaagent.automation.AppLauncher
import com.editog.novaagent.automation.ContactsHelper
import com.editog.novaagent.automation.TelephonyHelper
import com.editog.novaagent.automation.VoiceManager
import com.editog.novaagent.data.api.AgentBrainOrchestrator
import com.editog.novaagent.data.model.*
import com.editog.novaagent.data.repository.ApiConfigRepository
import com.editog.novaagent.data.repository.ChatRepository
import com.editog.novaagent.data.repository.SettingsRepository
import com.editog.novaagent.data.repository.VoicemailRepository
import com.editog.novaagent.service.AgentAccessibilityService
import com.editog.novaagent.service.DynamicIslandService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NovaApplication : Application() {

    init {
        instance = this
    }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val apiConfigRepository: ApiConfigRepository by lazy { ApiConfigRepository(this) }
    val chatRepository: ChatRepository by lazy { ChatRepository(this) }
    val voicemailRepository: VoicemailRepository by lazy { VoicemailRepository(this) }

    val appLauncher: AppLauncher by lazy { AppLauncher(this) }
    val contactsHelper: ContactsHelper by lazy { ContactsHelper(this) }
    val telephonyHelper: TelephonyHelper by lazy { TelephonyHelper(this) }
    val voiceManager: VoiceManager by lazy {
        VoiceManager(this).also { vm ->
            vm.onSpeechFinalResult = { speechText ->
                processGlobalCommand(speechText)
            }
        }
    }
    val brainOrchestrator: AgentBrainOrchestrator by lazy { AgentBrainOrchestrator() }

    private val appScope by lazy { CoroutineScope(Dispatchers.Main + SupervisorJob()) }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Global uncaught crash guard
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("NovaApplication", "Uncaught exception on thread ${thread.name}", throwable)
            if (Looper.myLooper() == Looper.getMainLooper()) {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    fun processGlobalCommand(userPrompt: String) {
        val trimmed = userPrompt.trim()
        if (trimmed.isBlank()) return

        try {
            DynamicIslandService.postAction("Heard: \"$trimmed\"")
            chatRepository.addMessage(ChatMessage(sender = MessageSender.USER, content = trimmed))

            val lower = trimmed.lowercase()

            // 1. FAST LOCAL ACTION DISPATCH
            if (lower.startsWith("open ") || lower.startsWith("launch ") || lower.startsWith("start ")) {
                val appQuery = lower.removePrefix("open ").removePrefix("launch ").removePrefix("start ").trim()
                val opened = appLauncher.openAppByName(appQuery)
                if (opened) {
                    val resp = "Opening $appQuery now."
                    voiceManager.speak(resp)
                    DynamicIslandService.postAction("Opened $appQuery")
                    chatRepository.addMessage(ChatMessage(sender = MessageSender.AGENT, content = resp, actionBadge = "Opened $appQuery"))
                    return
                }
            }

            if (lower.contains("scroll") || lower.contains("next reel") || lower.contains("next short") || lower.contains("next video")) {
                val dir = if (lower.contains("down") || lower.contains("previous")) "down" else "up"
                val service = AgentAccessibilityService.instance
                val scrolled = service?.scrollMedia(dir) ?: false
                if (scrolled) {
                    voiceManager.speak("Scrolled")
                    DynamicIslandService.postAction("Scrolled $dir")
                    chatRepository.addMessage(ChatMessage(sender = MessageSender.AGENT, content = "Scrolled $dir.", actionBadge = "Scrolled $dir"))
                } else {
                    voiceManager.speak("Please enable Nova Agent Accessibility Service in Settings to scroll.")
                    DynamicIslandService.postAction("Accessibility required")
                }
                return
            }

            if (lower == "play" || lower == "pause" || lower.contains("pause video") || lower.contains("play video") || lower.contains("stop video")) {
                val service = AgentAccessibilityService.instance
                val toggled = service?.togglePlayPause() ?: false
                if (toggled) {
                    voiceManager.speak("Toggled playback")
                    DynamicIslandService.postAction("Play/Pause")
                    chatRepository.addMessage(ChatMessage(sender = MessageSender.AGENT, content = "Toggled video playback.", actionBadge = "Play/Pause"))
                } else {
                    voiceManager.speak("Accessibility needed to tap screen.")
                }
                return
            }

            if (lower.startsWith("call ") || lower.startsWith("dial ")) {
                val targetName = lower.removePrefix("call ").removePrefix("dial ").trim()
                val contacts = contactsHelper.searchContacts(targetName)
                if (contacts.isEmpty()) {
                    val resp = "Could not find any contact named $targetName."
                    voiceManager.speak(resp)
                    chatRepository.addMessage(ChatMessage(sender = MessageSender.AGENT, content = resp))
                } else if (contacts.size == 1) {
                    val single = contacts.first()
                    telephonyHelper.dialCall(single.phoneNumber)
                    voiceManager.speak("Calling ${single.name} at ${single.phoneNumber}")
                    DynamicIslandService.postAction("Calling ${single.name}")
                    chatRepository.addMessage(ChatMessage(sender = MessageSender.AGENT, content = "Calling ${single.name} (${single.phoneNumber}).", actionBadge = "Calling ${single.name}"))
                } else {
                    val resp = "Found ${contacts.size} numbers for $targetName. Please open Nova Agent to select a row number."
                    voiceManager.speak(resp)
                    chatRepository.addMessage(
                        ChatMessage(
                            sender = MessageSender.AGENT,
                            content = resp,
                            pendingConfirmation = ActionPendingConfirmation(
                                actionType = "CALL",
                                targetName = targetName,
                                targetNumber = contacts.first().phoneNumber,
                                candidateRows = contacts
                            )
                        )
                    )
                }
                return
            }

            if (lower.startsWith("type ") || lower.startsWith("search for ") || lower.startsWith("search ")) {
                val textToType = lower.removePrefix("type ").removePrefix("search for ").removePrefix("search ").trim()
                val service = AgentAccessibilityService.instance
                val typed = service?.typeTextIntoInput("", textToType) ?: false
                if (typed) {
                    voiceManager.speak("Typed $textToType")
                    DynamicIslandService.postAction("Typed: $textToType")
                    chatRepository.addMessage(ChatMessage(sender = MessageSender.AGENT, content = "Typed: \"$textToType\".", actionBadge = "Typed Text"))
                } else {
                    voiceManager.speak("No input field focused. Please tap an input box first.")
                }
                return
            }

            // 2. AI BRAIN REASONING (Gemini / AI Studio)
            val config = apiConfigRepository.config.value
            if (config.apiKey.isBlank()) {
                val noKeyMessage = "I heard: \"$trimmed\". To enable AI reasoning, please open Nova Agent and paste your Gemini API Studio key in the API Setup tab."
                voiceManager.speak(noKeyMessage)
                DynamicIslandService.postAction("API Key Required")
                chatRepository.addMessage(ChatMessage(sender = MessageSender.AGENT, content = noKeyMessage))
                return
            }

            appScope.launch {
                try {
                    val screenContext = AgentAccessibilityService.instance?.inspectCurrentScreen()
                    val result = brainOrchestrator.processIntent(trimmed, screenContext, config)

                    result.onSuccess { decision ->
                        voiceManager.speak(decision.assistant_response)
                        DynamicIslandService.postAction(decision.assistant_response.take(24))

                        when (val cmd = decision.command) {
                            is AgentCommand.OpenApp -> appLauncher.openAppByName(cmd.app_name)
                            is AgentCommand.Scroll -> AgentAccessibilityService.instance?.scrollMedia(cmd.direction)
                            is AgentCommand.Click -> AgentAccessibilityService.instance?.clickElement(cmd.target_text)
                            is AgentCommand.TypeText -> AgentAccessibilityService.instance?.typeTextIntoInput(cmd.target, cmd.text)
                            is AgentCommand.MakeCall -> telephonyHelper.dialCall(cmd.phone_number)
                            is AgentCommand.SendSms -> telephonyHelper.sendSms(cmd.phone_number, cmd.message)
                            else -> {}
                        }

                        chatRepository.addMessage(
                            ChatMessage(sender = MessageSender.AGENT, content = decision.assistant_response)
                        )
                    }.onFailure { err ->
                        val errMsg = "Error from AI Studio: ${err.localizedMessage ?: "Unknown error"}. Please check your API key."
                        voiceManager.speak(errMsg)
                        chatRepository.addMessage(ChatMessage(sender = MessageSender.AGENT, content = errMsg))
                    }
                } catch (e: Throwable) {
                    Log.e("NovaApplication", "Error during AI reasoning launch", e)
                }
            }
        } catch (e: Throwable) {
            Log.e("NovaApplication", "Error processing global command", e)
        }
    }

    companion object {
        var instance: NovaApplication? = null
            private set
    }
}
