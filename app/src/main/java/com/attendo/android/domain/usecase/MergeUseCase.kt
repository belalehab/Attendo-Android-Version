package com.attendo.android.domain.usecase

import android.database.sqlite.SQLiteDatabase
import com.attendo.android.data.local.AppDatabase
import com.attendo.android.data.local.Attendance
import com.attendo.android.data.local.Student
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MergeUseCase @Inject constructor(
    private val appDatabase: AppDatabase
) {
    suspend fun mergeDatabase(
        importedFile: File, 
        activeWorkspace: String, 
        resolutions: Map<String, String>? = null // key: "$nationalId|$sessionName", value: "LOCAL" or "IMPORTED"
    ): Result<MergeSummary> = withContext(Dispatchers.IO) {
        try {
            val extDb = SQLiteDatabase.openDatabase(importedFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            
            val conflicts = mutableListOf<MergeConflict>()
            var insertedCount = 0

            // To replicate desktop exactly, we read all from imported DB
            val extAttendanceCursor = extDb.rawQuery(
                "SELECT a.national_id, a.session_name, a.is_excused, a.bonus_points, a.timestamp, a.excuse_reason, a.is_archived, a.audit_trail, s.name " +
                "FROM attendance a INNER JOIN students s ON a.national_id = s.national_id " +
                "WHERE a.session_name LIKE ? AND s.grade = ?", 
                arrayOf("[$activeWorkspace]%", activeWorkspace)
            )

            val extRecords = mutableListOf<ExtAttendance>()
            while (extAttendanceCursor.moveToNext()) {
                extRecords.add(
                    ExtAttendance(
                        nationalId = extAttendanceCursor.getString(0) ?: "",
                        sessionName = extAttendanceCursor.getString(1) ?: "",
                        isExcused = extAttendanceCursor.getInt(2),
                        bonusPoints = extAttendanceCursor.getInt(3),
                        timestamp = extAttendanceCursor.getString(4),
                        excuseReason = extAttendanceCursor.getString(5),
                        isArchived = extAttendanceCursor.getInt(6),
                        auditTrail = extAttendanceCursor.getString(7),
                        studentName = extAttendanceCursor.getString(8) ?: ""
                    )
                )
            }
            extAttendanceCursor.close()

            val localAttendanceList = appDatabase.query("SELECT * FROM attendance WHERE session_name LIKE ?", arrayOf("[$activeWorkspace]%"))
            val localMap = mutableMapOf<String, LocalAtt>()
            while (localAttendanceList.moveToNext()) {
                val nid = localAttendanceList.getString(localAttendanceList.getColumnIndexOrThrow("national_id"))
                val sname = localAttendanceList.getString(localAttendanceList.getColumnIndexOrThrow("session_name"))
                val exc = localAttendanceList.getInt(localAttendanceList.getColumnIndexOrThrow("is_excused"))
                val bp = localAttendanceList.getInt(localAttendanceList.getColumnIndexOrThrow("bonus_points"))
                localMap["$nid|$sname"] = LocalAtt(exc, bp)
            }
            localAttendanceList.close()

            // 2. Compare and detect conflicts
            val toInsert = mutableListOf<ExtAttendance>()
            val toUpdate = mutableListOf<ExtAttendance>()
            
            for (ext in extRecords) {
                val key = "${ext.nationalId}|${ext.sessionName}"
                val local = localMap[key]
                if (local != null) {
                    if (local.isExcused != ext.isExcused || local.bonusPoints != ext.bonusPoints) {
                        val choice = resolutions?.get(key)
                        if (choice == "IMPORTED") {
                            toUpdate.add(ext)
                        } else if (choice == null) { // Unresolved
                            conflicts.add(
                                MergeConflict(
                                    studentName = ext.studentName,
                                    nationalId = ext.nationalId,
                                    sessionName = ext.sessionName,
                                    localExcused = local.isExcused,
                                    extExcused = ext.isExcused,
                                    localBonus = local.bonusPoints,
                                    extBonus = ext.bonusPoints
                                )
                            )
                        }
                    }
                } else {
                    toInsert.add(ext)
                }
            }

            // If no unresolved conflicts, commit inserts and updates
            if (conflicts.isEmpty()) {
                appDatabase.withTransaction {
                    // Insert missing students
                    val extStudentsCursor = extDb.rawQuery("SELECT * FROM students WHERE grade = ?", arrayOf(activeWorkspace))
                    while (extStudentsCursor.moveToNext()) {
                        appDatabase.studentDao().insertStudent(
                            Student(
                                name = extStudentsCursor.getString(extStudentsCursor.getColumnIndexOrThrow("name")),
                                nationalId = extStudentsCursor.getString(extStudentsCursor.getColumnIndexOrThrow("national_id")),
                                grade = extStudentsCursor.getString(extStudentsCursor.getColumnIndexOrThrow("grade")),
                                status = extStudentsCursor.getString(extStudentsCursor.getColumnIndexOrThrow("status")),
                                isDeleted = extStudentsCursor.getInt(extStudentsCursor.getColumnIndexOrThrow("is_deleted"))
                            )
                        )
                    }
                    extStudentsCursor.close()

                    // Insert missing attendance
                    for (ext in toInsert) {
                        appDatabase.attendanceDao().insertAttendance(
                            Attendance(
                                nationalId = ext.nationalId,
                                sessionName = ext.sessionName,
                                timestamp = ext.timestamp,
                                isExcused = ext.isExcused,
                                bonusPoints = ext.bonusPoints,
                                isArchived = ext.isArchived,
                                excuseReason = ext.excuseReason,
                                auditTrail = ext.auditTrail
                            )
                        )
                        insertedCount++
                    }

                    // Update resolved attendance
                    for (ext in toUpdate) {
                        val attendanceDao = appDatabase.attendanceDao()
                        appDatabase.query("UPDATE attendance SET is_excused = ?, bonus_points = ? WHERE national_id = ? AND session_name = ?", 
                            arrayOf(ext.isExcused, ext.bonusPoints, ext.nationalId, ext.sessionName))
                        insertedCount++
                    }
                }
            }

            extDb.close()
            
            Result.success(MergeSummary(conflicts, insertedCount))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}

data class LocalAtt(val isExcused: Int, val bonusPoints: Int)
data class ExtAttendance(
    val nationalId: String, val sessionName: String, val isExcused: Int, val bonusPoints: Int,
    val timestamp: String?, val excuseReason: String?, val isArchived: Int, val auditTrail: String?, val studentName: String
)

data class MergeConflict(
    val studentName: String, val nationalId: String, val sessionName: String,
    val localExcused: Int, val extExcused: Int, val localBonus: Int, val extBonus: Int
)

data class MergeSummary(val conflicts: List<MergeConflict>, val insertedCount: Int)
