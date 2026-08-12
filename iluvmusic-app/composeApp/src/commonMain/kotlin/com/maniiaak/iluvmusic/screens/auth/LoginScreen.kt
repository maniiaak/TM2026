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
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isSignUp by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Success -> onLoginSuccess(state.email)
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
        Text(
            if (isSignUp) "Create Account" else "Sign In",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))
        Text(
            if (isSignUp) "Create a new account to get started" else "Sign in to your account",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(32.dp))

        when (authState) {
            is AuthState.Idle -> {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    enabled = authState !is AuthState.Loading
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        Button(
                            onClick = { isPasswordVisible = !isPasswordVisible },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(if (isPasswordVisible) "Hide" else "Show")
                        }
                    },
                    enabled = authState !is AuthState.Loading
                )

                errorMessage?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                Button(
                    onClick = {
                        if (email.isNotBlank() && password.isNotBlank()) {
                            errorMessage = null
                            if (isSignUp) {
                                viewModel.signUp(email, password)
                            } else {
                                viewModel.signIn(email, password)
                            }
                        } else {
                            errorMessage = "Email and password cannot be empty"
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = email.isNotBlank() && password.isNotBlank()
                ) {
                    Text(if (isSignUp) "Create Account" else "Sign In")
                }

                Spacer(Modifier.height(16.dp))

                TextButton(
                    onClick = {
                        isSignUp = !isSignUp
                        errorMessage = null
                    }
                ) {
                    Text(if (isSignUp) "Already have an account? Sign In" else "Don't have an account? Sign Up")
                }
            }

            is AuthState.Loading -> {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(if (isSignUp) "Creating account..." else "Signing in...", style = MaterialTheme.typography.bodyLarge)
            }

            is AuthState.Success -> {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Welcome!", style = MaterialTheme.typography.bodyLarge)
            }

            is AuthState.Error -> {
                Text(
                    errorMessage ?: "An error occurred",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(
                    onClick = {
                        viewModel.reset()
                        errorMessage = null
                    }
                ) {
                    Text("Try Again")
                }
            }
        }
    }
}