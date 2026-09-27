package com.editog.novaagent.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
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
                    delay(20000) // 20 seconds unanswered
                    // Start Voicemail Service
                    val vmIntent = Intent(context, VoicemailService::class.java).apply {
                        putExtra("INCOMING_NUMBER", currentIncomingNumber)
                    }
                    context.startService(vmIntent)
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
