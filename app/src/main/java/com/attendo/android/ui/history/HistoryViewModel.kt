package com.attendo.android.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendo.android.data.local.Attendance
import com.attendo.android.data.local.AttendanceDao
import com.attendo.android.data.local.AttendanceWithStudent
import com.attendo.android.data.local.SessionSummary
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

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val attendanceDao: AttendanceDao
) : ViewModel() {

    private val _isArchiveView = MutableStateFlow(false)
    private val _activeWorkspace = MutableStateFlow<String?>(null)
    private val _sessionSummaries = MutableStateFlow<List<SessionSummary>>(emptyList())
    private val _selectedSession = MutableStateFlow<String?>(null)
    private val _sessionRecords = MutableStateFlow<List<AttendanceWithStudent>>(emptyList())
    private val _activeCount = MutableStateFlow(0)

    val uiState: StateFlow<HistoryUiState> = combine(
        combine(_isArchiveView, _activeWorkspace, _sessionSummaries) { archive, ws, summaries ->
            Triple(archive, ws, summaries)
        },
        combine(_selectedSession, _sessionRecords, _activeCount) { sel, recs, count ->
            Triple(sel, recs, count)
        }
    ) { p1, p2 ->
        HistoryUiState(
            isArchiveView = p1.first,
            activeWorkspace = p1.second,
            sessionSummaries = p1.third,
            selectedSession = p2.first,
            sessionRecords = p2.second,
            activeCount = p2.third
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())

    private var sessionsJob: Job? = null
    private var detailJob: Job? = null
    private var countJob: Job? = null

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

    fun updateBonusPoints(record: Attendance, newPoints: Int) {
        viewModelScope.launch {
            val updated = record.copy(
                bonusPoints = newPoints,
                auditTrail = appendAudit(record.auditTrail, "Updated bonus points to $newPoints")
            )
            attendanceDao.updateAttendance(updated)
        }
    }

    fun toggleExcused(record: Attendance, isExcused: Boolean, reason: String? = null) {
        viewModelScope.launch {
            val updated = record.copy(
                isExcused = if (isExcused) 1 else 0,
                excuseReason = if (isExcused) reason else null,
                auditTrail = appendAudit(record.auditTrail, if (isExcused) "Marked excused: $reason" else "Removed excuse")
            )
            attendanceDao.updateAttendance(updated)
        }
    }

    fun removeAttendance(record: Attendance) {
        viewModelScope.launch {
            attendanceDao.deleteAttendance(record)
        }
    }
    
    // Bulk / Single actions for sessions
    fun archiveSessions(sessionNames: List<String>) {
        viewModelScope.launch {
            sessionNames.forEach { name ->
                attendanceDao.archiveSession(name)
            }
        }
    }
    
    fun restoreSessions(sessionNames: List<String>) {
        viewModelScope.launch {
            sessionNames.forEach { name ->
                attendanceDao.restoreSession(name)
            }
        }
    }
    
    fun deleteSessions(sessionNames: List<String>) {
        viewModelScope.launch {
            sessionNames.forEach { name ->
                attendanceDao.deleteSession(name)
            }
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
    val activeCount: Int = 0
)
