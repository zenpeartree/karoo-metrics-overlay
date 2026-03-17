package com.zenpeartree.karoometricsoverlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import io.hammerhead.karooext.KarooSystemService

class OverlayService : Service() {

    companion object {
        private const val TAG = "OverlayService"
        private const val CHANNEL_ID = "overlay_service"
        private const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.zenpeartree.karoometricsoverlay.START"
        const val ACTION_STOP = "com.zenpeartree.karoometricsoverlay.STOP"

        var serverAddress: String? = null
            private set
        var isRunning = false
            private set
        var lastError: String? = null
            private set
    }

    private var karooSystem: KarooSystemService? = null
    private var metricsCollector: MetricsCollector? = null
    private var overlayServer: OverlayServer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        if (isRunning) {
            Log.i(TAG, "Service already running, ignoring start")
            return START_STICKY
        }

        lastError = null
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Starting..."))

        val system = KarooSystemService(this)
        karooSystem = system

        system.connect { connected ->
            if (connected) {
                Log.i(TAG, "Connected to Karoo System")
                startServer(system)
            } else {
                Log.w(TAG, "Failed to connect to Karoo System")
                lastError = "Karoo connection failed"
                updateNotification("Karoo connection failed")
            }
        }

        return START_STICKY
    }

    private fun startServer(system: KarooSystemService) {
        try {
            val collector = MetricsCollector(system)
            metricsCollector = collector
            collector.start()

            val server = OverlayServer.getInstance(this)
            overlayServer = server
            server.start()

            val addr = getServerAddress()
            serverAddress = addr
            isRunning = true
            lastError = null
            updateNotification("Overlay running at $addr")
            Log.i(TAG, "=== OBS Overlay available at: $addr ===")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start overlay server", e)
            lastError = "Server failed: ${e.message}"
            updateNotification("Failed: ${e.message}")
            // Clean up partial state so a retry is possible
            metricsCollector?.stop()
            metricsCollector = null
            overlayServer = null
            stopSelf()
        }
    }

    override fun onDestroy() {
        isRunning = false
        serverAddress = null
        metricsCollector?.stop()
        metricsCollector = null
        try {
            overlayServer?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping server", e)
        }
        overlayServer = null
        karooSystem?.disconnect()
        karooSystem = null
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    private fun getServerAddress(): String {
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as? WifiManager
        val ip = wifiManager?.connectionInfo?.ipAddress ?: 0
        val ipStr = if (ip != 0) {
            "%d.%d.%d.%d".format(
                ip and 0xff,
                (ip shr 8) and 0xff,
                (ip shr 16) and 0xff,
                (ip shr 24) and 0xff,
            )
        } else {
            "localhost"
        }
        return "http://$ipStr:${OverlayServer.DEFAULT_PORT}/"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "OBS Overlay",
                NotificationManager.IMPORTANCE_LOW,
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    @Suppress("DEPRECATION")
    private fun buildNotification(text: String): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Karoo Metrics Overlay")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("Karoo Metrics Overlay")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build()
        }
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }
}
