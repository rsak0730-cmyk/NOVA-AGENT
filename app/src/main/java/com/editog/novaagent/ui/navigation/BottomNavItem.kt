package com.editog.novaagent.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Chat : BottomNavItem("chat", "Chat", Icons.Default.Chat)
    object ApiSetup : BottomNavItem("api_setup", "API Setup", Icons.Default.Key)
    object Voicemail : BottomNavItem("voicemail", "Voicemail", Icons.Default.Voicemail)
    object Settings : BottomNavItem("settings", "Settings", Icons.Default.Settings)

    companion object {
        val items = listOf(Chat, ApiSetup, Voicemail, Settings)
    }
}
