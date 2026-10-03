package com.attendo.android.ui.shell

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.attendo.android.ui.scanner.ScannerScreen
import com.attendo.android.ui.tabs.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShellScreen(
    mainViewModel: MainViewModel,
    onChangeWorkspace: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "scanner"
    
    val activeWorkspace by mainViewModel.activeWorkspace.collectAsState()
    val searchQuery by mainViewModel.searchQuery.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = mainViewModel::onSearchQueryChanged,
                        placeholder = { Text("Search ${activeWorkspace ?: ""}...") },
                        modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                actions = {
                    IconButton(onClick = { 
                        mainViewModel.clearWorkspace()
                        onChangeWorkspace() 
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Change Workspace")
                    }
                }
            )
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
                    icon = { Icon(Icons.Default.Info, contentDescription = "Analytics") },
                    label = { Text("Analytics") },
                    selected = currentRoute == "analytics",
                    onClick = { navController.navigate("analytics") { launchSingleTop = true } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Vault") },
                    label = { Text("Vault") },
                    selected = currentRoute == "vault",
                    onClick = { navController.navigate("vault") { launchSingleTop = true } }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "scanner",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("scanner") { ScannerScreen() }
            composable("roster") { 
                RosterScreen(
                    activeWorkspace = activeWorkspace,
                    searchQuery = searchQuery
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
