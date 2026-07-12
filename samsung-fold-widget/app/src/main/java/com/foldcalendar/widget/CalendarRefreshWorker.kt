package com.foldcalendar.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Runs on a ~15 minute cadence (WorkManager's periodic-work floor) plus once
 * immediately after the widget is added or the user signs in. Silently
 * refreshes the Microsoft token and re-fetches the calendar; never prompts
 * the user interactively (that only happens from MainActivity).
 */
class CalendarRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val glanceIds = GlanceAppWidgetManager(applicationContext).getGlanceIds(FoldCalendarWidget::class.java)
        if (glanceIds.isEmpty()) return Result.success()

        val auth = MsalAuthManager.getInstance(applicationContext)
        val token = try {
            auth.acquireTokenSilent()
        } catch (e: Exception) {
            null
        }

        if (token == null) {
            glanceIds.forEach { id -> FoldCalendarWidget.updateState(applicationContext, id, signedIn = false) }
            return Result.success()
        }

        return try {
            val appointments = GraphCalendarClient.fetchUpcoming(token)
            glanceIds.forEach { id ->
                FoldCalendarWidget.updateState(applicationContext, id, signedIn = true, appointments = appointments)
            }
            Result.success()
        } catch (e: Exception) {
            glanceIds.forEach { id ->
                FoldCalendarWidget.updateState(applicationContext, id, signedIn = true, error = e.message)
            }
            Result.retry()
        }
    }
}
