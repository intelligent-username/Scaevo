package com.scaevo.blocking

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.scaevo.data.repository.BlocklistRepository
import com.scaevo.data.usage.UsageStatsHelper
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn

class AppBlockingAccessibilityService : AccessibilityService() {

    // Services cannot use @Inject directly with Hilt. Use EntryPoint instead.
    private val entryPoint by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            BlockingServiceEntryPoint::class.java
        )
    }
    private val blocklistRepo by lazy { entryPoint.blocklistRepository() }
    private val usageStatsHelper by lazy { entryPoint.usageStatsHelper() }

    // Cache the blocklist to avoid a DB hit on every window event.
    // Refresh frequently so limit edits are reflected quickly.
    private var cachedBlocklist: Map<String, Int?> = emptyMap()
    private var cacheTimestamp = 0L
    private val cacheLifetimeMs = 5_000L
    private var lastBlockActionMsByPackage: MutableMap<String, Long> = mutableMapOf()

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Phase 2: start foreground service for Samsung hardening
        BlockingForegroundService.start(applicationContext)
        refreshCache()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return

        // Ignore system UI, launcher, and self
        if (packageName == "com.android.systemui" ||
            packageName == applicationContext.packageName) return

        refreshCacheIfStale()

        val limitMinutes = cachedBlocklist[packageName]
        if (cachedBlocklist.containsKey(packageName)) {
            if (limitMinutes == null) {
                // Hard block
                blockPackage(packageName)
            } else {
                // Check if limit exceeded
                Thread {
                    val today = java.time.LocalDate.now()
                    val startMs = today.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val endMs = System.currentTimeMillis()
                    val stats = usageStatsHelper.queryForegroundDurationsForRange(startMs, endMs)
                    val usedMs = stats.find { it.packageName == packageName }?.totalForegroundMs ?: 0L
                    if (usedMs >= limitMinutes * 60_000L) {
                        blockPackage(packageName)
                    }
                }.start()
            }
        }
    }

    override fun onInterrupt() { /* Required override */ }

    private fun refreshCacheIfStale() {
        if (System.currentTimeMillis() - cacheTimestamp > cacheLifetimeMs) {
            refreshCache()
        }
    }

    private fun refreshCache() {
        cachedBlocklist = blocklistRepo.getEnabledBlockedAppsSync()
            .associate { it.packageName to it.dailyLimitMinutes }
        cacheTimestamp = System.currentTimeMillis()
    }

    private fun blockPackage(packageName: String) {
        val now = System.currentTimeMillis()
        val lastAction = lastBlockActionMsByPackage[packageName] ?: 0L
        if (now - lastAction < 2_500L) return
        lastBlockActionMsByPackage[packageName] = now
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BlockingServiceEntryPoint {
        fun blocklistRepository(): BlocklistRepository
        fun usageStatsHelper(): UsageStatsHelper
    }
}
