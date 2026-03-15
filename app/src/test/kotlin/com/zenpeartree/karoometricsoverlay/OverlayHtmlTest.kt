package com.zenpeartree.karoometricsoverlay

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class OverlayHtmlTest {

    private val html: String by lazy {
        File("src/main/assets/overlay.html").readText()
    }

    @Test
    fun `overlay html exists and is not empty`() {
        assertTrue("overlay.html should not be empty", html.isNotEmpty())
    }

    @Test
    fun `overlay has transparent background for OBS compositing`() {
        assertTrue(
            "body should have transparent background",
            html.contains("background: transparent") || html.contains("background:transparent"),
        )
    }

    @Test
    fun `overlay contains all metric elements`() {
        val requiredIds = listOf("speed", "power", "hr", "dist", "grade", "avgPower")
        for (id in requiredIds) {
            assertTrue("Missing element with id='$id'", html.contains("id=\"$id\""))
        }
    }

    @Test
    fun `overlay contains zone bar elements`() {
        assertTrue("Missing power zone bar", html.contains("id=\"power-bar\""))
        assertTrue("Missing power zone label", html.contains("id=\"power-zone\""))
        assertTrue("Missing HR zone bar", html.contains("id=\"hr-bar\""))
        assertTrue("Missing HR zone label", html.contains("id=\"hr-zone\""))
    }

    @Test
    fun `overlay contains WebSocket connection code`() {
        assertTrue("Missing WebSocket constructor", html.contains("new WebSocket("))
    }

    @Test
    fun `overlay contains polling fallback`() {
        assertTrue("Missing fetch fallback", html.contains("fetch('/metrics')"))
    }

    @Test
    fun `overlay contains power zone definitions`() {
        assertTrue("Missing FTP config", html.contains("var FTP = 250;"))
        assertTrue("Missing power zones", html.contains("powerZones"))
        assertTrue("Missing Z1", html.contains("Z1"))
        assertTrue("Missing Z7", html.contains("Z7"))
    }

    @Test
    fun `overlay contains HR zone definitions`() {
        assertTrue("Missing MAX_HR config", html.contains("var MAX_HR = 187;"))
        assertTrue("Missing HR zones", html.contains("hrZones"))
    }

    @Test
    fun `overlay FTP and MAX_HR use replaceable default format`() {
        // These exact strings are what OverlayServer.loadOverlayHtml() replaces
        assertTrue("FTP default must be in replaceable format", html.contains("var FTP = 250;"))
        assertTrue("MAX_HR default must be in replaceable format", html.contains("var MAX_HR = 187;"))
    }

    @Test
    fun `overlay contains grade color classes`() {
        assertTrue("Missing grade-positive class", html.contains("grade-positive"))
        assertTrue("Missing grade-negative class", html.contains("grade-negative"))
        assertTrue("Missing grade-flat class", html.contains("grade-flat"))
    }

    @Test
    fun `overlay contains reconnection logic`() {
        assertTrue("Missing reconnect logic", html.contains("reconnectDelay"))
        assertTrue("Missing fail count tracking", html.contains("wsFailCount"))
    }

    @Test
    fun `overlay has correct metric units`() {
        assertTrue("Missing km/h unit", html.contains("km/h"))
        assertTrue("Missing W unit for power", html.contains("'W'"))
        assertTrue("Missing bpm unit", html.contains("bpm"))
        assertTrue("Missing km unit for distance", html.contains("'km'"))
        assertTrue("Missing % unit for grade", html.contains("'%'"))
    }

    @Test
    fun `overlay is self-contained with no external dependencies`() {
        // No CDN links or external script/css references
        assertTrue("Should not have external scripts", !html.contains("src=\"http"))
        assertTrue("Should not have external stylesheets", !html.contains("href=\"http"))
    }
}
