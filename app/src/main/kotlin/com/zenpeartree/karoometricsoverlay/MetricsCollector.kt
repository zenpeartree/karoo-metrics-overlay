package com.zenpeartree.karoometricsoverlay

import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.OnStreamState
import io.hammerhead.karooext.models.StreamState

class MetricsCollector(
    private val karooSystem: KarooSystemService,
    private val shareLocation: Boolean,
    private val onReconnectRequested: (String) -> Unit = {},
) {

    companion object {
        private const val TAG = "MetricsCollector"
        private const val MS_TO_KMH = 3.6
        private const val M_TO_KM = 0.001
        private const val WATCHDOG_INTERVAL_MS = 5_000L
        private const val INITIAL_DATA_TIMEOUT_MS = 15_000L
        private const val STALE_DATA_TIMEOUT_MS = 15_000L
        private const val RESUBSCRIBE_DELAY_MS = 2_000L
    }

    private val lock = Any()
    private val consumerIds = mutableListOf<String>()
    private var watchdogThread: HandlerThread? = null
    private var watchdogHandler: Handler? = null
    private var started = false
    private var refreshPending = false
    private var refreshAttempts = 0

    @Volatile
    private var lastMetricDataAt = 0L

    @Volatile
    private var lastSubscriptionAt = 0L

    private val watchdogRunnable = object : Runnable {
        override fun run() {
            checkStreamHealth()
            watchdogHandler?.postDelayed(this, WATCHDOG_INTERVAL_MS)
        }
    }

    fun start() {
        synchronized(lock) {
            if (started) {
                Log.i(TAG, "Metrics collector already started")
                return
            }
            started = true
            lastMetricDataAt = 0L
            refreshAttempts = 0
            startWatchdogLocked()
            subscribeAllLocked()
        }
    }

    fun stop() {
        synchronized(lock) {
            if (!started) return
            started = false
            refreshPending = false
            stopWatchdogLocked()
            unsubscribeAllLocked()
            lastMetricDataAt = 0L
            lastSubscriptionAt = 0L
            refreshAttempts = 0
            Log.i(TAG, "Stopped collecting metrics")
        }
    }

    private fun subscribeSpeed() {
        val id = karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.SPEED),
            onError = { handleStreamError("Speed", it) },
        ) { event: OnStreamState ->
            when (val state = event.state) {
                is StreamState.Streaming -> {
                    val value = state.dataPoint.values[DataType.Field.SPEED]
                    if (value != null) {
                        markMetricDataReceived()
                        MetricsState.updateSpeed(value * MS_TO_KMH)
                    }
                }
                else -> Log.d(TAG, "Speed stream state: $state")
            }
        }
        consumerIds.add(id)
    }

    private fun subscribePower() {
        val id = karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.POWER),
            onError = { handleStreamError("Power", it) },
        ) { event: OnStreamState ->
            when (val state = event.state) {
                is StreamState.Streaming -> {
                    val value = state.dataPoint.values[DataType.Field.POWER]
                    if (value != null) {
                        markMetricDataReceived()
                        MetricsState.updatePower(value.toInt())
                    }
                }
                else -> Log.d(TAG, "Power stream state: $state")
            }
        }
        consumerIds.add(id)
    }

    private fun subscribeAvgSpeed() {
        val id = karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.AVERAGE_SPEED),
            onError = { handleStreamError("Avg speed", it) },
        ) { event: OnStreamState ->
            when (val state = event.state) {
                is StreamState.Streaming -> {
                    val value = state.dataPoint.values[DataType.Field.AVERAGE_SPEED]
                    if (value != null) {
                        markMetricDataReceived()
                        MetricsState.updateAvgSpeed(value * MS_TO_KMH)
                    }
                }
                else -> Log.d(TAG, "Avg speed stream state: $state")
            }
        }
        consumerIds.add(id)
    }

    private fun subscribeHeartRate() {
        val id = karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.HEART_RATE),
            onError = { handleStreamError("HR", it) },
        ) { event: OnStreamState ->
            when (val state = event.state) {
                is StreamState.Streaming -> {
                    val value = state.dataPoint.values[DataType.Field.HEART_RATE]
                    if (value != null) {
                        markMetricDataReceived()
                        MetricsState.updateHeartRate(value.toInt())
                    }
                }
                else -> Log.d(TAG, "HR stream state: $state")
            }
        }
        consumerIds.add(id)
    }

    private fun subscribeDistance() {
        val id = karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.DISTANCE),
            onError = { handleStreamError("Distance", it) },
        ) { event: OnStreamState ->
            when (val state = event.state) {
                is StreamState.Streaming -> {
                    val value = state.dataPoint.values[DataType.Field.DISTANCE]
                    if (value != null) {
                        markMetricDataReceived()
                        MetricsState.updateDistance(value * M_TO_KM)
                    }
                }
                else -> Log.d(TAG, "Distance stream state: $state")
            }
        }
        consumerIds.add(id)
    }

    private fun subscribeGrade() {
        val id = karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.ELEVATION_GRADE),
            onError = { handleStreamError("Grade", it) },
        ) { event: OnStreamState ->
            when (val state = event.state) {
                is StreamState.Streaming -> {
                    val value = state.dataPoint.values[DataType.Field.ELEVATION_GRADE]
                    if (value != null) {
                        markMetricDataReceived()
                        MetricsState.updateGrade(value)
                    }
                }
                else -> Log.d(TAG, "Grade stream state: $state")
            }
        }
        consumerIds.add(id)
    }

    private fun subscribeAvgPower() {
        val id = karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.AVERAGE_POWER),
            onError = { handleStreamError("Avg power", it) },
        ) { event: OnStreamState ->
            when (val state = event.state) {
                is StreamState.Streaming -> {
                    val value = state.dataPoint.values[DataType.Field.AVERAGE_POWER]
                    if (value != null) {
                        markMetricDataReceived()
                        MetricsState.updateAvgPower(value.toInt())
                    }
                }
                else -> Log.d(TAG, "Avg power stream state: $state")
            }
        }
        consumerIds.add(id)
    }

    private fun subscribeElevationGain() {
        val id = karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.ELEVATION_GAIN),
            onError = { handleStreamError("Elevation gain", it) },
        ) { event: OnStreamState ->
            when (val state = event.state) {
                is StreamState.Streaming -> {
                    val value = state.dataPoint.values[DataType.Field.ELEVATION_GAIN]
                    if (value != null) {
                        markMetricDataReceived()
                        MetricsState.updateElevationGain(value)
                    }
                }
                else -> Log.d(TAG, "Elevation gain stream state: $state")
            }
        }
        consumerIds.add(id)
    }

    private fun subscribeLocation() {
        val id = karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.LOCATION),
            onError = { handleStreamError("Location", it) },
        ) { event: OnStreamState ->
            when (val state = event.state) {
                is StreamState.Streaming -> {
                    val lat = state.dataPoint.values[DataType.Field.LOC_LATITUDE]
                    val lng = state.dataPoint.values[DataType.Field.LOC_LONGITUDE]
                    if (lat != null && lng != null) {
                        MetricsState.updateLocation(lat, lng)
                    }
                }
                else -> Log.d(TAG, "Location stream state: $state")
            }
        }
        consumerIds.add(id)
    }

    private fun subscribeAllLocked() {
        unsubscribeAllLocked()
        lastSubscriptionAt = System.currentTimeMillis()
        subscribeSpeed()
        subscribeAvgSpeed()
        subscribePower()
        subscribeHeartRate()
        subscribeDistance()
        subscribeGrade()
        subscribeElevationGain()
        subscribeAvgPower()
        if (shareLocation) {
            subscribeLocation()
        }
        Log.i(TAG, "Started collecting metrics (${consumerIds.size} consumers)")
    }

    private fun unsubscribeAllLocked() {
        consumerIds.forEach { karooSystem.removeConsumer(it) }
        consumerIds.clear()
    }

    private fun startWatchdogLocked() {
        val thread = HandlerThread("metrics-watchdog").apply { start() }
        watchdogThread = thread
        watchdogHandler = Handler(thread.looper).also {
            it.postDelayed(watchdogRunnable, WATCHDOG_INTERVAL_MS)
        }
    }

    private fun stopWatchdogLocked() {
        watchdogHandler?.removeCallbacksAndMessages(null)
        watchdogThread?.quitSafely()
        watchdogThread = null
        watchdogHandler = null
    }

    private fun checkStreamHealth() {
        val now = System.currentTimeMillis()
        val shouldRefresh = synchronized(lock) {
            if (!started) {
                false
            } else {
                StreamHealthPolicy.shouldRefresh(
                    now = now,
                    lastSubscriptionAt = lastSubscriptionAt,
                    lastMetricDataAt = lastMetricDataAt,
                    initialDataTimeoutMs = INITIAL_DATA_TIMEOUT_MS,
                    staleDataTimeoutMs = STALE_DATA_TIMEOUT_MS,
                )
            }
        }
        if (shouldRefresh) {
            scheduleRefresh("No metric updates received recently")
        }
    }

    private fun markMetricDataReceived() {
        synchronized(lock) {
            lastMetricDataAt = System.currentTimeMillis()
            refreshAttempts = 0
        }
    }

    private fun handleStreamError(streamName: String, error: Any?) {
        Log.w(TAG, "$streamName stream error: $error")
        scheduleRefresh("$streamName stream error")
    }

    private fun scheduleRefresh(reason: String) {
        synchronized(lock) {
            if (!started || refreshPending) return
            refreshPending = true
        }
        val handler = watchdogHandler
        if (handler == null) {
            refreshSubscriptions(reason)
            return
        }
        handler.postDelayed(
            {
                refreshSubscriptions(reason)
            },
            RESUBSCRIBE_DELAY_MS,
        )
    }

    private fun refreshSubscriptions(reason: String) {
        var shouldReconnect = false
        synchronized(lock) {
            refreshPending = false
            if (!started) return
            refreshAttempts += 1
            shouldReconnect = refreshAttempts >= 2
            if (!shouldReconnect) {
                Log.w(TAG, "Refreshing metric subscriptions: $reason")
                subscribeAllLocked()
            }
        }
        if (shouldReconnect) {
            Log.w(TAG, "Escalating to Karoo reconnect after repeated refresh failures: $reason")
            onReconnectRequested(reason)
        }
    }
}

internal object StreamHealthPolicy {
    fun shouldRefresh(
        now: Long,
        lastSubscriptionAt: Long,
        lastMetricDataAt: Long,
        initialDataTimeoutMs: Long,
        staleDataTimeoutMs: Long,
    ): Boolean {
        if (lastSubscriptionAt <= 0L) return false
        if (lastMetricDataAt <= 0L) {
            return now - lastSubscriptionAt >= initialDataTimeoutMs
        }
        return now - lastMetricDataAt >= staleDataTimeoutMs
    }
}
