package com.maniiaak.iluvmusic.data

import kotlinx.coroutines.flow.Flow

expect fun createPreferencesStorage(): PreferencesStorage

interface PreferencesStorage {
    fun isLoggedIn(): Flow<Boolean>
    fun email(): Flow<String>
    fun userId(): Flow<Int>
    fun firebaseUid(): Flow<String>
    fun username(): Flow<String>
    fun handle(): Flow<String>
    fun needsProfile(): Flow<Boolean>

    suspend fun login(
        userEmail: String,
        backendUserId: Int,
        firebaseUserId: String,
        userUsername: String = "",
        userHandle: String = "",
        profileRequired: Boolean = false
    )

    suspend fun completeProfile(userUsername: String, userHandle: String, backendUserId: Int)

    suspend fun logout()

    suspend fun getUserId(): Int?
}