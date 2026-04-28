package com.scaevo.ui.report

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scaevo.ui.components.DonutChart
import com.scaevo.ui.components.WeeklyTrendChart
import com.scaevo.ui.utils.formatDuration
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: ReportViewModel,
    onNavigateToAppDetail: (String) -> Unit,
    onNavigateToDaySummary: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val totals by viewModel.weeklyDonutStats.collectAsStateWithLifecycle()
    val summaries by viewModel.weeklySummaries.collectAsStateWithLifecycle()
    val delta by viewModel.weekOverWeekDelta.collectAsStateWithLifecycle()
    val range by viewModel.weekRange.collectAsStateWithLifecycle()
    val canGoNext by viewModel.canGoToNextWeek.collectAsStateWithLifecycle()
    var pressedDay by remember { mutableStateOf<com.scaevo.data.db.entity.DailyDeviceSummary?>(null) }
    
    val sortedTotals = totals.sortedByDescending { it.totalForegroundMs }
    val maxMs = sortedTotals.firstOrNull()?.totalForegroundMs ?: 1L

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Last 7 Days") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (totals.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No weekly data yet", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "The daily worker runs at ~3 AM to aggregate yesterday's usage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(contentPadding = padding) {
                item {
                    // Week navigation
                    val startDate = LocalDate.ofEpochDay(range.first)
                    val endDate = LocalDate.ofEpochDay(range.second)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = {
                            pressedDay = null
                            viewModel.goToPreviousWeek()
                        }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous week")
                        }

                        Text(
                            text = "${startDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${startDate.dayOfMonth} – ${endDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${endDate.dayOfMonth}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        IconButton(
                            onClick = {
                                pressedDay = null
                                viewModel.goToNextWeek()
                            },
                            enabled = canGoNext
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next week")
                        }
                    }

                    if (delta != null) {
                        val sign = if (delta!! > 0) "+" else ""
                        Text(
                            text = "$sign${String.format("%.0f", delta)}% vs last week",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (delta!! > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    WeeklyTrendChart(
                        summaries = summaries,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        onDayPressed = { day -> pressedDay = day },
                        onDayReleased = { pressedDay = null },
                        onDayClicked = { day -> onNavigateToDaySummary(day.dateEpochDay) },
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = pressedDay?.let {
                            val d = LocalDate.ofEpochDay(it.dateEpochDay)
                            "${d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())}: ${formatDuration(it.totalScreenTimeMs)}"
                        } ?: "Press a day to preview, tap to open",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(24.dp))
                }

                item {
                    DonutChart(stats = sortedTotals)
                    Spacer(Modifier.height(24.dp))
                }

                item {
                    Text(
                        "By App",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }

                itemsIndexed(sortedTotals) { index, stat ->
                    ReportRow(
                        rank = index + 1,
                        packageName = stat.packageName,
                        appLabel = stat.appLabel,
                        totalMs = stat.totalForegroundMs,
                        maxMs = maxMs,
                        onClick = { onNavigateToAppDetail(stat.packageName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportRow(
    rank: Int,
    packageName: String,
    appLabel: String,
    totalMs: Long,
    maxMs: Long,
    onClick: () -> Unit
) {
    val fraction = (totalMs.toFloat() / maxMs).coerceIn(0f, 1f)
    val context = LocalContext.current
    val appIcon = remember(packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(packageName)
                .toBitmap(72, 72)
                .asImageBitmap()
        }.getOrNull()
    }

    ListItem(
        modifier = Modifier.clickable { onClick() },
        leadingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            "$rank",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon,
                        contentDescription = "$appLabel icon",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        headlineContent = {
            Text(appLabel, style = MaterialTheme.typography.bodyLarge)
        },
        supportingContent = {
            Column {
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        },
        trailingContent = {
            Text(
                formatDuration(totalMs),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    )
}
