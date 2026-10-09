package com.attendo.android.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendo.android.data.local.Setting
import com.attendo.android.data.local.SettingsDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val settingsDao: SettingsDao
) : ViewModel() {
    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState

    init {
        viewModelScope.launch {
            val instructorName = settingsDao.getSetting("instructor_name") ?: ""
            val semester = settingsDao.getSetting("semester") ?: "First Semester"
            val maxGroups = settingsDao.getSetting("total_groups")?.toIntOrNull() ?: 5
            val university = settingsDao.getSetting("university_name") ?: ""
            val faculty = settingsDao.getSetting("faculty_name") ?: ""
            val startDate = settingsDao.getSetting("semester_start") ?: ""
            val endDate = settingsDao.getSetting("semester_end") ?: ""
            
            val gradesStr = settingsDao.getSetting("grades") ?: "[]"
            val subjectsStr = settingsDao.getSetting("subject_name") ?: "{}"
            
            val grades = mutableSetOf<String>()
            val subjects = mutableMapOf<String, String>()
            
            try {
                val gradesJson = JSONArray(gradesStr)
                for (i in 0 until gradesJson.length()) {
                    grades.add(gradesJson.getString(i))
                }
                
                val subjectsJson = JSONObject(subjectsStr)
                val keys = subjectsJson.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    subjects[key] = subjectsJson.getString(key)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            _uiState.value = _uiState.value.copy(
                instructorName = instructorName,
                semester = semester,
                maxGroups = maxGroups,
                university = university,
                faculty = faculty,
                startDate = startDate,
                endDate = endDate,
                grades = grades,
                subjects = subjects
            )
        }
    }

    fun updateField(modifier: (SetupUiState) -> SetupUiState) {
        _uiState.value = modifier(_uiState.value)
    }

    fun toggleGrade(grade: String) {
        val currentGrades = _uiState.value.grades.toMutableSet()
        val currentSubjects = _uiState.value.subjects.toMutableMap()
        if (currentGrades.contains(grade)) {
            currentGrades.remove(grade)
            currentSubjects.remove(grade)
        } else {
            currentGrades.add(grade)
        }
        _uiState.value = _uiState.value.copy(grades = currentGrades, subjects = currentSubjects)
    }

    fun updateSubject(grade: String, subject: String) {
        val currentSubjects = _uiState.value.subjects.toMutableMap()
        currentSubjects[grade] = subject
        _uiState.value = _uiState.value.copy(subjects = currentSubjects)
    }

    fun isValid(): Boolean {
        val s = _uiState.value
        if (s.instructorName.isBlank() || s.university.isBlank() || s.faculty.isBlank() ||
            s.startDate.isBlank() || s.endDate.isBlank() || s.grades.isEmpty()
        ) return false

        if (s.grades.any { s.subjects[it].isNullOrBlank() }) return false
        return true
    }

    fun completeSetup() {
        viewModelScope.launch {
            val s = _uiState.value
            
            val gradesJson = JSONArray(s.grades).toString()
            val subjectsJson = JSONObject(s.subjects as Map<*, *>).toString()

            settingsDao.saveSetting(Setting("setup_complete", "true"))
            settingsDao.saveSetting(Setting("instructor_name", s.instructorName.trim()))
            settingsDao.saveSetting(Setting("semester", s.semester))
            settingsDao.saveSetting(Setting("total_groups", s.maxGroups.toString()))
            settingsDao.saveSetting(Setting("grades", gradesJson))
            settingsDao.saveSetting(Setting("subject_name", subjectsJson))
            settingsDao.saveSetting(Setting("university_name", s.university.trim()))
            settingsDao.saveSetting(Setting("faculty_name", s.faculty.trim()))
            settingsDao.saveSetting(Setting("semester_start", s.startDate))
            settingsDao.saveSetting(Setting("semester_end", s.endDate))

            _uiState.value = s.copy(isComplete = true)
        }
    }
}

data class SetupUiState(
    val instructorName: String = "",
    val semester: String = "First Semester",
    val maxGroups: Int = 5,
    val grades: Set<String> = emptySet(),
    val subjects: Map<String, String> = emptyMap(),
    val university: String = "",
    val faculty: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val isComplete: Boolean = false
)
