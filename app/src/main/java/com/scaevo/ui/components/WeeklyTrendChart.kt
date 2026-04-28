package com.scaevo.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.roundToInt

@Composable
fun WeeklyTrendChart(
    summaries: List<DailyDeviceSummary>, // 7 items, sorted by date ASC
    modifier: Modifier = Modifier
    ,
    onDayPressed: (DailyDeviceSummary) -> Unit = {},
    onDayReleased: () -> Unit = {},
    onDayClicked: (DailyDeviceSummary) -> Unit = {},
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

    Box(
        modifier = modifier
            .height(160.dp)
            .pointerInput(summaries) {
                detectTapGestures(
                    onPress = { down ->
                        val lastIndex = summaries.lastIndex
                        val index = if (lastIndex <= 0) 0
                        else ((down.x / size.width.toFloat()) * lastIndex)
                            .roundToInt()
                            .coerceIn(0, lastIndex)
                        onDayPressed(summaries[index])
                        tryAwaitRelease()
                        onDayReleased()
                    },
                    onTap = { tap ->
                        val lastIndex = summaries.lastIndex
                        val index = if (lastIndex <= 0) 0
                        else ((tap.x / size.width.toFloat()) * lastIndex)
                            .roundToInt()
                            .coerceIn(0, lastIndex)
                        onDayClicked(summaries[index])
                    }
                )
            }
    ) {
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
                    },
                    itemPlacer = remember {
                        HorizontalAxis.ItemPlacer.aligned(addExtremeLabelPadding = true)
                    }
                )
            ),
            modelProducer = modelProducer,
            modifier = Modifier.height(160.dp)
        )
    }
}
