package com.editog.novaagent.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.editog.novaagent.NovaApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AgentAccessibilityService : AccessibilityService() {

    companion object {
        var instance: AgentAccessibilityService? = null
            private set

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        private val _currentAppPackage = MutableStateFlow("")
        val currentAppPackage: StateFlow<String> = _currentAppPackage.asStateFlow()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceActive.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString()
        if (!pkg.isNullOrBlank()) {
            _currentAppPackage.value = pkg
        }
    }

    override fun onInterrupt() {
        _isServiceActive.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _isServiceActive.value = false
    }

    /**
     * Intercepts Volume Up hardware key to toggle voice listening mode
     */
    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event != null && event.keyCode == KeyEvent.KEYCODE_VOLUME_UP && event.action == KeyEvent.ACTION_DOWN) {
            // Trigger voice listening toggle via Application context
            val app = application as? NovaApplication
            app?.voiceManager?.toggleListening()
            DynamicIslandService.postAction("Listening toggle (Vol+ key)")
            return true // Consume key event
        }
        return super.onKeyEvent(event)
    }

    /**
     * Watchdog: Summarizes screen content for Gemini
     */
    fun inspectCurrentScreen(): String {
        val rootNode = rootInActiveWindow ?: return "Screen content unavailable"
        val stringBuilder = StringBuilder()
        stringBuilder.append("Current App Package: ").append(rootNode.packageName ?: "Unknown").append("\n")
        stringBuilder.append("Visible Elements:\n")
        traverseNode(rootNode, stringBuilder, 0)
        return stringBuilder.toString()
    }

    private fun traverseNode(node: AccessibilityNodeInfo, builder: StringBuilder, depth: Int) {
        if (depth > 8) return
        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        val viewId = node.viewIdResourceName

        if (!text.isNullOrBlank() || !desc.isNullOrBlank()) {
            builder.append("  ".repeat(depth))
            if (!text.isNullOrBlank()) builder.append("Text: \"$text\" ")
            if (!desc.isNullOrBlank()) builder.append("Desc: \"$desc\" ")
            if (node.isClickable) builder.append("[Clickable] ")
            if (node.isEditable) builder.append("[Editable] ")
            if (!viewId.isNullOrBlank()) builder.append("Id: $viewId")
            builder.append("\n")
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            traverseNode(child, builder, depth + 1)
            child.recycle()
        }
    }

    /**
     * Performs click on an element matching text or ID
     */
    fun clickElement(targetTextOrId: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(targetTextOrId)
        for (node in nodes) {
            if (node.isClickable) {
                val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (clicked) return true
            }
            // Try parent if clickable
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable) {
                    val clicked = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (clicked) return true
                }
                parent = parent.parent
            }
        }
        return false
    }

    /**
     * Types text into target input field (search box, chat box)
     */
    fun typeTextIntoInput(targetHint: String, textToType: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused != null && focused.isEditable) {
            val args = Bundle()
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToType)
            return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }

        // Search for editable nodes
        val editableNodes = mutableListOf<AccessibilityNodeInfo>()
        findEditableNodes(root, editableNodes)

        val targetNode = editableNodes.firstOrNull {
            it.text?.toString()?.contains(targetHint, ignoreCase = true) == true ||
            it.contentDescription?.toString()?.contains(targetHint, ignoreCase = true) == true ||
            it.viewIdResourceName?.contains(targetHint, ignoreCase = true) == true
        } ?: editableNodes.firstOrNull()

        if (targetNode != null) {
            targetNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            val args = Bundle()
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToType)
            return targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }
        return false
    }

    private fun findEditableNodes(node: AccessibilityNodeInfo, list: MutableList<AccessibilityNodeInfo>) {
        if (node.isEditable) list.add(node)
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findEditableNodes(child, list)
        }
    }

    /**
     * Dispatches scroll / swipe gestures for Reels (Instagram), Shorts (YouTube), Facebook
     */
    fun scrollMedia(direction: String = "up"): Boolean {
        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels.toFloat()
        val height = displayMetrics.heightPixels.toFloat()

        val startX = width / 2f
        val endX = width / 2f
        val startY: Float
        val endY: Float

        if (direction.lowercase() == "up") {
            // Swipe up to see NEXT reel/short
            startY = height * 0.75f
            endY = height * 0.25f
        } else {
            // Swipe down to see PREVIOUS reel/short
            startY = height * 0.25f
            endY = height * 0.75f
        }

        val swipePath = Path()
        swipePath.moveTo(startX, startY)
        swipePath.lineTo(endX, endY)

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(swipePath, 0, 300))
            .build()

        return dispatchGesture(gesture, null, null)
    }

    /**
     * Taps the center of screen to play/pause video reels
     */
    fun togglePlayPause(): Boolean {
        val displayMetrics = resources.displayMetrics
        val x = displayMetrics.widthPixels / 2f
        val y = displayMetrics.heightPixels / 2f

        val tapPath = Path()
        tapPath.moveTo(x, y)

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(tapPath, 0, 80))
            .build()

        return dispatchGesture(gesture, null, null)
    }
}
