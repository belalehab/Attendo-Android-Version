package com.attendo.android.utils

import android.content.Context
import android.net.Uri
import com.attendo.android.data.local.Attendance
import com.attendo.android.data.local.Student
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExcelExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun exportAnalytics(
        uri: Uri,
        grade: String,
        students: List<Student>,
        attendance: List<Attendance>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.bufferedWriter().use { writer ->
                    // Write Header
                    writer.write("Name,National ID,Grade,Total Attended,Total Excused,Total Bonus Points,Status\n")

                    val attendanceByStudent = attendance.groupBy { it.nationalId }

                    students.forEach { student ->
                        val studentRecords = attendanceByStudent[student.nationalId] ?: emptyList()
                        val attendedCount = studentRecords.size
                        val excusedCount = studentRecords.count { it.isExcused == 1 }
                        val bonusTotal = studentRecords.sumOf { it.bonusPoints ?: 0 }
                        val isAtRisk = attendedCount < 2 

                        val status = if (isAtRisk) "At Risk" else "Good"
                        
                        val name = student.name.replace("\"", "\"\"")
                        val nationalId = student.nationalId ?: ""
                        val studentGrade = student.grade ?: ""
                        
                        writer.write("\"${name}\",\"${nationalId}\",\"${studentGrade}\",${attendedCount},${excusedCount},${bonusTotal},\"${status}\"\n")
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
