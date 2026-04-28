package com.scaevo.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.scaevo.data.db.dao.DailyUsageStatDao
import com.scaevo.data.db.dao.DailyDeviceSummaryDao
import com.scaevo.data.db.entity.DailyUsageStat
import com.scaevo.data.db.entity.DailyDeviceSummary
import com.scaevo.data.settings.UserSettingsRepository
import com.scaevo.data.usage.UsageStatsHelper
import com.scaevo.widget.UsageWidget
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

@HiltWorker
class DailyAggregationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val usageStatsHelper: UsageStatsHelper,
    private val dao: DailyUsageStatDao,
    private val dailyDeviceSummaryDao: DailyDeviceSummaryDao,
    private val userSettingsRepository: UserSettingsRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val includeHome = userSettingsRepository.readIncludeHomeScreen()
        val yesterday = LocalDate.now().minusDays(1)
        val zoneId = ZoneId.systemDefault()
        val startMs = yesterday.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMs = yesterday.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val rawStats = usageStatsHelper.queryForegroundDurationsForRange(startMs, endMs, includeHome)
        if (rawStats.isEmpty()) return Result.success()

        val launchCounts = usageStatsHelper.queryLaunchCounts(startMs, endMs, includeHome)

        val entities = rawStats.map { stat ->
            DailyUsageStat(
                packageName = stat.packageName,
                appLabel = usageStatsHelper.getAppLabel(stat.packageName),
                dateEpochDay = yesterday.toEpochDay(),
                totalForegroundMs = stat.totalForegroundMs,
                launchCount = launchCounts[stat.packageName] ?: 0
            )
        }.groupBy { normalizeLabel(it.appLabel) }
            .mapNotNull { (_, items) ->
                val canonical = items.maxByOrNull { it.totalForegroundMs } ?: return@mapNotNull null
                canonical.copy(
                    totalForegroundMs = items.sumOf { it.totalForegroundMs },
                    launchCount = items.sumOf { it.launchCount }
                )
            }

        dao.upsertAll(entities)

        // Trigger widget refresh after data is updated (Phase 2)
        try {
            val widgetManager = androidx.glance.appwidget.GlanceAppWidgetManager(applicationContext)
            widgetManager.getGlanceIds(UsageWidget::class.java)
                .forEach { id -> UsageWidget().update(applicationContext, id) }
        } catch (_: Exception) {
            // Widget may not be placed — ignore
        }

        val unlockCount = usageStatsHelper.queryUnlockCount(startMs, endMs)
        val totalMs = rawStats.sumOf { it.totalForegroundMs }
        dailyDeviceSummaryDao.upsert(
            DailyDeviceSummary(
                dateEpochDay = yesterday.toEpochDay(),
                totalScreenTimeMs = totalMs,
                unlockCount = unlockCount
            )
        )

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "DailyAggregation"

        fun schedule(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<DailyAggregationWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .setInitialDelay(computeDelayUntil3Am(), TimeUnit.MILLISECONDS)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        private fun computeDelayUntil3Am(): Long {
            val now = LocalDateTime.now()
            var next3am = now.toLocalDate().atTime(3, 0)
            if (now.hour >= 3) next3am = next3am.plusDays(1)
            return ChronoUnit.MILLIS.between(now, next3am).coerceAtLeast(0)
        }

        private fun normalizeLabel(label: String): String {
            return label.trim().lowercase().replace(Regex("\\s+"), " ")
        }
    }
}
