package com.maprandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.maprandroid.data.local.AppDatabase
import com.maprandroid.data.local.SettingsRepository
import com.maprandroid.data.remote.NetworkModule
import com.maprandroid.data.repository.ConfigProfileRepository
import com.maprandroid.data.repository.GameRepository
import com.maprandroid.data.repository.MapRandoRepository
import com.maprandroid.navigation.GenerateRoute
import com.maprandroid.navigation.LibraryDetailRoute
import com.maprandroid.navigation.LibraryListRoute
import com.maprandroid.navigation.PresetsRoute
import com.maprandroid.navigation.SettingsRoute
import com.maprandroid.ui.config.ConfigViewModel
import com.maprandroid.ui.config.ConfigViewModelFactory
import com.maprandroid.ui.generate.GenerateScreen
import com.maprandroid.ui.library.LibraryDetailPane
import com.maprandroid.ui.library.LibraryListPane
import com.maprandroid.ui.library.LibraryViewModel
import com.maprandroid.ui.library.LibraryViewModelFactory
import com.maprandroid.ui.presets.PresetsScreen
import com.maprandroid.ui.settings.SettingsScreen
import com.maprandroid.ui.settings.SettingsViewModel
import com.maprandroid.ui.settings.SettingsViewModelFactory
import com.maprandroid.ui.theme.SuperMetroidMapRandoTheme
import com.maprandroid.util.EmulatorLauncher

class MainActivity : ComponentActivity() {

    private val configViewModel by viewModels<ConfigViewModel> {
        val database = AppDatabase.getDatabase(applicationContext)
        ConfigViewModelFactory(
            MapRandoRepository(NetworkModule.mapRandoApiService),
            GameRepository(database.gameDao()),
            ConfigProfileRepository(database.configProfileDao()),
            SettingsRepository(applicationContext)
        )
    }
    
    private val libraryViewModel by viewModels<LibraryViewModel> {
        val database = AppDatabase.getDatabase(applicationContext)
        LibraryViewModelFactory(
            GameRepository(database.gameDao()),
            SettingsRepository(applicationContext)
        )
    }

    private val settingsViewModel by viewModels<SettingsViewModel> {
        SettingsViewModelFactory(
            SettingsRepository(applicationContext),
            applicationContext
        )
    }

    @OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SuperMetroidMapRandoTheme {
                val backStack = remember { mutableStateListOf<NavKey>(GenerateRoute) }
                val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()
                val configuration = LocalConfiguration.current
                val isWideScreen = configuration.screenWidthDp >= 600
                val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                val showNavRail = isWideScreen || isLandscape

                val context = LocalContext.current
                LaunchedEffect(isLandscape) {
                    val window = (context as? android.app.Activity)?.window ?: return@LaunchedEffect
                    val insetsController = WindowInsetsControllerCompat(window, window.decorView)
                    if (isLandscape) {
                        insetsController.hide(WindowInsetsCompat.Type.systemBars())
                        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    } else {
                        insetsController.show(WindowInsetsCompat.Type.systemBars())
                    }
                }

                Row(modifier = Modifier.fillMaxSize()) {
                    if (showNavRail) {
                        NavigationRail {
                            val currentRoute = backStack.lastOrNull()
                            val isGenerateSelected = currentRoute == GenerateRoute
                            val isPresetsSelected = currentRoute == PresetsRoute
                            val isLibrarySelected = currentRoute == LibraryListRoute || currentRoute is LibraryDetailRoute
                            val isSettingsSelected = currentRoute == SettingsRoute

                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

                            NavigationRailItem(
                                selected = isGenerateSelected,
                                onClick = { 
                                    backStack.clear()
                                    backStack.add(GenerateRoute) 
                                },
                                icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Generate") },
                                label = { Text("Generate") }
                            )
                            NavigationRailItem(
                                selected = isPresetsSelected,
                                onClick = { 
                                    backStack.clear()
                                    backStack.add(PresetsRoute) 
                                },
                                icon = { Icon(Icons.Default.Tune, contentDescription = "Presets") },
                                label = { Text("Presets") }
                            )
                            NavigationRailItem(
                                selected = isLibrarySelected,
                                onClick = { 
                                    backStack.clear()
                                    backStack.add(LibraryListRoute) 
                                },
                                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Library") },
                                label = { Text("Library") }
                            )
                            NavigationRailItem(
                                selected = isSettingsSelected,
                                onClick = { 
                                    backStack.clear()
                                    backStack.add(SettingsRoute) 
                                },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text("Settings") }
                            )
                        }
                    }

                    Scaffold(
                        modifier = Modifier.weight(1f),
                        bottomBar = {
                            if (!showNavRail) {
                                NavigationBar {
                                    val currentRoute = backStack.lastOrNull()
                                    val isGenerateSelected = currentRoute == GenerateRoute
                                    val isPresetsSelected = currentRoute == PresetsRoute
                                    val isLibrarySelected = currentRoute == LibraryListRoute || currentRoute is LibraryDetailRoute
                                    val isSettingsSelected = currentRoute == SettingsRoute

                                    NavigationBarItem(
                                        selected = isGenerateSelected,
                                        onClick = { 
                                            backStack.clear()
                                            backStack.add(GenerateRoute) 
                                        },
                                        icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Generate") },
                                        label = { Text("Generate") }
                                    )
                                    NavigationBarItem(
                                        selected = isPresetsSelected,
                                        onClick = { 
                                            backStack.clear()
                                            backStack.add(PresetsRoute) 
                                        },
                                        icon = { Icon(Icons.Default.Tune, contentDescription = "Presets") },
                                        label = { Text("Presets") }
                                    )
                                    NavigationBarItem(
                                        selected = isLibrarySelected,
                                        onClick = { 
                                            backStack.clear()
                                            backStack.add(LibraryListRoute) 
                                        },
                                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Library") },
                                        label = { Text("Library") }
                                    )
                                    NavigationBarItem(
                                        selected = isSettingsSelected,
                                        onClick = { 
                                            backStack.clear()
                                            backStack.add(SettingsRoute) 
                                        },
                                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                        label = { Text("Settings") }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        NavDisplay(
                            backStack = backStack,
                            modifier = Modifier.padding(innerPadding).fillMaxSize(),
                            onBack = { backStack.removeLastOrNull() },
                            sceneStrategy = listDetailStrategy,
                            entryProvider = entryProvider {
                                entry<PresetsRoute> {
                                    PresetsScreen(viewModel = configViewModel)
                                }
                                entry<GenerateRoute> {
                                    GenerateScreen(
                                        viewModel = configViewModel,
                                        onEditPresetClick = {
                                            backStack.clear()
                                            backStack.add(PresetsRoute)
                                        }
                                    )
                                }
                                entry<LibraryListRoute>(
                                    metadata = ListDetailSceneStrategy.listPane(
                                        detailPlaceholder = {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.surfaceContainer),
                                                contentAlignment = androidx.compose.ui.Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Select a game to view details",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    )
                                ) {
                                    LibraryListPane(
                                        viewModel = libraryViewModel,
                                        onGameClick = { gameId ->
                                            backStack.removeAll { it is LibraryDetailRoute }
                                            backStack.add(LibraryDetailRoute(gameId))
                                        }
                                    )
                                }
                                entry<LibraryDetailRoute>(
                                    metadata = ListDetailSceneStrategy.detailPane()
                                ) { route ->
                                    LibraryDetailPane(
                                        gameId = route.gameId,
                                        viewModel = libraryViewModel,
                                        onPlayClick = { _, romPath ->
                                            val packageName = libraryViewModel.uiState.value.emulatorPackageName
                                            launchEmulator(romPath, packageName)
                                        },
                                        onBackClick = {
                                            backStack.removeLastOrNull()
                                        }
                                    )
                                }
                                entry<SettingsRoute> {
                                    SettingsScreen(viewModel = settingsViewModel)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    
    private fun launchEmulator(romPath: String, packageName: String?) {
        if (!EmulatorLauncher.launchEmulator(this, romPath, packageName)) {
            android.widget.Toast.makeText(this, "Could not launch emulator.", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}
