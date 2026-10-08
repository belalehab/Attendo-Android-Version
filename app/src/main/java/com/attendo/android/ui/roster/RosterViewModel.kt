package com.attendo.android.ui.roster

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendo.android.data.local.Attendance
import com.attendo.android.data.local.AttendanceDao
import com.attendo.android.data.local.Student
import com.attendo.android.data.local.StudentDao
import com.attendo.android.utils.CsvParser
import com.attendo.android.utils.HardwareFeedbackManager
import com.attendo.android.utils.QRGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimelineRecord(
    val sessionName: String,
    val status: String, // "ATTENDED", "EXCUSED", "ABSENT"
    val timestamp: String?
)

data class StudentProfileData(
    val student: Student,
    val attendedCount: Int,
    val excusedCount: Int,
    val absentCount: Int,
    val bonusPoints: Int,
    val timeline: List<TimelineRecord>
)

@HiltViewModel
class RosterViewModel @Inject constructor(
    private val studentDao: StudentDao,
    private val attendanceDao: AttendanceDao,
    private val csvParser: CsvParser,
    private val feedbackManager: HardwareFeedbackManager
) : ViewModel() {

    private val _isArchiveView = MutableStateFlow(false)
    private val _isImporting = MutableStateFlow(false)
    private val _activeWorkspace = MutableStateFlow<String?>(null)
    private val _students = MutableStateFlow<List<Student>>(emptyList())
    private val _activeCount = MutableStateFlow(0)
    
    private val _studentProfile = MutableStateFlow<StudentProfileData?>(null)
    val studentProfile: StateFlow<StudentProfileData?> = _studentProfile

    val uiState: StateFlow<RosterUiState> = combine(
        _isArchiveView,
        _isImporting,
        _activeWorkspace,
        _students,
        _activeCount
    ) { isArchive, isImporting, workspace, students, count ->
        RosterUiState(
            isArchiveView = isArchive,
            isImporting = isImporting,
            activeWorkspace = workspace,
            students = students,
            activeCount = count
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RosterUiState())

    private var activeJob: Job? = null
    private var archivedJob: Job? = null

    fun setArchiveView(isArchive: Boolean) {
        _isArchiveView.value = isArchive
        _activeWorkspace.value?.let { loadStudents(it) }
    }

    fun loadStudents(workspace: String) {
        _activeWorkspace.value = workspace
        
        activeJob?.cancel()
        archivedJob?.cancel()
        
        activeJob = viewModelScope.launch {
            studentDao.getActiveStudentsByGrade(workspace).collect { activeList ->
                _activeCount.value = activeList.size
                if (!_isArchiveView.value) {
                    _students.value = activeList
                }
            }
        }
        
        archivedJob = viewModelScope.launch {
            studentDao.getArchivedStudentsByGrade(workspace).collect { archivedList ->
                if (_isArchiveView.value) {
                    _students.value = archivedList
                }
            }
        }
    }

    fun addStudent(name: String, nationalId: String) {
        val workspace = _activeWorkspace.value ?: return
        viewModelScope.launch {
            val student = Student(
                name = name,
                nationalId = nationalId,
                grade = workspace,
                isDeleted = 0
            )
            studentDao.insertStudent(student)
            feedbackManager.playSuccessFeedback()
        }
    }

    fun editStudent(student: Student, newName: String, newId: String) {
        viewModelScope.launch {
            val updated = student.copy(name = newName, nationalId = newId)
            studentDao.updateStudent(updated)
            feedbackManager.playSuccessFeedback()
        }
    }

    fun archiveStudent(student: Student) {
        viewModelScope.launch {
            val updated = student.copy(isDeleted = 1)
            studentDao.updateStudent(updated)
            feedbackManager.playSuccessFeedback()
        }
    }

    fun restoreStudent(student: Student) {
        viewModelScope.launch {
            val updated = student.copy(isDeleted = 0)
            studentDao.updateStudent(updated)
            feedbackManager.playSuccessFeedback()
        }
    }

    fun deleteStudentPermanently(student: Student) {
        viewModelScope.launch {
            studentDao.deleteStudent(student)
            feedbackManager.playSuccessFeedback()
        }
    }

    fun exportTemplate(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val csvContent = "Name,ID,Grade\nاحمد محمد,12345678901234,1\n"
                    outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
                }
                feedbackManager.playSuccessFeedback()
            } catch (e: Exception) {
                e.printStackTrace()
                feedbackManager.playErrorFeedback()
            }
        }
    }

    fun importCsv(uri: Uri) {
        val workspace = _activeWorkspace.value ?: return
        viewModelScope.launch {
            _isImporting.value = true
            val result = csvParser.parseStudents(uri, workspace)
            if (result.isSuccess) {
                studentDao.insertStudents(result.getOrNull()!!)
                feedbackManager.playSuccessFeedback()
            } else {
                feedbackManager.playErrorFeedback()
            }
            _isImporting.value = false
        }
    }

    fun exportQRs(uri: Uri, context: Context, students: List<Student>) {
        viewModelScope.launch {
            QRGenerator.generateQrsZip(context, uri, students)
            feedbackManager.playSuccessFeedback()
        }
    }

    fun openStudentProfile(student: Student) {
        val workspace = _activeWorkspace.value ?: return
        viewModelScope.launch {
            val nid = student.nationalId ?: return@launch
            
            // Fetch all past active sessions in this workspace
            val allSessions = attendanceDao.getActiveSessionsList("$workspace%")
            
            // Fetch student's attendance records in this workspace
            val records = attendanceDao.getStudentAttendanceList(nid, "$workspace%")
            
            val attendedRecords = mutableListOf<TimelineRecord>()
            var attendedCount = 0
            var excusedCount = 0
            var bonusPoints = 0
            
            val recordMap = records.associateBy { it.sessionName }
            
            // Process attended/excused
            records.forEach { record ->
                if (record.isArchived == 0) {
                    val status = if (record.isExcused == 1) "EXCUSED" else "ATTENDED"
                    if (status == "ATTENDED") attendedCount++
                    if (status == "EXCUSED") excusedCount++
                    bonusPoints += (record.bonusPoints ?: 0)
                    
                    attendedRecords.add(
                        TimelineRecord(
                            sessionName = record.sessionName ?: "Unknown",
                            status = status,
                            timestamp = record.timestamp
                        )
                    )
                }
            }
            
            // Process absents
            val absentRecords = mutableListOf<TimelineRecord>()
            allSessions.forEach { session ->
                if (!recordMap.containsKey(session)) {
                    absentRecords.add(
                        TimelineRecord(
                            sessionName = session,
                            status = "ABSENT",
                            timestamp = null
                        )
                    )
                }
            }
            
            // Combine and sort chronologically (descending)
            // Desktop groups by attended/absent, but we can provide a flat list or let UI handle it. 
            // We'll provide all of them so the UI can show the timeline.
            val fullTimeline = (attendedRecords + absentRecords).sortedByDescending { it.sessionName }

            _studentProfile.value = StudentProfileData(
                student = student,
                attendedCount = attendedCount,
                excusedCount = excusedCount,
                absentCount = absentRecords.size,
                bonusPoints = bonusPoints,
                timeline = fullTimeline
            )
        }
    }

    fun clearStudentProfile() {
        _studentProfile.value = null
    }
}

data class RosterUiState(
    val isArchiveView: Boolean = false,
    val isImporting: Boolean = false,
    val activeWorkspace: String? = null,
    val students: List<Student> = emptyList(),
    val activeCount: Int = 0
)
