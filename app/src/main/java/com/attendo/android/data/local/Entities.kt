package com.attendo.android.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "students", indices = [Index(value = ["national_id"], unique = true)])
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "national_id") val nationalId: String?,
    @ColumnInfo(name = "grade") val grade: String?,
    @ColumnInfo(name = "status", defaultValue = "'offline'") val status: String? = "offline",
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Int? = 0
)

@Entity(tableName = "attendance")
data class Attendance(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "national_id") val nationalId: String?,
    @ColumnInfo(name = "session_name") val sessionName: String?,
    @ColumnInfo(name = "timestamp", defaultValue = "CURRENT_TIMESTAMP") val timestamp: String?,
    @ColumnInfo(name = "is_excused", defaultValue = "0") val isExcused: Int? = 0,
    @ColumnInfo(name = "bonus_points", defaultValue = "0") val bonusPoints: Int? = 0,
    @ColumnInfo(name = "is_archived", defaultValue = "0") val isArchived: Int? = 0,
    @ColumnInfo(name = "excuse_reason") val excuseReason: String? = null,
    @ColumnInfo(name = "audit_trail", defaultValue = "'[]'") val auditTrail: String? = "[]"
)

@Entity(tableName = "settings")
data class Setting(
    @PrimaryKey @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "value") val value: String?
)
