package com.jetbrains.kmpapp.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.kmpapp.data.AuthRepository
import com.jetbrains.kmpapp.data.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val username: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val repository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun exchangeSpotifyCode(code: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            val result = repository.exchangeSpotifyCode(code)

            result.fold(
                onSuccess = { username ->
                    // 1. Save the session
                    sessionManager.login(username)

                    // 2. Update state to success
                    _authState.value = AuthState.Success(username)
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(error.message ?: "Login failed")
                }
            )
        }
    }

    fun reset() {
        _authState.value = AuthState.Idle
    }
}