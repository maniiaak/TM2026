package com.maniiaak.iluvmusic.screens.auth

sealed class AuthResult {
    data class Success(val email: String, val userId: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

interface FirebaseAuthManager {
    suspend fun signUp(email: String, password: String): AuthResult
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun signOut()
    fun isUserLoggedIn(): Boolean
    fun getCurrentUserEmail(): String?
    suspend fun getIdToken(): String?
}
