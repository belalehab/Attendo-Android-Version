package com.attendo.android.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendo.android.data.local.Attendance
import com.attendo.android.data.local.AttendanceDao
import com.attendo.android.data.local.SettingsDao
import com.attendo.android.data.local.StudentDao
import com.attendo.android.domain.usecase.QRValidator
import com.attendo.android.utils.HardwareFeedbackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val settingsDao: SettingsDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState

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
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            _uiState.value = _uiState.value.copy(
                instructorName = instructor,
                subjectName = subjectName,
                activeWorkspace = activeWorkspace
            )
        }
    }

    fun updateConfig(modifier: (SessionConfig) -> SessionConfig) {
        _uiState.value = _uiState.value.copy(sessionConfig = modifier(_uiState.value.sessionConfig))
    }

    fun toggleSession() {
        val currentlyActive = _uiState.value.isSessionActive
        _uiState.value = _uiState.value.copy(isSessionActive = !currentlyActive)
    }

    fun onQrScanned(payload: String) {
        if (!_uiState.value.isSessionActive || _uiState.value.isProcessingScan) return
        _uiState.value = _uiState.value.copy(isProcessingScan = true)
        
        viewModelScope.launch {
            val result = qrValidator.validatePayload(payload)
            if (result.isSuccess) {
                processAttendance(result.getOrNull()!!)
            } else {
                feedbackManager.playErrorFeedback()
                _uiState.value = _uiState.value.copy(lastScannedMessage = "Error: Invalid QR Code")
                resetScanState()
            }
        }
    }

    fun onManualEntry(nationalId: String) {
        if (!_uiState.value.isSessionActive || _uiState.value.isProcessingScan) return
        _uiState.value = _uiState.value.copy(isProcessingScan = true)
        
        viewModelScope.launch {
            processAttendance(nationalId)
        }
    }

    private suspend fun processAttendance(nationalId: String) {
        val student = studentDao.getStudentById(nationalId)
        val config = _uiState.value.sessionConfig
        val sessionTitle = "${config.type} - ${config.group} - W${config.week}"
        
        if (student != null) {
            val attendance = Attendance(
                nationalId = nationalId,
                sessionName = sessionTitle,
                timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            )
            attendanceDao.insertAttendance(attendance)
            feedbackManager.playSuccessFeedback()
            _uiState.value = _uiState.value.copy(lastScannedMessage = "Success: ${student.name}")
        } else {
            feedbackManager.playErrorFeedback()
            _uiState.value = _uiState.value.copy(lastScannedMessage = "Error: Student Not Found")
        }
        resetScanState()
    }

    private suspend fun resetScanState() {
        kotlinx.coroutines.delay(1500)
        _uiState.value = _uiState.value.copy(isProcessingScan = false, lastScannedMessage = null)
    }
}

data class SessionConfig(
    val type: String = "Lecture",
    val group: String = "All Groups",
    val week: String = "1",
    val topic: String = ""
)

data class ScannerUiState(
    val isSessionActive: Boolean = false,
    val sessionConfig: SessionConfig = SessionConfig(),
    val instructorName: String = "",
    val subjectName: String = "",
    val activeWorkspace: String = "",
    val isProcessingScan: Boolean = false,
    val lastScannedMessage: String? = null
)
