package com.attendo.android.ui.licensing

import androidx.lifecycle.ViewModel
import com.attendo.android.security.HardwareIdGenerator
import com.attendo.android.security.JwtValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class LicenseViewModel @Inject constructor(
    private val hardwareIdGenerator: HardwareIdGenerator,
    private val jwtValidator: JwtValidator
) : ViewModel() {

    private val _uiState = MutableStateFlow(LicenseUiState())
    val uiState: StateFlow<LicenseUiState> = _uiState

    init {
        _uiState.value = _uiState.value.copy(
            hardwareId = hardwareIdGenerator.generateHardwareId()
        )
    }

    fun onTokenChanged(token: String) {
        _uiState.value = _uiState.value.copy(tokenInput = token, errorMessage = null)
    }

    fun activateLicense() {
        val currentToken = _uiState.value.tokenInput
        val hwId = _uiState.value.hardwareId
        
        val result = jwtValidator.validateLicense(currentToken, hwId)
        if (result.isSuccess) {
            _uiState.value = _uiState.value.copy(isValid = true)
        } else {
            _uiState.value = _uiState.value.copy(errorMessage = result.exceptionOrNull()?.message ?: "Invalid License")
        }
    }
}

data class LicenseUiState(
    val hardwareId: String = "",
    val tokenInput: String = "",
    val isValid: Boolean = false,
    val errorMessage: String? = null
)
