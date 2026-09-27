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
import android.view.View
import android.view.WindowManager
import kotlinx.coroutines.*

class WatchDogOverlayService : Service() {

    companion object {
        var instance: WatchDogOverlayService? = null
            private set

        fun highlightArea(x: Int, y: Int, width: Int, height: Int) {
            instance?.showHighlight(x, y, width, height)
        }
    }

    private var windowManager: WindowManager? = null
    private var highlightBox: View? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    fun showHighlight(x: Int, y: Int, width: Int, height: Int) {
        if (!android.provider.Settings.canDrawOverlays(this)) return

        removeCurrent()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            width,
            height,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
        }

        val box = View(this).apply {
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#2600F0FF"))
                setStroke(4, Color.parseColor("#FF00F0FF"))
                cornerRadius = 16f
            }
        }
        highlightBox = box

        try {
            windowManager?.addView(highlightBox, params)
            scope.launch {
                delay(4000)
                removeCurrent()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeCurrent() {
        if (highlightBox != null) {
            try {
                windowManager?.removeView(highlightBox)
            } catch (e: Exception) {}
            highlightBox = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeCurrent()
        scope.cancel()
        instance = null
    }
}
