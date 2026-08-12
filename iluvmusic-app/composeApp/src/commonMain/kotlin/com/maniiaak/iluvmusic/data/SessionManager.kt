package com.maniiaak.iluvmusic.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val IS_LOGGED_IN_KEY = booleanPreferencesKey("is_logged_in")
val EMAIL_KEY = stringPreferencesKey("email")
val USER_ID_KEY = stringPreferencesKey("user_id")

class SessionManager(private val dataStore: DataStore<Preferences>) {

    val isLoggedIn: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[IS_LOGGED_IN_KEY] ?: false
    }

    val email: Flow<String> = dataStore.data.map { preferences ->
        preferences[EMAIL_KEY] ?: ""
    }

    val firebaseUserId: Flow<String> = dataStore.data.map { preferences ->
        preferences[USER_ID_KEY] ?: ""
    }

    // Legacy userId for backend - converts Firebase UID to Int for compatibility
    val userId: Flow<Int> = firebaseUserId.map { firebaseUid ->
        if (firebaseUid.isEmpty()) 0
        else firebaseUid.hashCode().let { if (it < 0) -it else it } % 1_000_000_000
    }

    suspend fun login(userEmail: String, firebaseUserId: String) {
        dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN_KEY] = true
            preferences[EMAIL_KEY] = userEmail
            preferences[USER_ID_KEY] = firebaseUserId
        }
    }

    suspend fun logout() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    suspend fun getUserId(): Int? {
        return userId.first().takeIf { it != 0 }
    }

    suspend fun getEmail(): String? {
        return email.first().takeIf { it.isNotEmpty() }
    }
}