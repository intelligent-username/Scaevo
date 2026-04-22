package com.scaevo.data.repository

import com.scaevo.data.db.dao.BlockedAppDao
import com.scaevo.data.db.entity.BlockedApp
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BlocklistRepository @Inject constructor(
    private val dao: BlockedAppDao
) {
    fun getAllBlockedApps(): Flow<List<BlockedApp>> = dao.getAllBlocked()

    suspend fun addApp(packageName: String, label: String) {
        dao.upsert(BlockedApp(packageName = packageName, appLabel = label))
    }

    suspend fun removeApp(packageName: String) {
        dao.delete(BlockedApp(packageName = packageName, appLabel = ""))
    }

    suspend fun setEnabled(app: BlockedApp, enabled: Boolean) {
        dao.update(app.copy(isEnabled = enabled))
    }

    suspend fun updateAppLimit(app: BlockedApp, limitMinutes: Int?) {
        dao.update(app.copy(dailyLimitMinutes = limitMinutes))
    }

    /** Synchronous — called from AccessibilityService background thread */
    fun getEnabledBlockedAppsSync(): List<BlockedApp> = dao.getEnabledBlockedAppsSync()
}
