package io.github.redballoonx.fableverse.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object LibraryScreen : Screen("library", "Library", Icons.Default.Home)
    object PlayerScreen : Screen("player", "Player", Icons.Default.PlayArrow)
    object SettingsScreen : Screen("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        Screen.LibraryScreen,
        Screen.PlayerScreen,
        Screen.SettingsScreen
    )
    // Aktuelle Route herausfinden
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    NavigationBar {
        items.forEach { screen ->
            NavigationBarItem(
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        // Verhindert mehrfache Copies im Stack
                        launchSingleTop = true
                        // Springt zu Start-Destination beim Home-Klick
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.title
                    )
                },
                label = { Text(screen.title) }
            )
        }
    }
}
