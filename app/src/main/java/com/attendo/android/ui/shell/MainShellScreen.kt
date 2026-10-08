package com.attendo.android.ui.shell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.attendo.android.ui.scanner.ScannerScreen
import com.attendo.android.ui.tabs.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    val searchQuery by mainViewModel.searchQuery.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Attendo",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 28.dp))
                Spacer(modifier = Modifier.height(16.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.ExitToApp, contentDescription = null) },
                    label = { Text("Switch Workspace") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        mainViewModel.clearWorkspace()
                        onChangeWorkspace()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToSettings()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    label = { Text("About") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToAbout()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                // Modern Floating Search Bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(32.dp)),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Menu")
                        }
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = mainViewModel::onSearchQueryChanged,
                            placeholder = { Text("Search ${activeWorkspace ?: ""}...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            )
                        )
                        IconButton(onClick = { /* Placeholder for profile/account action */ }) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Account")
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
                        icon = { Icon(Icons.Default.List, contentDescription = "Analytics") },
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
            NavHost(
                navController = navController,
                startDestination = "scanner",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("scanner") { 
                    ScannerScreen(activeWorkspace = activeWorkspace ?: "") 
                }
                composable("roster") { 
                    RosterScreen(activeWorkspace = activeWorkspace, searchQuery = searchQuery) 
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
