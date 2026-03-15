package com.zenpeartree.karoometricsoverlay

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoWSD
import java.io.IOException
import java.net.ServerSocket
import java.nio.charset.StandardCharsets
import java.util.concurrent.CopyOnWriteArraySet

class OverlayServer(
    private val context: Context,
    port: Int = DEFAULT_PORT,
) : NanoWSD(port) {

    companion object {
        private const val TAG = "OverlayServer"
        const val DEFAULT_PORT = 9091
        private const val BROADCAST_INTERVAL_MS = 500L
    }

    private val clients = CopyOnWriteArraySet<MetricsWebSocket>()
    private var overlayHtml: ByteArray? = null
    private var broadcastThread: HandlerThread? = null
    private var broadcastHandler: Handler? = null

    private val broadcastRunnable = object : Runnable {
        override fun run() {
            broadcastMetrics()
            broadcastHandler?.postDelayed(this, BROADCAST_INTERVAL_MS)
        }
    }

    init {
        setServerSocketFactory(ReuseAddrSocketFactory())
    }

    override fun start() {
        loadOverlayHtml()
        // Force stop any lingering server before binding
        try { super.stop() } catch (_: Exception) {}
        Thread.sleep(200)
        super.start()
        startBroadcastLoop()
        Log.i(TAG, "Server started on port $listeningPort")
    }

    private class ReuseAddrSocketFactory : NanoHTTPD.ServerSocketFactory {
        override fun create(): ServerSocket {
            return ServerSocket().apply { reuseAddress = true }
        }
    }

    override fun stop() {
        stopBroadcastLoop()
        clients.clear()
        super.stop()
        Log.i(TAG, "Server stopped")
    }

    override fun openWebSocket(handshake: IHTTPSession): WebSocket {
        return MetricsWebSocket(handshake)
    }

    override fun serveHttp(session: IHTTPSession): Response {
        return when (session.uri) {
            "/", "/index.html" -> {
                val html = overlayHtml
                if (html != null) {
                    newFixedLengthResponse(Response.Status.OK, "text/html", html.inputStream(), html.size.toLong())
                } else {
                    newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Overlay HTML not loaded")
                }
            }
            "/metrics" -> {
                val json = MetricsState.get().toJson()
                newFixedLengthResponse(Response.Status.OK, "application/json", json).also {
                    it.addHeader("Access-Control-Allow-Origin", "*")
                }
            }
            else -> {
                newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
            }
        }
    }

    private fun loadOverlayHtml() {
        try {
            val template = context.assets.open("overlay.html").use {
                it.readBytes().toString(StandardCharsets.UTF_8)
            }
            val prefs = context.getSharedPreferences("karoo_overlay_prefs", Context.MODE_PRIVATE)
            val ftp = prefs.getInt(MainActivity.KEY_FTP, MainActivity.DEFAULT_FTP)
            val maxHr = prefs.getInt(MainActivity.KEY_MAX_HR, MainActivity.DEFAULT_MAX_HR)

            val injected = template
                .replace("var FTP = 250;", "var FTP = $ftp;")
                .replace("var MAX_HR = 187;", "var MAX_HR = $maxHr;")

            overlayHtml = injected.toByteArray(StandardCharsets.UTF_8)
            Log.i(TAG, "Loaded overlay.html (${overlayHtml?.size} bytes) with FTP=$ftp, MAX_HR=$maxHr")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to load overlay.html", e)
        }
    }

    private fun startBroadcastLoop() {
        val thread = HandlerThread("overlay-broadcast").apply { start() }
        broadcastThread = thread
        broadcastHandler = Handler(thread.looper).also {
            it.postDelayed(broadcastRunnable, BROADCAST_INTERVAL_MS)
        }
    }

    private fun stopBroadcastLoop() {
        broadcastHandler?.removeCallbacks(broadcastRunnable)
        broadcastThread?.quitSafely()
        broadcastThread = null
        broadcastHandler = null
    }

    private fun broadcastMetrics() {
        if (clients.isEmpty()) return
        val json = MetricsState.get().toJson()
        val toRemove = mutableListOf<MetricsWebSocket>()
        for (client in clients) {
            try {
                client.send(json)
            } catch (e: IOException) {
                toRemove.add(client)
            }
        }
        toRemove.forEach { clients.remove(it) }
    }

    inner class MetricsWebSocket(handshake: IHTTPSession) : WebSocket(handshake) {

        override fun onOpen() {
            clients.add(this)
            Log.d(TAG, "WebSocket client connected (${clients.size} total)")
        }

        override fun onClose(code: NanoWSD.WebSocketFrame.CloseCode?, reason: String?, initiatedByRemote: Boolean) {
            clients.remove(this)
            Log.d(TAG, "WebSocket client disconnected (${clients.size} total)")
        }

        override fun onMessage(message: NanoWSD.WebSocketFrame?) {
            // Server push only — ignore client messages
        }

        override fun onPong(pong: NanoWSD.WebSocketFrame?) {}

        override fun onException(exception: IOException?) {
            clients.remove(this)
            Log.w(TAG, "WebSocket error", exception)
        }
    }
}
