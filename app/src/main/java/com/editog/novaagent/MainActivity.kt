package com.editog.novaagent

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
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
import com.editog.novaagent.ui.navigation.AppNavHost
import com.editog.novaagent.ui.theme.NovaAgentTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            enableEdgeToEdge()
        } catch (e: Throwable) {
            Log.e("MainActivity", "EdgeToEdge error", e)
        }

        super.onCreate(savedInstanceState)

        // Samsung / Android 15 Display Cutout Safe Mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                window.attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } catch (e: Throwable) {
                Log.e("MainActivity", "Cutout mode error", e)
            }
        }

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
    }
}
