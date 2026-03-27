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
    private val subscribePower: Boolean,
    private val subscribeHeartRate: Boolean,
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
    private val lastStreamStates = mutableMapOf<String, String>()

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
            lastStreamStates.clear()
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
            lastStreamStates.clear()
            DiagnosticEvents.record(TAG, "Stopped collecting metrics")
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
                else -> logStreamState("Speed", state)
            }
        }
        consumerIds.add(id)
        DiagnosticEvents.record(TAG, "Subscribed to Speed")
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
                else -> logStreamState("Power", state)
            }
        }
        consumerIds.add(id)
        DiagnosticEvents.record(TAG, "Subscribed to Power")
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
                else -> logStreamState("Avg speed", state)
            }
        }
        consumerIds.add(id)
        DiagnosticEvents.record(TAG, "Subscribed to Avg speed")
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
                else -> logStreamState("HR", state)
            }
        }
        consumerIds.add(id)
        DiagnosticEvents.record(TAG, "Subscribed to HR")
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
                else -> logStreamState("Distance", state)
            }
        }
        consumerIds.add(id)
        DiagnosticEvents.record(TAG, "Subscribed to Distance")
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
                else -> logStreamState("Grade", state)
            }
        }
        consumerIds.add(id)
        DiagnosticEvents.record(TAG, "Subscribed to Grade")
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
                else -> logStreamState("Avg power", state)
            }
        }
        consumerIds.add(id)
        DiagnosticEvents.record(TAG, "Subscribed to Avg power")
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
                else -> logStreamState("Elevation gain", state)
            }
        }
        consumerIds.add(id)
        DiagnosticEvents.record(TAG, "Subscribed to Elevation gain")
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
                else -> logStreamState("Location", state)
            }
        }
        consumerIds.add(id)
        DiagnosticEvents.record(TAG, "Subscribed to Location")
    }

    private fun subscribeAllLocked() {
        unsubscribeAllLocked()
        lastSubscriptionAt = System.currentTimeMillis()
        subscribeSpeed()
        subscribeAvgSpeed()
        if (subscribePower) {
            subscribePower()
            subscribeAvgPower()
        }
        if (subscribeHeartRate) {
            subscribeHeartRate()
        }
        subscribeDistance()
        subscribeGrade()
        subscribeElevationGain()
        if (shareLocation) {
            subscribeLocation()
        }
        DiagnosticEvents.record(TAG, "Started collecting metrics (${consumerIds.size} consumers)")
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
            DiagnosticEvents.recordWarning(
                TAG,
                "Watchdog detected stale metrics: age=${now - lastMetricDataAt}ms subscriptionAge=${now - lastSubscriptionAt}ms",
            )
            scheduleRefresh("No metric updates received recently")
        }
    }

    private fun markMetricDataReceived() {
        synchronized(lock) {
            if (lastMetricDataAt == 0L) {
                DiagnosticEvents.record(TAG, "Received first live metric datapoint")
            }
            lastMetricDataAt = System.currentTimeMillis()
            refreshAttempts = 0
        }
    }

    private fun handleStreamError(streamName: String, error: Any?) {
        DiagnosticEvents.recordWarning(TAG, "$streamName stream error: $error")
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
                DiagnosticEvents.recordWarning(TAG, "Refreshing metric subscriptions: $reason")
                subscribeAllLocked()
            }
        }
        if (shouldReconnect) {
            DiagnosticEvents.recordWarning(TAG, "Escalating to Karoo reconnect after repeated refresh failures: $reason")
            onReconnectRequested(reason)
        }
    }

    private fun logStreamState(streamName: String, state: StreamState) {
        val description = state.javaClass.simpleName ?: state.toString()
        synchronized(lock) {
            if (lastStreamStates[streamName] == description) return
            lastStreamStates[streamName] = description
        }
        DiagnosticEvents.record(TAG, "$streamName stream state: $description")
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
