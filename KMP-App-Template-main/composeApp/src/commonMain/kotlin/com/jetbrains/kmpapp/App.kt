package com.jetbrains.kmpapp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.jetbrains.kmpapp.data.SessionManager
import com.jetbrains.kmpapp.screens.auth.LoginScreen
import com.jetbrains.kmpapp.screens.detail.DetailScreen
import com.jetbrains.kmpapp.screens.list.ListScreen
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
object ListDestination

@Serializable
object LoginDestination

@Serializable
data class DetailDestination(val objectId: Int)

@Composable
fun App(
    sessionManager: SessionManager = koinInject()
) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    ) {
        Surface {
            val navController: NavHostController = rememberNavController()

            val isLoggedIn by sessionManager.isLoggedIn.collectAsState(initial = false)
            val startDestination = if (isLoggedIn) ListDestination else LoginDestination

            NavHost(navController = navController, startDestination = startDestination) {

                composable<LoginDestination> {
                    LoginScreen(
                        onLoginSuccess = { username ->
                            navController.navigate(ListDestination) {
                                popUpTo(LoginDestination) { inclusive = true }
                            }
                        }
                    )
                }

                composable<ListDestination> {
                    ListScreen(
                        navigateToDetails = { objectId ->
                            navController.navigate(DetailDestination(objectId))
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
                    println("Navigating to DetailScreen with ID: $objectId")

                    DetailScreen(
                        objectId = objectId,
                        navigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}