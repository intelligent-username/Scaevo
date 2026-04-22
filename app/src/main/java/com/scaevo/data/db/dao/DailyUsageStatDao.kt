package com.scaevo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.scaevo.data.db.entity.DailyUsageStat
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyUsageStatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(stats: List<DailyUsageStat>)

    /**
     * Returns the last N days of data as a reactive Flow.
     * UI observes this; no manual refresh needed.
     */
    @Query("""
        SELECT * FROM daily_usage_stats
        WHERE date_epoch_day >= :startEpochDay
        ORDER BY date_epoch_day ASC, total_foreground_ms DESC
    """)
    fun getStatsSince(startEpochDay: Long): Flow<List<DailyUsageStat>>

    @Query("""
        SELECT * FROM daily_usage_stats
        WHERE date_epoch_day = :epochDay
        ORDER BY total_foreground_ms DESC
    """)
    suspend fun getStatsForDay(epochDay: Long): List<DailyUsageStat>
}
