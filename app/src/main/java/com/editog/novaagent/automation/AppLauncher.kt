package com.editog.novaagent.automation

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log

class AppLauncher(private val context: Context) {

    data class AppMetadata(
        val label: String,
        val packageName: String,
        val isSystem: Boolean
    )

    fun getInstalledApps(): List<AppMetadata> {
        val result = mutableMapOf<String, AppMetadata>()
        val pm = context.packageManager

        // Method 1: Query Launcher Intent Activities (Guaranteed on Android 11-14)
        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            for (ri in resolveInfos) {
                val label = ri.loadLabel(pm).toString()
                val pkg = ri.activityInfo.packageName
                val isSys = (ri.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                if (pkg.isNotBlank() && label.isNotBlank()) {
                    result[pkg] = AppMetadata(label, pkg, isSys)
                }
            }
        } catch (e: Throwable) {
            Log.e("AppLauncher", "Error querying launcher intent activities", e)
        }

        // Method 2: Query Installed Applications (Catches modded & sideloaded APKs)
        try {
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (appInfo in packages) {
                if (!result.containsKey(appInfo.packageName)) {
                    val label = pm.getApplicationLabel(appInfo).toString()
                    val isSys = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    if (pm.getLaunchIntentForPackage(appInfo.packageName) != null) {
                        result[appInfo.packageName] = AppMetadata(label, appInfo.packageName, isSys)
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e("AppLauncher", "Error querying installed applications", e)
        }

        return result.values.toList()
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

        // 3. Common aliases (WhatsApp, YouTube, Instagram, etc.)
        if (match == null) {
            val aliasMap = mapOf(
                "ig" to "instagram",
                "insta" to "instagram",
                "yt" to "youtube",
                "wa" to "whatsapp",
                "fb" to "facebook",
                "play store" to "vending",
                "messages" to "messaging",
                "revanced" to "youtube"
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
        return try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(packageName) ?: return false
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            context.startActivity(intent)
            true
        } catch (e: Throwable) {
            Log.e("AppLauncher", "Error launching package: $packageName", e)
            false
        }
    }
}
