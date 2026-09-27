package com.editog.novaagent

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.editog.novaagent.service.DynamicIslandService
import com.editog.novaagent.ui.navigation.AppNavHost
import com.editog.novaagent.ui.theme.NovaAgentTheme

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle runtime permissions results
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as NovaApplication
        requestNecessaryPermissions()
        startDynamicIslandIfPermitted()

        setContent {
            val settings by app.settingsRepository.settings.collectAsState()

            NovaAgentTheme(themeColor = settings.themeColor) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF090A0F)
                ) {
                    AppNavHost(app = app, settings = settings)
                }
            }
        }
    }

    private fun requestNecessaryPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val ungranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (ungranted.isNotEmpty()) {
            permissionLauncher.launch(ungranted.toTypedArray())
        }
    }

    private fun startDynamicIslandIfPermitted() {
        if (Settings.canDrawOverlays(this)) {
            val intent = Intent(this, DynamicIslandService::class.java)
            startService(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        startDynamicIslandIfPermitted()
    }
}
