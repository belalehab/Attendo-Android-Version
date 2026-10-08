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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
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
        MergeConflictsDialog(
            conflicts = uiState.pendingConflicts,
            onResolve = { resolutions ->
                viewModel.resolveConflicts(resolutions, activeWorkspace ?: "")
            },
            onCancel = {
                viewModel.cancelMerge()
            }
        )
    }
}

@Composable
fun MergeConflictsDialog(
    conflicts: List<com.attendo.android.domain.usecase.MergeConflict>,
    onResolve: (Map<String, String>) -> Unit,
    onCancel: () -> Unit
) {
    val resolutions = remember { mutableStateMapOf<String, String>() }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onCancel,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF1E293B)).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Resolve Conflicts", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${conflicts.size} conflicts detected", fontSize = 12.sp, color = Color.Gray)
                    }
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                // Bulk actions
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { 
                            conflicts.forEach { resolutions["${it.nationalId}|${it.sessionName}"] = "LOCAL" }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Keep All Local", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { 
                            conflicts.forEach { resolutions["${it.nationalId}|${it.sessionName}"] = "IMPORTED" }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Use All Imported", fontSize = 12.sp)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // List
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(conflicts) { conflict ->
                        val key = "${conflict.nationalId}|${conflict.sessionName}"
                        val selectedChoice = resolutions[key]
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(conflict.studentName, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("${conflict.nationalId} • ${conflict.sessionName}", fontSize = 12.sp, color = Color.Gray)
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Local Card
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { resolutions[key] = "LOCAL" }
                                        .background(if (selectedChoice == "LOCAL") Color(0xFF14B8A6).copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(8.dp))
                                        .border(2.dp, if (selectedChoice == "LOCAL") Color(0xFF14B8A6) else Color.DarkGray, RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        Text("LOCAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (selectedChoice == "LOCAL") Color(0xFF14B8A6) else Color.Gray)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Excused: ${if (conflict.localExcused == 1) "Yes" else "No"}", fontSize = 12.sp, color = Color.White)
                                        Text("Bonus: ${conflict.localBonus}", fontSize = 12.sp, color = Color.White)
                                    }
                                }
                                
                                // Imported Card
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { resolutions[key] = "IMPORTED" }
                                        .background(if (selectedChoice == "IMPORTED") Color(0xFF3B82F6).copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(8.dp))
                                        .border(2.dp, if (selectedChoice == "IMPORTED") Color(0xFF3B82F6) else Color.DarkGray, RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        Text("IMPORTED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (selectedChoice == "IMPORTED") Color(0xFF3B82F6) else Color.Gray)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Excused: ${if (conflict.extExcused == 1) "Yes" else "No"}", fontSize = 12.sp, color = Color.White)
                                        Text("Bonus: ${conflict.extBonus}", fontSize = 12.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer
                val isAllResolved = conflicts.size == resolutions.size
                Button(
                    onClick = { if (isAllResolved) onResolve(resolutions) },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    enabled = isAllResolved,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14B8A6)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isAllResolved) "Apply Resolutions" else "Resolve all conflicts to apply")
                }
            }
        }
    }
}
