package com.maniiaak.iluvmusic.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.await
import kotlin.js.JsAny
import kotlin.js.Promise

// Firebase JS SDK external interfaces for Kotlin/WASM
// Use Promise<JsAny?> for all Promise returns since external interfaces
// are not automatically subtypes of JsAny? for type parameter bounds

external interface FirebaseAuth : JsAny {
    val currentUser: FirebaseUser?
    fun signOut(): Promise<JsAny?>
}

external interface FirebaseUser : JsAny {
    val uid: String
    val email: String?
    fun getIdToken(forceRefresh: Boolean): Promise<JsString?>
}

external interface FirebaseIdTokenResult : JsAny {
    val token: String
}

external interface AuthResultJS : JsAny {
    val user: FirebaseUser?
}

external interface FirebaseAuthModule {
    fun createUserWithEmailAndPassword(auth: FirebaseAuth, email: String, password: String): Promise<kotlin.js.JsAny?>
    fun signInWithEmailAndPassword(auth: FirebaseAuth, email: String, password: String): Promise<kotlin.js.JsAny?>
}

external interface UserCredentialJS : JsAny {
    val user: FirebaseUser?
}

// Helper to get Firebase Auth instance from global window
// Use nullable external properties to avoid WebAssembly.Exception at module load time
external val __FIREBASE_AUTH__: FirebaseAuth?
external val firebaseAuthModule: FirebaseAuthModule?

private fun getFirebaseAuth(): FirebaseAuth {
    return __FIREBASE_AUTH__ ?: throw IllegalStateException("Firebase Auth not initialized. Make sure index.html includes Firebase initialization.")
}

private fun getFirebaseAuthModule(): FirebaseAuthModule {
    return firebaseAuthModule ?: throw IllegalStateException("Firebase Auth module not available. Make sure firebase/auth is loaded.")
}

class WasmFirebaseAuthManager : FirebaseAuthManager {

    override suspend fun signUp(email: String, password: String): AuthResult {
        return withContext(Dispatchers.Default) {
            try {
                val auth = getFirebaseAuth()
                val module = getFirebaseAuthModule()
                val result = module.createUserWithEmailAndPassword(auth, email, password).await<JsAny?>() as AuthResultJS
                val user = result.user
                if (user != null) {
                    AuthResult.Success(email = user.email ?: "", userId = user.uid)
                } else {
                    AuthResult.Error("User creation failed")
                }
            } catch (e: Throwable) {
                val msg = e.message ?: ""
                val message = when {
                    msg.contains("auth/email-already-in-use") -> "Email already in use"
                    msg.contains("auth/invalid-email") -> "Invalid email address"
                    msg.contains("auth/weak-password") -> "Password is too weak"
                    else -> msg.ifBlank { "Sign up failed" }
                }
                AuthResult.Error(message)
            }
        }
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        return withContext(Dispatchers.Default) {
            try {
                val auth = getFirebaseAuth()
                val module = getFirebaseAuthModule()
                val resultJs = module.signInWithEmailAndPassword(auth, email, password).await<JsAny?>()
                val user = (resultJs as UserCredentialJS).user
                if (user != null) {
                    AuthResult.Success(email = user.email ?: "", userId = user.uid)
                } else {
                    AuthResult.Error("Login failed")
                }
            } catch (e: Throwable) {
                val message = when {
                    e.message?.contains("auth/user-not-found") == true -> "No account found with this email"
                    e.message?.contains("auth/wrong-password") == true -> "Incorrect password"
                    e.message?.contains("auth/invalid-credential") == true -> "Invalid email or password"
                    e.message?.contains("auth/invalid-email") == true -> "Invalid email address"
                    else -> e.message ?: "Sign in failed"
                }
                AuthResult.Error(message)
            }
        }
    }

    override suspend fun signOut() {
        withContext(Dispatchers.Default) {
            try {
                val auth = getFirebaseAuth()
                auth.signOut().await<JsAny?>()
            } catch (e: Throwable) {
                println("Firebase failure: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    override fun isUserLoggedIn(): Boolean {
        return try {
            getFirebaseAuth().currentUser != null
        } catch (e: Exception) {
            false
        }
    }

    override fun getCurrentUserEmail(): String? {
        return try {
            getFirebaseAuth().currentUser?.email
        } catch (e: Throwable) {
            println("Firebase failure: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    override suspend fun getIdToken(): String? {
        return withContext(Dispatchers.Default) {
            try {
                val auth = getFirebaseAuth()
                val user = auth.currentUser
                if (user != null) {
                    val token = user.getIdToken(false).await<JsString?>()
                    token?.toString()
                } else {
                    null
                }
            } catch (e: Throwable) {
                println("Firebase failure: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }
}

actual fun createFirebaseAuthManager(): FirebaseAuthManager = WasmFirebaseAuthManager()