package com.zenpeartree.karoometricsoverlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import io.hammerhead.karooext.KarooSystemService

class OverlayService : Service() {

    companion object {
        private const val TAG = "OverlayService"
        private const val CHANNEL_ID = "overlay_service"
        private const val NOTIFICATION_ID = 1
        private const val CONNECT_RETRY_DELAY_MS = 3_000L
        private const val MAX_CONNECT_RETRIES = 5
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
    private val serviceHandler = Handler(Looper.getMainLooper())
    private var reconnectInProgress = false
    private var connectRetryCount = 0
    private var destroyed = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        destroyed = false
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
        serverAddress = null
        connectRetryCount = 0
        MetricsState.reset()
        DiagnosticEvents.clear()
        DiagnosticEvents.record(TAG, "Overlay service start requested")
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Starting..."))

        beginKarooConnection(startServer = true)

        return START_STICKY
    }

    private fun beginKarooConnection(startServer: Boolean) {
        if (destroyed) return
        DiagnosticEvents.record(TAG, "Beginning Karoo connection (startServer=$startServer)")
        val system = KarooSystemService(this)
        karooSystem = system
        connectToKarooSystem(system, startServer)
    }

    private fun connectToKarooSystem(system: KarooSystemService, startServer: Boolean) {
        system.connect { connected ->
            serviceHandler.post {
                if (destroyed || karooSystem !== system) {
                    return@post
                }
                if (connected) {
                    DiagnosticEvents.record(TAG, "Connected to Karoo System")
                    connectRetryCount = 0
                    startOverlay(system, startServer)
                } else {
                    handleConnectionFailure(startServer)
                }
            }
        }
    }

    private fun startOverlay(system: KarooSystemService, startServer: Boolean) {
        try {
            val prefs = getSharedPreferences("karoo_overlay_prefs", Context.MODE_PRIVATE)
            val shareLocation = prefs.getBoolean(
                MainActivity.KEY_SHARE_LOCATION,
                MainActivity.DEFAULT_SHARE_LOCATION,
            )
            val subscribePower = prefs.getBoolean(
                MainActivity.KEY_SUBSCRIBE_POWER,
                MainActivity.DEFAULT_SUBSCRIBE_POWER,
            )
            val subscribeHeartRate = prefs.getBoolean(
                MainActivity.KEY_SUBSCRIBE_HR,
                MainActivity.DEFAULT_SUBSCRIBE_HR,
            )

            val collector = MetricsCollector(
                karooSystem = system,
                shareLocation = shareLocation,
                subscribePower = subscribePower,
                subscribeHeartRate = subscribeHeartRate,
                onReconnectRequested = ::requestKarooReconnect,
            )
            metricsCollector = collector
            collector.start()

            if (startServer) {
                val server = OverlayServer.getInstance(this)
                overlayServer = server
                server.start()
            }

            val addr = getServerAddress()
            serverAddress = addr
            isRunning = true
            lastError = null
            reconnectInProgress = false
            connectRetryCount = 0
            updateNotification("Overlay running at $addr")
            DiagnosticEvents.record(TAG, "Overlay running at $addr")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start overlay server", e)
            lastError = "Server failed: ${e.message}"
            reconnectInProgress = false
            DiagnosticEvents.recordWarning(TAG, "Failed to start overlay server: ${e.message}")
            updateNotification("Failed: ${e.message}")
            // Clean up partial state so a retry is possible
            metricsCollector?.stop()
            metricsCollector = null
            try {
                overlayServer?.stop()
            } catch (stopError: Exception) {
                Log.w(TAG, "Error stopping partially started server", stopError)
            }
            overlayServer = null
            stopSelf()
        }
    }

    override fun onDestroy() {
        destroyed = true
        serviceHandler.removeCallbacksAndMessages(null)
        DiagnosticEvents.record(TAG, "Overlay service stopping")
        isRunning = false
        serverAddress = null
        shutdownSession(stopServer = true, resetMetrics = true)
        clearForegroundNotification()
        super.onDestroy()
    }

    private fun handleConnectionFailure(startServer: Boolean) {
        if (destroyed) return
        karooSystem?.disconnect()
        karooSystem = null

        if (connectRetryCount < MAX_CONNECT_RETRIES) {
            connectRetryCount += 1
            reconnectInProgress = true
            lastError = "Karoo connection failed, retrying"
            updateNotification("Karoo reconnect ${connectRetryCount}/$MAX_CONNECT_RETRIES...")
            DiagnosticEvents.recordWarning(
                TAG,
                "Failed to connect to Karoo System, retry ${connectRetryCount}/$MAX_CONNECT_RETRIES",
            )
            serviceHandler.postDelayed(
                { beginKarooConnection(startServer) },
                CONNECT_RETRY_DELAY_MS,
            )
            return
        }

        DiagnosticEvents.recordWarning(TAG, "Failed to connect to Karoo System after $MAX_CONNECT_RETRIES retries")
        reconnectInProgress = false
        lastError = "Karoo connection failed"
        serverAddress = null
        isRunning = false
        updateNotification("Karoo connection failed")
        stopSelf()
    }

    private fun requestKarooReconnect(reason: String) {
        serviceHandler.post {
            if (destroyed) return@post
            if (reconnectInProgress) {
                Log.i(TAG, "Karoo reconnect already in progress, ignoring request: $reason")
                return@post
            }
            reconnectInProgress = true
            connectRetryCount = 0
            DiagnosticEvents.recordWarning(TAG, "Reconnecting to Karoo System after metric stall: $reason")
            updateNotification("Recovering metric stream...")
            shutdownSession(stopServer = true, resetMetrics = true)
            beginKarooConnection(startServer = true)
        }
    }

    private fun shutdownSession(stopServer: Boolean, resetMetrics: Boolean) {
        metricsCollector?.stop()
        metricsCollector = null
        if (stopServer) {
            try {
                overlayServer?.stop()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping server", e)
            }
            overlayServer = null
        }
        karooSystem?.disconnect()
        karooSystem = null
        if (resetMetrics) {
            MetricsState.reset()
        }
        isRunning = false
        serverAddress = null
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

    @Suppress("DEPRECATION")
    private fun clearForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
    }
}
