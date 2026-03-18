package com.zenpeartree.karoometricsoverlay

import java.util.concurrent.atomic.AtomicReference
import java.util.Locale

data class MetricsSnapshot(
    val speed: Double? = null,
    val avgSpeed: Double? = null,
    val power: Int? = null,
    val heartRate: Int? = null,
    val distance: Double? = null,
    val grade: Double? = null,
    val elevationGain: Double? = null,
    val avgPower: Int? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
) {
    fun toJson(): String {
        val s = speed?.let { String.format(Locale.US, "%.1f", it) } ?: "null"
        val aspeed = avgSpeed?.let { String.format(Locale.US, "%.1f", it) } ?: "null"
        val p = power?.toString() ?: "null"
        val hr = heartRate?.toString() ?: "null"
        val d = distance?.let { String.format(Locale.US, "%.1f", it) } ?: "null"
        val g = grade?.let { String.format(Locale.US, "%.1f", it) } ?: "null"
        val eg = elevationGain?.let { String.format(Locale.US, "%.0f", it) } ?: "null"
        val ap = avgPower?.toString() ?: "null"
        val la = lat?.let { String.format(Locale.US, "%.6f", it) } ?: "null"
        val ln = lng?.let { String.format(Locale.US, "%.6f", it) } ?: "null"
        return """{"speed":$s,"avgSpeed":$aspeed,"power":$p,"hr":$hr,"dist":$d,"grade":$g,"elevGain":$eg,"avgPower":$ap,"lat":$la,"lng":$ln,"ts":$timestamp}"""
    }
}

object MetricsState {
    private val ref = AtomicReference(MetricsSnapshot())

    fun get(): MetricsSnapshot = ref.get()

    fun reset() {
        ref.set(MetricsSnapshot())
    }

    fun updateSpeed(value: Double) {
        ref.updateAndGet { it.copy(speed = value, timestamp = System.currentTimeMillis()) }
    }

    fun updateAvgSpeed(value: Double) {
        ref.updateAndGet { it.copy(avgSpeed = value, timestamp = System.currentTimeMillis()) }
    }

    fun updatePower(value: Int) {
        ref.updateAndGet { it.copy(power = value, timestamp = System.currentTimeMillis()) }
    }

    fun updateHeartRate(value: Int) {
        ref.updateAndGet { it.copy(heartRate = value, timestamp = System.currentTimeMillis()) }
    }

    fun updateDistance(value: Double) {
        ref.updateAndGet { it.copy(distance = value, timestamp = System.currentTimeMillis()) }
    }

    fun updateGrade(value: Double) {
        ref.updateAndGet { it.copy(grade = value, timestamp = System.currentTimeMillis()) }
    }

    fun updateElevationGain(value: Double) {
        ref.updateAndGet { it.copy(elevationGain = value, timestamp = System.currentTimeMillis()) }
    }

    fun updateAvgPower(value: Int) {
        ref.updateAndGet { it.copy(avgPower = value, timestamp = System.currentTimeMillis()) }
    }

    fun updateLocation(lat: Double, lng: Double) {
        ref.updateAndGet { it.copy(lat = lat, lng = lng, timestamp = System.currentTimeMillis()) }
    }
}
