package com.attendo.android.ui.setup

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState

    fun onUniversityChanged(value: String) {
        _uiState.value = _uiState.value.copy(university = value)
    }

    fun onFacultyChanged(value: String) {
        _uiState.value = _uiState.value.copy(faculty = value)
    }
    
    fun onCompleteSetup() {
        // Save to DB via repository in next steps
        _uiState.value = _uiState.value.copy(isComplete = true)
    }
}

data class SetupUiState(
    val university: String = "",
    val faculty: String = "",
    val isComplete: Boolean = false
)
