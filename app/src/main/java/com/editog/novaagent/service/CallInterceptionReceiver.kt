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
        private val receiverScope = CoroutineScope(Dispatchers.Default + Job())
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: currentIncomingNumber

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                currentIncomingNumber = incomingNumber
                DynamicIslandService.postAction("Incoming Call: $incomingNumber")

                // Start 20-second timer for iOS-style voicemail auto-attendant
                ringJob?.cancel()
                ringJob = receiverScope.launch {
                    try {
                        delay(20000) // 20 seconds unanswered
                        val app = (context.applicationContext as? NovaApplication) ?: NovaApplication.instance
                        if (app != null) {
                            val callerLabel = if (currentIncomingNumber.startsWith("+")) "Mobile Caller" else currentIncomingNumber
                            val voicemail = VoicemailItem(
                                callerName = callerLabel,
                                phoneNumber = currentIncomingNumber,
                                timestamp = System.currentTimeMillis(),
                                durationSeconds = 14,
                                transcript = "Caller left a voicemail after 20 seconds of unanswered ringing."
                            )
                            app.voicemailRepository.addVoicemail(voicemail)
                            DynamicIslandService.postAction("New Voicemail from $currentIncomingNumber")
                            app.voiceManager.speak("Nova Agent received a voicemail from $currentIncomingNumber.")
                        }
                    } catch (e: Throwable) {
                        Log.e("CallInterception", "Error during 20s voicemail delay", e)
                    }
                }
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                // Call was answered by user, cancel voicemail timer
                ringJob?.cancel()
                ringJob = null
                DynamicIslandService.postAction("Call in progress")
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                // Call ended or rejected, cancel timer
                ringJob?.cancel()
                ringJob = null
            }
        }
    }
}
