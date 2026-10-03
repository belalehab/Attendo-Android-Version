package com.attendo.android.ui.tabs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendo.android.ui.analytics.AnalyticsViewModel
import com.attendo.android.ui.analytics.StudentStats

@Composable
fun AnalyticsScreen(
    activeWorkspace: String?,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri: Uri? -> uri?.let { viewModel.exportToExcel(it) } }
    )

    LaunchedEffect(activeWorkspace) {
        if (activeWorkspace != null) {
            viewModel.loadAnalytics(activeWorkspace)
        }
    }

    LaunchedEffect(uiState.exportResult) {
        if (uiState.exportResult != null) {
            // In a real app we'd show a Snackbar here
            viewModel.clearExportResult()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Summary Header
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Sessions: ${uiState.totalSessions}", style = MaterialTheme.typography.titleMedium)
                        Text("At Risk Students: ${uiState.totalAtRisk}", color = MaterialTheme.colorScheme.error)
                    }
                    Button(onClick = { exportLauncher.launch("Attendo_Export_${activeWorkspace}.xlsx") }) {
                        Text("Export Excel")
                    }
                }
            }

            // Matrix List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.stats, key = { it.nationalId }) { stat ->
                    StatCard(stat)
                }
            }
        }

        if (uiState.isExporting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun StatCard(stat: StudentStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = if (stat.isAtRisk) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stat.studentName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (stat.isAtRisk) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Warning, contentDescription = "At Risk", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Attended: ${stat.attendedCount}", style = MaterialTheme.typography.bodySmall)
                    Text("Excused: ${stat.excusedCount}", style = MaterialTheme.typography.bodySmall)
                    Text("Bonus: ${stat.bonusPoints}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

