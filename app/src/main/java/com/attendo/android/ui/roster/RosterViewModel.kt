package com.attendo.android.ui.roster

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendo.android.data.local.Student
import com.attendo.android.data.local.StudentDao
import com.attendo.android.utils.CsvParser
import com.attendo.android.utils.QRGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RosterViewModel @Inject constructor(
    private val studentDao: StudentDao,
    private val csvParser: CsvParser
) : ViewModel() {

    private val _isImporting = MutableStateFlow(false)
    private val _isArchiveView = MutableStateFlow(false)
    private val _activeStudents = MutableStateFlow<List<Student>>(emptyList())
    private val _archivedStudents = MutableStateFlow<List<Student>>(emptyList())

    val uiState: StateFlow<RosterUiState> = combine(
        _isImporting,
        _isArchiveView,
        _activeStudents,
        _archivedStudents
    ) { importing, isArchive, active, archived ->
        RosterUiState(
            students = if (isArchive) archived else active,
            isImporting = importing,
            isArchiveView = isArchive,
            activeCount = active.size
        )
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), RosterUiState())

    private var currentGrade: String = ""
    private var activeJob: Job? = null
    private var archivedJob: Job? = null

    fun loadStudents(grade: String) {
        if (currentGrade == grade) return
        currentGrade = grade
        
        activeJob?.cancel()
        archivedJob?.cancel()
        
        activeJob = viewModelScope.launch {
            studentDao.getActiveStudentsByGrade(grade).collect { students ->
                _activeStudents.value = students
            }
        }
        archivedJob = viewModelScope.launch {
            studentDao.getArchivedStudentsByGrade(grade).collect { students ->
                _archivedStudents.value = students
            }
        }
    }

    fun setArchiveView(isArchive: Boolean) {
        _isArchiveView.value = isArchive
    }

    fun addStudent(name: String, nationalId: String) {
        viewModelScope.launch {
            val student = Student(name = name, nationalId = nationalId, grade = currentGrade)
            studentDao.insertStudent(student)
        }
    }

    fun editStudent(student: Student, newName: String, newNationalId: String) {
        viewModelScope.launch {
            val updated = student.copy(name = newName, nationalId = newNationalId)
            studentDao.updateStudent(updated)
        }
    }

    fun archiveStudent(student: Student) {
        viewModelScope.launch {
            val archived = student.copy(isDeleted = 1)
            studentDao.updateStudent(archived)
        }
    }
    
    fun unarchiveStudent(student: Student) {
        viewModelScope.launch {
            val unarchived = student.copy(isDeleted = 0)
            studentDao.updateStudent(unarchived)
        }
    }

    fun deleteStudentPermanently(student: Student) {
        viewModelScope.launch {
            studentDao.deleteStudent(student)
        }
    }

    fun importCsv(uri: Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            val result = csvParser.parseStudents(uri, currentGrade)
            if (result.isSuccess) {
                studentDao.insertStudents(result.getOrNull()!!)
            }
            _isImporting.value = false
        }
    }

    fun exportTemplate(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write("Name,NationalID\n".toByteArray())
                    outputStream.write("John Doe,12345678901234\n".toByteArray())
                    outputStream.write("Jane Smith,22345678901234\n".toByteArray())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun exportQRs(uri: Uri, context: Context, selectedStudents: List<Student>) {
        viewModelScope.launch {
            QRGenerator.generateQRsPdf(context, uri, selectedStudents)
        }
    }
}

data class RosterUiState(
    val students: List<Student> = emptyList(),
    val isImporting: Boolean = false,
    val isArchiveView: Boolean = false,
    val activeCount: Int = 0
)
