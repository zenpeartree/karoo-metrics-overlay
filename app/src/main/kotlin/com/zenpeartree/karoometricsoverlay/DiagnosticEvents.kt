package com.zenpeartree.karoometricsoverlay

import android.util.Log
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale

internal object DiagnosticEvents {
    private const val MAX_EVENTS = 200
    private val lock = Any()
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    private val events = ArrayDeque<DiagnosticEvent>(MAX_EVENTS)

    fun record(tag: String, message: String) {
        append(tag, message)
        runCatching { Log.i(tag, message) }
    }

    fun recordWarning(tag: String, message: String) {
        append(tag, message)
        runCatching { Log.w(tag, message) }
    }

    private fun append(tag: String, message: String) {
        val event = DiagnosticEvent(
            ts = System.currentTimeMillis(),
            tag = tag,
            message = message,
        )
        synchronized(lock) {
            if (events.size >= MAX_EVENTS) {
                events.removeFirst()
            }
            events.addLast(event)
        }
    }

    fun clear() {
        synchronized(lock) {
            events.clear()
        }
    }

    fun toJson(): String {
        val snapshot = synchronized(lock) { events.toList() }
        return buildString {
            append("{\"events\":[")
            snapshot.forEachIndexed { index, event ->
                if (index > 0) append(',')
                append("{\"ts\":")
                append(event.ts)
                append(",\"time\":\"")
                append(escape(timestampFormat.format(Date(event.ts))))
                append("\",\"tag\":\"")
                append(escape(event.tag))
                append("\",\"message\":\"")
                append(escape(event.message))
                append("\"}")
            }
            append("]}")
        }
    }

    private fun escape(value: String): String {
        return buildString {
            value.forEach { ch ->
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
        }
    }
}

internal data class DiagnosticEvent(
    val ts: Long,
    val tag: String,
    val message: String,
)
