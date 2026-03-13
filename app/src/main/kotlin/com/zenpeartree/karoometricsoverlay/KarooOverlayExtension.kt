package com.zenpeartree.karoometricsoverlay

import android.net.wifi.WifiManager
import android.util.Log
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.KarooExtension

class KarooOverlayExtension : KarooExtension("karoo-obs-overlay", "1") {

    companion object {
        private const val TAG = "KarooOverlayExt"
    }

    private lateinit var karooSystem: KarooSystemService
    private lateinit var metricsCollector: MetricsCollector
    private lateinit var overlayServer: OverlayServer

    override fun onCreate() {
        super.onCreate()
        karooSystem = KarooSystemService(this)
        metricsCollector = MetricsCollector(karooSystem)
        overlayServer = OverlayServer(this)

        karooSystem.connect { connected ->
            if (connected) {
                Log.i(TAG, "Connected to Karoo System")
                metricsCollector.start()
                try {
                    overlayServer.start()
                    logServerAddress()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start overlay server", e)
                }
            } else {
                Log.w(TAG, "Failed to connect to Karoo System")
            }
        }
    }

    override fun onDestroy() {
        try {
            overlayServer.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping server", e)
        }
        metricsCollector.stop()
        karooSystem.disconnect()
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    private fun logServerAddress() {
        try {
            val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as? WifiManager
            val ip = wifiManager?.connectionInfo?.ipAddress ?: return
            val ipStr = "%d.%d.%d.%d".format(
                ip and 0xff,
                (ip shr 8) and 0xff,
                (ip shr 16) and 0xff,
                (ip shr 24) and 0xff,
            )
            Log.i(TAG, "=== OBS Overlay available at: http://$ipStr:${OverlayServer.DEFAULT_PORT}/ ===")
        } catch (e: Exception) {
            Log.w(TAG, "Could not determine IP address", e)
        }
    }
}
