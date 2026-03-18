package com.zenpeartree.karoometricsoverlay

import android.util.Log
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.OnStreamState
import io.hammerhead.karooext.models.StreamState

class MetricsCollector(
    private val karooSystem: KarooSystemService,
    private val shareLocation: Boolean,
) {

    companion object {
        private const val TAG = "MetricsCollector"
        private const val MS_TO_KMH = 3.6
        private const val M_TO_KM = 0.001
    }

    private val consumerIds = mutableListOf<String>()

    fun start() {
        subscribeSpeed()
        subscribePower()
        subscribeHeartRate()
        subscribeDistance()
        subscribeGrade()
        subscribeAvgPower()
        if (shareLocation) {
            subscribeLocation()
        }
        Log.i(TAG, "Started collecting metrics (${consumerIds.size} consumers)")
    }

    fun stop() {
        consumerIds.forEach { karooSystem.removeConsumer(it) }
        consumerIds.clear()
        Log.i(TAG, "Stopped collecting metrics")
    }

    private fun subscribeSpeed() {
        val id = karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.SPEED),
            onError = { Log.w(TAG, "Speed stream error: $it") },
        ) { event: OnStreamState ->
            when (val state = event.state) {
                is StreamState.Streaming -> {
                    val value = state.dataPoint.values[DataType.Field.SPEED]
                    if (value != null) {
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
            onError = { Log.w(TAG, "Power stream error: $it") },
        ) { event: OnStreamState ->
            when (val state = event.state) {
                is StreamState.Streaming -> {
                    val value = state.dataPoint.values[DataType.Field.POWER]
                    if (value != null) {
                        MetricsState.updatePower(value.toInt())
                    }
                }
                else -> Log.d(TAG, "Power stream state: $state")
            }
        }
        consumerIds.add(id)
    }

    private fun subscribeHeartRate() {
        val id = karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.HEART_RATE),
            onError = { Log.w(TAG, "HR stream error: $it") },
        ) { event: OnStreamState ->
            when (val state = event.state) {
                is StreamState.Streaming -> {
                    val value = state.dataPoint.values[DataType.Field.HEART_RATE]
                    if (value != null) {
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
            onError = { Log.w(TAG, "Distance stream error: $it") },
        ) { event: OnStreamState ->
            when (val state = event.state) {
                is StreamState.Streaming -> {
                    val value = state.dataPoint.values[DataType.Field.DISTANCE]
                    if (value != null) {
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
            onError = { Log.w(TAG, "Grade stream error: $it") },
        ) { event: OnStreamState ->
            when (val state = event.state) {
                is StreamState.Streaming -> {
                    val value = state.dataPoint.values[DataType.Field.ELEVATION_GRADE]
                    if (value != null) {
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
            onError = { Log.w(TAG, "Avg power stream error: $it") },
        ) { event: OnStreamState ->
            when (val state = event.state) {
                is StreamState.Streaming -> {
                    val value = state.dataPoint.values[DataType.Field.AVERAGE_POWER]
                    if (value != null) {
                        MetricsState.updateAvgPower(value.toInt())
                    }
                }
                else -> Log.d(TAG, "Avg power stream state: $state")
            }
        }
        consumerIds.add(id)
    }

    private fun subscribeLocation() {
        val id = karooSystem.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.LOCATION),
            onError = { Log.w(TAG, "Location stream error: $it") },
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
}
