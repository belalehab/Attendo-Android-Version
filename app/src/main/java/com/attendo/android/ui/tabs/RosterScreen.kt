package com.attendo.android.ui.tabs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendo.android.data.local.Student
import com.attendo.android.ui.roster.AddStudentDialog
import com.attendo.android.ui.roster.EditStudentDialog
import com.attendo.android.ui.roster.RosterViewModel

@Composable
fun RosterScreen(
    activeWorkspace: String?,
    searchQuery: String,
    viewModel: RosterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingStudent by remember { mutableStateOf<Student?>(null) }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? -> uri?.let { viewModel.importCsv(it) } }
    )

    LaunchedEffect(activeWorkspace) {
        if (activeWorkspace != null) {
            viewModel.loadStudents(activeWorkspace)
        }
    }

    val filteredStudents = uiState.students.filter {
        it.name.contains(searchQuery, ignoreCase = true) || 
        (it.nationalId?.contains(searchQuery) == true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isImporting) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (filteredStudents.isEmpty()) {
            Text(
                "No students found in ${activeWorkspace ?: "workspace"}",
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredStudents, key = { it.id }) { student ->
                    StudentCard(
                        student = student,
                        onEdit = { editingStudent = student },
                        onArchive = { viewModel.archiveStudent(student) },
                        onDelete = { viewModel.deleteStudentPermanently(student) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Student")
        }

        // Temporary CSV button (Usually put in a TopAppBar menu, placing here for Phase 4)
        ExtendedFloatingActionButton(
            onClick = { csvLauncher.launch(arrayOf("text/comma-separated-values", "text/csv")) },
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
        ) {
            Text("Import CSV")
        }
    }

    if (showAddDialog) {
        AddStudentDialog(
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
}

@Composable
fun StudentCard(
    student: Student,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(student.name, style = MaterialTheme.typography.titleMedium)
                Text(student.nationalId ?: "No ID", style = MaterialTheme.typography.bodySmall)
            }
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = { expanded = false; onEdit() },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Archive") },
                        onClick = { expanded = false; onArchive() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Permanently", color = MaterialTheme.colorScheme.error) },
                        onClick = { expanded = false; onDelete() },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}
