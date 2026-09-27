package com.editog.novaagent.automation

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

class AppLauncher(private val context: Context) {

    data class AppMetadata(
        val label: String,
        val packageName: String,
        val isSystem: Boolean
    )

    fun getInstalledApps(): List<AppMetadata> {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        return packages.mapNotNull { appInfo ->
            val label = pm.getApplicationLabel(appInfo).toString()
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (pm.getLaunchIntentForPackage(appInfo.packageName) != null) {
                AppMetadata(label, appInfo.packageName, isSystem)
            } else {
                null
            }
        }
    }

    fun openAppByName(query: String): Boolean {
        val cleanedQuery = query.lowercase().trim()
        val apps = getInstalledApps()

        // 1. Exact match
        var match = apps.firstOrNull { it.label.lowercase() == cleanedQuery }

        // 2. Starts with / contains match
        if (match == null) {
            match = apps.firstOrNull { it.label.lowercase().contains(cleanedQuery) || cleanedQuery.contains(it.label.lowercase()) }
        }

        // 3. Common aliases
        if (match == null) {
            val aliasMap = mapOf(
                "ig" to "instagram",
                "yt" to "youtube",
                "wa" to "whatsapp",
                "fb" to "facebook",
                "play store" to "vending",
                "messages" to "messaging"
            )
            val resolvedName = aliasMap[cleanedQuery]
            if (resolvedName != null) {
                match = apps.firstOrNull { it.label.lowercase().contains(resolvedName) || it.packageName.contains(resolvedName) }
            }
        }

        return if (match != null) {
            launchPackage(match.packageName)
        } else {
            false
        }
    }

    fun launchPackage(packageName: String): Boolean {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        context.startActivity(intent)
        return true
    }
}
