package com.attendo.android.ui.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendo.android.data.local.Setting
import com.attendo.android.data.local.SettingsDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val settingsDao: SettingsDao
) : ViewModel() {

    private val _hasSeenWelcome = MutableStateFlow<Boolean?>(null)
    val hasSeenWelcome: StateFlow<Boolean?> = _hasSeenWelcome

    init {
        checkWelcomeStatus()
    }

    private fun checkWelcomeStatus() {
        viewModelScope.launch {
            val seen = settingsDao.getSetting("has_seen_welcome")
            _hasSeenWelcome.value = seen == "true"
        }
    }

    fun setHasSeenWelcome() {
        viewModelScope.launch {
            settingsDao.saveSetting(Setting("has_seen_welcome", "true"))
            _hasSeenWelcome.value = true
        }
    }
}
