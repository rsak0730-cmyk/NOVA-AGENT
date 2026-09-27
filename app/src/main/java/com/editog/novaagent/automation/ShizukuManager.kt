package com.editog.novaagent.automation

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.DisplayMetrics
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

data class ShellResult(
    val success: Boolean,
    val output: String,
    val exitCode: Int
)

class ShizukuManager(private val context: Context) {

    companion object {
        const val REQUEST_CODE_SHIZUKU = 10091
        private const val TAG = "ShizukuManager"
    }

    private val _isAvailable = MutableStateFlow(false)
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val _shizukuVersion = MutableStateFlow(0)
    val shizukuVersion: StateFlow<Int> = _shizukuVersion.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        checkShizukuState()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _isAvailable.value = false
        _hasPermission.value = false
        Log.w(TAG, "Shizuku binder died")
    }

    private val requestPermissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQUEST_CODE_SHIZUKU) {
            val granted = grantResult == PackageManager.PERMISSION_GRANTED
            _hasPermission.value = granted
            Log.i(TAG, "Shizuku permission result: granted=$granted")
        }
    }

    fun init() {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
            checkShizukuState()
        } catch (e: Throwable) {
            Log.e(TAG, "Error initializing ShizukuManager", e)
        }
    }

    fun checkShizukuState() {
        try {
            val ping = Shizuku.pingBinder()
            _isAvailable.value = ping
            if (ping) {
                _shizukuVersion.value = Shizuku.getVersion()
                val perm = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                _hasPermission.value = perm
                Log.i(TAG, "Shizuku online: version=${_shizukuVersion.value}, granted=$perm")
            } else {
                _hasPermission.value = false
            }
        } catch (e: Throwable) {
            _isAvailable.value = false
            _hasPermission.value = false
            Log.w(TAG, "Shizuku check state failed: ${e.message}")
        }
    }

    fun isAvailableAndAuthorized(): Boolean {
        checkShizukuState()
        return _isAvailable.value && _hasPermission.value
    }

    fun requestPermission(activity: Activity) {
        try {
            if (!Shizuku.pingBinder()) {
                Log.w(TAG, "Cannot request permission: Shizuku binder not running")
                return
            }
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(REQUEST_CODE_SHIZUKU)
            } else {
                _hasPermission.value = true
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error requesting Shizuku permission", e)
        }
    }

    fun onPermissionResult(requestCode: Int, grantResult: Int) {
        if (requestCode == REQUEST_CODE_SHIZUKU) {
            val granted = grantResult == PackageManager.PERMISSION_GRANTED
            _hasPermission.value = granted
        }
    }

    /**
     * Executes shell command with ADB/Shell privileges via Shizuku
     */
    fun exec(command: String): ShellResult {
        if (!isAvailableAndAuthorized()) {
            return ShellResult(success = false, output = "Shizuku not authorized or unavailable", exitCode = -1)
        }
        return try {
            val process: Process = try {
                val method = Shizuku::class.java.getMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                )
                method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            } catch (e: NoSuchMethodException) {
                Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            }

            val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
            val stdout = stdoutReader.readText()
            stdoutReader.close()

            val stderrReader = BufferedReader(InputStreamReader(process.errorStream))
            val stderr = stderrReader.readText()
            stderrReader.close()

            val exitCode = process.waitFor()
            val output = if (stdout.isNotBlank()) stdout else stderr
            ShellResult(success = exitCode == 0, output = output.trim(), exitCode = exitCode)
        } catch (e: Throwable) {
            Log.e(TAG, "Error executing shell command: $command", e)
            ShellResult(success = false, output = e.localizedMessage ?: "Execution error", exitCode = -1)
        }
    }

    suspend fun execAsync(command: String): ShellResult = withContext(Dispatchers.IO) {
        exec(command)
    }

    // -------------------------------------------------------------
    // AUTONOMOUS ZERO-TOUCH DEVICE CONTROL PRIMITIVES
    // -------------------------------------------------------------

    fun tap(x: Int, y: Int): Boolean {
        val res = exec("input tap $x $y")
        return res.success
    }

    fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Int = 200): Boolean {
        val res = exec("input swipe $x1 $y1 $x2 $y2 $durationMs")
        return res.success
    }

    fun scrollDown(): Boolean {
        val metrics = context.resources.displayMetrics
        val width = if (metrics.widthPixels > 0) metrics.widthPixels else 1080
        val height = if (metrics.heightPixels > 0) metrics.heightPixels else 2400

        val cx = width / 2
        val startY = (height * 0.75f).toInt()
        val endY = (height * 0.22f).toInt()
        return swipe(cx, startY, cx, endY, 190)
    }

    fun scrollUp(): Boolean {
        val metrics = context.resources.displayMetrics
        val width = if (metrics.widthPixels > 0) metrics.widthPixels else 1080
        val height = if (metrics.heightPixels > 0) metrics.heightPixels else 2400

        val cx = width / 2
        val startY = (height * 0.22f).toInt()
        val endY = (height * 0.75f).toInt()
        return swipe(cx, startY, cx, endY, 190)
    }

    fun typeText(text: String): Boolean {
        if (text.isBlank()) return false
        // Android shell input text requires spaces to be %s and special shell symbols escaped
        val escaped = text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("'", "\\'")
            .replace(" ", "%s")
        val res = exec("input text \"$escaped\"")
        return res.success
    }

    fun pressKey(keyCode: Int): Boolean {
        val res = exec("input keyevent $keyCode")
        return res.success
    }

    fun pressBack(): Boolean = pressKey(4)
    fun pressHome(): Boolean = pressKey(3)
    fun pressRecents(): Boolean = pressKey(187)
    fun pressPlayPause(): Boolean = pressKey(85)
    fun pressVolumeUp(): Boolean = pressKey(24)
    fun pressVolumeDown(): Boolean = pressKey(25)

    fun launchAppPackage(packageName: String): Boolean {
        val res = exec("monkey -p $packageName -c android.intent.category.LAUNCHER 1")
        return res.success
    }

    fun forceStop(packageName: String): Boolean {
        val res = exec("am force-stop $packageName")
        return res.success
    }

    /**
     * Dumps the current window XML via uiautomator and finds matching element coordinates
     * to perform a zero-touch tap with ADB privileges.
     */
    fun clickElementByTextOrDescription(targetText: String): Boolean {
        if (targetText.isBlank() || !isAvailableAndAuthorized()) return false
        return try {
            val dumpPath = "/data/local/tmp/nova_uidump.xml"
            val dumpRes = exec("uiautomator dump $dumpPath && cat $dumpPath")
            if (!dumpRes.success || dumpRes.output.isBlank()) {
                return false
            }

            val xml = dumpRes.output
            val cleanTarget = targetText.trim()

            // Look for nodes matching targetText in text="..." or content-desc="..."
            val regex = Regex("""bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"[^>]*?(?:text|content-desc)="([^"]*?)"""", RegexOption.IGNORE_CASE)
            val regexReverse = Regex("""(?:text|content-desc)="([^"]*?)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"""", RegexOption.IGNORE_CASE)

            var bestX = -1
            var bestY = -1

            for (match in regexReverse.findAll(xml)) {
                val foundText = match.groupValues[1]
                if (foundText.contains(cleanTarget, ignoreCase = true)) {
                    val x1 = match.groupValues[2].toIntOrNull() ?: 0
                    val y1 = match.groupValues[3].toIntOrNull() ?: 0
                    val x2 = match.groupValues[4].toIntOrNull() ?: 0
                    val y2 = match.groupValues[5].toIntOrNull() ?: 0
                    if (x2 > x1 && y2 > y1) {
                        bestX = (x1 + x2) / 2
                        bestY = (y1 + y2) / 2
                        break
                    }
                }
            }

            if (bestX == -1) {
                for (match in regex.findAll(xml)) {
                    val foundText = match.groupValues[5]
                    if (foundText.contains(cleanTarget, ignoreCase = true)) {
                        val x1 = match.groupValues[1].toIntOrNull() ?: 0
                        val y1 = match.groupValues[2].toIntOrNull() ?: 0
                        val x2 = match.groupValues[3].toIntOrNull() ?: 0
                        val y2 = match.groupValues[4].toIntOrNull() ?: 0
                        if (x2 > x1 && y2 > y1) {
                            bestX = (x1 + x2) / 2
                            bestY = (y1 + y2) / 2
                            break
                        }
                    }
                }
            }

            if (bestX > 0 && bestY > 0) {
                Log.i(TAG, "Found element \"$targetText\" at ($bestX, $bestY) via uiautomator dump, tapping...")
                tap(bestX, bestY)
            } else {
                false
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error clicking element by text via Shizuku", e)
            false
        }
    }

    fun release() {
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
        } catch (e: Throwable) {}
    }
}
