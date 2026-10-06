package com.example.callscribe

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.callscribe.ui.CallDetailScreen
import com.example.callscribe.ui.CallDetailViewModel
import com.example.callscribe.ui.HomeScreen
import com.example.callscribe.ui.HomeViewModel
import com.example.callscribe.ui.ModelManagerScreen
import com.example.callscribe.ui.ModelManagerViewModel
import com.example.callscribe.ui.PermissionsScreen
import com.example.callscribe.ui.Screen
import com.example.callscribe.ui.SearchScreen
import com.example.callscribe.ui.SettingsScreen
import com.example.callscribe.ui.SettingsViewModel

@Composable
fun MainNavigation(
    homeViewModel: HomeViewModel,
    callDetailViewModel: CallDetailViewModel,
    settingsViewModel: SettingsViewModel,
    modelManagerViewModel: ModelManagerViewModel,
    modifier: Modifier = Modifier
) {
    val hasAcceptedConsent by homeViewModel.hasAcceptedConsent.collectAsState()

    val backStack = remember {
        mutableStateListOf<Screen>(
            if (hasAcceptedConsent) Screen.Home else Screen.Permissions
        )
    }

    val currentScreen = backStack.lastOrNull() ?: Screen.Home

    BackHandler(enabled = backStack.size > 1) {
        backStack.removeLastOrNull()
    }

    when (currentScreen) {
        Screen.Permissions -> {
            PermissionsScreen(
                onContinue = {
                    settingsViewModel.setHasAcceptedConsent(true)
                    backStack.clear()
                    backStack.add(Screen.Home)
                },
                onOpenBatterySettings = {
                    settingsViewModel.openBatteryOptimizationSettings()
                }
            )
        }

        Screen.Home -> {
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToDetail = { id -> backStack.add(Screen.CallDetail(id)) },
                onNavigateToSearch = { backStack.add(Screen.Search) },
                onNavigateToSettings = { backStack.add(Screen.Settings) },
                onNavigateToModels = { backStack.add(Screen.ModelManager) }
            )
        }

        is Screen.CallDetail -> {
            CallDetailScreen(
                recordingId = currentScreen.recordingId,
                viewModel = callDetailViewModel,
                onBack = { backStack.removeLastOrNull() }
            )
        }

        Screen.Search -> {
            SearchScreen(
                viewModel = homeViewModel,
                onNavigateToDetail = { id -> backStack.add(Screen.CallDetail(id)) },
                onBack = { backStack.removeLastOrNull() }
            )
        }

        Screen.Settings -> {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { backStack.removeLastOrNull() }
            )
        }

        Screen.ModelManager -> {
            ModelManagerScreen(
                viewModel = modelManagerViewModel,
                onBack = { backStack.removeLastOrNull() }
            )
        }
    }
}
