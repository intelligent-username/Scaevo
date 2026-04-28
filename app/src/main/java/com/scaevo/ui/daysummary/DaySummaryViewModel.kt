package com.scaevo.ui.daysummary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scaevo.data.db.entity.DailyDeviceSummary
import com.scaevo.data.db.entity.DailyUsageStat
import com.scaevo.data.repository.UsageRepository
import com.scaevo.data.settings.UserSettingsRepository
import com.scaevo.data.usage.UsageStatsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class DaySummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val usageRepository: UsageRepository,
    private val usageStatsHelper: UsageStatsHelper,
    private val userSettingsRepository: UserSettingsRepository,
) : ViewModel() {

    private val epochDay: Long = checkNotNull(savedStateHandle["epochDay"]) // Nav argument

    private val _stats = MutableStateFlow<List<DailyUsageStat>>(emptyList())
    val stats: StateFlow<List<DailyUsageStat>> = _stats.asStateFlow()

    private val _deviceSummary = MutableStateFlow<DailyDeviceSummary?>(null)
    val deviceSummary: StateFlow<DailyDeviceSummary?> = _deviceSummary.asStateFlow()

    private val _hourlyBreakdown = MutableStateFlow(LongArray(24))
    val hourlyBreakdown: StateFlow<LongArray> = _hourlyBreakdown.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val includeHome = userSettingsRepository.readIncludeHomeScreen()
            val dayStats = usageRepository.getStatsForDay(epochDay)
                .filter { includeHome || !usageStatsHelper.isHomePackage(it.packageName) }

            _stats.value = dayStats
            _deviceSummary.value = usageRepository.getDeviceSummaryForDay(epochDay)

            // Optional: compute hourly breakdown from UsageEvents for that day.
            // This uses UsageStatsManager history; results can vary by device/OEM.
            val date = LocalDate.ofEpochDay(epochDay)
            val startMs = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMs = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            _hourlyBreakdown.value = usageStatsHelper.queryHourlyBreakdown(startMs, endMs, includeHome)
        }
    }
}
