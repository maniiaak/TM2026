package com.jetbrains.kmpapp.screens.auth

import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.ktor.http.*
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    viewModel: AuthViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Spotify Config
    val clientId = "f1d29529a59846d09d04199689cc2446"
    val redirectUri = "com.jetbrains.kmpapp://callback"
    val scopes = "user-read-email user-read-private"

    val authUrl = remember(clientId, redirectUri, scopes) {
        "https://accounts.spotify.com/authorize?" +
                "client_id=${clientId.encodeURLParameter()}" +
                "&response_type=code" +
                "&redirect_uri=${redirectUri.encodeURLParameter()}" +
                "&scope=${scopes.encodeURLParameter()}" +
                "&show_dialog=true"
    }

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Success -> onLoginSuccess(state.username)
            is AuthState.Error -> errorMessage = state.message
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Sign in with Spotify", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text("Log in to start reviewing albums", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(32.dp))

        when (authState) {
            is AuthState.Idle, is AuthState.Loading -> {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true

                            webViewClient = object : WebViewClient() {

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    url: String?
                                ): Boolean {

                                    if (url != null && url.startsWith(redirectUri)) {

                                        val parsedUrl = Url(url)
                                        val code = parsedUrl.parameters["code"]

                                        if (code != null) {
                                            viewModel.exchangeSpotifyCode(code)
                                            return true
                                        }
                                    }

                                    return false
                                }
                            }

                            webChromeClient = WebChromeClient()
                            loadUrl(authUrl)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                )
            }
            is AuthState.Success -> {
                CircularProgressIndicator()
                Text("Logging you in...", modifier = Modifier.padding(top = 16.dp))
            }
            is AuthState.Error -> {
                Text(errorMessage ?: "An error occurred", color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { viewModel.reset(); errorMessage = null }) {
                    Text("Try Again")
                }
            }
        }
    }
}