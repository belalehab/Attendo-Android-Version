package com.attendo.android.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    activeWorkspace: String,
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showManualEntry by remember { mutableStateOf(false) }

    LaunchedEffect(activeWorkspace) {
        viewModel.loadInstructorAndSubject(activeWorkspace)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.toggleSession()
            } else {
                // Handle permission denial if needed
            }
        }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Background Camera Preview
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
                    imageVector = Icons.Default.VolumeUp,
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
                                // Type
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

                                // Group
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

                            // Inputs row 2
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
                                viewModel.toggleSession()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.isSessionActive) MaterialTheme.colorScheme.error else Color(0xFF14B8A6),
                            contentColor = if (uiState.isSessionActive) Color.White else Color.Black
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
            
            AnimatedVisibility(visible = !uiState.isSessionActive) {
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
                            text = "249", // Hardcoded placeholder matching the desktop screenshot
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // Bottom Controls for Manual Entry when active
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
