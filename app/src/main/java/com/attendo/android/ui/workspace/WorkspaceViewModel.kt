package com.attendo.android.ui.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
}

data class WorkspaceItem(
    val grade: String,
    val subject: String
)

data class WorkspaceUiState(
    val workspaces: List<WorkspaceItem> = emptyList(),
    val isLoading: Boolean = true
)
