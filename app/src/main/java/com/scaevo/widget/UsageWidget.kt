package com.scaevo.widget

import android.content.Context
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
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.layout.Spacer
import androidx.glance.text.TextAlign
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.scaevo.data.usage.UsageStatsHelper
import com.scaevo.ui.utils.formatDuration
import java.time.LocalDate
import java.time.ZoneId
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class UsageWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val summary = try {
            withContext(Dispatchers.IO) { queryLiveTodaySummary(context) }
        } catch (e: Exception) {
            WidgetSummary(false, 0L, emptyList())
        }

        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xFF0D1117)))
                    .padding(8.dp)
                    .clickable(actionStartActivity<com.scaevo.MainActivity>())
            ) {
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                ) {
                    Text(
                        text = if (summary.hasPermission) formatDuration(summary.totalMs) else "Grant usage access",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            color = ColorProvider(Color(0xFFC9D1D9)),
                            textAlign = TextAlign.Center
                        ),
                        modifier = GlanceModifier.fillMaxWidth()
                    )

                    if (summary.hasPermission && summary.totalMs > 0) {
                        Spacer(modifier = GlanceModifier.height(8.dp))
                        Row(
                            modifier = GlanceModifier.fillMaxWidth().height(8.dp)
                        ) {
                            if (summary.segments.isEmpty()) {
                                Box(
                                    modifier = GlanceModifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .background(ColorProvider(Color(0xFF30363D)))
                                ) {}
                            } else {
                                summary.segments.forEachIndexed { index, segment ->
                                    Box(
                                        modifier = GlanceModifier
                                            .defaultWeight()
                                            .height(8.dp)
                                            .background(ColorProvider(segment.color))
                                    ) {}

                                    if (index < summary.segments.lastIndex) {
                                        Spacer(modifier = GlanceModifier.width(1.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private data class WidgetSegment(
        val color: Color,
        val weight: Float,
    )

    private data class WidgetSummary(
        val hasPermission: Boolean,
        val totalMs: Long,
        val segments: List<WidgetSegment>,
    )

    private fun queryLiveTodaySummary(context: Context): WidgetSummary {
        val nowMs = System.currentTimeMillis()
        val startMs = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val usageStatsHelper = UsageStatsHelper(context)

        if (!usageStatsHelper.hasUsagePermission()) {
            return WidgetSummary(
                hasPermission = false,
                totalMs = 0L,
                segments = emptyList(),
            )
        }

        val durations = usageStatsHelper.queryForegroundDurationsForRange(startMs, nowMs)
            .associate { it.packageName to it.totalForegroundMs }

        val totalMs = durations.values.sum()

        val segmentPalette = listOf(
            Color(0xFF00E5FF), // Cyan
            Color(0xFF39FF14), // Neon Green
            Color(0xFFFF2BD6), // Neon Pink
            Color(0xFFFFE600), // Yellow
        )
        
        val sortedDurations = durations.entries
            .sortedByDescending { it.value }
            .take(4)

        val segments = mutableListOf<WidgetSegment>()
        var accountedMs = 0L
        
        if (totalMs > 0L) {
            sortedDurations.forEachIndexed { index, entry ->
                segments.add(
                    WidgetSegment(
                        color = segmentPalette[index % segmentPalette.size],
                        weight = entry.value.toFloat()
                    )
                )
                accountedMs += entry.value
            }
            
            // Add "Other" segment if there's significant remaining time
            val otherMs = totalMs - accountedMs
            if (otherMs > totalMs * 0.05) { // Only if > 5%
                segments.add(
                    WidgetSegment(
                        color = Color(0xFF30363D), // Gray
                        weight = otherMs.toFloat()
                    )
                )
            }
        }

        return WidgetSummary(
            hasPermission = true,
            totalMs = totalMs,
            segments = segments,
        )
    }
}

class UsageWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UsageWidget()
}
