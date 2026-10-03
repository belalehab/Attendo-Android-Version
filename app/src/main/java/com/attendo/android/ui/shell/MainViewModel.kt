package com.attendo.android.ui.shell

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {
    private val _activeWorkspace = MutableStateFlow<String?>(null)
    val activeWorkspace = _activeWorkspace.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    fun setWorkspace(workspace: String) {
        _activeWorkspace.value = workspace
    }

    fun clearWorkspace() {
        _activeWorkspace.value = null
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }
}
