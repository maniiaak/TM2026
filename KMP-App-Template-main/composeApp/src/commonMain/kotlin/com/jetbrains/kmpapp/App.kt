package com.jetbrains.kmpapp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.jetbrains.kmpapp.data.SessionManager
import com.jetbrains.kmpapp.screens.auth.LoginScreen
import com.jetbrains.kmpapp.screens.detail.DetailScreen
import com.jetbrains.kmpapp.screens.list.ListScreen
import com.jetbrains.kmpapp.screens.profile.ProfileScreen
import com.jetbrains.kmpapp.screens.profile.ProfileViewModel
import com.jetbrains.kmpapp.screens.search.SearchScreen
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Serializable
object ListDestination

@Serializable
object LoginDestination

@Serializable
object SearchDestination

@Serializable
data class DetailDestination(val objectId: Int)

@Serializable
data class ProfileDestination(val userId: Int? = null)

@Composable
fun App(
    sessionManager: SessionManager = koinInject()
) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    ) {
        Surface {
            val navController: NavHostController = rememberNavController()

            // Collect the state from the Flow
            val isLoggedIn by sessionManager.isLoggedIn.collectAsState(initial = false)

            // If NOT logged in, show LoginScreen immediately and return
            if (!isLoggedIn) {
                LoginScreen(
                    onLoginSuccess = { username ->
                        navController.navigate(ListDestination) {
                            popUpTo(LoginDestination) { inclusive = true }
                        }
                    }
                )

                return@Surface
            }

            Scaffold(
                bottomBar = {
                    NavigationBar (
                        modifier = Modifier.height(100.dp)
                        ){
                        val currentRoute = navController.currentBackStackEntryFlow.collectAsState(initial = null).value?.destination?.route ?: ""
                        // current session user id to allow opening the profile page for the current user
                        val currentSessionUserId by sessionManager.userId.collectAsState(initial = 0)

                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            selected = currentRoute.contains("list"),
                            onClick = {
                                navController.navigate(ListDestination) {
                                    popUpTo(navController.graph.findStartDestination().id)
                                }
                            }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            selected = currentRoute.contains("search"),
                            onClick = {
                                navController.navigate(SearchDestination) {
                                    popUpTo(navController.graph.findStartDestination().id)
                                }
                            }
                        )
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Profile"
                                )
                            },
                            selected = currentRoute.contains("profile"),
                            onClick = {
                                val idForNav = if (currentSessionUserId != 0) currentSessionUserId else null
                                navController.navigate(ProfileDestination(idForNav)) {
                                    popUpTo(navController.graph.findStartDestination().id)
                                }
                            }
                        )
                    }
                }
            ) { paddingValues ->
                NavHost(
                    navController = navController,
                    startDestination = ListDestination
                ) {
                    composable<ListDestination> {
                        ListScreen(
                            navigateToDetails = { id ->
                                navController.navigate(DetailDestination(id))
                            },
                            onLogout = {
                                sessionManager.logout()
                                navController.navigate(LoginDestination) {
                                    popUpTo(ListDestination) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable<DetailDestination> { backStackEntry ->
                        val objectId = backStackEntry.toRoute<DetailDestination>().objectId
                        DetailScreen(
                            objectId = objectId,
                            navigateBack = { navController.popBackStack() },
                            navigateToUserProfile = { userId ->
                                navController.navigate(ProfileDestination(userId = userId))
                            }
                        )
                    }

                    composable<SearchDestination> {
                        SearchScreen(
                            onAlbumFound = { id ->
                                navController.navigate(DetailDestination(id))
                            },
                            onError = { message ->
                                println("Search Error: $message")
                            }
                        )
                    }

                    composable<LoginDestination> {
                        LoginScreen(
                            onLoginSuccess = { username ->
                                navController.navigate(ListDestination) {
                                    popUpTo(LoginDestination) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable<ProfileDestination> { backStackEntry ->
                        val currentSessionUserId by sessionManager.userId.collectAsState(initial = 0)

                        // extract the userId from the destination and pass it to the ProfileViewModel
                        val userId = backStackEntry.toRoute<ProfileDestination>().userId

                        println("Trying to resolve ProfileViewModel for userId=$userId")

                        // Determine if this is the current user's own profile
                        val isOwnProfile = if (userId != null) {
                            userId == currentSessionUserId
                        } else {
                            true // If no userId, we're viewing current user's profile
                        }

                        val profileViewModel: ProfileViewModel = org.koin.compose.viewmodel.koinViewModel(parameters = {
                            org.koin.core.parameter.parametersOf(userId)
                        })

                        ProfileScreen(
                            viewModel = profileViewModel,
                            navigateToAlbum = { albumId ->
                                navController.navigate(DetailDestination(albumId))
                            },
                            isOwnProfile = isOwnProfile
                        )
                    }
                }
            }
        }
    }
}