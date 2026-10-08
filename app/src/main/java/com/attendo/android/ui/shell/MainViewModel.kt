package com.attendo.android.ui.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendo.android.data.local.SettingsDao
import com.attendo.android.security.HardwareIdGenerator
import com.attendo.android.security.JwtValidator
import com.attendo.android.security.LicenseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val jwtValidator: JwtValidator,
    private val hardwareIdGenerator: HardwareIdGenerator,
    private val settingsDao: SettingsDao
) : ViewModel() {
    private val _activeWorkspace = MutableStateFlow<String?>(null)
    val activeWorkspace = _activeWorkspace.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _daysLeftWarning = MutableStateFlow<Long?>(null)
    val daysLeftWarning = _daysLeftWarning.asStateFlow()

    init {
        viewModelScope.launch {
            val hwId = hardwareIdGenerator.generateHardwareId()
            val token = settingsDao.getSetting("license_token")
            if (!token.isNullOrBlank()) {
                val result = jwtValidator.validateLicense(token, hwId)
                if (result is LicenseResult.ExpiringSoon) {
                    _daysLeftWarning.value = result.daysLeft
                }
            }
        }
    }

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
