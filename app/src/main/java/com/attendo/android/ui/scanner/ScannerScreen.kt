package com.attendo.android.ui.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var sessionInput by remember { mutableStateOf("") }
    var showManualEntry by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isSessionActive) {
            CameraPreview(
                onQrCodeScanned = viewModel::onQrScanned
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text("Camera Offline. Start a session.", color = Color.White)
            }
        }

        // Top Control Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (!uiState.isSessionActive) {
                    OutlinedTextField(
                        value = sessionInput,
                        onValueChange = { sessionInput = it },
                        label = { Text("Session Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    Text("Active: ${uiState.sessionName}", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Button(
                    onClick = { viewModel.toggleSession(sessionInput) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isSessionActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (uiState.isSessionActive) "Stop Session" else "Start Session")
                }
            }
        }

        // Bottom Controls
        if (uiState.isSessionActive) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ElevatedButton(onClick = { showManualEntry = true }) {
                    Text("Manual Entry")
                }
                ElevatedButton(onClick = { /* TODO: Phase 4 Bottom Sheet Integration */ }) {
                    Text("Rapid Entry")
                }
            }
        }

        // Feedback Overlay
        if (uiState.lastScannedMessage != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .padding(24.dp)
            ) {
                Text(
                    text = uiState.lastScannedMessage!!,
                    color = if (uiState.lastScannedMessage!!.startsWith("Success")) Color.Green else Color.Red,
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
    }

    if (showManualEntry) {
        ManualEntryDialog(
            onDismiss = { showManualEntry = false },
            onSubmit = { id -> 
                viewModel.onManualEntry(id) 
                showManualEntry = false
            }
        )
    }
}

@Composable
fun ManualEntryDialog(onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var idInput by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manual Entry") },
        text = {
            OutlinedTextField(
                value = idInput,
                onValueChange = { idInput = it },
                label = { Text("National ID") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(idInput) }) { Text("Submit") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
