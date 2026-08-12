package com.maniiaak.iluvmusic.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    viewModel: AuthViewModel = koinViewModel()
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var handle by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isSignUp by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Success -> onLoginSuccess(state.username)
            is AuthState.Error -> errorMessage = state.message
            else -> {}
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (authState is AuthState.NeedsProfile) "Set up your profile" else if (isSignUp) "Create Account" else "Sign In",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))

        when (authState) {
            is AuthState.Idle -> {
                Text(if (isSignUp) "Create a new account to get started" else "Sign in to your account")
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                OutlinedTextField(
                    password, { password = it }, label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = { TextButton(onClick = { isPasswordVisible = !isPasswordVisible }) { Text(if (isPasswordVisible) "Hide" else "Show") } }
                )
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 16.dp)) }
                Button(
                    onClick = { errorMessage = null; if (isSignUp) viewModel.signUp(email, password) else viewModel.signIn(email, password) },
                    enabled = email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text(if (isSignUp) "Create Account" else "Sign In") }
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = { isSignUp = !isSignUp; errorMessage = null }) {
                    Text(if (isSignUp) "Already have an account? Sign In" else "Don't have an account? Sign Up")
                }
            }
            is AuthState.NeedsProfile -> {
                Text("Choose a username and a handle for your profile.")
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(username, { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), singleLine = true)
                OutlinedTextField(handle, { handle = it.removePrefix("@").replace(" ", "") }, label = { Text("Handle") }, prefix = { Text("@") }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), singleLine = true)
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 16.dp)) }
                Button(
                    onClick = { errorMessage = null; viewModel.completeProfile(username, handle) },
                    enabled = username.trim().isNotEmpty() && handle.trim().isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("Continue") }
            }
            is AuthState.Loading -> { CircularProgressIndicator(); Spacer(Modifier.height(16.dp)); Text("Please wait...") }
            is AuthState.Success -> { CircularProgressIndicator(); Spacer(Modifier.height(16.dp)); Text("Welcome!") }
            is AuthState.Error -> {
                Text(errorMessage ?: "An error occurred", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 16.dp))
                Button(onClick = { viewModel.reset(); errorMessage = null }) { Text("Try Again") }
            }
        }
    }
}
