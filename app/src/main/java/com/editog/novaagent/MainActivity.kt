package com.editog.novaagent

import android.os.Bundle
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                var initError by remember { mutableStateOf<String?>(null) }

                if (initError != null) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFF090A0F)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Nova Agent Recovery", color = Color(0xFF00F0FF), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(initError ?: "An initialization error occurred.", color = Color(0xFFE2E8F0), fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    initError = null
                                    recreate()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black)
                            ) {
                                Text("Retry Launch", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
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
            }
        } catch (e: Throwable) {
            Log.e("MainActivity", "Fatal setContent error", e)
            try {
                setContent {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFF090A0F)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Nova Agent Recovery", color = Color(0xFF00F0FF), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(e.localizedMessage ?: "Unknown initialization error", color = Color.White, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { recreate() }) {
                                Text("Restart Nova Agent")
                            }
                        }
                    }
                }
            } catch (e2: Throwable) {
                Log.e("MainActivity", "Fallback setContent failed", e2)
            }
        }
    }
}
