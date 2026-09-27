package com.editog.novaagent.automation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager

class TelephonyHelper(private val context: Context) {

    fun dialCall(phoneNumber: String, directCall: Boolean = true): Boolean {
        return try {
            val action = if (directCall) Intent.ACTION_CALL else Intent.ACTION_DIAL
            val intent = Intent(action, Uri.parse("tel:${phoneNumber.trim()}"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to dialer if direct call permission is not granted
            try {
                val fallback = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phoneNumber.trim()}"))
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(fallback)
                true
            } catch (e2: Exception) {
                false
            }
        }
    }

    fun sendSms(phoneNumber: String, message: String): Boolean {
        return try {
            val smsManager = context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to SMS app intent
            try {
                val sendIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("sms:$phoneNumber")
                    putExtra("sms_body", message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(sendIntent)
                true
            } catch (e2: Exception) {
                false
            }
        }
    }
}
