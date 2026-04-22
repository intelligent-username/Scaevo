package com.scaevo.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.scaevo.data.db.dao.BlockedAppDao
import com.scaevo.data.db.dao.DailyUsageStatDao
import com.scaevo.data.db.dao.DailyDeviceSummaryDao
import com.scaevo.data.db.entity.BlockedApp
import com.scaevo.data.db.entity.DailyUsageStat
import com.scaevo.data.db.entity.DailyDeviceSummary

@Database(
    entities = [DailyUsageStat::class, BlockedApp::class, DailyDeviceSummary::class],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dailyUsageStatDao(): DailyUsageStatDao
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun dailyDeviceSummaryDao(): DailyDeviceSummaryDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS daily_device_summary (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                date_epoch_day INTEGER NOT NULL,
                total_screen_time_ms INTEGER NOT NULL,
                unlock_count INTEGER NOT NULL
            )
        """)
        db.execSQL("""
            CREATE UNIQUE INDEX IF NOT EXISTS index_daily_device_summary_date_epoch_day
            ON daily_device_summary (date_epoch_day)
        """)
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE blocked_apps ADD COLUMN daily_limit_minutes INTEGER DEFAULT NULL")
    }
}
