package com.foldcalendar.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import java.time.format.DateTimeFormatter

val SignedInKey = booleanPreferencesKey("signed_in")
val AppointmentsJsonKey = stringPreferencesKey("appointments_json")
val LastErrorKey = stringPreferencesKey("last_error")

private val CoverSize = DpSize(140.dp, 90.dp)
private val UnfoldedSize = DpSize(420.dp, 260.dp)

private val Forest = ColorProvider(Color(0xCC163126)) // ~80% opaque, lets wallpaper show through
private val Gold = ColorProvider(Color(0xFFC8A96A))
private val Cream = ColorProvider(Color(0xFFF5EFE3))
private val Sage = ColorProvider(Color(0xFFB8C4B6))

private const val OUTLOOK_CALENDAR_URL = "https://outlook.office.com/calendar/view/week"

class FoldCalendarWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Responsive(setOf(CoverSize, UnfoldedSize))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val size = LocalSize.current
            val prefs = currentState<Preferences>()
            val signedIn = prefs[SignedInKey] ?: false
            val appointments = if (signedIn) {
                appointmentsFromJson(prefs[AppointmentsJsonKey] ?: "")
            } else {
                emptyList()
            }
            CalendarContent(isUnfolded = size.width >= 280.dp, signedIn = signedIn, appointments = appointments)
        }
    }

    companion object {
        suspend fun updateState(
            context: Context,
            glanceId: GlanceId,
            signedIn: Boolean,
            appointments: List<Appointment>? = null,
            error: String? = null,
        ) {
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[SignedInKey] = signedIn
                    if (appointments != null) this[AppointmentsJsonKey] = appointmentsToJson(appointments)
                    if (error != null) this[LastErrorKey] = error else remove(LastErrorKey)
                }
            }
            FoldCalendarWidget().update(context, glanceId)
        }
    }
}

@Composable
private fun CalendarContent(isUnfolded: Boolean, signedIn: Boolean, appointments: List<Appointment>) {
    val context = LocalContext.current
    val clickAction = if (signedIn) {
        actionStartActivity(Intent(Intent.ACTION_VIEW, Uri.parse(OUTLOOK_CALENDAR_URL)))
    } else {
        actionStartActivity(Intent(context, MainActivity::class.java))
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Forest)
            .cornerRadius(20.dp)
            .padding(if (isUnfolded) 16.dp else 10.dp)
            .clickable(clickAction),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
    ) {
        when {
            !signedIn -> Text(
                text = context.getString(R.string.widget_not_signed_in),
                style = TextStyle(color = Cream, fontSize = 14.sp, textAlign = TextAlign.Center),
            )
            appointments.isEmpty() -> Text(
                text = context.getString(R.string.widget_no_appointments),
                style = TextStyle(color = Sage, fontSize = 14.sp),
            )
            else -> {
                val visible = if (isUnfolded) appointments.take(5) else appointments.take(1)
                visible.forEach { appt -> AppointmentRow(appt, isUnfolded) }
            }
        }
    }
}

@Composable
private fun AppointmentRow(appointment: Appointment, isUnfolded: Boolean) {
    val time = appointment.start.format(DateTimeFormatter.ofPattern("h:mm a"))
    Column(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = time,
            style = TextStyle(color = Gold, fontSize = if (isUnfolded) 14.sp else 18.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
        )
        if (isUnfolded) {
            Text(
                text = appointment.location?.let { "${appointment.subject} · $it" } ?: appointment.subject,
                style = TextStyle(color = Cream, fontSize = 15.sp),
                maxLines = 2,
            )
        }
    }
}
