package com.editog.novaagent.ui.navigation

import androidx.annotation.DrawableRes
import com.editog.novaagent.R

sealed class BottomNavItem(
    val route: String,
    val title: String,
    @DrawableRes val iconResId: Int
) {
    object Chat : BottomNavItem("chat", "Chat", R.drawable.ic_nav_chat)
    object ApiSetup : BottomNavItem("api_setup", "API Setup", R.drawable.ic_nav_key)
    object Voicemail : BottomNavItem("voicemail", "Voicemail", R.drawable.ic_voicemail)
    object Settings : BottomNavItem("settings", "Settings", R.drawable.ic_nav_settings)

    companion object {
        val items = listOf(Chat, ApiSetup, Voicemail, Settings)
    }
}
