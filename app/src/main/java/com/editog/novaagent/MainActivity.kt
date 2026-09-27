package com.editog.novaagent

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.editog.novaagent.service.DynamicIslandService
import com.editog.novaagent.ui.CrashReportActivity
import com.editog.novaagent.ui.navigation.AppNavHost
import com.editog.novaagent.ui.theme.NovaAgentTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            enableEdgeToEdge()
        } catch (e: Throwable) {
            Log.e("MainActivity", "EdgeToEdge init error", e)
        }

        try {
            setContent {
                val app = (application as? NovaApplication) ?: NovaApplication.instance

                if (app != null) {
                    val settings by app.settingsRepository.settings.collectAsState()

                    NovaAgentTheme(themeColor = settings.themeColor) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = Color(0xFF090A0F)
                        ) {
                            AppNavHost(app = app, settings = settings)
                        }
                    }
                } else {
                    NovaAgentTheme {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = Color(0xFF090A0F)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(color = Color(0xFF00F0FF))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Starting Nova Agent...", color = Color.White, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e("MainActivity", "Fatal setContent error", e)
            try {
                val sw = java.io.StringWriter()
                e.printStackTrace(java.io.PrintWriter(sw))
                val stackTrace = sw.toString()
                val intent = Intent(this, CrashReportActivity::class.java).apply {
                    putExtra(CrashReportActivity.EXTRA_ERROR_MESSAGE, e.localizedMessage ?: e.javaClass.simpleName)
                    putExtra(CrashReportActivity.EXTRA_STACK_TRACE, stackTrace)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
                finish()
            } catch (e2: Throwable) {
                Log.e("MainActivity", "Fallback to CrashReportActivity failed", e2)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndStartDynamicIsland()
    }

    private fun checkAndStartDynamicIsland() {
        try {
            val app = (application as? NovaApplication) ?: NovaApplication.instance ?: return
            val settings = app.settingsRepository.settings.value
            if (settings.dynamicIslandEnabled && Settings.canDrawOverlays(this)) {
                if (DynamicIslandService.instance == null) {
                    val serviceIntent = Intent(this, DynamicIslandService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e("MainActivity", "Error starting DynamicIslandService onResume", e)
        }
    }
}
