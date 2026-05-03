package com.jetbrains.kmpapp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.jetbrains.kmpapp.screens.create.CreateScreen
import com.jetbrains.kmpapp.screens.detail.DetailScreen
import com.jetbrains.kmpapp.screens.list.ListScreen
import com.jetbrains.kmpapp.screens.profile.ProfileScreen
import com.jetbrains.kmpapp.screens.search.SearchScreen
import kotlinx.serialization.Serializable

@Serializable
object ListDestination

@Serializable
data class DetailDestination(val objectId: Int)

@Serializable
object SearchDestination

@Serializable
object CreateDestination

@Serializable
object ProfileDestination

enum class BottomNavItem(val label: String, val icon: androidx.compose.material.icons.Icons.Filled) {
    HOME("Home", Icons.Filled.Home),
    SEARCH("Search", Icons.Filled.Search),
    CREATE("Create", Icons.Filled.Add),
    PROFILE("Profile", Icons.Filled.Person)
}

@Composable
fun App() {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    ) {
        Surface {
            val navController: NavHostController = rememberNavController()
            var selectedBottomNavItem by remember { mutableStateOf(BottomNavItem.HOME) }

            Scaffold(
                bottomBar = {
                    NavigationBar {
                        BottomNavItem.entries.forEach { item ->
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                selected = selectedBottomNavItem == item,
                                onClick = {
                                    selectedBottomNavItem = item
                                    when (item) {
                                        BottomNavItem.HOME -> {
                                            navController.navigate(ListDestination) {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                restoreState = true
                                                launchSingleTop = true
                                            }
                                        }
                                        BottomNavItem.SEARCH -> {
                                            navController.navigate(SearchDestination) {
                                                launchSingleTop = true
                                            }
                                        }
                                        BottomNavItem.CREATE -> {
                                            navController.navigate(CreateDestination) {
                                                launchSingleTop = true
                                            }
                                        }
                                        BottomNavItem.PROFILE -> {
                                            navController.navigate(ProfileDestination) {
                                                launchSingleTop = true
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            ) { paddingValues ->
                NavHost(
                    navController = navController,
                    startDestination = ListDestination,
                    modifier = Modifier.padding(paddingValues)
                ) {
                    composable<ListDestination> {
                        selectedBottomNavItem = BottomNavItem.HOME
                        ListScreen(navigateToDetails = { objectId ->
                            navController.navigate(DetailDestination(objectId))
                        })
                    }
                    composable<DetailDestination> { backStackEntry ->
                        DetailScreen(
                            objectId = backStackEntry.toRoute<DetailDestination>().objectId,
                            navigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                    composable<SearchDestination> {
                        SearchScreen()
                    }
                    composable<CreateDestination> {
                        CreateScreen()
                    }
                    composable<ProfileDestination> {
                        ProfileScreen()
                    }
                }
            }
        }
    }
}