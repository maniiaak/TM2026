package com.maniiaak.iluvmusic

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.maniaak.iluvmusic.data.SessionManager
import com.maniaak.iluvmusic.screens.auth.LoginScreen
import com.maniaak.iluvmusic.screens.detail.DetailScreen
import com.maniaak.iluvmusic.screens.list.CategoryDetailScreen
import com.maniaak.iluvmusic.screens.list.ListScreen
import com.maniaak.iluvmusic.screens.profile.ProfileScreen
import com.maniaak.iluvmusic.screens.profile.ProfileViewModel
import com.maniaak.iluvmusic.screens.search.SearchScreen
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

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

@Serializable
data class CategoryDetailDestination(val category: String)

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
                            navigateToCategory = { category ->
                                navController.navigate(CategoryDetailDestination(category))
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

                     composable<CategoryDetailDestination> { backStackEntry ->
                         val category = backStackEntry.toRoute<CategoryDetailDestination>().category
                         CategoryDetailScreen(
                             category = category,
                             navigateToDetails = { id ->
                                 navController.navigate(DetailDestination(id))
                             },
                             navigateBack = { navController.popBackStack() }
                         )
                     }
                }
            }
        }
    }
}