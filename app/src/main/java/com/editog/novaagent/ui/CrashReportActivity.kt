package com.editog.novaagent.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.editog.novaagent.MainActivity

class CrashReportActivity : Activity() {

    companion object {
        const val EXTRA_ERROR_MESSAGE = "extra_error_message"
        const val EXTRA_STACK_TRACE = "extra_stack_trace"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val errorMessage = intent?.getStringExtra(EXTRA_ERROR_MESSAGE) ?: "Unexpected startup failure"
        val stackTrace = intent?.getStringExtra(EXTRA_STACK_TRACE) ?: "No stack trace available."

        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "Unknown"
        } catch (_: Throwable) {
            "2.0.0"
        }

        val density = resources.displayMetrics.density
        fun dp(value: Int): Int = (value * density).toInt()

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(0xFF090A0F.toInt())
            isFillViewport = true
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(32), dp(20), dp(32))
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // Header Title
        val titleText = TextView(this).apply {
            text = "Nova Agent Recovery"
            setTextColor(0xFF00F0FF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        rootLayout.addView(titleText)

        // Version Tag
        val versionText = TextView(this).apply {
            text = "Installed Version: v$versionName"
            setTextColor(0xFF6B7280.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            gravity = Gravity.CENTER
            setPadding(0, dp(2), 0, dp(8))
        }
        rootLayout.addView(versionText)

        // Subtitle
        val subtitleText = TextView(this).apply {
            text = "Nova Agent encountered an unhandled issue. Review the diagnostic report below, update to the latest build, or restart in safe mode."
            setTextColor(0xFF9CA3AF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(14))
        }
        rootLayout.addView(subtitleText)

        // Stack Trace Card
        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val cardBg = GradientDrawable().apply {
                setColor(0xFF131622.toInt())
                cornerRadius = dp(12).toFloat()
                setStroke(dp(1), 0x3300F0FF.toInt())
            }
            background = cardBg
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f
            ).apply {
                setMargins(0, dp(4), 0, dp(14))
            }
        }

        val errLabel = TextView(this).apply {
            text = "Error: $errorMessage"
            setTextColor(0xFFFF4466.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(8))
        }
        cardLayout.addView(errLabel)

        val traceScroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(200)
            )
        }

        val traceText = TextView(this).apply {
            text = stackTrace
            setTextColor(0xFFE2E8F0.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
        }
        traceScroll.addView(traceText)
        cardLayout.addView(traceScroll)
        rootLayout.addView(cardLayout)

        // Buttons Container
        val buttonsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Update to Latest APK Button
        val updateBtn = Button(this).apply {
            text = "Download Latest Update APK (v2.0.0)"
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            val btnBg = GradientDrawable().apply {
                setColor(0xFF00FF66.toInt())
                cornerRadius = dp(10).toFloat()
            }
            background = btnBg
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            ).apply {
                setMargins(0, 0, 0, dp(8))
            }
            setOnClickListener {
                try {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/rsak0730-cmyk/NOVA-AGENT/releases/latest"))
                    browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(browserIntent)
                } catch (_: Throwable) {
                    Toast.makeText(this@CrashReportActivity, "Could not open browser", Toast.LENGTH_SHORT).show()
                }
            }
        }
        buttonsContainer.addView(updateBtn)

        // Restart in Safe Mode Button (Clears Corrupted Preferences)
        val safeRestartBtn = Button(this).apply {
            text = "Clear Cache & Restart (Safe Mode)"
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            val btnBg = GradientDrawable().apply {
                setColor(0xFF00F0FF.toInt())
                cornerRadius = dp(10).toFloat()
            }
            background = btnBg
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            ).apply {
                setMargins(0, 0, 0, dp(8))
            }
            setOnClickListener {
                clearAllPreferences()
                launchMainActivity()
            }
        }
        buttonsContainer.addView(safeRestartBtn)

        // Normal Restart Button
        val normalRestartBtn = Button(this).apply {
            text = "Restart Nova Agent"
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            val btnBg = GradientDrawable().apply {
                setColor(0xFF1E2436.toInt())
                cornerRadius = dp(10).toFloat()
                setStroke(dp(1), 0x5500F0FF.toInt())
            }
            background = btnBg
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(46)
            ).apply {
                setMargins(0, 0, 0, dp(8))
            }
            setOnClickListener {
                launchMainActivity()
            }
        }
        buttonsContainer.addView(normalRestartBtn)

        // Copy Error Log Button
        val copyBtn = Button(this).apply {
            text = "Copy Error Details"
            setTextColor(0xFF9CA3AF.toInt())
            val btnBg = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                cornerRadius = dp(10).toFloat()
                setStroke(dp(1), 0x339CA3AF.toInt())
            }
            background = btnBg
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(42)
            )
            setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Nova Agent Error Log", "Version: v$versionName\nError: $errorMessage\n\nStack:\n$stackTrace")
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this@CrashReportActivity, "Error copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        }
        buttonsContainer.addView(copyBtn)

        rootLayout.addView(buttonsContainer)
        scrollView.addView(rootLayout)
        setContentView(scrollView)
    }

    private fun clearAllPreferences() {
        try {
            getSharedPreferences("nova_settings", Context.MODE_PRIVATE).edit().clear().apply()
            getSharedPreferences("nova_chat_history", Context.MODE_PRIVATE).edit().clear().apply()
            getSharedPreferences("nova_api_config", Context.MODE_PRIVATE).edit().clear().apply()
            getSharedPreferences("nova_voicemails", Context.MODE_PRIVATE).edit().clear().apply()
        } catch (_: Throwable) {}
    }

    private fun launchMainActivity() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
        } catch (_: Throwable) {}
        finish()
    }
}
