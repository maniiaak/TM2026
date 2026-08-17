package com.maniiaak.iluvmusic.auth

import com.google.firebase.auth.FirebaseAuth
import com.maniiaak.iluvmusic.auth.AuthResult
import com.maniiaak.iluvmusic.auth.FirebaseAuthManager
import kotlinx.coroutines.tasks.await

class AndroidFirebaseAuthManager : FirebaseAuthManager {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override suspend fun signUp(email: String, password: String): AuthResult {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                AuthResult.Success(email = user.email ?: "", userId = user.uid)
            } else {
                AuthResult.Error("User creation failed")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Sign up failed")
        }
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                AuthResult.Success(email = user.email ?: "", userId = user.uid)
            } else {
                AuthResult.Error("Login failed")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Sign in failed")
        }
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    override fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email
    }

    override suspend fun getIdToken(): String? {
        return try {
            auth.currentUser?.getIdToken(false)?.await()?.token
        } catch (e: Exception) {
            null
        }
    }
}

actual fun createFirebaseAuthManager(): FirebaseAuthManager = AndroidFirebaseAuthManager()