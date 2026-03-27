package com.zenpeartree.karoometricsoverlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.json.JSONObject
import org.junit.Test

class DiagnosticEventsTest {

    @Test
    fun `toJson returns recorded events`() {
        DiagnosticEvents.clear()
        DiagnosticEvents.record("TestTag", "first message")
        DiagnosticEvents.recordWarning("WarnTag", "second message")

        val json = JSONObject(DiagnosticEvents.toJson())
        val events = json.getJSONArray("events")

        assertEquals(2, events.length())
        assertEquals("TestTag", events.getJSONObject(0).getString("tag"))
        assertEquals("first message", events.getJSONObject(0).getString("message"))
        assertEquals("WarnTag", events.getJSONObject(1).getString("tag"))
        assertEquals("second message", events.getJSONObject(1).getString("message"))
        assertTrue(events.getJSONObject(0).has("time"))
    }
}
