package com.scaevo.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import com.patrykandpatrick.vico.compose.common.fill
import com.scaevo.ui.utils.formatDuration

@Composable
fun HourlyBarChart(
    hourlyMs: LongArray,  // 24-element array, index = hour
    modifier: Modifier = Modifier
) {
    if (hourlyMs.all { it == 0L }) {
        Box(modifier.height(120.dp), contentAlignment = Alignment.Center) {
            Text("No data for this day", style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    // Build Vico model: one column per hour
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(hourlyMs) {
        modelProducer.runTransaction {
            columnSeries {
                series(hourlyMs.map { it.toFloat() })
            }
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(
                columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                    rememberLineComponent(
                        fill = fill(primaryColor),
                        thickness = 8.dp,
                        shape = CorneredShape.rounded(topLeftPercent = 40, topRightPercent = 40)
                    )
                )
            ),
            startAxis = VerticalAxis.rememberStart(
                valueFormatter = { _, value, _ ->
                    if (value == 0.0) "0" else formatDuration(value.toLong())
                },
                itemPlacer = remember { VerticalAxis.ItemPlacer.count({ 4 }) }
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = { _, value, _ ->
                    val hour = value.toInt()
                    when {
                        hour == 0 -> "12am"
                        hour < 12 -> "${hour}am"
                        hour == 12 -> "12pm"
                        else -> "${hour - 12}pm"
                    }
                },
                itemPlacer = remember {
                    HorizontalAxis.ItemPlacer.aligned(addExtremeLabelPadding = true)
                }
            )
        ),
        modelProducer = modelProducer,
        modifier = modifier.height(160.dp)
    )
}
