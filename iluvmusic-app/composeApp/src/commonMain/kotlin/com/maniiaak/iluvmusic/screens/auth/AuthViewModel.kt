package com.maniiaak.iluvmusic.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maniiaak.iluvmusic.data.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val email: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val firebaseAuthManager: FirebaseAuthManager,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = firebaseAuthManager.signUp(email, password)

            when (result) {
                is AuthResult.Success -> {
                    sessionManager.login(result.email, result.userId)
                    _authState.value = AuthState.Success(result.email)
                }
                is AuthResult.Error -> {
                    _authState.value = AuthState.Error(result.message)
                }
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = firebaseAuthManager.signIn(email, password)

            when (result) {
                is AuthResult.Success -> {
                    sessionManager.login(result.email, result.userId)
                    _authState.value = AuthState.Success(result.email)
                }
                is AuthResult.Error -> {
                    _authState.value = AuthState.Error(result.message)
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            firebaseAuthManager.signOut()
            sessionManager.logout()
            _authState.value = AuthState.Idle
        }
    }

    fun reset() {
        _authState.value = AuthState.Idle
    }
}