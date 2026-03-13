package com.zenpeartree.karoometricsoverlay

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var addressText: TextView
    private lateinit var toggleButton: Button
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }

        val titleText = TextView(this).apply {
            text = "Karoo OBS Overlay"
            textSize = 22f
            gravity = Gravity.CENTER
        }
        layout.addView(titleText)

        statusText = TextView(this).apply {
            text = "Stopped"
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 8)
        }
        layout.addView(statusText)

        addressText = TextView(this).apply {
            text = ""
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }
        layout.addView(addressText)

        toggleButton = Button(this).apply {
            text = "Start Server"
            setOnClickListener { toggleService() }
        }
        layout.addView(toggleButton)

        val infoText = TextView(this).apply {
            text = "\nAdd the URL above as a Browser Source in OBS.\nWidth: 400, Height: 150"
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 0)
        }
        layout.addView(infoText)

        setContentView(layout)
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun toggleService() {
        if (OverlayService.isRunning) {
            val intent = Intent(this, OverlayService::class.java)
            intent.action = OverlayService.ACTION_STOP
            startService(intent)
            handler.postDelayed({ updateUI() }, 500)
        } else {
            val intent = Intent(this, OverlayService::class.java)
            intent.action = OverlayService.ACTION_START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            handler.postDelayed({ updateUI() }, 2000)
        }
    }

    private fun updateUI() {
        if (OverlayService.isRunning) {
            statusText.text = "Running"
            addressText.text = OverlayService.serverAddress ?: "Getting address..."
            toggleButton.text = "Stop Server"
        } else {
            statusText.text = "Stopped"
            addressText.text = ""
            toggleButton.text = "Start Server"
        }
    }
}
