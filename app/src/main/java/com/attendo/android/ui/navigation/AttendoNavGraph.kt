package com.attendo.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.attendo.android.ui.licensing.LicenseScreen
import com.attendo.android.ui.setup.SetupScreen
import com.attendo.android.ui.shell.MainShellScreen
import com.attendo.android.ui.shell.MainViewModel
import com.attendo.android.ui.workspace.WorkspaceSelectorScreen
import com.attendo.android.ui.settings.SettingsScreen
import com.attendo.android.ui.settings.AboutScreen

@Composable
fun AttendoNavGraph() {
    val navController = rememberNavController()
    // We hoist MainViewModel here to share activeWorkspace globally across the shell
    val mainViewModel: MainViewModel = hiltViewModel()
    
    NavHost(navController = navController, startDestination = "license") {
        composable("license") {
            LicenseScreen(
                onLicenseValid = { navController.navigate("setup") { popUpTo("license") { inclusive = true } } }
            )
        }
        composable("setup") {
            SetupScreen(
                onSetupComplete = { navController.navigate("workspace_selector") { popUpTo("setup") { inclusive = true } } }
            )
        }
        composable("workspace_selector") {
            WorkspaceSelectorScreen(
                onWorkspaceSelected = { workspace ->
                    mainViewModel.setWorkspace(workspace)
                    navController.navigate("main_shell") { popUpTo("workspace_selector") { inclusive = true } }
                },
                onEditSetup = {
                    navController.navigate("setup")
                }
            )
        }
        composable("main_shell") {
            MainShellScreen(
                mainViewModel = mainViewModel,
                onChangeWorkspace = {
                    navController.navigate("workspace_selector") { popUpTo("main_shell") { inclusive = true } }
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToAbout = {
                    navController.navigate("about")
                }
            )
        }
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable("about") {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
