package com.editog.novaagent.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Chat : BottomNavItem("chat", "Chat", Icons.Default.Email)
    object ApiSetup : BottomNavItem("api_setup", "API Setup", Icons.Default.Lock)
    object Voicemail : BottomNavItem("voicemail", "Voicemail", Icons.Default.Call)
    object Settings : BottomNavItem("settings", "Settings", Icons.Default.Settings)

    companion object {
        val items = listOf(Chat, ApiSetup, Voicemail, Settings)
    }
}
