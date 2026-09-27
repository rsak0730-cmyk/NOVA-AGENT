package com.editog.novaagent.service

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.editog.novaagent.MainActivity
import com.editog.novaagent.NovaApplication
import com.editog.novaagent.R
import com.editog.novaagent.data.model.AppSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs

class DynamicIslandService : Service() {

    companion object {
        var instance: DynamicIslandService? = null
            private set

        private val mainHandler = Handler(Looper.getMainLooper())
        private const val CHANNEL_ID = "nova_island_service_channel"
        private const val NOTIFICATION_ID = 2001

        fun postAction(actionDescription: String) {
            mainHandler.post {
                try {
                    instance?.showAction(actionDescription)
                } catch (e: Throwable) {
                    Log.e("DynamicIslandService", "postAction error", e)
                }
            }
        }
    }

    private var windowManager: WindowManager? = null
    private var islandContainer: LinearLayout? = null
    private var islandIcon: ImageView? = null
    private var islandText: TextView? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var revertActionJob: Job? = null
    private var currentAnimDuration: Long = 350L
    private var baseWidthPx: Int = 220
    private var baseHeightPx: Int = 48
    private var baseCornerRadiusPx: Float = 24f
    private var activeBorderColor: Int = Color.parseColor("#4000F0FF")

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            startInForeground()
            initOverlay()
            observeSettings()
            observeVoiceState()
        } catch (e: Throwable) {
            Log.e("DynamicIslandService", "Error in onCreate", e)
        }
    }

    private fun startInForeground() {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Nova Dynamic Island",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Displays live iOS-style dynamic island on top of all apps"
                    setShowBadge(false)
                }
                nm.createNotificationChannel(channel)
            }

            val appIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Nova Agent Dynamic Island")
                .setContentText("Tap island pill on top of screen anytime to talk with Jarvis")
                .setSmallIcon(R.drawable.ic_island_sparkle)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= 34) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Throwable) {
            Log.e("DynamicIslandService", "Foreground service start failed", e)
        }
    }

    private fun initOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            Log.w("DynamicIslandService", "Overlay permission not granted")
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        val app = (application as? NovaApplication) ?: NovaApplication.instance
        val settings = app?.settingsRepository?.settings?.value ?: AppSettings()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val density = resources.displayMetrics.density
        baseWidthPx = (settings.dynamicIslandWidth * density).toInt()
        baseHeightPx = (settings.dynamicIslandHeight * density).toInt()
        baseCornerRadiusPx = settings.dynamicIslandCornerRadius * density
        currentAnimDuration = settings.dynamicIslandAnimationDurationMs.toLong()
        activeBorderColor = Color.parseColor(String.format("#%06X", 0xFFFFFF and settings.themeColor.primaryHex.toInt()))

        val params = WindowManager.LayoutParams(
            baseWidthPx,
            baseHeightPx,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = settings.dynamicIslandX
            y = (settings.dynamicIslandY * density).toInt()
        }

        // Pill layout
        val pill = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding((14 * density).toInt(), (4 * density).toInt(), (14 * density).toInt(), (4 * density).toInt())
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#F2000000"))
                cornerRadius = baseCornerRadiusPx
                setStroke((2 * density).toInt(), activeBorderColor)
            }
            background = bg

            // Single tap: Toggle voice listening mode
            // Long tap: Launch Nova Agent App
            setOnTouchListener(object : View.OnTouchListener {
                private var startX = 0f
                private var startY = 0f
                private var initialX = 0
                private var initialY = 0
                private var isDragging = false
                private var startTime = 0L

                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            startX = event.rawX
                            startY = event.rawY
                            initialX = params.x
                            initialY = params.y
                            startTime = System.currentTimeMillis()
                            isDragging = false
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = (event.rawX - startX).toInt()
                            val dy = (event.rawY - startY).toInt()
                            if (abs(dx) > 10 || abs(dy) > 10) {
                                isDragging = true
                                params.x = initialX + dx
                                params.y = initialY + dy
                                try {
                                    windowManager?.updateViewLayout(pill, params)
                                } catch (e: Throwable) {}
                            }
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (!isDragging) {
                                // Click / Tap: Turn on or toggle voice listening mode directly!
                                // Never open the agent app - stay in current app and stream speech to chat
                                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                toggleVoiceListening()
                            } else {
                                // Persist user drag location to settings
                                app?.settingsRepository?.let { repo ->
                                    val current = repo.settings.value
                                    val newYDp = (params.y / density).toInt()
                                    repo.updateSettings(current.copy(dynamicIslandX = params.x, dynamicIslandY = newYDp))
                                }
                            }
                            return true
                        }
                    }
                    return false
                }
            })
        }

        val icon = ImageView(this).apply {
            try {
                setImageDrawable(ContextCompat.getDrawable(this@DynamicIslandService, R.drawable.ic_island_sparkle))
            } catch (e: Throwable) {
                val fallback = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(activeBorderColor)
                }
                setImageDrawable(fallback)
            }
            layoutParams = LinearLayout.LayoutParams((22 * density).toInt(), (22 * density).toInt()).apply {
                marginEnd = (8 * density).toInt()
            }
        }

        val text = TextView(this).apply {
            id = View.generateViewId()
            this.text = "Nova Agent"
            setTextColor(Color.WHITE)
            textSize = 12.5f
            isSingleLine = true
            gravity = Gravity.CENTER_VERTICAL
        }

        pill.addView(icon)
        pill.addView(text)

        islandContainer = pill
        islandIcon = icon
        islandText = text

        try {
            windowManager?.addView(islandContainer, params)
            Log.d("DynamicIslandService", "Overlay pill added successfully to WindowManager")
        } catch (e: Throwable) {
            Log.e("DynamicIslandService", "Could not add view to WindowManager", e)
        }
    }

    private fun toggleVoiceListening() {
        val app = (application as? NovaApplication) ?: NovaApplication.instance ?: return
        app.voiceManager.toggleListening()
    }



    private fun observeSettings() {
        val app = (application as? NovaApplication) ?: NovaApplication.instance ?: return
        serviceScope.launch {
            app.settingsRepository.settings.collectLatest { s ->
                val density = resources.displayMetrics.density
                baseWidthPx = (s.dynamicIslandWidth * density).toInt()
                baseHeightPx = (s.dynamicIslandHeight * density).toInt()
                baseCornerRadiusPx = s.dynamicIslandCornerRadius * density
                currentAnimDuration = s.dynamicIslandAnimationDurationMs.toLong()
                activeBorderColor = Color.parseColor(String.format("#%06X", 0xFFFFFF and s.themeColor.primaryHex.toInt()))

                updateIslandParams(s.dynamicIslandX, s.dynamicIslandY, s.dynamicIslandWidth, s.dynamicIslandHeight, s.dynamicIslandCornerRadius)
            }
        }
    }

    private fun observeVoiceState() {
        val app = (application as? NovaApplication) ?: NovaApplication.instance ?: return

        // 1. Observe Voice Listening state
        serviceScope.launch {
            app.voiceManager.isListening.collectLatest { listening ->
                if (listening) {
                    revertActionJob?.cancel()
                    showListeningState()
                } else {
                    serviceScope.launch {
                        delay(1200)
                        if (!app.voiceManager.isListening.value && revertActionJob?.isActive != true) {
                            showIdleState()
                        }
                    }
                }
            }
        }

        // 2. Observe Live Recognized Text
        serviceScope.launch {
            app.voiceManager.recognizedText.collectLatest { partialText ->
                if (app.voiceManager.isListening.value && partialText.isNotBlank()) {
                    islandText?.text = "🎙️ $partialText"
                    expandPillTemporarily(WindowManager.LayoutParams.WRAP_CONTENT)
                }
            }
        }
    }

    private fun showListeningState() {
        val density = resources.displayMetrics.density
        islandIcon?.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_mic))
        islandText?.text = "Listening... Speak to Jarvis"
        (islandContainer?.background as? GradientDrawable)?.apply {
            setStroke((2 * density).toInt(), Color.parseColor("#FF0055"))
        }
        expandPillTemporarily(WindowManager.LayoutParams.WRAP_CONTENT)
    }

    private fun showIdleState() {
        val density = resources.displayMetrics.density
        islandIcon?.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_island_sparkle))
        islandText?.text = "Nova Agent"
        (islandContainer?.background as? GradientDrawable)?.apply {
            setStroke((2 * density).toInt(), activeBorderColor)
        }
        animatePillSize(baseWidthPx, baseHeightPx)
    }

    private fun updateIslandParams(xOffset: Int, yOffset: Int, width: Int, height: Int, cornerRadius: Int) {
        val view = islandContainer ?: return
        val wm = windowManager ?: return
        val lp = view.layoutParams as? WindowManager.LayoutParams ?: return
        val density = resources.displayMetrics.density

        lp.x = xOffset
        lp.y = (yOffset * density).toInt()
        lp.width = (width * density).toInt()
        lp.height = (height * density).toInt()

        (view.background as? GradientDrawable)?.apply {
            this.cornerRadius = cornerRadius * density
            setStroke((2 * density).toInt(), activeBorderColor)
        }

        try {
            wm.updateViewLayout(view, lp)
        } catch (e: Throwable) {
            Log.e("DynamicIslandService", "Error updating view layout", e)
        }
    }

    fun showAction(action: String) {
        val view = islandContainer ?: return
        val density = resources.displayMetrics.density

        islandText?.text = action
        (view.background as? GradientDrawable)?.apply {
            setStroke((2 * density).toInt(), activeBorderColor)
        }

        // Set action icon depending on content
        val iconRes = when {
            action.contains("Call", ignoreCase = true) || action.contains("Dial", ignoreCase = true) -> R.drawable.ic_island_sparkle
            action.contains("Heard", ignoreCase = true) || action.contains("Speak", ignoreCase = true) -> R.drawable.ic_mic
            action.contains("Voicemail", ignoreCase = true) -> R.drawable.ic_voicemail
            action.contains("Play", ignoreCase = true) -> R.drawable.ic_play
            action.contains("Stop", ignoreCase = true) -> R.drawable.ic_stop
            else -> R.drawable.ic_island_sparkle
        }
        islandIcon?.setImageDrawable(ContextCompat.getDrawable(this, iconRes))

        expandPillTemporarily(WindowManager.LayoutParams.WRAP_CONTENT)

        revertActionJob?.cancel()
        revertActionJob = serviceScope.launch {
            delay(3800)
            val app = (application as? NovaApplication) ?: NovaApplication.instance
            if (app?.voiceManager?.isListening?.value == true) {
                showListeningState()
            } else {
                showIdleState()
            }
        }
    }

    private fun expandPillTemporarily(targetWidth: Int) {
        val view = islandContainer ?: return
        val wm = windowManager ?: return
        val lp = view.layoutParams as? WindowManager.LayoutParams ?: return

        lp.width = targetWidth
        try {
            wm.updateViewLayout(view, lp)
        } catch (e: Throwable) {}
    }

    private fun animatePillSize(targetWidthPx: Int, targetHeightPx: Int) {
        val view = islandContainer ?: return
        val wm = windowManager ?: return
        val lp = view.layoutParams as? WindowManager.LayoutParams ?: return

        val initialWidth = if (lp.width > 0) lp.width else baseWidthPx
        val anim = ValueAnimator.ofInt(initialWidth, targetWidthPx).apply {
            duration = currentAnimDuration
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                lp.width = animator.animatedValue as Int
                lp.height = targetHeightPx
                try {
                    wm.updateViewLayout(view, lp)
                } catch (e: Throwable) {}
            }
        }
        anim.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        try {
            if (islandContainer != null) {
                windowManager?.removeView(islandContainer)
                islandContainer = null
            }
        } catch (e: Throwable) {}
        instance = null
    }
}
