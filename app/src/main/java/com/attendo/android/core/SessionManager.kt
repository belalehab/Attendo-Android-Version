package com.attendo.android.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class SessionConfig(
    val type: String = "Lecture",
    val group: String = "All Groups",
    val week: String = "1",
    val topic: String = ""
)

data class SessionState(
    val isActive: Boolean = false,
    val config: SessionConfig = SessionConfig(),
    val scannedAttendees: Map<String, String> = emptyMap(),
    val lastMessage: String? = null,
    val isProcessing: Boolean = false
)

@Singleton
class SessionManager @Inject constructor() {
    private val _sessionState = MutableStateFlow(SessionState())
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    fun toggleSession() {
        _sessionState.update { current ->
            if (!current.isActive) {
                // starting fresh
                current.copy(isActive = true, scannedAttendees = emptyMap(), lastMessage = null)
            } else {
                current.copy(isActive = false)
            }
        }
    }

    fun updateConfig(modifier: (SessionConfig) -> SessionConfig) {
        _sessionState.update { it.copy(config = modifier(it.config)) }
    }

    fun addAttendee(nationalId: String, timestamp: String) {
        _sessionState.update { current ->
            val newMap = current.scannedAttendees.toMutableMap()
            newMap[nationalId] = timestamp
            current.copy(scannedAttendees = newMap)
        }
    }

    fun setLastScannedMessage(msg: String?) {
        _sessionState.update { it.copy(lastMessage = msg) }
    }
    
    fun setProcessingScan(isProcessing: Boolean) {
        _sessionState.update { it.copy(isProcessing = isProcessing) }
    }

    fun clearSession() {
        _sessionState.update { 
            it.copy(
                isActive = false,
                scannedAttendees = emptyMap(),
                lastMessage = null,
                isProcessing = false
            )
        }
    }
}
