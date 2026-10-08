package com.attendo.android.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendo.android.data.local.Student

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    activeWorkspace: String,
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showManualEntry by remember { mutableStateOf(false) }
    var showSessionSummary by remember { mutableStateOf(false) }

    LaunchedEffect(activeWorkspace) {
        viewModel.loadInstructorAndSubject(activeWorkspace)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.toggleSession()
            }
        }
    )

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
        // Foreground UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = uiState.subjectName,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Audio",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(4.dp)
                )
            }
            Text(
                text = "Instructor: ${uiState.instructorName}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Session Configuration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header inside card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Session Configuration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (uiState.isSessionActive) Color(0xFF10B981) else Color(0xFFF59E0B),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (uiState.isSessionActive) "IN PROGRESS" else "STANDBY",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }

                    AnimatedVisibility(visible = !uiState.isSessionActive) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            // Dropdowns and inputs row 1
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                var typeExpanded by remember { mutableStateOf(false) }
                                val types = listOf("Lecture", "Section")
                                ExposedDropdownMenuBox(
                                    expanded = typeExpanded,
                                    onExpandedChange = { typeExpanded = !typeExpanded },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = uiState.sessionConfig.type,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("TYPE", fontSize = 10.sp) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                                        modifier = Modifier.menuAnchor()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = typeExpanded,
                                        onDismissRequest = { typeExpanded = false }
                                    ) {
                                        types.forEach { type ->
                                            DropdownMenuItem(
                                                text = { Text(type) },
                                                onClick = {
                                                    viewModel.updateConfig { it.copy(type = type) }
                                                    typeExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                var groupExpanded by remember { mutableStateOf(false) }
                                val groups = listOf("All Groups", "1", "2", "3", "4", "5")
                                ExposedDropdownMenuBox(
                                    expanded = groupExpanded,
                                    onExpandedChange = { groupExpanded = !groupExpanded },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = uiState.sessionConfig.group,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("GROUP", fontSize = 10.sp) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupExpanded) },
                                        modifier = Modifier.menuAnchor()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = groupExpanded,
                                        onDismissRequest = { groupExpanded = false }
                                    ) {
                                        groups.forEach { g ->
                                            DropdownMenuItem(
                                                text = { Text(g) },
                                                onClick = {
                                                    viewModel.updateConfig { it.copy(group = g) }
                                                    groupExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = uiState.sessionConfig.week,
                                    onValueChange = { viewModel.updateConfig { c -> c.copy(week = it) } },
                                    label = { Text("WEEK", fontSize = 10.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(0.5f)
                                )
                                OutlinedTextField(
                                    value = uiState.sessionConfig.topic,
                                    onValueChange = { viewModel.updateConfig { c -> c.copy(topic = it) } },
                                    label = { Text("TOPIC (OPTIONAL)", fontSize = 10.sp) },
                                    placeholder = { Text("e.g., Introduction") },
                                    modifier = Modifier.weight(1.5f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (!uiState.isSessionActive) {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                
                                if (hasPermission) {
                                    viewModel.toggleSession()
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            } else {
                                // STOP SESSION => Show Summary
                                showSessionSummary = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.isSessionActive) Color(0xFFEF4444) else Color(0xFF14B8A6),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (uiState.isSessionActive) "Stop Session" else "Start Session",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (!uiState.isSessionActive) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "WORKSPACE STUDENTS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.workspaceStudentCount.toString(),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            } else {
                // Active Counter Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SCANNED ATTENDEES",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = uiState.attendeeCount.toString(),
                        color = Color(0xFF14B8A6),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Active Camera View
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Black, RoundedCornerShape(16.dp))
                        .drawBehind {
                            val strokeWidth = 8.dp.toPx()
                            val lineLength = 40.dp.toPx()
                            val color = Color.White
                            
                            // Top-Left
                            drawLine(color, Offset(0f, 0f), Offset(lineLength, 0f), strokeWidth)
                            drawLine(color, Offset(0f, 0f), Offset(0f, lineLength), strokeWidth)
                            
                            // Top-Right
                            drawLine(color, Offset(size.width, 0f), Offset(size.width - lineLength, 0f), strokeWidth)
                            drawLine(color, Offset(size.width, 0f), Offset(size.width, lineLength), strokeWidth)
                            
                            // Bottom-Left
                            drawLine(color, Offset(0f, size.height), Offset(lineLength, size.height), strokeWidth)
                            drawLine(color, Offset(0f, size.height), Offset(0f, size.height - lineLength), strokeWidth)
                            
                            // Bottom-Right
                            drawLine(color, Offset(size.width, size.height), Offset(size.width - lineLength, size.height), strokeWidth)
                            drawLine(color, Offset(size.width, size.height), Offset(size.width, size.height - lineLength), strokeWidth)
                        }
                        .padding(2.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        color = Color.Black,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(2.dp, Color(0xFF14B8A6))
                    ) {
                        CameraPreview(
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                            onQrCodeScanned = viewModel::onQrScanned
                        )
                    }
                }
            }
        }

        // Bottom Controls for Manual Entry when active
        if (uiState.isSessionActive) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Button(
                    onClick = { showManualEntry = true },
                    modifier = Modifier.align(Alignment.Center),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Manual Entry", color = Color(0xFF14B8A6), fontWeight = FontWeight.Bold)
                }
            }
        }

        // Feedback Overlay
        if (uiState.lastScannedMessage != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                Text(
                    text = uiState.lastScannedMessage!!,
                    color = if (uiState.lastScannedMessage!!.startsWith("Success")) Color(0xFF10B981) else Color(0xFFEF4444),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showManualEntry) {
        UnifiedEntryPanelDialog(
            students = uiState.workspaceStudents,
            onDismiss = { showManualEntry = false },
            onSubmitBatch = { ids ->
                viewModel.onBatchSubmit(ids)
                showManualEntry = false
            }
        )
    }

    if (showSessionSummary) {
        SessionSummaryDialog(
            attendeeCount = uiState.attendeeCount,
            onSaveToHistory = {
                viewModel.saveSessionToHistory()
                showSessionSummary = false
            },
            onExportPdf = {
                // TODO: Stage 2/3 Export functionality
            },
            onExportExcel = {
                // TODO: Stage 2/3 Export functionality
            },
            onDiscard = {
                viewModel.clearSession()
                showSessionSummary = false
            }
        )
    }
}

@Composable
fun SessionSummaryDialog(
    attendeeCount: Int,
    onSaveToHistory: () -> Unit,
    onExportPdf: () -> Unit,
    onExportExcel: () -> Unit,
    onDiscard: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {}, // Force interaction
        title = { Text("Session Ended", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("You have successfully ended the session.")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Total Scanned Attendees: $attendeeCount", fontWeight = FontWeight.Black, color = Color(0xFF14B8A6))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Choose an action below to save this data.")
            }
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onSaveToHistory, modifier = Modifier.fillMaxWidth()) {
                    Text("Save to History")
                }
                OutlinedButton(onClick = onExportPdf, modifier = Modifier.fillMaxWidth()) {
                    Text("Export as PDF")
                }
                OutlinedButton(onClick = onExportExcel, modifier = Modifier.fillMaxWidth()) {
                    Text("Export as Excel (CSV)")
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDiscard, modifier = Modifier.fillMaxWidth()) {
                    Text("Discard Session", color = Color.Red)
                }
            }
        }
    )
}

// ... [UnifiedEntryPanelDialog remains untouched below] ...

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedEntryPanelDialog(
    students: List<Student>,
    onDismiss: () -> Unit,
    onSubmitBatch: (List<String>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    
    val filteredStudents = remember(searchQuery, students) {
        if (searchQuery.isBlank()) students
        else students.filter { 
            it.name.contains(searchQuery, ignoreCase = true) || 
            it.nationalId?.contains(searchQuery) == true
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(24.dp)),
            color = Color(0xFF1E293B),
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Unified Entry Panel",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF818CF8) // Light Indigo for desktop match
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Select names, type a 14-digit ID, or paste bulk IDs directly into the search bar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Search Bar
                Box(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search by name, or paste multiple 14-digit IDs here...", color = Color.Gray, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color(0xFF818CF8),
                            unfocusedIndicatorColor = Color(0xFF818CF8).copy(alpha = 0.5f)
                        )
                    )
                }

                // List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredStudents) { student ->
                        val isSelected = student.nationalId != null && selectedIds.contains(student.nationalId)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedIds = if (isSelected) {
                                        selectedIds - (student.nationalId ?: "")
                                    } else {
                                        selectedIds + (student.nationalId ?: "")
                                    }
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = student.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = student.nationalId ?: "",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                            Icon(
                                imageVector = if (isSelected) Icons.Outlined.CheckCircle else Icons.Outlined.Circle,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFF818CF8) else Color.Gray
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Selected: ${selectedIds.size}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = { onSubmitBatch(selectedIds.toList()) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        enabled = selectedIds.isNotEmpty()
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Submit Batch", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
