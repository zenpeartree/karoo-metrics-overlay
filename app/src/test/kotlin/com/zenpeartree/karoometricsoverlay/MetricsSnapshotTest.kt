package com.zenpeartree.karoometricsoverlay

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class MetricsSnapshotTest {

    @Test
    fun `toJson with all null values produces valid JSON with nulls`() {
        val snapshot = MetricsSnapshot(timestamp = 1000L)
        val json = JSONObject(snapshot.toJson())

        assertTrue(json.isNull("speed"))
        assertTrue(json.isNull("avgSpeed"))
        assertTrue(json.isNull("power"))
        assertTrue(json.isNull("hr"))
        assertTrue(json.isNull("cadence"))
        assertTrue(json.isNull("dist"))
        assertTrue(json.isNull("grade"))
        assertTrue(json.isNull("elevGain"))
        assertTrue(json.isNull("avgPower"))
        assertTrue(json.isNull("lat"))
        assertTrue(json.isNull("lng"))
        assertEquals(1000L, json.getLong("ts"))
    }

    @Test
    fun `toJson with all values populated produces correct JSON`() {
        val snapshot = MetricsSnapshot(
            speed = 32.5,
            avgSpeed = 30.1,
            power = 245,
            heartRate = 152,
            cadence = 91,
            distance = 42.3,
            grade = 5.7,
            elevationGain = 812.0,
            avgPower = 210,
            lat = 38.7870,
            lng = -9.3900,
            timestamp = 2000L,
        )
        val json = JSONObject(snapshot.toJson())

        assertEquals(32.5, json.getDouble("speed"), 0.1)
        assertEquals(30.1, json.getDouble("avgSpeed"), 0.1)
        assertEquals(245, json.getInt("power"))
        assertEquals(152, json.getInt("hr"))
        assertEquals(91, json.getInt("cadence"))
        assertEquals(42.3, json.getDouble("dist"), 0.1)
        assertEquals(5.7, json.getDouble("grade"), 0.1)
        assertEquals(812.0, json.getDouble("elevGain"), 0.1)
        assertEquals(210, json.getInt("avgPower"))
        assertEquals(38.787, json.getDouble("lat"), 0.001)
        assertEquals(-9.39, json.getDouble("lng"), 0.001)
        assertEquals(2000L, json.getLong("ts"))
    }

    @Test
    fun `toJson formats speed to one decimal place`() {
        val snapshot = MetricsSnapshot(speed = 32.456, timestamp = 0L)
        val json = JSONObject(snapshot.toJson())
        assertEquals(32.5, json.getDouble("speed"), 0.01)
    }

    @Test
    fun `toJson formats distance to one decimal place`() {
        val snapshot = MetricsSnapshot(distance = 100.789, timestamp = 0L)
        val json = JSONObject(snapshot.toJson())
        assertEquals(100.8, json.getDouble("dist"), 0.01)
    }

    @Test
    fun `toJson formats grade to one decimal place`() {
        val snapshot = MetricsSnapshot(grade = -3.456, timestamp = 0L)
        val json = JSONObject(snapshot.toJson())
        assertEquals(-3.5, json.getDouble("grade"), 0.01)
    }

    @Test
    fun `toJson with zero values`() {
        val snapshot = MetricsSnapshot(
            speed = 0.0,
            avgSpeed = 0.0,
            power = 0,
            heartRate = 0,
            cadence = 0,
            distance = 0.0,
            grade = 0.0,
            elevationGain = 0.0,
            avgPower = 0,
            timestamp = 0L,
        )
        val json = JSONObject(snapshot.toJson())

        assertEquals(0.0, json.getDouble("speed"), 0.01)
        assertEquals(0.0, json.getDouble("avgSpeed"), 0.01)
        assertEquals(0, json.getInt("power"))
        assertEquals(0, json.getInt("hr"))
        assertEquals(0, json.getInt("cadence"))
        assertEquals(0.0, json.getDouble("dist"), 0.01)
        assertEquals(0.0, json.getDouble("grade"), 0.01)
        assertEquals(0.0, json.getDouble("elevGain"), 0.01)
        assertEquals(0, json.getInt("avgPower"))
    }

    @Test
    fun `toJson with partial values`() {
        val snapshot = MetricsSnapshot(speed = 25.0, heartRate = 140, cadence = 88, timestamp = 0L)
        val json = JSONObject(snapshot.toJson())

        assertEquals(25.0, json.getDouble("speed"), 0.1)
        assertTrue(json.isNull("avgSpeed"))
        assertTrue(json.isNull("power"))
        assertEquals(140, json.getInt("hr"))
        assertEquals(88, json.getInt("cadence"))
        assertTrue(json.isNull("dist"))
        assertTrue(json.isNull("grade"))
        assertTrue(json.isNull("elevGain"))
        assertTrue(json.isNull("avgPower"))
        assertTrue(json.isNull("lat"))
        assertTrue(json.isNull("lng"))
    }

    @Test
    fun `toJson formats lat lng to six decimal places`() {
        val snapshot = MetricsSnapshot(lat = 38.787012345, lng = -9.390067890, timestamp = 0L)
        val json = JSONObject(snapshot.toJson())
        assertEquals(38.787012, json.getDouble("lat"), 0.000001)
        assertEquals(-9.390068, json.getDouble("lng"), 0.000001)
    }

    @Test
    fun `toJson with negative grade`() {
        val snapshot = MetricsSnapshot(grade = -8.2, timestamp = 0L)
        val json = JSONObject(snapshot.toJson())
        assertEquals(-8.2, json.getDouble("grade"), 0.1)
    }

    @Test
    fun `toJson with large values`() {
        val snapshot = MetricsSnapshot(
            speed = 99.9,
            avgSpeed = 37.4,
            power = 2000,
            heartRate = 220,
            cadence = 150,
            distance = 999.9,
            grade = 25.0,
            elevationGain = 5432.0,
            avgPower = 1500,
            timestamp = Long.MAX_VALUE,
        )
        val json = JSONObject(snapshot.toJson())

        assertEquals(99.9, json.getDouble("speed"), 0.1)
        assertEquals(37.4, json.getDouble("avgSpeed"), 0.1)
        assertEquals(2000, json.getInt("power"))
        assertEquals(220, json.getInt("hr"))
        assertEquals(150, json.getInt("cadence"))
        assertEquals(999.9, json.getDouble("dist"), 0.1)
        assertEquals(25.0, json.getDouble("grade"), 0.1)
        assertEquals(5432.0, json.getDouble("elevGain"), 0.1)
        assertEquals(1500, json.getInt("avgPower"))
    }

    @Test
    fun `default snapshot has all null metric values`() {
        val snapshot = MetricsSnapshot()
        assertEquals(null, snapshot.speed)
        assertEquals(null, snapshot.avgSpeed)
        assertEquals(null, snapshot.power)
        assertEquals(null, snapshot.heartRate)
        assertEquals(null, snapshot.cadence)
        assertEquals(null, snapshot.distance)
        assertEquals(null, snapshot.grade)
        assertEquals(null, snapshot.elevationGain)
        assertEquals(null, snapshot.avgPower)
        assertEquals(null, snapshot.lat)
        assertEquals(null, snapshot.lng)
    }

    @Test
    fun `copy preserves unchanged fields`() {
        val original = MetricsSnapshot(speed = 30.0, avgSpeed = 28.0, power = 200, heartRate = 150, cadence = 92, elevationGain = 200.0, timestamp = 100L)
        val updated = original.copy(power = 250, timestamp = 200L)

        assertEquals(30.0, updated.speed!!, 0.01)
        assertEquals(28.0, updated.avgSpeed!!, 0.01)
        assertEquals(250, updated.power)
        assertEquals(150, updated.heartRate)
        assertEquals(92, updated.cadence)
        assertEquals(200.0, updated.elevationGain!!, 0.01)
        assertEquals(200L, updated.timestamp)
    }

    @Test
    fun `toJson uses locale independent decimal formatting`() {
        val originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.GERMANY)
        try {
            val json = JSONObject(MetricsSnapshot(speed = 32.5, grade = 4.2, timestamp = 0L).toJson())
            assertEquals(32.5, json.getDouble("speed"), 0.01)
            assertEquals(4.2, json.getDouble("grade"), 0.01)
        } finally {
            Locale.setDefault(originalLocale)
        }
    }
}
