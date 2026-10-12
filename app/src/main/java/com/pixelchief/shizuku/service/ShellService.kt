package com.pixelchief.shizuku.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.pixelchief.shizuku.IShellService
import com.pixelchief.shizuku.ShizukuManager
import kotlinx.coroutines.runBlocking

class ShellService : Service() {

    private val binder = object : IShellService.Stub() {
        override fun destroy() {
            stopSelf()
        }

        override fun execCommand(cmd: String): String {
            return runBlocking {
                val res = ShizukuManager.executeCommand(cmd)
                if (res.exitCode == 0) res.stdout else "ERROR (${res.exitCode}): ${res.stderr}"
            }
        }

        override fun execInt(cmd: String): Int {
            return runBlocking {
                ShizukuManager.executeCommand(cmd).exitCode
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
}
