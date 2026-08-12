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

val IS_LOGGED_IN_KEY = booleanPreferencesKey("is_logged_in")
val EMAIL_KEY = stringPreferencesKey("email")
val USER_ID_KEY = intPreferencesKey("user_id")
val FIREBASE_UID_KEY = stringPreferencesKey("firebase_uid")
val USERNAME_KEY = stringPreferencesKey("username")
val HANDLE_KEY = stringPreferencesKey("handle")
val NEEDS_PROFILE_KEY = booleanPreferencesKey("needs_profile")

class SessionManager(private val dataStore: DataStore<Preferences>) {
    val isLoggedIn: Flow<Boolean> = dataStore.data.map { it[IS_LOGGED_IN_KEY] ?: false }
    val email: Flow<String> = dataStore.data.map { it[EMAIL_KEY] ?: "" }
    val userId: Flow<Int> = dataStore.data.map { it[USER_ID_KEY] ?: 0 }
    val firebaseUid: Flow<String> = dataStore.data.map { it[FIREBASE_UID_KEY] ?: "" }
    val username: Flow<String> = dataStore.data.map { it[USERNAME_KEY] ?: "" }
    val handle: Flow<String> = dataStore.data.map { it[HANDLE_KEY] ?: "" }
    val needsProfile: Flow<Boolean> = dataStore.data.map { it[NEEDS_PROFILE_KEY] ?: false }

    suspend fun login(
        userEmail: String,
        backendUserId: Int,
        firebaseUserId: String,
        userUsername: String = "",
        userHandle: String = "",
        profileRequired: Boolean = false
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

    suspend fun completeProfile(userUsername: String, userHandle: String, backendUserId: Int) {
        dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = backendUserId
            preferences[USERNAME_KEY] = userUsername
            preferences[HANDLE_KEY] = userHandle
            preferences[NEEDS_PROFILE_KEY] = false
        }
    }

    suspend fun logout() { dataStore.edit { it.clear() } }
    suspend fun getUserId(): Int? = userId.first().takeIf { it != 0 }
}
