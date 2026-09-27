package com.editog.novaagent

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import com.editog.novaagent.automation.AppLauncher
import com.editog.novaagent.automation.ContactsHelper
import com.editog.novaagent.automation.TelephonyHelper
import com.editog.novaagent.automation.VoiceManager
import com.editog.novaagent.automation.ShizukuManager
import com.editog.novaagent.automation.WakeWordManager
import com.editog.novaagent.data.api.AgentBrainOrchestrator
import com.editog.novaagent.data.model.*
import com.editog.novaagent.data.repository.ApiConfigRepository
import com.editog.novaagent.data.repository.ChatRepository
import com.editog.novaagent.data.repository.SettingsRepository
import com.editog.novaagent.data.repository.VoicemailRepository
import com.editog.novaagent.service.AgentAccessibilityService
import com.editog.novaagent.service.CallInterceptionReceiver
import com.editog.novaagent.service.DynamicIslandService
import com.editog.novaagent.ui.CrashReportActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NovaApplication : Application() {

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
    val shizukuManager: ShizukuManager by lazy { ShizukuManager(this) }
    val wakeWordManager: WakeWordManager by lazy { WakeWordManager(this, voiceManager, settingsRepository) }
    val brainOrchestrator: AgentBrainOrchestrator by lazy { AgentBrainOrchestrator() }

    private val appScope by lazy { CoroutineScope(Dispatchers.Main + SupervisorJob()) }
    private var callReceiver: CallInterceptionReceiver? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        setupCrashHandler()
        registerCallReceiverDynamically()
        tryStartDynamicIsland()
        shizukuManager.init()
        wakeWordManager.startIfEnabled()
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e("NovaAgent", "FATAL UNCAUGHT EXCEPTION on thread: ${thread.name}", throwable)
                val sw = java.io.StringWriter()
                throwable.printStackTrace(java.io.PrintWriter(sw))
                val stackTrace = sw.toString()

                val intent = Intent(this, CrashReportActivity::class.java).apply {
                    putExtra(CrashReportActivity.EXTRA_ERROR_MESSAGE, throwable.localizedMessage ?: throwable.javaClass.simpleName)
                    putExtra(CrashReportActivity.EXTRA_STACK_TRACE, stackTrace)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(10)
            } catch (e: Throwable) {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun registerCallReceiverDynamically() {
        try {
            if (callReceiver == null) {
                callReceiver = CallInterceptionReceiver()
                val filter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
                registerReceiver(callReceiver, filter)
            }
        } catch (e: Throwable) {
            Log.e("NovaApplication", "Error registering CallInterceptionReceiver dynamically", e)
        }
    }

    fun tryStartDynamicIsland() {
        try {
            if (Settings.canDrawOverlays(this) && settingsRepository.settings.value.dynamicIslandEnabled) {
                val intent = Intent(this, DynamicIslandService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }
        } catch (e: Throwable) {
            Log.e("NovaApplication", "Failed to start DynamicIslandService in app init", e)
        }
    }

    fun processGlobalCommand(userPrompt: String) {
        val trimmed = userPrompt.trim()
        if (trimmed.isBlank()) return

        try {
            DynamicIslandService.postAction("Heard: \"$trimmed\"")
            chatRepository.addMessage(ChatMessage(sender = MessageSender.USER, content = trimmed))

            val rawLower = trimmed.lowercase()

            // 0. WAKE UP & SLEEP (VOICE COMMAND CONTROL OF LISTENING MODE)
            if (rawLower == "wake up" || rawLower == "turn on listening" || rawLower == "start listening" ||
                rawLower == "wake up jarvis" || rawLower == "hey jarvis wake up" || rawLower == "jarvis wake up") {
                val resp = "I am awake and listening, sir. What are your orders?"
                voiceManager.speak(resp)
                DynamicIslandService.postAction("🎙️ Jarvis Awake")
                chatRepository.addMessage(ChatMessage(sender = MessageSender.AGENT, content = resp, actionBadge = "Awake"))
                voiceManager.startListening()
                return
            }

            if (rawLower == "turn off" || rawLower == "go to sleep" || rawLower == "sleep" ||
                rawLower == "stop listening" || rawLower == "turn off jarvis" || rawLower == "hey jarvis turn off" || rawLower == "jarvis turn off") {
                val resp = "Deactivating listening mode, sir. Say 'Hey Jarvis wake up' whenever you need me."
                voiceManager.speak(resp)
                DynamicIslandService.postAction("💤 Jarvis Standby")
                chatRepository.addMessage(ChatMessage(sender = MessageSender.AGENT, content = resp, actionBadge = "Standby"))
                voiceManager.stopListening()
                return
            }

            val lower = trimmed.lowercase()
                .removePrefix("hey jarvis").removePrefix("jarvis")
                .removePrefix("hey nova").removePrefix("nova")
                .removePrefix("can you please").removePrefix("could you please")
                .removePrefix("can you").removePrefix("could you")
                .removePrefix("please").trim()

            // SYSTEM NAVIGATION KEYS (HOME / BACK / RECENTS)
            if (lower == "go back" || lower == "back") {
                val handled = if (shizukuManager.isAvailableAndAuthorized()) {
                    shizukuManager.pressBack()
                } else {
                    AgentAccessibilityService.instance?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK) ?: false
                }
                if (handled) {
                    voiceManager.speak("Went back.")
                    DynamicIslandService.postAction("Key: Back")
                }
                return
            }

            if (lower == "go home" || lower == "home screen" || lower == "home") {
                val handled = if (shizukuManager.isAvailableAndAuthorized()) {
                    shizukuManager.pressHome()
                } else {
                    AgentAccessibilityService.instance?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME) ?: false
                }
                if (handled) {
                    voiceManager.speak("Went to home screen.")
                    DynamicIslandService.postAction("Key: Home")
                }
                return
            }

            if (lower == "recent apps" || lower == "recents" || lower == "switch app" || lower == "app switcher") {
                val handled = if (shizukuManager.isAvailableAndAuthorized()) {
                    shizukuManager.pressRecents()
                } else {
                    AgentAccessibilityService.instance?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS) ?: false
                }
                if (handled) {
                    voiceManager.speak("Opened recent apps.")
                    DynamicIslandService.postAction("Key: Recents")
                }
                return
            }

            // 1. GREETINGS & JARVIS CONVERSATIONAL ESSENTIALS
            if (lower == "hello" || lower == "hi" || lower == "hey" || lower == "who are you" ||
                lower == "what can you do" || lower == "what is your name" || lower == "are you jarvis") {
                val jarvisIntro = "Greetings, sir. I am Jarvis (Nova Agent), your autonomous personal companion. All systems are online and at your service. I can launch apps, dial contacts, scroll reels, and converse naturally. What are your orders?"
                voiceManager.speak(jarvisIntro)
                DynamicIslandService.postAction("Jarvis Active")
                chatRepository.addMessage(ChatMessage(sender = MessageSender.AGENT, content = jarvisIntro))
                return
            }

            // 2. FAST APP LAUNCH
            if (lower.startsWith("open ") || lower.startsWith("launch ") || lower.startsWith("start ")) {
                val appQuery = lower.removePrefix("open ").removePrefix("launch ").removePrefix("start ").trim()
                val opened = appLauncher.openAppByName(appQuery)
                if (opened) {
                    val resp = "Opening $appQuery right away, sir."
                    voiceManager.speak(resp)
                    DynamicIslandService.postAction("Opened $appQuery")
                    chatRepository.addMessage(ChatMessage(sender = MessageSender.AGENT, content = resp, actionBadge = "Opened $appQuery"))
                } else {
                    val resp = "I couldn't locate \"$appQuery\" installed on this device."
                    voiceManager.speak(resp)
                    chatRepository.addMessage(ChatMessage(sender = MessageSender.AGENT, content = resp))
                }
                return
            }

            // 3. FAST MEDIA SCROLLING
            if (lower.contains("scroll down") || lower.contains("next reel") || lower.contains("next short") ||
                lower.contains("next video") || lower == "next" || lower == "scroll next") {
                val scrolled = if (shizukuManager.isAvailableAndAuthorized()) {
                    shizukuManager.scrollDown()
                } else {
                    AgentAccessibilityService.instance?.scrollMedia("up") ?: false
                }
                if (scrolled) {
                    voiceManager.speak("Scrolling to next video.")
                    DynamicIslandService.postAction("Scrolled Next")
                    chatRepository.addMessage(ChatMessage(sender = MessageSender.AGENT, content = "Scrolled to next media item.", actionBadge = "Scrolled Down"))
                } else {
                    voiceManager.speak("Accessibility or Shizuku permission needed to scroll the screen.")
                }
                return
            }

            if (lower.contains("scroll up") || lower.contains("previous reel") || lower.contains("previous video") || lower == "previous") {
                val scrolled = if (shizukuManager.isAvailableAndAuthorized()) {
                    shizukuManager.scrollUp()
                } else {
                    AgentAccessibilityService.instance?.scrollMedia("down") ?: false
                }
                if (scrolled) {
                    voiceManager.speak("Scrolling to previous video.")
                    DynamicIslandService.postAction("Scrolled Previous")
                    chatRepository.addMessage(ChatMessage(sender = MessageSender.AGENT, content = "Scrolled to previous media item.", actionBadge = "Scrolled Up"))
                } else {
                    voiceManager.speak("Accessibility or Shizuku permission needed to scroll the screen.")
                }
                return
            }

            // 4. PLAY / PAUSE
            if (lower == "play" || lower == "pause" || lower == "resume" || lower.contains("play video") || lower.contains("pause video")) {
                val toggled = if (shizukuManager.isAvailableAndAuthorized()) {
                    shizukuManager.pressPlayPause()
                } else {
                    AgentAccessibilityService.instance?.togglePlayPause() ?: false
                }
                if (toggled) {
                    voiceManager.speak("Toggled playback.")
                    DynamicIslandService.postAction("Play/Pause")
                    chatRepository.addMessage(ChatMessage(sender = MessageSender.AGENT, content = "Toggled video playback.", actionBadge = "Play/Pause"))
                } else {
                    voiceManager.speak("Accessibility or Shizuku needed to tap video controls.")
                }
                return
            }

            // 5. CALLING & CONTACT SEARCH
            if (lower.startsWith("call ") || lower.startsWith("dial ") || lower.startsWith("phone ")) {
                val targetName = lower.removePrefix("call ").removePrefix("dial ").removePrefix("phone ").trim()
                val contacts = contactsHelper.searchContacts(targetName)
                if (contacts.isEmpty()) {
                    val resp = "Could not find any contact named \"$targetName\"."
                    voiceManager.speak(resp)
                    chatRepository.addMessage(ChatMessage(sender = MessageSender.AGENT, content = resp))
                } else if (contacts.size == 1) {
                    val single = contacts.first()
                    telephonyHelper.dialCall(single.phoneNumber)
                    voiceManager.speak("Calling ${single.name} now.")
                    DynamicIslandService.postAction("Calling ${single.name}")
                    chatRepository.addMessage(ChatMessage(sender = MessageSender.AGENT, content = "Calling ${single.name} (${single.phoneNumber}).", actionBadge = "Calling ${single.name}"))
                } else {
                    val resp = "Found ${contacts.size} entries for $targetName. Please select an option in the app."
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

            // 6. FAST CLICK / TAP ON ACTIVE SCREEN
            if (lower.startsWith("click ") || lower.startsWith("tap ") || lower.startsWith("press ")) {
                val targetText = lower.removePrefix("click ").removePrefix("tap ").removePrefix("press ").trim()
                val clicked = if (shizukuManager.isAvailableAndAuthorized()) {
                    shizukuManager.clickElementByTextOrDescription(targetText) ||
                    (AgentAccessibilityService.instance?.clickElement(targetText) ?: false)
                } else {
                    AgentAccessibilityService.instance?.clickElement(targetText) ?: false
                }
                if (clicked) {
                    val resp = "Clicked $targetText."
                    voiceManager.speak(resp)
                    DynamicIslandService.postAction("Clicked $targetText")
                    chatRepository.addMessage(ChatMessage(sender = MessageSender.AGENT, content = resp, actionBadge = "Clicked $targetText"))
                } else {
                    voiceManager.speak("Could not locate \"$targetText\" on the screen.")
                }
                return
            }

            // 7. FAST SCREEN INSPECTION
            if (lower.contains("what is on my screen") || lower.contains("what's on my screen") || lower.contains("inspect screen") || lower.contains("read screen")) {
                val service = AgentAccessibilityService.instance
                val screenContext = service?.inspectCurrentScreen() ?: "Screen unavailable"
                DynamicIslandService.postAction("Inspecting Screen")
                val cfg = apiConfigRepository.config.value
                if (cfg.apiKey.isNotBlank()) {
                    appScope.launch {
                        val result = brainOrchestrator.processIntent("Summarize and explain what is visible on this screen for the user.", screenContext, cfg)
                        result.onSuccess { decision ->
                            voiceManager.speak(decision.assistant_response)
                            DynamicIslandService.postAction("Screen Analyzed")
                            chatRepository.addMessage(ChatMessage(sender = MessageSender.AGENT, content = decision.assistant_response, actionBadge = "Watchdog Screen Analysis"))
                        }
                    }
                } else {
                    val resp = "Screen elements found:\n$screenContext"
                    voiceManager.speak("Screen inspected. To get deep AI reasoning, add your Gemini key in API Setup.")
                    chatRepository.addMessage(ChatMessage(sender = MessageSender.AGENT, content = resp, actionBadge = "Screen Context"))
                }
                return
            }

            // 8. TYPING INTO FOCUSED INPUT
            if (lower.startsWith("type ") || lower.startsWith("search for ") || lower.startsWith("search ")) {
                val textToType = lower.removePrefix("type ").removePrefix("search for ").removePrefix("search ").trim()
                val typed = if (shizukuManager.isAvailableAndAuthorized()) {
                    shizukuManager.typeText(textToType) ||
                    (AgentAccessibilityService.instance?.typeTextIntoInput("", textToType) ?: false)
                } else {
                    AgentAccessibilityService.instance?.typeTextIntoInput("", textToType) ?: false
                }
                if (typed) {
                    voiceManager.speak("Typed: $textToType")
                    DynamicIslandService.postAction("Typed: $textToType")
                    chatRepository.addMessage(ChatMessage(sender = MessageSender.AGENT, content = "Typed: \"$textToType\".", actionBadge = "Typed Text"))
                } else {
                    voiceManager.speak("No input field is currently active. Tap an input box first.")
                }
                return
            }

            // 9. AI BRAIN REASONING (Gemini / AI Studio or OpenAI)
            val config = apiConfigRepository.config.value
            if (config.apiKey.isBlank()) {
                val noKeyMessage = "At your command, sir. For deep conversational reasoning and Jarvis intelligence, please paste your Gemini API Studio key in the API Setup tab. You can still use voice commands to open apps, make calls, or scroll."
                voiceManager.speak(noKeyMessage)
                DynamicIslandService.postAction("API Key Setup Needed")
                chatRepository.addMessage(ChatMessage(sender = MessageSender.AGENT, content = noKeyMessage))
                return
            }

            // AI Processing
            DynamicIslandService.postAction("Nova: Thinking...")
            appScope.launch {
                try {
                    val screenContext = AgentAccessibilityService.instance?.inspectCurrentScreen()
                    val result = brainOrchestrator.processIntent(trimmed, screenContext, config)

                    result.onSuccess { decision ->
                        voiceManager.speak(decision.assistant_response)
                        DynamicIslandService.postAction(decision.assistant_response.take(28))

                        var badge: String? = null
                        when (val cmd = decision.command) {
                            is AgentCommand.OpenApp -> {
                                val opened = appLauncher.openAppByName(cmd.app_name)
                                badge = if (opened) "Opened ${cmd.app_name}" else "App not found"
                            }
                            is AgentCommand.Scroll -> {
                                val scrolled = if (shizukuManager.isAvailableAndAuthorized()) {
                                    if (cmd.direction.lowercase() == "down") shizukuManager.scrollUp() else shizukuManager.scrollDown()
                                } else {
                                    AgentAccessibilityService.instance?.scrollMedia(cmd.direction) ?: false
                                }
                                badge = if (scrolled) "Scrolled ${cmd.direction.uppercase()}" else "Accessibility or Shizuku required"
                            }
                            is AgentCommand.Click -> {
                                val clicked = if (shizukuManager.isAvailableAndAuthorized()) {
                                    shizukuManager.clickElementByTextOrDescription(cmd.target_text) ||
                                    (AgentAccessibilityService.instance?.clickElement(cmd.target_text) ?: false)
                                } else {
                                    AgentAccessibilityService.instance?.clickElement(cmd.target_text) ?: false
                                }
                                badge = if (clicked) "Clicked ${cmd.target_text}" else "Element not found"
                            }
                            is AgentCommand.TypeText -> {
                                val typed = if (shizukuManager.isAvailableAndAuthorized()) {
                                    shizukuManager.typeText(cmd.text) ||
                                    (AgentAccessibilityService.instance?.typeTextIntoInput(cmd.target, cmd.text) ?: false)
                                } else {
                                    AgentAccessibilityService.instance?.typeTextIntoInput(cmd.target, cmd.text) ?: false
                                }
                                badge = if (typed) "Typed \"${cmd.text}\"" else "No input field focused"
                            }
                            is AgentCommand.MakeCall -> {
                                telephonyHelper.dialCall(cmd.phone_number)
                                badge = "Calling ${cmd.contact_name}"
                            }
                            is AgentCommand.SendSms -> {
                                telephonyHelper.sendSms(cmd.phone_number, cmd.message)
                                badge = "Sent SMS to ${cmd.contact_name}"
                            }
                            is AgentCommand.FindContact -> {
                                val contacts = contactsHelper.searchContacts(cmd.name)
                                if (contacts.isNotEmpty()) {
                                    val single = contacts.first()
                                    telephonyHelper.dialCall(single.phoneNumber)
                                    badge = "Calling ${single.name}"
                                }
                            }
                            is AgentCommand.InspectScreen -> {
                                badge = "Screen Inspected"
                            }
                            else -> {}
                        }

                        chatRepository.addMessage(
                            ChatMessage(
                                sender = MessageSender.AGENT,
                                content = decision.assistant_response,
                                actionBadge = badge
                            )
                        )
                    }.onFailure { err ->
                        val errMsg = "Jarvis connection note: ${err.localizedMessage ?: "Unable to complete request"}. Please verify your API key and model in API Setup."
                        voiceManager.speak("I encountered an issue processing with the AI Studio model. Please check your API key in settings.")
                        DynamicIslandService.postAction("API Error")
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
