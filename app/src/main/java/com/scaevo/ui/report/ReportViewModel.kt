package com.scaevo.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scaevo.data.db.entity.DailyUsageStat
import com.scaevo.data.repository.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import com.scaevo.data.db.entity.DailyDeviceSummary
import javax.inject.Inject

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val usageRepository: UsageRepository
) : ViewModel() {

    /** Emits automatically whenever Room is updated by the Worker */
    val weeklyStats: StateFlow<List<DailyUsageStat>> =
        usageRepository.getWeeklyStats(days = 7)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Aggregated total per app over the week, sorted by most used */
    val weeklyTotalByApp: StateFlow<List<Pair<String, Long>>> =
        weeklyStats.map { stats ->
            stats.groupBy { it.appLabel }
                .mapValues { entry -> entry.value.sumOf { it.totalForegroundMs } }
                .entries
                .sortedByDescending { it.value }
                .map { it.key to it.value }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyDonutStats: StateFlow<List<DailyUsageStat>> =
        weeklyStats.map { stats ->
            stats.groupBy { it.packageName }
                .map { (pkg, entries) ->
                    DailyUsageStat(
                        packageName = pkg,
                        appLabel = entries.first().appLabel,
                        dateEpochDay = 0L,
                        totalForegroundMs = entries.sumOf { it.totalForegroundMs },
                        launchCount = entries.sumOf { it.launchCount }
                    )
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklySummaries: StateFlow<List<DailyDeviceSummary>> =
        usageRepository.getWeeklySummaries(days = 7)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weekOverWeekDelta: StateFlow<Float?> =
        combine(
            usageRepository.getWeeklySummaries(days = 7),
            usageRepository.getWeeklySummaries(days = 14)
        ) { thisWeek, twoWeeks ->
            val lastWeek = twoWeeks.filter {
                it.dateEpochDay < LocalDate.now().minusDays(7).toEpochDay()
            }
            val thisTotal = thisWeek.sumOf { it.totalScreenTimeMs }.toFloat()
            val lastTotal = lastWeek.sumOf { it.totalScreenTimeMs }.toFloat()
            if (lastTotal == 0f) null
            else ((thisTotal - lastTotal) / lastTotal) * 100f
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
