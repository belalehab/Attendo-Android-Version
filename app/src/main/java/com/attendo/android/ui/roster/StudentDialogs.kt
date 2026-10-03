package com.attendo.android.ui.roster

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*

@Composable
fun AddStudentDialog(
    onDismiss: () -> Unit,
    onSubmit: (name: String, nationalId: String) -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    var idInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Student") },
        text = {
            Column {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Name") }
                )
                OutlinedTextField(
                    value = idInput,
                    onValueChange = { idInput = it },
                    label = { Text("National ID") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(nameInput, idInput) }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditStudentDialog(
    initialName: String,
    initialId: String,
    onDismiss: () -> Unit,
    onSubmit: (name: String, nationalId: String) -> Unit
) {
    var nameInput by remember { mutableStateOf(initialName) }
    var idInput by remember { mutableStateOf(initialId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Student") },
        text = {
            Column {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Name") }
                )
                OutlinedTextField(
                    value = idInput,
                    onValueChange = { idInput = it },
                    label = { Text("National ID") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(nameInput, idInput) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
