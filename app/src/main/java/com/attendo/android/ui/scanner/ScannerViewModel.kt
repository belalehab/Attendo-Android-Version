package com.attendo.android.ui.scanner

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendo.android.core.SessionConfig
import com.attendo.android.core.SessionManager
import com.attendo.android.data.local.Attendance
import com.attendo.android.data.local.AttendanceDao
import com.attendo.android.data.local.SettingsDao
import com.attendo.android.data.local.Student
import com.attendo.android.data.local.StudentDao
import com.attendo.android.domain.usecase.QRValidator
import com.attendo.android.utils.AttendeeEntry
import com.attendo.android.utils.HardwareFeedbackManager
import com.attendo.android.utils.SessionExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val qrValidator: QRValidator,
    private val feedbackManager: HardwareFeedbackManager,
    private val attendanceDao: AttendanceDao,
    private val studentDao: StudentDao,
    private val settingsDao: SettingsDao,
    val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())

    val uiState: StateFlow<ScannerUiState> = combine(
        _uiState,
        sessionManager.sessionState
    ) { baseState, session ->
        baseState.copy(
            isSessionActive = session.isActive,
            sessionConfig = session.config,
            attendeeCount = session.scannedAttendees.size,
            scannedAttendeesMap = session.scannedAttendees,
            lastScannedMessage = session.lastMessage,
            isProcessingScan = session.isProcessing
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScannerUiState())

    private var studentsJob: Job? = null
    
    // We cache the latest session data for the summary dialog so it survives clearing
    var lastSavedSessionTitle: String = ""
    var lastSavedAttendees: List<AttendeeEntry> = emptyList()
    var lastSavedAttendeeCount: Int = 0

    fun loadInstructorAndSubject(activeWorkspace: String) {
        viewModelScope.launch {
            val instructor = settingsDao.getSetting("instructor_name") ?: "Unknown Instructor"
            var subjectName = "Unknown Subject"
            try {
                val subjectsString = settingsDao.getSetting("subject_name") ?: "{}"
                val subjectsObj = JSONObject(subjectsString)
                if (subjectsObj.has(activeWorkspace)) {
                    subjectName = subjectsObj.getString(activeWorkspace)
                }
            } catch (e: Exception) { e.printStackTrace() }

            _uiState.value = _uiState.value.copy(
                instructorName = instructor,
                subjectName = subjectName,
                activeWorkspace = activeWorkspace
            )
            observeStudents(activeWorkspace)
        }
    }

    private fun observeStudents(workspace: String) {
        studentsJob?.cancel()
        studentsJob = viewModelScope.launch {
            studentDao.getActiveStudentsByGrade(workspace).collectLatest { students ->
                _uiState.value = _uiState.value.copy(
                    workspaceStudents = students,
                    workspaceStudentCount = students.size
                )
            }
        }
    }

    fun updateConfig(modifier: (SessionConfig) -> SessionConfig) {
        sessionManager.updateConfig(modifier)
    }

    fun startSession() {
        if (!sessionManager.sessionState.value.isActive) {
            sessionManager.toggleSession()
        }
    }

    suspend fun stopAndSaveSession() {
        if (!sessionManager.sessionState.value.isActive) return
        
        val session = sessionManager.sessionState.value
        val config = session.config
        val workspace = _uiState.value.activeWorkspace
        val sessionTitle = "[$workspace] ${config.type} - ${config.group} - W${config.week}"
        
        lastSavedSessionTitle = sessionTitle
        lastSavedAttendeeCount = session.scannedAttendees.size
        
        val entries = mutableListOf<AttendeeEntry>()
        for ((nationalId, timestamp) in session.scannedAttendees) {
            val student = studentDao.getStudentById(nationalId)
            if (student != null) {
                entries.add(AttendeeEntry(student.name, nationalId, timestamp))
            }
            val attendance = Attendance(
                nationalId = nationalId,
                sessionName = sessionTitle,
                timestamp = timestamp
            )
            attendanceDao.insertAttendance(attendance)
        }
        lastSavedAttendees = entries
        
        // Turn off session but keep the UI state clean
        sessionManager.clearSession()
    }

    fun onQrScanned(payload: String) {
        val session = sessionManager.sessionState.value
        if (!session.isActive || session.isProcessing) return
        sessionManager.setProcessingScan(true)
        viewModelScope.launch {
            val result = qrValidator.validatePayload(payload)
            if (result.isSuccess) {
                processAttendance(result.getOrNull()!!)
            } else {
                feedbackManager.playErrorFeedback()
                sessionManager.setLastScannedMessage("Error: Invalid QR Code")
                resetScanState()
            }
        }
    }

    fun onManualEntry(nationalId: String) {
        val session = sessionManager.sessionState.value
        if (!session.isActive || session.isProcessing) return
        sessionManager.setProcessingScan(true)
        viewModelScope.launch { processAttendance(nationalId) }
    }

    fun onBatchSubmit(nationalIds: List<String>) {
        val session = sessionManager.sessionState.value
        if (!session.isActive) return
        viewModelScope.launch {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            var addedCount = 0
            for (id in nationalIds) {
                val student = studentDao.getStudentById(id)
                if (student != null) {
                    sessionManager.addAttendee(id, timestamp)
                    addedCount++
                }
            }
            if (addedCount > 0) {
                feedbackManager.playSuccessFeedback()
                sessionManager.setLastScannedMessage("Success: Added $addedCount students")
            }
            resetScanState()
        }
    }

    fun onBatchExcuse(nationalIds: List<String>, reason: String) {
        val session = sessionManager.sessionState.value
        if (!session.isActive) return
        val sessionTitle = "[${_uiState.value.activeWorkspace}] ${session.config.type} - ${session.config.group} - W${session.config.week}"
        viewModelScope.launch {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            var excusedCount = 0
            for (id in nationalIds) {
                val student = studentDao.getStudentById(id)
                if (student != null) {
                    val attendance = com.attendo.android.data.local.Attendance(
                        nationalId = id,
                        sessionName = sessionTitle,
                        timestamp = timestamp,
                        isExcused = 1,
                        excuseReason = reason
                    )
                    attendanceDao.insertAttendance(attendance)
                    excusedCount++
                }
            }
            if (excusedCount > 0) {
                feedbackManager.playSuccessFeedback()
                sessionManager.setLastScannedMessage("Success: Excused $excusedCount students")
            }
            resetScanState()
        }
    }

    private suspend fun processAttendance(nationalId: String) {
        val student = studentDao.getStudentById(nationalId)
        if (student != null) {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            sessionManager.addAttendee(nationalId, timestamp)
            feedbackManager.playSuccessFeedback()
            sessionManager.setLastScannedMessage("Success: ${student.name}")
        } else {
            feedbackManager.playErrorFeedback()
            sessionManager.setLastScannedMessage("Error: Student Not Found")
        }
        resetScanState()
    }

    private suspend fun resetScanState() {
        kotlinx.coroutines.delay(1500)
        sessionManager.setProcessingScan(false)
        sessionManager.setLastScannedMessage(null)
    }

    fun triggerColdCall() {
        val session = sessionManager.sessionState.value
        if (!session.isActive) return
        
        val attendees = session.scannedAttendees.keys.toList()
        if (attendees.isNotEmpty()) {
            val randomId = attendees.random()
            _uiState.value = _uiState.value.copy(coldCallStudentId = randomId)
            feedbackManager.playSuccessFeedback()
        }
    }

    fun dismissColdCall() {
        _uiState.value = _uiState.value.copy(coldCallStudentId = null)
    }

    fun exportSessionAsPdf(context: Context, uri: Uri) {
        viewModelScope.launch {
            SessionExporter.exportPdf(context, uri, lastSavedSessionTitle, lastSavedAttendees)
        }
    }

    fun exportSessionAsXlsx(context: Context, uri: Uri) {
        viewModelScope.launch {
            SessionExporter.exportXlsx(context, uri, lastSavedSessionTitle, lastSavedAttendees)
        }
    }
}

data class ScannerUiState(
    val isSessionActive: Boolean = false,
    val sessionConfig: SessionConfig = SessionConfig(),
    val instructorName: String = "",
    val subjectName: String = "",
    val activeWorkspace: String = "",
    val workspaceStudents: List<Student> = emptyList(),
    val workspaceStudentCount: Int = 0,
    val attendeeCount: Int = 0,
    val scannedAttendeesMap: Map<String, String> = emptyMap(),
    val isProcessingScan: Boolean = false,
    val lastScannedMessage: String? = null,
    val coldCallStudentId: String? = null
)
