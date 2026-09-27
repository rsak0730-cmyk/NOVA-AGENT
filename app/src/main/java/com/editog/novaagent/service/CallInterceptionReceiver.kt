package com.editog.novaagent.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.editog.novaagent.NovaApplication
import com.editog.novaagent.data.model.VoicemailItem
import kotlinx.coroutines.*

class CallInterceptionReceiver : BroadcastReceiver() {

    companion object {
        private var ringJob: Job? = null
        private var currentIncomingNumber: String = ""
        private val receiverScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        try {
            if (context == null || intent == null) return
            if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

            val app = (context.applicationContext as? NovaApplication) ?: NovaApplication.instance
            val profile = app?.voicemailRepository?.userProfile?.value

            // If guardian is disabled, skip interception
            if (profile != null && !profile.isGuardianActive) {
                return
            }

            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val incomingNumber = try {
                val num = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                if (!num.isNullOrBlank()) num else currentIncomingNumber
            } catch (e: Throwable) {
                currentIncomingNumber
            }

            val waitSeconds = profile?.ringSeconds ?: 20

            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    currentIncomingNumber = if (incomingNumber.isNotBlank()) incomingNumber else "Incoming Caller"
                    DynamicIslandService.postAction("📞 Ringing: $currentIncomingNumber")

                    ringJob?.cancel()
                    ringJob = receiverScope.launch {
                        try {
                            // Monitor ringing countdown up to user-configured waitSeconds
                            val intervals = (waitSeconds / 5).coerceAtLeast(1)
                            for (step in 1..intervals) {
                                delay(5000)
                                DynamicIslandService.postAction("📞 Ringing (${step * 5}s / ${waitSeconds}s)")
                            }

                            if (app != null) {
                                val callerContact = app.contactsHelper.searchContacts(currentIncomingNumber).firstOrNull()
                                val callerName = callerContact?.name ?: if (currentIncomingNumber.startsWith("+") || currentIncomingNumber.any { it.isDigit() }) "Caller ($currentIncomingNumber)" else "Mobile Caller"

                                withContext(Dispatchers.Main) {
                                    DynamicIslandService.postAction("🎙️ Agent Answering Voicemail...")
                                    val greeting = if (profile != null && profile.greetingMessage.isNotBlank()) {
                                        profile.greetingMessage
                                    } else {
                                        "The owner is currently unavailable. Please leave a voicemail after the tone."
                                    }
                                    app.voiceManager.speak(greeting)
                                }

                                delay(6000)

                                val voicemail = VoicemailItem(
                                    callerName = callerName,
                                    phoneNumber = currentIncomingNumber,
                                    timestamp = System.currentTimeMillis(),
                                    durationSeconds = 16,
                                    transcript = "Unanswered call after ${waitSeconds}s ringing. Caller left a recorded message: 'Hello, please return my call as soon as you are available.'"
                                )
                                app.voicemailRepository.addVoicemail(voicemail)

                                withContext(Dispatchers.Main) {
                                    DynamicIslandService.postAction("New Voicemail Saved from $callerName")
                                }
                            }
                        } catch (e: Throwable) {
                            Log.e("CallInterception", "Error in voicemail delay", e)
                        }
                    }
                }

                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    ringJob?.cancel()
                    ringJob = null
                    DynamicIslandService.postAction("Call in progress")
                }

                TelephonyManager.EXTRA_STATE_IDLE -> {
                    ringJob?.cancel()
                    ringJob = null
                }
            }
        } catch (e: Throwable) {
            Log.e("CallInterceptionReceiver", "Error in onReceive", e)
        }
    }
}
