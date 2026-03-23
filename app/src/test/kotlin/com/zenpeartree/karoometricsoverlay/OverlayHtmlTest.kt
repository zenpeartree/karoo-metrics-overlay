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
        val requiredIds = listOf("speed", "power", "hr", "dist", "grade", "avgPower", "avgSpeed", "elevGain")
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
    }

    @Test
    fun `overlay includes compact mode for explicit mobile viewer mode`() {
        assertTrue("Should define narrow mode breakpoints", html.contains("var NARROW_MAX_WIDTH = 840;"))
        assertTrue("Should define narrow mode breakpoints", html.contains("var NARROW_MAX_HEIGHT = 620;"))
        assertTrue("Should define compact mode breakpoints", html.contains("var COMPACT_MAX_WIDTH = 420;"))
        assertTrue("Should define compact mode breakpoints", html.contains("var COMPACT_MAX_HEIGHT = 760;"))
        assertTrue("Should update responsive mode on resize", html.contains("window.addEventListener('resize', updateResponsiveMode)"))
        assertTrue("Should mark lower-priority cards as compact hidden", html.contains("compact-hidden"))
        assertTrue("Should hide map in compact mode", html.contains("body.compact-mode .map-panel"))
        assertTrue("Should support narrow mode class", html.contains("body.narrow-mode .overlay"))
        assertTrue("Should resize map after mode changes", html.contains("map.invalidateSize()"))
    }

    @Test
    fun `overlay supports viewer driven layout overrides`() {
        assertTrue("Should define desktop viewer mode", html.contains("var VIEWER_MODE_DESKTOP = 'desktop';"))
        assertTrue("Should define mobile viewer mode", html.contains("var VIEWER_MODE_MOBILE = 'mobile';"))
        assertTrue("Should parse viewer query parameter", html.contains("params.get('viewer')"))
        assertTrue("Should support audience alias", html.contains("params.get('audience')"))
        assertTrue("Should default to desktop layout", html.contains("return VIEWER_MODE_DESKTOP;"))
        assertTrue("Should force desktop layout when requested", html.contains("if (viewerMode === VIEWER_MODE_DESKTOP)"))
        assertTrue("Should force mobile layout when requested", html.contains("} else if (viewerMode === VIEWER_MODE_MOBILE)"))
    }
}
