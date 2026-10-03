package com.attendo.android.ui.tabs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendo.android.data.local.Attendance
import com.attendo.android.data.local.AttendanceWithStudent
import com.attendo.android.ui.history.HistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    activeWorkspace: String?,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(activeWorkspace) {
        if (activeWorkspace != null) {
            viewModel.loadSessions(activeWorkspace)
        }
    }

    if (uiState.selectedSession == null) {
        // Session List
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.sessions.isEmpty()) {
                Text(
                    "No sessions recorded in ${activeWorkspace ?: "workspace"}",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.sessions) { sessionName ->
                        ListItem(
                            headlineContent = { Text(sessionName) },
                            modifier = Modifier.clickable { viewModel.selectSession(sessionName) },
                            shadowElevation = 1.dp
                        )
                        Divider()
                    }
                }
            }
        }
    } else {
        // Session Detail View
        BackHandler { viewModel.selectSession(null) }
        
        var selectedAudit by remember { mutableStateOf<String?>(null) }
        
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(uiState.selectedSession ?: "") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.selectSession(null) }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.sessionRecords, key = { it.attendance.id }) { record ->
                    AttendanceCard(
                        record = record,
                        onUpdateBonus = { pts -> viewModel.updateBonusPoints(record.attendance, pts) },
                        onToggleExcused = { exc, reason -> viewModel.toggleExcused(record.attendance, exc, reason) },
                        onRemove = { viewModel.removeAttendance(record.attendance) },
                        onViewAudit = { selectedAudit = record.attendance.auditTrail }
                    )
                }
            }
        }
        
        if (selectedAudit != null) {
            AlertDialog(
                onDismissRequest = { selectedAudit = null },
                title = { Text("Audit Ledger") },
                text = { Text(selectedAudit ?: "[]") },
                confirmButton = { TextButton(onClick = { selectedAudit = null }) { Text("Close") } }
            )
        }
    }
}

@Composable
fun AttendanceCard(
    record: AttendanceWithStudent,
    onUpdateBonus: (Int) -> Unit,
    onToggleExcused: (Boolean, String?) -> Unit,
    onRemove: () -> Unit,
    onViewAudit: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(record.studentName, style = MaterialTheme.typography.titleMedium)
                    Text("Time: ${record.attendance.timestamp}", style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onViewAudit) {
                    Icon(Icons.Default.Info, "Audit Log")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, "Bonus", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${record.attendance.bonusPoints}")
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onUpdateBonus((record.attendance.bonusPoints ?: 0) + 1) }) { Text("+1") }
                }
                
                if (record.attendance.isExcused == 1) {
                    OutlinedButton(onClick = { onToggleExcused(false, null) }) { Text("Un-Excuse") }
                } else {
                    OutlinedButton(onClick = { onToggleExcused(true, "Late (Manual)") }) { Text("Excuse") }
                }
            }
            
            if (record.attendance.isExcused == 1) {
                Text("Reason: ${record.attendance.excuseReason ?: "None"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            
            TextButton(onClick = onRemove, modifier = Modifier.align(Alignment.End)) {
                Text("Remove Attendance", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
