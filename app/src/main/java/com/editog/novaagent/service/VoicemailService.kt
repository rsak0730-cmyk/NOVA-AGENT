package com.editog.novaagent.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.editog.novaagent.NovaApplication
import com.editog.novaagent.data.model.VoicemailItem
import kotlinx.coroutines.*

class VoicemailService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val incomingNumber = intent?.getStringExtra("INCOMING_NUMBER") ?: "Unknown Caller"

        serviceScope.launch {
            // Trigger Dynamic Island Notification
            withContext(Dispatchers.Main) {
                DynamicIslandService.postAction("Recording Voicemail: $incomingNumber")
            }

            // Speak greeting to caller (simulated or telecom audio route)
            val app = application as? NovaApplication
            withContext(Dispatchers.Main) {
                app?.voiceManager?.speak("The recipient is currently unavailable. Please leave a voicemail for Nova Agent after the tone.")
            }

            delay(6000)

            // Create new Voicemail Item in repository
            val voicemail = VoicemailItem(
                callerName = if (incomingNumber.startsWith("+")) "Mobile Caller" else incomingNumber,
                phoneNumber = incomingNumber,
                timestamp = System.currentTimeMillis(),
                durationSeconds = 14,
                transcript = "Hello! Called regarding the update. Please check back when you get a chance."
            )

            app?.voicemailRepository?.addVoicemail(voicemail)

            withContext(Dispatchers.Main) {
                DynamicIslandService.postAction("New Voicemail Saved from $incomingNumber")
            }

            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
