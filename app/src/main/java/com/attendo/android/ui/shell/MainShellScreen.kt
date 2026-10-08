package com.attendo.android.ui.shell

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.attendo.android.ui.scanner.ScannerScreen
import com.attendo.android.ui.tabs.*

@Composable
fun MainShellScreen(
    mainViewModel: MainViewModel,
    onChangeWorkspace: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "scanner"
    
    val activeWorkspace by mainViewModel.activeWorkspace.collectAsState()
    // searchQuery is no longer needed in the universal shell, Roster handles its own.
    
    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF0F172A),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Attendo Logo & Title
                    Icon(
                        imageVector = Icons.Default.ChangeHistory, // Stylized 'A' placeholder
                        contentDescription = "Attendo Logo",
                        tint = Color(0xFF14B8A6),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Attendo",
                        color = Color(0xFF14B8A6),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Global Actions
                    IconButton(onClick = onNavigateToAbout) {
                        Icon(Icons.Outlined.Info, contentDescription = "About", tint = Color.Gray)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = Color.Gray)
                    }
                    IconButton(
                        onClick = {
                            mainViewModel.clearWorkspace()
                            onChangeWorkspace()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp, 
                            contentDescription = "Switch Workspace", 
                            tint = Color.Gray
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Scanner") },
                    label = { Text("Scanner") },
                    selected = currentRoute == "scanner",
                    onClick = { navController.navigate("scanner") { launchSingleTop = true } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Roster") },
                    label = { Text("Roster") },
                    selected = currentRoute == "roster",
                    onClick = { navController.navigate("roster") { launchSingleTop = true } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "History") },
                    label = { Text("History") },
                    selected = currentRoute == "history",
                    onClick = { navController.navigate("history") { launchSingleTop = true } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Menu, contentDescription = "Analytics") },
                    label = { Text("Analytics") },
                    selected = currentRoute == "analytics",
                    onClick = { navController.navigate("analytics") { launchSingleTop = true } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Lock, contentDescription = "Vault") },
                    label = { Text("Vault") },
                    selected = currentRoute == "vault",
                    onClick = { navController.navigate("vault") { launchSingleTop = true } }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = "scanner",
                modifier = Modifier.fillMaxSize()
            ) {
                composable("scanner") { 
                    ScannerScreen(activeWorkspace = activeWorkspace ?: "") 
                }
                composable("roster") { 
                    RosterScreen(
                        activeWorkspace = activeWorkspace, 
                        searchQuery = "" // Localized search handled inside Roster
                    ) 
                }
                composable("history") { 
                    HistoryScreen(activeWorkspace = activeWorkspace) 
                }
                composable("analytics") { 
                    AnalyticsScreen(activeWorkspace = activeWorkspace) 
                }
                composable("vault") { 
                    VaultScreen(activeWorkspace = activeWorkspace) 
                }
            }
        }
    }
}
