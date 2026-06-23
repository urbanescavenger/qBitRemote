package com.jbcbros.qbitremote.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.res.stringResource
import com.jbcbros.qbitremote.R
import com.jbcbros.qbitremote.ui.home.HomeScreen
import com.jbcbros.qbitremote.ui.settings.SettingsScreen
import com.jbcbros.qbitremote.ui.upload.UploadScreen
import com.jbcbros.qbitremote.ui.detail.TorrentDetailScreen

sealed class Screen(val route: String, val title: String) {
    data object Home : Screen("home", "首页")
    data object Settings : Screen("settings", "设置")
    data object Upload : Screen("upload", "添加")
    data object Detail : Screen("detail/{hash}/{name}", "详情")
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }

    val showBottomBar = currentRoute == Screen.Home.route || currentRoute == Screen.Settings.route

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val items = listOf(
                        Triple(Screen.Home, Icons.Default.Home, stringResource(R.string.nav_home)),
                        Triple(Screen.Settings, Icons.Default.Settings, stringResource(R.string.nav_settings))
                    )
                    items.forEach { (screen, icon, label) ->
                        val selected = navBackStackEntry?.destination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToUpload = { navController.navigate(Screen.Upload.route) },
                    onNavigateToDetail = { hash, name ->
                        navController.navigate("detail/$hash/$name")
                    }
                )
            }
            composable(Screen.Upload.route) {
                UploadScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Detail.route) { backStackEntry ->
                val hash = backStackEntry.arguments?.getString("hash") ?: ""
                val name = backStackEntry.arguments?.getString("name") ?: ""
                TorrentDetailScreen(
                    hash = hash,
                    name = name,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
