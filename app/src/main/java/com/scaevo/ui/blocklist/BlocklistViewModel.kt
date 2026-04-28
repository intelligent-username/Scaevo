package com.scaevo.ui.blocklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scaevo.data.db.entity.BlockedApp
import com.scaevo.data.repository.BlocklistRepository
import com.scaevo.data.settings.UserSettingsRepository
import com.scaevo.data.usage.UsageStatsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class BlocklistViewModel @Inject constructor(
    private val blocklistRepository: BlocklistRepository,
    private val usageStatsHelper: UsageStatsHelper,
    private val userSettingsRepository: UserSettingsRepository,
) : ViewModel() {

    val blockedApps: StateFlow<List<BlockedApp>> =
        blocklistRepository.getAllBlockedApps()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _installedApps = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val installedApps: StateFlow<List<Pair<String, String>>> = _installedApps.asStateFlow()

    private val _todayLiveUsages = MutableStateFlow<Map<String, Long>>(emptyMap())
    val todayLiveUsages: StateFlow<Map<String, Long>> = _todayLiveUsages.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _installedApps.value = usageStatsHelper.getInstalledUserApps()
        }
        startLiveUsageRefreshLoop()
    }

    private fun startLiveUsageRefreshLoop() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                refreshTodayLiveUsages()
                delay(20_000L)
            }
        }
    }

    private fun refreshTodayLiveUsages() {
        val today = LocalDate.now()
        val startMs = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMs = System.currentTimeMillis()
        val includeHome = userSettingsRepository.readIncludeHomeScreen()
        val stats = usageStatsHelper.queryForegroundDurationsForRange(startMs, endMs, includeHome)
        _todayLiveUsages.value = stats.associate { it.packageName to it.totalForegroundMs }
    }

    fun addToBlocklist(packageName: String, label: String) {
        viewModelScope.launch { blocklistRepository.addApp(packageName, label) }
    }

    fun removeFromBlocklist(packageName: String) {
        viewModelScope.launch { blocklistRepository.removeApp(packageName) }
    }

    fun toggleEnabled(app: BlockedApp) {
        viewModelScope.launch { blocklistRepository.setEnabled(app, !app.isEnabled) }
    }

    fun updateLimit(app: BlockedApp, limitMinutes: Int?) {
        viewModelScope.launch { blocklistRepository.updateAppLimit(app, limitMinutes) }
    }
}
