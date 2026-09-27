package com.editog.novaagent.ui.navigation

import androidx.annotation.DrawableRes
import com.editog.novaagent.R

data class BottomNavItem(
    val route: String,
    val title: String,
    @DrawableRes val iconResId: Int
) {
    companion object {
        val Chat = BottomNavItem("chat", "Chat", R.drawable.ic_nav_chat)
        val ApiSetup = BottomNavItem("api_setup", "API Setup", R.drawable.ic_nav_key)
        val Voicemail = BottomNavItem("voicemail", "Voicemail", R.drawable.ic_voicemail)
        val Settings = BottomNavItem("settings", "Settings", R.drawable.ic_nav_settings)

        val items: List<BottomNavItem> = listOf(
            BottomNavItem("chat", "Chat", R.drawable.ic_nav_chat),
            BottomNavItem("api_setup", "API Setup", R.drawable.ic_nav_key),
            BottomNavItem("voicemail", "Voicemail", R.drawable.ic_voicemail),
            BottomNavItem("settings", "Settings", R.drawable.ic_nav_settings)
        )
    }
}
