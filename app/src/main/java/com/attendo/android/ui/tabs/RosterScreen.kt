package com.attendo.android.ui.tabs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendo.android.data.local.Student
import com.attendo.android.ui.roster.AddStudentDialog
import com.attendo.android.ui.roster.EditStudentDialog
import com.attendo.android.ui.roster.RosterViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RosterScreen(
    activeWorkspace: String?,
    searchQuery: String,
    viewModel: RosterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val studentProfile by viewModel.studentProfile.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingStudent by remember { mutableStateOf<Student?>(null) }
    var archivingStudent by remember { mutableStateOf<Student?>(null) }
    var deletingStudent by remember { mutableStateOf<Student?>(null) }
    val localSearch by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    var selectedStudentIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    val isSelectionMode = selectedStudentIds.isNotEmpty()

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? -> uri?.let { viewModel.importCsv(it) } }
    )

    val templateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri: Uri? -> uri?.let { viewModel.exportTemplate(it, context) } }
    )

    val qrZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri: Uri? ->
            uri?.let {
                val studentsToExport = if (isSelectionMode) {
                    uiState.students.filter { s -> selectedStudentIds.contains(s.id) }
                } else {
                    uiState.students
                }
                viewModel.exportQRs(it, context, studentsToExport)
                selectedStudentIds = emptySet()
            }
        }
    )

    LaunchedEffect(activeWorkspace) {
        if (activeWorkspace != null) {
            viewModel.loadStudents(activeWorkspace)
        }
    }

    val query = localSearch.ifBlank { searchQuery }
    val filteredStudents = uiState.students.filter {
        it.name.contains(query, ignoreCase = true) || 
        (it.nationalId?.contains(query) == true)
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
                        text = "Selected: ${selectedStudentIds.size}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF14B8A6)
                    )
                    TextButton(onClick = { selectedStudentIds = emptySet() }) {
                        Text("Cancel Selection", color = Color.Gray)
                    }
                } else {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (uiState.isArchiveView) "Archived Roster" else "Workspace Roster",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF14B8A6)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.isArchiveView) "Manage archived students." else "Import new lists and manage enrolled students.",
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
                            text = if (uiState.isArchiveView) "VIEW ACTIVE ${uiState.activeCount}" else "VIEW ARCHIVED",
                            color = Color(0xFF60A5FA),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            if (!uiState.isArchiveView) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { templateLauncher.launch("Attendo_Template.csv") },
                        border = BorderStroke(1.dp, Color.Gray),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Template", fontSize = 12.sp)
                    }

                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .drawBehind {
                                drawRoundRect(
                                    color = Color.Gray,
                                    style = Stroke(
                                        width = 2f,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                    ),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                                )
                            }
                            .background(Color.Transparent)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.clickable { csvLauncher.launch(arrayOf("text/comma-separated-values", "text/csv", "application/csv")) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.UploadFile, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import CSV", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = { qrZipLauncher.launch("Attendo_QRs.zip") },
                        border = BorderStroke(1.dp, Color(0xFF14B8A6)),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF14B8A6)),
                        modifier = Modifier.padding(0.dp)
                    ) {
                        Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("QRs", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488), contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Row {
                    OutlinedButton(
                        onClick = { qrZipLauncher.launch("Attendo_QRs.zip") },
                        border = BorderStroke(1.dp, Color(0xFF14B8A6)),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF14B8A6))
                    ) {
                        Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download QRs", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(8.dp))

            // List Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("STUDENT NAME", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Text("GRADE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // List
            if (uiState.isImporting) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF14B8A6))
                }
            } else if (filteredStudents.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No students found.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredStudents, key = { it.id }) { student ->
                        val isSelected = selectedStudentIds.contains(student.id)
                        StudentCard(
                            student = student,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            isArchiveView = uiState.isArchiveView,
                            onToggleSelection = {
                                selectedStudentIds = if (isSelected) {
                                    selectedStudentIds - student.id
                                } else {
                                    selectedStudentIds + student.id
                                }
                            },
                            onClick = {
                                if (isSelectionMode) {
                                    selectedStudentIds = if (isSelected) {
                                        selectedStudentIds - student.id
                                    } else {
                                        selectedStudentIds + student.id
                                    }
                                } else {
                                    viewModel.openStudentProfile(student)
                                }
                            },
                            onEdit = { editingStudent = student },
                            onSwipeLeftAction = { 
                                if (uiState.isArchiveView) {
                                    deletingStudent = student
                                } else {
                                    archivingStudent = student
                                }
                            },
                            onSwipeRightAction = {
                                if (uiState.isArchiveView) {
                                    viewModel.restoreStudent(student)
                                } else {
                                    editingStudent = student
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    studentProfile?.let { profile ->
        StudentProfileDialog(
            profile = profile,
            onDismiss = { viewModel.clearStudentProfile() }
        )
    }

    if (showAddDialog) {
        AddStudentDialog(
            activeWorkspace = activeWorkspace,
            onDismiss = { showAddDialog = false },
            onSubmit = { name, id ->
                viewModel.addStudent(name, id)
                showAddDialog = false
            }
        )
    }

    editingStudent?.let { student ->
        EditStudentDialog(
            initialName = student.name,
            initialId = student.nationalId ?: "",
            onDismiss = { editingStudent = null },
            onSubmit = { newName, newId ->
                viewModel.editStudent(student, newName, newId)
                editingStudent = null
            }
        )
    }

    archivingStudent?.let { student ->
        AlertDialog(
            onDismissRequest = { archivingStudent = null },
            title = { Text("Archive Student") },
            text = { Text("Are you sure you want to archive ${student.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.archiveStudent(student)
                        archivingStudent = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Archive")
                }
            },
            dismissButton = {
                TextButton(onClick = { archivingStudent = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    deletingStudent?.let { student ->
        AlertDialog(
            onDismissRequest = { deletingStudent = null },
            title = { Text("Delete Student") },
            text = { Text("Are you sure you want to permanently delete ${student.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStudentPermanently(student)
                        deletingStudent = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingStudent = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StudentCard(
    student: Student,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    isArchiveView: Boolean,
    onToggleSelection: () -> Unit,
    onClick: () -> Unit,
    onEdit: () -> Unit,
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
                    onClick = { onClick() }
                ),
            color = if (isSelected) Color(0xFF1E293B) else Color(0xFF0F172A),
            shape = RoundedCornerShape(12.dp),
            border = if (isSelected) BorderStroke(1.dp, Color(0xFF14B8A6)) else null
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                        text = student.nationalId ?: "No ID", 
                        color = Color.Gray, 
                        fontSize = 12.sp
                    )
                }
                
                Text(
                    text = student.grade ?: "N/A",
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}

@Composable
fun StudentProfileDialog(
    profile: com.attendo.android.ui.roster.StudentProfileData,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).clip(RoundedCornerShape(24.dp)),
            color = Color(0xFF0F172A), // Slate 900
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF1E293B)).padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = profile.student.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF14B8A6) // Teal 400
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = profile.student.nationalId ?: "No ID",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Stats Cards
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(title = "ATTENDED", value = profile.attendedCount, color = Color(0xFF14B8A6), bonus = profile.bonusPoints, modifier = Modifier.weight(1f))
                    StatCard(title = "EXCUSED", value = profile.excusedCount, color = Color(0xFF818CF8), bonus = 0, modifier = Modifier.weight(1f))
                    StatCard(title = "ABSENT", value = profile.absentCount, color = Color(0xFFFB7185), bonus = 0, modifier = Modifier.weight(1f))
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                var selectedTab by remember { mutableStateOf("ATTENDED") }
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { selectedTab = "ATTENDED" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == "ATTENDED") Color(0xFF14B8A6).copy(alpha = 0.2f) else Color.Transparent,
                            contentColor = if (selectedTab == "ATTENDED") Color(0xFF14B8A6) else Color.Gray
                        ),
                        border = BorderStroke(1.dp, if (selectedTab == "ATTENDED") Color(0xFF14B8A6).copy(alpha = 0.5f) else Color.Transparent),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("ATTENDED / EXC", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { selectedTab = "ABSENT" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == "ABSENT") Color(0xFFFB7185).copy(alpha = 0.2f) else Color.Transparent,
                            contentColor = if (selectedTab == "ABSENT") Color(0xFFFB7185) else Color.Gray
                        ),
                        border = BorderStroke(1.dp, if (selectedTab == "ABSENT") Color(0xFFFB7185).copy(alpha = 0.5f) else Color.Transparent),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("ABSENT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Timeline List
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filteredTimeline = if (selectedTab == "ATTENDED") {
                        profile.timeline.filter { it.status == "ATTENDED" || it.status == "EXCUSED" }
                    } else {
                        profile.timeline.filter { it.status == "ABSENT" }
                    }

                    if (filteredTimeline.isEmpty()) {
                        item {
                            Text(
                                "No active records found for this filter.",
                                color = Color.Gray,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                fontSize = 14.sp,
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        items(filteredTimeline) { record ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = record.sessionName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = record.timestamp ?: "Unknown Date",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                                val statusColor = when (record.status) {
                                    "ATTENDED" -> Color(0xFF14B8A6)
                                    "EXCUSED" -> Color(0xFF818CF8)
                                    else -> Color(0xFFFB7185)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .border(1.dp, statusColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = record.status,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: Int, color: Color, bonus: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value.toString(), fontSize = 24.sp, fontWeight = FontWeight.Black, color = color)
        }
        if (bonus > 0) {
            Text(
                "+${bonus} XP",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFBBF24),
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 4.dp, top = 4.dp)
            )
        }
    }
}
