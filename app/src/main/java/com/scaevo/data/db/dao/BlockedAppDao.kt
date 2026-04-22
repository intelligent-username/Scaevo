package com.scaevo.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.scaevo.data.db.entity.BlockedApp
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedAppDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(app: BlockedApp)

    @Delete
    suspend fun delete(app: BlockedApp)

    @Update
    suspend fun update(app: BlockedApp)

    /** Reactive list for the UI blocklist screen */
    @Query("SELECT * FROM blocked_apps ORDER BY app_label ASC")
    fun getAllBlocked(): Flow<List<BlockedApp>>

    /**
     * IMPORTANT: Called on every window change event from the AccessibilityService.
     * Must be synchronous (no suspend) and is called from a background thread.
     * Cache this result in the AccessibilityService; do not query Room on every event.
     */
    @Query("SELECT * FROM blocked_apps WHERE is_enabled = 1")
    fun getEnabledBlockedAppsSync(): List<BlockedApp>
}
