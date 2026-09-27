package com.editog.novaagent.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.editog.novaagent.NovaApplication
import com.editog.novaagent.R
import com.editog.novaagent.data.model.AppSettings
import com.editog.novaagent.ui.screens.ApiSetupScreen
import com.editog.novaagent.ui.screens.ChatScreen
import com.editog.novaagent.ui.screens.SettingsScreen
import com.editog.novaagent.ui.screens.VoicemailScreen

@Composable
fun AppNavHost(
    app: NovaApplication,
    settings: AppSettings
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "chat"
    val primaryColor = Color(settings.themeColor.primaryHex)

    fun navigateTo(route: String) {
        if (currentRoute != route) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xF20B0E17),
                contentColor = Color.White
            ) {
                // Tab 1: Chat
                val isChat = currentRoute == "chat"
                NavigationBarItem(
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_nav_chat),
                            contentDescription = "Chat",
                            tint = if (isChat) primaryColor else Color(0xFF6B7280),
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Chat",
                            color = if (isChat) primaryColor else Color(0xFF6B7280)
                        )
                    },
                    selected = isChat,
                    onClick = { navigateTo("chat") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = primaryColor.copy(alpha = 0.15f)
                    )
                )

                // Tab 2: API Setup
                val isApi = currentRoute == "api_setup"
                NavigationBarItem(
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_nav_key),
                            contentDescription = "API Setup",
                            tint = if (isApi) primaryColor else Color(0xFF6B7280),
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "API Setup",
                            color = if (isApi) primaryColor else Color(0xFF6B7280)
                        )
                    },
                    selected = isApi,
                    onClick = { navigateTo("api_setup") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = primaryColor.copy(alpha = 0.15f)
                    )
                )

                // Tab 3: Voicemail
                val isVoicemail = currentRoute == "voicemail"
                NavigationBarItem(
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_voicemail),
                            contentDescription = "Voicemail",
                            tint = if (isVoicemail) primaryColor else Color(0xFF6B7280),
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Voicemail",
                            color = if (isVoicemail) primaryColor else Color(0xFF6B7280)
                        )
                    },
                    selected = isVoicemail,
                    onClick = { navigateTo("voicemail") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = primaryColor.copy(alpha = 0.15f)
                    )
                )

                // Tab 4: Settings
                val isSettings = currentRoute == "settings"
                NavigationBarItem(
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_nav_settings),
                            contentDescription = "Settings",
                            tint = if (isSettings) primaryColor else Color(0xFF6B7280),
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Settings",
                            color = if (isSettings) primaryColor else Color(0xFF6B7280)
                        )
                    },
                    selected = isSettings,
                    onClick = { navigateTo("settings") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = primaryColor.copy(alpha = 0.15f)
                    )
                )
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "chat",
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            composable("chat") {
                ChatScreen(
                    app = app,
                    settings = settings,
                    bottomBarHeight = innerPadding.calculateBottomPadding()
                )
            }
            composable("api_setup") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding())
                ) {
                    ApiSetupScreen(app = app, settings = settings)
                }
            }
            composable("voicemail") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding())
                ) {
                    VoicemailScreen(app = app, settings = settings)
                }
            }
            composable("settings") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding())
                ) {
                    SettingsScreen(app = app, currentSettings = settings)
                }
            }
        }
    }
}
