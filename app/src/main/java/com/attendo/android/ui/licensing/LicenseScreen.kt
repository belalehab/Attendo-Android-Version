package com.attendo.android.ui.licensing

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LicenseScreen(
    viewModel: LicenseViewModel = hiltViewModel(),
    onLicenseValid: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isValid) {
        if (uiState.isValid) {
            onLicenseValid()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Attendo Activation", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(text = "Hardware ID:")
        Text(text = uiState.hardwareId, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = uiState.tokenInput,
            onValueChange = viewModel::onTokenChanged,
            label = { Text("License Token") },
            modifier = Modifier.fillMaxWidth()
        )
        
        if (uiState.errorMessage != null) {
            Text(text = uiState.errorMessage!!, color = MaterialTheme.colorScheme.error)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = viewModel::activateLicense,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Activate")
        }
    }
}
