package com.foldcalendar.widget

import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Thin wrapper over the Microsoft Graph calendarView endpoint. Requests the
 * response already converted to the device's local time zone via the
 * "Prefer: outlook.timezone" header so no zone math is needed on the way out.
 */
object GraphCalendarClient {

    private const val LOOKAHEAD_DAYS = 7L
    private const val MAX_RESULTS = 8

    suspend fun fetchUpcoming(accessToken: String): List<Appointment> = withContext(Dispatchers.IO) {
        val now = LocalDateTime.now()
        val end = now.plusDays(LOOKAHEAD_DAYS)
        val iso = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        val url = URL(
            "https://graph.microsoft.com/v1.0/me/calendarView" +
                "?startDateTime=${now.format(iso)}" +
                "&endDateTime=${end.format(iso)}" +
                "&\$top=$MAX_RESULTS" +
                "&\$orderby=start/dateTime" +
                "&\$select=subject,start,location",
        )

        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.setRequestProperty("Prefer", "outlook.timezone=\"${TimeZone.getDefault().getID()}\"")
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000

        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream.bufferedReader().use { it.readText() }
            if (status !in 200..299) {
                throw GraphApiException("Graph request failed ($status): $body")
            }
            parseCalendarView(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseCalendarView(body: String): List<Appointment> {
        val root = JSONObject(body)
        val values = root.optJSONArray("value") ?: return emptyList()
        return (0 until values.length()).map { i ->
            val item = values.getJSONObject(i)
            val startObj = item.getJSONObject("start")
            val locationObj = item.optJSONObject("location")
            Appointment(
                subject = item.optString("subject", "(No subject)"),
                start = parseGraphDateTime(startObj.getString("dateTime")),
                location = locationObj?.optString("displayName")?.takeUnless { it.isBlank() },
            )
        }
    }
}

class GraphApiException(message: String) : Exception(message)
