package io.github.redballoonx.fableverse

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.redballoonx.fableverse.data.local.AudiobookDatabase
import io.github.redballoonx.fableverse.data.local.PreferencesManager
import io.github.redballoonx.fableverse.data.metadata.MetadataExtractor
import io.github.redballoonx.fableverse.data.repository.AudiobookRepository
import io.github.redballoonx.fableverse.player.PlayerManager
import io.github.redballoonx.fableverse.ui.library.LibraryScreen
import io.github.redballoonx.fableverse.ui.library.LibraryViewModel
import io.github.redballoonx.fableverse.ui.navigation.BottomNavigationBar
import io.github.redballoonx.fableverse.ui.navigation.Screen
import io.github.redballoonx.fableverse.ui.player.PlayerScreen
import io.github.redballoonx.fableverse.ui.player.PlayerViewModel
import io.github.redballoonx.fableverse.ui.settings.SettingsScreen
import io.github.redballoonx.fableverse.ui.settings.SettingsViewModel
import io.github.redballoonx.fableverse.ui.theme.FableVerseTheme

class MainActivity : ComponentActivity() {

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            settingsViewModel.onFolderSelected(it.toString())
        }
    }

    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var libraryViewModel: LibraryViewModel
    private lateinit var playerManager: PlayerManager
    private lateinit var repository: AudiobookRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Dependencies initialisieren
        val prefsManager = PreferencesManager(this)
        val database = AudiobookDatabase.getDatabase(this)
        val metadataExtractor = MetadataExtractor(this)
        repository = AudiobookRepository(this, prefsManager, database, metadataExtractor)
        playerManager = PlayerManager(this)

        settingsViewModel = SettingsViewModel(repository)
        libraryViewModel = LibraryViewModel(repository)

        setContent {
            FableVerseTheme {
                MainScreen(
                    settingsViewModel = settingsViewModel,
                    libraryViewModel = libraryViewModel,
                    playerManager = playerManager,
                    repository = repository,
                    onSelectFolder = { folderPickerLauncher.launch(null) }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        playerManager.release()
    }

    @Composable
    fun MainScreen(
        settingsViewModel: SettingsViewModel,
        libraryViewModel: LibraryViewModel,
        playerManager: PlayerManager,
        repository: AudiobookRepository,
        onSelectFolder: () -> Unit
    ) {
        val navController = rememberNavController()

        Scaffold(
            bottomBar = {
                // Bottom Bar immer anzeigen
                BottomNavigationBar(navController)
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = Screen.LibraryScreen.route,
                modifier = Modifier.padding(paddingValues)
            ) {
                // Library
                composable(Screen.LibraryScreen.route) {
                    LibraryScreen(
                        viewModel = libraryViewModel,
                        onAudiobookClick = { audiobookId ->
                            navController.navigate(Screen.AudiobookPlayer.createRoute(audiobookId))
                        }
                    )
                }

                // Player mit Parameter (echter Player)
                composable(
                    route = Screen.AudiobookPlayer.route,
                    arguments = listOf(
                        navArgument(Screen.AudiobookPlayer.audiobookIdArg) {
                            type = NavType.LongType
                        }
                    )
                ) { backStackEntry ->
                    val audiobookId = backStackEntry.arguments?.getLong(
                        Screen.AudiobookPlayer.audiobookIdArg
                    ) ?: 0L

                    // ViewModel wird mit remember gecached - wird nicht bei jedem Recompose neu erstellt
                    val playerViewModel = remember(audiobookId) {
                        android.util.Log.d("MainActivity", "Creating PlayerViewModel for audiobook $audiobookId")
                        PlayerViewModel(repository, playerManager)
                    }

                    // Cleanup wenn diese Route verlassen wird
                    DisposableEffect(audiobookId) {
                        android.util.Log.d("MainActivity", "PlayerScreen entered for audiobook $audiobookId")
                        onDispose {
                            android.util.Log.d("MainActivity", "PlayerScreen disposed for audiobook $audiobookId")
                        }
                    }

                    PlayerScreen(
                        viewModel = playerViewModel,
                        audiobookId = audiobookId,
                        onBack = {
                            android.util.Log.d("MainActivity", "Back button pressed in PlayerScreen")
                            navController.popBackStack()
                        }
                    )
                }

                // Player-Tab (Placeholder für Bottom Navigation)
                composable(Screen.PlayerScreen.route) {
                    PlayerPlaceholderScreen()
                }

                // Settings
                composable(Screen.SettingsScreen.route) {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onSelectFolderClick = onSelectFolder
                    )
                }
            }
        }
    }

    @Composable
    fun PlayerPlaceholderScreen() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Wähle ein Hörbuch aus der Bibliothek",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}