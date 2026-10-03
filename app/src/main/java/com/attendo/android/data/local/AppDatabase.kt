package com.attendo.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Student::class, Attendance::class, Setting::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun studentDao(): StudentDao
}
