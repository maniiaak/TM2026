package com.maniiaak.iluvmusic.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Platform-specific keys
private val IS_LOGGED_IN_KEY = booleanPreferencesKey("is_logged_in")
private val EMAIL_KEY = stringPreferencesKey("email")
private val USER_ID_KEY = intPreferencesKey("user_id")
private val FIREBASE_UID_KEY = stringPreferencesKey("firebase_uid")
private val USERNAME_KEY = stringPreferencesKey("username")
private val HANDLE_KEY = stringPreferencesKey("handle")
private val NEEDS_PROFILE_KEY = booleanPreferencesKey("needs_profile")

actual fun createPreferencesStorage(): PreferencesStorage {
    // Get DataStore from Koin at runtime
    return AndroidPreferencesStorage(org.koin.core.context.GlobalContext.get().get<DataStore<Preferences>>())
}

class AndroidPreferencesStorage(
    private val dataStore: DataStore<Preferences>
) : PreferencesStorage {

    override fun isLoggedIn(): Flow<Boolean> =
        dataStore.data.map { it[IS_LOGGED_IN_KEY] ?: false }

    override fun email(): Flow<String> =
        dataStore.data.map { it[EMAIL_KEY] ?: "" }

    override fun userId(): Flow<Int> =
        dataStore.data.map { it[USER_ID_KEY] ?: 0 }

    override fun firebaseUid(): Flow<String> =
        dataStore.data.map { it[FIREBASE_UID_KEY] ?: "" }

    override fun username(): Flow<String> =
        dataStore.data.map { it[USERNAME_KEY] ?: "" }

    override fun handle(): Flow<String> =
        dataStore.data.map { it[HANDLE_KEY] ?: "" }

    override fun needsProfile(): Flow<Boolean> =
        dataStore.data.map { it[NEEDS_PROFILE_KEY] ?: false }

    override suspend fun login(
        userEmail: String,
        backendUserId: Int,
        firebaseUserId: String,
        userUsername: String,
        userHandle: String,
        profileRequired: Boolean
    ) {
        dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN_KEY] = true
            preferences[EMAIL_KEY] = userEmail
            preferences[USER_ID_KEY] = backendUserId
            preferences[FIREBASE_UID_KEY] = firebaseUserId
            preferences[USERNAME_KEY] = userUsername
            preferences[HANDLE_KEY] = userHandle
            preferences[NEEDS_PROFILE_KEY] = profileRequired
        }
    }

    override suspend fun completeProfile(userUsername: String, userHandle: String, backendUserId: Int) {
        dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = backendUserId
            preferences[USERNAME_KEY] = userUsername
            preferences[HANDLE_KEY] = userHandle
            preferences[NEEDS_PROFILE_KEY] = false
        }
    }

    override suspend fun logout() {
        dataStore.edit { it.clear() }
    }

    override suspend fun getUserId(): Int? =
        userId().first().takeIf { it != 0 }
}