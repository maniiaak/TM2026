package com.maniiaak.iluvmusic.data

import kotlinx.browser.window
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

actual fun createPreferencesStorage(): PreferencesStorage {
    return WasmPreferencesStorage()
}

class WasmPreferencesStorage : PreferencesStorage {

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_EMAIL = "email"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_FIREBASE_UID = "firebase_uid"
        private const val KEY_USERNAME = "username"
        private const val KEY_HANDLE = "handle"
        private const val KEY_NEEDS_PROFILE = "needs_profile"
    }

    // State flows to mirror the reactive nature of DataStore
    private val _isLoggedIn = MutableStateFlow(false)
    private val _email = MutableStateFlow("")
    private val _userId = MutableStateFlow(0)
    private val _firebaseUid = MutableStateFlow("")
    private val _username = MutableStateFlow("")
    private val _handle = MutableStateFlow("")
    private val _needsProfile = MutableStateFlow(false)

    init {
        // Load initial values from localStorage
        loadFromStorage()
    }

    override fun isLoggedIn(): Flow<Boolean> = _isLoggedIn.asStateFlow()
    override fun email(): Flow<String> = _email.asStateFlow()
    override fun userId(): Flow<Int> = _userId.asStateFlow()
    override fun firebaseUid(): Flow<String> = _firebaseUid.asStateFlow()
    override fun username(): Flow<String> = _username.asStateFlow()
    override fun handle(): Flow<String> = _handle.asStateFlow()
    override fun needsProfile(): Flow<Boolean> = _needsProfile.asStateFlow()

    @Suppress("UnsafeCastFromDynamic")
    private fun loadFromStorage() {
        _isLoggedIn.value = window.localStorage.getItem(KEY_IS_LOGGED_IN)?.toBoolean() ?: false
        _email.value = window.localStorage.getItem(KEY_EMAIL) ?: ""
        _userId.value = window.localStorage.getItem(KEY_USER_ID)?.toIntOrNull() ?: 0
        _firebaseUid.value = window.localStorage.getItem(KEY_FIREBASE_UID) ?: ""
        _username.value = window.localStorage.getItem(KEY_USERNAME) ?: ""
        _handle.value = window.localStorage.getItem(KEY_HANDLE) ?: ""
        _needsProfile.value = window.localStorage.getItem(KEY_NEEDS_PROFILE)?.toBoolean() ?: false
    }

    @Suppress("UnsafeCastFromDynamic")
    override suspend fun login(
        userEmail: String,
        backendUserId: Int,
        firebaseUserId: String,
        userUsername: String,
        userHandle: String,
        profileRequired: Boolean
    ) {
        withContext(Dispatchers.Default) {
            window.localStorage.setItem(KEY_IS_LOGGED_IN, "true")
            window.localStorage.setItem(KEY_EMAIL, userEmail)
            window.localStorage.setItem(KEY_USER_ID, backendUserId.toString())
            window.localStorage.setItem(KEY_FIREBASE_UID, firebaseUserId)
            window.localStorage.setItem(KEY_USERNAME, userUsername)
            window.localStorage.setItem(KEY_HANDLE, userHandle)
            window.localStorage.setItem(KEY_NEEDS_PROFILE, profileRequired.toString())

            // Update state flows
            _isLoggedIn.value = true
            _email.value = userEmail
            _userId.value = backendUserId
            _firebaseUid.value = firebaseUserId
            _username.value = userUsername
            _handle.value = userHandle
            _needsProfile.value = profileRequired
        }
    }

    @Suppress("UnsafeCastFromDynamic")
    override suspend fun completeProfile(userUsername: String, userHandle: String, backendUserId: Int) {
        withContext(Dispatchers.Default) {
            window.localStorage.setItem(KEY_USER_ID, backendUserId.toString())
            window.localStorage.setItem(KEY_USERNAME, userUsername)
            window.localStorage.setItem(KEY_HANDLE, userHandle)
            window.localStorage.setItem(KEY_NEEDS_PROFILE, "false")

            _userId.value = backendUserId
            _username.value = userUsername
            _handle.value = userHandle
            _needsProfile.value = false
        }
    }

    @Suppress("UnsafeCastFromDynamic")
    override suspend fun logout() {
        withContext(Dispatchers.Default) {
            window.localStorage.clear()

            _isLoggedIn.value = false
            _email.value = ""
            _userId.value = 0
            _firebaseUid.value = ""
            _username.value = ""
            _handle.value = ""
            _needsProfile.value = false
        }
    }

    override suspend fun getUserId(): Int? =
        userId().first().takeIf { it != 0 }
}