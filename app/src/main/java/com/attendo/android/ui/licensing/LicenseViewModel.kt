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

import com.attendo.android.security.LicenseResult

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
                handleLicenseResult(result, savedToken)
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
        viewModelScope.launch {
            if (result is LicenseResult.Valid || result is LicenseResult.ExpiringSoon) {
                settingsDao.saveSetting(Setting("license_token", currentToken))
            }
            handleLicenseResult(result, currentToken)
        }
    }

    private fun handleLicenseResult(result: LicenseResult, token: String) {
        when (result) {
            is LicenseResult.Valid -> {
                _uiState.value = _uiState.value.copy(
                    isValid = true, planType = result.plan, tokenInput = token, 
                    daysLeft = result.daysLeft, isExpiringSoon = false, errorMessage = null
                )
            }
            is LicenseResult.ExpiringSoon -> {
                _uiState.value = _uiState.value.copy(
                    isValid = true, planType = result.plan, tokenInput = token, 
                    daysLeft = result.daysLeft, isExpiringSoon = true, errorMessage = null
                )
            }
            is LicenseResult.Expired -> {
                _uiState.value = _uiState.value.copy(isValid = false, errorMessage = "License Expired")
            }
            is LicenseResult.Tampered -> {
                _uiState.value = _uiState.value.copy(isValid = false, errorMessage = "License Tampered")
            }
            is LicenseResult.Invalid -> {
                _uiState.value = _uiState.value.copy(isValid = false, errorMessage = "Invalid License")
            }
        }
    }
}

data class LicenseUiState(
    val hardwareId: String = "",
    val tokenInput: String = "",
    val isValid: Boolean = false,
    val planType: String? = null,
    val daysLeft: Long? = null,
    val isExpiringSoon: Boolean = false,
    val errorMessage: String? = null
)
