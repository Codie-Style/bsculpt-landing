package com.foldcalendar.widget

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.json.JSONArray
import org.json.JSONObject

data class Appointment(
    val subject: String,
    val start: LocalDateTime,
    val location: String?,
)

/**
 * Graph returns a variable number of fractional-second digits
 * ("2026-07-13T09:00:00.0000000"); only the first 19 characters
 * (down to whole seconds) are needed for display.
 */
fun parseGraphDateTime(raw: String): LocalDateTime {
    val trimmed = if (raw.length > 19) raw.substring(0, 19) else raw
    return LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
}

fun appointmentsToJson(appointments: List<Appointment>): String {
    val array = JSONArray()
    appointments.forEach { appt ->
        val obj = JSONObject()
        obj.put("subject", appt.subject)
        obj.put("start", appt.start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        if (appt.location != null) obj.put("location", appt.location)
        array.put(obj)
    }
    return array.toString()
}

fun appointmentsFromJson(json: String): List<Appointment> {
    if (json.isBlank()) return emptyList()
    val array = JSONArray(json)
    return (0 until array.length()).map { i ->
        val obj = array.getJSONObject(i)
        Appointment(
            subject = obj.getString("subject"),
            start = LocalDateTime.parse(obj.getString("start"), DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            location = obj.optString("location", "").takeUnless { it.isBlank() },
        )
    }
}
