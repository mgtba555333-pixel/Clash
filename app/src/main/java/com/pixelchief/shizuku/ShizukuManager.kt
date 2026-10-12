package com.pixelchief.shizuku

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

object ShizukuManager {
    private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

    private val _status = MutableStateFlow(ShizukuStatus.CHECKING)
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        LogBus.info("Shizuku", "Binder received from Shizuku service.")
        checkStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        LogBus.warn("Shizuku", "Shizuku service died or disconnected.")
        _status.value = ShizukuStatus.NOT_RUNNING
    }

    private val requestPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                LogBus.success("Shizuku", "Shizuku permission granted by user!")
                _status.value = ShizukuStatus.READY
            } else {
                LogBus.error("Shizuku", "Shizuku permission denied by user (code $requestCode).")
                _status.value = ShizukuStatus.PERMISSION_DENIED
            }
        }

    fun init() {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
            checkStatus()
        } catch (e: Exception) {
            LogBus.warn("Shizuku", "Failed to register Shizuku listeners: ${e.message}")
            _status.value = ShizukuStatus.NOT_RUNNING
        }
    }

    fun checkStatus(context: Context? = null): ShizukuStatus {
        try {
            if (context != null) {
                val isInstalled = isShizukuInstalled(context)
                if (!isInstalled) {
                    _status.value = ShizukuStatus.NOT_INSTALLED
                    return ShizukuStatus.NOT_INSTALLED
                }
            }

            val isPingAlive = Shizuku.pingBinder()
            if (!isPingAlive) {
                _status.value = ShizukuStatus.NOT_RUNNING
                return ShizukuStatus.NOT_RUNNING
            }

            _version.value = Shizuku.getVersion()

            val hasPermission = if (Shizuku.isPreV11()) {
                false
            } else {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }

            _status.value = if (hasPermission) ShizukuStatus.READY else ShizukuStatus.PERMISSION_DENIED
            return _status.value
        } catch (e: Exception) {
            _status.value = ShizukuStatus.NOT_RUNNING
            return ShizukuStatus.NOT_RUNNING
        }
    }

    fun isShizukuInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun requestPermission(requestCode: Int = 1001) {
        try {
            if (Shizuku.isPreV11()) {
                LogBus.warn("Shizuku", "Shizuku pre-v11 does not support runtime permission requests.")
                return
            }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                _status.value = ShizukuStatus.READY
                return
            }
            if (Shizuku.shouldShowRequestPermissionRationale()) {
                LogBus.info("Shizuku", "Showing permission rationale for Shizuku.")
            }
            Shizuku.requestPermission(requestCode)
        } catch (e: Exception) {
            LogBus.error("Shizuku", "Failed to request Shizuku permission: ${e.message}")
        }
    }

    suspend fun executeCommand(command: String): ShellResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val process: Process = if (_status.value == ShizukuStatus.READY && Shizuku.pingBinder()) {
                try {
                    val method = Shizuku::class.java.getDeclaredMethod(
                        "newProcess",
                        Array<String>::class.java,
                        Array<String>::class.java,
                        String::class.java
                    )
                    method.isAccessible = true
                    method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
                } catch (e: Exception) {
                    Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                }
            } else {
                Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            }

            val stdout = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
            val exitCode = process.waitFor()
            val duration = System.currentTimeMillis() - startTime
            return@withContext ShellResult(
                exitCode = exitCode,
                stdout = stdout.trim(),
                stderr = stderr.trim(),
                durationMs = duration
            )
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            return@withContext ShellResult(
                exitCode = -1,
                stdout = "",
                stderr = "Execution failed: ${e.message}",
                durationMs = duration
            )
        }
    }

    suspend fun tap(x: Int, y: Int): ShellResult {
        LogBus.debug("ADB", "Tap -> ($x, $y)")
        return executeCommand("input tap $x $y")
    }

    suspend fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Int = 300): ShellResult {
        LogBus.debug("ADB", "Swipe -> ($x1, $y1) to ($x2, $y2) [${durationMs}ms]")
        return executeCommand("input swipe $x1 $y1 $x2 $y2 $durationMs")
    }

    suspend fun launchApp(packageName: String): ShellResult {
        LogBus.info("ADB", "Launching game package: $packageName")
        return executeCommand("monkey -p $packageName -c android.intent.category.LAUNCHER 1")
    }

    suspend fun keyPress(keyCode: Int): ShellResult {
        return executeCommand("input keyevent $keyCode")
    }
}
