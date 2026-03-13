package com.zenpeartree.karoometricsoverlay

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MetricsSnapshotTest {

    @Test
    fun `toJson with all null values produces valid JSON with nulls`() {
        val snapshot = MetricsSnapshot(timestamp = 1000L)
        val json = JSONObject(snapshot.toJson())

        assertTrue(json.isNull("speed"))
        assertTrue(json.isNull("power"))
        assertTrue(json.isNull("hr"))
        assertTrue(json.isNull("dist"))
        assertTrue(json.isNull("grade"))
        assertTrue(json.isNull("avgPower"))
        assertEquals(1000L, json.getLong("ts"))
    }

    @Test
    fun `toJson with all values populated produces correct JSON`() {
        val snapshot = MetricsSnapshot(
            speed = 32.5,
            power = 245,
            heartRate = 152,
            distance = 42.3,
            grade = 5.7,
            avgPower = 210,
            timestamp = 2000L,
        )
        val json = JSONObject(snapshot.toJson())

        assertEquals(32.5, json.getDouble("speed"), 0.1)
        assertEquals(245, json.getInt("power"))
        assertEquals(152, json.getInt("hr"))
        assertEquals(42.3, json.getDouble("dist"), 0.1)
        assertEquals(5.7, json.getDouble("grade"), 0.1)
        assertEquals(210, json.getInt("avgPower"))
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
            power = 0,
            heartRate = 0,
            distance = 0.0,
            grade = 0.0,
            avgPower = 0,
            timestamp = 0L,
        )
        val json = JSONObject(snapshot.toJson())

        assertEquals(0.0, json.getDouble("speed"), 0.01)
        assertEquals(0, json.getInt("power"))
        assertEquals(0, json.getInt("hr"))
        assertEquals(0.0, json.getDouble("dist"), 0.01)
        assertEquals(0.0, json.getDouble("grade"), 0.01)
        assertEquals(0, json.getInt("avgPower"))
    }

    @Test
    fun `toJson with partial values`() {
        val snapshot = MetricsSnapshot(speed = 25.0, heartRate = 140, timestamp = 0L)
        val json = JSONObject(snapshot.toJson())

        assertEquals(25.0, json.getDouble("speed"), 0.1)
        assertTrue(json.isNull("power"))
        assertEquals(140, json.getInt("hr"))
        assertTrue(json.isNull("dist"))
        assertTrue(json.isNull("grade"))
        assertTrue(json.isNull("avgPower"))
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
            power = 2000,
            heartRate = 220,
            distance = 999.9,
            grade = 25.0,
            avgPower = 1500,
            timestamp = Long.MAX_VALUE,
        )
        val json = JSONObject(snapshot.toJson())

        assertEquals(99.9, json.getDouble("speed"), 0.1)
        assertEquals(2000, json.getInt("power"))
        assertEquals(220, json.getInt("hr"))
        assertEquals(999.9, json.getDouble("dist"), 0.1)
        assertEquals(25.0, json.getDouble("grade"), 0.1)
        assertEquals(1500, json.getInt("avgPower"))
    }

    @Test
    fun `default snapshot has all null metric values`() {
        val snapshot = MetricsSnapshot()
        assertEquals(null, snapshot.speed)
        assertEquals(null, snapshot.power)
        assertEquals(null, snapshot.heartRate)
        assertEquals(null, snapshot.distance)
        assertEquals(null, snapshot.grade)
        assertEquals(null, snapshot.avgPower)
    }

    @Test
    fun `copy preserves unchanged fields`() {
        val original = MetricsSnapshot(speed = 30.0, power = 200, heartRate = 150, timestamp = 100L)
        val updated = original.copy(power = 250, timestamp = 200L)

        assertEquals(30.0, updated.speed!!, 0.01)
        assertEquals(250, updated.power)
        assertEquals(150, updated.heartRate)
        assertEquals(200L, updated.timestamp)
    }
}
