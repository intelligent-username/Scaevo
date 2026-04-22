package com.scaevo.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.scaevo.data.db.entity.DailyDeviceSummary
import com.scaevo.ui.utils.formatDuration
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun WeeklyTrendChart(
    summaries: List<DailyDeviceSummary>, // 7 items, sorted by date ASC
    modifier: Modifier = Modifier
) {
    if (summaries.isEmpty()) return

    val modelProducer = remember { CartesianChartModelProducer() }
    val dayLabels = summaries.map { summary ->
        LocalDate.ofEpochDay(summary.dateEpochDay)
            .dayOfWeek
            .getDisplayName(TextStyle.SHORT, Locale.getDefault())
    }

    LaunchedEffect(summaries) {
        modelProducer.runTransaction {
            lineSeries {
                series(summaries.map { it.totalScreenTimeMs.toFloat() })
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(
                valueFormatter = { _, value, _ -> formatDuration(value.toLong()) },
                itemPlacer = remember { VerticalAxis.ItemPlacer.count({ 4 }) }
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = { _, value, _ ->
                    dayLabels.getOrElse(value.toInt()) { "" }
                }
            )
        ),
        modelProducer = modelProducer,
        modifier = modifier.height(160.dp)
    )
}
