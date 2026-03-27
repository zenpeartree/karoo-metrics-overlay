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
        MetricsState.reset()
    }

    @Test
    fun `updateSpeed updates speed field`() {
        MetricsState.updateSpeed(32.5)
        assertEquals(32.5, MetricsState.get().speed!!, 0.01)
    }

    @Test
    fun `updateAvgSpeed updates avgSpeed field`() {
        MetricsState.updateAvgSpeed(29.4)
        assertEquals(29.4, MetricsState.get().avgSpeed!!, 0.01)
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
    fun `updateCadence updates cadence field`() {
        MetricsState.updateCadence(91)
        assertEquals(91, MetricsState.get().cadence)
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
    fun `updateElevationGain updates elevation gain field`() {
        MetricsState.updateElevationGain(812.0)
        assertEquals(812.0, MetricsState.get().elevationGain!!, 0.01)
    }

    @Test
    fun `updateAvgPower updates avgPower field`() {
        MetricsState.updateAvgPower(210)
        assertEquals(210, MetricsState.get().avgPower)
    }

    @Test
    fun `updateLocation updates lat and lng fields`() {
        MetricsState.updateLocation(38.787, -9.39)
        val snapshot = MetricsState.get()
        assertEquals(38.787, snapshot.lat!!, 0.001)
        assertEquals(-9.39, snapshot.lng!!, 0.001)
    }

    @Test
    fun `updates preserve other fields`() {
        MetricsState.updateSpeed(30.0)
        MetricsState.updateAvgSpeed(28.0)
        MetricsState.updatePower(200)
        MetricsState.updateHeartRate(140)
        MetricsState.updateCadence(88)

        MetricsState.updatePower(250)

        val snapshot = MetricsState.get()
        assertEquals(30.0, snapshot.speed!!, 0.01)
        assertEquals(28.0, snapshot.avgSpeed!!, 0.01)
        assertEquals(250, snapshot.power)
        assertEquals(140, snapshot.heartRate)
        assertEquals(88, snapshot.cadence)
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
        MetricsState.updateAvgSpeed(28.0)
        MetricsState.updatePower(200)
        MetricsState.updateHeartRate(150)
        MetricsState.updateCadence(89)
        MetricsState.updateDistance(10.0)
        MetricsState.updateGrade(3.5)
        MetricsState.updateElevationGain(812.0)
        MetricsState.updateAvgPower(190)

        val snapshot = MetricsState.get()
        assertNotNull(snapshot.speed)
        assertNotNull(snapshot.avgSpeed)
        assertNotNull(snapshot.power)
        assertNotNull(snapshot.heartRate)
        assertNotNull(snapshot.cadence)
        assertNotNull(snapshot.distance)
        assertNotNull(snapshot.grade)
        assertNotNull(snapshot.elevationGain)
        assertNotNull(snapshot.avgPower)
    }

    @Test
    fun `reset clears all metric fields`() {
        MetricsState.updateSpeed(25.0)
        MetricsState.updatePower(180)
        MetricsState.updateAvgSpeed(24.0)
        MetricsState.updateCadence(86)
        MetricsState.updateElevationGain(300.0)
        MetricsState.updateLocation(38.787, -9.39)

        MetricsState.reset()

        val snapshot = MetricsState.get()
        assertNull(snapshot.speed)
        assertNull(snapshot.avgSpeed)
        assertNull(snapshot.power)
        assertNull(snapshot.heartRate)
        assertNull(snapshot.cadence)
        assertNull(snapshot.distance)
        assertNull(snapshot.grade)
        assertNull(snapshot.elevationGain)
        assertNull(snapshot.avgPower)
        assertNull(snapshot.lat)
        assertNull(snapshot.lng)
    }

    @Test
    fun `concurrent updates do not lose data`() {
        val executor = Executors.newFixedThreadPool(9)
        val iterations = 1000
        val latch = CountDownLatch(9)

        executor.submit {
            repeat(iterations) { MetricsState.updateSpeed(it.toDouble()) }
            latch.countDown()
        }
        executor.submit {
            repeat(iterations) { MetricsState.updateAvgSpeed(it.toDouble()) }
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
            repeat(iterations) { MetricsState.updateCadence(it) }
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
            repeat(iterations) { MetricsState.updateElevationGain(it.toDouble()) }
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
        assertEquals(999.0, snapshot.avgSpeed!!, 0.01)
        assertEquals(999, snapshot.power)
        assertEquals(999, snapshot.heartRate)
        assertEquals(999, snapshot.cadence)
        assertEquals(999.0, snapshot.distance!!, 0.01)
        assertEquals(999.0, snapshot.grade!!, 0.01)
        assertEquals(999.0, snapshot.elevationGain!!, 0.01)
        assertEquals(999, snapshot.avgPower)
    }

    @Test
    fun `toJson from state returns valid JSON`() {
        MetricsState.updateSpeed(25.5)
        MetricsState.updateAvgSpeed(24.9)
        MetricsState.updatePower(180)
        MetricsState.updateCadence(87)
        MetricsState.updateElevationGain(345.0)
        val json = MetricsState.get().toJson()

        assertTrue(json.contains("\"speed\":25.5"))
        assertTrue(json.contains("\"avgSpeed\":24.9"))
        assertTrue(json.contains("\"power\":180"))
        assertTrue(json.contains("\"cadence\":87"))
        assertTrue(json.contains("\"elevGain\":345"))
    }
}
