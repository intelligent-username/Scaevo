package com.scaevo.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Refresh
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
import com.scaevo.data.db.entity.DailyUsageStat
import com.scaevo.ui.components.DonutChart
import com.scaevo.ui.components.HourlyBarChart
import com.scaevo.ui.components.SummaryStatsRow
import com.scaevo.ui.utils.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToReport: () -> Unit,
    onNavigateToBlocklist: () -> Unit,
    onNavigateToAppDetail: (String) -> Unit
) {
    val stats by viewModel.todayStats.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val hourlyBreakdown by viewModel.hourlyBreakdown.collectAsStateWithLifecycle()
    val unlockCount by viewModel.unlockCount.collectAsStateWithLifecycle()
    var allAppsExpanded by remember { mutableStateOf(false) }

    val totalMs = stats.sumOf { it.totalForegroundMs }
    val mostUsedApp = stats.firstOrNull()?.appLabel
    val visibleStats = if (allAppsExpanded) stats else stats.take(5)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Today") },
                actions = {
                    IconButton(onClick = { viewModel.loadToday() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onNavigateToReport) {
                        Icon(Icons.Default.BarChart, contentDescription = "Weekly Report")
                    }
                    IconButton(onClick = onNavigateToBlocklist) {
                        Icon(Icons.Default.Block, contentDescription = "Blocklist")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(contentPadding = padding) {
                item {
                    SummaryStatsRow(
                        totalMs = totalMs,
                        unlockCount = unlockCount,
                        mostUsedApp = mostUsedApp
                    )
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    DonutChart(stats = stats)
                    Spacer(Modifier.height(24.dp))
                }

                item {
                    Text(
                        "By Time of Day",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    HourlyBarChart(
                        hourlyMs = hourlyBreakdown,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(24.dp))
                }

                item {
                    Text(
                        "All Apps",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }
                items(visibleStats) { stat ->
                    AppUsageRow(
                        stat = stat,
                        totalMs = totalMs,
                        onClick = { onNavigateToAppDetail(stat.packageName) }
                    )
                }

                if (stats.size > 5) {
                    item {
                        TextButton(
                            onClick = { allAppsExpanded = !allAppsExpanded },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(if (allAppsExpanded) "Show fewer" else "Show all (${stats.size})")
                        }
                    }
                }

                if (stats.isEmpty()) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No data yet", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Usage Access permission must be granted and some time must have passed.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppUsageRow(
    stat: DailyUsageStat,
    totalMs: Long = 0L,
    onClick: () -> Unit
) {
    val fraction = if (totalMs > 0) stat.totalForegroundMs.toFloat() / totalMs else 0f
    val context = LocalContext.current
    val appIcon = remember(stat.packageName) {
        runCatching {
            val drawable = context.packageManager.getApplicationIcon(stat.packageName)
            drawable.toBitmap(72, 72).asImageBitmap()
        }.getOrNull()
    }

    ListItem(
        modifier = Modifier.clickable { onClick() },
        leadingContent = {
            if (appIcon != null) {
                Image(
                    bitmap = appIcon,
                    contentDescription = "${stat.appLabel} icon",
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        headlineContent = {
            Text(stat.appLabel, style = MaterialTheme.typography.bodyLarge)
        },
        supportingContent = {
            Column {
                Text(
                    formatDuration(stat.totalForegroundMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                "${stat.launchCount}×",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
