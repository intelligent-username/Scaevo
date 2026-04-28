package com.scaevo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.scaevo.data.db.entity.DailyDeviceSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyDeviceSummaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(summary: DailyDeviceSummary)

    @Query("SELECT * FROM daily_device_summary WHERE date_epoch_day >= :startDay ORDER BY date_epoch_day ASC")
    fun getSummariesSince(startDay: Long): Flow<List<DailyDeviceSummary>>

    @Query(
        "SELECT * FROM daily_device_summary " +
            "WHERE date_epoch_day BETWEEN :startDay AND :endDay " +
            "ORDER BY date_epoch_day ASC"
    )
    fun getSummariesBetween(startDay: Long, endDay: Long): Flow<List<DailyDeviceSummary>>

    @Query("SELECT * FROM daily_device_summary WHERE date_epoch_day = :day LIMIT 1")
    suspend fun getSummaryForDay(day: Long): DailyDeviceSummary?
}
