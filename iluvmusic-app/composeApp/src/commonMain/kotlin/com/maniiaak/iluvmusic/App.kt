package com.maniiaak.iluvmusic

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.maniiaak.iluvmusic.data.SessionManager
import com.maniiaak.iluvmusic.auth.FirebaseAuthManager
import com.maniiaak.iluvmusic.screens.auth.LoginScreen
import com.maniiaak.iluvmusic.screens.detail.DetailScreen
import com.maniiaak.iluvmusic.screens.list.CategoryDetailScreen
import com.maniiaak.iluvmusic.screens.list.ListScreen
import com.maniiaak.iluvmusic.screens.profile.ProfileScreen
import com.maniiaak.iluvmusic.screens.profile.ProfileViewModel
import com.maniiaak.iluvmusic.screens.search.SearchScreen
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Row
import com.maniiaak.iluvmusic.screens.settings.SettingsScreen
import com.maniiaak.iluvmusic.screens.settings.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip

val BrandPrimary = Color(0xFFEA42F9)

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

@Serializable
data class SettingsDestination(val userId: Int? = null)

@Composable
fun CompactBottomBar(
    currentRoute: String,
    onHomeClick: () -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomBarItem(
                icon = Icons.Default.Home,
                contentDescription = "Home",
                isSelected = currentRoute.contains("list"),
                onClick = onHomeClick
            )
            BottomBarItem(
                icon = Icons.Default.Search,
                contentDescription = "Search",
                isSelected = currentRoute.contains("search"),
                onClick = onSearchClick
            )
            BottomBarItem(
                icon = Icons.Default.Person,
                contentDescription = "Profile",
                isSelected = currentRoute.contains("profile"),
                onClick = onProfileClick
            )
        }
    }
}

@Composable
private fun BottomBarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
    } else {
        Color.Transparent
    }
    val tintColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = tintColor
        )
    }
}

@Composable
fun App(
    sessionManager: SessionManager = koinInject(),
    firebaseAuthManager: FirebaseAuthManager = koinInject()
) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) {
            darkColorScheme(primary = BrandPrimary)
        } else {
            lightColorScheme(primary = BrandPrimary)
        }
    ) {
        Surface {
            AppContentContainer {
                val navController: NavHostController = rememberNavController()
                val isLoggedIn by sessionManager.isLoggedIn.collectAsState(initial = false)

                // The login screen is owned by the session state. Once logout clears
                // the session, this branch is shown immediately and there is no
                // intermediate authenticated/loading screen.
                if (!isLoggedIn) {
                    LoginScreen(
                        onLoginSuccess = { _ ->
                            navController.navigate(ListDestination) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                        }
                    )
                    return@AppContentContainer
                }


                Scaffold(
                    bottomBar = {
                        val currentRoute = navController.currentBackStackEntryFlow
                            .collectAsState(initial = null).value?.destination?.route ?: ""
                        val currentSessionUserId by sessionManager.userId.collectAsState(initial = 0)

                        CompactBottomBar(
                            currentRoute = currentRoute,
                            onHomeClick = {
                                navController.navigate(ListDestination) {
                                    popUpTo(navController.graph.findStartDestination().id)
                                }
                            },
                            onSearchClick = {
                                navController.navigate(SearchDestination) {
                                    popUpTo(navController.graph.findStartDestination().id)
                                }
                            },
                            onProfileClick = {
                                val idForNav = if (currentSessionUserId != 0) currentSessionUserId else null
                                navController.navigate(ProfileDestination(idForNav)) {
                                    popUpTo(navController.graph.findStartDestination().id)
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    NavHost(
                        navController = navController,
                        startDestination = ListDestination,
                        modifier = Modifier.padding(paddingValues)
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
                                    // Sign out of Firebase as well as clearing the
                                    // local session. Clearing only storage leaves
                                    // Firebase's current user alive, which causes
                                    // LoginScreen to show its authenticated/loading state.
                                    firebaseAuthManager.signOut()
                                    sessionManager.logout()
                                    navController.popBackStack(
                                        navController.graph.findStartDestination().id,
                                        false
                                    )
                                }
                            )
                        }

                        composable<DetailDestination> (
                            enterTransition = {slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }, animationSpec = tween(450))},
                            popExitTransition = { slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }, animationSpec = tween(450))}
                        ){ backStackEntry ->
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

                        // Kept for compatibility with existing navigation calls.
                        // Normal authentication is rendered directly from the
                        // session state above.
                        composable<LoginDestination> {
                            LoginScreen(
                                onLoginSuccess = { _ ->
                                    navController.navigate(ListDestination) {
                                        popUpTo(LoginDestination) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable<ProfileDestination> { backStackEntry ->
                            val currentSessionUserId by sessionManager.userId.collectAsState(initial = 0)
                            val userId = backStackEntry.toRoute<ProfileDestination>().userId
                            val isOwnProfile = if (userId != null) {
                                userId == currentSessionUserId
                            } else {
                                true
                            }

                            val profileViewModel: ProfileViewModel = koinViewModel(
                                parameters = {
                                    parametersOf(userId)
                                }
                            )

                            ProfileScreen(
                                viewModel = profileViewModel,
                                navigateToAlbum = { albumId ->
                                    navController.navigate(DetailDestination(albumId))
                                },
                                navigateToSettings = {
                                    navController.navigate(SettingsDestination(userId = userId))
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

                        composable<SettingsDestination>(
                            enterTransition = {
                                slideInVertically(
                                    initialOffsetY = { fullHeight -> fullHeight },
                                    animationSpec = tween(450)
                                )
                            },
                            exitTransition = {
                                fadeOut(animationSpec = tween(225))
                            },
                            popExitTransition = {
                                slideOutVertically(
                                    targetOffsetY = { fullHeight -> fullHeight },
                                    animationSpec = tween(450)
                                )
                            },
                            popEnterTransition = {
                                fadeIn(animationSpec = tween(225))
                            }
                        ) { backStackEntry ->
                            val userId = backStackEntry.toRoute<SettingsDestination>().userId
                            val settingsViewModel: SettingsViewModel = koinViewModel(parameters = { parametersOf(userId) })

                            SettingsScreen(
                                viewModel = settingsViewModel,
                                navigateBack = { navController.popBackStack() },
                                onLogout = {
                                    // Sign out of Firebase as well as clearing the
                                    // local session. Clearing only storage leaves
                                    // Firebase's current user alive, which causes
                                    // LoginScreen to show its authenticated/loading state.
                                    firebaseAuthManager.signOut()
                                    sessionManager.logout()
                                    navController.popBackStack(
                                        navController.graph.findStartDestination().id,
                                        false
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}