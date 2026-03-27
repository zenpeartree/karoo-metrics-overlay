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
        val requiredIds = listOf("speed", "power", "hr", "dist", "grade", "avgSpeed", "elevGain", "power-avg")
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
        assertTrue("SHOW_MAP default must be in replaceable format", html.contains("var SHOW_MAP = false;"))
        assertTrue("SHOW_POWER default must be in replaceable format", html.contains("var SHOW_POWER = true;"))
        assertTrue("SHOW_HR default must be in replaceable format", html.contains("var SHOW_HR = true;"))
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
        assertTrue("Missing W unit for power", html.contains(">W<"))
        assertTrue("Missing bpm unit", html.contains("bpm"))
        assertTrue("Missing km unit for distance", html.contains(">km<"))
        assertTrue("Missing % unit for grade", html.contains(">%<"))
    }

    @Test
    fun `overlay includes Leaflet for map rendering`() {
        assertTrue("Should include Leaflet CSS", html.contains("leaflet"))
        assertTrue("Should include map panel", html.contains("id=\"map-panel\""))
        assertTrue("Should include map element", html.contains("id=\"map\""))
        assertTrue("Should include map fallback element", html.contains("id=\"map-fallback\""))
    }

    @Test
    fun `overlay contains map initialization code`() {
        assertTrue("Should have initMap function", html.contains("function initMap"))
        assertTrue("Should have updateMap function", html.contains("function updateMap"))
        assertTrue("Should have rider marker", html.contains("riderMarker"))
        assertTrue("Should have route trail", html.contains("routeTrail"))
    }

    @Test
    fun `overlay treats zero coordinates as valid`() {
        assertTrue(
            "Map update should allow zero coordinates",
            html.contains("lat == null || lng == null"),
        )
    }

    @Test
    fun `overlay includes graceful map fallback handling`() {
        assertTrue("Should detect missing Leaflet", html.contains("typeof L === 'undefined'"))
        assertTrue("Should expose map unavailable handler", html.contains("function showMapUnavailable"))
        assertTrue("Should handle tile load failures", html.contains("tileerror"))
    }

    @Test
    fun `overlay caps route trail growth and reduces recenter jitter`() {
        assertTrue("Should cap trail points", html.contains("var MAX_TRAIL_POINTS = 250;"))
        assertTrue("Should trim old trail points", html.contains("trailPoints.shift()"))
        assertTrue("Should only recenter when needed", html.contains("function shouldRecenterMap"))
    }

    @Test
    fun `overlay includes production card layout and secondary ride stats`() {
        assertTrue("Should include metrics shell", html.contains("metrics-shell"))
        assertTrue("Should include metrics grid", html.contains("metrics-grid"))
        assertTrue("Should show avg speed subline", html.contains("Avg -- km/h"))
        assertTrue("Should show elevation gain subline", html.contains("Elev Gain -- m"))
        assertTrue("Should show avg power in the power tile", html.contains("Avg -- W"))
    }

    @Test
    fun `overlay keeps desktop layout only`() {
        assertTrue("Should keep the desktop metrics shell", html.contains(".metrics-shell"))
        assertTrue("Should use a six-column grid to support 2-over-3 layout", html.contains("grid-template-columns: repeat(6, 1fr);"))
        assertTrue("Should make top row tiles wider", html.contains(".metrics-grid > .metric-card:nth-child(-n+2)"))
        assertTrue("Should always render distance card", html.contains("<div class=\"metric-card distance\">"))
        assertTrue("Should shrink the metric shell width", html.contains("width: clamp(240px, 36vw, 351px);"))
        assertTrue("Should shrink the map width", html.contains("width: clamp(110px, 14vw, 160px);"))
    }

    @Test
    fun `overlay can remove optional power and hr cards`() {
        assertTrue("Should include power card id", html.contains("<div class=\"metric-card power\" id=\"power-card\">"))
        assertTrue("Should include hr card id", html.contains("<div class=\"metric-card hr\" id=\"hr-card\">"))
        assertTrue("Should remove power card when disabled", html.contains("if (!SHOW_POWER)"))
        assertTrue("Should remove hr card when disabled", html.contains("if (!SHOW_HR && els.hrCard && els.hrCard.parentNode)"))
    }

    @Test
    fun `overlay no longer includes mobile viewer overrides`() {
        assertTrue("Should not contain compact mode CSS", !html.contains("compact-mode"))
        assertTrue("Should not contain narrow mode CSS", !html.contains("narrow-mode"))
        assertTrue("Should not parse viewer query parameters", !html.contains("params.get('viewer')"))
        assertTrue("Should not define a mobile viewer mode", !html.contains("VIEWER_MODE_MOBILE"))
    }
}
