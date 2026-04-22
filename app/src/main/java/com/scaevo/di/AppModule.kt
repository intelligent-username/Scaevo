package com.scaevo.di

import android.content.Context
import androidx.room.Room
import com.scaevo.data.db.AppDatabase
import com.scaevo.data.db.MIGRATION_1_2
import com.scaevo.data.db.MIGRATION_2_3
import com.scaevo.data.db.dao.BlockedAppDao
import com.scaevo.data.db.dao.DailyUsageStatDao
import com.scaevo.data.db.dao.DailyDeviceSummaryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "screentime.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides
    fun provideDailyUsageStatDao(db: AppDatabase): DailyUsageStatDao = db.dailyUsageStatDao()

    @Provides
    fun provideBlockedAppDao(db: AppDatabase): BlockedAppDao = db.blockedAppDao()

    @Provides
    fun provideDailyDeviceSummaryDao(db: AppDatabase): DailyDeviceSummaryDao = db.dailyDeviceSummaryDao()
}
