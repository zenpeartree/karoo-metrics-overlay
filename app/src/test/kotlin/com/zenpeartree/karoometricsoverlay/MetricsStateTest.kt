package com.zenpeartree.karoometricsoverlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MetricsStateTest {

    @Before
    fun reset() {
        // Reset state by updating all fields to null via a fresh snapshot
        // MetricsState is a singleton, so we reset by overwriting each field
        MetricsState.updateSpeed(0.0)
        MetricsState.updatePower(0)
        MetricsState.updateHeartRate(0)
        MetricsState.updateDistance(0.0)
        MetricsState.updateGrade(0.0)
        MetricsState.updateAvgPower(0)
    }

    @Test
    fun `updateSpeed updates speed field`() {
        MetricsState.updateSpeed(32.5)
        assertEquals(32.5, MetricsState.get().speed!!, 0.01)
    }

    @Test
    fun `updatePower updates power field`() {
        MetricsState.updatePower(245)
        assertEquals(245, MetricsState.get().power)
    }

    @Test
    fun `updateHeartRate updates heartRate field`() {
        MetricsState.updateHeartRate(152)
        assertEquals(152, MetricsState.get().heartRate)
    }

    @Test
    fun `updateDistance updates distance field`() {
        MetricsState.updateDistance(42.3)
        assertEquals(42.3, MetricsState.get().distance!!, 0.01)
    }

    @Test
    fun `updateGrade updates grade field`() {
        MetricsState.updateGrade(-5.2)
        assertEquals(-5.2, MetricsState.get().grade!!, 0.01)
    }

    @Test
    fun `updateAvgPower updates avgPower field`() {
        MetricsState.updateAvgPower(210)
        assertEquals(210, MetricsState.get().avgPower)
    }

    @Test
    fun `updates preserve other fields`() {
        MetricsState.updateSpeed(30.0)
        MetricsState.updatePower(200)
        MetricsState.updateHeartRate(140)

        MetricsState.updatePower(250)

        val snapshot = MetricsState.get()
        assertEquals(30.0, snapshot.speed!!, 0.01)
        assertEquals(250, snapshot.power)
        assertEquals(140, snapshot.heartRate)
    }

    @Test
    fun `update sets timestamp`() {
        val before = System.currentTimeMillis()
        MetricsState.updateSpeed(25.0)
        val after = System.currentTimeMillis()

        val ts = MetricsState.get().timestamp
        assertTrue("Timestamp should be >= before", ts >= before)
        assertTrue("Timestamp should be <= after", ts <= after)
    }

    @Test
    fun `get returns consistent snapshot`() {
        MetricsState.updateSpeed(30.0)
        MetricsState.updatePower(200)
        MetricsState.updateHeartRate(150)
        MetricsState.updateDistance(10.0)
        MetricsState.updateGrade(3.5)
        MetricsState.updateAvgPower(190)

        val snapshot = MetricsState.get()
        assertNotNull(snapshot.speed)
        assertNotNull(snapshot.power)
        assertNotNull(snapshot.heartRate)
        assertNotNull(snapshot.distance)
        assertNotNull(snapshot.grade)
        assertNotNull(snapshot.avgPower)
    }

    @Test
    fun `concurrent updates do not lose data`() {
        val executor = Executors.newFixedThreadPool(6)
        val iterations = 1000
        val latch = CountDownLatch(6)

        executor.submit {
            repeat(iterations) { MetricsState.updateSpeed(it.toDouble()) }
            latch.countDown()
        }
        executor.submit {
            repeat(iterations) { MetricsState.updatePower(it) }
            latch.countDown()
        }
        executor.submit {
            repeat(iterations) { MetricsState.updateHeartRate(it) }
            latch.countDown()
        }
        executor.submit {
            repeat(iterations) { MetricsState.updateDistance(it.toDouble()) }
            latch.countDown()
        }
        executor.submit {
            repeat(iterations) { MetricsState.updateGrade(it.toDouble()) }
            latch.countDown()
        }
        executor.submit {
            repeat(iterations) { MetricsState.updateAvgPower(it) }
            latch.countDown()
        }

        assertTrue("Timed out waiting for threads", latch.await(10, TimeUnit.SECONDS))
        executor.shutdown()

        val snapshot = MetricsState.get()
        assertEquals(999.0, snapshot.speed!!, 0.01)
        assertEquals(999, snapshot.power)
        assertEquals(999, snapshot.heartRate)
        assertEquals(999.0, snapshot.distance!!, 0.01)
        assertEquals(999.0, snapshot.grade!!, 0.01)
        assertEquals(999, snapshot.avgPower)
    }

    @Test
    fun `toJson from state returns valid JSON`() {
        MetricsState.updateSpeed(25.5)
        MetricsState.updatePower(180)
        val json = MetricsState.get().toJson()

        assertTrue(json.contains("\"speed\":25.5"))
        assertTrue(json.contains("\"power\":180"))
    }
}
