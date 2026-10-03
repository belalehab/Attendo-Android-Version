package com.attendo.android.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT value FROM settings WHERE key = :key")
    suspend fun getSetting(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(setting: Setting)
}

@Dao
interface AttendanceDao {
    @Insert
    suspend fun insertAttendance(attendance: Attendance)

    @Update
    suspend fun updateAttendance(attendance: Attendance)

    @Delete
    suspend fun deleteAttendance(attendance: Attendance)

    @Query("SELECT * FROM attendance WHERE session_name = :sessionName ORDER BY timestamp DESC")
    suspend fun getAttendanceForSession(sessionName: String): List<Attendance>

    @Query("SELECT DISTINCT session_name FROM attendance WHERE session_name LIKE :workspacePrefix ORDER BY timestamp DESC")
    fun getDistinctSessions(workspacePrefix: String): Flow<List<String>>

    @Query("""
        SELECT a.*, s.name as studentName 
        FROM attendance a 
        INNER JOIN students s ON a.national_id = s.national_id 
        WHERE a.session_name = :sessionName
    """)
    fun getAttendanceWithStudentNames(sessionName: String): Flow<List<AttendanceWithStudent>>

    @Query("""
        SELECT a.* 
        FROM attendance a
        INNER JOIN students s ON a.national_id = s.national_id
        WHERE s.grade = :grade
    """)
    fun getAllAttendanceForGrade(grade: String): Flow<List<Attendance>>
}

data class AttendanceWithStudent(
    @androidx.room.Embedded val attendance: Attendance,
    val studentName: String
)

@Dao
interface StudentDao {
    @Query("SELECT * FROM students WHERE national_id = :nationalId LIMIT 1")
    suspend fun getStudentById(nationalId: String): Student?

    @Query("SELECT * FROM students WHERE grade = :grade AND is_deleted = 0 ORDER BY name ASC")
    fun getActiveStudentsByGrade(grade: String): Flow<List<Student>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStudent(student: Student): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStudents(students: List<Student>)

    @Update
    suspend fun updateStudent(student: Student)

    @Delete
    suspend fun deleteStudent(student: Student)
}
