package com.scaevo.data.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

class UsageStatsHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    private val packageManager = context.packageManager

    data class AppForegroundDuration(
        val packageName: String,
        val totalForegroundMs: Long
    )

    /**
     * Returns a list of per-package usage stats for the given time window.
     * Filters out entries with zero foreground time and the app's own package.
     * Returns empty list if PACKAGE_USAGE_STATS permission is not granted.
     */
    fun queryUsageForRange(startMs: Long, endMs: Long): List<UsageStats> {
        if (!hasUsagePermission()) return emptyList()
        return usageStatsManager
            .queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startMs, endMs)
            ?.filter { it.totalTimeInForeground > 0 && it.packageName != context.packageName }
            ?: emptyList()
    }

    /**
     * Computes exact foreground durations per package from UsageEvents in [startMs, endMs].
     * This is more precise for "today" windows (local midnight -> now) than interval aggregates.
     */
    fun queryForegroundDurationsForRange(startMs: Long, endMs: Long): List<AppForegroundDuration> {
        if (!hasUsagePermission() || endMs <= startMs) return emptyList()

        val durations = mutableMapOf<String, Long>()
        val activeStart = mutableMapOf<String, Long>()
        var currentForegroundPackage: String? = null

        val events = usageStatsManager.queryEvents(startMs, endMs)
        val event = UsageEvents.Event()

        fun closeSession(packageName: String, stopMs: Long) {
            val start = activeStart.remove(packageName) ?: return
            val duration = stopMs - start
            if (duration > 0) durations[packageName] = (durations[packageName] ?: 0L) + duration
        }

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val packageName = event.packageName ?: continue
            if (packageName == context.packageName) continue

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    val current = currentForegroundPackage
                    if (current != null && current != packageName) {
                        closeSession(current, event.timeStamp)
                    }

                    // Ignore duplicate resume signals for a package that's already tracked.
                    if (currentForegroundPackage == packageName && activeStart.containsKey(packageName)) {
                        continue
                    }

                    activeStart[packageName] = event.timeStamp
                    currentForegroundPackage = packageName
                }

                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.MOVE_TO_BACKGROUND,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    closeSession(packageName, event.timeStamp)
                    if (currentForegroundPackage == packageName) {
                        currentForegroundPackage = null
                    }
                }

                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    val current = currentForegroundPackage
                    if (current != null && current != packageName) {
                        closeSession(current, event.timeStamp)
                    }
                    if (!activeStart.containsKey(packageName)) {
                        activeStart[packageName] = event.timeStamp
                    }
                    currentForegroundPackage = packageName
                }
            }
        }

        activeStart.forEach { (packageName, start) ->
            val duration = endMs - start
            if (duration > 0) durations[packageName] = (durations[packageName] ?: 0L) + duration
        }

        return durations
            .filterValues { it > 0L }
            .map { (packageName, totalMs) -> AppForegroundDuration(packageName, totalMs) }
    }

    /**
     * Uses UsageEvents to count actual app launches (more accurate than UsageStats alone).
     * Counts ACTIVITY_RESUMED events per package in the given window.
     */
    fun queryLaunchCounts(startMs: Long, endMs: Long): Map<String, Int> {
        if (!hasUsagePermission()) return emptyMap()
        val counts = mutableMapOf<String, Int>()
        val events = usageStatsManager.queryEvents(startMs, endMs)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                counts[event.packageName] = (counts[event.packageName] ?: 0) + 1
            }
        }
        return counts
    }

    fun hasUsagePermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getAppLabel(packageName: String): String {
        return try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    /**
     * Returns all installed apps that have a launcher icon (user-facing apps).
     * Excludes the current app. Requires QUERY_ALL_PACKAGES on Android 11+.
     */
    fun getInstalledUserApps(): List<Pair<String, String>> {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        return packageManager.queryIntentActivities(intent, 0)
            .map { it.activityInfo.packageName to it.activityInfo.loadLabel(packageManager).toString() }
            .filter { it.first != context.packageName }
            .distinctBy { it.first }
            .sortedBy { it.second }
    }

    /**
     * Returns a 24-element array where index = hour of day (0–23),
     * value = total foreground milliseconds in that hour.
     *
     * Attributes each session's duration to the hour the session started.
     * Sessions spanning midnight are split at midnight.
     * Returns a zeroed array if permission is not granted.
     */
    fun queryHourlyBreakdown(startMs: Long, endMs: Long): LongArray {
        if (!hasUsagePermission()) return LongArray(24)
        val hourlyMs = LongArray(24)
        val events = usageStatsManager.queryEvents(startMs, endMs)
        val event = UsageEvents.Event()
        val resumeTimes = mutableMapOf<String, Long>()
        var currentForegroundPackage: String? = null

        fun closeAndSplit(packageName: String, stopMs: Long) {
            val start = resumeTimes.remove(packageName) ?: return
            if (stopMs <= start) return
            addDurationSplitByHour(hourlyMs, start, stopMs)
        }

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    val packageName = event.packageName ?: continue
                    if (packageName == context.packageName) continue

                    val current = currentForegroundPackage
                    if (current != null && current != packageName) {
                        closeAndSplit(current, event.timeStamp)
                    }

                    if (currentForegroundPackage == packageName && resumeTimes.containsKey(packageName)) {
                        continue
                    }

                    resumeTimes[packageName] = event.timeStamp
                    currentForegroundPackage = packageName
                }

                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val packageName = event.packageName ?: continue
                    closeAndSplit(packageName, event.timeStamp)
                    if (currentForegroundPackage == packageName) {
                        currentForegroundPackage = null
                    }
                }

                UsageEvents.Event.ACTIVITY_STOPPED,
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val packageName = event.packageName ?: continue
                    closeAndSplit(packageName, event.timeStamp)
                    if (currentForegroundPackage == packageName) {
                        currentForegroundPackage = null
                    }
                }

                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    val packageName = event.packageName ?: continue
                    if (packageName == context.packageName) continue

                    val current = currentForegroundPackage
                    if (current != null && current != packageName) {
                        closeAndSplit(current, event.timeStamp)
                    }
                    if (!resumeTimes.containsKey(packageName)) {
                        resumeTimes[packageName] = event.timeStamp
                    }
                    currentForegroundPackage = packageName
                }
            }
        }

        resumeTimes.forEach { (_, start) ->
            if (endMs > start) {
                addDurationSplitByHour(hourlyMs, start, endMs)
            }
        }

        return hourlyMs
    }

    private fun addDurationSplitByHour(hourlyMs: LongArray, startMs: Long, endMs: Long) {
        if (endMs <= startMs) return
        val zone = ZoneId.systemDefault()
        var cursor = startMs
        while (cursor < endMs) {
            val zdt = Instant.ofEpochMilli(cursor).atZone(zone)
            val nextHourMs = zdt.withMinute(0).withSecond(0).withNano(0).plusHours(1).toInstant().toEpochMilli()
            val segmentEnd = minOf(endMs, nextHourMs)
            val hour = zdt.hour
            if (hour in 0..23 && segmentEnd > cursor) {
                hourlyMs[hour] += (segmentEnd - cursor)
            }
            cursor = segmentEnd
        }
    }

    /**
     * Counts KEYGUARD_HIDDEN events (screen unlock) in the given window.
     */
    fun queryUnlockCount(startMs: Long, endMs: Long): Int {
        if (!hasUsagePermission()) return 0
        var count = 0
        val events = usageStatsManager.queryEvents(startMs, endMs)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.KEYGUARD_HIDDEN) count++
        }
        return count
    }
}
