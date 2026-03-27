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
    fun `overlay contains all primary pov metric elements`() {
        val requiredIds = listOf(
            "speed",
            "power",
            "cadence",
            "hr",
            "dist",
            "grade",
            "avgSpeed",
            "power-avg",
        )
        for (id in requiredIds) {
            assertTrue("Missing element with id='$id'", html.contains("id=\"$id\""))
        }
    }

    @Test
    fun `overlay retains zone elements for power and hr`() {
        assertTrue("Missing power zone bar", html.contains("id=\"power-bar\""))
        assertTrue("Missing power zone label", html.contains("id=\"power-zone\""))
        assertTrue("Missing HR zone bar", html.contains("id=\"hr-bar\""))
        assertTrue("Missing HR zone label", html.contains("id=\"hr-zone\""))
    }

    @Test
    fun `overlay polls metrics over http`() {
        assertTrue("Missing fetch polling", html.contains("fetch('/metrics'"))
        assertTrue("Missing polling interval", html.contains("setInterval(pollMetrics, 1000)"))
        assertTrue("Missing no-store fetch option", html.contains("cache: 'no-store'"))
    }

    @Test
    fun `overlay contains power and hr zone definitions`() {
        assertTrue("Missing FTP config", html.contains("var FTP = 250;"))
        assertTrue("Missing MAX_HR config", html.contains("var MAX_HR = 187;"))
        assertTrue("Missing power zones", html.contains("powerZones"))
        assertTrue("Missing HR zones", html.contains("hrZones"))
        assertTrue("Missing Z1", html.contains("Z1"))
        assertTrue("Missing Z7", html.contains("Z7"))
    }

    @Test
    fun `overlay FTP and feature flags use replaceable default format`() {
        assertTrue("FTP default must be in replaceable format", html.contains("var FTP = 250;"))
        assertTrue("MAX_HR default must be in replaceable format", html.contains("var MAX_HR = 187;"))
        assertTrue("SHOW_MAP default must be in replaceable format", html.contains("var SHOW_MAP = false;"))
        assertTrue("SHOW_POWER default must be in replaceable format", html.contains("var SHOW_POWER = true;"))
        assertTrue("SHOW_HR default must be in replaceable format", html.contains("var SHOW_HR = true;"))
        assertTrue("SHOW_CADENCE default must be in replaceable format", html.contains("var SHOW_CADENCE = true;"))
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
    fun `overlay includes pov layout scaffolding without imperial elevation callout`() {
        assertTrue("Should include hud wrapper", html.contains("class=\"hud\""))
        assertTrue("Should include terrain panel", html.contains("class=\"terrain-panel\""))
        assertTrue("Should include speed shell", html.contains("class=\"speed-shell\""))
        assertTrue("Should include secondary shell", html.contains("class=\"secondary-shell\""))
        assertTrue("Should label cadence", html.contains(">Cadence<"))
        assertTrue("Should not render mph label", !html.contains(">MPH<"))
        assertTrue("Should not include elevation gain feet id", !html.contains("elevGain-ft"))
    }

    @Test
    fun `overlay can remove optional power cadence and hr sections`() {
        assertTrue("Should include power block id", html.contains("id=\"power-card\""))
        assertTrue("Should include cadence block id", html.contains("id=\"cadence-card\""))
        assertTrue("Should include hr block id", html.contains("id=\"hr-card\""))
        assertTrue("Should remove power block when disabled", html.contains("if (!SHOW_POWER && els.powerCard && els.powerCard.parentNode)"))
        assertTrue("Should remove cadence block when disabled", html.contains("if (!SHOW_CADENCE && els.cadenceCard && els.cadenceCard.parentNode)"))
        assertTrue("Should remove hr block when disabled", html.contains("if (!SHOW_HR && els.hrCard && els.hrCard.parentNode)"))
    }

    @Test
    fun `overlay exposes polling status logic`() {
        assertTrue("Missing poll failure counter", html.contains("pollFailures"))
        assertTrue("Missing polling status text", html.contains("els.status.textContent = 'polling'"))
    }
}
