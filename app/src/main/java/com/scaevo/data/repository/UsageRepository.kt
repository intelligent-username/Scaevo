package com.scaevo.data.repository

import com.scaevo.data.db.dao.DailyUsageStatDao
import com.scaevo.data.db.dao.DailyDeviceSummaryDao
import com.scaevo.data.db.entity.DailyUsageStat
import com.scaevo.data.db.entity.DailyDeviceSummary
import com.scaevo.data.settings.UserSettingsRepository
import com.scaevo.data.usage.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class UsageRepository @Inject constructor(
    private val dao: DailyUsageStatDao,
    private val dailyDeviceSummaryDao: DailyDeviceSummaryDao,
    private val usageStatsHelper: UsageStatsHelper,
    private val userSettingsRepository: UserSettingsRepository
) {
    /**
     * Returns a Flow of the last [days] days of aggregated usage from Room.
     * Never queries UsageStatsManager — data must be pre-loaded by DailyAggregationWorker.
     */
    fun getWeeklyStats(days: Int = 7): Flow<List<DailyUsageStat>> {
        val startDay = LocalDate.now().minusDays(days.toLong()).toEpochDay()
        return dao.getStatsSince(startDay)
    }

    /**
     * One-time query of today's live usage from UsageStatsManager.
     * Used for the dashboard view only — the Worker has not yet run for today.
     */
    suspend fun getTodayLive(): List<DailyUsageStat> = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val startMs = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMs = System.currentTimeMillis()
        val includeHomeScreen = userSettingsRepository.readIncludeHomeScreen()
        val launchCounts = usageStatsHelper.queryLaunchCounts(startMs, endMs, includeHomeScreen)

        val raw = usageStatsHelper.queryForegroundDurationsForRange(startMs, endMs, includeHomeScreen)
            .map { stat ->
            DailyUsageStat(
                packageName = stat.packageName,
                appLabel = usageStatsHelper.getAppLabel(stat.packageName),
                dateEpochDay = today.toEpochDay(),
                totalForegroundMs = stat.totalForegroundMs,
                launchCount = launchCounts[stat.packageName] ?: 0
            )
        }

        raw.groupBy { normalizeLabel(it.appLabel) }
            .mapNotNull { (_, items) ->
                val canonical = items.maxByOrNull { it.totalForegroundMs } ?: return@mapNotNull null
                canonical.copy(
                    totalForegroundMs = items.sumOf { it.totalForegroundMs },
                    launchCount = items.sumOf { it.launchCount }
                )
            }
            .sortedByDescending { it.totalForegroundMs }
    }

    private fun normalizeLabel(label: String): String {
        return label.trim().lowercase().replace(Regex("\\s+"), " ")
    }

    fun getWeeklySummaries(days: Int = 7): Flow<List<DailyDeviceSummary>> {
        val startDay = LocalDate.now().minusDays(days.toLong()).toEpochDay()
        return dailyDeviceSummaryDao.getSummariesSince(startDay)
    }

    fun getWeeklyStatsBetween(startEpochDay: Long, endEpochDay: Long): Flow<List<DailyUsageStat>> {
        return dao.getStatsBetween(startEpochDay, endEpochDay)
    }

    fun getWeeklySummariesBetween(startEpochDay: Long, endEpochDay: Long): Flow<List<DailyDeviceSummary>> {
        return dailyDeviceSummaryDao.getSummariesBetween(startEpochDay, endEpochDay)
    }

    suspend fun getStatsForDay(epochDay: Long): List<DailyUsageStat> = withContext(Dispatchers.IO) {
        dao.getStatsForDay(epochDay)
    }

    suspend fun getDeviceSummaryForDay(epochDay: Long): DailyDeviceSummary? = withContext(Dispatchers.IO) {
        dailyDeviceSummaryDao.getSummaryForDay(epochDay)
    }
}
