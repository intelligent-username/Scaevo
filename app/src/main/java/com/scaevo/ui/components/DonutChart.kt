package com.scaevo.ui.components

import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scaevo.data.db.entity.DailyUsageStat
import com.scaevo.ui.utils.formatDuration

private val CHART_COLORS = listOf(
    Color(0xFF58A6FF),
    Color(0xFF3FB950),
    Color(0xFFF2CC60),
    Color(0xFFBC8CFF),
    Color(0xFF7EE787),
)
private val OTHERS_COLOR = Color(0xFF8B949E)

data class DonutSlice(val label: String, val value: Long, val color: Color)

@Composable
fun DonutChart(
    stats: List<DailyUsageStat>,
    modifier: Modifier = Modifier,
    maxSlices: Int = 5,
    strokeWidthDp: Float = 36f
) {
    if (stats.isEmpty()) return

    val totalMs = stats.sumOf { it.totalForegroundMs }
    val sorted = stats.sortedByDescending { it.totalForegroundMs }
    val topSlices = sorted.take(maxSlices).mapIndexed { i, stat ->
        DonutSlice(
            label = stat.appLabel,
            value = stat.totalForegroundMs,
            color = CHART_COLORS[i % CHART_COLORS.size]
        )
    }
    val othersMs = sorted.drop(maxSlices).sumOf { it.totalForegroundMs }
    val slices = if (othersMs > 0) topSlices + DonutSlice("Others", othersMs, OTHERS_COLOR)
    else topSlices

    val totalLabel = formatDuration(totalMs)
    val chartSize = 200.dp

    val density = LocalDensity.current
    val strokeWidthPx = with(density) { strokeWidthDp.dp.toPx() }
    val chartSizePx = with(density) { chartSize.toPx() }
    val diameter = chartSizePx - strokeWidthPx
    val centerX = chartSizePx / 2f
    val centerY = chartSizePx / 2f
    // Place labels in the radial middle of the donut ring.
    val labelRadius = diameter / 2f
    val maxLabelFontPx = with(density) { 12.sp.toPx() }
    val minLabelFontPx = with(density) { 8.sp.toPx() }

    val labelPaint = Paint().apply {
        isAntiAlias = true
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    data class FittedLabel(val text: String, val fontPx: Float)

    fun fitLabel(text: String, maxWidthPx: Float): FittedLabel? {
        if (maxWidthPx <= 0f) return null

        var fontPx = maxLabelFontPx
        while (fontPx >= minLabelFontPx) {
            labelPaint.textSize = fontPx
            if (labelPaint.measureText(text) <= maxWidthPx) {
                return FittedLabel(text, fontPx)
            }
            fontPx -= 1f
        }

        labelPaint.textSize = minLabelFontPx
        var end = text.length
        while (end > 2) {
            val candidate = text.take(end) + "…"
            if (labelPaint.measureText(candidate) <= maxWidthPx) {
                return FittedLabel(candidate, minLabelFontPx)
            }
            end--
        }

        return null
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(chartSize)
        ) {
            // Canvas for drawing arcs
            Canvas(modifier = Modifier.matchParentSize()) {
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                val arcSize = Size(diameter, diameter)
                var startAngle = -90f
                val gapDegrees = 2f

                slices.forEach { slice ->
                    val sweep = (slice.value.toFloat() / totalMs.toFloat()) * (360f - gapDegrees * slices.size)
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Butt)
                    )
                    startAngle += sweep + gapDegrees
                }
            }

            // Overlay layer for labels
            Box(modifier = Modifier.matchParentSize()) {
                var startAngle = -90f
                val gapDegrees = 2f
                val placedBounds = mutableListOf<RectF>()

                slices.forEach { slice ->
                    val sweep = (slice.value.toFloat() / totalMs.toFloat()) * (360f - gapDegrees * slices.size)

                    if (sweep >= 10f) {
                        val midAngle = startAngle + sweep / 2f
                        val rad = Math.toRadians(midAngle.toDouble())
                        val xPx = centerX + (labelRadius * kotlin.math.cos(rad)).toFloat()
                        val yPx = centerY + (labelRadius * kotlin.math.sin(rad)).toFloat()
                        val theta = Math.toRadians(sweep.toDouble())
                        val chordWidthPx = (2.0 * labelRadius * kotlin.math.sin(theta / 2.0)).toFloat()
                        val arcLengthPx = (labelRadius * theta).toFloat()
                        val maxWidthPx = (kotlin.math.min(chordWidthPx, arcLengthPx) * 0.9f).coerceAtLeast(20f)

                        val fitted = fitLabel(slice.label, maxWidthPx)
                        if (fitted != null) {
                            val fontSp = with(density) { fitted.fontPx.toSp() }
                            val boxLeft = xPx - maxWidthPx / 2f
                            val boxTop = yPx - fitted.fontPx * 0.65f
                            val boxBottom = yPx + fitted.fontPx * 0.35f
                            val currentBounds = RectF(boxLeft, boxTop, boxLeft + maxWidthPx, boxBottom)

                            val overlapsExisting = placedBounds.any { RectF.intersects(it, currentBounds) }
                            if (!overlapsExisting) {
                                placedBounds.add(currentBounds)
                                Text(
                                    text = fitted.text,
                                    fontSize = fontSp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .offset(
                                            x = with(density) { boxLeft.toDp() },
                                            y = with(density) { boxTop.toDp() }
                                        )
                                        .width(with(density) { maxWidthPx.toDp() })
                                )
                            }
                        }
                    }

                    startAngle += sweep + gapDegrees
                }
            }

            Text(
                text = totalLabel,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
