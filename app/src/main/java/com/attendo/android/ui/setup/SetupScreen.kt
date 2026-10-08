package com.attendo.android.ui.setup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    viewModel: SetupViewModel = hiltViewModel(),
    onSetupComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            onSetupComplete()
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.Start
    ) {
        Text("System Setup", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        // Row 1: Instructor
        OutlinedTextField(
            value = uiState.instructorName,
            onValueChange = { viewModel.updateField { s -> s.copy(instructorName = it) } },
            label = { Text("Instructor Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Row 2: Grades Taught
        Text("Grades Taught (Select all that apply)", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("1", "2", "3", "4", "5").forEach { grade ->
                FilterChip(
                    selected = uiState.grades.contains(grade),
                    onClick = { viewModel.toggleGrade(grade) },
                    label = { Text(grade) }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Subject Names per grade
        if (uiState.grades.isNotEmpty()) {
            Text("Subject Name (Per Grade)", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(8.dp))
            uiState.grades.sorted().forEach { grade ->
                OutlinedTextField(
                    value = uiState.subjects[grade] ?: "",
                    onValueChange = { viewModel.updateSubject(grade, it) },
                    label = { Text("Grade $grade Subject") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Semester Dropdown
        var expanded by remember { mutableStateOf(false) }
        val semesters = listOf("First Semester", "Second Semester", "Summer Semester")
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = uiState.semester,
                onValueChange = {},
                readOnly = true,
                label = { Text("Semester") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                semesters.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            viewModel.updateField { s -> s.copy(semester = selectionOption) }
                            expanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Max Groups
        OutlinedTextField(
            value = uiState.maxGroups.toString(),
            onValueChange = { 
                val num = it.toIntOrNull() ?: 1
                viewModel.updateField { s -> s.copy(maxGroups = num.coerceIn(1, 26)) } 
            },
            label = { Text("Max Groups") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // University & Faculty
        OutlinedTextField(
            value = uiState.university,
            onValueChange = { viewModel.updateField { s -> s.copy(university = it) } },
            label = { Text("University Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.faculty,
            onValueChange = { viewModel.updateField { s -> s.copy(faculty = it) } },
            label = { Text("Faculty Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Dates
        DateField("Semester Start Date", uiState.startDate) { dateStr ->
            viewModel.updateField { s -> s.copy(startDate = dateStr) }
        }
        Spacer(modifier = Modifier.height(16.dp))

        DateField("Semester End Date", uiState.endDate) { dateStr ->
            viewModel.updateField { s -> s.copy(endDate = dateStr) }
        }
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { viewModel.completeSetup() },
            modifier = Modifier.fillMaxWidth(),
            enabled = viewModel.isValid()
        ) {
            Text("Complete Setup")
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(label: String, selectedDate: String, onDateSelected: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    Box(modifier = Modifier.fillMaxWidth().clickable { showDialog = true }) {
        OutlinedTextField(
            value = selectedDate,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            trailingIcon = {
                Icon(Icons.Default.DateRange, contentDescription = "Pick Date")
            }
        )
    }

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        onDateSelected(formatter.format(Date(millis)))
                    }
                    showDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
