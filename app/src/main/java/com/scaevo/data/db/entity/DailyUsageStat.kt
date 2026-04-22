package com.scaevo.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_usage_stats",
    indices = [Index(value = ["package_name", "date_epoch_day"], unique = true)]
)
data class DailyUsageStat(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "app_label") val appLabel: String,
    @ColumnInfo(name = "date_epoch_day") val dateEpochDay: Long,
    @ColumnInfo(name = "total_foreground_ms") val totalForegroundMs: Long,
    @ColumnInfo(name = "launch_count") val launchCount: Int
)
