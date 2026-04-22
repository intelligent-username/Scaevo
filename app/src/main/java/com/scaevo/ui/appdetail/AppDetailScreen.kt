package com.scaevo.ui.appdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scaevo.ui.components.AppTrendChart
import com.scaevo.ui.utils.formatDuration
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    viewModel: AppDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val stats by viewModel.appStats.collectAsStateWithLifecycle()
    val totalMs by viewModel.totalMs.collectAsStateWithLifecycle()
    
    val appLabel = stats.firstOrNull()?.appLabel ?: "App Details"
    val daysCount = stats.size.coerceAtLeast(1)
    val avgMs = totalMs / daysCount

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appLabel) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (stats.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No data available")
            }
        } else {
            LazyColumn(contentPadding = padding) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("Total (7d)", formatDuration(totalMs))
                        StatItem("Daily Avg", formatDuration(avgMs))
                    }
                    Spacer(Modifier.height(16.dp))
                }

                item {
                    Text(
                        "7-Day Trend",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    AppTrendChart(
                        stats = stats,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(24.dp))
                }

                item {
                    Text(
                        "Daily Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }

                items(stats.reversed()) { stat ->
                    val dayName = LocalDate.ofEpochDay(stat.dateEpochDay)
                        .dayOfWeek
                        .getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    
                    ListItem(
                        headlineContent = { Text(dayName, style = MaterialTheme.typography.bodyLarge) },
                        trailingContent = {
                            Text(
                                formatDuration(stat.totalForegroundMs),
                                style = MaterialTheme.typography.labelLarge,
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
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
