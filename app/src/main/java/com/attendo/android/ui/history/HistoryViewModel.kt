package com.attendo.android.ui.history

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendo.android.data.local.Attendance
import com.attendo.android.data.local.AttendanceDao
import com.attendo.android.data.local.AttendanceWithStudent
import com.attendo.android.data.local.SessionSummary
import com.attendo.android.data.local.Student
import com.attendo.android.data.local.StudentDao
import com.attendo.android.utils.AttendeeEntry
import com.attendo.android.utils.SessionExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class SessionAuditEntry(
    val student: Student,
    val attendance: Attendance?
) {
    val status: String
        get() = when {
            attendance == null -> "ABSENT"
            attendance.isExcused == 1 -> "EXCUSED"
            else -> "PRESENT"
        }
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val attendanceDao: AttendanceDao,
    private val studentDao: StudentDao
) : ViewModel() {

    private val _isArchiveView = MutableStateFlow(false)
    private val _activeWorkspace = MutableStateFlow<String?>(null)
    private val _sessionSummaries = MutableStateFlow<List<SessionSummary>>(emptyList())
    private val _selectedSession = MutableStateFlow<String?>(null)
    
    // For Audit View
    private val _workspaceStudents = MutableStateFlow<List<Student>>(emptyList())
    private val _sessionRecords = MutableStateFlow<List<AttendanceWithStudent>>(emptyList())
    private val _activeCount = MutableStateFlow(0)

    val uiState: StateFlow<HistoryUiState> = combine(
        combine(_isArchiveView, _activeWorkspace, _sessionSummaries) { archive, ws, summaries ->
            Triple(archive, ws, summaries)
        },
        combine(_selectedSession, _sessionRecords, _activeCount) { sel, recs, count ->
            Triple(sel, recs, count)
        },
        _workspaceStudents
    ) { p1, p2, students ->
        val selectedSessionName = p2.first
        val records = p2.second
        
        // Build the audit entries for the detail view
        val auditEntries = students.map { student ->
            val attendanceRecord = records.find { it.attendance.nationalId == student.nationalId }?.attendance
            SessionAuditEntry(student, attendanceRecord)
        }

        HistoryUiState(
            isArchiveView = p1.first,
            activeWorkspace = p1.second,
            sessionSummaries = p1.third,
            selectedSession = selectedSessionName,
            sessionRecords = records,
            auditEntries = auditEntries,
            activeCount = p2.third
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())

    private var sessionsJob: Job? = null
    private var detailJob: Job? = null
    private var countJob: Job? = null
    private var studentsJob: Job? = null

    fun setArchiveView(isArchive: Boolean) {
        _isArchiveView.value = isArchive
        _activeWorkspace.value?.let { loadSessions(it) }
    }

    fun loadSessions(workspace: String) {
        _activeWorkspace.value = workspace
        val prefix = "[$workspace]%"
        
        // Track active count always
        countJob?.cancel()
        countJob = viewModelScope.launch {
            attendanceDao.getSessionSummaries(prefix, 0).collect { activeList ->
                _activeCount.value = activeList.size
                if (!_isArchiveView.value) {
                    _sessionSummaries.value = activeList
                }
            }
        }
        
        sessionsJob?.cancel()
        sessionsJob = viewModelScope.launch {
            if (_isArchiveView.value) {
                attendanceDao.getSessionSummaries(prefix, 1).collect { archivedList ->
                    if (_isArchiveView.value) {
                        _sessionSummaries.value = archivedList
                    }
                }
            }
        }
        
        studentsJob?.cancel()
        studentsJob = viewModelScope.launch {
            studentDao.getAllStudentsByGrade(workspace).collect { students ->
                _workspaceStudents.value = students
            }
        }
    }

    fun selectSession(sessionName: String?) {
        _selectedSession.value = sessionName
        
        detailJob?.cancel()
        if (sessionName != null) {
            detailJob = viewModelScope.launch {
                attendanceDao.getAttendanceWithStudentNames(sessionName).collect { records ->
                    _sessionRecords.value = records
                }
            }
        } else {
            _sessionRecords.value = emptyList()
        }
    }

    fun cycleAttendanceState(student: Student) {
        val sessionName = _selectedSession.value ?: return
        val currentRecord = _sessionRecords.value.find { it.attendance.nationalId == student.nationalId }?.attendance
        
        viewModelScope.launch {
            if (currentRecord == null) {
                // ABSENT -> PRESENT
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                val newRecord = Attendance(
                    nationalId = student.nationalId,
                    sessionName = sessionName,
                    timestamp = timestamp,
                    isExcused = 0,
                    auditTrail = appendAudit("[]", "Marked Present manually")
                )
                attendanceDao.insertAttendance(newRecord)
            } else if (currentRecord.isExcused == 0) {
                // PRESENT -> EXCUSED
                val updated = currentRecord.copy(
                    isExcused = 1,
                    excuseReason = "Manual",
                    auditTrail = appendAudit(currentRecord.auditTrail, "Marked Excused manually")
                )
                attendanceDao.updateAttendance(updated)
            } else {
                // EXCUSED -> ABSENT
                attendanceDao.deleteAttendance(currentRecord)
            }
        }
    }

    fun changeBonusPoints(student: Student, delta: Int) {
        val sessionName = _selectedSession.value ?: return
        val currentRecord = _sessionRecords.value.find { it.attendance.nationalId == student.nationalId }?.attendance
        
        viewModelScope.launch {
            if (currentRecord == null) {
                // Create record implicitly if missing
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                val newRecord = Attendance(
                    nationalId = student.nationalId,
                    sessionName = sessionName,
                    timestamp = timestamp,
                    isExcused = 0,
                    bonusPoints = delta,
                    auditTrail = appendAudit("[]", "Added bonus points manually")
                )
                attendanceDao.insertAttendance(newRecord)
            } else {
                val newPoints = (currentRecord.bonusPoints ?: 0) + delta
                val updated = currentRecord.copy(
                    bonusPoints = newPoints,
                    auditTrail = appendAudit(currentRecord.auditTrail, "Updated bonus points to $newPoints")
                )
                attendanceDao.updateAttendance(updated)
            }
        }
    }
    
    // Bulk / Single actions for sessions
    fun archiveSessions(sessionNames: List<String>) {
        viewModelScope.launch {
            sessionNames.forEach { attendanceDao.archiveSession(it) }
        }
    }
    
    fun restoreSessions(sessionNames: List<String>) {
        viewModelScope.launch {
            sessionNames.forEach { attendanceDao.restoreSession(it) }
        }
    }
    
    fun deleteSessions(sessionNames: List<String>) {
        viewModelScope.launch {
            sessionNames.forEach { attendanceDao.deleteSession(it) }
        }
    }
    
    fun exportSessionAsPdf(context: Context, uri: Uri) {
        viewModelScope.launch {
            val sessionName = _selectedSession.value ?: return@launch
            val entries = _sessionRecords.value.map { 
                AttendeeEntry(it.studentName, it.attendance.nationalId ?: "", it.attendance.timestamp ?: "")
            }
            SessionExporter.exportPdf(context, uri, sessionName, entries)
        }
    }

    fun exportSessionAsCsv(context: Context, uri: Uri) {
        viewModelScope.launch {
            val sessionName = _selectedSession.value ?: return@launch
            val entries = _sessionRecords.value.map { 
                AttendeeEntry(it.studentName, it.attendance.nationalId ?: "", it.attendance.timestamp ?: "")
            }
            SessionExporter.exportCsv(context, uri, sessionName, entries)
        }
    }

    private fun appendAudit(existingTrail: String?, action: String): String {
        val trail = if (existingTrail.isNullOrBlank()) "[]" else existingTrail
        val array = try { JSONArray(trail) } catch (e: Exception) { JSONArray() }
        
        val entry = JSONObject().apply {
            put("timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
            put("action", action)
            put("actor", "Teacher_Android")
        }
        
        array.put(entry)
        return array.toString()
    }
}

data class HistoryUiState(
    val isArchiveView: Boolean = false,
    val activeWorkspace: String? = null,
    val sessionSummaries: List<SessionSummary> = emptyList(),
    val selectedSession: String? = null,
    val sessionRecords: List<AttendanceWithStudent> = emptyList(),
    val auditEntries: List<SessionAuditEntry> = emptyList(),
    val activeCount: Int = 0
)
