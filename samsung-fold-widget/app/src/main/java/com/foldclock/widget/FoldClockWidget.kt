package com.foldclock.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Widths chosen to bracket the Fold's two postures: a narrow cover-screen
 * placement and a wide unfolded-screen placement. Glance re-renders the
 * closest matching size whenever the widget is resized or the device folds.
 */
private val CoverSize = DpSize(130.dp, 70.dp)
private val UnfoldedSize = DpSize(380.dp, 180.dp)

private val Forest = ColorProvider(androidx.compose.ui.graphics.Color(0xFF163126))
private val Gold = ColorProvider(androidx.compose.ui.graphics.Color(0xFFC8A96A))
private val Cream = ColorProvider(androidx.compose.ui.graphics.Color(0xFFF5EFE3))

class FoldClockWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(CoverSize, UnfoldedSize))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val size = LocalSize.current
            val isUnfolded = size.width >= 240.dp
            ClockContent(isUnfolded)
        }
    }
}

@Composable
private fun ClockContent(isUnfolded: Boolean) {
    val now = Date()
    val time = SimpleDateFormat("h:mm", Locale.getDefault()).format(now)
    val amPm = SimpleDateFormat("a", Locale.getDefault()).format(now)
    val date = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(now)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Forest)
            .cornerRadius(20.dp)
            .padding(if (isUnfolded) 20.dp else 10.dp),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(
            text = "$time $amPm",
            style = TextStyle(
                color = Cream,
                fontSize = if (isUnfolded) 48.sp else 22.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            ),
        )
        if (isUnfolded) {
            Text(
                text = date,
                style = TextStyle(
                    color = Gold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }
}
