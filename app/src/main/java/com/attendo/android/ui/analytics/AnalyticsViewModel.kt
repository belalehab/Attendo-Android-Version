package com.attendo.android.ui.analytics

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendo.android.data.local.Attendance
import com.attendo.android.data.local.AttendanceDao
import com.attendo.android.data.local.Setting
import com.attendo.android.data.local.SettingsDao
import com.attendo.android.data.local.Student
import com.attendo.android.data.local.StudentDao
import com.attendo.android.utils.ExcelExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class SessionTurnout(val sessionIndex: Int, val turnout: Int, val label: String)

data class AnalyticsUiState(
    val stats: List<StudentStats> = emptyList(),
    val totalSessions: Int = 0,
    val totalAtRisk: Int = 0,
    val totalSafe: Int = 0,
    val avgTurnout: Int = 0,
    val totalWeeks: Int = 1,
    val currentWeekIndex: Int = 0,
    val turnoutTrend: List<SessionTurnout> = emptyList(),
    val isExporting: Boolean = false,
    val exportResult: String? = null,
    val threshold: Int = 3,
    val searchQuery: String = "",
    val sessionTypeFilter: String = "All Sessions"
)

data class StudentStats(
    val studentName: String,
    val nationalId: String,
    val attendedCount: Int,
    val absentCount: Int,
    val bonusPoints: Int,
    val isAtRisk: Boolean,
    val recentTrend: List<Boolean> // Last 3 sessions: true = attended, false = absent
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val studentDao: StudentDao,
    private val attendanceDao: AttendanceDao,
    private val settingsDao: SettingsDao,
    private val excelExporter: ExcelExporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState

    private val _threshold = MutableStateFlow(3)
    private val _searchQuery = MutableStateFlow("")
    private val _sessionTypeFilter = MutableStateFlow("All Sessions")

    private var currentWorkspace: String? = null
    
    private var rawStudents = emptyList<Student>()
    private var rawAttendance = emptyList<Attendance>()

    fun loadAnalytics(workspace: String) {
        if (currentWorkspace == workspace) return
        currentWorkspace = workspace

        viewModelScope.launch {
            val studentsFlow = studentDao.getActiveStudentsByGrade(workspace)
            val attendanceFlow = attendanceDao.getWorkspaceAttendance(workspace)
            
            // Get semester dates
            val currentWeekIndex = 0

            combine(
                studentsFlow, 
                attendanceFlow, 
                _threshold, 
                _searchQuery, 
                _sessionTypeFilter
            ) { students, attendances, threshold, query, typeFilter ->
                
                val filteredAttendances = if (typeFilter == "All Sessions") {
                    attendances
                } else {
                    attendances.filter { it.sessionName?.contains(typeFilter, ignoreCase = true) == true }
                }

                rawStudents = students
                rawAttendance = filteredAttendances
                
                val attendanceByStudent = filteredAttendances.groupBy { it.nationalId }
                
                // Sessions chronologically
                val uniqueSessions = filteredAttendances.mapNotNull { it.sessionName }.distinct().sorted()
                val totalSessions = uniqueSessions.size
                
                // Real past weeks count instead of semester calendar
                val totalWeeks = if (totalSessions > 0) totalSessions else 1

                // Calculate Trend
                val turnoutTrend = uniqueSessions.mapIndexed { index, sessionName ->
                    val turnout = filteredAttendances.count { it.sessionName == sessionName }
                    val weekLabel = sessionName.substringAfterLast(" - ", "W${index + 1}")
                    SessionTurnout(sessionIndex = index, turnout = turnout, label = weekLabel)
                }
                
                val avgTurnout = if (totalSessions > 0) turnoutTrend.sumOf { it.turnout } / totalSessions else 0

                val last3Sessions = uniqueSessions.takeLast(3)

                val stats = students.filter { 
                    it.name.contains(query, ignoreCase = true) || (it.nationalId?.contains(query) == true) 
                }.map { student ->
                    val records = attendanceByStudent[student.nationalId] ?: emptyList()
                    val attendedCount = records.size
                    val absentCount = totalSessions - attendedCount
                    
                    val isAtRisk = absentCount >= threshold

                    val recentTrend = last3Sessions.map { sessionName ->
                        records.any { it.sessionName == sessionName }
                    }

                    StudentStats(
                        studentName = student.name,
                        nationalId = student.nationalId ?: "",
                        attendedCount = attendedCount,
                        absentCount = absentCount,
                        bonusPoints = records.sumOf { it.bonusPoints ?: 0 },
                        isAtRisk = isAtRisk,
                        recentTrend = recentTrend
                    )
                }.sortedByDescending { it.isAtRisk }

                val totalAtRisk = stats.count { it.isAtRisk }
                val totalSafe = stats.size - totalAtRisk

                AnalyticsUiState(
                    stats = stats,
                    totalSessions = totalSessions,
                    totalAtRisk = totalAtRisk,
                    totalSafe = totalSafe,
                    avgTurnout = avgTurnout,
                    totalWeeks = totalWeeks,
                    currentWeekIndex = currentWeekIndex,
                    turnoutTrend = turnoutTrend,
                    threshold = threshold,
                    searchQuery = query,
                    sessionTypeFilter = typeFilter
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    private fun calculateSemesterWeeks(start: String, end: String): Pair<Int, Int> {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val startDate = format.parse(start) ?: return Pair(15, 1)
            val endDate = format.parse(end) ?: return Pair(15, 1)
            
            val startCal = Calendar.getInstance().apply { time = startDate }
            val endCal = Calendar.getInstance().apply { time = endDate }
            
            // Adjust to previous Saturday
            while (startCal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
                startCal.add(Calendar.DAY_OF_MONTH, -1)
            }
            
            val diffMs = endCal.timeInMillis - startCal.timeInMillis
            val totalWeeks = (diffMs / (1000 * 60 * 60 * 24 * 7)).toInt().coerceAtLeast(1)
            
            val now = Calendar.getInstance()
            val currentDiff = now.timeInMillis - startCal.timeInMillis
            val currentWeek = (currentDiff / (1000 * 60 * 60 * 24 * 7)).toInt().coerceIn(0, totalWeeks - 1)
            
            Pair(totalWeeks, currentWeek)
        } catch (e: Exception) {
            Pair(15, 1) // default 15 weeks
        }
    }

    fun updateThreshold(newThreshold: Int) {
        if (newThreshold >= 0) {
            _threshold.value = newThreshold
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSessionTypeFilter(filter: String) {
        _sessionTypeFilter.value = filter
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

    fun exportToPdf(context: Context, uri: Uri) {
        // Fallback for now if PdfExporter doesn't exist natively for Master Reports
        // Usually you'd invoke a dedicated PdfExporter.
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, exportResult = null)
            
            try {
                // Here we will use the existing ExcelExporter conceptually, but to PDF.
                // Since this might not be implemented, we inform success.
                // Actually, I'll invoke a native Android Canvas PDF generator.
                com.attendo.android.utils.PdfMasterReportExporter.export(context, uri, currentWorkspace ?: "", rawStudents, rawAttendance, _sessionTypeFilter.value)
                _uiState.value = _uiState.value.copy(isExporting = false, exportResult = "Export Successful")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isExporting = false, exportResult = "Export Failed: ${e.message}")
            }
        }
    }

    fun clearExportResult() {
        _uiState.value = _uiState.value.copy(exportResult = null)
    }
}
