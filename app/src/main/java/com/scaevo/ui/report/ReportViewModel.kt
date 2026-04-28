package com.scaevo.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scaevo.data.db.entity.DailyUsageStat
import com.scaevo.data.settings.UserSettingsRepository
import com.scaevo.data.usage.UsageStatsHelper
import com.scaevo.data.repository.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import com.scaevo.data.db.entity.DailyDeviceSummary
import javax.inject.Inject

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val usageRepository: UsageRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val usageStatsHelper: UsageStatsHelper,
) : ViewModel() {

    private data class WeekRange(val startEpochDay: Long, val endEpochDay: Long)

    private fun weekRangeForOffset(offset: Int): WeekRange {
        // Reports are aggregated by the worker for completed days; end at yesterday.
        val end = LocalDate.now().minusDays(1).toEpochDay() - (offset * 7L)
        val start = end - 6L
        return WeekRange(start, end)
    }

    private val weekOffset = MutableStateFlow(0)

    val canGoToNextWeek: StateFlow<Boolean> = weekOffset
        .map { it > 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val weekRange: StateFlow<Pair<Long, Long>> = weekOffset
        .map { offset ->
            val r = weekRangeForOffset(offset)
            r.startEpochDay to r.endEpochDay
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), weekRangeForOffset(0).let { it.startEpochDay to it.endEpochDay })

    fun goToPreviousWeek() {
        weekOffset.value = weekOffset.value + 1
    }

    fun goToNextWeek() {
        if (weekOffset.value > 0) weekOffset.value = weekOffset.value - 1
    }

    private val includeHomeFlow = userSettingsRepository.includeHomeScreen

    /** Emits automatically whenever Room is updated by the Worker */
    val weeklyStats: StateFlow<List<DailyUsageStat>> =
        weekOffset
            .map { weekRangeForOffset(it) }
            .flatMapLatest { range ->
                usageRepository.getWeeklyStatsBetween(range.startEpochDay, range.endEpochDay)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Aggregated total per app over the week, sorted by most used */
    val weeklyTotalByApp: StateFlow<List<Pair<String, Long>>> =
        combine(weeklyStats, includeHomeFlow) { stats, includeHome ->
            val filtered = if (includeHome) stats
            else stats.filter { !usageStatsHelper.isHomePackage(it.packageName) }

            filtered.groupBy { it.appLabel }
                .mapValues { entry -> entry.value.sumOf { it.totalForegroundMs } }
                .entries
                .sortedByDescending { it.value }
                .map { it.key to it.value }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyDonutStats: StateFlow<List<DailyUsageStat>> =
        combine(weeklyStats, includeHomeFlow) { stats, includeHome ->
            val filtered = if (includeHome) stats
            else stats.filter { !usageStatsHelper.isHomePackage(it.packageName) }

            filtered.groupBy { it.packageName }
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
        weekOffset
            .map { weekRangeForOffset(it) }
            .flatMapLatest { range ->
                usageRepository.getWeeklySummariesBetween(range.startEpochDay, range.endEpochDay)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weekOverWeekDelta: StateFlow<Float?> =
        weekOffset
            .flatMapLatest { offset ->
                val thisRange = weekRangeForOffset(offset)
                val prevRange = weekRangeForOffset(offset + 1)
                combine(
                    usageRepository.getWeeklySummariesBetween(thisRange.startEpochDay, thisRange.endEpochDay),
                    usageRepository.getWeeklySummariesBetween(prevRange.startEpochDay, prevRange.endEpochDay)
                ) { thisWeek, lastWeek ->
                    val thisTotal = thisWeek.sumOf { it.totalScreenTimeMs }.toFloat()
                    val lastTotal = lastWeek.sumOf { it.totalScreenTimeMs }.toFloat()
                    if (lastTotal == 0f) null
                    else ((thisTotal - lastTotal) / lastTotal) * 100f
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
