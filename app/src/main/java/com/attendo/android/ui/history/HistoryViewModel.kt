package com.attendo.android.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendo.android.data.local.Attendance
import com.attendo.android.data.local.AttendanceDao
import com.attendo.android.data.local.AttendanceWithStudent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState

    private var sessionsJob: Job? = null
    private var detailJob: Job? = null
    private var currentWorkspace: String? = null

    fun loadSessions(workspace: String) {
        if (currentWorkspace == workspace) return
        currentWorkspace = workspace
        
        sessionsJob?.cancel()
        sessionsJob = viewModelScope.launch {
            // Match sessions like "[Grade 10] Math..."
            val prefix = "[$workspace]%"
            attendanceDao.getDistinctSessions(prefix).collect { sessions ->
                _uiState.value = _uiState.value.copy(sessions = sessions)
            }
        }
    }

    fun selectSession(sessionName: String?) {
        _uiState.value = _uiState.value.copy(selectedSession = sessionName)
        
        detailJob?.cancel()
        if (sessionName != null) {
            detailJob = viewModelScope.launch {
                attendanceDao.getAttendanceWithStudentNames(sessionName).collect { records ->
                    _uiState.value = _uiState.value.copy(sessionRecords = records)
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(sessionRecords = emptyList())
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
    val sessions: List<String> = emptyList(),
    val selectedSession: String? = null,
    val sessionRecords: List<AttendanceWithStudent> = emptyList()
)
