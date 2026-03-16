package com.zenpeartree.karoometricsoverlay

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    companion object {
        private const val PREFS_NAME = "karoo_overlay_prefs"
        const val KEY_FTP = "ftp"
        const val KEY_MAX_HR = "max_hr"
        const val DEFAULT_FTP = 200
        const val DEFAULT_MAX_HR = 190
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var statusText: TextView
    private lateinit var addressText: TextView
    private lateinit var errorText: TextView
    private lateinit var toggleButton: Button
    private lateinit var ftpInput: EditText
    private lateinit var maxHrInput: EditText
    private val handler = Handler(Looper.getMainLooper())

    private val uiUpdater = object : Runnable {
        override fun run() {
            updateUI()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val scroll = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(48, 48, 48, 48)
        }

        val titleText = TextView(this).apply {
            text = "Karoo Metrics Overlay"
            textSize = 22f
            gravity = Gravity.CENTER
        }
        layout.addView(titleText)

        // --- Zone Settings ---
        val settingsLabel = TextView(this).apply {
            text = "Zone Settings"
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 12)
        }
        layout.addView(settingsLabel)

        // FTP row
        val ftpRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 4, 0, 4)
        }
        val ftpLabel = TextView(this).apply {
            text = "FTP (watts):"
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        ftpInput = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            imeOptions = EditorInfo.IME_ACTION_NEXT
            setText(prefs.getInt(KEY_FTP, DEFAULT_FTP).toString())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            gravity = Gravity.CENTER
        }
        ftpRow.addView(ftpLabel)
        ftpRow.addView(ftpInput)
        layout.addView(ftpRow)

        // Max HR row
        val hrRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 4, 0, 4)
        }
        val hrLabel = TextView(this).apply {
            text = "Max HR (bpm):"
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        maxHrInput = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            imeOptions = EditorInfo.IME_ACTION_DONE
            setText(prefs.getInt(KEY_MAX_HR, DEFAULT_MAX_HR).toString())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            gravity = Gravity.CENTER
        }
        hrRow.addView(hrLabel)
        hrRow.addView(maxHrInput)
        layout.addView(hrRow)

        // --- Status ---
        statusText = TextView(this).apply {
            text = "Stopped"
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 4)
        }
        layout.addView(statusText)

        addressText = TextView(this).apply {
            text = ""
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 4)
        }
        layout.addView(addressText)

        errorText = TextView(this).apply {
            text = ""
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
            setTextColor(0xFFFF6666.toInt())
        }
        layout.addView(errorText)

        toggleButton = Button(this).apply {
            text = "Start Server"
            setOnClickListener { toggleService() }
        }
        layout.addView(toggleButton)

        val infoText = TextView(this).apply {
            text = "\nAdd the URL above as a Browser Source in OBS.\nWidth: 440, Height: 180"
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 0)
        }
        layout.addView(infoText)

        scroll.addView(layout)
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        updateUI()
        handler.postDelayed(uiUpdater, 1000)
    }

    override fun onPause() {
        handler.removeCallbacks(uiUpdater)
        super.onPause()
    }

    private fun saveSettings() {
        val ftp = ftpInput.text.toString().toIntOrNull() ?: DEFAULT_FTP
        val maxHr = maxHrInput.text.toString().toIntOrNull() ?: DEFAULT_MAX_HR
        prefs.edit()
            .putInt(KEY_FTP, ftp)
            .putInt(KEY_MAX_HR, maxHr)
            .apply()
    }

    private fun toggleService() {
        if (OverlayService.isRunning) {
            val intent = Intent(this, OverlayService::class.java)
            intent.action = OverlayService.ACTION_STOP
            startService(intent)
            handler.postDelayed({ updateUI() }, 500)
        } else {
            saveSettings()
            val intent = Intent(this, OverlayService::class.java)
            intent.action = OverlayService.ACTION_START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    private fun updateUI() {
        if (OverlayService.isRunning) {
            statusText.text = "Running"
            addressText.text = OverlayService.serverAddress ?: "Getting address..."
            toggleButton.text = "Stop Server"
            errorText.text = ""
            ftpInput.isEnabled = false
            maxHrInput.isEnabled = false
        } else {
            statusText.text = "Stopped"
            addressText.text = ""
            toggleButton.text = "Start Server"
            errorText.text = OverlayService.lastError ?: ""
            ftpInput.isEnabled = true
            maxHrInput.isEnabled = true
        }
    }
}
