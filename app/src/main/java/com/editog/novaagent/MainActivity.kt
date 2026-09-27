package com.editog.novaagent

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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

class MainActivity : AppCompatActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d("MainActivity", "Permissions updated: $permissions")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Modern Android 15 Edge-to-Edge compliance
        try {
            enableEdgeToEdge()
        } catch (e: Throwable) {
            Log.e("MainActivity", "EdgeToEdge error", e)
        }

        super.onCreate(savedInstanceState)

        // Android 15 Display Cutout Safe Mode: avoids crash on modern display cutouts
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                window.attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } catch (e: Throwable) {
                Log.e("MainActivity", "Cutout mode error", e)
            }
        }

        try {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        } catch (e: Throwable) {
            Log.e("MainActivity", "Vector compat error", e)
        }

        val app = (application as? NovaApplication) ?: NovaApplication.instance

        // Post-init checks on the window decor view so onCreate never blocks or hangs
        window.decorView.post {
            try {
                requestNecessaryPermissions()
                startDynamicIslandIfPermitted()
            } catch (e: Throwable) {
                Log.e("MainActivity", "Post-launch check error", e)
            }
        }

        setContent {
            val validApp = app ?: (application as NovaApplication)
            val settings by validApp.settingsRepository.settings.collectAsState()

            NovaAgentTheme(themeColor = settings.themeColor) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF090A0F)
                ) {
                    AppNavHost(app = validApp, settings = settings)
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
        try {
            if (Settings.canDrawOverlays(this)) {
                val intent = Intent(this, DynamicIslandService::class.java)
                startService(intent)
            }
        } catch (e: Throwable) {
            Log.e("MainActivity", "Overlay start error", e)
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            startDynamicIslandIfPermitted()
        } catch (e: Throwable) {
            Log.e("MainActivity", "Error onResume dynamic island start", e)
        }
    }
}
