package com.attendo.android.ui.vault

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendo.android.data.local.AppDatabase
import com.attendo.android.domain.usecase.MergeConflict
import com.attendo.android.domain.usecase.MergeUseCase
import com.attendo.android.utils.DatabaseManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val databaseManager: DatabaseManager,
    private val mergeUseCase: MergeUseCase,
    private val appDatabase: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState

    private var pendingImportFile: java.io.File? = null

    fun exportDatabase(destinationUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, statusMessage = null)
            val result = databaseManager.exportDatabase(destinationUri)
            _uiState.value = _uiState.value.copy(
                isProcessing = false,
                statusMessage = if (result.isSuccess) "Backup Exported Successfully" else "Export Failed: ${result.exceptionOrNull()?.message}"
            )
        }
    }

    fun importAndMerge(sourceUri: Uri, activeWorkspace: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, statusMessage = null)
            
            val tempFileResult = databaseManager.createTempImport(sourceUri)
            if (tempFileResult.isSuccess) {
                val tempFile = tempFileResult.getOrNull()!!
                val mergeResult = mergeUseCase.mergeDatabase(tempFile, activeWorkspace)
                
                if (mergeResult.isSuccess) {
                    val summary = mergeResult.getOrNull()!!
                    if (summary.conflicts.isNotEmpty()) {
                        pendingImportFile = tempFile // Keep for resolution
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            pendingConflicts = summary.conflicts,
                            statusMessage = "Conflicts Detected. Manual resolution required."
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            statusMessage = "Merged Successfully. Inserted ${summary.insertedCount} records."
                        )
                        tempFile.delete() // Clean up
                    }
                } else {
                    _uiState.value = _uiState.value.copy(isProcessing = false, statusMessage = "Merge Failed")
                    tempFile.delete() // Clean up
                }
            } else {
                _uiState.value = _uiState.value.copy(isProcessing = false, statusMessage = "Failed to copy import file")
            }
        }
    }

    fun resolveConflicts(resolutions: Map<String, String>, activeWorkspace: String) {
        val tempFile = pendingImportFile ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, statusMessage = "Resolving...")
            
            val mergeResult = mergeUseCase.mergeDatabase(tempFile, activeWorkspace, resolutions)
            if (mergeResult.isSuccess) {
                val summary = mergeResult.getOrNull()!!
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    pendingConflicts = emptyList(),
                    statusMessage = "Conflicts Resolved. Processed ${summary.insertedCount} records."
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    pendingConflicts = emptyList(),
                    statusMessage = "Resolution Failed"
                )
            }
            
            tempFile.delete()
            pendingImportFile = null
        }
    }

    fun cancelMerge() {
        pendingImportFile?.delete()
        pendingImportFile = null
        _uiState.value = _uiState.value.copy(pendingConflicts = emptyList(), statusMessage = "Merge Cancelled")
    }

    fun factoryReset() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            databaseManager.factoryReset(appDatabase)
            _uiState.value = _uiState.value.copy(isProcessing = false, statusMessage = "Database Reset Complete")
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }
}

data class VaultUiState(
    val isProcessing: Boolean = false,
    val statusMessage: String? = null,
    val pendingConflicts: List<MergeConflict> = emptyList()
)
