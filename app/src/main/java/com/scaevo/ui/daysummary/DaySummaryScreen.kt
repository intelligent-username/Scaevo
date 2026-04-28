package com.scaevo.ui.daysummary

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scaevo.ui.components.DonutChart
import com.scaevo.ui.components.HourlyBarChart
import com.scaevo.ui.utils.formatDuration
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaySummaryScreen(
    viewModel: DaySummaryViewModel,
    epochDay: Long,
    onNavigateBack: () -> Unit,
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val summary by viewModel.deviceSummary.collectAsStateWithLifecycle()
    val hourly by viewModel.hourlyBreakdown.collectAsStateWithLifecycle()

    val date = LocalDate.ofEpochDay(epochDay)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(date.toString()) },
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
        if (stats.isEmpty() && summary == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No data for this day yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                item {
                    Spacer(Modifier.height(12.dp))
                    summary?.let {
                        Text(
                            text = formatDuration(it.totalScreenTimeMs),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${it.unlockCount} unlocks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }

                item {
                    if (stats.isNotEmpty()) {
                        DonutChart(
                            stats = stats,
                            modifier = Modifier.fillMaxWidth(),
                            chartSizeDp = 300.dp,
                        )
                        Spacer(Modifier.height(20.dp))
                    }
                }

                item {
                    Text(
                        "By Time of Day",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    HourlyBarChart(
                        hourlyMs = hourly,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}
