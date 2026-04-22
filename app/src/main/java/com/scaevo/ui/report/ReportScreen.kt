package com.scaevo.ui.report

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scaevo.ui.components.DonutChart
import com.scaevo.ui.components.WeeklyTrendChart
import com.scaevo.ui.utils.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: ReportViewModel,
    onNavigateToAppDetail: (String) -> Unit
) {
    val totals by viewModel.weeklyDonutStats.collectAsStateWithLifecycle()
    val summaries by viewModel.weeklySummaries.collectAsStateWithLifecycle()
    val delta by viewModel.weekOverWeekDelta.collectAsStateWithLifecycle()
    
    val sortedTotals = totals.sortedByDescending { it.totalForegroundMs }
    val maxMs = sortedTotals.firstOrNull()?.totalForegroundMs ?: 1L

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Last 7 Days") },
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
    appLabel: String,
    totalMs: Long,
    maxMs: Long,
    onClick: () -> Unit
) {
    val fraction = (totalMs.toFloat() / maxMs).coerceIn(0f, 1f)

    ListItem(
        modifier = Modifier.clickable { onClick() },
        leadingContent = {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        "$rank",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
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
