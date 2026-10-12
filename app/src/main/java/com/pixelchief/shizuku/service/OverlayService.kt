package com.pixelchief.shizuku.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.R
import com.pixelchief.shizuku.BotState
import com.pixelchief.shizuku.LogBus
import com.pixelchief.shizuku.PixelChiefApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var stateCollectJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        setupFloatingView()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFloatingView() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 200
        }
        overlayParams = params

        // Create overlay container
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xDD0F172A.toInt())
            setPadding(16, 12, 16, 12)
            elevation = 12f
        }

        val iconView = ImageView(this).apply {
            setImageResource(R.drawable.ic_notif)
            layoutParams = LinearLayout.LayoutParams(64, 64).apply {
                gravity = Gravity.CENTER_VERTICAL
                marginEnd = 16
            }
        }

        val textLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val titleView = TextView(this).apply {
            text = "PixelChief"
            setTextColor(0xFFFFD700.toInt())
            textSize = 12f
            paint.isFakeBoldText = true
        }

        val statusView = TextView(this).apply {
            text = "Idle"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 11f
        }

        textLayout.addView(titleView)
        textLayout.addView(statusView)

        val btnAction = TextView(this).apply {
            text = "START"
            setTextColor(0xFF00FFCC.toInt())
            textSize = 12f
            paint.isFakeBoldText = true
            setPadding(24, 8, 12, 8)
            setOnClickListener {
                val botController = (applicationContext as PixelChiefApp).botController
                if (botController.botState.value == BotState.RUNNING) {
                    botController.pause()
                } else {
                    botController.start()
                }
            }
        }

        rootLayout.addView(iconView)
        rootLayout.addView(textLayout)
        rootLayout.addView(btnAction)

        // Make root layout draggable
        rootLayout.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                val p = overlayParams ?: return false
                val wm = windowManager ?: return false
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = p.x
                        initialY = p.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        p.x = initialX + (event.rawX - initialTouchX).toInt()
                        p.y = initialY + (event.rawY - initialTouchY).toInt()
                        wm.updateViewLayout(rootLayout, p)
                        return true
                    }
                }
                return false
            }
        })

        overlayView = rootLayout
        try {
            windowManager?.addView(rootLayout, params)
        } catch (e: Exception) {
            LogBus.error("Overlay", "Failed to add window view: ${e.message}")
        }

        // Observe bot state
        val botController = (applicationContext as PixelChiefApp).botController
        stateCollectJob = scope.launch {
            botController.botState.collectLatest { state ->
                when (state) {
                    BotState.RUNNING -> {
                        statusView.text = "Running..."
                        statusView.setTextColor(0xFF10B981.toInt())
                        btnAction.text = "PAUSE"
                    }
                    BotState.PAUSED -> {
                        statusView.text = "Paused"
                        statusView.setTextColor(0xFFF59E0B.toInt())
                        btnAction.text = "RESUME"
                    }
                    BotState.SEARCHING -> {
                        statusView.text = "Searching..."
                        statusView.setTextColor(0xFF38BDF8.toInt())
                        btnAction.text = "PAUSE"
                    }
                    BotState.ATTACKING -> {
                        statusView.text = "Attacking..."
                        statusView.setTextColor(0xFFEF4444.toInt())
                        btnAction.text = "PAUSE"
                    }
                    else -> {
                        statusView.text = "Idle"
                        statusView.setTextColor(0xFF94A3B8.toInt())
                        btnAction.text = "START"
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stateCollectJob?.cancel()
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
