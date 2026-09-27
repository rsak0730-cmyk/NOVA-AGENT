package com.editog.novaagent.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.editog.novaagent.NovaApplication
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

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0C0E16),
                contentColor = Color.White
            ) {
                BottomNavItem.items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    NavigationBarItem(
                        icon = {
                            Icon(
                                painter = painterResource(item.iconResId),
                                contentDescription = item.title,
                                tint = if (isSelected) primaryColor else Color(0xFF6B7280),
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item.title,
                                color = if (isSelected) primaryColor else Color(0xFF6B7280)
                            )
                        },
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = primaryColor.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        },
        containerColor = Color(0xFF090A0F)
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "chat",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable("chat") {
                ChatScreen(app = app, settings = settings)
            }
            composable("api_setup") {
                ApiSetupScreen(app = app, settings = settings)
            }
            composable("voicemail") {
                VoicemailScreen(app = app, settings = settings)
            }
            composable("settings") {
                SettingsScreen(app = app, currentSettings = settings)
            }
        }
    }
}
