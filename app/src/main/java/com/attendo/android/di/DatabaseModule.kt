package com.attendo.android.di

import android.content.Context
import androidx.room.Room
import com.attendo.android.data.local.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "attendo_core.db"
        ).build()
    }

    @Provides
    fun provideSettingsDao(database: AppDatabase) = database.settingsDao()

    @Provides
    fun provideAttendanceDao(database: AppDatabase) = database.attendanceDao()

    @Provides
    fun provideStudentDao(database: AppDatabase) = database.studentDao()
}
