package com.attendo.android.ui.tabs

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendo.android.data.local.SessionSummary
import com.attendo.android.ui.history.HistoryViewModel
import com.attendo.android.ui.history.SessionAuditEntry

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    activeWorkspace: String?,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    var searchQuery by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf("All Sessions") }
    var typeExpanded by remember { mutableStateOf(false) }
    
    var selectedSessionNames by remember { mutableStateOf<Set<String>>(emptySet()) }
    val isSelectionMode = selectedSessionNames.isNotEmpty()
    
    var archivingSession by remember { mutableStateOf<SessionSummary?>(null) }
    var deletingSession by remember { mutableStateOf<SessionSummary?>(null) }
    
    LaunchedEffect(activeWorkspace) {
        if (activeWorkspace != null) {
            viewModel.loadSessions(activeWorkspace)
        }
    }

    if (uiState.selectedSession == null) {
        // Session List View
        val filteredSessions = uiState.sessionSummaries.filter { summary ->
            summary.sessionName.contains(searchQuery, ignoreCase = true) &&
            (typeFilter == "All Sessions" || summary.sessionName.contains(typeFilter, ignoreCase = true))
        }

        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .background(Color(0xFF1E293B), RoundedCornerShape(24.dp))
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    if (isSelectionMode) {
                        Text(
                            text = "Selected: ${selectedSessionNames.size}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF14B8A6)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.isArchiveView) {
                                Button(
                                    onClick = { 
                                        viewModel.restoreSessions(selectedSessionNames.toList())
                                        selectedSessionNames = emptySet()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                ) { Text("Restore All") }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { 
                                        viewModel.deleteSessions(selectedSessionNames.toList())
                                        selectedSessionNames = emptySet()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                ) { Text("Delete All") }
                            } else {
                                Button(
                                    onClick = { 
                                        viewModel.archiveSessions(selectedSessionNames.toList())
                                        selectedSessionNames = emptySet()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                                ) { Text("Archive All") }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = { selectedSessionNames = emptySet() }) {
                                Text("Cancel", color = Color.Gray)
                            }
                        }
                    } else {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (uiState.isArchiveView) "Archived Sessions" else "Workspace History",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF14B8A6)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Manage and edit past attendance sessions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF3B82F6).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .clickable { viewModel.setArchiveView(!uiState.isArchiveView) }
                        ) {
                            Text(
                                text = if (uiState.isArchiveView) "VIEW ACTIVE ${uiState.activeCount}" else "ARCHIVED SESSIONS",
                                color = Color(0xFF60A5FA),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Control Bar
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f).height(50.dp),
                        placeholder = { Text("Search Active sessions...", color = Color.Gray, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    Row(
                        modifier = Modifier
                            .height(50.dp)
                            .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                            .clickable {
                                if (selectedSessionNames.size == filteredSessions.size && filteredSessions.isNotEmpty()) {
                                    selectedSessionNames = emptySet()
                                } else {
                                    selectedSessionNames = filteredSessions.map { it.sessionName }.toSet()
                                }
                            }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (selectedSessionNames.size == filteredSessions.size && filteredSessions.isNotEmpty()) Icons.Outlined.CheckCircle else Icons.Outlined.Circle,
                            contentDescription = null,
                            tint = if (selectedSessionNames.size == filteredSessions.size && filteredSessions.isNotEmpty()) Color(0xFF14B8A6) else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SELECT ALL", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(modifier = Modifier.height(50.dp).background(Color(0xFF0F172A), RoundedCornerShape(12.dp))) {
                        Row(
                            modifier = Modifier.fillMaxHeight().clickable { typeExpanded = true }.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TYPE: ", color = Color.Gray, fontSize = 10.sp)
                            Text(typeFilter, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                            listOf("All Sessions", "Lecture", "Section").forEach { type ->
                                DropdownMenuItem(text = { Text(type) }, onClick = { typeFilter = type; typeExpanded = false })
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))

                // List Header
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("SESSION", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                    Text("DATE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("ATTENDEES", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (filteredSessions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No sessions found.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredSessions, key = { it.sessionName }) { summary ->
                            val isSelected = selectedSessionNames.contains(summary.sessionName)
                            SessionCard(
                                summary = summary,
                                isSelected = isSelected,
                                isSelectionMode = isSelectionMode,
                                isArchiveView = uiState.isArchiveView,
                                onToggleSelection = {
                                    selectedSessionNames = if (isSelected) selectedSessionNames - summary.sessionName
                                    else selectedSessionNames + summary.sessionName
                                },
                                onSwipeLeftAction = {
                                    if (uiState.isArchiveView) deletingSession = summary else archivingSession = summary
                                },
                                onSwipeRightAction = {
                                    if (uiState.isArchiveView) viewModel.restoreSessions(listOf(summary.sessionName))
                                    else viewModel.selectSession(summary.sessionName)
                                }
                            )
                        }
                    }
                }
            }
        }
        
        archivingSession?.let { summary ->
            AlertDialog(
                onDismissRequest = { archivingSession = null },
                title = { Text("Archive Session") },
                text = { Text("Are you sure you want to archive ${summary.sessionName}?") },
                confirmButton = {
                    Button(onClick = { viewModel.archiveSessions(listOf(summary.sessionName)); archivingSession = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Archive") }
                },
                dismissButton = { TextButton(onClick = { archivingSession = null }) { Text("Cancel") } }
            )
        }
        
        deletingSession?.let { summary ->
            AlertDialog(
                onDismissRequest = { deletingSession = null },
                title = { Text("Delete Session") },
                text = { Text("Are you sure you want to permanently delete ${summary.sessionName}?") },
                confirmButton = {
                    Button(onClick = { viewModel.deleteSessions(listOf(summary.sessionName)); deletingSession = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Delete") }
                },
                dismissButton = { TextButton(onClick = { deletingSession = null }) { Text("Cancel") } }
            )
        }

    } else {
        // Session Audit View (Editing a session)
        BackHandler { viewModel.selectSession(null) }
        
        var auditSearch by remember { mutableStateOf("") }
        var selectedAudit by remember { mutableStateOf<String?>(null) }
        
        val pdfLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/pdf"),
            onResult = { uri: Uri? -> uri?.let { viewModel.exportSessionAsPdf(context, it) } }
        )
        val csvLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("text/csv"),
            onResult = { uri: Uri? -> uri?.let { viewModel.exportSessionAsCsv(context, it) } }
        )

        val filteredAudit = uiState.auditEntries.filter {
            it.student.name.contains(auditSearch, ignoreCase = true) ||
            (it.student.nationalId?.contains(auditSearch) == true)
        }
        
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .background(Color(0xFF1E293B), RoundedCornerShape(24.dp))
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.selectSession(null) },
                        modifier = Modifier.background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Session Audit", color = Color(0xFF14B8A6), fontWeight = FontWeight.Black, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.background(Color(0xFF0D9488), RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text("EDIT MODE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(uiState.selectedSession!!, color = Color.Gray, fontSize = 12.sp)
                    }
                }
                
                // Controls
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = auditSearch,
                        onValueChange = { auditSearch = it },
                        modifier = Modifier.weight(1f).height(50.dp),
                        placeholder = { Text("Search roster to add missing student...", color = Color.Gray, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    
                    Button(
                        onClick = { 
                            val safeTitle = uiState.selectedSession!!.replace("[", "").replace("]", "").replace(" ", "_")
                            pdfLauncher.launch("Attendo_Audit_$safeTitle.pdf") 
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(50.dp)
                    ) { Text("Export PDF", fontWeight = FontWeight.Bold, color = Color.White) }
                    
                    Button(
                        onClick = { 
                            val safeTitle = uiState.selectedSession!!.replace("[", "").replace("]", "").replace(" ", "_")
                            csvLauncher.launch("Attendo_Audit_$safeTitle.csv") 
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14B8A6)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(50.dp)
                    ) { Text("Export Excel", fontWeight = FontWeight.Bold, color = Color.White) }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))

                // List Header
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("STUDENT NAME", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                    Text("ID", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                    Text("BONUS POINTS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text("ATTENDANCE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredAudit, key = { it.student.id }) { entry ->
                        AuditEntryCard(
                            entry = entry,
                            onToggleState = { viewModel.cycleAttendanceState(entry.student) },
                            onDeltaBonus = { delta -> viewModel.changeBonusPoints(entry.student, delta) },
                            onViewAudit = { selectedAudit = entry.attendance?.auditTrail }
                        )
                    }
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SessionCard(
    summary: SessionSummary,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    isArchiveView: Boolean,
    onToggleSelection: () -> Unit,
    onSwipeLeftAction: () -> Unit,
    onSwipeRightAction: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                onSwipeRightAction()
                false
            } else if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onSwipeLeftAction()
                false
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = !isSelectionMode,
        enableDismissFromEndToStart = !isSelectionMode,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.StartToEnd -> if (isArchiveView) Color(0xFF10B981) else Color(0xFF3B82F6)
                SwipeToDismissBoxValue.EndToStart -> Color(0xFFEF4444)
                else -> Color.Transparent
            }
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> if (isArchiveView) Icons.Default.Unarchive else Icons.Default.Edit
                SwipeToDismissBoxValue.EndToStart -> if (isArchiveView) Icons.Default.Delete else Icons.Outlined.Archive
                else -> Icons.Default.Edit
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Icon(icon, contentDescription = null, tint = Color.White)
            }
        }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onLongClick = { onToggleSelection() },
                    onClick = { if (isSelectionMode) onToggleSelection() }
                ),
            color = if (isSelected) Color(0xFF334155) else Color(0xFF0F172A),
            shape = RoundedCornerShape(12.dp),
            border = if (isSelected) BorderStroke(1.dp, Color(0xFF14B8A6)) else null
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val safeName = summary.sessionName.replace("[", "").replace("]", "")
                Text(
                    text = safeName, 
                    color = Color.White, 
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(2f)
                )
                
                val dateStr = try {
                    val parser = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                    val formatter = java.text.SimpleDateFormat("M/d/yyyy", java.util.Locale.US)
                    val date = parser.parse(summary.date)
                    if (date != null) formatter.format(date) else summary.date.take(10)
                } catch (e: Exception) {
                    summary.date.take(10)
                }
                
                Text(
                    text = dateStr, 
                    color = Color.Gray, 
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    text = "${summary.attendeesCount}", 
                    color = Color(0xFF14B8A6), 
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun AuditEntryCard(
    entry: SessionAuditEntry,
    onToggleState: () -> Unit,
    onDeltaBonus: (Int) -> Unit,
    onViewAudit: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = entry.student.name, 
                color = Color.White, 
                fontWeight = FontWeight.Bold, 
                fontSize = 13.sp, 
                modifier = Modifier.weight(2f)
            )
            
            Text(
                text = entry.student.nationalId ?: "N/A", 
                color = Color.Gray, 
                fontSize = 12.sp, 
                modifier = Modifier.weight(1.5f)
            )
            
            Row(
                modifier = Modifier.weight(1.5f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.background(Color(0xFF1E293B), RoundedCornerShape(12.dp)).padding(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "-", 
                            color = Color.Gray, 
                            fontWeight = FontWeight.Bold, 
                            modifier = Modifier.clickable { onDeltaBonus(-1) }.padding(horizontal = 8.dp)
                        )
                        Text(
                            text = "${entry.attendance?.bonusPoints ?: 0}", 
                            color = Color.White, 
                            fontWeight = FontWeight.Bold, 
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Text(
                            text = "+", 
                            color = Color.Gray, 
                            fontWeight = FontWeight.Bold, 
                            modifier = Modifier.clickable { onDeltaBonus(1) }.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
            
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                val (bgColor, textColor, label) = when (entry.status) {
                    "PRESENT" -> Triple(Color(0xFF0D9488).copy(alpha = 0.2f), Color(0xFF14B8A6), "PRESENT")
                    "EXCUSED" -> Triple(Color(0xFFD97706).copy(alpha = 0.2f), Color(0xFFF59E0B), "EXCUSED")
                    else -> Triple(Color.Transparent, Color.Gray, "ABSENT")
                }
                
                Box(
                    modifier = Modifier
                        .background(bgColor, RoundedCornerShape(12.dp))
                        .clickable { onToggleState() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = label, color = textColor, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }
            
            if (entry.attendance != null) {
                IconButton(onClick = onViewAudit, modifier = Modifier.size(24.dp).padding(start = 4.dp)) {
                    Icon(Icons.Default.Info, contentDescription = "Audit", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            } else {
                Spacer(modifier = Modifier.width(24.dp))
            }
        }
    }
}
