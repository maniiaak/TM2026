package com.maniiaak.iluvmusic.data

import kotlinx.coroutines.flow.Flow

class SessionManager(
    private val storage: PreferencesStorage
) {
    val isLoggedIn: Flow<Boolean> = storage.isLoggedIn()
    val email: Flow<String> = storage.email()
    val userId: Flow<Int> = storage.userId()
    val firebaseUid: Flow<String> = storage.firebaseUid()
    val username: Flow<String> = storage.username()
    val handle: Flow<String> = storage.handle()
    val needsProfile: Flow<Boolean> = storage.needsProfile()

    suspend fun login(
        userEmail: String,
        backendUserId: Int,
        firebaseUserId: String,
        userUsername: String = "",
        userHandle: String = "",
        profileRequired: Boolean = false
    ) {
        storage.login(
            userEmail = userEmail,
            backendUserId = backendUserId,
            firebaseUserId = firebaseUserId,
            userUsername = userUsername,
            userHandle = userHandle,
            profileRequired = profileRequired
        )
    }

    suspend fun completeProfile(userUsername: String, userHandle: String, backendUserId: Int) {
        storage.completeProfile(userUsername, userHandle, backendUserId)
    }

    suspend fun logout() {
        storage.logout()
    }

    suspend fun getUserId(): Int? = storage.getUserId()
}
