package com.attendo.android.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class SessionSummary(
    val sessionName: String = "",
    val date: String = "",
    val attendeesCount: Int = 0
)

data class AttendanceWithStudent(
    @androidx.room.Embedded val attendance: Attendance = Attendance(nationalId = null, sessionName = null, timestamp = null),
    val studentName: String = ""
)

@Dao
interface StudentDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStudent(student: Student)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStudents(students: List<Student>)

    @Update
    suspend fun updateStudent(student: Student)

    @Delete
    suspend fun deleteStudent(student: Student)

    @Query("SELECT * FROM students WHERE grade = :grade AND is_deleted = 0 ORDER BY name ASC")
    fun getActiveStudentsByGrade(grade: String): Flow<List<Student>>
    
    @Query("SELECT * FROM students WHERE grade = :grade AND is_deleted = 1 ORDER BY name ASC")
    fun getArchivedStudentsByGrade(grade: String): Flow<List<Student>>
    
    @Query("SELECT * FROM students WHERE grade = :grade ORDER BY name ASC")
    fun getAllStudentsByGrade(grade: String): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE national_id = :nationalId LIMIT 1")
    suspend fun getStudentById(nationalId: String): Student?
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
        SELECT COALESCE(session_name, '') AS sessionName, 
               COALESCE(MAX(timestamp), '') AS date, 
               COUNT(*) AS attendeesCount 
        FROM attendance 
        WHERE session_name LIKE :workspacePrefix AND is_archived = :isArchived
        GROUP BY session_name 
        ORDER BY MAX(timestamp) DESC
    """)
    fun getSessionSummaries(workspacePrefix: String, isArchived: Int): Flow<List<SessionSummary>>
    
    @Query("UPDATE attendance SET is_archived = 1 WHERE session_name = :sessionName")
    suspend fun archiveSession(sessionName: String)

    @Query("UPDATE attendance SET is_archived = 0 WHERE session_name = :sessionName")
    suspend fun restoreSession(sessionName: String)
    
    @Query("DELETE FROM attendance WHERE session_name = :sessionName")
    suspend fun deleteSession(sessionName: String)

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
        WHERE s.grade = :workspace
    """)
    fun getWorkspaceAttendance(workspace: String): Flow<List<Attendance>>

    @Query("""
        SELECT a.* 
        FROM attendance a
        WHERE a.national_id = :nationalId AND a.session_name LIKE :workspacePrefix
        ORDER BY a.timestamp DESC
    """)
    suspend fun getStudentAttendanceList(nationalId: String, workspacePrefix: String): List<Attendance>

    @Query("SELECT DISTINCT session_name FROM attendance WHERE session_name LIKE :workspacePrefix AND is_archived = 0 ORDER BY timestamp DESC")
    suspend fun getActiveSessionsList(workspacePrefix: String): List<String>
}

@Dao
interface SettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(setting: Setting)

    @Query("SELECT value FROM settings WHERE `key` = :key")
    suspend fun getSetting(key: String): String?
}
