package com.attendo.android.ui.licensing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendo.android.data.local.Setting
import com.attendo.android.data.local.SettingsDao
import com.attendo.android.security.HardwareIdGenerator
import com.attendo.android.security.JwtValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LicenseViewModel @Inject constructor(
    private val hardwareIdGenerator: HardwareIdGenerator,
    private val jwtValidator: JwtValidator,
    private val settingsDao: SettingsDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(LicenseUiState())
    val uiState: StateFlow<LicenseUiState> = _uiState

    init {
        val hwId = hardwareIdGenerator.generateHardwareId()
        _uiState.value = _uiState.value.copy(hardwareId = hwId)
        
        viewModelScope.launch {
            val savedToken = settingsDao.getSetting("license_token")
            if (!savedToken.isNullOrBlank()) {
                val result = jwtValidator.validateLicense(savedToken, hwId)
                if (result.isSuccess) {
                    val plan = result.getOrNull() ?: "Free"
                    _uiState.value = _uiState.value.copy(isValid = true, planType = plan, tokenInput = savedToken)
                }
            }
        }
    }

    fun onTokenChanged(token: String) {
        _uiState.value = _uiState.value.copy(tokenInput = token, errorMessage = null)
    }

    fun activateLicense() {
        val currentToken = _uiState.value.tokenInput
        val hwId = _uiState.value.hardwareId
        
        val result = jwtValidator.validateLicense(currentToken, hwId)
        if (result.isSuccess) {
            val plan = result.getOrNull() ?: "Free"
            viewModelScope.launch {
                settingsDao.saveSetting(Setting("license_token", currentToken))
                _uiState.value = _uiState.value.copy(isValid = true, planType = plan)
            }
        } else {
            _uiState.value = _uiState.value.copy(errorMessage = result.exceptionOrNull()?.message ?: "Invalid License")
        }
    }
}

data class LicenseUiState(
    val hardwareId: String = "",
    val tokenInput: String = "",
    val isValid: Boolean = false,
    val planType: String? = null,
    val errorMessage: String? = null
)
