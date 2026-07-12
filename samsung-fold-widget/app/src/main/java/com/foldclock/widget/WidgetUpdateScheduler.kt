package com.foldclock.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * Glance widgets don't tick on their own, so we drive minute-level refreshes
 * with an inexact repeating alarm. Battery cost is negligible since the
 * update itself is a cheap RemoteViews re-render.
 */
object WidgetUpdateScheduler {

    private const val TICK_INTERVAL_MS = 60_000L

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, WidgetTickReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun start(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME,
            android.os.SystemClock.elapsedRealtime(),
            TICK_INTERVAL_MS,
            pendingIntent(context),
        )
    }

    fun stop(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context))
    }
}
