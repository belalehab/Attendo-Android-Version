package com.attendo.android.ui.tabs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendo.android.ui.vault.VaultViewModel

@Composable
fun VaultScreen(
    activeWorkspace: String?,
    viewModel: VaultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var resetConfirmText by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

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
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column {
                Text(
                    text = "System Vault",
                    color = Color(0xFF14B8A6),
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Manage local backups, merge records, and reset for a new academic year.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Card 1: Manual Export
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            tint = Color(0xFF14B8A6),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Manual Export",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Compress and save a hard copy of the database to your local machine.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    OutlinedButton(
                        onClick = { exportLauncher.launch("backup_${activeWorkspace ?: "app"}.attdb") },
                        enabled = !uiState.isProcessing,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White.copy(alpha = 0.05f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export .attdb", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // Card 2: Merge Records
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Merge Records",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Import a backup file from another machine to combine attendance data.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("*/*")) },
                        enabled = !uiState.isProcessing,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.35f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF6366F1).copy(alpha = 0.12f),
                            contentColor = Color(0xFF818CF8)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF818CF8)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import & Merge", fontWeight = FontWeight.Bold, color = Color(0xFF818CF8))
                    }
                }
            }

            // Card 3: Danger Zone
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFFF43F5E).copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFF43F5E),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Danger Zone",
                            color = Color(0xFFF43F5E),
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Start a brand new academic year. This permanently wipes all students and attendance records but keeps your environment setup.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = resetConfirmText,
                            onValueChange = { resetConfirmText = it.uppercase() },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            placeholder = {
                                Text(
                                    "TYPE 'RESET' TO CONFIRM",
                                    color = Color(0xFFF43F5E).copy(alpha = 0.45f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.6f),
                                unfocusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.6f),
                                focusedTextColor = Color(0xFFF43F5E),
                                unfocusedTextColor = Color(0xFFF43F5E),
                                focusedIndicatorColor = Color(0xFFF43F5E).copy(alpha = 0.6f),
                                unfocusedIndicatorColor = Color(0xFFF43F5E).copy(alpha = 0.3f)
                            ),
                            textStyle = TextStyle(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                fontSize = 13.sp
                            )
                        )

                        Button(
                            onClick = {
                                if (resetConfirmText == "RESET") {
                                    viewModel.factoryReset()
                                    resetConfirmText = ""
                                }
                            },
                            enabled = resetConfirmText == "RESET" && !uiState.isProcessing,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF43F5E),
                                disabledContainerColor = Color(0xFFF43F5E).copy(alpha = 0.2f),
                                contentColor = Color.White,
                                disabledContentColor = Color.White.copy(alpha = 0.4f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Factory Reset", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (uiState.isProcessing) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFF14B8A6))
        }

        if (uiState.statusMessage != null) {
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                containerColor = Color(0xFF1E293B),
                contentColor = Color.White
            ) {
                Text(uiState.statusMessage!!)
            }
        }
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
