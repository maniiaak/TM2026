package com.maniiaak.iluvmusic.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maniaak.iluvmusic.data.AuthRepository
import com.maniaak.iluvmusic.data.SessionManager
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
                onSuccess = { response ->
                    println("🔍 Received user_id: ${response.user_id} (Type: ${response.user_id::class.simpleName})")
                    if (response.user_id == 0) {
                        println("❌ ERROR: user_id is 0!")
                    }

                    // 1. Save the session
                    val userId = response.user_id
                    val username = response.username

                    sessionManager.login(username, userId)

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