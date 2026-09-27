package com.editog.novaagent

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.editog.novaagent.service.DynamicIslandService
import com.editog.novaagent.ui.navigation.AppNavHost
import com.editog.novaagent.ui.theme.NovaAgentTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 15 edge-to-edge support: must be invoked before super.onCreate
        try {
            enableEdgeToEdge()
        } catch (e: Throwable) {
            Log.e("MainActivity", "EdgeToEdge init error", e)
        }

        super.onCreate(savedInstanceState)

        // Safe background overlay check
        window.decorView.post {
            try {
                val validApp = (application as? NovaApplication) ?: NovaApplication.instance
                val isIslandEnabled = validApp?.settingsRepository?.settings?.value?.dynamicIslandEnabled == true
                if (isIslandEnabled && Settings.canDrawOverlays(this)) {
                    val intent = Intent(this, DynamicIslandService::class.java)
                    startService(intent)
                }
            } catch (e: Throwable) {
                Log.e("MainActivity", "Dynamic island init error", e)
            }
        }

        try {
            setContent {
                val validApp = (application as? NovaApplication) ?: NovaApplication.instance

                if (validApp != null) {
                    val settings by validApp.settingsRepository.settings.collectAsState()

                    NovaAgentTheme(themeColor = settings.themeColor) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = Color(0xFF090A0F)
                        ) {
                            AppNavHost(app = validApp, settings = settings)
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFF090A0F)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF00F0FF))
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e("MainActivity", "Fatal setContent error", e)
        }
    }
}
