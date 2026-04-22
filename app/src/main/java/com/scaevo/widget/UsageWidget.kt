package com.scaevo.widget

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.TextAlign
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.scaevo.ui.utils.formatDuration
import java.time.LocalDate
import java.time.ZoneId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UsageWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val summary = withContext(Dispatchers.IO) { queryLiveTodaySummary(context) }

        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(androidx.compose.ui.graphics.Color(0xFF0D1117)))
                    .padding(10.dp)
                    .clickable(actionStartActivity<com.scaevo.MainActivity>())
            ) {
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    horizontalAlignment = Alignment.Horizontal.Start
                ) {
                    Text(
                        text = if (summary.hasPermission) formatDuration(summary.totalMs) else "Grant usage access",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 34.sp,
                            color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFC9D1D9)),
                            textAlign = TextAlign.Start
                        ),
                        modifier = GlanceModifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    private data class WidgetSummary(
        val hasPermission: Boolean,
        val totalMs: Long,
    )

    private fun queryLiveTodaySummary(context: Context): WidgetSummary {
        val nowMs = System.currentTimeMillis()
        val startMs = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        if (!hasUsagePermission(context)) {
            return WidgetSummary(
                hasPermission = false,
                totalMs = 0L,
            )
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val packageManager = context.packageManager
        val events = usageStatsManager.queryEvents(startMs, nowMs)
        val event = UsageEvents.Event()
        val activeStart = mutableMapOf<String, Long>()
        val durations = mutableMapOf<String, Long>()
        var currentForegroundPackage: String? = null

        fun closeSession(packageName: String, stopMs: Long) {
            val start = activeStart.remove(packageName) ?: return
            val duration = stopMs - start
            if (duration > 0) durations[packageName] = (durations[packageName] ?: 0L) + duration
        }

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val packageName = event.packageName ?: continue
            if (packageName == context.packageName) continue

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED,
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    val current = currentForegroundPackage
                    if (current != null && current != packageName) {
                        closeSession(current, event.timeStamp)
                    }
                    if (!(currentForegroundPackage == packageName && activeStart.containsKey(packageName))) {
                        activeStart[packageName] = event.timeStamp
                    }
                    currentForegroundPackage = packageName
                }

                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED,
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    closeSession(packageName, event.timeStamp)
                    if (currentForegroundPackage == packageName) {
                        currentForegroundPackage = null
                    }
                }
            }
        }

        activeStart.forEach { (packageName, start) ->
            val duration = nowMs - start
            if (duration > 0) durations[packageName] = (durations[packageName] ?: 0L) + duration
        }

        val totalMs = durations.values.sum()

        return WidgetSummary(
            hasPermission = true,
            totalMs = totalMs,
        )
    }

    private fun hasUsagePermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}

class UsageWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UsageWidget()
}
