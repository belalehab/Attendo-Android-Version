package com.attendo.android.ui.workspace

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
class WorkspaceViewModel @Inject constructor(
    private val settingsDao: SettingsDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkspaceUiState())
    val uiState: StateFlow<WorkspaceUiState> = _uiState

    init {
        loadWorkspaces()
    }

    fun loadWorkspaces() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val gradesString = settingsDao.getSetting("grades") ?: "[]"
                val subjectsString = settingsDao.getSetting("subject_name") ?: "{}"

                val gradesArray = JSONArray(gradesString)
                val subjectsObj = JSONObject(subjectsString)

                val workspaces = mutableListOf<WorkspaceItem>()
                for (i in 0 until gradesArray.length()) {
                    val grade = gradesArray.getString(i)
                    val subject = if (subjectsObj.has(grade)) subjectsObj.getString(grade) else "Subject"
                    workspaces.add(WorkspaceItem(grade, subject))
                }

                _uiState.value = _uiState.value.copy(
                    workspaces = workspaces,
                    isLoading = false
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    workspaces = emptyList(),
                    isLoading = false
                )
            }
        }
    }

    fun saveWorkspaces(workspaces: List<WorkspaceItem>, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val validWorkspaces = workspaces.filter { it.grade.isNotBlank() }
            if (validWorkspaces.isEmpty()) {
                onResult(false, "You must have at least one valid workspace with an ID!")
                return@launch
            }

            val gradesList = validWorkspaces.map { it.grade.trim() }
            if (gradesList.toSet().size != gradesList.size) {
                onResult(false, "Workspace IDs must be unique!")
                return@launch
            }

            try {
                val subjectsString = settingsDao.getSetting("subject_name") ?: "{}"
                val oldSubjects = JSONObject(subjectsString)
                val newSubjects = JSONObject()
                
                // Copy old ones first so we never permanently lose a deleted workspace's subject name
                val keys = oldSubjects.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    newSubjects.put(key, oldSubjects.getString(key))
                }
                
                // Overwrite with new changes
                validWorkspaces.forEach { w ->
                    val id = w.grade.trim()
                    val subject = w.subject.trim().takeIf { it.isNotBlank() } 
                        ?: (if (oldSubjects.has(id)) oldSubjects.getString(id) else "Unknown Subject")
                    newSubjects.put(id, subject)
                }

                val newGradesJson = JSONArray(gradesList).toString()
                
                settingsDao.saveSetting(Setting("grades", newGradesJson))
                settingsDao.saveSetting(Setting("subject_name", newSubjects.toString()))
                
                loadWorkspaces() // reload the UI
                onResult(true, "Workspaces updated successfully!")
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, "Failed to save workspaces.")
            }
        }
    }
}

data class WorkspaceItem(
    val grade: String,
    val subject: String
)

data class WorkspaceUiState(
    val workspaces: List<WorkspaceItem> = emptyList(),
    val isLoading: Boolean = true
)
