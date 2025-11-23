package io.github.redballoonx.fableverse

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.redballoonx.fableverse.data.local.AudiobookDatabase
import io.github.redballoonx.fableverse.data.local.PreferencesManager
import io.github.redballoonx.fableverse.data.repository.AudiobookRepository
import io.github.redballoonx.fableverse.ui.library.LibraryScreen
import io.github.redballoonx.fableverse.ui.library.LibraryViewModel
import io.github.redballoonx.fableverse.ui.navigation.BottomNavigationBar
import io.github.redballoonx.fableverse.ui.navigation.Screen
import io.github.redballoonx.fableverse.ui.screens.PlayerScreen
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

            // Library wird automatisch aktualisiert durch Flow
        }
    }

    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var libraryViewModel: LibraryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Dependencies initialisieren
        val prefsManager = PreferencesManager(this)
        val database = AudiobookDatabase.getDatabase(this)
        val repository = AudiobookRepository(this, prefsManager, database)

        settingsViewModel = SettingsViewModel(repository)
        libraryViewModel = LibraryViewModel(repository)

        setContent {
            FableVerseTheme {
                MainScreen(
                    settingsViewModel = settingsViewModel,
                    libraryViewModel = libraryViewModel,
                    onSelectFolder = { folderPickerLauncher.launch(null) }
                )
            }
        }
    }

    @Composable
    fun MainScreen(
        settingsViewModel: SettingsViewModel,
        libraryViewModel: LibraryViewModel,
        onSelectFolder: () -> Unit
    ) {
        val navController = rememberNavController()

        Scaffold(
            bottomBar = {
                BottomNavigationBar(navController)
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = Screen.LibraryScreen.route,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(Screen.LibraryScreen.route) {
                    LibraryScreen(viewModel = libraryViewModel)
                }
                composable(Screen.PlayerScreen.route) {
                    PlayerScreen()
                }
                composable(Screen.SettingsScreen.route) {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onSelectFolderClick = onSelectFolder
                    )
                }
            }
        }
    }
}