package com.zenpeartree.karoometricsoverlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsTest {

    @Test
    fun `default FTP is a reasonable value`() {
        assertTrue("Default FTP should be > 0", MainActivity.DEFAULT_FTP > 0)
        assertTrue("Default FTP should be < 500", MainActivity.DEFAULT_FTP < 500)
    }

    @Test
    fun `default max HR is a reasonable value`() {
        assertTrue("Default max HR should be > 100", MainActivity.DEFAULT_MAX_HR > 100)
        assertTrue("Default max HR should be < 250", MainActivity.DEFAULT_MAX_HR < 250)
    }

    @Test
    fun `settings keys are unique`() {
        assertTrue(
            "FTP and MAX_HR keys must be different",
            MainActivity.KEY_FTP != MainActivity.KEY_MAX_HR,
        )
        assertTrue(
            "Location sharing key must be different from FTP key",
            MainActivity.KEY_SHARE_LOCATION != MainActivity.KEY_FTP,
        )
        assertTrue(
            "Location sharing key must be different from MAX_HR key",
            MainActivity.KEY_SHARE_LOCATION != MainActivity.KEY_MAX_HR,
        )
        assertTrue(
            "Power subscription key must be different from location sharing key",
            MainActivity.KEY_SUBSCRIBE_POWER != MainActivity.KEY_SHARE_LOCATION,
        )
        assertTrue(
            "HR subscription key must be different from location sharing key",
            MainActivity.KEY_SUBSCRIBE_HR != MainActivity.KEY_SHARE_LOCATION,
        )
        assertTrue(
            "Power and HR subscription keys must be different",
            MainActivity.KEY_SUBSCRIBE_POWER != MainActivity.KEY_SUBSCRIBE_HR,
        )
        assertTrue(
            "Cadence subscription key must be different from power key",
            MainActivity.KEY_SUBSCRIBE_CADENCE != MainActivity.KEY_SUBSCRIBE_POWER,
        )
        assertTrue(
            "Cadence subscription key must be different from HR key",
            MainActivity.KEY_SUBSCRIBE_CADENCE != MainActivity.KEY_SUBSCRIBE_HR,
        )
    }

    @Test
    fun `location sharing defaults to disabled`() {
        assertEquals(false, MainActivity.DEFAULT_SHARE_LOCATION)
    }

    @Test
    fun `power hr and cadence subscriptions default to enabled`() {
        assertEquals(true, MainActivity.DEFAULT_SUBSCRIBE_POWER)
        assertEquals(true, MainActivity.DEFAULT_SUBSCRIBE_HR)
        assertEquals(true, MainActivity.DEFAULT_SUBSCRIBE_CADENCE)
    }

    @Test
    fun `overlay html default FTP matches settings default`() {
        val html = java.io.File("src/main/assets/overlay.html").readText()
        assertTrue(
            "overlay.html default FTP (250) should be present for replacement",
            html.contains("var FTP = 250;"),
        )
    }

    @Test
    fun `overlay html default MAX_HR matches settings default`() {
        val html = java.io.File("src/main/assets/overlay.html").readText()
        assertTrue(
            "overlay.html default MAX_HR (187) should be present for replacement",
            html.contains("var MAX_HR = 187;"),
        )
    }

    @Test
    fun `overlay html default SHOW_MAP matches location sharing default`() {
        val html = java.io.File("src/main/assets/overlay.html").readText()
        assertTrue(
            "overlay.html default SHOW_MAP should be present for replacement",
            html.contains("var SHOW_MAP = false;"),
        )
    }

    @Test
    fun `overlay html defaults for optional metric tiles are present for replacement`() {
        val html = java.io.File("src/main/assets/overlay.html").readText()
        assertTrue(
            "overlay.html default SHOW_POWER should be present for replacement",
            html.contains("var SHOW_POWER = true;"),
        )
        assertTrue(
            "overlay.html default SHOW_HR should be present for replacement",
            html.contains("var SHOW_HR = true;"),
        )
        assertTrue(
            "overlay.html default SHOW_CADENCE should be present for replacement",
            html.contains("var SHOW_CADENCE = true;"),
        )
    }
}
