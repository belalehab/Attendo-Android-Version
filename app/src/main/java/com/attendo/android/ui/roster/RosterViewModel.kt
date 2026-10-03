package com.attendo.android.ui.roster

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendo.android.data.local.Student
import com.attendo.android.data.local.StudentDao
import com.attendo.android.utils.CsvParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RosterViewModel @Inject constructor(
    private val studentDao: StudentDao,
    private val csvParser: CsvParser
) : ViewModel() {

    private val _uiState = MutableStateFlow(RosterUiState())
    val uiState: StateFlow<RosterUiState> = _uiState

    private var currentGrade: String = ""
    private var observeJob: Job? = null

    fun loadStudents(grade: String) {
        if (currentGrade == grade) return
        currentGrade = grade
        
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            studentDao.getActiveStudentsByGrade(grade).collect { students ->
                _uiState.value = _uiState.value.copy(students = students)
            }
        }
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

    fun deleteStudentPermanently(student: Student) {
        viewModelScope.launch {
            studentDao.deleteStudent(student)
        }
    }

    fun importCsv(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true)
            val result = csvParser.parseStudents(uri, currentGrade)
            if (result.isSuccess) {
                studentDao.insertStudents(result.getOrNull()!!)
            }
            _uiState.value = _uiState.value.copy(isImporting = false)
        }
    }
}

data class RosterUiState(
    val students: List<Student> = emptyList(),
    val isImporting: Boolean = false
)
