package com.scaevo.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_device_summary",
    indices = [Index(value = ["date_epoch_day"], unique = true)]
)
data class DailyDeviceSummary(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "date_epoch_day") val dateEpochDay: Long,
    @ColumnInfo(name = "total_screen_time_ms") val totalScreenTimeMs: Long,
    @ColumnInfo(name = "unlock_count") val unlockCount: Int
)
