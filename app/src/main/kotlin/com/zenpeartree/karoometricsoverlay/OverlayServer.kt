package com.zenpeartree.karoometricsoverlay

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoWSD
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.SocketAddress
import java.nio.channels.ServerSocketChannel
import java.nio.charset.StandardCharsets
import java.util.concurrent.CopyOnWriteArraySet

class OverlayServer private constructor(
    private val context: Context,
    port: Int = DEFAULT_PORT,
) : NanoWSD(port) {

    companion object {
        private const val TAG = "OverlayServer"
        const val DEFAULT_PORT = 9091
        private const val BROADCAST_INTERVAL_MS = 500L

        private var instance: OverlayServer? = null

        @Synchronized
        fun getInstance(context: Context): OverlayServer {
            return instance ?: OverlayServer(context.applicationContext).also { instance = it }
        }
    }

    private val clients = CopyOnWriteArraySet<MetricsWebSocket>()
    private var overlayHtml: ByteArray? = null
    private var broadcastThread: HandlerThread? = null
    private var broadcastHandler: Handler? = null
    private var managedChannel: ServerSocketChannel? = null

    private val broadcastRunnable = object : Runnable {
        override fun run() {
            broadcastMetrics()
            broadcastHandler?.postDelayed(this, BROADCAST_INTERVAL_MS)
        }
    }

    init {
        setServerSocketFactory(object : NanoHTTPD.ServerSocketFactory {
            override fun create(): ServerSocket {
                return createBoundSocket(DEFAULT_PORT)
            }
        })
    }

    /**
     * Creates a ServerSocket via ServerSocketChannel with SO_REUSEADDR,
     * pre-bound so NanoHTTPD won't try to bind again.
     * ServerSocketChannel provides more reliable reuseAddress on Android.
     */
    private fun createBoundSocket(port: Int): ServerSocket {
        closeManagedChannel()
        val channel = ServerSocketChannel.open()
        managedChannel = channel
        val socket = channel.socket()
        socket.reuseAddress = true
        socket.bind(InetSocketAddress(port))
        // Return a wrapper that ignores bind() — NanoHTTPD 2.3.1
        // calls bind() in ServerRunnable without checking isBound().
        return object : ServerSocket() {
            private val delegate = socket

            override fun bind(endpoint: SocketAddress?) = Unit
            override fun bind(endpoint: SocketAddress?, backlog: Int) = Unit
            override fun accept() = delegate.accept()
            override fun close() { delegate.close(); channel.close() }
            override fun isBound() = delegate.isBound
            override fun isClosed() = delegate.isClosed
            override fun getLocalPort() = delegate.localPort
            override fun getInetAddress() = delegate.inetAddress
            override fun getLocalSocketAddress() = delegate.localSocketAddress
            override fun getReuseAddress() = delegate.reuseAddress
            override fun setReuseAddress(on: Boolean) { delegate.reuseAddress = on }
            override fun setSoTimeout(timeout: Int) { delegate.soTimeout = timeout }
            override fun getSoTimeout() = delegate.soTimeout
            override fun setReceiveBufferSize(size: Int) { delegate.receiveBufferSize = size }
            override fun getReceiveBufferSize() = delegate.receiveBufferSize
        }
    }

    private fun closeManagedChannel() {
        managedChannel?.let { ch ->
            try {
                if (ch.isOpen) {
                    ch.socket().close()
                    ch.close()
                    Log.i(TAG, "Closed previous server channel")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error closing previous channel", e)
            }
        }
        managedChannel = null
    }

    override fun start() {
        loadOverlayHtml()
        // Stop any previous NanoHTTPD state
        try { super.stop() } catch (_: Exception) {}
        closeManagedChannel()

        var lastException: IOException? = null
        for (attempt in 1..5) {
            try {
                super.start()
                startBroadcastLoop()
                Log.i(TAG, "Server started on port $listeningPort (attempt $attempt)")
                return
            } catch (e: IOException) {
                Log.w(TAG, "Start attempt $attempt/5 failed: ${e.message}")
                lastException = e
                try { super.stop() } catch (_: Exception) {}
                closeManagedChannel()
                Thread.sleep(1000L * attempt)
            }
        }
        throw lastException ?: IOException("Failed to start server")
    }

    override fun stop() {
        stopBroadcastLoop()
        clients.clear()
        super.stop()
        closeManagedChannel()
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
            val shareLocation = prefs.getBoolean(
                MainActivity.KEY_SHARE_LOCATION,
                MainActivity.DEFAULT_SHARE_LOCATION,
            )

            val injected = template
                .replace("var FTP = 250;", "var FTP = $ftp;")
                .replace("var MAX_HR = 187;", "var MAX_HR = $maxHr;")
                .replace("var SHOW_MAP = false;", "var SHOW_MAP = $shareLocation;")

            overlayHtml = injected.toByteArray(StandardCharsets.UTF_8)
            Log.i(
                TAG,
                "Loaded overlay.html (${overlayHtml?.size} bytes) with FTP=$ftp, MAX_HR=$maxHr, SHOW_MAP=$shareLocation",
            )
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
