package com.scaevo.ui.components

import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import com.scaevo.data.db.entity.DailyUsageStat
import com.scaevo.ui.utils.formatDuration
import kotlin.math.atan2
import kotlin.math.sqrt

private val CHART_COLORS = listOf(
    Color(0xFF00E5FF),
    Color(0xFF39FF14),
    Color(0xFFFF2BD6),
    Color(0xFFFFE600),
    Color(0xFFFF6B00),
    Color(0xFF8A2BFF),
)
private val OTHERS_COLOR = Color(0xFF6E7681)

data class DonutSlice(val label: String, val value: Long, val color: Color)
private data class SliceAngle(val startAngle: Float, val sweepAngle: Float, val slice: DonutSlice)

@Composable
fun DonutChart(
    stats: List<DailyUsageStat>,
    modifier: Modifier = Modifier,
    maxSlices: Int = 5,
    strokeWidthDp: Float = 42f,
    chartSizeDp: Dp = 280.dp
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
    val gapDegrees = 2f
    val sliceAngles = remember(slices, totalMs) {
        buildList {
            var startAngle = -90f
            val maxSweep = 360f - gapDegrees * slices.size
            slices.forEach { slice ->
                val sweep = (slice.value.toFloat() / totalMs.toFloat()) * maxSweep
                add(SliceAngle(startAngle, sweep, slice))
                startAngle += sweep + gapDegrees
            }
        }
    }

    val density = LocalDensity.current
    val strokeWidthPx = with(density) { strokeWidthDp.dp.toPx() }
    val chartSizePx = with(density) { chartSizeDp.toPx() }
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

    var selectedSlice by remember(slices) { mutableStateOf<DonutSlice?>(null) }

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

    fun normalizeAngle(degrees: Float): Float {
        var a = degrees % 360f
        if (a < 0f) a += 360f
        return a
    }

    fun angleInsideSlice(angle: Float, start: Float, sweep: Float): Boolean {
        val a = normalizeAngle(angle)
        val s = normalizeAngle(start)
        val e = normalizeAngle(start + sweep)
        return if (s <= e) a in s..e else a >= s || a <= e
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(chartSizeDp)
                .pointerInput(sliceAngles, strokeWidthPx, diameter) {
                    detectTapGestures(
                        onPress = { down ->
                            val dx = down.x - centerX
                            val dy = down.y - centerY
                            val distance = sqrt(dx * dx + dy * dy)
                            val ringRadius = diameter / 2f
                            val innerRadius = ringRadius - strokeWidthPx / 2f
                            val outerRadius = ringRadius + strokeWidthPx / 2f

                            if (distance < innerRadius || distance > outerRadius) {
                                selectedSlice = null
                                tryAwaitRelease()
                                selectedSlice = null
                                return@detectTapGestures
                            }

                            var angle = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
                            if (angle < 0f) angle += 360f

                            val hit = sliceAngles.firstOrNull { angleInsideSlice(angle, it.startAngle, it.sweepAngle) }
                            selectedSlice = hit?.slice

                            // Keep it visible while pressing; clear on release/cancel.
                            tryAwaitRelease()
                            selectedSlice = null
                        }
                    )
                }
        ) {
            // Canvas for drawing arcs
            Canvas(modifier = Modifier.matchParentSize()) {
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                val arcSize = Size(diameter, diameter)
                sliceAngles.forEach { angleSlice ->
                    drawArc(
                        color = angleSlice.slice.color,
                        startAngle = angleSlice.startAngle,
                        sweepAngle = angleSlice.sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Butt)
                    )
                }
            }

            // Overlay layer for labels
            Box(modifier = Modifier.matchParentSize()) {
                val placedBounds = mutableListOf<RectF>()

                sliceAngles.forEach { angleSlice ->
                    val sweep = angleSlice.sweepAngle

                    if (sweep >= 9f) {
                        val midAngle = angleSlice.startAngle + sweep / 2f
                        val rad = Math.toRadians(midAngle.toDouble())
                        val xPx = centerX + (labelRadius * kotlin.math.cos(rad)).toFloat()
                        val yPx = centerY + (labelRadius * kotlin.math.sin(rad)).toFloat()
                        val theta = Math.toRadians(sweep.toDouble())
                        val chordWidthPx = (2.0 * labelRadius * kotlin.math.sin(theta / 2.0)).toFloat()
                        val arcLengthPx = (labelRadius * theta).toFloat()
                        val maxWidthPx = (kotlin.math.min(chordWidthPx, arcLengthPx) * 0.9f).coerceAtLeast(20f)

                        val fitted = fitLabel(angleSlice.slice.label, maxWidthPx)
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
                }
            }

            Text(
                text = totalLabel,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = selectedSlice?.let {
                val minutes = (it.value / 60_000L).coerceAtLeast(1L)
                "${it.label}: ${minutes} min"
            } ?: "Press and hold a slice",
            style = MaterialTheme.typography.labelLarge,
            color = if (selectedSlice != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
