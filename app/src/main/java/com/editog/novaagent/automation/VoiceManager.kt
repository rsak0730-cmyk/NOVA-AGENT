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
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.content.ContextCompat
import com.editog.novaagent.service.DynamicIslandService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceManager(private val context: Context) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    var onSpeechFinalResult: ((String) -> Unit)? = null

    private fun attachUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }
            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
            }
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
            }
        })
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        mainHandler.post {
            try {
                if (tts == null) {
                    tts = TextToSpeech(context.applicationContext) { status ->
                        if (status == TextToSpeech.SUCCESS) {
                            try {
                                tts?.language = Locale.US
                                tts?.setPitch(1.0f)
                                tts?.setSpeechRate(1.05f)
                                attachUtteranceListener()
                                _isSpeaking.value = true
                                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "NovaAgentTTS_${System.currentTimeMillis()}")
                            } catch (e: Throwable) {
                                Log.e("VoiceManager", "Error speaking after init", e)
                                _isSpeaking.value = false
                            }
                        }
                    }
                } else {
                    attachUtteranceListener()
                    _isSpeaking.value = true
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "NovaAgentTTS_${System.currentTimeMillis()}")
                }
            } catch (e: Throwable) {
                Log.e("VoiceManager", "TTS error", e)
                _isSpeaking.value = false
            }
        }
    }

    fun stopSpeaking() {
        mainHandler.post {
            try {
                tts?.stop()
            } catch (e: Throwable) {}
            _isSpeaking.value = false
        }
    }

    fun toggleListening() {
        mainHandler.post {
            if (_isListening.value) {
                stopListeningInternal()
            } else {
                startListeningInternal()
            }
        }
    }

    fun startListening() {
        mainHandler.post {
            startListeningInternal()
        }
    }

    fun stopListening() {
        mainHandler.post {
            stopListeningInternal()
        }
    }

    private fun startListeningInternal() {
        stopSpeaking()

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w("VoiceManager", "RECORD_AUDIO permission not granted")
            speak("Microphone permission needed. Please allow microphone in settings.")
            _isListening.value = false
            DynamicIslandService.postAction("Mic Permission Needed")
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w("VoiceManager", "Speech recognition not available")
            speak("Speech recognition service is not available on this device.")
            _isListening.value = false
            DynamicIslandService.postAction("Speech Service Unavailable")
            return
        }

        try {
            // Clean up previous recognizer instance completely
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Throwable) {}

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                    DynamicIslandService.postAction("🎙️ Listening... Speak now")
                }
                override fun onBeginningOfSpeech() {
                    _isListening.value = true
                }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    _isListening.value = false
                }
                override fun onError(error: Int) {
                    _isListening.value = false
                    Log.w("VoiceManager", "Speech recognition error: $error")
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> DynamicIslandService.postAction("No speech heard")
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> DynamicIslandService.postAction("Listening timeout")
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> DynamicIslandService.postAction("Network timeout")
                        SpeechRecognizer.ERROR_AUDIO -> DynamicIslandService.postAction("Audio error")
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> DynamicIslandService.postAction("Mic permission needed")
                        else -> DynamicIslandService.postAction("Listening ended ($error)")
                    }
                }
                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    if (text.isNotBlank()) {
                        _recognizedText.value = text
                        onSpeechFinalResult?.invoke(text)
                    } else {
                        DynamicIslandService.postAction("No words recognized")
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    if (text.isNotBlank()) {
                        _recognizedText.value = text
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            speechRecognizer?.startListening(intent)
            _isListening.value = true
            DynamicIslandService.postAction("🎙️ Listening...")
        } catch (e: Throwable) {
            Log.e("VoiceManager", "Error starting speech recognizer", e)
            _isListening.value = false
            DynamicIslandService.postAction("Mic error: ${e.localizedMessage?.take(16)}")
        }
    }

    private fun stopListeningInternal() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (e: Throwable) {}
        _isListening.value = false
    }

    fun release() {
        mainHandler.post {
            try {
                tts?.stop()
                tts?.shutdown()
                tts = null
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Throwable) {}
        }
    }
}
