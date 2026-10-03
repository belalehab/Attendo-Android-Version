package com.attendo.android.ui.tabs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendo.android.ui.vault.VaultViewModel

@Composable
fun VaultScreen(
    activeWorkspace: String?, // Needs to be passed down in MainShell
    viewModel: VaultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
        onResult = { uri: Uri? -> uri?.let { viewModel.exportDatabase(it) } }
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? -> uri?.let { viewModel.importAndMerge(it, activeWorkspace ?: "") } }
    )

    LaunchedEffect(uiState.statusMessage) {
        if (uiState.statusMessage != null) {
            // Usually trigger a snackbar here
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("The Vault", style = MaterialTheme.typography.headlineMedium)
            
            // Backup Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Database Backup", style = MaterialTheme.typography.titleMedium)
                    Text("Export an exact .attdb copy compatible with the desktop application.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { exportLauncher.launch("backup_${activeWorkspace ?: "app"}.attdb") }) {
                        Text("Export .attdb")
                    }
                }
            }

            // Import/Merge Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Import & Merge", style = MaterialTheme.typography.titleMedium)
                    Text("Merge a colleague's backup. Conflicts will be flagged.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
                        Text("Import Backup")
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Danger Zone
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = "Warning", tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Danger Zone", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Factory Reset Database")
                    }
                }
            }
        }

        if (uiState.isProcessing) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        if (uiState.statusMessage != null) {
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
            ) {
                Text(uiState.statusMessage!!)
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Factory Reset") },
            text = { Text("Are you sure? This will permanently delete all students and attendance records.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.factoryReset()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("RESET")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (uiState.pendingConflicts.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { /* Force resolution */ },
            title = { Text("Conflicts Detected") },
            text = { Text("${uiState.pendingConflicts.size} conflicts found. Resolving UI to be displayed.") },
            confirmButton = {
                TextButton(onClick = { viewModel.resolveConflicts(emptyList()) }) {
                    Text("Auto-Resolve (Keep Local)")
                }
            }
        )
    }
}
