package com.editog.novaagent.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.editog.novaagent.NovaApplication
import com.editog.novaagent.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class DynamicIslandService : Service() {

    companion object {
        var instance: DynamicIslandService? = null
            private set

        fun postAction(actionDescription: String) {
            try {
                instance?.showAction(actionDescription)
            } catch (e: Exception) {
                Log.e("DynamicIslandService", "postAction error", e)
            }
        }
    }

    private var windowManager: WindowManager? = null
    private var islandView: View? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            initOverlay()
            observeSettings()
        } catch (e: Exception) {
            Log.e("DynamicIslandService", "Error in onCreate", e)
        }
    }

    private fun initOverlay() {
        if (!Settings.canDrawOverlays(this)) return

        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        val settings = (application as? NovaApplication)?.settingsRepository?.settings?.value

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // Density conversion to pixels
        val density = resources.displayMetrics.density
        val widthPx = ((settings?.dynamicIslandWidth ?: 220) * density).toInt()
        val heightPx = ((settings?.dynamicIslandHeight ?: 48) * density).toInt()
        val cornerRadiusPx = (settings?.dynamicIslandCornerRadius ?: 24) * density

        val params = WindowManager.LayoutParams(
            widthPx,
            heightPx,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = settings?.dynamicIslandX ?: 0
            y = ((settings?.dynamicIslandY ?: 40) * density).toInt()
        }

        val pill = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((16 * density).toInt(), (8 * density).toInt(), (16 * density).toInt(), (8 * density).toInt())
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#E6000000"))
                cornerRadius = cornerRadiusPx
                setStroke((2 * density).toInt(), Color.parseColor("#4000F0FF"))
            }
            background = bg
        }

        val icon = ImageView(this).apply {
            try {
                val drawable = ContextCompat.getDrawable(this@DynamicIslandService, R.drawable.ic_island_sparkle)
                setImageDrawable(drawable)
            } catch (e: Exception) {
                val fallback = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#FF00F0FF"))
                }
                setImageDrawable(fallback)
            }
            layoutParams = android.widget.LinearLayout.LayoutParams((24 * density).toInt(), (24 * density).toInt()).apply {
                marginEnd = (10 * density).toInt()
            }
        }

        val text = TextView(this).apply {
            id = View.generateViewId()
            this.text = "Nova Agent Active"
            setTextColor(Color.WHITE)
            textSize = 13f
            isSingleLine = true
        }

        pill.addView(icon)
        pill.addView(text)
        islandView = pill

        try {
            windowManager?.addView(islandView, params)
        } catch (e: Exception) {
            Log.e("DynamicIslandService", "Could not add view to WindowManager", e)
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
        val density = resources.displayMetrics.density

        lp.x = xOffset
        lp.y = (yOffset * density).toInt()
        lp.width = (width * density).toInt()
        lp.height = (height * density).toInt()

        (view.background as? GradientDrawable)?.cornerRadius = cornerRadius * density

        try {
            wm.updateViewLayout(view, lp)
        } catch (e: Exception) {
            Log.e("DynamicIslandService", "Error updating view layout", e)
        }
    }

    fun showAction(action: String) {
        val view = islandView as? android.widget.LinearLayout ?: return
        for (i in 0 until view.childCount) {
            val child = view.getChildAt(i)
            if (child is TextView) {
                child.text = action
                val lp = view.layoutParams as? WindowManager.LayoutParams
                lp?.width = WindowManager.LayoutParams.WRAP_CONTENT
                try {
                    windowManager?.updateViewLayout(view, lp)
                } catch (e: Exception) {}

                serviceScope.launch {
                    delay(3500)
                    child.text = "Nova AI Ready"
                    val settings = (application as? NovaApplication)?.settingsRepository?.settings?.value
                    val density = resources.displayMetrics.density
                    lp?.width = ((settings?.dynamicIslandWidth ?: 220) * density).toInt()
                    try {
                        windowManager?.updateViewLayout(view, lp)
                    } catch (e: Exception) {}
                }
                break
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        try {
            if (islandView != null) {
                windowManager?.removeView(islandView)
                islandView = null
            }
        } catch (e: Exception) {}
        instance = null
    }
}
