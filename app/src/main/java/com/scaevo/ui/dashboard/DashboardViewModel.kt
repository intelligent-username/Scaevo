package com.scaevo.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scaevo.data.db.entity.DailyUsageStat
import com.scaevo.data.repository.UsageRepository
import com.scaevo.data.usage.UsageStatsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val usageRepository: UsageRepository,
    private val usageStatsHelper: UsageStatsHelper
) : ViewModel() {

    private val _todayStats = MutableStateFlow<List<DailyUsageStat>>(emptyList())
    val todayStats: StateFlow<List<DailyUsageStat>> = _todayStats.asStateFlow()

    private val _hourlyBreakdown = MutableStateFlow(LongArray(24))
    val hourlyBreakdown: StateFlow<LongArray> = _hourlyBreakdown.asStateFlow()

    private val _unlockCount = MutableStateFlow(0)
    val unlockCount: StateFlow<Int> = _unlockCount.asStateFlow()

    val isLoading = MutableStateFlow(true)

    init {
        loadToday(showLoading = true)
        startLiveRefreshLoop()
        startMidnightRefreshLoop()
    }

    fun loadToday(showLoading: Boolean = false) {
        viewModelScope.launch {
            if (showLoading) {
                isLoading.value = true
            }
            val today = LocalDate.now()
            val startMs = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val currentMs = System.currentTimeMillis()
            
            _todayStats.value = usageRepository.getTodayLive()
            _hourlyBreakdown.value = withContext(Dispatchers.IO) {
                usageStatsHelper.queryHourlyBreakdown(startMs, currentMs)
            }
            _unlockCount.value = withContext(Dispatchers.IO) {
                usageStatsHelper.queryUnlockCount(startMs, currentMs)
            }

            if (showLoading) {
                isLoading.value = false
            }
        }
    }

    private fun startLiveRefreshLoop() {
        viewModelScope.launch {
            while (isActive) {
                delay(20_000L)
                loadToday(showLoading = false)
            }
        }
    }

    private fun startMidnightRefreshLoop() {
        viewModelScope.launch {
            while (isActive) {
                delay(computeDelayUntilNextMidnightMs())
                loadToday()
            }
        }
    }

    private fun computeDelayUntilNextMidnightMs(): Long {
        val now = LocalDateTime.now()
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
        val nowMs = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val nextMs = nextMidnight.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return (nextMs - nowMs).coerceAtLeast(1_000L)
    }
}
