package com.maniiaak.iluvmusic.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maniiaak.iluvmusic.data.AuthRepository
import com.maniiaak.iluvmusic.data.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class NeedsProfile(val email: String, val firebaseUid: String, val idToken: String) : AuthState()
    data class Success(val username: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val firebaseAuthManager: FirebaseAuthManager,
    private val repository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun signUp(email: String, password: String) = authenticate { firebaseAuthManager.signUp(email, password) }
    fun signIn(email: String, password: String) = authenticate { firebaseAuthManager.signIn(email, password) }

    private fun authenticate(action: suspend () -> AuthResult) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            when (val result = action()) {
                is AuthResult.Error -> _authState.value = AuthState.Error(result.message)
                is AuthResult.Success -> {
                    val token = firebaseAuthManager.getIdToken()
                    if (token == null) {
                        _authState.value = AuthState.Error("Could not obtain Firebase ID token")
                        return@launch
                    }
                    authenticateBackend(token, result.email, result.userId)
                }
            }
        }
    }

    private suspend fun authenticateBackend(token: String, email: String, firebaseUid: String) {
        repository.authenticateFirebase(token).fold(
            onSuccess = { response ->
                if (response.needs_profile || response.user_id == null) {
                    _authState.value = AuthState.NeedsProfile(email, firebaseUid, token)
                } else {
                    sessionManager.login(
                        userEmail = response.email ?: email,
                        backendUserId = response.user_id,
                        firebaseUserId = response.firebase_uid ?: firebaseUid,
                        userUsername = response.username.orEmpty(),
                        userHandle = response.handle.orEmpty()
                    )
                    _authState.value = AuthState.Success(response.username ?: response.handle.orEmpty())
                }
            },
            onFailure = { _authState.value = AuthState.Error(it.message ?: "Backend authentication failed") }
        )
    }

    fun completeProfile(username: String, handle: String) {
        viewModelScope.launch {
            val state = _authState.value
            if (state !is AuthState.NeedsProfile) return@launch
            _authState.value = AuthState.Loading
            repository.authenticateFirebase(state.idToken, username.trim(), handle.trim()).fold(
                onSuccess = { response ->
                    if (response.user_id == null) {
                        _authState.value = AuthState.Error(response.error ?: "Could not create profile")
                    } else {
                        sessionManager.login(
                            userEmail = response.email ?: state.email,
                            backendUserId = response.user_id,
                            firebaseUserId = response.firebase_uid ?: state.firebaseUid,
                            userUsername = response.username ?: username.trim(),
                            userHandle = response.handle ?: handle.trim(),
                            profileRequired = false
                        )
                        _authState.value = AuthState.Success(response.username ?: username.trim())
                    }
                },
                onFailure = { _authState.value = AuthState.Error(it.message ?: "Could not create profile") }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            firebaseAuthManager.signOut()
            sessionManager.logout()
            _authState.value = AuthState.Idle
        }
    }

    fun reset() { _authState.value = AuthState.Idle }
}
