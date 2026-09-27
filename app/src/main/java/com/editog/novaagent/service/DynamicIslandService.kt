package com.editog.novaagent.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import com.editog.novaagent.NovaApplication
import com.editog.novaagent.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class DynamicIslandService : Service() {

    companion object {
        var instance: DynamicIslandService? = null
            private set

        fun postAction(actionDescription: String) {
            instance?.showAction(actionDescription)
        }
    }

    private var windowManager: WindowManager? = null
    private var islandView: View? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        initOverlay()
        observeSettings()
    }

    private fun initOverlay() {
        if (!android.provider.Settings.canDrawOverlays(this)) return

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val inflater = LayoutInflater.from(this)

        val settings = (application as? NovaApplication)?.settingsRepository?.settings?.value

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            settings?.dynamicIslandWidth ?: 220,
            settings?.dynamicIslandHeight ?: 48,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = settings?.dynamicIslandX ?: 0
            y = settings?.dynamicIslandY ?: 40
        }

        // Programmatic dynamic island pill view
        val pill = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 8, 16, 8)
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#E6000000"))
                cornerRadius = (settings?.dynamicIslandCornerRadius ?: 24).toFloat()
                setStroke(2, Color.parseColor("#4000F0FF"))
            }
            background = bg
        }

        val icon = ImageView(this).apply {
            setImageResource(R.drawable.ic_island_sparkle)
            layoutParams = android.widget.LinearLayout.LayoutParams(36, 36).apply {
                marginEnd = 12
            }
        }

        val text = TextView(this).apply {
            id = View.generateViewId()
            this.text = "Nova Agent Active"
            setTextColor(Color.WHITE)
            textSize = 12f
            isSingleLine = true
        }

        pill.addView(icon)
        pill.addView(text)
        islandView = pill

        try {
            windowManager?.addView(islandView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun observeSettings() {
        val repo = (application as? NovaApplication)?.settingsRepository ?: return
        serviceScope.launch {
            repo.settings.collectLatest { s ->
                updateIslandParams(s.dynamicIslandX, s.dynamicIslandY, s.dynamicIslandWidth, s.dynamicIslandHeight, s.dynamicIslandCornerRadius)
            }
        }
    }

    private fun updateIslandParams(xOffset: Int, yOffset: Int, width: Int, height: Int, cornerRadius: Int) {
        val view = islandView ?: return
        val wm = windowManager ?: return
        val lp = view.layoutParams as? WindowManager.LayoutParams ?: return

        lp.x = xOffset
        lp.y = yOffset
        lp.width = width
        lp.height = height

        (view.background as? GradientDrawable)?.cornerRadius = cornerRadius.toFloat()

        try {
            wm.updateViewLayout(view, lp)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showAction(action: String) {
        val view = islandView as? android.widget.LinearLayout ?: return
        for (i in 0 until view.childCount) {
            val child = view.getChildAt(i)
            if (child is TextView) {
                child.text = action
                // Expand width temporarily for clarity
                val lp = view.layoutParams as? WindowManager.LayoutParams
                lp?.width = WindowManager.LayoutParams.WRAP_CONTENT
                windowManager?.updateViewLayout(view, lp)

                serviceScope.launch {
                    delay(3000)
                    child.text = "Nova AI Ready"
                    val settings = (application as? NovaApplication)?.settingsRepository?.settings?.value
                    lp?.width = settings?.dynamicIslandWidth ?: 220
                    windowManager?.updateViewLayout(view, lp)
                }
                break
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        if (islandView != null) {
            windowManager?.removeView(islandView)
            islandView = null
        }
        instance = null
    }
}
