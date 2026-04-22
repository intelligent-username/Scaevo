package com.scaevo.ui.appdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scaevo.data.db.entity.DailyUsageStat
import com.scaevo.data.repository.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val usageRepository: UsageRepository
) : ViewModel() {

    private val packageName: String = checkNotNull(savedStateHandle["packageName"])

    val appStats: StateFlow<List<DailyUsageStat>> = usageRepository.getWeeklyStats(days = 7)
        .map { stats ->
            stats.filter { it.packageName == packageName }
                .sortedBy { it.dateEpochDay }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalMs: StateFlow<Long> = appStats.map { stats ->
        stats.sumOf { it.totalForegroundMs }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
}
