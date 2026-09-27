package com.editog.novaagent.automation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import com.editog.novaagent.data.repository.SettingsRepository
import com.editog.novaagent.service.DynamicIslandService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale

class WakeWordManager(
    private val context: Context,
    private val voiceManager: VoiceManager,
    private val settingsRepository: SettingsRepository
) {
    companion object {
        private const val TAG = "WakeWordManager"

        private val WAKE_PHRASES = listOf(
            "wake up", "hey jarvis", "jarvis wake up", "wake up jarvis",
            "hey jarvis wake up", "ok jarvis", "jarvis online", "wake up nova"
        )

        private val SLEEP_PHRASES = listOf(
            "turn off", "hey jarvis turn off", "jarvis turn off", "turn off jarvis",
            "go to sleep", "sleep jarvis", "stop listening", "sleep now"
        )
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var standbyRecognizer: SpeechRecognizer? = null
    private var isStandbyListening = false
    private var restartRunnable: Runnable? = null

    private val _isWakeWordListening = MutableStateFlow(false)
    val isWakeWordListening: StateFlow<Boolean> = _isWakeWordListening.asStateFlow()

    fun startIfEnabled() {
        scope.launch {
            // Listen to settings toggle
            settingsRepository.settings.collectLatest { settings ->
                if (settings.wakeWordEnabled) {
                    observeStatesAndRun()
                } else {
                    stopWakeWordListening()
                }
            }
        }
    }

    private fun observeStatesAndRun() {
        scope.launch {
            // Re-evaluate whenever active listening or TTS speaking states change
            launch {
                voiceManager.isListening.collectLatest { listening ->
                    if (listening) {
                        stopWakeWordListening()
                    } else {
                        scheduleWakeWordRestart(400)
                    }
                }
            }
            launch {
                voiceManager.isSpeaking.collectLatest { speaking ->
                    if (speaking) {
                        stopWakeWordListening()
                    } else {
                        scheduleWakeWordRestart(500)
                    }
                }
            }
        }
    }

    private fun scheduleWakeWordRestart(delayMs: Long) {
        restartRunnable?.let { mainHandler.removeCallbacks(it) }
        restartRunnable = Runnable {
            val enabled = settingsRepository.settings.value.wakeWordEnabled
            val activeListening = voiceManager.isListening.value
            val isSpeaking = voiceManager.isSpeaking.value
            if (enabled && !activeListening && !isSpeaking) {
                startStandbyListeningInternal()
            }
        }
        mainHandler.postDelayed(restartRunnable!!, delayMs)
    }

    private fun startStandbyListeningInternal() {
        if (isStandbyListening) return

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Cannot start wake word: RECORD_AUDIO not granted")
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "SpeechRecognizer not available for wake word")
            return
        }

        try {
            cleanupRecognizer()

            standbyRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            standbyRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isStandbyListening = true
                    _isWakeWordListening.value = true
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    isStandbyListening = false
                    _isWakeWordListening.value = false
                    scheduleWakeWordRestart(400)
                }

                override fun onError(error: Int) {
                    isStandbyListening = false
                    _isWakeWordListening.value = false
                    Log.d(TAG, "Standby wake listener error ($error), scheduling auto-restart")
                    scheduleWakeWordRestart(errorDelay(error))
                }

                override fun onResults(results: Bundle?) {
                    isStandbyListening = false
                    _isWakeWordListening.value = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf()
                    checkHeardPhrases(matches)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf()
                    checkHeardPhrases(matches)
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            standbyRecognizer?.startListening(intent)
            isStandbyListening = true
            _isWakeWordListening.value = true
            Log.d(TAG, "Wake word standby listening active in background")
        } catch (e: Throwable) {
            Log.e(TAG, "Error starting standby recognizer", e)
            isStandbyListening = false
            _isWakeWordListening.value = false
            scheduleWakeWordRestart(1000)
        }
    }

    private fun checkHeardPhrases(matches: List<String>) {
        for (raw in matches) {
            val lower = raw.lowercase().trim()

            // 1. Check for WAKE phrases
            val isWake = WAKE_PHRASES.any { phrase -> lower.contains(phrase) }
            if (isWake) {
                Log.i(TAG, "Wake word detected: \"$lower\"")
                stopWakeWordListening()
                mainHandler.post {
                    voiceManager.speak("Jarvis online. What can I do for you, sir?")
                    DynamicIslandService.postAction("🎙️ Jarvis Awake")
                    // Start active command recognition after short greeting
                    mainHandler.postDelayed({
                        voiceManager.startListening()
                    }, 1500)
                }
                return
            }

            // 2. Check for SLEEP phrases
            val isSleep = SLEEP_PHRASES.any { phrase -> lower.contains(phrase) }
            if (isSleep) {
                Log.i(TAG, "Sleep word detected: \"$lower\"")
                stopWakeWordListening()
                mainHandler.post {
                    voiceManager.speak("Deactivating listening mode, sir. Say 'Hey Jarvis wake up' whenever you need me.")
                    DynamicIslandService.postAction("💤 Jarvis Standby")
                    voiceManager.stopListening()
                    scheduleWakeWordRestart(2500)
                }
                return
            }
        }
    }

    private fun errorDelay(errorCode: Int): Long {
        return when (errorCode) {
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 800
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 350
            SpeechRecognizer.ERROR_NO_MATCH -> 300
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> 1200
            else -> 600
        }
    }

    private fun stopWakeWordListening() {
        restartRunnable?.let { mainHandler.removeCallbacks(it) }
        isStandbyListening = false
        _isWakeWordListening.value = false
        mainHandler.post {
            cleanupRecognizer()
        }
    }

    private fun cleanupRecognizer() {
        try {
            standbyRecognizer?.stopListening()
            standbyRecognizer?.cancel()
            standbyRecognizer?.destroy()
            standbyRecognizer = null
        } catch (e: Throwable) {}
    }

    fun release() {
        stopWakeWordListening()
        scope.cancel()
    }
}
