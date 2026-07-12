package com.foldclock.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class FoldClockWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = FoldClockWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUpdateScheduler.start(context)
    }

    override fun onDisabled(context: Context) {
        WidgetUpdateScheduler.stop(context)
        super.onDisabled(context)
    }
}
