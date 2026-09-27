package com.editog.novaagent.automation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat

class TelephonyHelper(private val context: Context) {

    fun dialCall(phoneNumber: String, directCall: Boolean = true): Boolean {
        return try {
            val hasCallPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
            val action = if (directCall && hasCallPermission) Intent.ACTION_CALL else Intent.ACTION_DIAL
            val intent = Intent(action, Uri.parse("tel:${phoneNumber.trim()}"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
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
            val sendIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("sms:${phoneNumber.trim()}")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(sendIntent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
