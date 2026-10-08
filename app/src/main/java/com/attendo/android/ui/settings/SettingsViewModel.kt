package com.attendo.android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendo.android.data.local.Setting
import com.attendo.android.data.local.SettingsDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDao: SettingsDao
) : ViewModel() {

    private val _instructorName = MutableStateFlow("")
    val instructorName: StateFlow<String> = _instructorName

    // We can store subjects as a raw JSON string or map
    private val _subjectsMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val subjectsMap: StateFlow<Map<String, String>> = _subjectsMap

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val name = settingsDao.getSetting("instructor_name") ?: ""
            _instructorName.value = name

            val subjectsJson = settingsDao.getSetting("subject_name") ?: "{}"
            try {
                val obj = JSONObject(subjectsJson)
                val map = mutableMapOf<String, String>()
                obj.keys().forEach { key ->
                    map[key] = obj.getString(key)
                }
                _subjectsMap.value = map
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveInstructorName(name: String) {
        viewModelScope.launch {
            _instructorName.value = name
            settingsDao.saveSetting(Setting("instructor_name", name))
        }
    }

    fun saveSubject(workspace: String, subject: String) {
        viewModelScope.launch {
            val updatedMap = _subjectsMap.value.toMutableMap()
            updatedMap[workspace] = subject
            _subjectsMap.value = updatedMap
            
            val json = JSONObject(updatedMap as Map<*, *>).toString()
            settingsDao.saveSetting(Setting("subject_name", json))
        }
    }
}
