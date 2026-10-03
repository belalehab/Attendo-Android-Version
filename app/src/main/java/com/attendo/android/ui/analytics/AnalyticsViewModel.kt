package com.attendo.android.ui.analytics

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendo.android.data.local.Attendance
import com.attendo.android.data.local.AttendanceDao
import com.attendo.android.data.local.Student
import com.attendo.android.data.local.StudentDao
import com.attendo.android.utils.ExcelExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val studentDao: StudentDao,
    private val attendanceDao: AttendanceDao,
    private val excelExporter: ExcelExporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState

    private var currentWorkspace: String? = null

    // For export
    private var rawStudents = emptyList<Student>()
    private var rawAttendance = emptyList<Attendance>()

    fun loadAnalytics(workspace: String) {
        if (currentWorkspace == workspace) return
        currentWorkspace = workspace

        viewModelScope.launch {
            val studentsFlow = studentDao.getActiveStudentsByGrade(workspace)
            val attendanceFlow = attendanceDao.getAllAttendanceForGrade(workspace)

            combine(studentsFlow, attendanceFlow) { students, attendances ->
                rawStudents = students
                rawAttendance = attendances
                
                val attendanceByStudent = attendances.groupBy { it.nationalId }
                // Calculate the max possible sessions by counting unique sessions in this grade
                val totalSessions = attendances.map { it.sessionName }.distinct().size

                val stats = students.map { student ->
                    val records = attendanceByStudent[student.nationalId] ?: emptyList()
                    val attendedCount = records.size
                    
                    // "At Risk" Logic: Missed more than 2 sessions (simple threshold)
                    val isAtRisk = (totalSessions - attendedCount) > 2

                    StudentStats(
                        studentName = student.name,
                        nationalId = student.nationalId ?: "",
                        attendedCount = attendedCount,
                        excusedCount = records.count { it.isExcused == 1 },
                        bonusPoints = records.sumOf { it.bonusPoints ?: 0 },
                        isAtRisk = isAtRisk
                    )
                }.sortedByDescending { it.isAtRisk } // Bubble at-risk to the top

                val totalAtRisk = stats.count { it.isAtRisk }
                AnalyticsUiState(
                    stats = stats,
                    totalSessions = totalSessions,
                    totalAtRisk = totalAtRisk
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun exportToExcel(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, exportResult = null)
            val result = excelExporter.exportAnalytics(uri, currentWorkspace ?: "", rawStudents, rawAttendance)
            _uiState.value = _uiState.value.copy(
                isExporting = false, 
                exportResult = if (result.isSuccess) "Export Successful" else "Export Failed: ${result.exceptionOrNull()?.message}"
            )
        }
    }

    fun clearExportResult() {
        _uiState.value = _uiState.value.copy(exportResult = null)
    }
}

data class AnalyticsUiState(
    val stats: List<StudentStats> = emptyList(),
    val totalSessions: Int = 0,
    val totalAtRisk: Int = 0,
    val isExporting: Boolean = false,
    val exportResult: String? = null
)

data class StudentStats(
    val studentName: String,
    val nationalId: String,
    val attendedCount: Int,
    val excusedCount: Int,
    val bonusPoints: Int,
    val isAtRisk: Boolean
)
